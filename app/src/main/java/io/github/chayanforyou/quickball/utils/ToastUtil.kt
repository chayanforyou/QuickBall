package io.github.chayanforyou.quickball.utils

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ScaleDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.getSystemService
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.isVisible
import io.github.chayanforyou.quickball.R
import io.github.chayanforyou.quickball.domain.AppPreference
import java.lang.ref.WeakReference

object ToastUtil {

    enum class ToastType(val iconRes: Int?) {
        TEXT(null),
        VOLUME(R.drawable.ic_volume_up),
        BRIGHTNESS(R.drawable.ic_brightness_up);
    }

    private class ToastViewHolder(context: Context) {
        val iconView: ImageView
        val textView: TextView
        val seekBar: SeekBar
        val rootLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(
                DensityUtils.dp2px(20f),
                DensityUtils.dp2px(12f),
                DensityUtils.dp2px(20f),
                DensityUtils.dp2px(12f)
            )
            background = GradientDrawable().apply {
                setColor(AppPreference.getInstance(context).toastBgColor)
                cornerRadius = DensityUtils.dp2px(30f).toFloat()
            }
        }

        init {
            val topRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }

            iconView = ImageView(context).apply {
                val iconSize = DensityUtils.dp2px(20f)
                layoutParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
                    marginEnd = DensityUtils.dp2px(8f)
                }
                setColorFilter(Color.WHITE)
                isVisible = false
            }

            textView = TextView(context).apply {
                setTextColor(Color.WHITE)
                textSize = 15f
                gravity = Gravity.CENTER
            }

            topRow.addView(iconView)
            topRow.addView(textView)

            seekBar = SeekBar(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    DensityUtils.dp2px(160f),
                    DensityUtils.dp2px(8f)
                ).apply {
                    topMargin = DensityUtils.dp2px(8f)
                    bottomMargin = DensityUtils.dp2px(4f)
                }
                progressDrawable = createProgressDrawable()
                thumb = Color.TRANSPARENT.toDrawable()
                thumbOffset = 0
                splitTrack = false
                setPadding(0, 0, 0, 0)
                isVisible = false
            }

            rootLayout.addView(topRow)
            rootLayout.addView(seekBar)
        }

        fun updateColors(bgColor: Int, fgColor: Int) {
            (rootLayout.background as? GradientDrawable)?.setColor(bgColor)
            textView.setTextColor(fgColor)
            iconView.setColorFilter(fgColor)
            seekBar.progressDrawable = createProgressDrawable(fgColor)
        }

        private fun createProgressDrawable(fgColor: Int = Color.WHITE): LayerDrawable {
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = DensityUtils.dp2px(6f).toFloat()
                setColor(ColorUtils.setAlphaComponent(fgColor, 77))
            }

            val progress = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = DensityUtils.dp2px(6f).toFloat()
                setColor(fgColor)
            }

            val scaledProgress = ScaleDrawable(
                progress,
                Gravity.START or Gravity.FILL_VERTICAL,
                1f, -1f
            )

            return LayerDrawable(arrayOf(bg, scaledProgress)).apply {
                setId(0, android.R.id.background)
                setId(1, android.R.id.progress)
            }
        }
    }

    private var viewHolderRef: WeakReference<ToastViewHolder>? = null
    private var windowManager: WindowManager? = null
    private var windowParams: WindowManager.LayoutParams? = null
    private var dismissRunnable: Runnable? = null
    private val handler = Handler(Looper.getMainLooper())

    private fun getWindowManager(context: Context): WindowManager {
        return windowManager ?: (context.getSystemService<WindowManager>() as WindowManager).also {
            windowManager = it
        }
    }

    private fun getOrCreateViewHolder(context: Context): ToastViewHolder {
        var holder = viewHolderRef?.get()
        if (holder == null) {
            val appContext = context.applicationContext

            holder = ToastViewHolder(appContext)
            viewHolderRef = WeakReference(holder)

            windowParams = WindowManager.LayoutParams().apply {
                type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
                }
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                width = WindowManager.LayoutParams.WRAP_CONTENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
                format = PixelFormat.TRANSLUCENT
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = DensityUtils.dp2px(72f)
            }
        }
        return holder
    }

    private fun bringToFrontIfNeeded(context: Context) {
        val wm = getWindowManager(context)
        val holder = getOrCreateViewHolder(context)
        val params = windowParams ?: return

        val isVisible = holder.rootLayout.isVisible && holder.rootLayout.isAttachedToWindow

        if (isVisible) {
            runCatching { wm.updateViewLayout(holder.rootLayout, params) }
        } else {
            if (holder.rootLayout.isAttachedToWindow) {
                runCatching { wm.removeView(holder.rootLayout) }
            }
            runCatching { wm.addView(holder.rootLayout, params) }
        }
    }

    fun show(context: Context, message: String) {
        showToastInternal(context, ToastType.TEXT, message)
    }

    private fun calculatePercentage(value: Int, max: Int): Int {
        return if (max > 0) (value * 100) / max else 0
    }

    fun showVolumeToast(
        context: Context,
        currentVolume: Int,
        maxVolume: Int,
        onVolumeChanged: ((Int) -> Unit)?
    ) {
        val percentage = calculatePercentage(currentVolume, maxVolume)
        showToastInternal(
            context = context,
            type = ToastType.VOLUME,
            text = "Volume: $percentage%",
            progress = currentVolume,
            maxProgress = maxVolume,
            onValueAdjusted = { valIndex ->
                onVolumeChanged?.invoke(valIndex)
                "Volume: ${calculatePercentage(valIndex, maxVolume)}%"
            }
        )
    }

    fun showBrightnessToast(
        context: Context,
        currentBrightness: Int,
        maxBrightness: Int,
        minBrightness: Int = 1,
        onBrightnessChanged: ((Int) -> Unit)?
    ) {
        val range = (maxBrightness - minBrightness).coerceAtLeast(1)
        val progress = currentBrightness - minBrightness
        val percentage = calculatePercentage(progress, range)

        showToastInternal(
            context = context,
            type = ToastType.BRIGHTNESS,
            text = "Brightness: $percentage%",
            progress = progress,
            maxProgress = range,
            onValueAdjusted = { progressVal ->
                onBrightnessChanged?.invoke(minBrightness + progressVal)
                "Brightness: ${calculatePercentage(progressVal, range)}%"
            }
        )
    }

    private fun showToastInternal(
        context: Context,
        type: ToastType,
        text: String,
        progress: Int = 0,
        maxProgress: Int = 0,
        onValueAdjusted: ((Int) -> String)? = null
    ) {
        val holder = getOrCreateViewHolder(context)
        val prefs = AppPreference.getInstance(context)
        holder.updateColors(prefs.toastBgColor, prefs.toastFgColor)

        holder.textView.text = text

        if (type == ToastType.TEXT) {
            holder.iconView.isVisible = false
            holder.seekBar.isVisible = false
            holder.seekBar.setOnSeekBarChangeListener(null)
        } else {
            val iconRes = type.iconRes
            if (iconRes != null) {
                holder.iconView.setImageResource(iconRes)
                holder.iconView.isVisible = true
            } else {
                holder.iconView.isVisible = false
            }

            holder.seekBar.isVisible = true
            holder.seekBar.max = maxProgress.coerceAtLeast(1)
            holder.seekBar.progress = progress.coerceIn(0, holder.seekBar.max)

            holder.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progressVal: Int, fromUser: Boolean) {
                    if (fromUser) {
                        val newText = onValueAdjusted?.invoke(progressVal)
                        if (newText != null) {
                            holder.textView.text = newText
                        }
                        scheduleDismiss(holder.rootLayout, 1500L)
                    }
                }

                override fun onStartTrackingTouch(sb: SeekBar?) {
                    dismissRunnable?.let(handler::removeCallbacks)
                    holder.rootLayout.animate().cancel()
                    holder.rootLayout.alpha = 1f
                }

                override fun onStopTrackingTouch(sb: SeekBar?) {
                    scheduleDismiss(holder.rootLayout, 1500L)
                }
            })
        }

        bringToFrontIfNeeded(context)
        showView(holder.rootLayout)
    }

    private fun showView(view: View, delayMs: Long = 1500L) {
        view.isVisible = true
        view.alpha = 1f
        view.animate().cancel()
        scheduleDismiss(view, delayMs)
    }

    private fun scheduleDismiss(view: View, delayMs: Long) {
        dismissRunnable?.let(handler::removeCallbacks)
        Runnable {
            view.animate()
                .alpha(0f)
                .setDuration(250L)
                .withEndAction { view.isVisible = false }
                .start()
        }.also {
            dismissRunnable = it
            handler.postDelayed(it, delayMs)
        }
    }

    fun destroy() {
        dismissRunnable?.let(handler::removeCallbacks)
        viewHolderRef?.get()?.let { holder ->
            runCatching {
                holder.rootLayout.animate().cancel()
                windowManager?.removeView(holder.rootLayout)
            }
        }
        viewHolderRef?.clear()
        windowManager = null
        windowParams = null
        dismissRunnable = null
    }
}