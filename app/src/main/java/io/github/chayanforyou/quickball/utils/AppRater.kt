package io.github.chayanforyou.quickball.utils

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.core.content.edit
import androidx.core.net.toUri

object AppRater {
    private const val DAYS_UNTIL_PROMPT = 2L     // Min number of days
    private const val LAUNCHES_UNTIL_PROMPT = 4L // Min number of launches

    private const val PREF_NAME = "quickball_apprater"
    private const val DONT_SHOW_AGAIN = "dont_show_again"
    private const val LAUNCH_COUNT = "launch_count"
    private const val FIRST_LAUNCH_TIME = "first_launch_time"

    const val DEVELOPER_EMAIL = "chayanmistrry@gmail.com"

    @JvmStatic
    fun initAppRater(context: Context, onShowPrompt: () -> Unit) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(DONT_SHOW_AGAIN, false)) {
            return
        }

        prefs.edit {
            // Increment launch counter
            val launchCount = prefs.getLong(LAUNCH_COUNT, 0L) + 1
            putLong(LAUNCH_COUNT, launchCount)

            // Get date of first launch
            var firstLaunchTime = prefs.getLong(FIRST_LAUNCH_TIME, 0L)
            if (firstLaunchTime == 0L) {
                firstLaunchTime = System.currentTimeMillis()
                putLong(FIRST_LAUNCH_TIME, firstLaunchTime)
            }

            // Wait at least n days before opening
            if (launchCount >= LAUNCHES_UNTIL_PROMPT) {
                if (System.currentTimeMillis() >= firstLaunchTime + DAYS_UNTIL_PROMPT * 24 * 60 * 60 * 1000L) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        onShowPrompt()
                    }, 600)
                }
            }
        }
    }

    fun setDontShowAgain(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(DONT_SHOW_AGAIN, true) }
    }

    fun remindLater(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit { putLong(FIRST_LAUNCH_TIME, System.currentTimeMillis()) }
    }

    fun openPlayStore(context: Context) {
        setDontShowAgain(context)
        val packageName = context.packageName
        val marketUri = "market://details?id=$packageName".toUri()
        val webUri = "https://play.google.com/store/apps/details?id=$packageName".toUri()

        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, marketUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }.onFailure {
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, webUri).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }
        }
    }

    fun openEmail(context: Context) {
        val subject = "Quick Ball - Feedback & Suggestions"

        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = "mailto:".toUri()
            putExtra(Intent.EXTRA_EMAIL, arrayOf(DEVELOPER_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, subject)
        }

        val targetIntent = if (emailIntent.resolveActivity(context.packageManager) != null) {
            emailIntent
        } else {
            Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822"
                putExtra(Intent.EXTRA_EMAIL, arrayOf(DEVELOPER_EMAIL))
                putExtra(Intent.EXTRA_SUBJECT, subject)
            }
        }

        runCatching {
            context.startActivity(
                Intent.createChooser(targetIntent, "Send email").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }
}
