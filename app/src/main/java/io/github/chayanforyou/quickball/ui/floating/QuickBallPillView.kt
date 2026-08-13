package io.github.chayanforyou.quickball.ui.floating

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.core.graphics.toColorInt
import io.github.chayanforyou.quickball.domain.AppPreference
import io.github.chayanforyou.quickball.utils.DensityUtils
import kotlin.math.abs
import kotlin.math.hypot

/**
 * A stashed edge handle view that renders a curved translucent arc along the left or right screen edge.
 *
 * Handles single tap, double tap, triple tap, long-press, and vertical swipe gestures via exposed callback listeners.
 */
class QuickBallPillView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val prefs by lazy { AppPreference.getInstance(context) }
    private val strokeWidthPx get() = DensityUtils.dp2px(prefs.pillThickness).toFloat()
    private val pillEdgePaddingPx = DensityUtils.dp2px(0f).toFloat()
    private val visiblePillWidthPx = DensityUtils.dp2px(10f).toFloat()
    private val arcIndentAngle get() = prefs.pillArcAngle

    // Sweep angle depends on arcIndentAngle
    private val sweepAngle get() = 180f - (2f * arcIndentAngle)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = prefs.pillColor
        style = Paint.Style.STROKE
        strokeWidth = strokeWidthPx
        strokeCap = Paint.Cap.ROUND
    }

    private val rectF = RectF()
    private var startAngle = 0f

    // Exposed Callback Listeners
    var isGestureEnabled: Boolean = false
    var onSingleTapListener: (() -> Unit)? = null
    var onDoubleTapListener: (() -> Unit)? = null
    var onTripleTapListener: (() -> Unit)? = null
    var onLongPressListener: (() -> Unit)? = null
    var onSwipeUpListener: (() -> Unit)? = null
    var onSwipeDownListener: (() -> Unit)? = null

    // Touch and Gesture State
    private val handler = Handler(Looper.getMainLooper())
    private val swipeThreshold = 24f * resources.displayMetrics.density
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    private val tapTimeoutMs = 200L
    private val longPressTimeoutMs = ViewConfiguration.getLongPressTimeout().toLong()

    private var startX = 0f
    private var startY = 0f
    private var downTime = 0L
    private var isSwipeTriggered = false
    private var isLongPressTriggered = false
    private var tapCount = 0

    private val tapRunnable = Runnable {
        when (tapCount) {
            1 -> onSingleTapListener?.invoke()
            2 -> onDoubleTapListener?.invoke()
        }
        tapCount = 0
    }

    private val longPressRunnable = Runnable {
        isLongPressTriggered = true
        handler.removeCallbacks(tapRunnable)
        tapCount = 0
        onLongPressListener?.invoke()
    }

    /**
     * Side of the screen the pill is stashed on.
     */
    var onRight: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                updateArcGeometry()
                postInvalidate()
            }
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paint.color = prefs.pillColor
        paint.strokeWidth = strokeWidthPx
        canvas.drawArc(rectF, startAngle, sweepAngle, false, paint)
    }

    fun setPillColor(color: Int) {
        paint.color = color
        postInvalidate()
    }

    fun setPillThickness(thicknessDp: Float) {
        updateArcGeometry()
        postInvalidate()
    }

    fun setPillArcAngle(angle: Float) {
        updateArcGeometry()
        postInvalidate()
    }

    private fun updateArcGeometry() {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w == 0f || h == 0f) return

        val halfStroke = strokeWidthPx / 2f
        val topBound = halfStroke
        val bottomBound = h - halfStroke

        if (onRight) {
            // Arc curves to the left (flat side on the right).
            // Position endpoints exactly pillEdgePaddingPx from the right screen edge.
            val centerX = w - pillEdgePaddingPx
            val leftBound = w - pillEdgePaddingPx - visiblePillWidthPx
            val rightBound = 2f * centerX - leftBound
            rectF.set(leftBound, topBound, rightBound, bottomBound)
            startAngle = 90f + arcIndentAngle
        } else {
            // Arc curves to the right (flat side on the left).
            // Position endpoints exactly pillEdgePaddingPx from the left screen edge.
            val centerX = pillEdgePaddingPx
            val rightBound = pillEdgePaddingPx + visiblePillWidthPx
            val leftBound = 2f * centerX - rightBound
            rectF.set(leftBound, topBound, rightBound, bottomBound)
            startAngle = 270f + arcIndentAngle
        }
    }

    fun updateGestureExclusion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rect = Rect(0, 0, width, height)
            systemGestureExclusionRects = listOf(rect)
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        updateArcGeometry()
        //updateGestureExclusion()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isGestureEnabled) {
            return if (event.action == MotionEvent.ACTION_UP) {
                onSingleTapListener?.invoke()
                true
            } else {
                false
            }
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.rawX
                startY = event.rawY
                downTime = System.currentTimeMillis()
                isSwipeTriggered = false
                isLongPressTriggered = false
                handler.removeCallbacks(longPressRunnable)
                handler.postDelayed(longPressRunnable, longPressTimeoutMs)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (isSwipeTriggered || isLongPressTriggered) return true

                val dx = event.rawX - startX
                val dy = event.rawY - startY

                if (hypot(dx, dy) > touchSlop) {
                    handler.removeCallbacks(longPressRunnable)
                }

                // Check if vertical swipe is predominant and passes threshold
                if (abs(dy) > swipeThreshold && abs(dy) > abs(dx) * 2f) {
                    isSwipeTriggered = true
                    handler.removeCallbacks(longPressRunnable)

                    // Cancel any pending tap scheduler tasks
                    handler.removeCallbacks(tapRunnable)
                    tapCount = 0

                    if (dy < 0) {
                        onSwipeUpListener?.invoke()
                    } else {
                        onSwipeDownListener?.invoke()
                    }
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(longPressRunnable)
                if (!isSwipeTriggered && !isLongPressTriggered && event.action == MotionEvent.ACTION_UP) {
                    val duration = System.currentTimeMillis() - downTime
                    if (duration < ViewConfiguration.getLongPressTimeout()) {
                        tapCount++
                        handler.removeCallbacks(tapRunnable)
                        if (tapCount >= 3) {
                            onTripleTapListener?.invoke()
                            tapCount = 0
                        } else {
                            handler.postDelayed(tapRunnable, tapTimeoutMs)
                        }
                    }
                }
                isSwipeTriggered = false
                isLongPressTriggered = false
                return true
            }

            else -> return super.onTouchEvent(event)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacks(longPressRunnable)
        handler.removeCallbacks(tapRunnable)
    }
}