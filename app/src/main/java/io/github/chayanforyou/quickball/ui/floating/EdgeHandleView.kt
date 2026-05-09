package io.github.chayanforyou.quickball.ui.floating

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager

class EdgeHandleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var onTrigger: (() -> Unit)? = null
    var onSideChanged: ((newSide: String) -> Unit)? = null
    var isRightSide: Boolean = true
    var showPill: Boolean = true
        set(value) {
            field = value
//            updateHandle()
        }

    private val handler = Handler(Looper.getMainLooper())

    private var startX = 0f
    private var startY = 0f
    private var hasPassedThreshold = false
    private var isTriggered = false

    private val density = resources.displayMetrics.density
    private val triggerThreshold = 16 * density
    private val holdDurationMs = 250L

    // ── Drag-to-reposition state ──────────────────────────────────────────────
    private var isDragMode = false
    private var dragStartRawY = 0f
    private var dragStartWindowY = 0f    // WindowManager params.y at drag start
    private var dragStartRawX = 0f

    /** Long-press runnable: enters drag-repositioning mode */
    private val dragModeRunnable = Runnable {
        isDragMode = true
        // Grow the pill slightly to signal drag mode
        animate().scaleX(1.15f).scaleY(1.15f).setDuration(120).start()
    }

    private val holdRunnable = Runnable {
        if (!hasPassedThreshold) return@Runnable
        isTriggered = true
        onTrigger?.invoke()
        if (showPill) {
            animate().scaleX(1f).scaleY(1f).setDuration(150).start()
        }
    }

    // ── Tap Detection ─────────────────────────────────────────────────────────
    private var tapCount = 0
    private val tapTimeoutMs = ViewConfiguration.getDoubleTapTimeout().toLong()

    private fun handleTap() {
        onTrigger?.invoke()
    }

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
        setWillNotDraw(false)
        post {
            updateHandle()
        }
    }

    private fun updateLayoutSafely(params: WindowManager.LayoutParams) {
        if (isAttachedToWindow) {
            try {
                val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                wm.updateViewLayout(this, params)
            } catch (e: Exception) {}
        }
    }

    fun updateHandle() {
        isRightSide = true
        showPill = true

        val handleHeight = 80f
        val handleWidth = 32f
        val handleVerticalOffset = 0f
        val pillWidth = 5f

        val params = layoutParams as? WindowManager.LayoutParams
        if (params != null) {
            val screenH = resources.displayMetrics.heightPixels
            val safeMargin = (10 * density).toInt()

            val heightPx = if (showPill) {
                (handleHeight * density).toInt()
            } else {
                (screenH * 0.60f).toInt()
            }

            val maxOffset = (screenH / 2) - (heightPx / 2) - safeMargin

            params.apply {
//                y = handleVerticalOffset.toInt().coerceIn(-maxOffset, maxOffset)
                y = maxOffset
                width = (handleWidth * density).toInt()
                height = heightPx
            }

            updateLayoutSafely(params)
        }

        if (!showPill) {
            background = null
            alpha = 0f
            invalidate()
            return
        }

        val radius = 12 * density
        val insetPx = ((handleWidth - pillWidth).coerceAtLeast(0f) * density).toInt()

        val shape = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadii = if (isRightSide) {
                floatArrayOf(radius, radius, 0f, 0f, 0f, 0f, radius, radius)
            } else {
                floatArrayOf(0f, 0f, radius, radius, radius, radius, 0f, 0f)
            }

            setColor(Color.WHITE)
        }

        background = if (isRightSide) {
            InsetDrawable(shape, insetPx, 0, 0, 0)
        } else {
            InsetDrawable(shape, 0, 0, insetPx, 0)
        }

        alpha = 1f

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            post {
                systemGestureExclusionRects = listOf(Rect(0, 0, width, height))
            }
        }

        invalidate()
    }

    private var downTime = 0L

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (onTrigger == null) return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downTime = System.currentTimeMillis()
                return true
            }

            MotionEvent.ACTION_UP -> {
                val duration = System.currentTimeMillis() - downTime

                if (duration < ViewConfiguration.getLongPressTimeout()) {
                    handleTap()
                }

                return true
            }
        }

        return true
    }

    /** Flips the pill to the given side with a smooth animation. */
    private fun flipSide(newSide: String) {
//        if ((newSide == PanelPreferences.SIDE_RIGHT) == isRightSide) return
//
//        vibrateHaptic(30)
//        isRightSide = newSide == PanelPreferences.SIDE_RIGHT
//
//        // Update the WindowManager gravity immediately
//        val params = layoutParams as? WindowManager.LayoutParams ?: return
//        params.gravity = if (isRightSide) {
//            android.view.Gravity.END or android.view.Gravity.CENTER_VERTICAL
//        } else {
//            android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL
//        }
//
//        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
//        if (isAttachedToWindow) {
//            try { wm.updateViewLayout(this, params) } catch (e: Exception) {}
//        }

        // Flip the pill visual (left/right rounded shape)
//        updatePill()
    }

    /** Persists the current window Y position and side to preferences. */
    private fun saveFinalPosition() {
//        val params = layoutParams as? WindowManager.LayoutParams ?: return
//        val offsetDp = (params.y / density).toInt()
//        panelPrefs.handleVerticalOffset = offsetDp
//
//        // Safely notify the service that the side changed, keeping layout passes out of the drag loop
//        val newSide = if (isRightSide) PanelPreferences.SIDE_RIGHT else PanelPreferences.SIDE_LEFT
//        if (panelPrefs.panelSide != newSide) {
//            panelPrefs.panelSide = newSide
//            onSideChanged?.invoke(newSide)
//        }
    }


    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            systemGestureExclusionRects = listOf(Rect(0, 0, width, height))
        }
    }
}