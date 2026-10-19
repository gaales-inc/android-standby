package com.gaales.standby.ui.clocks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaales.standby.data.ClockStyle
import com.gaales.standby.data.StandBySettings
import com.gaales.standby.ui.CloseGlyph
import com.gaales.standby.ui.DoneButton
import com.gaales.standby.ui.GlyphButton
import com.gaales.standby.ui.Ios
import com.gaales.standby.ui.PageDots
import com.gaales.standby.ui.is24Hour
import com.gaales.standby.ui.tap
import kotlinx.coroutines.delay

/** The third StandBy view: full-screen clocks, swiped vertically, each with its own colors. */
@Composable
fun ClocksPage(settings: StandBySettings, editing: Boolean, onEditRequest: () -> Unit, onDone: () -> Unit) {
    val styles = ClockStyle.entries
    val pager = rememberPagerState(initialPage = settings.clockStyle.coerceIn(0, styles.lastIndex)) { styles.size }
    LaunchedEffect(pager.settledPage) { settings.clockStyle = pager.settledPage }

    var dots by remember { mutableStateOf(false) }
    LaunchedEffect(pager.isScrollInProgress) {
        if (pager.isScrollInProgress) dots = true else { delay(1400); dots = false }
    }
    var picker by remember { mutableStateOf(false) }
    LaunchedEffect(editing) { if (!editing) picker = false }

    val h24 = is24Hour()
    val scale by animateFloatAsState(if (editing) 0.78f else 1f, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow), label = "clockEditScale")
    val corner by animateDpAsState(if (editing) 36.dp else 0.dp, label = "clockCorner")
    val style = styles[pager.currentPage]
    val palettes = ClockPalettes.forStyle(style)
    val selected = settings.clockColor(style).coerceIn(0, palettes.lastIndex)

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(editing) { if (!editing) detectTapGestures(onLongPress = { onEditRequest() }) }
    ) {
        VerticalPager(
            state = pager,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 0,
            userScrollEnabled = !picker,
        ) { i ->
            val pageStyle = styles[i]
            val pagePalettes = ClockPalettes.forStyle(pageStyle)
            val palette = pagePalettes[settings.clockColor(pageStyle).coerceIn(0, pagePalettes.lastIndex)]
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .clip(RoundedCornerShape(corner))
                    .then(if (editing) Modifier.border(1.dp, Ios.gray4, RoundedCornerShape(corner)) else Modifier)
            ) {
                ClockFace(pageStyle, palette, h24)
            }
        }
        PageDots(styles.size, pager.currentPage, visible = dots || editing, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 14.dp))

        AnimatedVisibility(editing, Modifier.align(Alignment.TopCenter).padding(top = 14.dp), enter = fadeIn(), exit = fadeOut()) {
            Text(style.title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
        AnimatedVisibility(editing, Modifier.align(Alignment.TopEnd).padding(20.dp), enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
            DoneButton(onClick = onDone)
        }
        if (editing && palettes.size > 1) {
            AnimatedVisibility(!picker, Modifier.align(Alignment.BottomEnd).padding(24.dp), enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
                ColorButton(palettes[selected]) { picker = true }
            }
            AnimatedVisibility(
                picker,
                Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp, start = 24.dp, end = 24.dp),
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
            ) {
                ColorPicker(palettes, selected, onSelect = { settings.setClockColor(style, it) }, onClose = { picker = false })
            }
        }
    }
}

/** The white circle in the corner that opens the color menu. */
@Composable
private fun ColorButton(palette: ClockPalette, onClick: () -> Unit) {
    Box(
        Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(Color.White)
            .tap(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(Brush.linearGradient(listOf(palette.primary, palette.secondary))))
    }
}

@Composable
private fun ColorPicker(palettes: List<ClockPalette>, selected: Int, onSelect: (Int) -> Unit, onClose: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(30.dp))
            .background(Ios.gray6.copy(alpha = 0.95f))
            .border(0.5.dp, Ios.gray4, RoundedCornerShape(30.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            Modifier.weight(1f, fill = false).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            palettes.forEachIndexed { i, p ->
                Box(
                    Modifier
                        .size(38.dp)
                        .then(if (i == selected) Modifier.border(2.5.dp, Color.White, CircleShape) else Modifier)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(p.primary, p.secondary)))
                        .tap { onSelect(i) }
                )
            }
        }
        GlyphButton(size = 34.dp, background = Ios.gray4, onClick = onClose) { CloseGlyph(it, Color.White) }
    }
}
