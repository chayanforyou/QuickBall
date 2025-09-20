package io.github.chayanforyou.quickball.utils

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjectionManager
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import androidx.core.graphics.createBitmap
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScreenshotUtils(private val context: Context) {

    companion object {
        private const val TAG = "ScreenshotUtils"
        private const val VIRTUAL_DISPLAY_NAME = "Screenshot"
    }

    /**
     * Takes a screenshot using MediaProjection API (for older devices)
     * This method requires the result from startActivityForResult
     */
    fun takeScreenshotWithMediaProjection(
        resultCode: Int,
        data: Intent?,
        callback: (Boolean, String?) -> Unit
    ) {
        val mediaProjectionManager =
            context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        try {
            val mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data!!)
            val metrics = Resources.getSystem().displayMetrics
            val imageReader = ImageReader.newInstance(
                metrics.widthPixels,
                metrics.heightPixels,
                PixelFormat.RGBA_8888,
                1
            )

            val virtualDisplay = mediaProjection?.createVirtualDisplay(
                VIRTUAL_DISPLAY_NAME,
                metrics.widthPixels,
                metrics.heightPixels,
                metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.surface,
                null,
                null
            )

            imageReader.setOnImageAvailableListener({
                val image = imageReader.acquireLatestImage() ?: return@setOnImageAvailableListener
                val bitmap = imageToBitmap(image, metrics)

                val filePath = saveBitmapToFile(bitmap)
                if (filePath != null) {
                    callback(true, filePath)
                } else {
                    callback(false, "Failed to save screenshot")
                }

                image.close()
                imageReader.close()
                virtualDisplay?.release()
                mediaProjection?.stop()

            }, Handler(Looper.getMainLooper()))
        } catch (e: Exception) {
            callback(false, "Failed to get media projection: ${e.message}")
        }
    }

    private fun imageToBitmap(image: Image, metrics: DisplayMetrics): Bitmap {
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * image.width

        val bitmap =
            createBitmap(metrics.widthPixels + rowPadding / pixelStride, metrics.heightPixels)
        bitmap.copyPixelsFromBuffer(buffer)
        return bitmap
    }

    private fun saveBitmapToFile(bitmap: Bitmap): String? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "Screenshot_$timestamp.png"

            val picturesDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val screenshotsDir = File(picturesDir, "QuickBall")

            if (!screenshotsDir.exists()) {
                screenshotsDir.mkdirs()
            }

            val file = File(screenshotsDir, fileName)
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()

            file.absolutePath
        } catch (e: IOException) {
            Log.e(TAG, "Failed to save bitmap to file", e)
            null
        }
    }
}
