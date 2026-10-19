package com.gaales.standby.data

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.gaales.standby.service.StandByNotificationListener

data class NowPlaying(
    val title: String,
    val artist: String,
    val art: Bitmap?,
    val playing: Boolean,
    val durationMs: Long,
    val positionMs: Long,
    val positionUpdatedAt: Long,
    val speed: Float,
    val source: String,
) {
    fun positionAt(elapsedRealtime: Long): Long {
        if (!playing) return positionMs
        val moved = ((elapsedRealtime - positionUpdatedAt) * speed).toLong()
        return (positionMs + moved).coerceIn(0, if (durationMs > 0) durationMs else Long.MAX_VALUE)
    }
}

/** What's playing on the device, read from active media sessions (requires notification access). */
object MediaRepository {
    var nowPlaying by mutableStateOf<NowPlaying?>(null)
        private set

    private val handler = Handler(Looper.getMainLooper())
    private var manager: MediaSessionManager? = null
    private var controller: MediaController? = null
    private var appContext: Context? = null
    private var users = 0

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { pick(it) }

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = update()
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            update()
            // A paused session may not be the best one any more.
            if (state?.state != PlaybackState.STATE_PLAYING) refreshSessions()
        }
        override fun onSessionDestroyed() {
            controller?.unregisterCallback(this)
            controller = null
            refreshSessions()
        }
    }

    fun start(context: Context) {
        users++
        appContext = context.applicationContext
        if (manager == null) attach()
        else refreshSessions()
    }

    fun stop() {
        users = (users - 1).coerceAtLeast(0)
        if (users > 0) return
        runCatching { manager?.removeOnActiveSessionsChangedListener(sessionsListener) }
        controller?.unregisterCallback(callback)
        controller = null
        manager = null
    }

    /** Called when notification access is granted or the listener reconnects. */
    fun onAccessChanged() {
        if (users > 0) {
            if (manager == null) attach() else refreshSessions()
        }
    }

    private fun attach() {
        val context = appContext ?: return
        if (!NotificationBus.isAccessGranted(context)) return
        val m = context.getSystemService(MediaSessionManager::class.java) ?: return
        val component = ComponentName(context, StandByNotificationListener::class.java)
        try {
            m.addOnActiveSessionsChangedListener(sessionsListener, component, handler)
            manager = m
            pick(m.getActiveSessions(component))
        } catch (_: SecurityException) {
            manager = null
        }
    }

    private fun refreshSessions() {
        val context = appContext ?: return
        val m = manager ?: return
        try {
            pick(m.getActiveSessions(ComponentName(context, StandByNotificationListener::class.java)))
        } catch (_: SecurityException) {
        }
    }

    private fun pick(list: List<MediaController>?) {
        val sessions = list.orEmpty()
        val best = sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: sessions.firstOrNull { it.sessionToken == controller?.sessionToken }
            ?: sessions.firstOrNull()
        if (best?.sessionToken != controller?.sessionToken) {
            controller?.unregisterCallback(callback)
            controller = best
            best?.registerCallback(callback, handler)
        }
        update()
    }

    private fun update() {
        val c = controller
        val meta = c?.metadata
        val state = c?.playbackState
        if (c == null || meta == null) {
            nowPlaying = null
            return
        }
        val title = meta.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: meta.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: ""
        if (title.isBlank()) {
            nowPlaying = null
            return
        }
        val artist = meta.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: meta.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: meta.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
            ?: ""
        val art = meta.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: meta.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: meta.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        val source = appContext?.let { ctx ->
            runCatching {
                val pm = ctx.packageManager
                pm.getApplicationLabel(pm.getApplicationInfo(c.packageName, 0)).toString()
            }.getOrNull()
        } ?: c.packageName
        nowPlaying = NowPlaying(
            title = title,
            artist = artist,
            art = art,
            playing = state?.state == PlaybackState.STATE_PLAYING,
            durationMs = meta.getLong(MediaMetadata.METADATA_KEY_DURATION),
            positionMs = state?.position ?: 0,
            positionUpdatedAt = state?.lastPositionUpdateTime?.takeIf { it > 0 } ?: SystemClock.elapsedRealtime(),
            speed = state?.playbackSpeed?.takeIf { it > 0f } ?: 1f,
            source = source,
        )
    }

    fun playPause() {
        val c = controller ?: return
        if (c.playbackState?.state == PlaybackState.STATE_PLAYING) c.transportControls.pause() else c.transportControls.play()
    }

    fun next() {
        controller?.transportControls?.skipToNext()
    }

    fun previous() {
        controller?.transportControls?.skipToPrevious()
    }
}
