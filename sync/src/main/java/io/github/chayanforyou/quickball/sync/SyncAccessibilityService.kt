package io.github.chayanforyou.quickball.sync

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat

/**
 * Accessibility service: current app, node tree, screenshot, input.
 * Screenshot via takeScreenshot(), not MediaProjection — that way
 * FOREGROUND_SERVICE_MEDIA_PROJECTION and its PP trigger aren't needed.
 */
class SyncAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        if (!SyncState.enabled(this)) return
        runCatching {
            ContextCompat.startForegroundService(this, Intent(this, SyncService::class.java))
        }
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        currentPackage = event.packageName?.toString()
    }

    override fun onInterrupt() = Unit

    companion object {
        @Volatile
        var instance: SyncAccessibilityService? = null
            private set

        @Volatile
        var currentPackage: String? = null
            private set
    }
}