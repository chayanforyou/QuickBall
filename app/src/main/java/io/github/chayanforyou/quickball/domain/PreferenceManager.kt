package io.github.chayanforyou.quickball.domain

import android.content.Context
import androidx.core.content.edit

object PreferenceManager {
    private const val PREFS_NAME = "quick_ball_prefs"
    private const val KEY_QUICK_BALL_ENABLED = "quick_ball_enabled"

    fun isQuickBallEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_QUICK_BALL_ENABLED, false)
    }

    fun setQuickBallEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_QUICK_BALL_ENABLED, enabled) }
    }
}