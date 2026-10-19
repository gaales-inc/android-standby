package com.gaales.standby.data

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class StandByNotice(
    val key: String,
    val packageName: String,
    val appName: String,
    val icon: Bitmap?,
    val title: String?,
    val text: String?,
    val contentIntent: PendingIntent?,
    val postedAt: Long,
)

/** Carries notifications from the listener service to whichever StandBy surface is on screen. */
object NotificationBus {
    private val _notices = MutableSharedFlow<StandByNotice>(extraBufferCapacity = 8)
    val notices: SharedFlow<StandByNotice> = _notices.asSharedFlow()

    fun post(notice: StandByNotice) {
        _notices.tryEmit(notice)
    }

    fun isAccessGranted(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
}
