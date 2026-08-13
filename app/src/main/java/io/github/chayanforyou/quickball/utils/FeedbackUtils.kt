package io.github.chayanforyou.quickball.utils

import android.content.Context
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresApi
import io.github.chayanforyou.quickball.domain.AppPreference
import io.github.chayanforyou.quickball.domain.models.HapticIntensity

object FeedbackUtils {

    fun performHapticFeedback(context: Context) {
        val prefs = AppPreference.getInstance(context)
        if (!prefs.isHapticFeedbackEnabled) return

        val vibrator = getVibrator(context)
        if (!vibrator.hasVibrator()) return

        val intensity = runCatching {
            HapticIntensity.valueOf(prefs.hapticIntensity)
        }.getOrDefault(HapticIntensity.LIGHT)

        val durationMs = when (intensity) {
            HapticIntensity.LIGHT -> 30L
            HapticIntensity.MEDIUM -> 60L
            HapticIntensity.STRONG -> 80L
        }

        val baseAmplitude = when (intensity) {
            HapticIntensity.LIGHT -> 60
            HapticIntensity.MEDIUM -> 80
            HapticIntensity.STRONG -> 100
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val amp =
                if (vibrator.hasAmplitudeControl()) baseAmplitude else VibrationEffect.DEFAULT_AMPLITUDE
            val effect = VibrationEffect.createOneShot(durationMs, amp)
            vibrateEffect(vibrator, effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun vibrateEffect(vibrator: Vibrator, effect: VibrationEffect) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching {
                val attributes =
                    VibrationAttributes.createForUsage(VibrationAttributes.USAGE_HARDWARE_FEEDBACK)
                vibrator.vibrate(effect, attributes)
            }.onFailure {
                vibrator.vibrate(effect)
            }
        } else {
            vibrator.vibrate(effect)
        }
    }

    private fun getVibrator(context: Context): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("deprecation")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
}

fun Context.performHapticFeedback() {
    FeedbackUtils.performHapticFeedback(this)
}