package com.gaales.standby.data

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.BatteryManager
import android.view.Surface
import android.view.WindowManager

/** Process-wide facts about the StandBy session, shared by the activity, the dream and the monitor service. */
object StandByState {
    /** True while the StandBy activity exists (including while the screen is off behind it). */
    @Volatile var sessionActive = false

    /** True while the screen saver is showing StandBy. */
    @Volatile var dreaming = false

    /** Set when the person leaves StandBy themselves; the monitor waits for a fresh trigger before reopening it. */
    @Volatile var dismissedByUser = false

    val isShowing get() = sessionActive || dreaming
}

object DeviceInfo {
    fun isCharging(context: Context): Boolean {
        val status = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return false
        val plugged = status.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val state = status.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        return plugged != 0 || state == BatteryManager.BATTERY_STATUS_CHARGING || state == BatteryManager.BATTERY_STATUS_FULL
    }

    /** Tablets whose natural orientation is landscape report sensor axes rotated by 90°. */
    @Suppress("DEPRECATION")
    fun isNaturalLandscape(context: Context): Boolean {
        val wm = context.getSystemService(WindowManager::class.java) ?: return false
        val rotation = wm.defaultDisplay.rotation
        val landscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val rotated = rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270
        return landscape != rotated
    }
}
