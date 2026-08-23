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
import android.view.KeyEvent
import androidx.core.net.toUri
import io.github.chayanforyou.quickball.domain.models.MenuAction
import io.github.chayanforyou.quickball.domain.models.QuickBallMenuItem
import io.github.chayanforyou.quickball.utils.BrightnessUtils
import io.github.chayanforyou.quickball.utils.ToastUtil
import io.github.chayanforyou.quickball.utils.performHapticFeedback

class QuickBallActionHandler(
    private val accessibilityService: AccessibilityService,
    private val performStash: (() -> Unit)? = null
) {

    companion object {
        private const val TAG = "QuickBallActionHandler"
        private const val MAX_BRIGHTNESS = 255
        private const val MIN_BRIGHTNESS = 1
        private const val BRIGHTNESS_STEP_PERCENT = 10
    }

    private val context: Context = accessibilityService.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private var isTorchOn = false

    private val cameraManager: CameraManager by lazy {
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    private val torchCallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                isTorchOn = enabled
            }

            override fun onTorchModeUnavailable(cameraId: String) {
                isTorchOn = false
            }
        }
    } else null

    init {
        initTorch()
    }

    private fun initTorch() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && torchCallback != null) {
            try {
                cameraManager.registerTorchCallback(torchCallback, null)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register torch callback", e)
            }
        }
    }

    fun cleanup() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && torchCallback != null) {
            try {
                cameraManager.unregisterTorchCallback(torchCallback)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unregister torch callback", e)
            }
        }
    }

    private val audioManager: AudioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    private val wifiManager: WifiManager by lazy {
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    private inline fun runDelayed(
        delayMillis: Long = 200L,
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

    private fun showToast(message: String, performHaptic: Boolean = false) {
        if (performHaptic) {
            runDelayed { context.performHapticFeedback() }
        }
        ToastUtil.show(accessibilityService, message)
    }

    private fun canWriteSettings() =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                Settings.System.canWrite(context)

    private fun requestSystemSettingsPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return

        try {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = "package:${context.packageName}".toUri()
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
            showToast("Allow 'Modify system settings' permission", performHaptic = true)
        } catch (_: Exception) {
            showToast("Could not request system settings permission")
        }
    }

    fun onMenuAction(menuItem: QuickBallMenuItem) {
        when (menuItem.action) {
            MenuAction.VOLUME_UP -> performVolumeUpAction()
            MenuAction.VOLUME_DOWN -> performVolumeDownAction()
            MenuAction.BRIGHTNESS_UP -> changeBrightness(increase = true)
            MenuAction.BRIGHTNESS_DOWN -> changeBrightness(increase = false)
            MenuAction.LOCK_SCREEN -> performLockScreenAction()
            MenuAction.SCREENSHOT -> performScreenshotAction()
            MenuAction.WIFI_TOGGLE -> toggleWifi()
            MenuAction.BLUETOOTH_TOGGLE -> toggleBluetooth()
            MenuAction.MOBILE_DATA_TOGGLE -> toggleMobileData()
            MenuAction.DND_TOGGLE -> toggleDndMode()
            MenuAction.VIBRATE_TOGGLE -> toggleVibrateMode()
            MenuAction.MEDIA_PLAY_PAUSE -> mediaPlayPause()
            MenuAction.MEDIA_NEXT -> mediaNext()
            MenuAction.MEDIA_PREVIOUS -> mediaPrevious()
            MenuAction.VOLUME_BAR -> showVolume()
            MenuAction.VOLUME_PANEL -> openVolumePanel()
            MenuAction.TORCH_TOGGLE -> toggleTorch()
            MenuAction.AUTO_ROTATE_TOGGLE -> toggleAutoRotate()
            MenuAction.AIRPLANE_MODE_TOGGLE -> toggleAirplaneMode()
            MenuAction.HOME -> performHomeAction()
            MenuAction.BACK -> performBackAction()
            MenuAction.RECENT -> performMenuAction()
            MenuAction.NOTIFICATION -> performNotificationAction()
            MenuAction.QUICK_SETTINGS -> performQuickSettingsAction()
            MenuAction.POWER_DIALOG -> performPowerDialogAction()
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

    private fun performNotificationAction() {
        accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
    }

    private fun performQuickSettingsAction() {
        accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)
    }

    private fun performPowerDialogAction() {
        accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG)
    }

    // -------------------- Volume Actions --------------------
    private fun performVolumeUpAction() {
        try {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_RAISE,
                AudioManager.FLAG_PLAY_SOUND
            )
            showVolumeToast()
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
            showVolumeToast()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to perform volume down action", e)
        }
    }

    private fun showVolumeToast() {
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

        ToastUtil.showVolumeToast(
            context = accessibilityService,
            currentVolume = currentVolume,
            maxVolume = maxVolume,
            onVolumeChanged = { newVol ->
                try {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to set volume via slider", e)
                }
            }
        )
    }

    // -------------------- Media Controls --------------------
    private fun sendMediaKeyEvent(keyCode: Int) {
        val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
        val upEvent = KeyEvent(KeyEvent.ACTION_UP, keyCode)
        audioManager.dispatchMediaKeyEvent(downEvent)
        audioManager.dispatchMediaKeyEvent(upEvent)
    }

    private fun mediaPlayPause() {
        sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
    }

    private fun mediaNext() {
        sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_NEXT)
    }

    private fun mediaPrevious() {
        sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
    }

    private fun openVolumePanel() {
        runDelayed {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.startActivity(Intent(Settings.Panel.ACTION_VOLUME).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } else {
                context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
        }
    }

    private fun showVolume() {
        performStash?.invoke()
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_SAME,
            AudioManager.FLAG_SHOW_UI
        )
    }

    // -------------------- Brightness --------------------
    private fun changeBrightness(increase: Boolean) {
        if (!canWriteSettings()) {
            requestSystemSettingsPermission()
            return
        }

        val current = getCurrentBrightness()
        val currentPercent = BrightnessUtils.linearToPercent(current, MIN_BRIGHTNESS, MAX_BRIGHTNESS)

        val newPercent = if (increase) {
            (currentPercent + BRIGHTNESS_STEP_PERCENT).coerceAtMost(100)
        } else {
            (currentPercent - BRIGHTNESS_STEP_PERCENT).coerceAtLeast(0)
        }

        val newBrightness = BrightnessUtils.percentToLinear(newPercent, MIN_BRIGHTNESS, MAX_BRIGHTNESS)
        setBrightness(newBrightness)
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
            showBrightnessToast()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set brightness", e)
        }
    }

    private fun showBrightnessToast() {
        val current = getCurrentBrightness()
        ToastUtil.showBrightnessToast(
            context = accessibilityService,
            currentBrightness = current,
            maxBrightness = MAX_BRIGHTNESS,
            minBrightness = MIN_BRIGHTNESS,
            onBrightnessChanged = { newBrightness ->
                if (canWriteSettings()) {
                    try {
                        val clamped = newBrightness.coerceIn(MIN_BRIGHTNESS, MAX_BRIGHTNESS)
                        Settings.System.putInt(
                            accessibilityService.contentResolver,
                            Settings.System.SCREEN_BRIGHTNESS,
                            clamped
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to set brightness via slider", e)
                    }
                } else {
                    requestSystemSettingsPermission()
                }
            }
        )
    }

    // -------------------- Do Not Disturb (DND) Mode --------------------
    private fun toggleDndMode() {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !notificationManager.isNotificationPolicyAccessGranted
        ) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runDelayed { context.startActivity(intent) }
            showToast("Grant Do Not Disturb access")
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
                showToast(getDndModeText(newMode))
            } catch (e: SecurityException) {
                Log.e(TAG, "Failed to toggle DND mode", e)
            }
        }
    }

    private fun getDndModeText(mode: Int): String {
        return when (mode) {
            AudioManager.RINGER_MODE_SILENT -> "Do Not Disturb ON"
            AudioManager.RINGER_MODE_NORMAL -> "Do Not Disturb OFF"
            else -> "Do Not Disturb OFF"
        }
    }

    // -------------------- Vibration Mode --------------------
    private fun toggleVibrateMode() {
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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            showToast("Torch is not supported on this device.", performHaptic = true)
            return
        }

        try {
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return

            val newState = !isTorchOn
            cameraManager.setTorchMode(cameraId, newState)
            showToast(if (newState) "Torch ON" else "Torch OFF")
        } catch (e: Exception) {
            Log.e(TAG, "Torch toggle failed", e)
        }
    }

    // -------------------- Connectivity --------------------
    private fun toggleWifi() {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runDelayed {
                context.startActivity(Intent(Settings.ACTION_DATA_ROAMING_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
        } else {
            showToast("Mobile data toggle not supported on this device", performHaptic = true)
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
            showToast("Screenshot is not supported on this device.", performHaptic = true)
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
            showToast("Lock screen not supported on this device.", performHaptic = true)
        }
    }

    // -------------------- Auto Rotate --------------------
    private fun toggleAutoRotate() {
        if (!canWriteSettings()) {
            requestSystemSettingsPermission()
            return
        }

        val current = Settings.System.getInt(
            accessibilityService.contentResolver,
            Settings.System.ACCELEROMETER_ROTATION, 0
        )
        val newValue = if (current == 1) 0 else 1
        Settings.System.putInt(
            accessibilityService.contentResolver,
            Settings.System.ACCELEROMETER_ROTATION,
            newValue
        )
        showToast(if (newValue == 1) "Auto-rotate ON" else "Auto-rotate OFF")
    }

    // -------------------- Airplane Mode --------------------
    private fun toggleAirplaneMode() {
        runDelayed {
            context.startActivity(
                Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
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