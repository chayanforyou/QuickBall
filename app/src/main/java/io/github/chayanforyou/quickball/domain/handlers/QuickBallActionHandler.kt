package io.github.chayanforyou.quickball.domain.handlers

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast

class QuickBallActionHandler(
    private val accessibilityService: AccessibilityService,
    private val onCloseMenu: (() -> Unit)? = null
) : MenuActionHandler {

    companion object {
        private const val TAG = "QuickBallActionHandler"
        private const val MAX_BRIGHTNESS = 255
        private const val MIN_BRIGHTNESS = 0
        private const val BRIGHTNESS_STEP = 5
    }

    private val audioManager: AudioManager by lazy {
        accessibilityService.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    override fun onMenuAction(action: MenuAction) {
        when (action) {
            MenuAction.VOLUME_UP -> performVolumeUpAction()
            MenuAction.VOLUME_DOWN -> performVolumeDownAction()
            MenuAction.BRIGHTNESS_UP -> performBrightnessUpAction()
            MenuAction.BRIGHTNESS_DOWN -> performBrightnessDownAction()
            MenuAction.LOCK_SCREEN -> performLockScreenAction()
            MenuAction.TAKE_SCREENSHOT -> performScreenshotAction()
        }
    }

    private fun performVolumeUpAction() {
        try {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_RAISE,
                AudioManager.FLAG_PLAY_SOUND
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to perform volume up action", e)
        }
    }

    private fun performVolumeDownAction() {
        try {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_LOWER,
                AudioManager.FLAG_PLAY_SOUND
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to perform volume down action", e)
        }
    }

    private fun performBrightnessUpAction() {
        if (!canModifyBrightness()) {
            Log.w(TAG, "Cannot modify brightness - permission not granted")
            return
        }

        val currentBrightness = getCurrentBrightness()
        val newBrightness = (currentBrightness + BRIGHTNESS_STEP).coerceAtMost(MAX_BRIGHTNESS)
        setBrightness(newBrightness)
    }

    private fun performBrightnessDownAction() {
        if (!canModifyBrightness()) {
            Log.w(TAG, "Cannot modify brightness - permission not granted")
            return
        }

        val currentBrightness = getCurrentBrightness()
        val newBrightness = (currentBrightness - BRIGHTNESS_STEP).coerceAtLeast(MIN_BRIGHTNESS)
        setBrightness(newBrightness)
    }

    private fun getCurrentBrightness(): Int {
        return try {
            Settings.System.getInt(
                accessibilityService.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS
            )
        } catch (_: Settings.SettingNotFoundException) {
            125 // Default to middle brightness
        }
    }

    private fun canModifyBrightness(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.System.canWrite(accessibilityService)
        } else {
            true // For older versions
        }
    }

    private fun setBrightness(brightness: Int) {
        try {
            val clampedBrightness = brightness.coerceIn(0, 255)

            Settings.System.putInt(
                accessibilityService.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                clampedBrightness
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set brightness", e)
        }
    }

    private fun performLockScreenAction() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
            } else {
                Toast.makeText(accessibilityService, "Locking screen is not supported on this device.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to perform lock screen action", e)
        }
    }

    private fun performScreenshotAction() {
        try {
            onCloseMenu?.invoke()
            Handler(Looper.getMainLooper()).postDelayed({
                takeScreenshot()
            }, 300L)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to perform screenshot action", e)
        }
    }
    
    private fun takeScreenshot() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
            } else {
                Toast.makeText(accessibilityService.applicationContext, "Taking screenshot is not supported on this device.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to take screenshot", e)
        }
    }
}
