package com.gaales.standby.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.cos
import kotlin.math.sin

/*
 * SF Symbols-like glyphs drawn on a 24×24 grid. Drawing them keeps the APK small and lets
 * Night Mode recolor them like everything else.
 */

private inline fun DrawScope.grid(block: DrawScope.(u: Float) -> Unit) {
    val u = size.minDimension / 24f
    translate((size.width - 24 * u) / 2, (size.height - 24 * u) / 2) { block(u) }
}

private fun path(u: Float, vararg points: Pair<Float, Float>) = Path().apply {
    points.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(x * u, y * u) else lineTo(x * u, y * u) }
    close()
}

@Composable
fun BoltGlyph(modifier: Modifier, color: Color = Color.White) = Canvas(modifier) {
    grid { u -> drawPath(path(u, 13.5f to 1.5f, 3.5f to 14f, 11f to 14f, 10f to 22.5f, 20.5f to 9.5f, 13f to 9.5f), color) }
}

@Composable
fun AlarmGlyph(modifier: Modifier, color: Color = Color.White) = Canvas(modifier) {
    grid { u ->
        val stroke = Stroke(2.2f * u, cap = StrokeCap.Round)
        drawCircle(color, 8f * u, Offset(12f * u, 13.5f * u), style = stroke)
        drawLine(color, Offset(12f * u, 13.5f * u), Offset(12f * u, 9.2f * u), 2f * u, StrokeCap.Round)
        drawLine(color, Offset(12f * u, 13.5f * u), Offset(15f * u, 15.5f * u), 2f * u, StrokeCap.Round)
        drawArc(color, 200f, 70f, false, Offset(1.5f * u, 1.8f * u), Size(8f * u, 8f * u), style = stroke)
        drawArc(color, 270f, 70f, false, Offset(14.5f * u, 1.8f * u), Size(8f * u, 8f * u), style = stroke)
        drawLine(color, Offset(6.5f * u, 20f * u), Offset(5f * u, 22f * u), 2f * u, StrokeCap.Round)
        drawLine(color, Offset(17.5f * u, 20f * u), Offset(19f * u, 22f * u), 2f * u, StrokeCap.Round)
    }
}

@Composable
fun LocationGlyph(modifier: Modifier, color: Color = Color.White) = Canvas(modifier) {
    grid { u -> drawPath(path(u, 21f to 3f, 3f to 10.5f, 11f to 13f, 13.5f to 21f), color) }
}

@Composable
fun MusicGlyph(modifier: Modifier, color: Color = Color.White) = Canvas(modifier) {
    grid { u ->
        drawOval(color, Offset(5f * u, 14.5f * u), Size(8f * u, 6.5f * u))
        drawRect(color, Offset(11f * u, 3f * u), Size(2.2f * u, 15f * u))
        drawPath(path(u, 11f to 3f, 20f to 5.5f, 20f to 9.5f, 13.2f to 7.6f), color)
    }
}

@Composable
fun PlayGlyph(modifier: Modifier, color: Color = Color.White) = Canvas(modifier) {
    grid { u ->
        val p = path(u, 6f to 3.5f, 21f to 12f, 6f to 20.5f)
        drawPath(p, color)
        drawPath(p, color, style = Stroke(2f * u, join = StrokeJoin.Round))
    }
}

@Composable
fun PauseGlyph(modifier: Modifier, color: Color = Color.White) = Canvas(modifier) {
    grid { u ->
        drawRoundRect(color, Offset(5f * u, 3.5f * u), Size(4.6f * u, 17f * u), CornerRadius(1.4f * u))
        drawRoundRect(color, Offset(14.4f * u, 3.5f * u), Size(4.6f * u, 17f * u), CornerRadius(1.4f * u))
    }
}

@Composable
fun ForwardGlyph(modifier: Modifier, color: Color = Color.White, backward: Boolean = false) = Canvas(modifier) {
    grid { u ->
        val a = path(u, 2f to 6f, 12f to 12f, 2f to 18f)
        val b = path(u, 12f to 6f, 22f to 12f, 12f to 18f)
        if (backward) {
            scale(-1f, 1f) {
                drawPath(a, color); drawPath(b, color)
                drawPath(a, color, style = Stroke(1.6f * u, join = StrokeJoin.Round))
                drawPath(b, color, style = Stroke(1.6f * u, join = StrokeJoin.Round))
            }
        } else {
            drawPath(a, color); drawPath(b, color)
            drawPath(a, color, style = Stroke(1.6f * u, join = StrokeJoin.Round))
            drawPath(b, color, style = Stroke(1.6f * u, join = StrokeJoin.Round))
        }
    }
}

@Composable
fun PhoneGlyph(modifier: Modifier, color: Color = Color.White) = Canvas(modifier) {
    grid { u ->
        drawRoundRect(color, Offset(6.5f * u, 1.5f * u), Size(11f * u, 21f * u), CornerRadius(2.8f * u), style = Stroke(1.7f * u))
        drawRoundRect(color, Offset(10f * u, 3.4f * u), Size(4f * u, 1.3f * u), CornerRadius(0.7f * u))
    }
}

@Composable
fun PlusGlyph(modifier: Modifier, color: Color = Color.White) = Canvas(modifier) {
    grid { u ->
        drawLine(color, Offset(12f * u, 5f * u), Offset(12f * u, 19f * u), 2.6f * u, StrokeCap.Round)
        drawLine(color, Offset(5f * u, 12f * u), Offset(19f * u, 12f * u), 2.6f * u, StrokeCap.Round)
    }
}

@Composable
fun MinusGlyph(modifier: Modifier, color: Color = Color.White) = Canvas(modifier) {
    grid { u -> drawLine(color, Offset(6f * u, 12f * u), Offset(18f * u, 12f * u), 2.8f * u, StrokeCap.Round) }
}

@Composable
fun CloseGlyph(modifier: Modifier, color: Color = Color.White) = Canvas(modifier) {
    grid { u ->
        drawLine(color, Offset(6.5f * u, 6.5f * u), Offset(17.5f * u, 17.5f * u), 2.4f * u, StrokeCap.Round)
        drawLine(color, Offset(17.5f * u, 6.5f * u), Offset(6.5f * u, 17.5f * u), 2.4f * u, StrokeCap.Round)
    }
}

@Composable
fun CheckGlyph(modifier: Modifier, color: Color = Color.White) = Canvas(modifier) {
    grid { u ->
        val p = Path().apply { moveTo(5f * u, 12.5f * u); lineTo(10f * u, 17.5f * u); lineTo(19.5f * u, 6.5f * u) }
        drawPath(p, color, style = Stroke(2.6f * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun EyeGlyph(modifier: Modifier, color: Color = Color.White, slashed: Boolean = false) = Canvas(modifier) {
    grid { u ->
        val eye = Path().apply {
            moveTo(1.5f * u, 12f * u)
            quadraticTo(12f * u, 1f * u, 22.5f * u, 12f * u)
            quadraticTo(12f * u, 23f * u, 1.5f * u, 12f * u)
            close()
        }
        drawPath(eye, color, style = Stroke(1.8f * u, join = StrokeJoin.Round))
        drawCircle(color, 3.6f * u, Offset(12f * u, 12f * u))
        if (slashed) drawLine(color, Offset(4f * u, 3f * u), Offset(20f * u, 21f * u), 2f * u, StrokeCap.Round)
    }
}

// ---------------------------------------------------------------------------------------------
// Weather (multicolor, like the iOS Weather widget)

private val SunColor = Color(0xFFFFD60A)
private val CloudColor = Color(0xFFF2F2F7)
private val DarkCloud = Color(0xFFAEAEB2)
private val RainColor = Color(0xFF64D2FF)
private val MoonColor = Color(0xFFE5E5EA)

private enum class Sky { CLEAR, PARTLY, CLOUDY, FOG, DRIZZLE, RAIN, SNOW, SLEET, STORM }

private fun skyFor(code: Int) = when (code) {
    0, 1 -> Sky.CLEAR
    2 -> Sky.PARTLY
    3 -> Sky.CLOUDY
    45, 48 -> Sky.FOG
    51, 53, 55 -> Sky.DRIZZLE
    61, 63, 65, 80, 81, 82 -> Sky.RAIN
    56, 57, 66, 67 -> Sky.SLEET
    71, 73, 75, 77, 85, 86 -> Sky.SNOW
    95, 96, 99 -> Sky.STORM
    else -> Sky.CLOUDY
}

@Composable
fun WeatherGlyph(code: Int, isDay: Boolean, modifier: Modifier) = Canvas(modifier) {
    grid { u ->
        when (skyFor(code)) {
            Sky.CLEAR -> if (isDay) sun(u, 12f, 12f, 1f) else moon(u, 12f, 12f, 1f)
            Sky.PARTLY -> {
                if (isDay) sun(u, 8.5f, 8.5f, 0.75f) else moon(u, 9f, 8f, 0.7f)
                cloud(u, 13.5f, 15f, 0.78f, CloudColor)
            }
            Sky.CLOUDY -> {
                cloud(u, 15f, 10f, 0.55f, DarkCloud)
                cloud(u, 11f, 14.5f, 0.85f, CloudColor)
            }
            Sky.FOG -> {
                cloud(u, 12f, 9.5f, 0.75f, CloudColor)
                for (i in 0..2) drawLine(CloudColor, Offset((4f + i) * u, (16.5f + i * 2.6f) * u), Offset((20f - i) * u, (16.5f + i * 2.6f) * u), 1.6f * u, StrokeCap.Round)
            }
            Sky.DRIZZLE, Sky.RAIN -> {
                cloud(u, 12f, 9.5f, 0.8f, CloudColor)
                val heavy = skyFor(code) == Sky.RAIN
                for (i in 0..2) {
                    val x = 7.5f + i * 4.5f
                    drawLine(RainColor, Offset(x * u, 16.5f * u), Offset((x - 1.4f) * u, (if (heavy) 21.5f else 19.5f) * u), 1.8f * u, StrokeCap.Round)
                }
            }
            Sky.SNOW, Sky.SLEET -> {
                cloud(u, 12f, 9.5f, 0.8f, CloudColor)
                for (i in 0..2) {
                    drawCircle(Color.White, 1.3f * u, Offset((7.5f + i * 4.5f) * u, 18f * u))
                    drawCircle(Color.White, 1.3f * u, Offset((5.5f + i * 4.5f) * u, 21.5f * u))
                }
            }
            Sky.STORM -> {
                cloud(u, 12f, 9f, 0.8f, CloudColor)
                drawPath(path(u, 13f to 13.5f, 8f to 19.5f, 11.5f to 19.5f, 10f to 23.5f, 16f to 17f, 12.5f to 17f), SunColor)
            }
        }
    }
}

private fun DrawScope.sun(u: Float, cx: Float, cy: Float, s: Float) {
    drawCircle(SunColor, 4.4f * s * u, Offset(cx * u, cy * u))
    for (i in 0 until 8) {
        val a = Math.toRadians(i * 45.0)
        val r1 = 6.6f * s
        val r2 = 9f * s
        drawLine(
            SunColor,
            Offset((cx + r1 * cos(a).toFloat()) * u, (cy + r1 * sin(a).toFloat()) * u),
            Offset((cx + r2 * cos(a).toFloat()) * u, (cy + r2 * sin(a).toFloat()) * u),
            1.8f * s * u,
            StrokeCap.Round,
        )
    }
}

private fun DrawScope.moon(u: Float, cx: Float, cy: Float, s: Float) {
    val r = 8f * s * u
    val disc = Path().apply { addOval(androidx.compose.ui.geometry.Rect(Offset(cx * u, cy * u), r)) }
    val bite = Path().apply { addOval(androidx.compose.ui.geometry.Rect(Offset(cx * u + r * 0.55f, cy * u - r * 0.45f), r * 0.85f)) }
    drawPath(Path.combine(PathOperation.Difference, disc, bite), MoonColor)
}

private fun DrawScope.cloud(u: Float, cx: Float, cy: Float, s: Float, color: Color) {
    val k = s * u
    val ox = cx * u
    val oy = cy * u
    drawCircle(color, 4.6f * k, Offset(ox - 4.2f * k, oy + 0.8f * k))
    drawCircle(color, 6.2f * k, Offset(ox + 0.6f * k, oy - 1.6f * k))
    drawCircle(color, 4.2f * k, Offset(ox + 5.6f * k, oy + 1.2f * k))
    drawRoundRect(color, Offset(ox - 8.8f * k, oy + 0.6f * k), Size(18.6f * k, 4.8f * k), CornerRadius(2.4f * k))
}
