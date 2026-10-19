package com.gaales.standby.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.edit
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/** Widgets that can live in a Smart Stack. */
enum class WidgetKind(val title: String, val subtitle: String) {
    CLOCK("Clock", "See the time on an analog clock."),
    DIGITAL_CLOCK("Digital Clock", "See the time in big, bold numbers."),
    CALENDAR("Calendar", "See the whole month at a glance."),
    UP_NEXT("Up Next", "See what's next on your calendar."),
    WEATHER("Weather", "See current conditions and the forecast."),
    BATTERIES("Batteries", "Check the battery level of this phone."),
    ALARM("Alarm", "See your next alarm."),
    WORLD_CLOCK("World Clock", "See the time in cities around the world."),
    MUSIC("Music", "See and control what's playing."),
    PHOTOS("Photos", "See your favorite photos."),
}

/** Full-screen clock faces on the third StandBy page. */
enum class ClockStyle(val title: String) {
    ANALOG("Analog"),
    DIGITAL("Digital"),
    WORLD("World"),
    SOLAR("Solar"),
    FLOAT("Float"),
}

/**
 * User preferences, mirrored into Compose state so every screen updates the moment a value changes.
 * One instance per process; the activity, the screen saver and the background service all share it.
 */
class StandBySettings private constructor(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("standby", Context.MODE_PRIVATE)

    // Settings > StandBy
    var enabled by bool("enabled", true)
    var alwaysOn by bool("always_on", true)
    var nightMode by bool("night_mode", true)
    var showNotifications by bool("show_notifications", true)
    var previewOnTap by bool("preview_on_tap", false)

    // Remembered StandBy state
    var lastPage by int("last_page", 0)
    var clockStyle by int("clock_style", 0)
    var leftStack by widgets("left_stack", listOf(WidgetKind.CLOCK, WidgetKind.WORLD_CLOCK, WidgetKind.DIGITAL_CLOCK, WidgetKind.ALARM))
    var rightStack by widgets("right_stack", listOf(WidgetKind.CALENDAR, WidgetKind.UP_NEXT, WidgetKind.WEATHER, WidgetKind.BATTERIES, WidgetKind.MUSIC, WidgetKind.PHOTOS))
    var leftSmartRotate by bool("left_smart_rotate", true)
    var rightSmartRotate by bool("right_smart_rotate", true)
    var hiddenAlbums by stringSet("hidden_albums", emptySet())
    var onboarded by bool("onboarded", false)

    private val clockColorStates = ClockStyle.entries.associateWith { style ->
        mutableStateOf(prefs.getInt("clock_color_${style.name}", 0))
    }

    fun clockColor(style: ClockStyle): Int = clockColorStates.getValue(style).value

    fun setClockColor(style: ClockStyle, index: Int) {
        clockColorStates.getValue(style).value = index
        prefs.edit { putInt("clock_color_${style.name}", index) }
    }

    private fun bool(key: String, default: Boolean) = object : ReadWriteProperty<Any?, Boolean> {
        val state = mutableStateOf(prefs.getBoolean(key, default))
        override fun getValue(thisRef: Any?, property: KProperty<*>) = state.value
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
            state.value = value
            prefs.edit { putBoolean(key, value) }
        }
    }

    private fun int(key: String, default: Int) = object : ReadWriteProperty<Any?, Int> {
        val state = mutableStateOf(prefs.getInt(key, default))
        override fun getValue(thisRef: Any?, property: KProperty<*>) = state.value
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
            state.value = value
            prefs.edit { putInt(key, value) }
        }
    }

    private fun stringSet(key: String, default: Set<String>) = object : ReadWriteProperty<Any?, Set<String>> {
        val state = mutableStateOf(prefs.getStringSet(key, default)?.toSet() ?: default)
        override fun getValue(thisRef: Any?, property: KProperty<*>) = state.value
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Set<String>) {
            state.value = value
            prefs.edit { putStringSet(key, value) }
        }
    }

    private fun widgets(key: String, default: List<WidgetKind>) = object : ReadWriteProperty<Any?, List<WidgetKind>> {
        val state = mutableStateOf(
            prefs.getString(key, null)?.let { saved ->
                saved.split(',').filter { it.isNotBlank() }.mapNotNull { name -> WidgetKind.entries.find { it.name == name } }
            } ?: default
        )
        override fun getValue(thisRef: Any?, property: KProperty<*>) = state.value
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: List<WidgetKind>) {
            state.value = value
            prefs.edit { putString(key, value.joinToString(",") { it.name }) }
        }
    }

    companion object {
        @Volatile private var instance: StandBySettings? = null

        fun get(context: Context): StandBySettings =
            instance ?: synchronized(this) { instance ?: StandBySettings(context).also { instance = it } }
    }
}
