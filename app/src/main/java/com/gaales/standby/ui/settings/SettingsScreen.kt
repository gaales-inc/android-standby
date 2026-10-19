package com.gaales.standby.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaales.standby.data.StandBySettings
import com.gaales.standby.ui.PreviewHost
import com.gaales.standby.ui.StandByRoot
import com.gaales.standby.ui.tap

@Immutable
data class IosColors(
    val background: Color,
    val cell: Color,
    val label: Color,
    val secondary: Color,
    val separator: Color,
    val blue: Color,
    val green: Color,
    val switchOff: Color,
    val chevron: Color,
)

val LightIos = IosColors(
    background = Color(0xFFF2F2F7), cell = Color.White, label = Color.Black, secondary = Color(0x993C3C43),
    separator = Color(0xFFC6C6C8), blue = Color(0xFF007AFF), green = Color(0xFF34C759), switchOff = Color(0xFFE9E9EA),
    chevron = Color(0x4D3C3C43),
)

val DarkIos = IosColors(
    background = Color.Black, cell = Color(0xFF1C1C1E), label = Color.White, secondary = Color(0x99EBEBF5),
    separator = Color(0xFF38383A), blue = Color(0xFF0A84FF), green = Color(0xFF30D158), switchOff = Color(0xFF39393D),
    chevron = Color(0x4DEBEBF5),
)

val LocalIos = staticCompositionLocalOf { LightIos }

/** Where each permission or system setting stands, and how to change it. */
data class AccessItem(val title: String, val granted: Boolean, val onClick: () -> Unit)

@Composable
fun SettingsScreen(
    settings: StandBySettings,
    autoStart: List<AccessItem>,
    widgetAccess: List<AccessItem>,
    onEnabledChange: (Boolean) -> Unit,
    onPreview: () -> Unit,
    onScreenSaver: () -> Unit,
    canOpenAutomatically: Boolean,
    onSetUp: () -> Unit,
    version: String,
) {
    val c = LocalIos.current
    LazyColumn(
        Modifier.fillMaxSize().background(c.background),
        contentPadding = WindowInsets.safeDrawing.asPaddingValues(),
    ) {
        item {
            Text(
                "StandBy",
                color = c.label,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 12.dp),
            )
        }
        item { Hero(onPreview) }
        if (settings.enabled && !canOpenAutomatically) {
            item { SetupCard(onSetUp) }
        }
        item {
            Group(footer = "StandBy shows a full-screen display with glanceable information when your phone is on its side and charging.") {
                ToggleRow("StandBy", settings.enabled, onEnabledChange)
            }
        }
        item {
            Group(header = "Display", footer = "Night Mode shows StandBy with a red tint in low light.") {
                ToggleRow("Always On", settings.alwaysOn) { settings.alwaysOn = it }
                Separator()
                ToggleRow("Night Mode", settings.nightMode) { settings.nightMode = it }
            }
        }
        item {
            Group(header = "Notifications", footer = "Notifications appear full screen in StandBy. With Show Preview on Tap Only, the content stays hidden until you tap.") {
                ToggleRow("Show Notifications", settings.showNotifications) { settings.showNotifications = it }
                Separator()
                ToggleRow("Show Preview on Tap Only", settings.previewOnTap) { settings.previewOnTap = it }
            }
        }
        item {
            Group(
                header = "Open Automatically",
                footer = "Android needs these so StandBy can open on its own when you put your phone on its side to charge, like on iPhone. You can also choose Android Standby as the screen saver.",
            ) {
                autoStart.forEachIndexed { i, item ->
                    if (i > 0) Separator()
                    AccessRow(item)
                }
                Separator()
                NavRow("Screen Saver", "Choose", onScreenSaver)
            }
        }
        item {
            Group(header = "Widgets", footer = "Widgets work without these, but show more with them: your music and notifications, photos, events and local weather.") {
                widgetAccess.forEachIndexed { i, item ->
                    if (i > 0) Separator()
                    AccessRow(item)
                }
            }
        }
        item {
            Group {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 46.dp).tap(onPreview).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Open StandBy Now", color = c.blue, fontSize = 17.sp)
                }
            }
        }
        item {
            Text(
                "Android Standby $version",
                color = c.secondary,
                fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 32.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

/** A phone on its side running a live, scaled-down StandBy. */
@Composable
private fun Hero(onPreview: () -> Unit) {
    val c = LocalIos.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2.17f)
                .shadow(12.dp, RoundedCornerShape(30.dp))
                .clip(RoundedCornerShape(30.dp))
                .background(Color(0xFF2C2C2E))
                .padding(7.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black)
                .tap(onPreview),
            contentAlignment = Alignment.Center,
        ) {
            val designWidth = 800.dp
            val scale = maxWidth / designWidth
            Box(
                Modifier
                    .requiredSize(designWidth, designWidth / 2.17f)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
            ) {
                StandByRoot(PreviewHost)
            }
            // Swallow touches so the preview stays put; a tap opens the real thing.
            Box(Modifier.fillMaxSize().tap(onPreview))
        }
        Text(
            "Tap to open StandBy. Swipe between Widgets, Photos and Clock; touch and hold to customize.",
            color = c.secondary,
            fontSize = 13.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp, start = 12.dp, end = 12.dp),
        )
    }
}

/** Shown until Android lets StandBy open by itself over the lock screen. */
@Composable
private fun SetupCard(onSetUp: () -> Unit) {
    val c = LocalIos.current
    Column(
        Modifier
            .padding(start = 16.dp, end = 16.dp, top = 22.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.cell)
            .padding(16.dp)
    ) {
        Text("Finish Setting Up StandBy", color = c.label, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Text(
            "To open StandBy automatically when your phone is charging on its side, allow Android Standby to display over other apps.",
            color = c.secondary,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(c.blue)
                .tap(onSetUp)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Allow", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun Group(header: String? = null, footer: String? = null, content: @Composable () -> Unit) {
    val c = LocalIos.current
    Column(Modifier.fillMaxWidth().padding(top = 22.dp)) {
        if (header != null) {
            Text(
                header.uppercase(),
                color = c.secondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 32.dp, bottom = 7.dp),
            )
        }
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(c.cell)
        ) { content() }
        if (footer != null) {
            Text(
                footer,
                color = c.secondary,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 7.dp),
            )
        }
    }
}

@Composable
private fun Separator() {
    val c = LocalIos.current
    Box(Modifier.padding(start = 16.dp).fillMaxWidth().height(0.5.dp).background(c.separator))
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = LocalIos.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = 46.dp).tap { onChange(!checked) }.padding(horizontal = 16.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = c.label, fontSize = 17.sp, modifier = Modifier.weight(1f))
        IosSwitch(checked, onChange)
    }
}

@Composable
private fun AccessRow(item: AccessItem) = NavRow(item.title, if (item.granted) "On" else "Off", item.onClick)

@Composable
private fun NavRow(title: String, value: String, onClick: () -> Unit) {
    val c = LocalIos.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = 46.dp).tap(onClick).padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, color = c.label, fontSize = 17.sp, modifier = Modifier.weight(1f))
        Text(value, color = c.secondary, fontSize = 17.sp)
        Text("›", color = c.chevron, fontSize = 24.sp, fontWeight = FontWeight.Medium, modifier = Modifier.offset(y = (-1).dp))
    }
}

/** The iOS switch: 51×31 pt, green when on. */
@Composable
fun IosSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = LocalIos.current
    val offset by animateDpAsState(if (checked) 22.dp else 2.dp, spring(dampingRatio = 0.7f, stiffness = 700f), label = "switchKnob")
    val track by animateColorAsState(if (checked) c.green else c.switchOff, label = "switchTrack")
    Box(
        Modifier
            .size(width = 51.dp, height = 31.dp)
            .clip(CircleShape)
            .background(track)
            .tap { onChange(!checked) }
    ) {
        Box(
            Modifier
                .offset(x = offset, y = 2.dp)
                .size(27.dp)
                .shadow(3.dp, CircleShape)
                .background(Color.White, CircleShape)
                .border(0.5.dp, Color.Black.copy(alpha = 0.04f), CircleShape)
        )
    }
}
