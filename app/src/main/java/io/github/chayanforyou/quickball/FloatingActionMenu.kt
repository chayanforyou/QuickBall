package io.github.chayanforyou.quickball

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
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import io.github.chayanforyou.quickball.animation.AnimationHandler
import io.github.chayanforyou.quickball.utils.WidgetUtil.dp2px
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class FloatingActionMenu(
    private val mainActionView: View,
    private val startAngle: Int,
    private val endAngle: Int,
    private val radius: Float,
    private val subActionItems: List<Item> = mutableListOf(),
    private val animationHandler: AnimationHandler? = null,
    private var stateChangeListener: MenuStateChangeListener? = null,
) {

    private val windowManager: WindowManager by lazy {
        mainActionView.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }
    private var overlayContainer: FrameLayout? = null
    private val menuRadius = dp2px(radius)
    private var isOpen = false
    private val individualMenuItems = mutableMapOf<Item, WindowManager.LayoutParams>()

    fun getSubActionItems(): List<Item> = subActionItems
    fun getOverlayContainer(): FrameLayout? = overlayContainer
    fun isOpen(): Boolean = isOpen

    init {
        animationHandler?.setMenu(this)
        subActionItems.filter { it.width <= 0 || it.height <= 0 }
            .forEach { measureMenuItemSize(it) }
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
            layoutParams.x = x
            layoutParams.y = y
            try {
                windowManager.updateViewLayout(item.view, layoutParams)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update individual menu item position: ${e.message}")
            }
        }
    }

    // ---------- Overlay management ----------
    private fun ensureOverlayContainer(): FrameLayout {
        if (overlayContainer == null) {
            overlayContainer = FrameLayout(mainActionView.context)
        }
        return overlayContainer!!
    }

    private fun attachOverlayContainer() {
        try {
            val container = ensureOverlayContainer()
            val overlayParams = calculateOverlayContainerParams()
            container.layoutParams = overlayParams

            if (container.parent == null) {
                windowManager.addView(container, overlayParams)
            } else {
                windowManager.updateViewLayout(container, overlayParams)
            }
            windowManager.updateViewLayout(mainActionView, mainActionView.layoutParams)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach overlay container: ${e.message}")
        }
    }

    fun detachOverlayContainer() {
        overlayContainer?.let { container ->
            try {
                windowManager.removeView(container)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to detach overlay container: ${e.message}")
            }
        }
    }

    private fun calculateOverlayContainerParams(): WindowManager.LayoutParams {
        if (subActionItems.isEmpty()) return getDefaultSystemWindowParams()

        val bounds = subActionItems.fold(
            Rect(Int.MAX_VALUE, Int.MAX_VALUE, Int.MIN_VALUE, Int.MIN_VALUE)
        ) { rect, item ->
            rect.apply {
                left = min(left, item.x)
                top = min(top, item.y)
                right = max(right, item.x + item.width)
                bottom = max(bottom, item.y + item.height)
            }
        }

        return getDefaultSystemWindowParams().apply {
            width = bounds.width().coerceAtLeast(0)
            height = bounds.height().coerceAtLeast(0)
            x = bounds.left
            y = bounds.top
            gravity = Gravity.TOP or Gravity.START
        }
    }

    // add a view to the overlay (safe helper)
    private fun addViewToCurrentContainer(view: View, layoutParams: ViewGroup.LayoutParams) {
        val container = ensureOverlayContainer()
        container.addView(view, layoutParams)
    }

    fun removeViewFromCurrentContainer(view: View) {
        overlayContainer?.removeView(view)
    }

    private fun measureMenuItemSize(item: Item) {
        if (item.view.parent != null) return

        // Temporarily add to a container to measure
        val container = ensureOverlayContainer()
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

    private fun addViewToOverlayTemporarily(item: Item) {
        measureMenuItemSize(item)
    }

    // ---------- data types ----------
    data class Item(val view: View) {
        var x: Int = 0
        var y: Int = 0
        var alpha: Float = view.alpha
        
        val width: Int
            get() = if (view.measuredWidth > 0) view.measuredWidth else view.layoutParams?.width ?: 0
            
        val height: Int
            get() = if (view.measuredHeight > 0) view.measuredHeight else view.layoutParams?.height ?: 0
    }

    interface MenuStateChangeListener {
        fun onMenuOpened(menu: FloatingActionMenu)
        fun onMenuClosed(menu: FloatingActionMenu)
    }

    companion object {
        private const val TAG = "FloatingActionMenu"

        fun create(
            context: Context,
            resId: Int,
            sizeDp: Float = 48f,
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
                FrameLayout(context).apply {
                    layoutParams = FrameLayout.LayoutParams(sizeInPx, sizeInPx)
                    background = ContextCompat.getDrawable(
                        context,
                        R.drawable.button_action_dark_selector
                    )?.mutate()?.constantState?.newDrawable()

                    isClickable = true
                    addView(imageView)
                }
            )
        }

        fun attached(
            actionView: View,
            startAngle: Int = 120,
            endAngle: Int = 240,
            radius: Float = 86f,
            menuItems: List<Item> = emptyList(),
            animationHandler: AnimationHandler? = AnimationHandler(),
            stateChangeListener: MenuStateChangeListener? = null
        ) = FloatingActionMenu(
            mainActionView = actionView,
            startAngle = startAngle,
            endAngle = endAngle,
            radius = radius,
            subActionItems = menuItems,
            animationHandler = animationHandler,
            stateChangeListener = stateChangeListener
        )

        fun getDefaultSystemWindowParams() = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
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
        }
    }
}
