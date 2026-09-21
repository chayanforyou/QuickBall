package io.github.chayanforyou.quickball.ui.floating

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.graphics.toColorInt
import io.github.chayanforyou.quickball.domain.AppPreference
import io.github.chayanforyou.quickball.domain.models.MenuAction
import io.github.chayanforyou.quickball.domain.models.QuickBallMenuItem
import io.github.chayanforyou.quickball.utils.DensityUtils
import io.github.chayanforyou.quickball.utils.getAppIcon
import io.github.chayanforyou.quickball.utils.getScreenSize
import kotlin.math.cos
import kotlin.math.sin

/**
 * Full-screen overlay displaying the radial menu items.
 *
 * Fans out action items from the floating ball position (adapting to left/right edge)
 * and animates them with overshoot and decelerate interpolators.
 */
@SuppressLint("ViewConstructor")
class FloatPanelView(
    context: Context,
    fabX: Int,
    fabY: Int,
    fabSize: Int,
    private val items: List<QuickBallMenuItem>,
    private val onDismiss: () -> Unit,
    private val onDismissFinished: () -> Unit,
    private val onMenuItemClicked: (QuickBallMenuItem) -> Unit
) : ViewGroup(context) {

    companion object {
        private val RIPPLE_COLOR = ColorStateList.valueOf("#40FFFFFF".toColorInt())

        private const val START_ANGLE = 100f
        private const val END_ANGLE = 160f

        private const val ANIMATION_DURATION = 240L
        private const val STAGGER_DELAY = 15L

        private val OVERSHOOT = OvershootInterpolator(0.9f)
        private val DECELERATE = DecelerateInterpolator(1.0f)
    }

    // Preferences & Dimensions
    private val prefs by lazy { AppPreference.getInstance(context) }
    private val buttonSize by lazy { DensityUtils.dp2px(prefs.menuSize) }
    private val iconSize by lazy { DensityUtils.dp2px(prefs.menuIconSize) }
    private val radius by lazy { DensityUtils.dp2px(prefs.menuRadius) }

    // Floating Ball Geometry
    private val halfFab = fabSize / 2
    private val fabCenterX = fabX + halfFab
    private val fabCenterY = fabY + halfFab
    private val isOnRight by lazy {
        val (screenWidth, _) = context.getScreenSize()
        fabCenterX > (screenWidth / 2)
    }

    // Views & Animation State
    private val itemViews = ArrayList<FrameLayout>(items.size)
    private var isCollapsing = false
    private var hasDismissed = false

    init {
        require(items.size >= 2) {
            "FloatPanelView requires at least 2 items"
        }

        clipChildren = false
        clipToPadding = false

        setupTouchHandling()
        createChildViews()
    }

    /**
     * Set up touch handling to dismiss the menu
     */
    private fun setupTouchHandling() {
        setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> true
                MotionEvent.ACTION_UP -> {
                    view.performClick()
                    onDismiss()
                    true
                }

                MotionEvent.ACTION_CANCEL,
                MotionEvent.ACTION_OUTSIDE -> {
                    onDismiss()
                    true
                }

                else -> false
            }
        }
    }

    /**
     * Create and initialize action item views
     */
    private fun createChildViews() {
        items.forEachIndexed { index, item ->

            val itemView = FrameLayout(context).apply {
                // Circle dark background
                background = GradientDrawable().apply {
                    this.shape = GradientDrawable.OVAL
                    setColor(prefs.menuColor)
                }

                // Circle ripple foreground
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    foreground = RippleDrawable(
                        RIPPLE_COLOR,
                        null,
                        background
                    )
                }

                // Centered ImageView
                val imageView = ImageView(context).apply {
                    if (item.action == MenuAction.LAUNCH_APP && item.packageName != null) {
                        setImageDrawable(context.getAppIcon(item.packageName))
                    } else {
                        setImageResource(item.iconRes)
                        setColorFilter(prefs.menuIconColor)
                    }
                }

                val iconParams = FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER)
                addView(imageView, iconParams)

                setOnClickListener {
                    if (!isCollapsing) {
                        onMenuItemClicked(item)
                    }
                }
            }

            // Initially collapse to the FAB center
            val (offsetX, offsetY) = getFanningOffsets(index)

            itemView.translationX = -offsetX
            itemView.translationY = -offsetY
            itemView.scaleX = 0f
            itemView.scaleY = 0f
            itemView.alpha = 0f

            val itemParams = LayoutParams(buttonSize, buttonSize)
            addView(itemView, itemParams)
            itemViews.add(itemView)
        }
    }

    /**
     * Helper to compute the target fanning offset relative to the central FAB.
     */
    private fun getFanningOffsets(index: Int): Pair<Float, Float> {
        val stepAngle = END_ANGLE / (items.size - 1)

        val angleDeg = START_ANGLE + stepAngle * index
        val angleRad = Math.toRadians(angleDeg.toDouble())

        val offsetX = (cos(angleRad) * radius).toFloat()
        val offsetY = (-sin(angleRad) * radius).toFloat()

        return Pair(
            if (isOnRight) offsetX else -offsetX,
            offsetY
        )
    }

    /**
     * Start fanning out animation
     */
    fun animateExpand() {
        isCollapsing = false
        hasDismissed = false

        val lastIndex = itemViews.size - 1

        itemViews.forEachIndexed { index, itemView ->
            itemView.animate()
                .translationX(0f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(ANIMATION_DURATION)
                .setStartDelay((lastIndex - index) * STAGGER_DELAY)
                .setInterpolator(OVERSHOOT)
                .start()
        }
    }

    /**
     * Start fanning in animation
     */
    fun animateCollapse() {
        if (isCollapsing) return
        isCollapsing = true

        val lastIndex = itemViews.size - 1

        itemViews.forEachIndexed { index, itemView ->
            val (offsetX, offsetY) = getFanningOffsets(index)

            val animator = itemView.animate()
                .translationX(-offsetX)
                .translationY(-offsetY)
                .scaleX(0f)
                .scaleY(0f)
                .alpha(0f)
                .setDuration(ANIMATION_DURATION)
                .setStartDelay((lastIndex - index) * STAGGER_DELAY)
                .setInterpolator(DECELERATE)

            if (index == 0) {
                animator.withEndAction {
                    if (!hasDismissed) {
                        hasDismissed = true
                        onDismissFinished()
                    }
                }
            }

            animator.start()
        }
    }

    /**
     * View Measurement & Layout
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        measureChildren(widthMeasureSpec, heightMeasureSpec)
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        itemViews.forEachIndexed { index, child ->
            val (offsetX, offsetY) = getFanningOffsets(index)

            val centerX = (fabCenterX + offsetX).toInt()
            val centerY = (fabCenterY + offsetY).toInt()

            val halfWidth = child.measuredWidth / 2
            val halfHeight = child.measuredHeight / 2

            child.layout(
                centerX - halfWidth,
                centerY - halfHeight,
                centerX + halfWidth,
                centerY + halfHeight
            )
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        itemViews.forEach { it.animate().cancel() }
    }
}