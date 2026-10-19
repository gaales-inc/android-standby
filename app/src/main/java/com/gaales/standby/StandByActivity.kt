package com.gaales.standby

import android.app.ActivityOptions
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.OrientationEventListener
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.gaales.standby.data.DeviceInfo
import com.gaales.standby.data.StandByNotice
import com.gaales.standby.data.StandBySettings
import com.gaales.standby.data.StandByState
import com.gaales.standby.service.ChargingMonitorService
import com.gaales.standby.ui.StandByHost
import com.gaales.standby.ui.StandByRoot

/**
 * Full-screen StandBy. Shows over the lock screen, keeps the display on while charging, and
 * leaves when the phone is unplugged or turned upright, like iOS.
 */
class StandByActivity : ComponentActivity(), StandByHost {

    private val settings by lazy { StandBySettings.get(this) }
    private val handler = Handler(Looper.getMainLooper())
    private var auto = false
    private var charging = false
    private var seenLandscape = false
    private var naturalLandscape = false
    private var orientationListener: OrientationEventListener? = null
    private var unlocking = false
    private val exitIfUpright = Runnable { exit() }

    private val powerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_POWER_CONNECTED -> {
                    charging = true
                    updateKeepScreenOn()
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    charging = false
                    if (auto) exit() else updateKeepScreenOn()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StandByState.sessionActive = true
        StandByState.dismissedByUser = false
        auto = intent.getBooleanExtra(EXTRA_AUTO, false)
        charging = DeviceInfo.isCharging(this)
        naturalLandscape = DeviceInfo.isNaturalLandscape(this)

        showOverLockScreen()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        NotificationManagerCompat.from(this).cancel(ChargingMonitorService.LAUNCH_NOTIFICATION_ID)
        ContextCompat.registerReceiver(
            this,
            powerReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        updateKeepScreenOn()
        watchOrientation()
        onBackPressedDispatcher.addCallback(this) { exit() }

        setContent { StandByRoot(host = this) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        auto = auto || intent.getBooleanExtra(EXTRA_AUTO, false)
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
    }

    private fun updateKeepScreenOn() {
        if (settings.alwaysOn && (charging || !auto)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    /** Turning the phone upright leaves StandBy, once it has been seen on its side. */
    private fun watchOrientation() {
        orientationListener = object : OrientationEventListener(this) {
            override fun onOrientationChanged(degrees: Int) {
                if (degrees == ORIENTATION_UNKNOWN) return
                val d = if (naturalLandscape) (degrees + 90) % 360 else degrees
                val upright = d < 35 || d > 325 || d in 145..215
                val sideways = d in 55..125 || d in 235..305
                if (sideways) {
                    seenLandscape = true
                    handler.removeCallbacks(exitIfUpright)
                } else if (upright && seenLandscape) {
                    handler.removeCallbacks(exitIfUpright)
                    handler.postDelayed(exitIfUpright, 900)
                }
            }
        }.also { if (it.canDetectOrientation()) it.enable() }
    }

    override fun onStop() {
        super.onStop()
        // Leaving for the home screen or another app ends the session; the screen turning off does not.
        val interactive = getSystemService(PowerManager::class.java)?.isInteractive ?: true
        if (!isFinishing && !isChangingConfigurations && interactive && !unlocking) {
            StandByState.dismissedByUser = true
            finish()
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        orientationListener?.disable()
        runCatching { unregisterReceiver(powerReceiver) }
        StandByState.sessionActive = false
        super.onDestroy()
    }

    // StandByHost

    override fun exit() {
        if (isFinishing) return
        StandByState.dismissedByUser = true
        finish()
    }

    override fun requestUnlock(onUnlocked: () -> Unit) {
        val keyguard = getSystemService(KeyguardManager::class.java)
        if (keyguard == null || !keyguard.isKeyguardLocked) {
            onUnlocked()
            return
        }
        unlocking = true
        keyguard.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
            override fun onDismissSucceeded() {
                unlocking = false
                onUnlocked()
            }

            override fun onDismissCancelled() {
                unlocking = false
            }

            override fun onDismissError() {
                unlocking = false
            }
        })
    }

    override fun openApp() {
        requestUnlock {
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            exit()
        }
    }

    override fun openNotification(notice: StandByNotice) {
        val pending = notice.contentIntent ?: return
        requestUnlock {
            runCatching {
                val options = ActivityOptions.makeBasic()
                if (Build.VERSION.SDK_INT >= 34) {
                    options.setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                }
                pending.send(this, 0, null, null, null, null, options.toBundle())
            }
            exit()
        }
    }

    override fun onNightModeChanged(night: Boolean) {
        window.attributes = window.attributes.apply {
            screenBrightness = if (night) 0.02f else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        }
    }

    companion object {
        const val EXTRA_AUTO = "com.gaales.standby.AUTO"

        fun intent(context: Context, auto: Boolean): Intent =
            Intent(context, StandByActivity::class.java)
                .putExtra(EXTRA_AUTO, auto)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
    }
}
