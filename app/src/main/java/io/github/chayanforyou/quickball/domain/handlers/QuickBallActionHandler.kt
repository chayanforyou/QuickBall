package io.github.chayanforyou.quickball.domain.handlers

import android.accessibilityservice.AccessibilityService
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import io.github.chayanforyou.quickball.core.DeviceAdminReceiver
import io.github.chayanforyou.quickball.ui.ScreenshotPermissionActivity

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

    private val devicePolicyManager: DevicePolicyManager by lazy {
        accessibilityService.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    }

    private val adminComponent: ComponentName by lazy {
        ComponentName(accessibilityService, DeviceAdminReceiver::class.java)
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
            // Switch to manual mode if currently automatic
            val currentMode = Settings.System.getInt(
                accessibilityService.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE
            )

            if (currentMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                Settings.System.putInt(
                    accessibilityService.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                )
            }

            // Set the brightness level
            Settings.System.putInt(
                accessibilityService.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                brightness
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
                performDeviceAdminLockScreen()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to perform lock screen action", e)
        }
    }

    private fun performDeviceAdminLockScreen() {
        try {
            if (devicePolicyManager.isAdminActive(adminComponent)) {
                devicePolicyManager.lockNow()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to perform device admin lock screen", e)
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
                performScreenshotForOlderDevices()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to take screenshot", e)
        }
    }

    private fun performScreenshotForOlderDevices() {
        try {
            val intent = Intent(accessibilityService, ScreenshotPermissionActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            accessibilityService.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to take screenshot", e)
        }
    }
}
