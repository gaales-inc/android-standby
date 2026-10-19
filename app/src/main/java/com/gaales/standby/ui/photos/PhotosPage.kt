package com.gaales.standby.ui.photos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.gaales.standby.data.Album
import com.gaales.standby.data.PhotoRepository
import com.gaales.standby.data.StandBySettings
import com.gaales.standby.ui.DoneButton
import com.gaales.standby.ui.EyeGlyph
import com.gaales.standby.ui.Ios
import com.gaales.standby.ui.StandByHost
import com.gaales.standby.ui.fmt
import com.gaales.standby.ui.is24Hour
import com.gaales.standby.ui.now
import com.gaales.standby.ui.tap
import com.gaales.standby.ui.timeText
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.random.Random

/** The second StandBy view: your photos, full screen, with the time in the corner. */
@Composable
fun PhotosPage(
    settings: StandBySettings,
    host: StandByHost,
    editing: Boolean,
    onEditRequest: () -> Unit,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    var resumes by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { resumes++ }
    val allowed = remember(resumes) { PhotoRepository.hasPermission(context) }
    var albums by remember { mutableStateOf<List<Album>?>(null) }
    LaunchedEffect(allowed) { albums = if (allowed) PhotoRepository.albums(context) else emptyList() }
    val visible = albums.orEmpty().filter { it.id !in settings.hiddenAlbums && it.photos.isNotEmpty() }
    var taps by remember { mutableIntStateOf(0) }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(editing) {
                if (!editing) detectTapGestures(onLongPress = { onEditRequest() }, onTap = { taps++ })
            }
    ) {
        if (visible.isEmpty()) {
            EmptyPhotos(allowed = allowed, loading = albums == null, host = host)
        } else {
            val pager = rememberPagerState { visible.size }
            VerticalPager(pager, Modifier.fillMaxSize(), beyondViewportPageCount = 0, key = { visible[it].id }) { i ->
                Slideshow(visible[i], active = pager.currentPage == i, taps = taps)
            }
            var showName by remember { mutableStateOf(false) }
            LaunchedEffect(pager.currentPage) { showName = true; delay(2200); showName = false }
            AnimatedVisibility(showName && !editing, Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp), enter = fadeIn(), exit = fadeOut()) {
                Text(
                    visible.getOrNull(pager.currentPage)?.name.orEmpty(),
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
        TimeOverlay(Modifier.align(Alignment.TopStart))
        if (editing) {
            AlbumEditor(albums.orEmpty(), settings, onDone)
        }
    }
}

@Composable
private fun TimeOverlay(modifier: Modifier) {
    val t = now()
    val h24 = is24Hour()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val h = maxHeight
        val density = LocalDensity.current
        val big = with(density) { (h * 0.2f).toSp() }
        val small = with(density) { (h * 0.055f).toSp() }
        val shadow = Shadow(Color.Black.copy(alpha = 0.55f), Offset(0f, 2f), blurRadius = 18f)
        Column(modifier.padding(start = maxWidth * 0.05f, top = h * 0.07f)) {
            Text(fmt(t, "EEEMMMd"), style = TextStyle(color = Color.White, fontSize = small, fontWeight = FontWeight.SemiBold, shadow = shadow))
            Text(timeText(t, h24), style = TextStyle(color = Color.White, fontSize = big, lineHeight = big, fontWeight = FontWeight.SemiBold, shadow = shadow))
        }
    }
}

@Composable
private fun Slideshow(album: Album, active: Boolean, taps: Int) {
    val context = LocalContext.current
    val window = LocalWindowInfo.current.containerSize
    val maxDim = max(window.width, window.height).coerceIn(720, 2400)
    var index by remember(album.id) { mutableIntStateOf(0) }
    var image by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(taps) { if (taps > 0 && active) index++ }
    LaunchedEffect(album.id, index, active) {
        val photos = album.photos
        val bitmap = PhotoRepository.load(context, photos[index % photos.size], maxDim)
        if (bitmap != null) image = bitmap.asImageBitmap()
        PhotoRepository.load(context, photos[(index + 1) % photos.size], maxDim)
        if (active) {
            delay(15_000)
            index++
        }
    }
    Crossfade(image, animationSpec = tween(1600), label = "slideshow") { img ->
        if (img != null) KenBurns(img) else Box(Modifier.fillMaxSize().background(Color.Black))
    }
}

/** Slow zoom and drift, like the Photos shuffle. */
@Composable
private fun KenBurns(image: ImageBitmap) {
    val progress = remember(image) { Animatable(0f) }
    val direction = remember(image) { Random.nextInt(4) }
    LaunchedEffect(image) { progress.animateTo(1f, tween(18_000, easing = LinearEasing)) }
    Image(
        bitmap = image,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                val p = progress.value
                val s = 1.04f + 0.08f * p
                scaleX = s
                scaleY = s
                val dx = size.width * 0.025f * (p - 0.5f)
                val dy = size.height * 0.02f * (p - 0.5f)
                translationX = if (direction % 2 == 0) dx else -dx
                translationY = if (direction < 2) dy else -dy
            },
    )
}

@Composable
private fun EmptyPhotos(allowed: Boolean, loading: Boolean, host: StandByHost) {
    val transition = rememberInfiniteTransition(label = "aurora")
    val shift by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(14_000, easing = LinearEasing), RepeatMode.Reverse), label = "auroraShift")
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF1B1464), Color(0xFF6A1B9A), Color(0xFFE65100), Color(0xFF1B1464)),
                        start = Offset(w * shift, 0f),
                        end = Offset(w * (1 - shift) + w * 0.6f, h),
                    )
                )
        )
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)))))
        if (!loading) {
            Column(
                Modifier.align(Alignment.Center).padding(horizontal = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Photos", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    if (allowed) "Your photos will appear here." else "Allow photo access in Android Standby to show your photos here.",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                )
                if (!allowed && !host.isPreview) {
                    Spacer(Modifier.height(18.dp))
                    Text(
                        "Allow Access",
                        color = Color.Black,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White)
                            .tap { host.openApp() }
                            .padding(horizontal = 22.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}

/** Long-press editor: choose which albums StandBy shuffles through. */
@Composable
private fun AlbumEditor(albums: List<Album>, settings: StandBySettings, onDone: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)).tap { }) {
        Column(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.5f)
                .fillMaxHeight(0.82f)
                .clip(RoundedCornerShape(24.dp))
                .background(Ios.gray6)
                .padding(vertical = 12.dp)
        ) {
            Text("Albums", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp))
            if (albums.isEmpty()) {
                Text("No albums yet.", color = Ios.gray, fontSize = 15.sp, modifier = Modifier.padding(20.dp))
            }
            LazyColumn {
                items(albums, key = { it.id }) { album ->
                    val hidden = album.id in settings.hiddenAlbums
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .tap {
                                settings.hiddenAlbums = if (hidden) settings.hiddenAlbums - album.id else settings.hiddenAlbums + album.id
                            }
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(album.name, color = if (hidden) Ios.gray else Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            Text("${album.photos.size} photos", color = Ios.gray, fontSize = 13.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        EyeGlyph(Modifier.size(24.dp), if (hidden) Ios.gray2 else Ios.blue, slashed = hidden)
                    }
                    HorizontalDivider(Modifier.padding(start = 18.dp), thickness = 0.5.dp, color = Ios.gray4)
                }
            }
        }
        DoneButton(Modifier.align(Alignment.TopEnd).padding(20.dp), onClick = onDone)
    }
}
