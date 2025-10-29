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
    private val subActionItems: List<Item> = mutableListOf(),
    private val animationHelper: AnimationHelper? = null,
    private var stateChangeListener: MenuStateChangeListener? = null,
    private var menuItemClickListener: MenuItemClickListener? = null,
) {

    private val windowManager: WindowManager by lazy {
        mainActionView.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }
    private val menuRadius = dp2px(radius)
    private var isOpen = false
    private val individualMenuItems = mutableMapOf<Item, WindowManager.LayoutParams>()

    init {
        animationHelper?.setMenu(this)
        subActionItems.forEach { item ->
            item.view.setOnClickListener {
                item.menuItem?.let { menuItem ->
                    menuItemClickListener?.onMenuItemClick(menuItem)
                }
            }
        }
    }

    // ---------- Public API ----------
    fun getSubActionItems(): List<Item> = subActionItems

    fun isOpen(): Boolean = isOpen

    fun isAnimating(): Boolean = animationHelper?.isAnimating() == true

    fun open(animated: Boolean) {
        if (isOpen) return // Already open
        if (animated && animationHelper?.isAnimating() == true) return
        
        val center = calculateItemPositions()

        if (animated && animationHelper != null) {
            // Validate all items are detached before animating
            subActionItems.forEach { item ->
                if (item.view.parent != null) {
                    throw RuntimeException("All of the sub action items have to be independent from a parent.")
                }
            }
            
            subActionItems.forEach { item ->
                addIndividualMenuItem(item, center.x - item.width / 2, center.y - item.height / 2)
            }
            animationHelper.animateMenuOpening(center)
        } else {
            subActionItems.forEach { item ->
                addIndividualMenuItem(item, item.x, item.y)
            }
        }

        isOpen = true
        stateChangeListener?.onMenuOpened(this)
    }

    fun close(animated: Boolean) {
        if (!isOpen) return // Already closed
        if (animated && animationHelper?.isAnimating() == true) return

        if (animated && animationHelper != null) {
            animationHelper.animateMenuClosing(getActionViewCenter())
        } else {
            subActionItems.forEach { removeIndividualMenuItem(it) }
        }

        isOpen = false
        stateChangeListener?.onMenuClosed(this)
    }

    fun toggle(animated: Boolean) = if (isOpen) close(animated) else open(animated)

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

    // ---------- Helpers & calculations ----------
    fun getActionViewCenter(): Point {
        val coords = IntArray(2)
        mainActionView.getLocationOnScreen(coords)
        return Point(
            coords[0] + mainActionView.measuredWidth / 2,
            coords[1] + mainActionView.measuredHeight / 2
        )
    }

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
        val divisor = if (abs(adjustedSpan.toDouble()) >= 360 || subActionItems.size <= 1) {
            subActionItems.size
        } else {
            subActionItems.size - 1
        }

        subActionItems.forEachIndexed { i, item ->
            val coords = FloatArray(2)
            measure.getPosTan(i * measure.length / divisor, coords, null)
            item.x = coords[0].toInt() - item.width / 2
            item.y = coords[1].toInt() - item.height / 2
        }

        return center
    }

    // ---------- Individual Menu Item Management ----------
    private fun addIndividualMenuItem(item: Item, x: Int, y: Int) {
        if (individualMenuItems.containsKey(item)) {
            return
        }

        try {
            val layoutParams = createIndividualMenuItemParams(x, y, item.width, item.height)
            individualMenuItems[item] = layoutParams
            windowManager.addView(item.view, layoutParams)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to add menu item: ${e.message}")
        }
    }

    fun removeIndividualMenuItem(item: Item) {
        if (!individualMenuItems.containsKey(item)) {
            return
        }

        try {
            windowManager.removeView(item.view)
            individualMenuItems.remove(item)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to remove menu item: ${e.message}")
        }
    }

    private fun createIndividualMenuItemParams(x: Int, y: Int, width: Int, height: Int): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            width,
            height,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
            },
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            format = PixelFormat.TRANSLUCENT
            gravity = Gravity.TOP or Gravity.START
            this.x = x
            this.y = y
        }
    }

    fun updateIndividualMenuItemPosition(item: Item, x: Int, y: Int) {
        val layoutParams = individualMenuItems[item] ?: return

        if (layoutParams.x == x && layoutParams.y == y) {
            return
        }
        
        layoutParams.x = x
        layoutParams.y = y

        try {
            windowManager.updateViewLayout(item.view, layoutParams)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update menu item position: ${e.message}")
        }
    }

    // ---------- Data Class ----------
    data class Item(val view: View, val action: MenuAction? = null, val menuItem: QuickBallMenuItemModel? = null) {
        var x: Int = 0
        var y: Int = 0
        
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

        private val sizeInPx by lazy { dp2px(52f) }
        private val margin by lazy { dp2px(14f) }

        fun create(
            context: Context,
            menuItem: QuickBallMenuItemModel,
        ): Item {
            val imageView = ImageView(context).apply {
                when {
                    menuItem.packageName != null -> {
                        val appIcon = context.getAppIcon(menuItem.packageName)
                        setImageDrawable(appIcon)
                    }
                    else -> {
                        setImageResource(menuItem.iconRes)
                    }
                }
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    Gravity.CENTER
                ).apply {
                    setMargins(margin, margin, margin, margin)
                }
            }

            return Item(
                action = menuItem.action,
                menuItem = menuItem,
                view = FrameLayout(context).apply {
                    layoutParams = FrameLayout.LayoutParams(sizeInPx, sizeInPx)
                    background = ContextCompat.getDrawable(
                        context,
                        R.drawable.floating_ball_background
                    )?.mutate()?.constantState?.newDrawable()
                    isClickable = true
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)
                    addView(imageView)
                }
            )
        }

        fun attached(
            actionView: View,
            startAngle: Int = 120,
            endAngle: Int = 240,
            radius: Float = 94f,
            menuItems: List<Item> = emptyList(),
            animationHelper: AnimationHelper? = AnimationHelper(),
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
