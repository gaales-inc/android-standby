package com.gaales.standby.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

data class BatteryInfo(val level: Int, val charging: Boolean, val full: Boolean, val timeToFullMs: Long)

private fun Intent.toBatteryInfo(context: Context): BatteryInfo {
    val level = getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
    val status = getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    val plugged = getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
    val charging = plugged != 0 || status == BatteryManager.BATTERY_STATUS_CHARGING
    val timeToFull = if (charging && Build.VERSION.SDK_INT >= 28) {
        context.getSystemService(BatteryManager::class.java)?.computeChargeTimeRemaining() ?: -1
    } else -1
    return BatteryInfo(
        level = if (level >= 0) level * 100 / scale else 0,
        charging = charging,
        full = status == BatteryManager.BATTERY_STATUS_FULL,
        timeToFullMs = timeToFull,
    )
}

@Composable
fun rememberBattery(): BatteryInfo {
    val context = LocalContext.current
    var info by remember {
        mutableStateOf(
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))?.toBatteryInfo(context)
                ?: BatteryInfo(0, charging = false, full = false, timeToFullMs = -1)
        )
    }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                info = intent.toBatteryInfo(c)
            }
        }
        ContextCompat.registerReceiver(
            context, receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
    return info
}

/** "Full in 1 hr 20 min" style remaining-time text. */
fun durationText(ms: Long): String {
    val minutes = (ms / 60_000).toInt()
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h > 0 && m > 0 -> "$h hr $m min"
        h > 0 -> "$h hr"
        else -> "${m.coerceAtLeast(1)} min"
    }
}
