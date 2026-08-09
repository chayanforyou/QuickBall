package io.github.chayanforyou.quickball.ui.floating

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.graphics.toColorInt
import io.github.chayanforyou.quickball.domain.models.MenuAction
import io.github.chayanforyou.quickball.domain.models.QuickBallMenuItem
import io.github.chayanforyou.quickball.utils.DensityUtils
import io.github.chayanforyou.quickball.utils.getAppIcon
import io.github.chayanforyou.quickball.utils.getScreenSize
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Full-screen overlay containing the fanning radial action items and dimming dismiss backdrop.
 *
 * Dynamically computes an optimal fan arc based on the FAB position relative to screen edges/corners
 * and animates items in and out with smooth overshoot/decelerate interpolators.
 */
@SuppressLint("ViewConstructor", "ClickableViewAccessibility")
class QuickBallFloatingMenu(
    context: Context,
    private val fabSize: Int,
    private val items: List<QuickBallMenuItem>,
    private val fabX: Int,
    private val fabY: Int,
    private val onDismiss: () -> Unit,
    private val onDismissFinished: () -> Unit,
    private val onMenuItemClicked: (QuickBallMenuItem) -> Unit,
) : FrameLayout(context) {

    companion object {
        const val ITEM_BUTTON_SIZE_DP = 53f
        const val SWEEP_ANGLE_DEG = 160f
        const val RADIUS_DP = 96f
        const val ICON_SIZE_DP = 22f

        private const val RIPPLE_COLOR = "#40FFFFFF"
        private const val ITEM_ICON_COLOR = "#FFFFFFFF"
        private const val ITEM_BG_COLOR = "#BF2C2C2C"

        private val OVERSHOOT_INTERPOLATOR = OvershootInterpolator(0.9f)
        private val DECELERATE_INTERPOLATOR = DecelerateInterpolator(1.0f)
    }

    private val halfFab = fabSize / 2
    private val isOnRight by lazy {
        val centerX = fabX + halfFab
        val screenWidth = context.getScreenSize().first
        centerX > screenWidth / 2
    }
    private val radiusPx = DensityUtils.dp2px(RADIUS_DP)
    private val itemHalfSize = DensityUtils.dp2px(ITEM_BUTTON_SIZE_DP) / 2
    private val buttonSizePx = DensityUtils.dp2px(ITEM_BUTTON_SIZE_DP)
    private val iconSizePx = DensityUtils.dp2px(ICON_SIZE_DP)

    private val rippleColorStateList = ColorStateList.valueOf(RIPPLE_COLOR.toColorInt())

    private val itemViews = ArrayList<FrameLayout>(items.size)
    private var isCollapsing = false

    init {
        require(items.size >= 2) { "RadialMenuOverlay requires at least 2 items" }

        // 1. Full-screen backdrop to detect tap outside
        val backdrop = View(context).apply {
            setBackgroundColor(Color.TRANSPARENT)
            setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> true // Consuming DOWN ensures UP/CANCEL are dispatched to us
                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL,
                    MotionEvent.ACTION_OUTSIDE -> {
                        if (event.action == MotionEvent.ACTION_UP) {
                            v.performClick()
                        }
                        onDismiss()
                        true
                    }
                    else -> false
                }
            }
        }
        addView(
            backdrop,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )

        // 2. Create and lay out fanning action items
        items.forEachIndexed { index, item ->
            val (targetOffX, targetOffY) = getFanningOffsets(index)

            // Item Button Container
            val itemView = FrameLayout(context).apply {
                // Circle dark background
                val shape = GradientDrawable().apply {
                    this.shape = GradientDrawable.OVAL
                    setColor(ITEM_BG_COLOR.toColorInt())
                }
                background = shape

                // Circle ripple foreground
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    foreground = RippleDrawable(
                        rippleColorStateList,
                        null,
                        shape
                    )
                }

                // Centered ImageView
                val imageView = ImageView(context).apply {
                    if (item.action == MenuAction.LAUNCH_APP && item.packageName != null) {
                        setImageDrawable(context.getAppIcon(item.packageName))
                    } else {
                        setImageResource(item.iconRes)
                        setColorFilter(ITEM_ICON_COLOR.toColorInt())
                    }
                }
                val iconParams = LayoutParams(iconSizePx, iconSizePx, Gravity.CENTER)
                addView(imageView, iconParams)

                // Click listener
                setOnClickListener {
                    if (!isCollapsing) {
                        onMenuItemClicked(item)
                    }
                }
            }

            // Lay out item centered around the target offset
            val itemParams = LayoutParams(buttonSizePx, buttonSizePx).apply {
                leftMargin = (fabX + halfFab + targetOffX - itemHalfSize).roundToInt()
                topMargin = (fabY + halfFab + targetOffY - itemHalfSize).roundToInt()
            }

            // Initially collapse to the FAB center
            itemView.translationX = -targetOffX
            itemView.translationY = -targetOffY
            itemView.scaleX = 0f
            itemView.scaleY = 0f
            itemView.alpha = 0f

            addView(itemView, itemParams)
            itemViews.add(itemView)
        }
    }

    /**
     * Helper to compute the target fanning offset relative to the central FAB.
     */
    private fun getFanningOffsets(index: Int): Pair<Float, Float> {
        val startAngle = if (isOnRight) 260f else -80f
        val sweepAngle = if (isOnRight) -SWEEP_ANGLE_DEG else SWEEP_ANGLE_DEG
        val stepAngle = sweepAngle / (items.size - 1)

        val angleDeg = startAngle + stepAngle * index
        val angleRad = Math.toRadians(angleDeg.toDouble())

        val offsetX = (cos(angleRad) * radiusPx).toFloat()
        val offsetY = (sin(angleRad) * radiusPx).toFloat()
        return Pair(offsetX, offsetY)
    }

    /**
     * Start fanning out animation
     */
    fun animateExpand() {
        isCollapsing = false
        itemViews.forEachIndexed { index, itemView ->
            itemView.animate()
                .translationX(0f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(240)
                .setStartDelay((itemViews.size - index) * 15L)
                .setInterpolator(OVERSHOOT_INTERPOLATOR)
                .start()
        }
    }

    /**
     * Start fanning in animation
     */
    fun animateCollapse() {
        isCollapsing = true
        itemViews.forEachIndexed { index, itemView ->
            val (targetOffX, targetOffY) = getFanningOffsets(index)

            itemView.animate()
                .translationX(-targetOffX)
                .translationY(-targetOffY)
                .scaleX(0f)
                .scaleY(0f)
                .alpha(0f)
                .setDuration(240)
                .setStartDelay((itemViews.size - index) * 15L)
                .setInterpolator(DECELERATE_INTERPOLATOR)
                .withEndAction {
                    if (index == 0) {
                        onDismissFinished()
                    }
                }
                .start()
        }
    }
}
