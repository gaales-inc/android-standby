package com.gaales.standby.data

import java.time.ZoneId
import kotlin.math.roundToInt

data class Place(val city: String, val lat: Double, val lon: Double, val zone: String) {
    val shortName: String get() = city.filter { it.isLetter() }.take(3).uppercase()
}

/**
 * A small offline gazetteer: lets the World clock, World Clock widget and Weather work without
 * location access by placing you at the main city of your time zone.
 */
object Places {
    private val table = listOf(
        Place("Los Angeles", 34.05, -118.24, "America/Los_Angeles"),
        Place("Vancouver", 49.28, -123.12, "America/Vancouver"),
        Place("Denver", 39.74, -104.99, "America/Denver"),
        Place("Phoenix", 33.45, -112.07, "America/Phoenix"),
        Place("Chicago", 41.88, -87.63, "America/Chicago"),
        Place("New York", 40.71, -74.01, "America/New_York"),
        Place("Toronto", 43.65, -79.38, "America/Toronto"),
        Place("Halifax", 44.65, -63.57, "America/Halifax"),
        Place("St. John's", 47.56, -52.71, "America/St_Johns"),
        Place("Anchorage", 61.22, -149.90, "America/Anchorage"),
        Place("Honolulu", 21.31, -157.86, "Pacific/Honolulu"),
        Place("Mexico City", 19.43, -99.13, "America/Mexico_City"),
        Place("Bogotá", 4.71, -74.07, "America/Bogota"),
        Place("Lima", -12.05, -77.04, "America/Lima"),
        Place("Caracas", 10.48, -66.90, "America/Caracas"),
        Place("Santiago", -33.45, -70.67, "America/Santiago"),
        Place("Buenos Aires", -34.60, -58.38, "America/Argentina/Buenos_Aires"),
        Place("São Paulo", -23.55, -46.63, "America/Sao_Paulo"),
        Place("Reykjavík", 64.15, -21.94, "Atlantic/Reykjavik"),
        Place("London", 51.51, -0.13, "Europe/London"),
        Place("Dublin", 53.35, -6.26, "Europe/Dublin"),
        Place("Lisbon", 38.72, -9.14, "Europe/Lisbon"),
        Place("Madrid", 40.42, -3.70, "Europe/Madrid"),
        Place("Paris", 48.86, 2.35, "Europe/Paris"),
        Place("Brussels", 50.85, 4.35, "Europe/Brussels"),
        Place("Amsterdam", 52.37, 4.90, "Europe/Amsterdam"),
        Place("Berlin", 52.52, 13.40, "Europe/Berlin"),
        Place("Zurich", 47.38, 8.54, "Europe/Zurich"),
        Place("Rome", 41.90, 12.50, "Europe/Rome"),
        Place("Vienna", 48.21, 16.37, "Europe/Vienna"),
        Place("Prague", 50.08, 14.44, "Europe/Prague"),
        Place("Warsaw", 52.23, 21.01, "Europe/Warsaw"),
        Place("Budapest", 47.50, 19.04, "Europe/Budapest"),
        Place("Bucharest", 44.43, 26.10, "Europe/Bucharest"),
        Place("Stockholm", 59.33, 18.07, "Europe/Stockholm"),
        Place("Oslo", 59.91, 10.75, "Europe/Oslo"),
        Place("Copenhagen", 55.68, 12.57, "Europe/Copenhagen"),
        Place("Helsinki", 60.17, 24.94, "Europe/Helsinki"),
        Place("Athens", 37.98, 23.73, "Europe/Athens"),
        Place("Istanbul", 41.01, 28.98, "Europe/Istanbul"),
        Place("Kyiv", 50.45, 30.52, "Europe/Kyiv"),
        Place("Kyiv", 50.45, 30.52, "Europe/Kiev"),
        Place("Moscow", 55.76, 37.62, "Europe/Moscow"),
        Place("Cairo", 30.04, 31.24, "Africa/Cairo"),
        Place("Lagos", 6.52, 3.38, "Africa/Lagos"),
        Place("Accra", 5.60, -0.19, "Africa/Accra"),
        Place("Casablanca", 33.57, -7.59, "Africa/Casablanca"),
        Place("Addis Ababa", 9.03, 38.74, "Africa/Addis_Ababa"),
        Place("Nairobi", -1.29, 36.82, "Africa/Nairobi"),
        Place("Kampala", 0.35, 32.58, "Africa/Kampala"),
        Place("Kigali", -1.94, 30.06, "Africa/Kigali"),
        Place("Dar es Salaam", -6.79, 39.21, "Africa/Dar_es_Salaam"),
        Place("Johannesburg", -26.20, 28.05, "Africa/Johannesburg"),
        Place("Jerusalem", 31.77, 35.21, "Asia/Jerusalem"),
        Place("Riyadh", 24.71, 46.68, "Asia/Riyadh"),
        Place("Doha", 25.29, 51.53, "Asia/Qatar"),
        Place("Dubai", 25.20, 55.27, "Asia/Dubai"),
        Place("Baku", 40.41, 49.87, "Asia/Baku"),
        Place("Tehran", 35.69, 51.39, "Asia/Tehran"),
        Place("Karachi", 24.86, 67.01, "Asia/Karachi"),
        Place("Tashkent", 41.30, 69.24, "Asia/Tashkent"),
        Place("Almaty", 43.24, 76.89, "Asia/Almaty"),
        Place("New Delhi", 28.61, 77.21, "Asia/Kolkata"),
        Place("New Delhi", 28.61, 77.21, "Asia/Calcutta"),
        Place("Colombo", 6.93, 79.86, "Asia/Colombo"),
        Place("Kathmandu", 27.72, 85.32, "Asia/Kathmandu"),
        Place("Dhaka", 23.81, 90.41, "Asia/Dhaka"),
        Place("Yangon", 16.84, 96.17, "Asia/Yangon"),
        Place("Bangkok", 13.76, 100.50, "Asia/Bangkok"),
        Place("Ho Chi Minh City", 10.82, 106.63, "Asia/Ho_Chi_Minh"),
        Place("Jakarta", -6.21, 106.85, "Asia/Jakarta"),
        Place("Kuala Lumpur", 3.14, 101.69, "Asia/Kuala_Lumpur"),
        Place("Singapore", 1.35, 103.82, "Asia/Singapore"),
        Place("Manila", 14.60, 120.98, "Asia/Manila"),
        Place("Hong Kong", 22.32, 114.17, "Asia/Hong_Kong"),
        Place("Shanghai", 31.23, 121.47, "Asia/Shanghai"),
        Place("Taipei", 25.03, 121.57, "Asia/Taipei"),
        Place("Seoul", 37.57, 126.98, "Asia/Seoul"),
        Place("Tokyo", 35.68, 139.69, "Asia/Tokyo"),
        Place("Perth", -31.95, 115.86, "Australia/Perth"),
        Place("Adelaide", -34.93, 138.60, "Australia/Adelaide"),
        Place("Brisbane", -27.47, 153.03, "Australia/Brisbane"),
        Place("Melbourne", -37.81, 144.96, "Australia/Melbourne"),
        Place("Sydney", -33.87, 151.21, "Australia/Sydney"),
        Place("Auckland", -36.85, 174.76, "Pacific/Auckland"),
        Place("Greenwich", 51.48, 0.0, "UTC"),
        Place("Greenwich", 51.48, 0.0, "Etc/UTC"),
        Place("Greenwich", 51.48, 0.0, "GMT"),
    )

    private val byZone = table.associateBy { it.zone }

    fun forZone(zoneId: String): Place = byZone[zoneId] ?: run {
        val zone = runCatching { ZoneId.of(zoneId) }.getOrDefault(ZoneId.systemDefault())
        val offsetHours = zone.rules.getOffset(java.time.Instant.now()).totalSeconds / 3600.0
        val name = zoneId.substringAfterLast('/').replace('_', ' ')
        Place(name, 20.0, (offsetHours * 15).coerceIn(-180.0, 180.0), zoneId)
    }

    fun local(): Place = forZone(ZoneId.systemDefault().id)

    private val worldClockCandidates = listOf(
        "Asia/Tokyo", "Europe/London", "America/New_York", "Australia/Sydney", "Europe/Paris", "Asia/Dubai", "America/Los_Angeles",
    )

    /** Four cities like the iOS World Clock widget: here first, then three far-away places. */
    fun worldClockCities(): List<Place> {
        val here = local()
        val now = java.time.Instant.now()
        val hereOffset = ZoneId.of(here.zone).rules.getOffset(now)
        val others = worldClockCandidates
            .map { forZone(it) }
            .filter { ZoneId.of(it.zone).rules.getOffset(now) != hereOffset }
            .distinctBy { it.zone }
            .take(3)
        return listOf(here) + others
    }

    /** "+9HRS", "-3HRS", "+5:30HRS" relative to the local zone, iOS style. */
    fun offsetLabel(place: Place): String {
        val now = java.time.Instant.now()
        val diff = ZoneId.of(place.zone).rules.getOffset(now).totalSeconds -
            ZoneId.systemDefault().rules.getOffset(now).totalSeconds
        if (diff == 0) return "Today"
        val hours = diff / 3600
        val minutes = (kotlin.math.abs(diff) % 3600) / 60
        val sign = if (diff > 0) "+" else "-"
        val h = kotlin.math.abs(hours)
        return if (minutes == 0) "$sign${h}HRS" else "$sign$h:${minutes.toString().padStart(2, '0')}HRS"
    }

    fun roundedOffsetHours(place: Place): Int =
        (ZoneId.of(place.zone).rules.getOffset(java.time.Instant.now()).totalSeconds / 3600.0).roundToInt()
}
