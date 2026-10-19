package com.gaales.standby.ui

import android.text.format.DateFormat
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.LongState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaales.standby.R
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

/** iOS system colors (dark appearance). */
object Ios {
    val red = Color(0xFFFF453A)
    val orange = Color(0xFFFF9F0A)
    val yellow = Color(0xFFFFD60A)
    val green = Color(0xFF30D158)
    val mint = Color(0xFF63E6E2)
    val teal = Color(0xFF40C8E0)
    val cyan = Color(0xFF64D2FF)
    val blue = Color(0xFF0A84FF)
    val indigo = Color(0xFF5E5CE6)
    val purple = Color(0xFFBF5AF2)
    val pink = Color(0xFFFF375F)
    val brown = Color(0xFFAC8E68)
    val gray = Color(0xFF8E8E93)
    val gray2 = Color(0xFF636366)
    val gray3 = Color(0xFF48484A)
    val gray4 = Color(0xFF3A3A3C)
    val gray5 = Color(0xFF2C2C2E)
    val gray6 = Color(0xFF1C1C1E)
}

/** Nunito stands in for SF Pro Rounded. */
val Rounded = FontFamily(
    listOf(400, 500, 600, 700, 800, 900).map { w ->
        Font(R.font.nunito, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
    }
)

// ---------------------------------------------------------------------------------------------
// Time

val LocalNow = compositionLocalOf<LongState> { mutableLongStateOf(System.currentTimeMillis()) }

/** A clock that ticks on each second boundary. */
@Composable
fun rememberTicker(): LongState {
    val state = remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            val now = System.currentTimeMillis()
            state.longValue = now
            delay(1000 - now % 1000 + 2)
        }
    }
    return state
}

@Composable
fun nowMs(): Long = LocalNow.current.longValue

@Composable
fun now(): ZonedDateTime = Instant.ofEpochMilli(nowMs()).atZone(ZoneId.systemDefault())

fun Long.atZone(zone: ZoneId = ZoneId.systemDefault()): ZonedDateTime = Instant.ofEpochMilli(this).atZone(zone)

@Composable
fun is24Hour(): Boolean = DateFormat.is24HourFormat(LocalContext.current)

fun hourText(t: ZonedDateTime, h24: Boolean): String =
    if (h24) t.hour.toString().padStart(2, '0') else ((t.hour + 11) % 12 + 1).toString()

fun minuteText(t: ZonedDateTime): String = t.minute.toString().padStart(2, '0')

fun timeText(t: ZonedDateTime, h24: Boolean): String = "${hourText(t, h24)}:${minuteText(t)}"

fun amPm(t: ZonedDateTime): String = if (t.hour < 12) "AM" else "PM"

private val formatters = ConcurrentHashMap<String, DateTimeFormatter>()

/** Formats with a locale-aware best pattern for the skeleton, e.g. "EEEMMMd" → "Mon, Oct 19". */
fun fmt(t: TemporalAccessor, skeleton: String): String {
    val locale = Locale.getDefault()
    val formatter = formatters.getOrPut("$locale|$skeleton") {
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale)
    }
    return formatter.format(t)
}

/** "7:00 AM" or "07:00" depending on the system setting. */
fun shortTime(t: ZonedDateTime, h24: Boolean): String =
    if (h24) timeText(t, true) else "${timeText(t, false)} ${amPm(t)}"

// ---------------------------------------------------------------------------------------------
// Layout helpers

/** Proportional units so a design drawn on a 100-unit grid scales to any widget or screen size. */
@Stable
class Scale(private val unitPx: Float, private val density: Density) {
    fun dp(x: Number): Dp = with(density) { (unitPx * x.toFloat()).toDp() }
    fun sp(x: Number): TextUnit = with(density) { (unitPx * x.toFloat()).toSp() }
    fun px(x: Number): Float = unitPx * x.toFloat()
}

@Composable
fun rememberScale(sizePx: Int, units: Float = 100f): Scale {
    val density = LocalDensity.current
    return remember(sizePx, units, density) { Scale(sizePx / units, density) }
}

private val trimmed = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)

/**
 * Text sized to fill its box. [template] decides the size, so a clock does not jump when
 * "1:11" becomes "10:08". [glyphHeight] is the share of the line the glyphs really use
 * (about 0.75 for digits), letting numerals fill the height instead of the line box.
 */
@Composable
fun FitText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    template: String = text,
    alignment: Alignment = Alignment.Center,
    fill: Float = 1f,
    glyphHeight: Float = 1f,
) {
    val measurer = rememberTextMeasurer(cacheSize = 4)
    BoxWithConstraints(modifier, contentAlignment = alignment) {
        val maxW = constraints.maxWidth.toFloat()
        val maxH = constraints.maxHeight.toFloat()
        val density = LocalDensity.current
        val fontSize = remember(template, style, maxW, maxH, density) {
            val probeStyle = style.copy(fontSize = 100.sp, lineHeight = 100.sp, lineHeightStyle = trimmed)
            val probe = measurer.measure(template, probeStyle, maxLines = 1, softWrap = false)
            val sx = if (maxW.isFinite() && maxW > 0) maxW / probe.size.width else Float.MAX_VALUE
            val sy = if (maxH.isFinite() && maxH > 0) maxH / (probe.size.height * glyphHeight) else Float.MAX_VALUE
            val s = min(sx, sy).takeIf { it != Float.MAX_VALUE } ?: 1f
            (100f * s * fill).sp
        }
        Text(
            text = text,
            style = style.copy(fontSize = fontSize, lineHeight = fontSize, lineHeightStyle = trimmed),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Visible,
            textAlign = TextAlign.Center,
        )
    }
}

/** Red monochrome rendering for Night Mode; [amount] fades between full color (0) and red (1). */
fun Modifier.nightTint(amount: Float): Modifier =
    if (amount <= 0.001f) this else drawWithContent {
        val paint = Paint().apply { colorFilter = ColorFilter.colorMatrix(nightMatrix(amount)) }
        drawIntoCanvas { canvas ->
            canvas.saveLayer(Rect(0f, 0f, size.width, size.height), paint)
            drawContent()
            canvas.restore()
        }
    }

private val identity = floatArrayOf(
    1f, 0f, 0f, 0f, 0f,
    0f, 1f, 0f, 0f, 0f,
    0f, 0f, 1f, 0f, 0f,
    0f, 0f, 0f, 1f, 0f,
)

private val red = floatArrayOf(
    0.40f, 0.70f, 0.16f, 0f, 0f,
    0.03f, 0.05f, 0.01f, 0f, 0f,
    0.02f, 0.03f, 0.01f, 0f, 0f,
    0f, 0f, 0f, 1f, 0f,
)

fun nightMatrix(t: Float): ColorMatrix = ColorMatrix(FloatArray(20) { i -> identity[i] + (red[i] - identity[i]) * t })

/** The edit-mode wobble from the iOS Home Screen. */
@Composable
fun Modifier.jiggle(active: Boolean, seed: Int = 0): Modifier {
    if (!active) return this
    val transition = rememberInfiniteTransition(label = "jiggle")
    val angle by transition.animateFloat(
        initialValue = -0.9f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(140 + (seed % 3) * 18, easing = LinearEasing), RepeatMode.Reverse),
        label = "jiggleAngle",
    )
    return graphicsLayer { rotationZ = angle }
}

/** Smart Stack and clock-style page dots. */
@Composable
fun PageDots(count: Int, current: Int, visible: Boolean, modifier: Modifier = Modifier, vertical: Boolean = true, dot: Dp = 6.dp) {
    val alpha by animateFloatAsState(if (visible && count > 1) 1f else 0f, tween(if (visible) 150 else 600), label = "dots")
    val content: @Composable () -> Unit = {
        repeat(count) { i ->
            Box(
                Modifier
                    .size(dot)
                    .background(if (i == current) Color.White else Color.White.copy(alpha = 0.3f), CircleShape)
            )
        }
    }
    if (vertical) {
        Column(modifier.alpha(alpha), verticalArrangement = Arrangement.spacedBy(dot * 0.9f), horizontalAlignment = Alignment.CenterHorizontally) { content() }
    } else {
        Row(modifier.alpha(alpha), horizontalArrangement = Arrangement.spacedBy(dot * 0.9f), verticalAlignment = Alignment.CenterVertically) { content() }
    }
}
