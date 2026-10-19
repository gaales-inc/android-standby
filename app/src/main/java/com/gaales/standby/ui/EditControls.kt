package com.gaales.standby.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Taps without the Material ripple, which iOS-style controls don't have. */
@Composable
fun Modifier.tap(onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    return clickable(source, indication = null, onClick = onClick)
}

/** The translucent "Done" pill in the top right corner while editing StandBy. */
@Composable
fun DoneButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(CircleShape)
            .background(Ios.gray5.copy(alpha = 0.92f))
            .tap(onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text("Done", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Round glyph buttons such as "+" for adding widgets. */
@Composable
fun GlyphButton(modifier: Modifier = Modifier, size: Dp = 44.dp, background: Color = Ios.gray5.copy(alpha = 0.92f), onClick: () -> Unit, glyph: @Composable (Modifier) -> Unit) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .tap(onClick),
        contentAlignment = Alignment.Center,
    ) { glyph(Modifier.size(size * 0.46f)) }
}

/** The grey "−" badge on jiggling widgets. */
@Composable
fun RemoveBadge(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .size(28.dp)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(Color(0xFFE5E5EA))
            .tap(onClick),
        contentAlignment = Alignment.Center,
    ) { MinusGlyph(Modifier.size(16.dp), Color.Black) }
}

/** A compact iOS switch for overlays on black. */
@Composable
fun MiniSwitch(checked: Boolean, modifier: Modifier = Modifier, onChange: (Boolean) -> Unit) {
    val knob by animateDpAsState(if (checked) 18.dp else 2.dp, label = "knob")
    val track by animateColorAsState(if (checked) Ios.green else Ios.gray3, label = "track")
    Box(
        modifier
            .size(width = 40.dp, height = 24.dp)
            .clip(CircleShape)
            .background(track)
            .tap { onChange(!checked) }
    ) {
        Box(
            Modifier
                .offset(x = knob, y = 2.dp)
                .size(20.dp)
                .shadow(2.dp, CircleShape)
                .background(Color.White, CircleShape)
        )
    }
}

@Composable
fun LabeledMiniSwitch(label: String, checked: Boolean, modifier: Modifier = Modifier, onChange: (Boolean) -> Unit) {
    Row(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Ios.gray6.copy(alpha = 0.92f))
            .border(0.5.dp, Ios.gray4, RoundedCornerShape(20.dp))
            .padding(start = 14.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.width(10.dp))
        MiniSwitch(checked, onChange = onChange)
    }
}

/** Full-screen dim layer that swallows touches and closes on tap. */
@Composable
fun Scrim(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .tap(onDismiss),
        contentAlignment = Alignment.Center,
    ) { content() }
}
