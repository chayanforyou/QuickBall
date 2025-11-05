package io.github.chayanforyou.quickball.ui.floating

import android.content.Context
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.RectF
import android.os.Build
import android.util.Log
import android.view.Gravity
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
    subActionItems: List<Item> = emptyList(),
    private val animationHelper: AnimationHelper? = null,
    private var stateChangeListener: MenuStateChangeListener? = null,
    private var menuItemClickListener: MenuItemClickListener? = null,
) {

    private val windowManager: WindowManager = mainActionView.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val menuRadius = dp2px(radius)
    private var isOpen = false
    private val itemParams = mutableMapOf<Item, WindowManager.LayoutParams>()
    private val subItems: List<Item> = subActionItems.onEach { item ->
        item.view.setOnClickListener {
            item.menuItem?.let { menuItemClickListener?.onMenuItemClick(it) }
        }
    }

    // ---------- Public API ----------
    fun getSubActionItems(): List<Item> = subItems

    fun isOpen(): Boolean = isOpen

    fun isAnimating(): Boolean = animationHelper?.isAnimating() == true

    fun open(animated: Boolean) {
        if (isOpen || (animated && isAnimating())) return

        val center = calculateItemPositions()

        subItems.forEach { addIndividualMenuItem(it, it.x, it.y) }

        if (animated && animationHelper != null) {
            animationHelper.animateMenuOpening(this, center)
        }

        isOpen = true
        stateChangeListener?.onMenuOpened(this)
    }

    fun close(animated: Boolean) {
        if (!isOpen || (animated && isAnimating())) return

        if (animated && animationHelper != null) {
            animationHelper.animateMenuClosing(this, getActionViewCenter())
        } else {
            subItems.forEach { removeIndividualMenuItem(it) }
        }

        isOpen = false
        stateChangeListener?.onMenuClosed(this)
    }

    fun toggle(animated: Boolean) = if (isOpen) close(animated) else open(animated)

    fun setAnimationCompletionListener(listener: (() -> Unit)?) {
        animationHelper?.setAnimationCompletionListener(listener)
    }

    inline fun doOnAnimationEnd(crossinline action: (isOpen: Boolean) -> Unit) {
        setAnimationCompletionListener {
            setAnimationCompletionListener(null)
            action(isOpen())
        }
    }

    // ---------- Center ----------
    fun getActionViewCenter(): Point {
        val coords = IntArray(2)
        mainActionView.getLocationOnScreen(coords)
        return Point(
            coords[0] + mainActionView.width / 2,
            coords[1] + mainActionView.height / 2
        )
    }

    // ---------- Position Calculation ----------
    private fun calculateItemPositions(): Point {
        val center = getActionViewCenter()
        val area = RectF(
            (center.x - menuRadius).toFloat(), (center.y - menuRadius).toFloat(),
            (center.x + menuRadius).toFloat(), (center.y + menuRadius).toFloat()
        )

        val (start, span) = if (endAngle < startAngle) {
            startAngle - 360 to endAngle - (startAngle - 360)
        } else {
            startAngle to endAngle - startAngle
        }

        val path = Path().apply { addArc(area, start.toFloat(), span.toFloat()) }
        val measure = PathMeasure(path, false)
        val totalLength = measure.length
        val count = subItems.size
        val divisor = if (abs(span) >= 360 || count <= 1) count else count - 1

        // Reuse FloatArray to avoid allocations
        val pos = FloatArray(2)
        subItems.forEachIndexed { i, item ->
            measure.getPosTan(i * totalLength / divisor, pos, null)
            // Cache width/height to avoid repeated getter calls
            val itemWidth = item.width
            val itemHeight = item.height
            item.x = pos[0].toInt() - itemWidth / 2
            item.y = pos[1].toInt() - itemHeight / 2
        }

        return center
    }

    // ---------- Item Management ----------
    private fun addIndividualMenuItem(item: Item, x: Int, y: Int) {
        if (itemParams.containsKey(item)) return

        val lp = createLayoutParams(x, y, item.width, item.height)
        itemParams[item] = lp
        try {
            windowManager.addView(item.view, lp)
        } catch (e: Exception) {
            Log.w(TAG, "Add failed: ${e.message}")
        }
    }

    fun removeIndividualMenuItem(item: Item) {
        if (!itemParams.containsKey(item)) return

        try {
            itemParams.remove(item)
            windowManager.removeView(item.view)
        } catch (e: Exception) {
            Log.w(TAG, "Remove failed: ${e.message}")
        }
    }

    private fun createLayoutParams(x: Int, y: Int, w: Int, h: Int): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            w, h,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            else @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x
            this.y = y
        }
    }

    fun updateIndividualMenuItemPosition(item: Item, x: Int, y: Int) {
        val lp = itemParams[item] ?: return
        if (!item.view.isAttachedToWindow || (lp.x == x && lp.y == y)) return

        lp.x = x
        lp.y = y
        try {
            windowManager.updateViewLayout(item.view, lp)
        } catch (e: Exception) {
            Log.w(TAG, "Update failed: ${e.message}")
        }
    }

    // ---------- Item Class ----------
    data class Item(
        val view: View,
        val action: MenuAction? = null,
        val menuItem: QuickBallMenuItemModel? = null
    ) {
        var x: Int = 0
        var y: Int = 0
        val width: Int get() = view.measuredWidth.coerceAtLeast(view.minimumWidth)
        val height: Int get() = view.measuredHeight.coerceAtLeast(view.minimumHeight)
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
        private val SIZE_PX by lazy { dp2px(52f) }
        private val MARGIN_PX by lazy { dp2px(14f) }

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
                // Pre-measure
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
            radius: Float = 94f,
            menuItems: List<Item> = emptyList(),
            animationHelper: AnimationHelper? = AnimationHelper,
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
    }
}