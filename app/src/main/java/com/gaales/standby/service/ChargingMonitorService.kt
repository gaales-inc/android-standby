package com.gaales.standby.service

import android.Manifest
import android.app.KeyguardManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.gaales.standby.MainActivity
import com.gaales.standby.R
import com.gaales.standby.StandByActivity
import com.gaales.standby.data.DeviceInfo
import com.gaales.standby.data.StandBySettings
import com.gaales.standby.data.StandByState
import kotlin.math.abs

/**
 * Opens StandBy the way iOS does: when the phone is charging, locked, and resting on its side.
 *
 * Runs as a small foreground service so it can hear the charger connect (that broadcast can no
 * longer be received from the manifest) and read the accelerometer while charging.
 */
class ChargingMonitorService : Service(), SensorEventListener {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var sensorManager: SensorManager
    private lateinit var keyguard: KeyguardManager
    private lateinit var power: PowerManager
    private var accelerometer: Sensor? = null
    private var sensorOn = false

    private val gravity = FloatArray(3)
    private var haveGravity = false
    private var charging = false
    private var landscape = false
    private var landscapeSince = 0L
    private var naturalLandscape = false

    /** Cleared after we open StandBy or the person dismisses it; set again by a fresh trigger. */
    private var armed = true

    private val evaluateRunnable = Runnable { evaluate() }
    private var wakeLock: PowerManager.WakeLock? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_POWER_CONNECTED -> {
                    charging = true
                    rearm()
                    holdAwake()
                    startSensor()
                    schedule(1500)
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    charging = false
                    stopSensor()
                }
                Intent.ACTION_SCREEN_OFF -> {
                    // Locking the phone is a fresh request for StandBy, as on iOS.
                    if (!StandByState.isShowing) rearm()
                    if (charging) holdAwake()
                    schedule(400)
                }
                Intent.ACTION_SCREEN_ON -> schedule(400)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannels(this)
        startInForeground()
        sensorManager = getSystemService(SensorManager::class.java)
        keyguard = getSystemService(KeyguardManager::class.java)
        power = getSystemService(PowerManager::class.java)
        wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AndroidStandby:orientation").apply { setReferenceCounted(false) }
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        naturalLandscape = DeviceInfo.isNaturalLandscape(this)
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        charging = DeviceInfo.isCharging(this)
        if (charging) startSensor()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!StandBySettings.get(this).enabled) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        wakeLock?.takeIf { it.isHeld }?.release()
        stopSensor()
        runCatching { unregisterReceiver(receiver) }
        super.onDestroy()
    }

    private fun startInForeground() {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val now = PendingIntent.getActivity(
            this, 2, StandByActivity.intent(this, auto = false),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_SERVICE)
            .setSmallIcon(R.drawable.ic_stat_standby)
            .setContentTitle(getString(R.string.notif_ready_title))
            .setContentText(getString(R.string.notif_ready_text))
            .setContentIntent(open)
            .addAction(0, "Open StandBy", now)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    /** Keeps the CPU up for a few seconds so orientation can be read while the screen is off. */
    private fun holdAwake() {
        wakeLock?.acquire(WAKE_MS)
    }

    private fun startSensor() {
        val sensor = accelerometer ?: return
        if (sensorOn) return
        haveGravity = false
        sensorOn = sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL, handler)
    }

    private fun stopSensor() {
        if (!sensorOn) return
        sensorManager.unregisterListener(this)
        sensorOn = false
        landscape = false
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onSensorChanged(event: SensorEvent) {
        if (!haveGravity) {
            event.values.copyInto(gravity, endIndex = 3)
            haveGravity = true
        } else {
            for (i in 0..2) gravity[i] = gravity[i] * 0.8f + event.values[i] * 0.2f
        }
        var x = gravity[0]
        var y = gravity[1]
        val z = gravity[2]
        if (naturalLandscape) {
            val t = x
            x = y
            y = t
        }
        // On its side: gravity mostly along the long axis of the screen's width, and not lying flat.
        val isLandscape = abs(x) > 5.5f && abs(x) > abs(y) * 1.4f && abs(z) < 8.2f
        val isPortraitOrFlat = (abs(y) > 5.5f && abs(y) > abs(x) * 1.4f) || abs(z) > 9.0f
        if (isLandscape && !landscape) {
            landscape = true
            landscapeSince = SystemClock.elapsedRealtime()
            schedule(STABLE_MS + 100)
        } else if (landscape && isPortraitOrFlat) {
            landscape = false
            // Turning the phone upright and back is a new request for StandBy.
            if (!StandByState.isShowing) rearm()
        }
    }

    private fun rearm() {
        armed = true
        StandByState.dismissedByUser = false
    }

    private fun schedule(delayMs: Long) {
        handler.removeCallbacks(evaluateRunnable)
        handler.postDelayed(evaluateRunnable, delayMs)
    }

    private fun evaluate() {
        val settings = StandBySettings.get(this)
        if (!settings.enabled || !charging || !landscape || !armed) return
        if (StandByState.isShowing || StandByState.dismissedByUser) return
        val stableFor = SystemClock.elapsedRealtime() - landscapeSince
        if (stableFor < STABLE_MS) {
            schedule(STABLE_MS - stableFor + 50)
            return
        }
        val locked = keyguard.isKeyguardLocked || !power.isInteractive
        if (!locked) return
        armed = false
        launch()
    }

    private fun launch() {
        val intent = StandByActivity.intent(this, auto = true)
        val canStartDirectly = Build.VERSION.SDK_INT < 29 || Settings.canDrawOverlays(this)
        if (canStartDirectly) {
            try {
                startActivity(intent)
                return
            } catch (e: Exception) {
                Log.w(TAG, "Direct launch refused, falling back to a full-screen notification", e)
            }
        }
        postFullScreenLaunch(intent)
    }

    private fun postFullScreenLaunch(intent: Intent) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val pending = PendingIntent.getActivity(
            this, 1, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_LAUNCH)
            .setSmallIcon(R.drawable.ic_stat_standby)
            .setContentTitle(getString(R.string.notif_launch_title))
            .setContentText(getString(R.string.notif_launch_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pending, true)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setTimeoutAfter(60_000)
            .setSilent(true)
            .build()
        NotificationManagerCompat.from(this).notify(LAUNCH_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val TAG = "StandByMonitor"
        private const val STABLE_MS = 1200L
        private const val WAKE_MS = 5000L
        const val NOTIFICATION_ID = 7
        const val LAUNCH_NOTIFICATION_ID = 8
        const val CHANNEL_SERVICE = "standby_service"
        const val CHANNEL_LAUNCH = "standby_launch"

        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, ChargingMonitorService::class.java))
            } catch (e: Exception) {
                // Starting from the background is not allowed on Android 12+; the app or boot will start it later.
                Log.w(TAG, "Could not start the StandBy monitor", e)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ChargingMonitorService::class.java))
        }

        fun createChannels(context: Context) {
            if (Build.VERSION.SDK_INT < 26) return
            val nm = context.getSystemService(NotificationManager::class.java) ?: return
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_SERVICE, context.getString(R.string.channel_service), NotificationManager.IMPORTANCE_MIN).apply {
                    setShowBadge(false)
                }
            )
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_LAUNCH, context.getString(R.string.channel_launch), NotificationManager.IMPORTANCE_HIGH).apply {
                    setShowBadge(false)
                    setSound(null, null)
                    enableVibration(false)
                }
            )
        }
    }
}
