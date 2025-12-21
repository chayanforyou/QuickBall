package io.github.chayanforyou.quickball.helpers

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import io.github.chayanforyou.quickball.domain.handlers.MenuAction
import io.github.chayanforyou.quickball.domain.models.QuickBallMenuItemModel

object AnalyticsHelper {

    private var firebaseAnalytics: FirebaseAnalytics? = null

    fun initialize(context: Context) {
        firebaseAnalytics = FirebaseAnalytics.getInstance(context)
    }

    /**
     * Track shortcut usage with detailed parameters
     */
    fun trackShortcutUsage(context: Context, menuItem: QuickBallMenuItemModel) {
        firebaseAnalytics?.let { analytics ->
            val bundle = Bundle().apply {
                putString("shortcut_title", menuItem.getTitle(context))
                putString("system_action", getActionCategory(menuItem.action))
                menuItem.packageName?.let { putString("target_package", it) }
                putLong("timestamp", System.currentTimeMillis())
            }

            analytics.logEvent("shortcut_used", bundle)
        }
    }

    private fun getActionCategory(action: MenuAction): String {
        return when (action) {
            MenuAction.VOLUME_UP, MenuAction.VOLUME_DOWN -> "volume"
            MenuAction.BRIGHTNESS_UP, MenuAction.BRIGHTNESS_DOWN -> "brightness"
            MenuAction.WIFI_TOGGLE, MenuAction.BLUETOOTH_TOGGLE, MenuAction.MOBILE_DATA_TOGGLE -> "connectivity"
            MenuAction.SILENT_TOGGLE, MenuAction.VIBRATE_TOGGLE -> "sound_profile"
            MenuAction.HOME, MenuAction.BACK, MenuAction.RECENT -> "navigation"
            MenuAction.LOCK_SCREEN -> "lock_screen"
            MenuAction.SCREENSHOT -> "screenshot"
            MenuAction.TORCH_TOGGLE -> "torch"
            MenuAction.LAUNCH_APP -> "app_launch"
        }
    }
}