package io.github.chayanforyou.quickball.ui.floating

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import io.github.chayanforyou.quickball.R
import io.github.chayanforyou.quickball.domain.handlers.MenuAction
import io.github.chayanforyou.quickball.domain.handlers.MenuActionHandler
import io.github.chayanforyou.quickball.ui.floating.FloatingActionMenu.MenuItemClickListener
import io.github.chayanforyou.quickball.utils.AnimationManager
import io.github.chayanforyou.quickball.utils.WidgetUtil.dp2px
import kotlin.math.abs

class FloatingActionButton(
    private val context: Context,
    private val menuActionHandler: MenuActionHandler? = null
) {

    companion object {
        private const val TAG = "FloatingBallView"
    }

    private val displayMetrics: DisplayMetrics by lazy { context.resources.displayMetrics }
    private val windowManager: WindowManager by lazy { context.getSystemService(Context.WINDOW_SERVICE) as WindowManager }
    private var floatingBall: View? = null
    private var floatingMenu: FloatingActionMenu? = null
    private val floatingMenuItems = listOf(
        FloatingActionMenu.create(context, R.drawable.ic_volume_up, MenuAction.VOLUME_UP),
        FloatingActionMenu.create(context, R.drawable.ic_volume_down, MenuAction.VOLUME_DOWN),
        FloatingActionMenu.create(context, R.drawable.ic_brightness_up, MenuAction.BRIGHTNESS_UP),
        FloatingActionMenu.create(context, R.drawable.ic_brightness_down, MenuAction.BRIGHTNESS_DOWN),
        FloatingActionMenu.create(context, R.drawable.ic_lock, MenuAction.LOCK_SCREEN),
    )

    // Ball properties
    private val ballSize = 36f
    private val ballMargin = 4f
    private val stashOffset = 22f
    private val topBoundary = 100f
    private val bottomBoundary = 100f
    private var isBallOnLeftSide = false
    private var isVisible = false
    private var isStashed = false
    private var isAnimatingStash = false

    // Callbacks
    private var onStashStateChangedListener: ((Boolean) -> Unit)? = null
    private var onDragStateChangedListener: ((Boolean) -> Unit)? = null
    private var onMenuStateChangedListener: ((Boolean) -> Unit)? = null

    fun initialize() {

        val sizeInPx = dp2px(ballSize)
        val margin = dp2px(7f)

        val imageView = ImageView(context).apply {
            setImageResource(R.drawable.ic_menu_open)
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.CENTER
            ).apply {
                setMargins(margin, margin, margin, margin)
            }
        }

        floatingBall = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(sizeInPx, sizeInPx)
            background = ContextCompat.getDrawable(
                context,
                R.drawable.floating_ball_background
            )?.mutate()?.constantState?.newDrawable()
            isClickable = true
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            addView(imageView)
            setOnTouchListener(floatingBallTouchListener)
        }
    }

    fun show() {
        if (isVisible) return

        val layoutParams = createLayoutParams()

        try {
            windowManager.addView(floatingBall, layoutParams)
            isVisible = true
            isStashed = false
        } catch (e: Exception) {
            Log.e(TAG, "Error showing floating ball", e)
        }
    }

    fun hide() {
        if (!isVisible || floatingBall == null) return

        try {
            windowManager.removeView(floatingBall)
            isVisible = false
            isStashed = false
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding floating ball", e)
        }
    }

    fun stash() {
        if (!isVisible || isStashed || isAnimatingStash || floatingBall == null) return

        isAnimatingStash = true
        val layoutParams = floatingBall!!.layoutParams as WindowManager.LayoutParams
        val ballSizePx = dp2px(ballSize)
        val stashOffsetPx = dp2px(stashOffset)

        // Determine which edge to stash to and calculate target position
        val isOnLeftEdge = layoutParams.x < displayMetrics.widthPixels / 2
        val targetX = if (isOnLeftEdge) {
            -stashOffsetPx // Hide to the left (go negative to hide part of ball)
        } else {
            displayMetrics.widthPixels - ballSizePx + stashOffsetPx // Hide to the right (go beyond screen edge)
        }
        
        // Animate to stash position and make transparent
        animateToPosition(layoutParams.x, layoutParams.y, targetX, layoutParams.y) {
            isStashed = true
            isAnimatingStash = false
            onStashStateChangedListener?.invoke(true)
        }

        // Animate alpha to transparent
        animateAlpha(1.0f, 0.5f)
    }

    fun unstash() {
        if (!isVisible || floatingBall == null) return

        // Cancel any ongoing stash animation
        if (isAnimatingStash) {
            isAnimatingStash = false
        }

        val layoutParams = floatingBall!!.layoutParams as WindowManager.LayoutParams
        val ballSizePx = dp2px(ballSize)
        val marginPx = dp2px(ballMargin)

        // Determine which edge to unstash from
        val isOnLeftEdge = layoutParams.x < displayMetrics.widthPixels / 2
        val targetX = if (isOnLeftEdge) {
            marginPx // Show on left edge
        } else {
            displayMetrics.widthPixels - ballSizePx - marginPx // Show on right edge
        }

        // Animate to unstash position and make opaque
        animateToPosition(layoutParams.x, layoutParams.y, targetX, layoutParams.y, 50) {
            isStashed = false
            onStashStateChangedListener?.invoke(false)

            floatingBall?.post {
                ensureMenuCreated()
                floatingMenu?.open(true)
            }
        }

        // Animate alpha to opaque
        animateAlpha(0.5f, 1.0f, 50)
    }

    private fun snapToEdge(view: View) {
        val layoutParams = view.layoutParams as WindowManager.LayoutParams
        val ballSizePx = dp2px(ballSize)
        val marginPx = dp2px(ballMargin)

        // Snap to left or right edge based on current position
        if (layoutParams.x < displayMetrics.widthPixels / 2) {
            layoutParams.x = marginPx
        } else {
            layoutParams.x = displayMetrics.widthPixels - ballSizePx - marginPx
        }

        // Keep Y position within bounds (respect top and bottom boundaries)
        val topBoundaryPx = dp2px(topBoundary)
        val bottomBoundaryPx = dp2px(bottomBoundary)
        layoutParams.y = layoutParams.y.coerceIn(
            topBoundaryPx,
            displayMetrics.heightPixels - ballSizePx - bottomBoundaryPx
        )

        windowManager.updateViewLayout(view, layoutParams)
    }

    fun isStashed(): Boolean = isStashed

    fun isVisible(): Boolean = isVisible

    fun isMenuOpen(): Boolean = floatingMenu?.isOpen() == true

    private fun updateMenuIcon(animDuration: Long = 80) {
        floatingBall?.let { ball ->
            if (ball is FrameLayout) {
                val imageView = ball.getChildAt(0) as? ImageView
                imageView?.let { image ->
                    val iconRes = if (isMenuOpen()) {
                        R.drawable.ic_menu_close
                    } else {
                        R.drawable.ic_menu_open
                    }

                    // Create rotating animation
                    ObjectAnimator.ofFloat(image, View.ROTATION, 0f, 90f).apply {
                        duration = animDuration
                        interpolator = DecelerateInterpolator()
                        doOnEnd {
                            image.setImageResource(iconRes)
                            ObjectAnimator.ofFloat(image, View.ROTATION, 90f, 0f).apply {
                                duration = animDuration
                            }.start()
                        }
                    }.start()
                }
            }
        }
    }

    private fun isBallOnLeftSide(): Boolean {
        if (floatingBall == null) return false

        val layoutParams = floatingBall!!.layoutParams
        return if (layoutParams is WindowManager.LayoutParams) {
            layoutParams.x < displayMetrics.widthPixels / 2
        } else {
            false
        }
    }

    private fun getMenuStartAngle(): Int {
        return if (isBallOnLeftSide) { 280 } else { 100 }
    }

    private fun getMenuEndAngle(): Int {
        return if (isBallOnLeftSide) { 80 } else { 260 }
    }

    private fun ensureMenuCreated() {
        val currentlyOnLeft = isBallOnLeftSide()

        // Create menu if it doesn't exist OR if ball position has changed
        if (floatingMenu == null || isBallOnLeftSide != currentlyOnLeft) {
            recreateMenu()
            isBallOnLeftSide = currentlyOnLeft
        }
    }

    private fun createMenu() {
        // Pre-calculate menu items to avoid work during animation
        val startAngle = getMenuStartAngle()
        val endAngle = getMenuEndAngle()
        val menuItems = if (isBallOnLeftSide) floatingMenuItems else floatingMenuItems.reversed()
        
        floatingMenu = FloatingActionMenu.attached(
            actionView = floatingBall!!,
            startAngle = startAngle,
            endAngle = endAngle,
            menuItems = menuItems,
            animationManager = AnimationManager(),
            stateChangeListener = object : FloatingActionMenu.MenuStateChangeListener {
                override fun onMenuOpened(menu: FloatingActionMenu) {
                    floatingBall?.post { updateMenuIcon() }
                    onMenuStateChangedListener?.invoke(true)
                }

                override fun onMenuClosed(menu: FloatingActionMenu) {
                    floatingBall?.post { updateMenuIcon() }
                    onMenuStateChangedListener?.invoke(false)
                }
            },
            menuItemClickListener = object : MenuItemClickListener {
                override fun onMenuItemClick(action: MenuAction) {
                    menuActionHandler?.onMenuAction(action)
                    if (action == MenuAction.LOCK_SCREEN) {
                        floatingMenu?.close(true)
                    }
                }
            }
        )
    }

    private fun recreateMenu() {
        floatingMenu?.close(false)
        isBallOnLeftSide = isBallOnLeftSide()
        createMenu()
    }

    fun setOnStashStateChangedListener(listener: ((Boolean) -> Unit)?) {
        onStashStateChangedListener = listener
    }

    fun setOnDragStateChangedListener(listener: ((Boolean) -> Unit)?) {
        onDragStateChangedListener = listener
    }

    fun setOnMenuStateChangedListener(listener: ((Boolean) -> Unit)?) {
        onMenuStateChangedListener = listener
    }

    private fun createLayoutParams(): WindowManager.LayoutParams {
        val layoutParams = WindowManager.LayoutParams()

        // Set window type based on Android version
        layoutParams.type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
        }

        // Set flags
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

        // Set format
        layoutParams.format = PixelFormat.TRANSLUCENT

        // Set size
        val sizeInPx = dp2px(ballSize)
        layoutParams.width = sizeInPx
        layoutParams.height = sizeInPx

        // Set initial position (right edge, center vertically)
        layoutParams.x = displayMetrics.widthPixels - sizeInPx - dp2px(ballMargin)
        layoutParams.y = displayMetrics.heightPixels / 2 - sizeInPx / 2

        // Set gravity
        layoutParams.gravity = Gravity.TOP or Gravity.START

        return layoutParams
    }

    private fun animateToPosition(
        fromX: Int,
        fromY: Int,
        toX: Int,
        toY: Int,
        animDuration: Long = 200,
        onComplete: () -> Unit
    ) {
        val layoutParams = floatingBall!!.layoutParams as WindowManager.LayoutParams

        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = animDuration
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                val progress = animation.animatedValue as Float
                val newX = (fromX + (toX - fromX) * progress).toInt()
                val newY = (fromY + (toY - fromY) * progress).toInt()

                if (layoutParams.x != newX || layoutParams.y != newY) {
                    layoutParams.x = newX
                    layoutParams.y = newY
                    windowManager.updateViewLayout(floatingBall, layoutParams)
                }
            }
            doOnEnd { onComplete() }
            start()
        }
    }

    private fun animateAlpha(fromAlpha: Float, toAlpha: Float, animDuration: Long = 100) {
        ValueAnimator.ofFloat(fromAlpha, toAlpha).apply {
            duration = animDuration
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                val alpha = it.animatedValue as Float
                floatingBall?.alpha = alpha
            }
            start()
        }
    }

    private val floatingBallTouchListener = object : View.OnTouchListener {
        private var initialX = 0
        private var initialY = 0
        private var initialTouchX = 0f
        private var initialTouchY = 0f
        private var isDragging = false

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            view.performClick()

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (floatingMenu?.isOpen() == true) return true

                    if (isStashed || isAnimatingStash) {
                        isDragging = false
                        return true
                    }

                    val layoutParams = view.layoutParams as WindowManager.LayoutParams
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    return true
                }

                MotionEvent.ACTION_MOVE -> {
                    if (floatingMenu?.isOpen() == true) return true
                    if (isStashed || isAnimatingStash) return true

                    val deltaX = (event.rawX - initialTouchX).toInt()
                    val deltaY = (event.rawY - initialTouchY).toInt()

                    if (abs(deltaX) > 10 || abs(deltaY) > 10 && !isDragging) {
                        isDragging = true
                        onDragStateChangedListener?.invoke(true)
                    }

                    if (isDragging) {
                        val layoutParams = view.layoutParams as WindowManager.LayoutParams
                        val ballSizePx = dp2px(ballSize)
                        val topBoundaryPx = dp2px(topBoundary)
                        val bottomBoundaryPx = dp2px(bottomBoundary)

                        val newX = (initialX + deltaX).coerceIn(0, displayMetrics.widthPixels - ballSizePx)
                        val newY = (initialY + deltaY).coerceIn(topBoundaryPx, displayMetrics.heightPixels - ballSizePx - bottomBoundaryPx)

                        if (layoutParams.x != newX || layoutParams.y != newY) {
                            layoutParams.x = newX
                            layoutParams.y = newY
                            windowManager.updateViewLayout(view, layoutParams)
                        }
                    }
                    return true
                }

                MotionEvent.ACTION_UP -> {
                    if (isDragging) {
                        onDragStateChangedListener?.invoke(false)
                        snapToEdge(view)
                    } else {
                        if (isStashed || isAnimatingStash) {
                            unstash()
                        } else {
                            ensureMenuCreated()
                            floatingMenu?.toggle(true)
                        }
                    }
                    return true
                }
            }
            return false
        }
    }
}