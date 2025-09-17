package io.github.chayanforyou.quickball

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
import io.github.chayanforyou.quickball.animation.AnimationHandler
import io.github.chayanforyou.quickball.utils.WidgetUtil.dp2px
import kotlin.math.abs

class FloatingActionMenu(
    private val mainActionView: View,
    private val startAngle: Int,
    private val endAngle: Int,
    private val radius: Float,
    private val subActionItems: List<Item> = mutableListOf(),
    private val animationHandler: AnimationHandler? = null,
    private var stateChangeListener: MenuStateChangeListener? = null,
    private var menuItemClickListener: MenuItemClickListener? = null,
) {

    private val windowManager: WindowManager by lazy {
        mainActionView.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }
    private val menuRadius = dp2px(radius)
    private var isOpen = false
    private val individualMenuItems = mutableMapOf<Item, WindowManager.LayoutParams>()

    fun getSubActionItems(): List<Item> = subActionItems
    fun isOpen(): Boolean = isOpen

    init {
        animationHandler?.setMenu(this)
        subActionItems.forEach { item ->
            item.view.setOnClickListener {
                item.action?.let { action ->
                    menuItemClickListener?.onMenuItemClick(action)
                }
            }
        }
    }

    // ---------- Public API ----------
    fun open(animated: Boolean) {
        val center = calculateItemPositions()

        if (animated && animationHandler?.isAnimating() == true) return

        if (animated && animationHandler != null) {
            subActionItems.forEach { item ->
                if (item.view.parent != null) {
                    throw RuntimeException("All of the sub action items have to be independent from a parent.")
                }
                addIndividualMenuItem(item, center.x - item.width / 2, center.y - item.height / 2)
            }
            animationHandler.animateMenuOpening(center)
        } else {
            subActionItems.forEach { item ->
                addIndividualMenuItem(item, item.x, item.y)
            }
        }

        isOpen = true
        stateChangeListener?.onMenuOpened(this)
    }

    fun close(animated: Boolean) {
        if (animated && animationHandler?.isAnimating() == true) return

        if (animated && animationHandler != null) {
            animationHandler.animateMenuClosing(getActionViewCenter())
        } else {
            subActionItems.forEach { removeIndividualMenuItem(it) }
        }

        isOpen = false
        stateChangeListener?.onMenuClosed(this)
    }

    fun toggle(animated: Boolean) = if (isOpen) close(animated) else open(animated)

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
        try {
            val layoutParams = createIndividualMenuItemParams(x, y, item.width, item.height)
            individualMenuItems[item] = layoutParams
            windowManager.addView(item.view, layoutParams)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add individual menu item: ${e.message}")
        }
    }

    fun removeIndividualMenuItem(item: Item) {
        try {
            windowManager.removeView(item.view)
            individualMenuItems.remove(item)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to remove individual menu item: ${e.message}")
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
        val layoutParams = individualMenuItems[item]
        if (layoutParams != null) {
            if (layoutParams.x != x || layoutParams.y != y) {
                layoutParams.x = x
                layoutParams.y = y
                try {
                    windowManager.updateViewLayout(item.view, layoutParams)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to update individual menu item position: ${e.message}")
                }
            }
        }
    }

    // ---------- Data Class ----------
    data class Item(val view: View, val action: MenuAction? = null) {
        var x: Int = 0
        var y: Int = 0
        
        val width: Int
            get() = if (view.measuredWidth > 0) view.measuredWidth else view.layoutParams?.width ?: 0
            
        val height: Int
            get() = if (view.measuredHeight > 0) view.measuredHeight else view.layoutParams?.height ?: 0
    }

    interface MenuStateChangeListener {
        fun onMenuOpened(menu: FloatingActionMenu)
        fun onMenuClosed(menu: FloatingActionMenu)
    }
    
    interface MenuItemClickListener {
        fun onMenuItemClick(action: MenuAction)
    }

    companion object {
        private const val TAG = "FloatingActionMenu"

        fun create(
            context: Context,
            resId: Int,
            action: MenuAction? = null,
            sizeDp: Float = 44f,
        ): Item {
            val sizeInPx = dp2px(sizeDp)
            val margin = dp2px(10f)

            val imageView = ImageView(context).apply {
                setImageResource(resId)
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    Gravity.CENTER
                ).apply {
                    setMargins(margin, margin, margin, margin)
                }
            }

            return Item(
                action = action,
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
            radius: Float = 80f,
            menuItems: List<Item> = emptyList(),
            animationHandler: AnimationHandler? = AnimationHandler(),
            stateChangeListener: MenuStateChangeListener? = null,
            menuItemClickListener: MenuItemClickListener? = null
        ) = FloatingActionMenu(
            mainActionView = actionView,
            startAngle = startAngle,
            endAngle = endAngle,
            radius = radius,
            subActionItems = menuItems,
            animationHandler = animationHandler,
            stateChangeListener = stateChangeListener,
            menuItemClickListener = menuItemClickListener
        )
    }
}
