package io.github.chayanforyou.quickball.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.core.content.edit
import java.util.concurrent.TimeUnit

object SupportPrompter {
    private const val LAUNCHES_UNTIL_PROMPT = 10L
    private const val DAYS_UNTIL_PROMPT = 7L
    private const val DAYS_BETWEEN_PROMPTS = 30L

    private const val PREF_NAME = "quickball_support_prompt"
    private const val DONT_SHOW_AGAIN = "dont_show_again"
    private const val LAUNCH_COUNT = "launch_count"
    private const val FIRST_LAUNCH_TIME = "first_launch_time"
    private const val LAST_SHOWN_TIME = "last_shown_time"

    @JvmStatic
    fun initSupportPrompter(context: Context, onShowPrompt: () -> Unit) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(DONT_SHOW_AGAIN, false)) {
            return
        }

        val now = System.currentTimeMillis()
        val launchCount = prefs.getLong(LAUNCH_COUNT, 0L) + 1
        val firstLaunchTime = prefs.getLong(FIRST_LAUNCH_TIME, 0L).takeIf { it != 0L } ?: now
        prefs.edit {
            putLong(LAUNCH_COUNT, launchCount)
            putLong(FIRST_LAUNCH_TIME, firstLaunchTime)
        }

        val lastShownTime = prefs.getLong(LAST_SHOWN_TIME, 0L)
        if (launchCount >= LAUNCHES_UNTIL_PROMPT &&
            now - firstLaunchTime >= TimeUnit.DAYS.toMillis(DAYS_UNTIL_PROMPT) &&
            now - lastShownTime >= TimeUnit.DAYS.toMillis(DAYS_BETWEEN_PROMPTS)
        ) {
            // Runs after the rating prompt check, so both never open together
            Handler(Looper.getMainLooper()).postDelayed({
                onShowPrompt()
            }, 1000)
        }
    }

    fun remindLater(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit {
            putLong(LAST_SHOWN_TIME, System.currentTimeMillis())
        }
    }

    fun setDontShowAgain(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit {
            putBoolean(DONT_SHOW_AGAIN, true)
        }
    }
}
