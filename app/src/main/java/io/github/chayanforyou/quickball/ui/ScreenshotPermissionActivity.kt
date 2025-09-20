package io.github.chayanforyou.quickball.ui

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.util.Log
import io.github.chayanforyou.quickball.utils.ScreenshotUtils

class ScreenshotPermissionActivity : Activity() {
    
    companion object {
        private const val TAG = "ScreenshotPermission"
        private const val REQUEST_CODE_SCREENSHOT = 1001
    }
    
    private lateinit var screenshotUtils: ScreenshotUtils
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        screenshotUtils = ScreenshotUtils(this)
        requestScreenshotPermission()
    }
    
    private fun requestScreenshotPermission() {
        try {
            val mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val intent = mediaProjectionManager.createScreenCaptureIntent()
            startActivityForResult(intent, REQUEST_CODE_SCREENSHOT)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request screenshot permission", e)
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SCREENSHOT) {
            if (resultCode == RESULT_OK && data != null) {
                screenshotUtils.takeScreenshotWithMediaProjection(resultCode, data) { success, message ->
                    if (success) {
                        Log.i(TAG, "Screenshot taken successfully: $message")
                    } else {
                        Log.e(TAG, "Failed to take screenshot: $message")
                    }
                }
            } else {
                Log.w(TAG, "User denied screenshot permission")
            }
        }
    }
}
