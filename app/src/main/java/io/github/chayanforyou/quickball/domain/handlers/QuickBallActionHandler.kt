package io.github.chayanforyou.quickball.domain.handlers

import android.accessibilityservice.AccessibilityService
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import io.github.chayanforyou.quickball.domain.models.QuickBallMenuItemModel
import io.github.chayanforyou.quickball.helpers.AnalyticsHelper
import io.github.chayanforyou.quickball.utils.ToastUtil

class QuickBallActionHandler(
    private val accessibilityService: AccessibilityService,
    private val performStash: (() -> Unit)? = null
) : QuickBallMenuActionHandler {

    companion object {
        private const val TAG = "QuickBallActionHandler"
        private const val MAX_BRIGHTNESS = 255
        private const val MIN_BRIGHTNESS = 1
        private const val BRIGHTNESS_STEP = 15
    }

    private val audioManager: AudioManager by lazy {
        accessibilityService.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    private val handler = Handler(Looper.getMainLooper())
    private var torchOn = false

    private fun showToast(message: String) {
        ToastUtil.show(accessibilityService, message)
    }

    private inline fun runDelayed(
        delayMillis: Long = 300L,
        crossinline action: () -> Unit
    ) {
        handler.postDelayed({
            try {
                action()
            } catch (e: Exception) {
                Log.e(TAG, "Error running delayed action", e)
            }
        }, delayMillis)
    }

    override fun onMenuAction(menuItem: QuickBallMenuItemModel) {
        AnalyticsHelper.trackShortcutUsage(accessibilityService, menuItem)
        
        when (menuItem.action) {
            MenuAction.VOLUME_UP -> performVolumeUpAction()
            MenuAction.VOLUME_DOWN -> performVolumeDownAction()
            MenuAction.BRIGHTNESS_UP -> changeBrightness(true)
            MenuAction.BRIGHTNESS_DOWN -> changeBrightness(false)
            MenuAction.LOCK_SCREEN -> performLockScreenAction()
            MenuAction.SCREENSHOT -> performScreenshotAction()
            MenuAction.WIFI_TOGGLE -> toggleWifi()
            MenuAction.BLUETOOTH_TOGGLE -> toggleBluetooth()
            MenuAction.MOBILE_DATA_TOGGLE -> toggleMobileData()
            MenuAction.SILENT_TOGGLE -> toggleSilentMode()
            MenuAction.VIBRATE_TOGGLE -> toggleVibrateMode()
            MenuAction.TORCH_TOGGLE -> toggleTorch()
            MenuAction.HOME -> performHomeAction()
            MenuAction.BACK -> performBackAction()
            MenuAction.RECENT -> performMenuAction()
            MenuAction.LAUNCH_APP -> launchApp(menuItem.packageName)
        }
    }

    // -------------------- Navigation Actions --------------------
    private fun performHomeAction() {
        accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
    }

    private fun performBackAction() {
        accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
    }

    private fun performMenuAction() {
        accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
    }

    // -------------------- Volume Actions --------------------
    private fun performVolumeUpAction() {
        try {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_RAISE,
                AudioManager.FLAG_PLAY_SOUND
            )
            showToast("Volume: ${getVolumePercentage()}%")
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
            showToast("Volume: ${getVolumePercentage()}%")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to perform volume down action", e)
        }
    }

    private fun getVolumePercentage(): Int {
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        return (currentVolume * 100) / maxVolume
    }

    // -------------------- Brightness --------------------
    private fun changeBrightness(increase: Boolean) {
        if (!canModifyBrightness()) {
            Log.w(TAG, "Cannot modify brightness - permission not granted")
            return
        }

        val current = getCurrentBrightness()
        val newBrightness = if (increase) {
            (current + BRIGHTNESS_STEP).coerceAtMost(MAX_BRIGHTNESS)
        } else {
            if (current <= BRIGHTNESS_STEP) MIN_BRIGHTNESS else current - BRIGHTNESS_STEP
        }

        setBrightness(newBrightness)
    }

    private fun canModifyBrightness(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.System.canWrite(accessibilityService)
        } else {
            true
        }
    }

    private fun getCurrentBrightness(): Int {
        return try {
            Settings.System.getInt(
                accessibilityService.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS
            )
        } catch (_: Settings.SettingNotFoundException) {
            MAX_BRIGHTNESS / 2 // Default to middle brightness
        }
    }

    private fun setBrightness(brightness: Int) {
        try {
            val clampedBrightness = brightness.coerceIn(MIN_BRIGHTNESS, MAX_BRIGHTNESS)
            Settings.System.putInt(
                accessibilityService.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                clampedBrightness
            )
            showToast("Brightness: ${toPercentage(clampedBrightness)}%")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set brightness", e)
        }
    }

    private fun toPercentage(brightness: Int): Int {
        return ((brightness - MIN_BRIGHTNESS) * 100) / (MAX_BRIGHTNESS - MIN_BRIGHTNESS)
    }

    // -------------------- Silent Mode --------------------
    private fun toggleSilentMode() {
        val context = accessibilityService.applicationContext
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !notificationManager.isNotificationPolicyAccessGranted
        ) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runDelayed { context.startActivity(intent) }
            showToast("Grant Do Not Disturb access for Silent mode")
            return
        }

        val newMode = when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_SILENT
            AudioManager.RINGER_MODE_SILENT -> AudioManager.RINGER_MODE_NORMAL
            else -> AudioManager.RINGER_MODE_NORMAL
        }

        runDelayed {
            try {
                audioManager.ringerMode = newMode
                showToast(getSilentModeText(newMode))
            } catch (e: SecurityException) {
                Log.e(TAG, "Failed to toggle silent mode", e)
            }
        }
    }

    private fun getSilentModeText(mode: Int): String {
        return when (mode) {
            AudioManager.RINGER_MODE_SILENT -> "Silent mode ON"
            AudioManager.RINGER_MODE_NORMAL -> "Silent mode OFF"
            else -> "Silent mode OFF"
        }
    }


    // -------------------- Vibration Mode --------------------
    private fun toggleVibrateMode() {
        val context = accessibilityService.applicationContext
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val newMode = when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE
            AudioManager.RINGER_MODE_VIBRATE -> AudioManager.RINGER_MODE_NORMAL
            else -> AudioManager.RINGER_MODE_NORMAL
        }

        runDelayed {
            try {
                audioManager.ringerMode = newMode
                showToast(getVibrationModeText(newMode))
            } catch (e: SecurityException) {
                Log.e(TAG, "Failed to toggle vibrate mode", e)
            }
        }
    }

    private fun getVibrationModeText(mode: Int): String {
        return when (mode) {
            AudioManager.RINGER_MODE_VIBRATE -> "Vibration mode ON"
            AudioManager.RINGER_MODE_NORMAL -> "Vibration mode OFF"
            else -> "Vibration mode OFF"
        }
    }

    // -------------------- Torch --------------------
    private fun toggleTorch() {
        try {
            val cameraManager =
                accessibilityService.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull {
                cameraManager.getCameraCharacteristics(it)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return
            torchOn = !torchOn
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraManager.setTorchMode(cameraId, torchOn)
                showToast(getTorchText(torchOn))
            } else {
                showToast("Torch is not supported on this device.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Torch toggle failed", e)
        }
    }

    private fun getTorchText(isOn: Boolean): String {
        return if (isOn) "Torch ON" else "Torch OFF"
    }

    // -------------------- Connectivity --------------------
    private fun toggleWifi() {
        val context = accessibilityService.applicationContext
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager

        runDelayed {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.startActivity(Intent(Settings.Panel.ACTION_WIFI).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } else {
                @Suppress("DEPRECATION")
                wifiManager.isWifiEnabled = !wifiManager.isWifiEnabled
            }
        }
    }

    private fun toggleBluetooth() {
        val context = accessibilityService.applicationContext
        runDelayed {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } else {
                @Suppress("DEPRECATION", "MissingPermission")
                BluetoothAdapter.getDefaultAdapter()?.let { adapter ->
                    if (adapter.isEnabled) adapter.disable() else adapter.enable()
                }
            }
        }
    }

    private fun toggleMobileData() {
        val context = accessibilityService.applicationContext

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runDelayed {
                context.startActivity(Intent(Settings.ACTION_DATA_ROAMING_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
        } else {
            showToast("Mobile data toggle not supported on this device")
        }
    }

    // -------------------- Screenshot --------------------
    private fun performScreenshotAction() {
        performStash?.invoke()
        runDelayed {
            takeScreenshot()
        }
    }

    private fun takeScreenshot() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
        } else {
            showToast("Screenshot is not supported on this device.")
        }
    }

    // -------------------- Lock Screen --------------------
    private fun performLockScreenAction() {
        runDelayed {
            lockScreen()
        }
    }

    private fun lockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
        } else {
            showToast("Lock screen not supported on this device.")
        }
    }

    // -------------------- App Launch --------------------
    private fun launchApp(packageName: String?) {
        if (packageName.isNullOrBlank()) {
            Log.w(TAG, "Cannot launch app - package name is null or empty")
            return
        }

        try {
            accessibilityService.packageManager
                .getLaunchIntentForPackage(packageName)
                ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                ?.let { intent ->
                    performStash?.invoke()
                    runDelayed { accessibilityService.startActivity(intent) }
                } ?: showToast("App not found or cannot be launched")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch app: $packageName", e)
        }
    }
}