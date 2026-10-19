package com.gaales.standby.ui.clocks

import android.app.AlarmManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import com.gaales.standby.data.ClockStyle
import com.gaales.standby.data.Places
import com.gaales.standby.data.Sun
import com.gaales.standby.data.WeatherRepository
import com.gaales.standby.data.WorldMapData
import com.gaales.standby.ui.AlarmGlyph
import com.gaales.standby.ui.FitText
import com.gaales.standby.ui.Ios
import com.gaales.standby.ui.LocalNow
import com.gaales.standby.ui.Rounded
import com.gaales.standby.ui.WeatherGlyph
import com.gaales.standby.ui.atZone
import com.gaales.standby.ui.fmt
import com.gaales.standby.ui.hourText
import com.gaales.standby.ui.minuteText
import com.gaales.standby.ui.now
import com.gaales.standby.ui.shortTime
import com.gaales.standby.ui.timeText
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun ClockFace(style: ClockStyle, palette: ClockPalette, h24: Boolean, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize()) {
        when (style) {
            ClockStyle.ANALOG -> AnalogFace(palette)
            ClockStyle.DIGITAL -> DigitalFace(palette, h24)
            ClockStyle.WORLD -> WorldFace(h24)
            ClockStyle.SOLAR -> SolarFace(palette, h24)
            ClockStyle.FLOAT -> FloatFace(palette, h24)
        }
    }
}

private val trimmed = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)

@Composable
private fun Dp.asSp(): TextUnit = with(LocalDensity.current) { this@asSp.toSp() }

/** Wall-clock milliseconds refreshed at ~30 fps, read only while drawing so nothing recomposes. */
@Composable
private fun rememberFrameMillis(): androidx.compose.runtime.LongState {
    val state = remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis {
                val now = System.currentTimeMillis()
                if (now - state.longValue >= 33) state.longValue = now
            }
        }
    }
    return state
}

/** Today's date as a state that changes once a day, so faces don't recompose every second. */
@Composable
private fun rememberDate(): java.time.LocalDate {
    val nowState = LocalNow.current
    val date by remember(nowState) { derivedStateOf { nowState.longValue.atZone().toLocalDate() } }
    return date
}

@Composable
private fun nextAlarm(): Long? {
    val context = LocalContext.current
    val minute = now().minute
    return remember(minute) { context.getSystemService(AlarmManager::class.java)?.nextAlarmClock?.triggerTime }
}

// ---------------------------------------------------------------------------------------------
// Analog: a large dial with a sweeping second hand and the weekday in the accent color.

@Composable
private fun AnalogFace(palette: ClockPalette) {
    val measurer = rememberTextMeasurer()
    val frame = rememberFrameMillis()
    val date = rememberDate()
    val weekday = fmt(date, "EEE").uppercase()
    val accent = palette.primary
    Box(
        Modifier
            .fillMaxSize()
            .drawWithCache {
                val r = min(size.height * 0.46f, size.width * 0.3f)
                val numeralStyle = TextStyle(color = Color.White, fontFamily = Rounded, fontWeight = FontWeight.SemiBold, fontSize = (r * 0.2f).toSp())
                val numerals = (1..12).map { measurer.measure(it.toString(), numeralStyle) }
                val complication = measurer.measure(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = accent)) { append(weekday) }
                        append(" ")
                        withStyle(SpanStyle(color = Color.White)) { append(date.dayOfMonth.toString()) }
                    },
                    TextStyle(fontFamily = Rounded, fontWeight = FontWeight.Bold, fontSize = (r * 0.1f).toSp()),
                )
                onDrawBehind {
                    val c = center
                    for (i in 0 until 60) {
                        val a = Math.toRadians(i * 6.0 - 90)
                        val major = i % 5 == 0
                        val outer = r
                        val inner = if (major) r * 0.9f else r * 0.94f
                        drawLine(
                            if (major) Color.White.copy(alpha = 0.85f) else Ios.gray3,
                            Offset(c.x + outer * cos(a).toFloat(), c.y + outer * sin(a).toFloat()),
                            Offset(c.x + inner * cos(a).toFloat(), c.y + inner * sin(a).toFloat()),
                            if (major) r * 0.016f else r * 0.009f,
                            StrokeCap.Round,
                        )
                    }
                    numerals.forEachIndexed { i, layout ->
                        val a = Math.toRadians((i + 1) * 30.0 - 90)
                        val rr = r * 0.74f
                        drawText(layout, topLeft = Offset(c.x + rr * cos(a).toFloat() - layout.size.width / 2f, c.y + rr * sin(a).toFloat() - layout.size.height / 2f))
                    }
                    drawText(complication, topLeft = Offset(c.x + r * 0.42f - complication.size.width / 2f, c.y - complication.size.height / 2f))

                    val t = frame.longValue.atZone()
                    val secs = t.second + t.nano / 1e9f
                    val minutes = t.minute + secs / 60f
                    val hours = (t.hour % 12) + minutes / 60f
                    bigHand(c, hours * 30f, r * 0.5f, r * 0.07f, r)
                    bigHand(c, minutes * 6f, r * 0.84f, r * 0.055f, r)
                    drawCircle(Color.White, r * 0.05f, c)
                    rotate(secs * 6f, c) {
                        drawLine(accent, Offset(c.x, c.y + r * 0.16f), Offset(c.x, c.y - r * 0.92f), r * 0.016f, StrokeCap.Round)
                    }
                    drawCircle(accent, r * 0.036f, c)
                    drawCircle(Color.Black, r * 0.014f, c)
                }
            }
    )
}

private fun DrawScope.bigHand(c: Offset, angle: Float, length: Float, width: Float, r: Float) {
    rotate(angle, c) {
        drawLine(Color.White, c, Offset(c.x, c.y - r * 0.13f), width * 0.38f, StrokeCap.Round)
        drawLine(Color.White, Offset(c.x, c.y - r * 0.13f), Offset(c.x, c.y - length), width, StrokeCap.Round)
    }
}

// ---------------------------------------------------------------------------------------------
// Digital: enormous rounded numerals with the date, weather and next alarm above them.

@Composable
private fun DigitalFace(palette: ClockPalette, h24: Boolean) {
    val t = now()
    val weather = WeatherRepository.weather
    val alarm = nextAlarm()
    val text = timeText(t, h24)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val h = maxHeight
        val w = maxWidth
        val info = (h * 0.075f).asSp()
        Column(Modifier.fillMaxSize().padding(horizontal = w * 0.045f, vertical = h * 0.06f)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(fmt(t, "EEEMMMd"), color = palette.secondary, fontSize = info, fontWeight = FontWeight.Bold, fontFamily = Rounded)
                Spacer(Modifier.weight(1f))
                if (weather != null) {
                    WeatherGlyph(weather.code, weather.isDay, Modifier.size(h * 0.085f))
                    Spacer(Modifier.width(h * 0.015f))
                    Text("${weather.temp}°", color = palette.secondary, fontSize = info, fontWeight = FontWeight.Bold, fontFamily = Rounded)
                }
                if (alarm != null) {
                    Spacer(Modifier.width(h * 0.05f))
                    AlarmGlyph(Modifier.size(h * 0.07f), palette.secondary)
                    Spacer(Modifier.width(h * 0.015f))
                    Text(shortTime(alarm.atZone(), h24), color = palette.secondary, fontSize = info, fontWeight = FontWeight.Bold, fontFamily = Rounded)
                }
            }
            FitText(
                text = text,
                style = TextStyle(color = palette.primary, fontFamily = Rounded, fontWeight = FontWeight.Bold, letterSpacing = (-0.02).em()),
                template = text.map { if (it.isDigit()) '0' else it }.joinToString(""),
                glyphHeight = 0.74f,
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
        }
    }
}

private fun Double.em() = androidx.compose.ui.unit.TextUnit(this.toFloat(), androidx.compose.ui.unit.TextUnitType.Em)

// ---------------------------------------------------------------------------------------------
// World: a dotted world map lit by the sun, with your place marked.

@Composable
private fun WorldFace(h24: Boolean) {
    val t = now()
    val place = remember { Places.local() }
    val nowState = LocalNow.current
    val minute by remember(nowState) { derivedStateOf { nowState.longValue / 60_000 } }
    val pulse = rememberInfiniteTransition(label = "pulse").animateFloat(
        0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "pulseValue",
    )
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val h = maxHeight
        Box(
            Modifier
                .fillMaxSize()
                .drawWithCache {
                    val cols = WorldMapData.COLS
                    val rows = WorldMapData.ROWS
                    val spacing = min(size.width * 0.98f / cols, size.height * 0.9f / rows)
                    val ox = (size.width - spacing * cols) / 2f + spacing / 2f
                    val oy = (size.height - spacing * rows) / 2f + spacing * 0.75f
                    val latStep = (WorldMapData.LAT_TOP - WorldMapData.LAT_BOTTOM) / rows
                    val sub = Sun.subsolarPoint(minute * 60_000)
                    val buckets = Array(7) { mutableListOf<Offset>() }
                    for (r in 0 until rows) {
                        val row = WorldMapData.rows[r]
                        val lat = WorldMapData.LAT_TOP - (r + 0.5) * latStep
                        for (c in 0 until cols) {
                            if (row[c] != '#') continue
                            val lon = -180.0 + (c + 0.5) * 360.0 / cols
                            val z = Sun.cosZenith(lat, lon, sub)
                            val light = ((z + 0.12) / 0.24).coerceIn(0.0, 1.0)
                            buckets[(light * 6).toInt()] += Offset(ox + c * spacing, oy + r * spacing)
                        }
                    }
                    val night = Color(0xFF2C2C2E)
                    val day = Color(0xFFE5E5EA)
                    val markerX = ox + ((place.lon + 180.0) / 360.0 * cols - 0.5).toFloat() * spacing
                    val markerY = oy + ((WorldMapData.LAT_TOP - place.lat) / latStep - 0.5).toFloat() * spacing
                    onDrawBehind {
                        buckets.forEachIndexed { i, points ->
                            if (points.isNotEmpty()) {
                                drawPoints(points, PointMode.Points, lerp(night, day, i / 6f), strokeWidth = spacing * 0.62f, cap = StrokeCap.Round)
                            }
                        }
                        val p = pulse.value
                        drawCircle(Ios.yellow.copy(alpha = (1f - p) * 0.6f), spacing * (0.8f + p * 2.6f), Offset(markerX, markerY), style = Stroke(spacing * 0.22f))
                        drawCircle(Color.Black, spacing * 0.95f, Offset(markerX, markerY))
                        drawCircle(Ios.yellow, spacing * 0.62f, Offset(markerX, markerY))
                    }
                }
        )
        Column(Modifier.align(Alignment.TopStart).padding(start = maxWidth * 0.05f, top = h * 0.07f)) {
            Text(
                timeText(t, h24),
                color = Color.White,
                fontSize = (h * 0.24f).asSp(),
                fontWeight = FontWeight.SemiBold,
                style = TextStyle(lineHeight = (h * 0.24f).asSp(), lineHeightStyle = trimmed, shadow = Shadow(Color.Black, blurRadius = 24f)),
            )
            Spacer(Modifier.size(h * 0.02f))
            Text(place.city, color = Color.White, fontSize = (h * 0.065f).asSp(), fontWeight = FontWeight.SemiBold, style = TextStyle(shadow = Shadow(Color.Black, blurRadius = 16f)))
            Text(fmt(t, "EEEEMMMMd"), color = Ios.gray, fontSize = (h * 0.05f).asSp(), fontWeight = FontWeight.Medium, style = TextStyle(shadow = Shadow(Color.Black, blurRadius = 16f)))
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Solar: a glowing sun that crosses the screen from sunrise to sunset, with thin numerals.

@Composable
private fun SolarFace(palette: ClockPalette, h24: Boolean) {
    val t = now()
    val place = remember { Places.local() }
    val date = t.toLocalDate()
    val ms = t.toInstant().toEpochMilli()
    val sun = remember(date) { Sun.day(place.lat, place.lon, ms, ZoneId.systemDefault()) }
    val (isDay, progress) = remember(ms / 60_000, sun) {
        if (sun == null) {
            (Sun.elevation(place.lat, place.lon, ms) > 0) to 0.5f
        } else when {
            ms in sun.sunrise..sun.sunset -> true to ((ms - sun.sunrise).toFloat() / (sun.sunset - sun.sunrise))
            ms > sun.sunset -> false to ((ms - sun.sunset).toFloat() / (sun.sunrise + 86_400_000L - sun.sunset)).coerceIn(0f, 1f)
            else -> false to ((ms - (sun.sunset - 86_400_000L)).toFloat() / (sun.sunrise - (sun.sunset - 86_400_000L))).coerceIn(0f, 1f)
        }
    }
    val breathe = rememberInfiniteTransition(label = "breathe").animateFloat(
        0.94f, 1.06f, infiniteRepeatable(tween(5000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "breatheValue",
    )
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val h = maxHeight
        val w = maxWidth
        Box(
            Modifier
                .fillMaxSize()
                .drawWithCache {
                    val wpx = size.width
                    val hpx = size.height
                    val x = if (isDay) wpx * (0.2f + 0.7f * progress) else wpx * (0.9f - 0.7f * progress)
                    val y = if (isDay) hpx * (1.0f - 0.62f * sin(PI * progress).toFloat()) else hpx * (1.12f - 0.1f * sin(PI * progress).toFloat())
                    val strength = if (isDay) 1f else 0.4f
                    val center = Offset(x, y)
                    onDrawBehind {
                        val b = breathe.value
                        val halo = hpx * 1.15f * b
                        drawCircle(Brush.radialGradient(listOf(palette.tertiary.copy(alpha = 0.5f * strength), Color.Transparent), center, halo), halo, center)
                        val glow = hpx * 0.55f * b
                        drawCircle(Brush.radialGradient(listOf(palette.secondary.copy(alpha = 0.9f * strength), palette.secondary.copy(alpha = 0.25f * strength), Color.Transparent), center, glow), glow, center)
                        val core = hpx * (if (isDay) 0.17f else 0.1f)
                        drawCircle(Brush.radialGradient(listOf(palette.primary.copy(alpha = strength), palette.secondary.copy(alpha = 0.7f * strength), Color.Transparent), center, core), core, center)
                    }
                }
        )
        Column(Modifier.align(Alignment.CenterStart).padding(start = w * 0.06f)) {
            Text(fmt(t, "EEEEMMMMd"), color = Color.White.copy(alpha = 0.8f), fontSize = (h * 0.06f).asSp(), fontWeight = FontWeight.Normal)
            Spacer(Modifier.size(h * 0.02f))
            Text(
                timeText(t, h24),
                color = Color.White,
                style = TextStyle(
                    fontSize = (h * 0.44f).asSp(),
                    lineHeight = (h * 0.44f).asSp(),
                    lineHeightStyle = trimmed,
                    fontWeight = FontWeight.Thin,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = (-0.02).em(),
                ),
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Float: big bubbly digits that bob gently and pop when they change.

@Composable
private fun FloatFace(palette: ClockPalette, h24: Boolean) {
    val t = now()
    val hours = hourText(t, h24)
    val digits = hours + minuteText(t)
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val byHeight = maxHeight * 0.8f
        val byWidth = maxWidth / (digits.length * 0.6f + 0.3f)
        val size = if (byHeight < byWidth) byHeight else byWidth
        val fontSize = size.asSp()
        Row(horizontalArrangement = Arrangement.spacedBy(-size * 0.06f), verticalAlignment = Alignment.CenterVertically) {
            digits.forEachIndexed { i, ch ->
                if (i == hours.length) Spacer(Modifier.width(size * 0.14f))
                val colors = if (i < hours.length) listOf(palette.primary, palette.secondary) else listOf(palette.secondary, palette.primary)
                FloatingDigit(ch, i, colors, fontSize, size)
            }
        }
    }
}

@Composable
private fun FloatingDigit(ch: Char, index: Int, colors: List<Color>, fontSize: TextUnit, size: Dp) {
    val transition = rememberInfiniteTransition(label = "float$index")
    val bob by transition.animateFloat(
        -1f, 1f,
        infiniteRepeatable(tween(2600 + index * 410, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob$index",
    )
    val density = LocalDensity.current
    val amplitude = with(density) { size.toPx() * 0.03f }
    AnimatedContent(
        targetState = ch,
        transitionSpec = {
            ((scaleIn(spring(dampingRatio = 0.42f, stiffness = 180f), initialScale = 0.15f) + fadeIn(tween(220)) +
                slideInVertically(spring(dampingRatio = 0.6f, stiffness = 200f)) { it / 3 }) togetherWith
                (scaleOut(tween(320), targetScale = 1.35f) + fadeOut(tween(260)) + slideOutVertically(tween(320)) { -it / 4 }))
                .using(SizeTransform(clip = false))
        },
        modifier = Modifier.graphicsLayer {
            translationY = bob * amplitude
            rotationZ = bob * 1.4f
        },
        label = "digit$index",
    ) { c ->
        Text(
            c.toString(),
            style = TextStyle(
                brush = Brush.verticalGradient(colors),
                fontFamily = Rounded,
                fontWeight = FontWeight.Black,
                fontSize = fontSize,
                lineHeight = fontSize,
                lineHeightStyle = trimmed,
                shadow = Shadow(colors.last().copy(alpha = 0.35f), blurRadius = 40f),
            ),
        )
    }
}
