package io.github.chayanforyou.quickball.utils

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import java.lang.ref.WeakReference
import androidx.core.graphics.toColorInt

object ToastUtil {

    private var textViewRef: WeakReference<TextView>? = null
    private var windowManager: WindowManager? = null
    private var runnable: Runnable? = null
    private val handler = Handler(Looper.getMainLooper())

    private fun getWindowManager(context: Context): WindowManager {
        return windowManager ?: (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).also {
            windowManager = it
        }
    }

    fun show(context: Context, message: String) {
        val wm = getWindowManager(context)
        var textView = textViewRef?.get()

        if (textView == null) {
            val appContext = context.applicationContext
            val density = appContext.resources.displayMetrics.density

            textView = TextView(appContext).apply {
                setTextColor(Color.WHITE)
                textSize = 14f
                setPadding(
                    (16 * density).toInt(),
                    (10 * density).toInt(),
                    (16 * density).toInt(),
                    (10 * density).toInt()
                )
                gravity = Gravity.CENTER

                background = GradientDrawable().apply {
                    setColor("#BF2C2C2C".toColorInt())
                    cornerRadius = 24f * density
                }
            }

            val params = WindowManager.LayoutParams().apply {
                type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
                }
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                width = WindowManager.LayoutParams.WRAP_CONTENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
                format = PixelFormat.TRANSLUCENT
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = (72 * density).toInt()
            }

            try {
                wm.addView(textView, params)
                textViewRef = WeakReference(textView)
            } catch (_: Exception) {
                textViewRef = null
                return
            }
        }

        textView.text = message
        textView.visibility = View.VISIBLE
        textView.alpha = 1f
        textView.animate().cancel()

        runnable?.let(handler::removeCallbacks)
        Runnable {
            textView.animate()
                .alpha(0f)
                .setDuration(300L)
                .withEndAction { textView.visibility = View.GONE }
                .start()
        }.also {
            runnable = it
            handler.postDelayed(it, 1200L)
        }
    }

    fun destroy() {
        runnable?.let { handler.removeCallbacks(it) }
        textViewRef?.get()?.let { textView ->
            runCatching {
                textView.animate().cancel()
                windowManager?.removeView(textView)
            }
        }
        textViewRef?.clear()
        windowManager = null
        runnable = null
    }
}