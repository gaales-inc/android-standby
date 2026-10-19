package com.gaales.standby.ui.overlays

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaales.standby.data.NotificationBus
import com.gaales.standby.data.StandByNotice
import com.gaales.standby.data.StandBySettings
import com.gaales.standby.ui.BoltGlyph
import com.gaales.standby.ui.Ios
import com.gaales.standby.ui.StandByHost
import com.gaales.standby.ui.rememberBattery
import com.gaales.standby.ui.tap
import kotlinx.coroutines.delay

/**
 * Notifications arrive full screen in StandBy: a large app icon, then the content. With
 * "Show Preview on Tap Only" the content stays hidden until tapped.
 */
@Composable
fun BoxScope.NotificationOverlay(settings: StandBySettings, host: StandByHost) {
    var current by remember { mutableStateOf<StandByNotice?>(null) }
    var last by remember { mutableStateOf<StandByNotice?>(null) }
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        NotificationBus.notices.collect { notice ->
            if (!settings.showNotifications || host.isPreview) return@collect
            current = notice
            last = notice
            revealed = !settings.previewOnTap
        }
    }
    LaunchedEffect(current, revealed) {
        if (current != null) {
            delay(if (revealed) 9_000 else 7_000)
            current = null
        }
    }
    AnimatedVisibility(
        visible = current != null,
        modifier = Modifier.matchParentSize(),
        enter = fadeIn(tween(250)) + scaleIn(spring(dampingRatio = 0.75f), initialScale = 0.92f),
        exit = fadeOut(tween(350)),
    ) {
        val notice = current ?: last ?: return@AnimatedVisibility
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.88f))
                .tap {
                    when {
                        !revealed -> revealed = true
                        else -> {
                            current = null
                            host.openNotification(notice)
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            val h = maxHeight
            val density = LocalDensity.current
            val icon = remember(notice.key, notice.postedAt) { notice.icon?.asImageBitmap() }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth(0.7f)) {
                if (icon != null) {
                    Image(icon, null, Modifier.size(h * 0.26f).clip(RoundedCornerShape(h * 0.06f)))
                } else {
                    Box(Modifier.size(h * 0.26f).clip(RoundedCornerShape(h * 0.06f)).background(Ios.gray5))
                }
                Spacer(Modifier.height(h * 0.04f))
                Text(
                    notice.appName,
                    color = Ios.gray,
                    fontSize = with(density) { (h * 0.05f).toSp() },
                    fontWeight = FontWeight.SemiBold,
                )
                if (revealed) {
                    notice.title?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            it,
                            color = Color.White,
                            fontSize = with(density) { (h * 0.075f).toSp() },
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    notice.text?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.height(h * 0.01f))
                        Text(
                            it,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = with(density) { (h * 0.055f).toSp() },
                            textAlign = TextAlign.Center,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                } else {
                    Text(
                        "Tap to View",
                        color = Color.White,
                        fontSize = with(density) { (h * 0.065f).toSp() },
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

/** The green charging badge that appears when StandBy starts on a charger. */
@Composable
fun BoxScope.ChargingIndicator(host: StandByHost) {
    val battery = rememberBattery()
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(battery.charging) {
        if (battery.charging && !host.isPreview) {
            delay(300)
            visible = true
            delay(3800)
        }
        visible = false
    }
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.align(Alignment.TopCenter).padding(top = 18.dp),
        enter = fadeIn() + slideInVertically(spring(dampingRatio = 0.7f)) { -it * 2 },
        exit = fadeOut() + slideOutVertically { -it * 2 },
    ) {
        Row(
            Modifier
                .clip(CircleShape)
                .background(Ios.gray6)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BoltGlyph(Modifier.size(18.dp), Ios.green)
            Spacer(Modifier.width(6.dp))
            Text("${battery.level}%", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(10.dp))
            Box(Modifier.size(width = 34.dp, height = 15.dp).clip(RoundedCornerShape(4.dp)).background(Ios.gray4)) {
                Box(Modifier.fillMaxSize().padding(2.dp)) {
                    Box(
                        Modifier
                            .fillMaxWidth(battery.level / 100f)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(2.dp))
                            .background(Ios.green)
                    )
                }
            }
        }
    }
}
