package io.github.chayanforyou.quickball.ui.floating

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import io.github.chayanforyou.quickball.R
import io.github.chayanforyou.quickball.domain.handlers.MenuAction
import io.github.chayanforyou.quickball.domain.models.QuickBallMenuItemModel
import io.github.chayanforyou.quickball.helpers.AnimationHelper
import io.github.chayanforyou.quickball.utils.WidgetUtil.dp2px
import io.github.chayanforyou.quickball.utils.getAppIcon
import kotlin.math.abs

class QuickBallFloatingMenu(
    private val mainActionView: View,
    private val startAngle: Int,
    private val endAngle: Int,
    private val radius: Float,
    subActionItems: List<Item> = mutableListOf(),
    private val animationHelper: AnimationHelper? = null,
    private var stateChangeListener: MenuStateChangeListener? = null,
    private var menuItemClickListener: MenuItemClickListener? = null,
) {

    private val windowManager: WindowManager by lazy {
        mainActionView.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    private var overlayContainer: FrameLayout? = null
    private val menuRadius = dp2px(radius)
    private var isOpen = false
    private val subItems: List<Item> = subActionItems.onEach { item ->
        item.view.setOnClickListener {
            item.menuItem?.let { menuItemClickListener?.onMenuItemClick(it) }
        }
    }

    init {
        animationHelper?.setMenu(this)
        subItems.filter { it.width <= 0 || it.height <= 0 }
            .forEach { addViewToOverlayTemporarily(it) }
    }

    // ---------- Public API ----------
    fun getSubActionItems(): List<Item> = subItems

    fun getOverlayContainer(): FrameLayout? = overlayContainer

    fun isOpen(): Boolean = isOpen

    fun isAnimating(): Boolean = animationHelper?.isAnimating() == true

    fun setAnimationCompletionListener(listener: (() -> Unit)?) {
        animationHelper?.setAnimationCompletionListener(listener)
    }

    inline fun doOnAnimationEnd(
        crossinline action: (isOpen: Boolean) -> Unit
    ) {
        setAnimationCompletionListener {
            setAnimationCompletionListener(null)
            action(isOpen())
        }
    }

    fun open(animated: Boolean) {
        if (isOpen || (animated && isAnimating())) return

        val center = calculateItemPositions()
        attachOverlayContainer()

        val overlayParams = overlayContainer?.layoutParams as? WindowManager.LayoutParams

        if (animated && animationHelper != null) {
            subItems.forEach { item ->
                if (item.view.parent != null) {
                    throw RuntimeException("Sub action item must not have a parent view.")
                }
                val params = FrameLayout.LayoutParams(item.width, item.height, Gravity.TOP or Gravity.START)
                params.setMargins(
                    center.x - (overlayParams?.x ?: 0) - item.width / 2,
                    center.y - (overlayParams?.y ?: 0) - item.height / 2,
                    0, 0
                )
                addViewToCurrentContainer(item.view, params)
            }
            animationHelper.animateMenuOpening(center)
        } else {
            subItems.forEach { item ->
                val params = FrameLayout.LayoutParams(item.width, item.height, Gravity.TOP or Gravity.START)
                params.setMargins(
                    item.x - (overlayParams?.x ?: 0),
                    item.y - (overlayParams?.y ?: 0),
                    0, 0
                )
                item.view.layoutParams = params
                addViewToCurrentContainer(item.view, params)
            }
        }

        isOpen = true
        stateChangeListener?.onMenuOpened(this)
    }

    fun close(animated: Boolean) {
        if (!isOpen || (animated && isAnimating())) return

        if (animated && animationHelper != null) {
            animationHelper.animateMenuClosing(getActionViewCenter())
        } else {
            removeAllSubActionViews()
            detachOverlayContainer()
        }

        isOpen = false
        stateChangeListener?.onMenuClosed(this)
    }

    fun toggle(animated: Boolean) {
        if (isOpen) {
            close(animated)
        } else {
            open(animated)
        }
    }

    // ---------- Center & Coordinates ----------
    fun getActionViewCenter(): Point {
        val coords = getActionViewCoordinates()
        return Point(
            coords.x + mainActionView.measuredWidth / 2,
            coords.y + mainActionView.measuredHeight / 2
        )
    }

    private fun getActionViewCoordinates(): Point {
        val coords = IntArray(2)
        mainActionView.getLocationOnScreen(coords)
        return Point(coords[0], coords[1])
    }

    // ---------- Position Calculation ----------
    private fun calculateItemPositions(): Point {
        val center = getActionViewCenter()
        val area = RectF(
            (center.x - menuRadius).toFloat(),
            (center.y - menuRadius).toFloat(),
            (center.x + menuRadius).toFloat(),
            (center.y + menuRadius).toFloat()
        )

        val (adjustedStartAngle, adjustedSpan) = if (endAngle < startAngle) {
            Pair(startAngle - 360, endAngle - (startAngle - 360))
        } else {
            Pair(startAngle, endAngle - startAngle)
        }

        val orbit = Path().apply { addArc(area, adjustedStartAngle.toFloat(), adjustedSpan.toFloat()) }
        val measure = PathMeasure(orbit, false)
        val divisor = if (abs(adjustedSpan) >= 360 || subItems.size <= 1) {
            subItems.size
        } else {
            subItems.size - 1
        }

        subItems.forEachIndexed { i, item ->
            val pos = FloatArray(2)
            measure.getPosTan(i * measure.length / divisor, pos, null)
            item.x = pos[0].toInt() - item.width / 2
            item.y = pos[1].toInt() - item.height / 2
        }

        return center
    }

    // ---------- Overlay Management ----------
    private var buttonBounds: Rect? = null

    @SuppressLint("ClickableViewAccessibility")
    private fun ensureOverlayContainer(): FrameLayout {
        if (overlayContainer == null) {
            overlayContainer = FrameLayout(mainActionView.context).apply {
                isClickable = true
                isFocusable = false
                setOnTouchListener { v, event ->
                    when (event.action) {
                        MotionEvent.ACTION_UP,
                        MotionEvent.ACTION_CANCEL,
                        MotionEvent.ACTION_OUTSIDE -> {
                            close(true)
                            true
                        }
                        else -> false
                    }
                }
            }
        }
        return overlayContainer!!
    }

    private fun attachOverlayContainer() {
        try {
            val container = ensureOverlayContainer()
            updateButtonBounds()
            val overlayParams = calculateOverlayContainerParams()
            container.layoutParams = overlayParams

            if (container.parent == null) {
                windowManager.addView(container, overlayParams)
            } else {
                windowManager.updateViewLayout(container, overlayParams)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach overlay: ${e.message}")
        }
    }

    fun detachOverlayContainer() {
        overlayContainer?.let { container ->
            try {
                windowManager.removeView(container)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to detach overlay: ${e.message}")
            }
        }
        overlayContainer = null
        buttonBounds?.setEmpty()
    }

    private fun removeAllSubActionViews() {
        subItems.forEach { item ->
            try {
                removeViewFromCurrentContainer(item.view)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to remove view from parent: ${e.message}")
            }
        }
    }

    private fun updateButtonBounds() {
        val bounds = buttonBounds ?: return
        val location = IntArray(2)
        mainActionView.getLocationOnScreen(location)

        val containerLocation = IntArray(2)
        overlayContainer?.getLocationOnScreen(containerLocation) ?: return

        val width = mainActionView.measuredWidth
        val height = mainActionView.measuredHeight

        if (width > 0 && height > 0) {
            val relativeX = location[0] - containerLocation[0]
            val relativeY = location[1] - containerLocation[1]
            bounds.set(relativeX, relativeY, relativeX + width, relativeY + height)
        } else {
            bounds.setEmpty()
        }
    }

    private fun calculateOverlayContainerParams(): WindowManager.LayoutParams {
        val displayMetrics = mainActionView.context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        return getDefaultSystemWindowParams().apply {
            width = screenWidth
            height = screenHeight
            x = 0
            y = 0
            gravity = Gravity.TOP or Gravity.START
        }
    }

    private fun addViewToCurrentContainer(view: View, layoutParams: ViewGroup.LayoutParams) {
        overlayContainer?.addView(view, layoutParams)
    }

    fun removeViewFromCurrentContainer(view: View) {
        overlayContainer?.removeView(view)
    }

    private fun addViewToOverlayTemporarily(item: Item) {
        val container = ensureOverlayContainer()
        if (item.view.parent != null) return

        item.view.alpha = 0f
        container.addView(item.view)

        item.view.post(object : Runnable {
            var attempts = 0
            val maxAttempts = 10
            override fun run() {
                val w = item.view.measuredWidth
                val h = item.view.measuredHeight
                if ((w == 0 || h == 0) && attempts < maxAttempts) {
                    attempts++
                    item.view.postDelayed(this, 16)
                    return
                }
                item.alpha = item.view.alpha
                item.view.alpha = item.alpha
                container.removeView(item.view)
            }
        })
    }

    // ---------- Item Class ----------
    data class Item(
        val view: View,
        val action: MenuAction? = null,
        val menuItem: QuickBallMenuItemModel? = null
    ) {
        var x: Int = 0
        var y: Int = 0
        var alpha: Float = view.alpha

        val width: Int
            get() = if (view.measuredWidth > 0) view.measuredWidth else view.layoutParams?.width ?: 0

        val height: Int
            get() = if (view.measuredHeight > 0) view.measuredHeight else view.layoutParams?.height ?: 0
    }

    interface MenuStateChangeListener {
        fun onMenuOpened(menu: QuickBallFloatingMenu)
        fun onMenuClosed(menu: QuickBallFloatingMenu)
    }

    interface MenuItemClickListener {
        fun onMenuItemClick(menuItem: QuickBallMenuItemModel)
    }

    companion object {
        private const val TAG = "QuickBallFloatingMenu"
        private val SIZE_PX by lazy { dp2px(53f) }
        private val MARGIN_PX by lazy { dp2px(15f) }

        fun create(context: Context, menuItem: QuickBallMenuItemModel): Item {
            val iconView = ImageView(context).apply {
                menuItem.packageName?.let { setImageDrawable(context.getAppIcon(it)) }
                    ?: setImageResource(menuItem.iconRes)
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    Gravity.CENTER
                ).apply { setMargins(MARGIN_PX, MARGIN_PX, MARGIN_PX, MARGIN_PX) }
            }

            val container = FrameLayout(context).apply {
                layoutParams = FrameLayout.LayoutParams(SIZE_PX, SIZE_PX)
                background = ContextCompat.getDrawable(context, R.drawable.floating_ball_background)
                    ?.mutate()?.constantState?.newDrawable()
                isClickable = true
                addView(iconView)
                measure(
                    View.MeasureSpec.makeMeasureSpec(SIZE_PX, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(SIZE_PX, View.MeasureSpec.EXACTLY)
                )
            }

            return Item(
                view = container,
                action = menuItem.action,
                menuItem = menuItem
            )
        }

        fun attached(
            actionView: View,
            startAngle: Int = 120,
            endAngle: Int = 240,
            radius: Float = 96f,
            menuItems: List<Item> = emptyList(),
            animationHelper: AnimationHelper? = null,
            stateChangeListener: MenuStateChangeListener? = null,
            menuItemClickListener: MenuItemClickListener? = null
        ) = QuickBallFloatingMenu(
            mainActionView = actionView,
            startAngle = startAngle,
            endAngle = endAngle,
            radius = radius,
            subActionItems = menuItems,
            animationHelper = animationHelper,
            stateChangeListener = stateChangeListener,
            menuItemClickListener = menuItemClickListener
        )

        fun getDefaultSystemWindowParams() = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
            },
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
    }
}