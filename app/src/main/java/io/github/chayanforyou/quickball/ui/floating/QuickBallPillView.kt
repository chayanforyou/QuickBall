package io.github.chayanforyou.quickball.ui.floating

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import io.github.chayanforyou.quickball.domain.AppPreference
import io.github.chayanforyou.quickball.utils.DensityUtils

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
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val rectF = RectF()
    private var startAngle = 0f

    // Screen edge where the pill is docked
    var onRight: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                updateArcGeometry()
                postInvalidate()
            }
        }

    // Listener for pill tap and gesture events
    var listener: GestureListener?
        get() = gestureDetector.listener
        set(value) {
            gestureDetector.listener = value
        }

    // Gesture Detector
    private val gestureDetector by lazy {
        GestureDetector(
            context = context,
            isGestureEnabled = { prefs.isGestureEnabled }
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paint.color = prefs.pillColor
        paint.strokeWidth = strokeWidthPx
        canvas.drawArc(rectF, startAngle, sweepAngle, false, paint)
    }

    fun update() {
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
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        gestureDetector.cleanup()
    }
}