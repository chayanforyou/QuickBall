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
import io.github.chayanforyou.quickball.utils.DensityUtils
import kotlin.math.hypot

/**
 * The main circular Floating Action Button (FAB).
 *
 * Renders a dark grey circular FAB that swaps between ic_menu_open and ic_menu_close when toggled,
 * and handles raw touch events for dragging, clicking, and release gestures via exposed callbacks.
 */
class QuickBallFloatingButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        const val ICON_MARGIN_DP = 9f
        private const val RIPPLE_COLOR = "#40FFFFFF"
        private const val ITEM_ICON_COLOR = "#FFFFFFFF"
        private const val ITEM_BG_COLOR = "#BF2C2C2C"
    }

    private val marginPx by lazy { DensityUtils.dp2px(ICON_MARGIN_DP) }
    private val imageView: ImageView

    // Expansion State
    var isExpanded: Boolean = false
        private set

    // Exposed Callback Listeners
    var onTouchDownListener: (() -> Unit)? = null
    var onDragMoveListener: ((dx: Float, dy: Float) -> Unit)? = null
    var onClickListener: (() -> Unit)? = null
    var onDragEndListener: (() -> Unit)? = null

    // Touch & Drag State
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    init {
        // Background shape (styled via common constants)
        val shape = GradientDrawable().apply {
            this.shape = GradientDrawable.OVAL
            setColor(ITEM_BG_COLOR.toColorInt())
        }
        background = shape

        // Circular click ripple foreground
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            foreground = RippleDrawable(
                ColorStateList.valueOf(RIPPLE_COLOR.toColorInt()),
                null,
                shape
            )
        }

        // Center menu icon ImageView
        imageView = ImageView(context).apply {
            setImageResource(R.drawable.ic_menu_open)
            setColorFilter(ITEM_ICON_COLOR.toColorInt())
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT,
                Gravity.CENTER
            ).apply {
                setMargins(marginPx, marginPx, marginPx, marginPx)
            }
        }

        addView(imageView)
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

    /*fun updateGestureExclusion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rect = Rect(0, 0, width, height)
            systemGestureExclusionRects = listOf(rect)
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        updateGestureExclusion()
    }*/

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isExpanded) return super.onTouchEvent(event)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                onTouchDownListener?.invoke()
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isDragging = false
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - initialTouchX
                val dy = event.rawY - initialTouchY

                if (!isDragging && hypot(dx, dy) > touchSlop) {
                    isDragging = true
                }

                if (isDragging) {
                    onDragMoveListener?.invoke(dx, dy)
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (!isDragging && event.action == MotionEvent.ACTION_UP) {
                    onClickListener?.invoke()
                } else if (isDragging) {
                    onDragEndListener?.invoke()
                }
                isDragging = false
                return true
            }

            else -> return super.onTouchEvent(event)
        }
    }
}
