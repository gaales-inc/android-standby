package com.gaales.standby.ui.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.gaales.standby.data.CalendarRepository
import com.gaales.standby.data.MediaRepository
import com.gaales.standby.data.StandBySettings
import com.gaales.standby.data.WidgetKind
import com.gaales.standby.ui.CheckGlyph
import com.gaales.standby.ui.CloseGlyph
import com.gaales.standby.ui.DoneButton
import com.gaales.standby.ui.GlyphButton
import com.gaales.standby.ui.Ios
import com.gaales.standby.ui.LabeledMiniSwitch
import com.gaales.standby.ui.PageDots
import com.gaales.standby.ui.PlusGlyph
import com.gaales.standby.ui.RemoveBadge
import com.gaales.standby.ui.StandByHost
import com.gaales.standby.ui.jiggle
import com.gaales.standby.ui.nowMs
import com.gaales.standby.ui.tap
import kotlinx.coroutines.delay

/** The first StandBy view: two Smart Stacks side by side. */
@Composable
fun WidgetsPage(
    settings: StandBySettings,
    host: StandByHost,
    editing: Boolean,
    onEditRequest: () -> Unit,
    onDone: () -> Unit,
) {
    var galleryOpen by remember { mutableStateOf(false) }
    var focus by remember { mutableIntStateOf(0) }
    LaunchedEffect(editing) { if (!editing) galleryOpen = false }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .pointerInput(editing) { if (!editing) detectTapGestures(onLongPress = { onEditRequest() }) }
    ) {
        val portrait = maxHeight > maxWidth
        val gap = if (portrait) maxHeight * 0.04f else maxWidth * 0.035f
        val side = if (portrait) min(maxWidth * 0.84f, (maxHeight - gap * 3) / 2) else min(maxHeight * 0.86f, (maxWidth - gap * 3) / 2)
        val scale by animateFloatAsState(if (editing) 0.8f else 1f, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow), label = "editScale")

        val stack: @Composable (Int) -> Unit = { index ->
            val kinds = if (index == 0) settings.leftStack else settings.rightStack
            SmartStack(
                kinds = kinds,
                side = side,
                editing = editing,
                seed = index,
                smartRotate = if (index == 0) settings.leftSmartRotate else settings.rightSmartRotate,
                focused = editing && focus == index,
                host = host,
                onFocus = { focus = index },
                onRemove = { kind ->
                    val updated = kinds - kind
                    if (index == 0) settings.leftStack = updated else settings.rightStack = updated
                },
                onSmartRotate = { on -> if (index == 0) settings.leftSmartRotate = on else settings.rightSmartRotate = on },
            )
        }
        val stacksModifier = Modifier
            .align(Alignment.Center)
            .graphicsLayer { scaleX = scale; scaleY = scale }
        if (portrait) {
            Column(stacksModifier, verticalArrangement = Arrangement.spacedBy(gap)) { stack(0); stack(1) }
        } else {
            Row(stacksModifier, horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.CenterVertically) { stack(0); stack(1) }
        }

        AnimatedVisibility(editing, Modifier.align(Alignment.TopStart).padding(20.dp), enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
            GlyphButton(onClick = { galleryOpen = true }) { PlusGlyph(it) }
        }
        AnimatedVisibility(editing, Modifier.align(Alignment.TopEnd).padding(20.dp), enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
            DoneButton(onClick = onDone)
        }
        if (galleryOpen) {
            WidgetGallery(
                target = focus,
                current = if (focus == 0) settings.leftStack else settings.rightStack,
                host = host,
                onTarget = { focus = it },
                onPick = { kind ->
                    if (focus == 0) settings.leftStack = settings.leftStack + kind else settings.rightStack = settings.rightStack + kind
                    galleryOpen = false
                },
                onClose = { galleryOpen = false },
            )
        }
    }
}

@Composable
private fun SmartStack(
    kinds: List<WidgetKind>,
    side: Dp,
    editing: Boolean,
    seed: Int,
    smartRotate: Boolean,
    focused: Boolean,
    host: StandByHost,
    onFocus: () -> Unit,
    onRemove: (WidgetKind) -> Unit,
    onSmartRotate: (Boolean) -> Unit,
) {
    val pager = rememberPagerState { kinds.size.coerceAtLeast(1) }
    var dots by remember { mutableStateOf(false) }
    LaunchedEffect(pager.isScrollInProgress) {
        if (pager.isScrollInProgress) dots = true else { delay(1400); dots = false }
    }
    SmartRotate(pager, kinds, enabled = smartRotate && !editing)

    val corner = side * 0.12f
    Box(Modifier.size(side)) {
        VerticalPager(
            state = pager,
            modifier = Modifier.fillMaxSize(),
            pageSpacing = 24.dp,
            beyondViewportPageCount = 0,
            key = { kinds.getOrNull(it)?.name ?: "empty" },
        ) { i ->
            val kind = kinds.getOrNull(i)
            Box(
                Modifier
                    .fillMaxSize()
                    .jiggle(editing, seed + i)
                    .clip(RoundedCornerShape(corner))
                    .then(if (editing) Modifier.background(Ios.gray6).tap(onFocus) else Modifier)
                    .then(if (focused) Modifier.border(2.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(corner)) else Modifier)
            ) {
                if (kind != null) WidgetContent(kind, host)
                else EmptyStack()
            }
        }
        PageDots(
            count = kinds.size,
            current = pager.currentPage,
            visible = dots || editing,
            modifier = Modifier.align(Alignment.CenterEnd).offset(x = 18.dp),
        )
        if (editing && kinds.isNotEmpty()) {
            RemoveBadge(Modifier.align(Alignment.TopStart).offset((-10).dp, (-10).dp)) {
                kinds.getOrNull(pager.currentPage)?.let(onRemove)
            }
        }
        if (editing) {
            LabeledMiniSwitch(
                label = "Smart Rotate",
                checked = smartRotate,
                modifier = Modifier.align(Alignment.BottomCenter).offset(y = 54.dp),
                onChange = onSmartRotate,
            )
        }
    }
}

/** iOS Smart Rotate: bring Music forward while something plays and Up Next before an event. */
@Composable
private fun SmartRotate(pager: PagerState, kinds: List<WidgetKind>, enabled: Boolean) {
    val playing = MediaRepository.nowPlaying?.playing == true
    val minute = nowMs() / 60_000
    val eventSoon = remember(minute, CalendarRepository.events) { CalendarRepository.eventSoon(System.currentTimeMillis()) != null }
    LaunchedEffect(enabled, playing, eventSoon, kinds) {
        if (!enabled) return@LaunchedEffect
        val target = when {
            playing && WidgetKind.MUSIC in kinds -> WidgetKind.MUSIC
            eventSoon && WidgetKind.UP_NEXT in kinds -> WidgetKind.UP_NEXT
            else -> null
        } ?: return@LaunchedEffect
        pager.animateScrollToPage(kinds.indexOf(target))
    }
}

@Composable
private fun EmptyStack() {
    Box(Modifier.fillMaxSize().background(Ios.gray6), contentAlignment = Alignment.Center) {
        Text("Tap + to add a widget", color = Ios.gray, fontSize = 15.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
    }
}

@Composable
private fun WidgetGallery(
    target: Int,
    current: List<WidgetKind>,
    host: StandByHost,
    onTarget: (Int) -> Unit,
    onPick: (WidgetKind) -> Unit,
    onClose: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .tap(onClose),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .fillMaxWidth(0.86f)
                .fillMaxHeight(0.9f)
                .clip(RoundedCornerShape(28.dp))
                .background(Ios.gray6)
                .tap { }
                .padding(top = 14.dp)
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Segmented(listOf("Left Stack", "Right Stack"), target, onTarget)
                Spacer(Modifier.weight(1f))
                Text("Add Widget", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                GlyphButton(size = 32.dp, background = Ios.gray4, onClick = onClose) { CloseGlyph(it, Ios.gray) }
            }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(WidgetKind.entries, key = { it.name }) { kind ->
                    val added = kind in current
                    Column(
                        Modifier
                            .alpha(if (added) 0.45f else 1f)
                            .then(if (added) Modifier else Modifier.tap { onPick(kind) })
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(22.dp))
                                .background(Color.Black)
                        ) {
                            WidgetContent(kind, host)
                            if (added) {
                                Box(
                                    Modifier.align(Alignment.TopEnd).padding(8.dp).size(22.dp).background(Ios.green, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) { CheckGlyph(Modifier.size(14.dp)) }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(kind.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text(kind.subtitle, color = Ios.gray, fontSize = 13.sp, lineHeight = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(Ios.gray5)
            .padding(2.dp)
    ) {
        options.forEachIndexed { i, label ->
            Box(
                Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (i == selected) Ios.gray2 else Color.Transparent)
                    .tap { onSelect(i) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(label, color = Color.White, fontSize = 13.sp, fontWeight = if (i == selected) FontWeight.SemiBold else FontWeight.Medium)
            }
        }
    }
}
