package io.github.chayanforyou.quickball.ui.floating

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.graphics.toColorInt
import io.github.chayanforyou.quickball.R
import io.github.chayanforyou.quickball.domain.AppPreference
import io.github.chayanforyou.quickball.utils.DensityUtils
import kotlin.math.hypot

/**
 * The main circular Floating Action Button (FAB).
 *
 * Renders a dark grey circular FAB that swaps between ic_menu_open and ic_menu_close when toggled,
 * and handles raw touch events for dragging, clicking, and release gestures via exposed callbacks.
 */
class FloatTouchView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        const val ICON_MARGIN_DP = 16f
        private val RIPPLE_COLOR = ColorStateList.valueOf("#40FFFFFF".toColorInt())
    }

    private val prefs by lazy { AppPreference.getInstance(context) }
    private val iconSize get() = DensityUtils.dp2px(prefs.ballSize - ICON_MARGIN_DP)

    private val imageView = ImageView(context).apply {
        setImageResource(R.drawable.ic_menu_open)
    }

    // Whether the radial menu is currently expanded
    var isExpanded: Boolean = false
        private set

    // Listener for touch, drag, and gesture events
    var listener: GestureListener?
        get() = gestureDetector.listener
        set(value) {
            gestureDetector.listener = value
        }

    // Touch & Drag State
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    private var startX = 0f
    private var startY = 0f
    private var isDragging = false

    // Gesture Detector (used when position is locked and stick to edge is disabled)
    private val gestureDetector by lazy {
        GestureDetector(
            context = context,
            isGestureEnabled = {
                prefs.isGestureEnabled
                        && !prefs.isStickToEdgeEnabled
            }
        )
    }

    init {
        addView(imageView)
        update()
    }

    /**
     * Update floating ball appearance and size
     */
    fun update() {
        setBallColor(prefs.ballColor)
        setBallIconColor(prefs.ballIconColor)
        updateBallSize()
    }

    /**
     * Update the background color of the floating ball.
     */
    fun setBallColor(color: Int) {
        background = GradientDrawable().apply {
            this.shape = GradientDrawable.OVAL
            setColor(color)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            foreground = RippleDrawable(
                RIPPLE_COLOR,
                null,
                background
            )
        }
    }

    /**
     * Update the icon color of the floating ball.
     */
    fun setBallIconColor(color: Int) {
        imageView.setColorFilter(color)
    }

    /**
     * Update the icon size
     */
    fun updateBallSize() {
        imageView.layoutParams = LayoutParams(
            iconSize, iconSize, Gravity.CENTER
        )
    }

    /**
     * Set the expansion state, animate rotation, and swap the drawable.
     */
    fun setExpanded(expanded: Boolean, animate: Boolean = true) {
        this.isExpanded = expanded
        val targetRotation = if (expanded) 180f else 0f
        val drawableRes = if (expanded) R.drawable.ic_menu_close else R.drawable.ic_menu_open

        if (animate) {
            if (expanded) {
                imageView.setImageResource(R.drawable.ic_menu_close)
            }
            imageView.animate()
                .rotation(targetRotation)
                .setDuration(300L)
                .withEndAction {
                    if (!expanded) {
                        imageView.setImageResource(R.drawable.ic_menu_open)
                    }
                }
                .start()
        } else {
            imageView.rotation = targetRotation
            imageView.setImageResource(drawableRes)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isExpanded) return super.onTouchEvent(event)

        return if (prefs.isLockBallPositionEnabled) {
            gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
        } else {
            handleDragTouchEvent(event)
        }
    }

    private fun handleDragTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                listener?.onTouchDown()
                startX = event.rawX
                startY = event.rawY
                isDragging = false
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - startX
                val dy = event.rawY - startY

                if (!isDragging && hypot(dx, dy) > touchSlop) {
                    isDragging = true
                }

                if (isDragging) {
                    listener?.onDragMove(dx, dy)
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                if (isDragging) {
                    listener?.onDragEnd()
                } else {
                    listener?.onSingleTap()
                }
                isDragging = false
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    listener?.onDragEnd()
                }
                listener?.onTouchCancel()
                isDragging = false
                return true
            }

            else -> return super.onTouchEvent(event)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        gestureDetector.cleanup()
    }
}
