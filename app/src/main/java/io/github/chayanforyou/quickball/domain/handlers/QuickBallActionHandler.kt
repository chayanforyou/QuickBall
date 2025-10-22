package io.github.chayanforyou.quickball.domain.handlers

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
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
import android.widget.Toast

class QuickBallActionHandler(
    private val accessibilityService: AccessibilityService,
    private val onCloseMenu: (() -> Unit)? = null
) : MenuActionHandler {

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
        Toast.makeText(accessibilityService, message, Toast.LENGTH_SHORT).show()
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

    override fun onMenuAction(action: MenuAction) {
        when (action) {
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
            MenuAction.TORCH_TOGGLE -> toggleTorch()
            MenuAction.HOME -> performHomeAction()
            MenuAction.BACK -> performBackAction()
            MenuAction.RECENT -> performMenuAction()
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
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set brightness", e)
        }
    }

    // -------------------- Silent Mode --------------------
    private fun toggleSilentMode() {
        val mode = audioManager.ringerMode
        audioManager.ringerMode =
            if (mode == AudioManager.RINGER_MODE_NORMAL) AudioManager.RINGER_MODE_SILENT else AudioManager.RINGER_MODE_NORMAL
    }

    // -------------------- Torch --------------------
    private fun toggleTorch() {
        try {
            val cameraManager = accessibilityService.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull {
                cameraManager.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return
            torchOn = !torchOn
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraManager.setTorchMode(cameraId, torchOn)
            } else {
                showToast("Torch is not supported on this device.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Torch toggle failed", e)
        }
    }

    // -------------------- Connectivity --------------------
    private fun toggleWifi() {
        val context = accessibilityService.applicationContext
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                val panelIntent = Intent(Settings.Panel.ACTION_WIFI)
                panelIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(panelIntent)
            }
            else -> {
                runDelayed {
                    wifiManager.isWifiEnabled = !wifiManager.isWifiEnabled
                }
            }
        }
    }

    private fun toggleBluetooth() {
        val context = accessibilityService.applicationContext
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
            else -> {
                @SuppressLint("MissingPermission")
                runDelayed {
                    if (adapter.isEnabled) adapter.disable() else adapter.enable()
                }
            }
        }
    }

    private fun toggleMobileData() {
        val context = accessibilityService.applicationContext

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val intent = Intent(Settings.ACTION_DATA_ROAMING_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } else {
            showToast("Mobile data toggle not supported on this device",)
        }
    }

    // -------------------- Screenshot --------------------
    private fun performScreenshotAction() {
        try {
            onCloseMenu?.invoke()
            runDelayed {
                takeScreenshot()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to perform screenshot action", e)
        }
    }

    private fun takeScreenshot() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
            } else {
                showToast("Screenshot is not supported on this device.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to take screenshot", e)
        }
    }

    // -------------------- Lock Screen --------------------
    private fun performLockScreenAction() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
        } else {
            showToast("Lock screen not supported on this device.")
        }
    }
}