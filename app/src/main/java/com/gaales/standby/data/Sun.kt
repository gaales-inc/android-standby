package com.gaales.standby.data

import java.time.Instant
import java.time.ZoneId
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** Low-precision solar ephemeris (good to a fraction of a degree), enough for clock faces. */
object Sun {
    private const val J2000_MS = 946_728_000_000.0

    /** Latitude and longitude, in degrees, of the point where the sun is directly overhead. */
    fun subsolarPoint(epochMs: Long): Pair<Double, Double> {
        val d = (epochMs - J2000_MS) / 86_400_000.0
        val g = Math.toRadians(norm360(357.529 + 0.98560028 * d))
        val q = norm360(280.459 + 0.98564736 * d)
        val l = Math.toRadians(norm360(q + 1.915 * sin(g) + 0.020 * sin(2 * g)))
        val e = Math.toRadians(23.439 - 0.00000036 * d)
        val declination = Math.toDegrees(asin(sin(e) * sin(l)))
        val rightAscension = Math.toDegrees(atan2(cos(e) * sin(l), cos(l)))
        val gmstDegrees = norm360((18.697374558 + 24.06570982441908 * d) * 15.0)
        var lon = rightAscension - gmstDegrees
        while (lon < -180) lon += 360
        while (lon > 180) lon -= 360
        return declination to lon
    }

    /** Cosine of the solar zenith angle: > 0 in daylight, < 0 at night. */
    fun cosZenith(lat: Double, lon: Double, subsolar: Pair<Double, Double>): Double {
        val phi = Math.toRadians(lat)
        val dec = Math.toRadians(subsolar.first)
        val h = Math.toRadians(lon - subsolar.second)
        return sin(phi) * sin(dec) + cos(phi) * cos(dec) * cos(h)
    }

    /** Sun elevation above the horizon in degrees. */
    fun elevation(lat: Double, lon: Double, epochMs: Long): Double =
        90.0 - Math.toDegrees(acos(cosZenith(lat, lon, subsolarPoint(epochMs)).coerceIn(-1.0, 1.0)))

    data class Day(val sunrise: Long, val sunset: Long)

    /** Sunrise and sunset around [epochMs] for a place, found by scanning the local day. Null in polar day/night. */
    fun day(lat: Double, lon: Double, epochMs: Long, zone: ZoneId = ZoneId.systemDefault()): Day? {
        val start = Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
        val step = 5 * 60_000L
        var rise: Long? = null
        var set: Long? = null
        var prev = elevation(lat, lon, start) > HORIZON
        var t = start + step
        while (t <= start + 86_400_000L) {
            val up = elevation(lat, lon, t) > HORIZON
            if (up && !prev && rise == null) rise = t
            if (!up && prev && set == null) set = t
            prev = up
            t += step
        }
        return if (rise != null && set != null && set > rise) Day(rise, set) else null
    }

    private const val HORIZON = -0.833

    private fun norm360(x: Double): Double {
        val r = x % 360.0
        return if (r < 0) r + 360.0 else r
    }
}
