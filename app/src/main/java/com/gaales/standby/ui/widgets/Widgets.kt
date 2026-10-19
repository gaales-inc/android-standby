package com.gaales.standby.ui.widgets

import android.app.AlarmManager
import android.os.SystemClock
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gaales.standby.data.CalendarEvent
import com.gaales.standby.data.CalendarRepository
import com.gaales.standby.data.MediaRepository
import com.gaales.standby.data.NotificationBus
import com.gaales.standby.data.PhotoRepository
import com.gaales.standby.data.Places
import com.gaales.standby.data.WeatherRepository
import com.gaales.standby.data.WidgetKind
import com.gaales.standby.ui.AlarmGlyph
import com.gaales.standby.ui.BoltGlyph
import com.gaales.standby.ui.FitText
import com.gaales.standby.ui.ForwardGlyph
import com.gaales.standby.ui.Ios
import com.gaales.standby.ui.LocationGlyph
import com.gaales.standby.ui.MusicGlyph
import com.gaales.standby.ui.PauseGlyph
import com.gaales.standby.ui.PhoneGlyph
import com.gaales.standby.ui.PlayGlyph
import com.gaales.standby.ui.Rounded
import com.gaales.standby.ui.Scale
import com.gaales.standby.ui.StandByHost
import com.gaales.standby.ui.WeatherGlyph
import com.gaales.standby.ui.amPm
import com.gaales.standby.ui.atZone
import com.gaales.standby.ui.durationText
import com.gaales.standby.ui.fmt
import com.gaales.standby.ui.is24Hour
import com.gaales.standby.ui.now
import com.gaales.standby.ui.nowMs
import com.gaales.standby.ui.rememberBattery
import com.gaales.standby.ui.rememberScale
import com.gaales.standby.ui.shortTime
import com.gaales.standby.ui.timeText
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale
import java.time.format.TextStyle as DayNameStyle

/*
 * StandBy shows widgets without their backgrounds, large, on black. Each widget is designed on a
 * 100×100 unit grid (see [Scale]) so the same layout works in the stack, the gallery and previews.
 */

@Composable
fun WidgetContent(kind: WidgetKind, host: StandByHost, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val k = rememberScale(constraints.maxWidth)
        when (kind) {
            WidgetKind.CLOCK -> ClockWidget()
            WidgetKind.DIGITAL_CLOCK -> DigitalClockWidget(k)
            WidgetKind.CALENDAR -> CalendarWidget(k)
            WidgetKind.UP_NEXT -> UpNextWidget(k)
            WidgetKind.WEATHER -> WeatherWidget(k)
            WidgetKind.BATTERIES -> BatteriesWidget(k)
            WidgetKind.ALARM -> AlarmWidget(k)
            WidgetKind.WORLD_CLOCK -> WorldClockWidget(k)
            WidgetKind.MUSIC -> MusicWidget(k, host)
            WidgetKind.PHOTOS -> PhotoWidget(k)
        }
    }
}

private fun style(k: Scale, size: Number, color: Color = Color.White, weight: FontWeight = FontWeight.SemiBold) =
    TextStyle(color = color, fontSize = k.sp(size), fontWeight = weight, lineHeight = k.sp(size.toFloat() * 1.18f))

@Composable
private fun Hint(k: Scale, text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, style = style(k, 6.5, Ios.gray, FontWeight.Medium), textAlign = TextAlign.Center)
}

// ---------------------------------------------------------------------------------------------

@Composable
private fun ClockWidget() {
    val t = now()
    val place = remember { Places.local() }
    AnalogDial(t, Modifier.fillMaxSize().padding(2.dp), faceColor = Color(0xFF0D0D0E), label = place.shortName)
}

@Composable
private fun DigitalClockWidget(k: Scale) {
    val t = now()
    val h24 = is24Hour()
    val text = timeText(t, h24)
    Column(Modifier.fillMaxSize().padding(k.dp(6)), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(fmt(t, "EEEMMMd").uppercase(), style = style(k, 8, Ios.gray, FontWeight.Bold))
        FitText(
            text = text,
            style = TextStyle(color = Color.White, fontFamily = Rounded, fontWeight = FontWeight.ExtraBold),
            template = text.map { if (it.isDigit()) '0' else it }.joinToString(""),
            glyphHeight = 0.74f,
            modifier = Modifier.fillMaxWidth().height(k.dp(50)),
        )
        Text(if (h24) Places.local().city else amPm(t), style = style(k, 8, Ios.gray, FontWeight.Bold))
    }
}

@Composable
private fun CalendarWidget(k: Scale) {
    val today = now().toLocalDate()
    val locale = Locale.getDefault()
    val firstDay = WeekFields.of(locale).firstDayOfWeek
    val first = today.withDayOfMonth(1)
    val offset = (first.dayOfWeek.value - firstDay.value + 7) % 7
    val days = today.lengthOfMonth()
    val rows = (offset + days + 6) / 7
    Column(Modifier.fillMaxSize().padding(horizontal = k.dp(5), vertical = k.dp(7))) {
        Text(
            fmt(today, "MMMM").uppercase(),
            style = style(k, 8.5, Ios.red, FontWeight.Bold),
            modifier = Modifier.padding(start = k.dp(3.5), bottom = k.dp(3)),
        )
        Row(Modifier.fillMaxWidth()) {
            for (i in 0..6) {
                val dow = firstDay.plus(i.toLong())
                Text(
                    dow.getDisplayName(DayNameStyle.NARROW, locale),
                    style = style(k, 6.5, if (dow.isWeekend()) Ios.gray2 else Ios.gray, FontWeight.Bold),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Column(Modifier.weight(1f).fillMaxWidth().padding(top = k.dp(2)), verticalArrangement = Arrangement.SpaceEvenly) {
            for (r in 0 until rows) {
                Row(Modifier.fillMaxWidth()) {
                    for (c in 0..6) {
                        val day = r * 7 + c - offset + 1
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            if (day in 1..days) {
                                val isToday = day == today.dayOfMonth
                                val weekend = firstDay.plus(c.toLong()).isWeekend()
                                if (isToday) Box(Modifier.size(k.dp(11.5)).background(Ios.red, CircleShape))
                                Text(
                                    "$day",
                                    style = style(k, 7.6, if (isToday || !weekend) Color.White else Ios.gray, FontWeight.Bold),
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun DayOfWeek.isWeekend() = this == DayOfWeek.SATURDAY || this == DayOfWeek.SUNDAY

@Composable
private fun UpNextWidget(k: Scale) {
    val context = LocalContext.current
    val t = now()
    val h24 = is24Hour()
    val allowed = remember(t.minute) { CalendarRepository.hasPermission(context) }
    val upcoming = CalendarRepository.events.filter { it.end > t.toInstant().toEpochMilli() }.take(3)
    Column(Modifier.fillMaxSize().padding(horizontal = k.dp(8), vertical = k.dp(7))) {
        Text(fmt(t, "EEEE").uppercase(), style = style(k, 8, Ios.red, FontWeight.Bold))
        Text("${t.dayOfMonth}", style = style(k, 30, Color.White, FontWeight.Normal).copy(lineHeight = k.sp(32)))
        Spacer(Modifier.height(k.dp(2)))
        when {
            !allowed -> Hint(k, "Allow calendar access in Android Standby to see your events.", Modifier.fillMaxWidth())
            upcoming.isEmpty() -> Text("No more events today", style = style(k, 7.5, Ios.gray, FontWeight.SemiBold))
            else -> Column(verticalArrangement = Arrangement.spacedBy(k.dp(3.2))) {
                upcoming.forEach { EventRow(k, it, t, h24) }
            }
        }
    }
}

@Composable
private fun EventRow(k: Scale, event: CalendarEvent, now: ZonedDateTime, h24: Boolean) {
    val start = event.begin.atZone()
    val end = event.end.atZone()
    val dayDiff = ChronoUnit.DAYS.between(now.toLocalDate(), start.toLocalDate())
    val dayLabel = when {
        dayDiff <= 0L -> ""
        dayDiff == 1L -> "Tomorrow, "
        else -> fmt(start, "EEEE") + ", "
    }
    val time = if (event.allDay) "All-Day" else "${shortTime(start, h24)} – ${shortTime(end, h24)}"
    Row(Modifier.height(IntrinsicSize.Min)) {
        Box(
            Modifier
                .width(k.dp(1.4))
                .fillMaxHeight()
                .background(Color(event.color).copy(alpha = 1f), RoundedCornerShape(k.dp(1)))
        )
        Spacer(Modifier.width(k.dp(3)))
        Column {
            Text(event.title, style = style(k, 7.4, Color.White, FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(dayLabel + time, style = style(k, 6.4, Ios.gray, FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun WeatherWidget(k: Scale) {
    val w = WeatherRepository.weather
    val h24 = is24Hour()
    if (w == null) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            WeatherGlyph(2, true, Modifier.size(k.dp(28)))
            Spacer(Modifier.height(k.dp(3)))
            Text("Weather", style = style(k, 9))
            Hint(k, "Waiting for the forecast…")
        }
        return
    }
    val nowMs = nowMs()
    val hours = w.hourly.filter { it.epochSec * 1000 > nowMs - 3_600_000 }.take(5)
    Column(Modifier.fillMaxSize().padding(horizontal = k.dp(7), vertical = k.dp(7))) {
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(w.city, style = style(k, 8.5, Color.White, FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (w.usingLocation) LocationGlyph(Modifier.padding(start = k.dp(1.5)).size(k.dp(5.5)))
                }
                Text("${w.temp}°", style = style(k, 28, Color.White, FontWeight.Normal).copy(lineHeight = k.sp(30)))
            }
            Column(horizontalAlignment = Alignment.End) {
                WeatherGlyph(w.code, w.isDay, Modifier.size(k.dp(11)))
                Spacer(Modifier.height(k.dp(1.5)))
                Text(WeatherRepository.describe(w.code), style = style(k, 7, Color.White, FontWeight.SemiBold))
                Text("H:${w.high}° L:${w.low}°", style = style(k, 7, Color.White, FontWeight.SemiBold))
            }
        }
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            hours.forEachIndexed { i, h ->
                val at = (h.epochSec * 1000).atZone()
                val label = when {
                    i == 0 -> "Now"
                    h24 -> at.hour.toString().padStart(2, '0')
                    else -> "${(at.hour + 11) % 12 + 1}${amPm(at)}"
                }
                // "Now" shows current conditions, matching the headline.
                val code = if (i == 0) w.code else h.code
                val isDay = if (i == 0) w.isDay else h.isDay
                val temp = if (i == 0) w.temp else h.temp
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(label, style = style(k, 6, Ios.gray, FontWeight.Bold))
                    Spacer(Modifier.height(k.dp(1.5)))
                    WeatherGlyph(code, isDay, Modifier.size(k.dp(9)))
                    Spacer(Modifier.height(k.dp(1.5)))
                    Text("$temp°", style = style(k, 7, Color.White, FontWeight.Bold))
                }
            }
        }
    }
}

@Composable
private fun BatteriesWidget(k: Scale) {
    val b = rememberBattery()
    val ring = when {
        b.charging || b.level > 20 -> Ios.green
        else -> Ios.red
    }
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(k.dp(56)), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = k.px(5.5)
                val inset = stroke / 2
                val arcSize = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
                drawArc(Ios.gray5, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
                drawArc(ring, -90f, 360f * b.level / 100f, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            PhoneGlyph(Modifier.size(k.dp(22)))
            if (b.charging) {
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = k.dp(-2))
                        .size(k.dp(10))
                        .background(Color.Black, CircleShape)
                        .padding(k.dp(1.5))
                ) { BoltGlyph(Modifier.fillMaxSize(), Ios.green) }
            }
        }
        Spacer(Modifier.height(k.dp(5)))
        Text("${b.level}%", style = style(k, 14, Color.White, FontWeight.SemiBold))
        val detail = when {
            b.full -> "Charged"
            b.charging && b.timeToFullMs > 0 -> "Full in ${durationText(b.timeToFullMs)}"
            b.charging -> "Charging"
            else -> "This Phone"
        }
        Text(detail, style = style(k, 6.5, Ios.gray, FontWeight.SemiBold))
    }
}

@Composable
private fun AlarmWidget(k: Scale) {
    val context = LocalContext.current
    val t = now()
    val h24 = is24Hour()
    val next = remember(t.minute) {
        context.getSystemService(AlarmManager::class.java)?.nextAlarmClock?.triggerTime
    }
    Column(Modifier.fillMaxSize().padding(k.dp(8))) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AlarmGlyph(Modifier.size(k.dp(10)), Ios.orange)
            Spacer(Modifier.width(k.dp(2.5)))
            Text("Alarm", style = style(k, 8.5, Ios.orange, FontWeight.Bold))
        }
        Spacer(Modifier.weight(1f))
        if (next != null) {
            val at = next.atZone()
            Row {
                Text(timeText(at, h24), style = style(k, 25, Color.White, FontWeight.SemiBold).copy(fontFamily = Rounded), modifier = Modifier.alignByBaseline())
                if (!h24) Text(" " + amPm(at), style = style(k, 10, Color.White, FontWeight.SemiBold).copy(fontFamily = Rounded), modifier = Modifier.alignByBaseline())
            }
            val days = ChronoUnit.DAYS.between(t.toLocalDate(), at.toLocalDate())
            val day = when (days) {
                0L -> "Today"
                1L -> "Tomorrow"
                else -> fmt(at, "EEEE")
            }
            Text(day, style = style(k, 8, Ios.gray, FontWeight.Bold))
            Text("in ${durationText(next - t.toInstant().toEpochMilli())}", style = style(k, 6.5, Ios.gray2, FontWeight.SemiBold))
        } else {
            Text("No Alarm", style = style(k, 15, Ios.gray, FontWeight.Bold).copy(fontFamily = Rounded))
            Spacer(Modifier.height(k.dp(1)))
            Text("Alarms you set in your clock app show here.", style = style(k, 6.5, Ios.gray2, FontWeight.SemiBold))
        }
    }
}

@Composable
private fun WorldClockWidget(k: Scale) {
    val cities = remember { Places.worldClockCities() }
    val now = nowMs()
    Column(Modifier.fillMaxSize().padding(k.dp(3)), verticalArrangement = Arrangement.SpaceEvenly) {
        cities.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                row.forEach { place ->
                    val t = now.atZone(ZoneId.of(place.zone))
                    val day = t.hour in 6..17
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(k.dp(44))) {
                        AnalogDial(
                            time = t,
                            modifier = Modifier.size(k.dp(30)),
                            faceColor = if (day) Color.White else Ios.gray5,
                            numeralColor = if (day) Color.Black else Color.White,
                            tickColor = if (day) Color(0xFFC7C7CC) else Ios.gray3,
                            handColor = if (day) Color.Black else Color.White,
                            numeralWeight = FontWeight.SemiBold,
                            numeralScale = 1.05f,
                        )
                        Spacer(Modifier.height(k.dp(1.8)))
                        Text(place.city, style = style(k, 6.6, Color.White, FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(Places.offsetLabel(place), style = style(k, 5.4, Ios.gray, FontWeight.Bold))
                    }
                }
            }
        }
    }
}

@Composable
private fun MusicWidget(k: Scale, host: StandByHost) {
    val context = LocalContext.current
    val minute = nowMs() / 60_000
    val access = remember(minute) { NotificationBus.isAccessGranted(context) }
    val np = MediaRepository.nowPlaying
    when {
        !access -> Column(Modifier.fillMaxSize().padding(k.dp(8)), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            MusicGlyph(Modifier.size(k.dp(18)), Ios.pink)
            Spacer(Modifier.height(k.dp(3)))
            Text("Music", style = style(k, 9))
            Spacer(Modifier.height(k.dp(1)))
            Hint(k, "Allow notification access in Android Standby to show what's playing.")
        }
        np == null -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            MusicGlyph(Modifier.size(k.dp(18)), Ios.gray2)
            Spacer(Modifier.height(k.dp(3)))
            Text("Not Playing", style = style(k, 9, Ios.gray, FontWeight.Bold))
        }
        else -> Column(Modifier.fillMaxSize().padding(k.dp(7))) {
            Row(Modifier.fillMaxWidth()) {
                Artwork(np.art, Modifier.size(k.dp(38)).clip(RoundedCornerShape(k.dp(4.5))))
                Spacer(Modifier.weight(1f))
                if (np.playing) EqualizerBars(k, Modifier.size(k.dp(9)))
            }
            Spacer(Modifier.weight(1f))
            Text(np.title, style = style(k, 8.4, Color.White, FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(np.artist.ifBlank { np.source }, style = style(k, 7.4, Ios.gray, FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(k.dp(3.5)))
            val position = np.positionAt(SystemClock.elapsedRealtime())
            val fraction = if (np.durationMs > 0) (position.toFloat() / np.durationMs).coerceIn(0f, 1f) else 0f
            Box(Modifier.fillMaxWidth().height(k.dp(1.4)).clip(CircleShape).background(Ios.gray4)) {
                Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(Color.White.copy(alpha = 0.85f)))
            }
            Spacer(Modifier.height(k.dp(4)))
            val tap = remember { MutableInteractionSource() }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                ForwardGlyph(Modifier.size(k.dp(11)).clickable(tap, null) { MediaRepository.previous() }, backward = true)
                val playModifier = Modifier.size(k.dp(12)).clickable(tap, null) { MediaRepository.playPause() }
                if (np.playing) PauseGlyph(playModifier) else PlayGlyph(playModifier)
                ForwardGlyph(Modifier.size(k.dp(11)).clickable(tap, null) { MediaRepository.next() })
            }
        }
    }
}

@Composable
private fun Artwork(art: android.graphics.Bitmap?, modifier: Modifier) {
    val image = remember(art) { art?.asImageBitmap() }
    if (image != null) {
        Image(image, null, modifier, contentScale = ContentScale.Crop)
    } else {
        Box(modifier.background(Brush.linearGradient(listOf(Ios.pink, Ios.purple))), contentAlignment = Alignment.Center) {
            MusicGlyph(Modifier.fillMaxSize(0.5f))
        }
    }
}

@Composable
private fun EqualizerBars(k: Scale, modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "eq")
    val bars = (0..2).map { i ->
        transition.animateFloat(
            0.25f, 1f,
            infiniteRepeatable(tween(420 + i * 130, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "bar$i",
        )
    }
    Canvas(modifier) {
        val w = size.width / 5
        bars.forEachIndexed { i, v ->
            val h = size.height * v.value
            drawRoundRect(
                Ios.pink,
                topLeft = Offset(i * w * 2, size.height - h),
                size = androidx.compose.ui.geometry.Size(w, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w / 2),
            )
        }
    }
}

@Composable
private fun PhotoWidget(k: Scale) {
    val context = LocalContext.current
    val minute = nowMs() / 60_000
    val allowed = remember(minute / 5) { PhotoRepository.hasPermission(context) }
    var photos by remember { mutableStateOf<List<android.net.Uri>>(emptyList()) }
    LaunchedEffect(allowed) {
        photos = if (allowed) PhotoRepository.albums(context).firstOrNull()?.photos.orEmpty() else emptyList()
    }
    if (photos.isEmpty()) {
        Box(
            Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF5E5CE6), Color(0xFFBF5AF2), Color(0xFFFF9F0A)))),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(k.dp(10))) {
                Text("Photos", style = style(k, 10, Color.White, FontWeight.Bold))
                Spacer(Modifier.height(k.dp(2)))
                Text(
                    if (allowed) "No photos yet." else "Allow photo access in Android Standby to see your photos.",
                    style = style(k, 6.5, Color.White.copy(alpha = 0.85f), FontWeight.SemiBold),
                    textAlign = TextAlign.Center,
                )
            }
        }
        return
    }
    var index by remember { mutableIntStateOf(0) }
    var image by remember { mutableStateOf<ImageBitmap?>(null) }
    val maxDim = k.px(100).toInt().coerceAtLeast(256)
    LaunchedEffect(photos, index) {
        image = PhotoRepository.load(context, photos[index % photos.size], maxDim)?.asImageBitmap()
        delay(60_000)
        index++
    }
    Crossfade(image, animationSpec = tween(900), label = "photoWidget") { img ->
        if (img != null) Image(img, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Box(Modifier.fillMaxSize().background(Ios.gray6))
    }
}
