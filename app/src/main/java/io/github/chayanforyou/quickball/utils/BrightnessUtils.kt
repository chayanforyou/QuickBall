package io.github.chayanforyou.quickball.utils

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Utility matching Android AOSP's Hybrid Log-Gamma (HLG) BrightnessUtils curve.
 * This provides 100% synchronization with Android's system brightness slider.
 */
object BrightnessUtils {

    const val MIN_BRIGHTNESS = 1
    const val MAX_BRIGHTNESS = 255

    private const val A = 0.17883277f
    private const val B = 0.28466892f
    private const val C = 0.55991073f
    private const val R = 0.5f
    private const val Y_MAX = 12f

    /**
     * Converts a linear brightness value to a perceptual percentage (0..100).
     */
    fun linearToPercent(valLinear: Int, min: Int = MIN_BRIGHTNESS, max: Int = MAX_BRIGHTNESS): Int {
        val range = (max - min).coerceAtLeast(1).toFloat()
        val normalized = ((valLinear - min).toFloat() / range).coerceIn(0f, 1f)
        val y = normalized * Y_MAX

        val gamma = if (y <= 1f) {
            sqrt(y) * R
        } else {
            A * ln(y - B) + C
        }

        return (gamma * 100f).roundToInt().coerceIn(0, 100)
    }

    /**
     * Converts a perceptual percentage (0..100) to a linear brightness value.
     */
    fun percentToLinear(percent: Int, min: Int = MIN_BRIGHTNESS, max: Int = MAX_BRIGHTNESS): Int {
        val gamma = (percent / 100f).coerceIn(0f, 1f)
        val y = if (gamma <= R) {
            (gamma / R) * (gamma / R)
        } else {
            exp((gamma - C) / A) + B
        }

        val normalized = (y / Y_MAX).coerceIn(0f, 1f)
        val range = (max - min).coerceAtLeast(1).toFloat()
        return (min + normalized * range).roundToInt().coerceIn(min, max)
    }
}
