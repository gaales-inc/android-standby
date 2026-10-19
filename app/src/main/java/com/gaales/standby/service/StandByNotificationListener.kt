package com.gaales.standby.service

import android.app.Notification
import android.app.NotificationManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.graphics.drawable.toBitmap
import com.gaales.standby.data.MediaRepository
import com.gaales.standby.data.NotificationBus
import com.gaales.standby.data.StandByNotice
import com.gaales.standby.data.StandByState

/**
 * Notification access lets StandBy show incoming notifications full screen and read the
 * active media session for the Music widget.
 */
class StandByNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        MediaRepository.onAccessChanged()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!StandByState.isShowing) return
        if (sbn.packageName == packageName) return
        val n = sbn.notification
        if (sbn.isOngoing || (n.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return
        if (n.category == Notification.CATEGORY_TRANSPORT || n.extras.containsKey(Notification.EXTRA_MEDIA_SESSION)) return
        if (n.category == Notification.CATEGORY_SERVICE || n.category == Notification.CATEGORY_PROGRESS) return

        val ranking = Ranking()
        if (currentRanking?.getRanking(sbn.key, ranking) == true &&
            ranking.importance in 0 until NotificationManager.IMPORTANCE_DEFAULT
        ) return

        val extras = n.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString()
        if (title.isNullOrBlank() && text.isNullOrBlank()) return

        val pm = packageManager
        val appName = runCatching { pm.getApplicationLabel(pm.getApplicationInfo(sbn.packageName, 0)).toString() }
            .getOrDefault(sbn.packageName)
        val icon = runCatching { pm.getApplicationIcon(sbn.packageName).toBitmap(192, 192) }.getOrNull()

        NotificationBus.post(
            StandByNotice(
                key = sbn.key,
                packageName = sbn.packageName,
                appName = appName,
                icon = icon,
                title = title,
                text = text,
                contentIntent = n.contentIntent,
                postedAt = sbn.postTime,
            )
        )
    }
}
