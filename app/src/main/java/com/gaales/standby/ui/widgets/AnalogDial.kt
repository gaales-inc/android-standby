package com.gaales.standby.ui.widgets

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import com.gaales.standby.ui.Ios
import java.time.ZonedDateTime
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Second hand angle that ticks once a second with a small spring, like a quartz movement. */
@Composable
fun rememberTickingSeconds(time: ZonedDateTime): Animatable<Float, *> {
    val target = time.toLocalTime().toSecondOfDay() * 6f
    val angle = remember { Animatable(target) }
    LaunchedEffect(target) {
        if (abs(target - angle.value) > 12f) angle.snapTo(target)
        else angle.animateTo(target, spring(dampingRatio = 0.5f, stiffness = 1400f))
    }
    return angle
}

/** The iOS Clock widget dial: numerals, minute track, white hands and an orange second hand. */
@Composable
fun AnalogDial(
    time: ZonedDateTime,
    modifier: Modifier = Modifier,
    faceColor: Color = Color.Transparent,
    numeralColor: Color = Color.White,
    tickColor: Color = Ios.gray3,
    handColor: Color = Color.White,
    secondColor: Color? = Ios.orange,
    label: String? = null,
    labelColor: Color = Ios.gray,
    numeralWeight: FontWeight = FontWeight.Medium,
    numeralScale: Float = 1f,
) {
    val measurer = rememberTextMeasurer()
    val seconds = rememberTickingSeconds(time)
    val hourAngle = ((time.hour % 12) + time.minute / 60f + time.second / 3600f) * 30f
    val minuteAngle = (time.minute + time.second / 60f) * 6f

    Spacer(
        modifier.drawWithCache {
            val r = size.minDimension / 2f
            val numeralStyle = TextStyle(color = numeralColor, fontSize = (r * 0.24f * numeralScale).toSp(), fontWeight = numeralWeight)
            val numerals = (1..12).map { measurer.measure(it.toString(), numeralStyle) }
            val labelLayout = label?.let {
                measurer.measure(it, TextStyle(color = labelColor, fontSize = (r * 0.13f).toSp(), fontWeight = FontWeight.SemiBold))
            }
            onDrawBehind {
                val c = center
                if (faceColor.alpha > 0f) drawCircle(faceColor, r, c)
                for (i in 0 until 60) {
                    if (i % 5 == 0) continue
                    val a = Math.toRadians(i * 6.0 - 90)
                    val outer = r * 0.95f
                    val inner = r * 0.89f
                    drawLine(
                        tickColor,
                        Offset(c.x + outer * cos(a).toFloat(), c.y + outer * sin(a).toFloat()),
                        Offset(c.x + inner * cos(a).toFloat(), c.y + inner * sin(a).toFloat()),
                        r * 0.012f,
                    )
                }
                numerals.forEachIndexed { i, layout ->
                    val a = Math.toRadians((i + 1) * 30.0 - 90)
                    val rr = r * 0.75f
                    val p = Offset(c.x + rr * cos(a).toFloat(), c.y + rr * sin(a).toFloat())
                    drawText(layout, topLeft = Offset(p.x - layout.size.width / 2f, p.y - layout.size.height / 2f))
                }
                labelLayout?.let {
                    drawText(it, topLeft = Offset(c.x - it.size.width / 2f, c.y - r * 0.42f - it.size.height / 2f))
                }
                hand(c, hourAngle, r * 0.5f, r * 0.075f, handColor, r)
                hand(c, minuteAngle, r * 0.84f, r * 0.06f, handColor, r)
                drawCircle(handColor, r * 0.055f, c)
                if (secondColor != null) {
                    rotate(seconds.value, c) {
                        drawLine(secondColor, Offset(c.x, c.y + r * 0.17f), Offset(c.x, c.y - r * 0.9f), r * 0.02f, StrokeCap.Round)
                    }
                    drawCircle(secondColor, r * 0.042f, c)
                    drawCircle(Color.Black, r * 0.016f, c)
                }
            }
        }
    )
}

/** iOS hands start as a thin stem and widen into a rounded bar. */
private fun DrawScope.hand(c: Offset, angle: Float, length: Float, width: Float, color: Color, r: Float) {
    rotate(angle, c) {
        drawLine(color, c, Offset(c.x, c.y - r * 0.14f), width * 0.4f, StrokeCap.Round)
        drawLine(color, Offset(c.x, c.y - r * 0.14f), Offset(c.x, c.y - length), width, StrokeCap.Round)
    }
}
