package com.gaales.standby.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

data class CalendarEvent(
    val title: String,
    val begin: Long,
    val end: Long,
    val allDay: Boolean,
    val color: Int,
    val location: String?,
)

/** Upcoming events from the device calendars, for the Up Next widget and Smart Rotate. */
object CalendarRepository {
    var events by mutableStateOf<List<CalendarEvent>>(emptyList())
        private set

    fun hasPermission(context: Context) =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    suspend fun refresh(context: Context) {
        if (!hasPermission(context)) {
            events = emptyList()
            return
        }
        events = withContext(Dispatchers.IO) { runCatching { query(context) }.getOrDefault(emptyList()) }
    }

    /** The next timed event starting within the hour, used by Smart Rotate. */
    fun eventSoon(now: Long): CalendarEvent? =
        events.firstOrNull { !it.allDay && it.begin - now in 0..60 * 60_000L }

    private fun query(context: Context): List<CalendarEvent> {
        val zone = ZoneId.systemDefault()
        val now = System.currentTimeMillis()
        val start = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = start + 3 * 86_400_000L
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, start)
            ContentUris.appendId(it, end)
        }.build()
        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.DISPLAY_COLOR,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.VISIBLE,
        )
        val result = mutableListOf<CalendarEvent>()
        context.contentResolver.query(uri, projection, null, null, "${CalendarContract.Instances.BEGIN} ASC")?.use { c ->
            while (c.moveToNext()) {
                if (c.getInt(6) == 0) continue
                val allDay = c.getInt(3) == 1
                var begin = c.getLong(1)
                var finish = c.getLong(2)
                if (allDay) {
                    // All-day instances are stored at UTC midnight; move them to local midnight.
                    begin = Instant.ofEpochMilli(begin).atZone(ZoneOffset.UTC).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
                    finish = Instant.ofEpochMilli(finish).atZone(ZoneOffset.UTC).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
                }
                if (finish <= now) continue
                result += CalendarEvent(
                    title = c.getString(0)?.takeIf { it.isNotBlank() } ?: "New Event",
                    begin = begin,
                    end = finish,
                    allDay = allDay,
                    color = c.getInt(4),
                    location = c.getString(5)?.takeIf { it.isNotBlank() },
                )
            }
        }
        return result.sortedBy { it.begin }
    }
}
