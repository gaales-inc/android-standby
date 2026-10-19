package com.gaales.standby

import android.Manifest
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.gaales.standby.data.MediaRepository
import com.gaales.standby.data.NotificationBus
import com.gaales.standby.data.PhotoRepository
import com.gaales.standby.data.StandBySettings
import com.gaales.standby.data.WeatherRepository
import com.gaales.standby.service.ChargingMonitorService
import com.gaales.standby.service.StandByNotificationListener
import com.gaales.standby.ui.settings.AccessItem
import com.gaales.standby.ui.settings.DarkIos
import com.gaales.standby.ui.settings.LightIos
import com.gaales.standby.ui.settings.LocalIos
import com.gaales.standby.ui.settings.SettingsScreen
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/** Settings › StandBy, plus the Android permissions StandBy needs to behave like iOS. */
class MainActivity : ComponentActivity() {

    private val settings by lazy { StandBySettings.get(this) }
    private var refresh by mutableIntStateOf(0)

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        refresh++
        if (results[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            MainScope().launch { WeatherRepository.refresh(this@MainActivity, force = true) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (settings.enabled) ChargingMonitorService.start(this)
        if (!settings.onboarded) {
            settings.onboarded = true
            if (Build.VERSION.SDK_INT >= 33) request(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val dark = isSystemInDarkTheme()
            val autoStart = remember(refresh) { autoStartItems() }
            val widgetAccess = remember(refresh) { widgetItems() }
            MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                CompositionLocalProvider(LocalIos provides if (dark) DarkIos else LightIos) {
                    SettingsScreen(
                        settings = settings,
                        autoStart = autoStart,
                        widgetAccess = widgetAccess,
                        onEnabledChange = { on ->
                            settings.enabled = on
                            if (on) ChargingMonitorService.start(this) else ChargingMonitorService.stop(this)
                        },
                        onPreview = { startActivity(StandByActivity.intent(this, auto = false)) },
                        onScreenSaver = { open(Intent(Settings.ACTION_DREAM_SETTINGS)) },
                        canOpenAutomatically = remember(refresh) { canOpenAutomatically() },
                        onSetUp = { open(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, packageUri())) },
                        version = BuildConfigVersion.name(this),
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refresh++
        MediaRepository.onAccessChanged()
    }

    /**
     * Overlay access is what reliably lets StandBy open itself over the lock screen. Recent Android
     * versions refuse full-screen notifications from apps that aren't alarms or calls, even when
     * canUseFullScreenIntent() says yes, so that path is only a fallback.
     */
    private fun canOpenAutomatically(): Boolean = Build.VERSION.SDK_INT < 29 || Settings.canDrawOverlays(this)

    private fun autoStartItems(): List<AccessItem> = buildList {
        add(
            AccessItem("Display Over Other Apps", Build.VERSION.SDK_INT < 29 || Settings.canDrawOverlays(this@MainActivity)) {
                open(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, packageUri()))
            }
        )
        if (Build.VERSION.SDK_INT >= 33) {
            add(AccessItem("Notifications", granted(Manifest.permission.POST_NOTIFICATIONS)) { request(Manifest.permission.POST_NOTIFICATIONS) })
        }
        if (Build.VERSION.SDK_INT >= 34) {
            val nm = getSystemService(NotificationManager::class.java)
            add(AccessItem("Full-Screen Alerts", nm?.canUseFullScreenIntent() == true) {
                open(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, packageUri()))
            })
        }
        val pm = getSystemService(PowerManager::class.java)
        add(AccessItem("Unrestricted Battery Use", pm?.isIgnoringBatteryOptimizations(packageName) == true) {
            @Suppress("BatteryLife")
            open(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri()))
        })
    }

    private fun widgetItems(): List<AccessItem> = listOf(
        AccessItem("Notifications & Music", NotificationBus.isAccessGranted(this)) {
            val component = ComponentName(this, StandByNotificationListener::class.java)
            val detail = if (Build.VERSION.SDK_INT >= 30) {
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                    .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, component.flattenToString())
            } else null
            if (detail == null || !open(detail)) open(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        },
        AccessItem("Photos", PhotoRepository.hasPermission(this)) { request(*PhotoRepository.permissions()) },
        AccessItem("Calendars", granted(Manifest.permission.READ_CALENDAR)) { request(Manifest.permission.READ_CALENDAR) },
        AccessItem("Location for Weather", granted(Manifest.permission.ACCESS_COARSE_LOCATION)) { request(Manifest.permission.ACCESS_COARSE_LOCATION) },
    )

    private fun granted(permission: String) =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    /** Asks for permissions, or opens App info when Android will no longer show the prompt. */
    private fun request(vararg permissions: String) {
        val prefs = getSharedPreferences("permission_asks", MODE_PRIVATE)
        val missing = permissions.filterNot { granted(it) }
        if (missing.isEmpty()) {
            open(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri()))
            return
        }
        val blocked = missing.all { prefs.getBoolean(it, false) && !shouldShowRequestPermissionRationale(it) }
        if (blocked) {
            open(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri()))
        } else {
            prefs.edit { missing.forEach { putBoolean(it, true) } }
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun packageUri() = Uri.parse("package:$packageName")

    private fun open(intent: Intent): Boolean = try {
        startActivity(intent)
        true
    } catch (_: Exception) {
        false
    }
}

private object BuildConfigVersion {
    fun name(activity: ComponentActivity): String = runCatching {
        activity.packageManager.getPackageInfo(activity.packageName, 0).versionName
    }.getOrNull() ?: ""
}
