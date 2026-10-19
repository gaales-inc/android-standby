package com.gaales.standby.ui

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.gaales.standby.data.CalendarRepository
import com.gaales.standby.data.MediaRepository
import com.gaales.standby.data.StandBySettings
import com.gaales.standby.data.WeatherRepository
import com.gaales.standby.ui.clocks.ClocksPage
import com.gaales.standby.ui.overlays.ChargingIndicator
import com.gaales.standby.ui.overlays.NotificationOverlay
import com.gaales.standby.ui.photos.PhotosPage
import com.gaales.standby.ui.widgets.WidgetsPage
import kotlinx.coroutines.delay
import java.time.LocalTime
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * StandBy: three views swiped horizontally — Widgets, Photos and Clock — with Night Mode,
 * full-screen notifications and long-press editing.
 */
@Composable
fun StandByRoot(host: StandByHost, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val settings = remember { StandBySettings.get(context) }
    val ticker = rememberTicker()

    val lowLight = rememberLowLight(settings.nightMode && !host.isPreview)
    val night by animateFloatAsState(if (lowLight) 1f else 0f, tween(1800), label = "night")
    LaunchedEffect(lowLight) { host.onNightModeChanged(lowLight) }

    LaunchedEffect(Unit) {
        while (true) {
            WeatherRepository.refresh(context)
            delay(15 * 60_000L)
        }
    }
    var resumes by remember { mutableStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { resumes++ }
    LaunchedEffect(resumes) {
        while (true) {
            CalendarRepository.refresh(context)
            delay(5 * 60_000L)
        }
    }
    DisposableEffect(Unit) {
        MediaRepository.start(context)
        onDispose { MediaRepository.stop() }
    }

    CompositionLocalProvider(LocalNow provides ticker) {
        val shift = rememberBurnInShift(!host.isPreview)
        Box(
            modifier
                .fillMaxSize()
                .background(Color.Black)
                .then(if (host.isPreview) Modifier else Modifier.swipeUpToExit(host::exit))
        ) {
            Box(Modifier.fillMaxSize().nightTint(night)) {
                var editingPage by remember { mutableStateOf<Int?>(null) }
                val pager = rememberPagerState(initialPage = settings.lastPage.coerceIn(0, 2)) { 3 }
                LaunchedEffect(pager.settledPage) { if (!host.isPreview) settings.lastPage = pager.settledPage }
                val haptics = LocalHapticFeedback.current
                val requestEdit: (Int) -> Unit = { page ->
                    if (!host.isPreview) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        host.requestUnlock { editingPage = page }
                    }
                }
                val done = { editingPage = null }
                if (!host.isPreview) BackHandler(enabled = editingPage != null) { editingPage = null }

                HorizontalPager(
                    state = pager,
                    modifier = Modifier.fillMaxSize().offset { shift.value.let { IntOffset(it.x.roundToInt(), it.y.roundToInt()) } },
                    userScrollEnabled = editingPage == null && !host.isPreview,
                    beyondViewportPageCount = 1,
                ) { page ->
                    when (page) {
                        0 -> WidgetsPage(settings, host, editingPage == 0, { requestEdit(0) }, done)
                        1 -> PhotosPage(settings, host, editingPage == 1, { requestEdit(1) }, done)
                        else -> ClocksPage(settings, editingPage == 2, { requestEdit(2) }, done)
                    }
                }
                NotificationOverlay(settings, host)
                ChargingIndicator(host)
            }
        }
    }
}

/** Ambient light below a few lux turns on Night Mode; falls back to the hour when there's no sensor. */
@Composable
private fun rememberLowLight(enabled: Boolean): Boolean {
    val context = LocalContext.current
    var raw by remember { mutableStateOf<Boolean?>(null) }
    var hasSensor by remember { mutableStateOf(true) }
    DisposableEffect(enabled) {
        val sm = context.getSystemService(SensorManager::class.java)
        val sensor = sm?.getDefaultSensor(Sensor.TYPE_LIGHT)
        if (!enabled || sm == null || sensor == null) {
            hasSensor = sensor != null
            raw = false
            return@DisposableEffect onDispose { }
        }
        hasSensor = true
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val lux = event.values[0]
                raw = when {
                    lux < 3f -> true
                    lux > 10f -> false
                    else -> raw ?: false
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        onDispose { sm.unregisterListener(listener) }
    }
    var stable by remember { mutableStateOf(false) }
    LaunchedEffect(enabled, raw, hasSensor) {
        when {
            !enabled -> stable = false
            !hasSensor -> while (true) {
                val hour = LocalTime.now().hour
                stable = hour >= 22 || hour < 6
                delay(60_000)
            }
            else -> {
                delay(if (raw == true) 3000 else 1500)
                stable = raw == true
            }
        }
    }
    return stable
}

/** Nudges everything a few pixels every minute to spare OLED panels, as iOS does. */
@Composable
private fun rememberBurnInShift(enabled: Boolean): Animatable<Offset, *> {
    val density = LocalDensity.current
    val shift = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    LaunchedEffect(enabled) {
        if (!enabled) return@LaunchedEffect
        val r = with(density) { 4.dp.toPx() }
        while (true) {
            delay(60_000)
            shift.animateTo(Offset(Random.nextFloat() * 2 * r - r, Random.nextFloat() * 2 * r - r), tween(4000))
        }
    }
    return shift
}

/** Swipe up from the bottom edge to leave StandBy. Observes touches without taking them from the pages. */
private fun Modifier.swipeUpToExit(onExit: () -> Unit): Modifier = pointerInput(Unit) {
    val threshold = 90.dp.toPx()
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        if (down.position.y < size.height * 0.9f) return@awaitEachGesture
        var dx = 0f
        var dy = 0f
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) break
            val delta = change.positionChangeIgnoreConsumed()
            dx += delta.x
            dy += delta.y
            if (-dy > threshold && abs(dy) > abs(dx) * 1.5f) {
                onExit()
                break
            }
        }
    }
}
