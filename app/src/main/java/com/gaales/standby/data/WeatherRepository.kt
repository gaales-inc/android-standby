package com.gaales.standby.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.roundToInt

data class HourForecast(val epochSec: Long, val temp: Int, val code: Int, val isDay: Boolean)

data class Weather(
    val city: String,
    val usingLocation: Boolean,
    val temp: Int,
    val code: Int,
    val isDay: Boolean,
    val high: Int,
    val low: Int,
    val hourly: List<HourForecast>,
    val sunrise: Long?,
    val sunset: Long?,
    val fetchedAt: Long,
)

/**
 * Current conditions from Open-Meteo (free, no API key). Uses the last known coarse location when
 * allowed, otherwise the main city of the device's time zone.
 */
object WeatherRepository {
    var weather by mutableStateOf<Weather?>(null)
        private set

    private const val PREFS = "weather_cache"
    private const val MAX_AGE_MS = 30 * 60_000L

    suspend fun refresh(context: Context, force: Boolean = false) {
        if (weather == null) loadCache(context)
        val current = weather
        if (!force && current != null && System.currentTimeMillis() - current.fetchedAt < MAX_AGE_MS) return
        val result = withContext(Dispatchers.IO) { runCatching { fetch(context) }.getOrNull() }
        if (result != null) weather = result
    }

    private fun loadCache(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json = prefs.getString("json", null) ?: return
        val city = prefs.getString("city", null) ?: return
        val usingLocation = prefs.getBoolean("using_location", false)
        val fetchedAt = prefs.getLong("fetched_at", 0)
        weather = runCatching { parse(JSONObject(json), city, usingLocation, fetchedAt) }.getOrNull()
    }

    private fun fetch(context: Context): Weather {
        val location = lastKnownLocation(context)
        val fallback = Places.local()
        val lat = location?.latitude ?: fallback.lat
        val lon = location?.longitude ?: fallback.lon
        val city = location?.let { cityName(context, it) } ?: fallback.city
        val unit = if (usesFahrenheit()) "fahrenheit" else "celsius"
        val url = URL(
            "https://api.open-meteo.com/v1/forecast?latitude=%.3f&longitude=%.3f".format(Locale.US, lat, lon) +
                "&current=temperature_2m,weather_code,is_day" +
                "&hourly=temperature_2m,weather_code,is_day" +
                "&daily=temperature_2m_max,temperature_2m_min,sunrise,sunset" +
                "&timezone=auto&forecast_days=2&timeformat=unixtime&temperature_unit=$unit"
        )
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
        }
        val body = try {
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
        val now = System.currentTimeMillis()
        val parsed = parse(JSONObject(body), city, location != null, now)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putString("json", body)
            putString("city", city)
            putBoolean("using_location", location != null)
            putLong("fetched_at", now)
        }
        return parsed
    }

    private fun parse(root: JSONObject, city: String, usingLocation: Boolean, fetchedAt: Long): Weather {
        val current = root.getJSONObject("current")
        val daily = root.getJSONObject("daily")
        val hourly = root.getJSONObject("hourly")
        val times = hourly.getJSONArray("time")
        val temps = hourly.getJSONArray("temperature_2m")
        val codes = hourly.getJSONArray("weather_code")
        val days = hourly.getJSONArray("is_day")
        val hours = (0 until times.length()).map { i ->
            HourForecast(times.getLong(i), temps.getDouble(i).roundToInt(), codes.getInt(i), days.getInt(i) == 1)
        }
        val sunrise = daily.optJSONArray("sunrise")?.optLong(0)?.takeIf { it > 0 }?.times(1000)
        val sunset = daily.optJSONArray("sunset")?.optLong(0)?.takeIf { it > 0 }?.times(1000)
        return Weather(
            city = city,
            usingLocation = usingLocation,
            temp = current.getDouble("temperature_2m").roundToInt(),
            code = current.getInt("weather_code"),
            isDay = current.optInt("is_day", 1) == 1,
            high = daily.getJSONArray("temperature_2m_max").getDouble(0).roundToInt(),
            low = daily.getJSONArray("temperature_2m_min").getDouble(0).roundToInt(),
            hourly = hours,
            sunrise = sunrise,
            sunset = sunset,
            fetchedAt = fetchedAt,
        )
    }

    fun hasLocationPermission(context: Context) =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @Suppress("MissingPermission")
    private fun lastKnownLocation(context: Context): Location? {
        if (!hasLocationPermission(context)) return null
        val lm = context.getSystemService(LocationManager::class.java) ?: return null
        return lm.getProviders(true)
            .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
    }

    @Suppress("DEPRECATION")
    private fun cityName(context: Context, location: Location): String? = runCatching {
        if (!Geocoder.isPresent()) return null
        Geocoder(context, Locale.getDefault()).getFromLocation(location.latitude, location.longitude, 1)
            ?.firstOrNull()?.let { it.locality ?: it.subAdminArea ?: it.adminArea }
    }.getOrNull()

    private fun usesFahrenheit(): Boolean =
        Locale.getDefault().country in setOf("US", "LR", "MM", "BS", "BZ", "KY", "PW", "FM", "MH")

    /** WMO weather interpretation codes, as used by Open-Meteo. */
    fun describe(code: Int): String = when (code) {
        0 -> "Clear"
        1 -> "Mostly Clear"
        2 -> "Partly Cloudy"
        3 -> "Cloudy"
        45, 48 -> "Foggy"
        51, 53, 55 -> "Drizzle"
        56, 57 -> "Freezing Drizzle"
        61, 63 -> "Rain"
        65 -> "Heavy Rain"
        66, 67 -> "Freezing Rain"
        71, 73 -> "Snow"
        75 -> "Heavy Snow"
        77 -> "Snow Grains"
        80, 81 -> "Showers"
        82 -> "Heavy Showers"
        85, 86 -> "Snow Showers"
        95 -> "Thunderstorms"
        96, 99 -> "Hail"
        else -> "—"
    }
}
