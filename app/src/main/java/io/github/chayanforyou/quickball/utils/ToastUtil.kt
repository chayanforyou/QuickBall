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

object ToastUtil {

    private var textViewRef: WeakReference<TextView>? = null
    private var windowManager: WindowManager? = null
    private var runnable: Runnable? = null
    private val handler = Handler(Looper.getMainLooper())

    private fun getWindowManager(context: Context): WindowManager {
        return windowManager ?: context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    fun show(context: Context, message: String) {
        val wm = getWindowManager(context)

        runnable?.let { handler.removeCallbacks(it) }

        var textView = textViewRef?.get()

        if (textView == null) {
            textView = TextView(context).apply {
                setPadding(42, 32, 42, 32)
                setTextColor(Color.WHITE)
                textSize = 14f
                gravity = Gravity.CENTER
                background = createBackground()
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
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = 200
                format = PixelFormat.TRANSLUCENT
            }

            try {
                wm.addView(textView, params)
                textViewRef = WeakReference(textView)
            } catch (_: Exception) {
                textViewRef = null
                return
            }
        }

        textView.apply {
            text = message
            background = createBackground()
            visibility = View.VISIBLE
        }

        runnable = Runnable {
            textView.visibility = View.GONE
        }
        handler.postDelayed(runnable!!, 1500L)
    }

    private fun createBackground(): GradientDrawable {
        return GradientDrawable().apply {
            cornerRadius = 80f
            setColor(Color.argb(0xBF, 0x2C, 0x2C, 0x2C))
        }
    }

    fun destroy() {
        runnable?.let { handler.removeCallbacks(it) }
        textViewRef?.get()?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        textViewRef?.clear()
        windowManager = null
        runnable = null
    }
}