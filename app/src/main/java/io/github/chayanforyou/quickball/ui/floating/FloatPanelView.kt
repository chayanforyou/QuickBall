package io.github.chayanforyou.quickball.ui.floating

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
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
import io.github.chayanforyou.quickball.domain.models.MenuConnectorStyle
import io.github.chayanforyou.quickball.domain.models.QuickBallMenuItem
import io.github.chayanforyou.quickball.utils.DensityUtils
import io.github.chayanforyou.quickball.utils.getAppIcon
import io.github.chayanforyou.quickball.utils.getScreenSize
import kotlin.math.hypot

/**
 * Full-screen overlay displaying the radial menu items.
 *
 * Fans out action items from the floating ball position (adapting to left/right edge)
 * and animates them with overshoot and decelerate interpolators. Up to [FanLayout.SINGLE_RING_MAX]
 * items sit on one arc; more items zigzag between an outer and an inner arc.
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

        private const val SCREEN_MARGIN_DP = 8f

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
    private val borderWidth by lazy { DensityUtils.dp2px(prefs.menuBorderWidth) }

    // Floating Ball Geometry
    private val halfFab = fabSize / 2
    private val fabCenterX = fabX + halfFab
    private val fabCenterY = fabY + halfFab
    private val isOnRight by lazy {
        val (screenWidth, _) = context.getScreenSize()
        fabCenterX > (screenWidth / 2)
    }

    // Connectors between the ball and each item
    private val connectors = ConnectorRenderer(
        style = runCatching { MenuConnectorStyle.valueOf(prefs.menuConnectorStyle) }
            .getOrDefault(MenuConnectorStyle.DOTTED),
        color = prefs.menuConnectorColor,
        strokeWidth = DensityUtils.dp2px(prefs.menuConnectorWidth).toFloat()
    )
    private val connectorStart = halfFab + DensityUtils.dp2px(ConnectorRenderer.INSET_DP).toFloat()

    // Fan geometry for the connectors, set while computing the fan
    private var arcRadius = 0f
    private var arcEdgeAngle = 0f
    private var fanShiftY = 0f
    private var ringStart = 0.0
    private var ringSweep = 0.0
    private var curveBends = emptyList<Float>()

    // Views & Animation State
    private val itemViews = ArrayList<FrameLayout>(items.size)
    private val itemOffsets: List<Pair<Float, Float>>
    private var isCollapsing = false
    private var hasDismissed = false

    init {
        require(items.size >= 2) {
            "FloatPanelView requires at least 2 items"
        }

        clipChildren = false
        clipToPadding = false

        itemOffsets = computeFanningOffsets()
        FanLayout.angleRange(itemOffsets).let { (start, sweep) ->
            ringStart = start
            ringSweep = sweep
        }
        curveBends = FanLayout.curveBends(itemOffsets)

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
                    if (borderWidth > 0) setStroke(borderWidth, prefs.menuBorderColor)
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
            val (offsetX, offsetY) = itemOffsets[index]

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
     * Computes the target offset of every item relative to the central FAB, see [FanLayout].
     */
    private fun computeFanningOffsets(): List<Pair<Float, Float>> {
        val (_, screenHeight) = context.getScreenSize()
        val margin = DensityUtils.dp2px(SCREEN_MARGIN_DP)

        val layout = FanLayout.compute(
            count = items.size,
            buttonSize = buttonSize.toFloat(),
            radius = radius.toFloat(),
            halfFab = halfFab.toFloat(),
            density = resources.displayMetrics.density,
            maxHalfSpan = if (screenHeight > 0) {
                (screenHeight - buttonSize) / 2f - margin
            } else Float.MAX_VALUE
        )

        val offsets = layout.offsets.map { (offsetX, offsetY) ->
            Pair(if (isOnRight) offsetX else -offsetX, offsetY)
        }

        fanShiftY = getScreenFitShift(offsets.minOf { it.second }, offsets.maxOf { it.second })

        arcEdgeAngle = layout.arcEdgeAngle
        arcRadius = layout.arcRadius

        return offsets.map { (offsetX, offsetY) -> Pair(offsetX, offsetY + fanShiftY) }
    }

    /**
     * Vertical shift that keeps the whole fan on screen when the FAB is near the top or bottom.
     */
    private fun getScreenFitShift(minOffsetY: Float, maxOffsetY: Float): Float {
        val (_, screenHeight) = context.getScreenSize()
        if (screenHeight <= 0) return 0f

        val margin = DensityUtils.dp2px(SCREEN_MARGIN_DP)
        val halfButton = buttonSize / 2f
        val top = fabCenterY + minOffsetY - halfButton
        val bottom = fabCenterY + maxOffsetY + halfButton

        return when {
            top < margin -> margin - top
            bottom > screenHeight - margin -> screenHeight - margin - bottom
            else -> 0f
        }
    }

    /**
     * Draw the connectors under the items, following the item animation
     */
    override fun dispatchDraw(canvas: Canvas) {
        when (connectors.style) {
            MenuConnectorStyle.NONE -> Unit
            MenuConnectorStyle.ARC -> drawArcConnectors(canvas)
            else -> drawRadialConnectors(canvas)
        }

        super.dispatchDraw(canvas)
    }

    /**
     * A connector from the FAB to each item, plus the ring around the FAB for ring styles.
     */
    private fun drawRadialConnectors(canvas: Canvas) {
        val centerX = fabCenterX.toFloat()
        val centerY = fabCenterY.toFloat()

        if (connectors.hasRing) {
            connectors.drawRing(
                canvas,
                centerX,
                centerY,
                connectorStart,
                ringStart,
                ringSweep,
                groupOpacity()
            )
        }

        itemViews.forEachIndexed { index, itemView ->
            val (offsetX, offsetY) = itemOffsets[index]

            // Follow the item while it animates
            val deltaX = offsetX + itemView.translationX
            val deltaY = offsetY + itemView.translationY
            val distance = hypot(deltaX, deltaY)
            if (distance <= 0f) return@forEachIndexed

            connectors.drawConnector(
                canvas = canvas,
                centerX = centerX,
                centerY = centerY,
                directionX = deltaX / distance,
                directionY = deltaY / distance,
                start = connectorStart,
                end = distance - buttonSize / 2f * itemView.scaleX,
                opacity = itemView.alpha,
                bend = curveBends.getOrElse(index) { 0f }
            )
        }
    }

    /**
     * An arc around the fan center, from straight above it to the last item, with a short
     * stub from the arc to each item.
     */
    private fun drawArcConnectors(canvas: Canvas) {
        val centerX = fabCenterX.toFloat()
        val centerY = fabCenterY + fanShiftY

        // Canvas angles: 0° points right, 270° points up, clockwise positive
        val startAngle = if (isOnRight) 180f - arcEdgeAngle else 270f
        connectors.drawRing(
            canvas, centerX, centerY, arcRadius,
            Math.toRadians(startAngle.toDouble()),
            Math.toRadians(90.0 + arcEdgeAngle),
            groupOpacity()
        )

        itemViews.forEachIndexed { index, itemView ->
            val (offsetX, offsetY) = itemOffsets[index]

            // Stub direction is fixed by the resting position of the item
            val restX = offsetX
            val restY = offsetY - fanShiftY
            val restDistance = hypot(restX, restY)
            if (restDistance <= 0f) return@forEachIndexed

            val distance = hypot(restX + itemView.translationX, restY + itemView.translationY)

            connectors.drawConnector(
                canvas = canvas,
                centerX = centerX,
                centerY = centerY,
                directionX = restX / restDistance,
                directionY = restY / restDistance,
                start = arcRadius,
                end = distance - buttonSize / 2f * itemView.scaleX,
                opacity = itemView.alpha
            )
        }
    }

    /**
     * Opacity of the ring or arc shared by all items: it follows the least visible item,
     * and while collapsing it is gone by the time that item is half faded.
     */
    private fun groupOpacity(): Float {
        val minAlpha = itemViews.minOf { it.alpha }
        return (if (isCollapsing) minAlpha * 2f - 1f else minAlpha).coerceIn(0f, 1f)
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
                .setUpdateListener { invalidate() }
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
            val (offsetX, offsetY) = itemOffsets[index]

            val animator = itemView.animate()
                .translationX(-offsetX)
                .translationY(-offsetY)
                .scaleX(0f)
                .scaleY(0f)
                .alpha(0f)
                .setDuration(ANIMATION_DURATION)
                .setStartDelay((lastIndex - index) * STAGGER_DELAY)
                .setInterpolator(DECELERATE)
                .setUpdateListener { invalidate() }

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
            val (offsetX, offsetY) = itemOffsets[index]

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