package io.github.chayanforyou.quickball.ui.floating

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
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
    private var menuOverlay: View? = null
    private var floatingMenu: FloatingActionMenu? = null
    private val floatingMenuItems = listOf(
        FloatingActionMenu.create(context, R.drawable.ic_volume_up, MenuAction.VOLUME_UP),
        FloatingActionMenu.create(context, R.drawable.ic_volume_down, MenuAction.VOLUME_DOWN),
        FloatingActionMenu.create(context, R.drawable.ic_brightness_up, MenuAction.BRIGHTNESS_UP),
        FloatingActionMenu.create(context, R.drawable.ic_brightness_down, MenuAction.BRIGHTNESS_DOWN),
        FloatingActionMenu.create(context, R.drawable.ic_lock, MenuAction.LOCK_SCREEN),
    )

    // Ball properties (cached as pixels for performance)
    private val ballSize = 36f
    private val ballMargin = 4f
    private val stashOffset = 20f
    private val topBoundary = 100f
    private val bottomBoundary = 100f
    
    // Cached pixel values to avoid repeated dp2px calls
    private val ballSizePx by lazy { dp2px(ballSize) }
    private val ballMarginPx by lazy { dp2px(ballMargin) }
    private val stashOffsetPx by lazy { dp2px(stashOffset) }
    private val topBoundaryPx by lazy { dp2px(topBoundary) }
    private val bottomBoundaryPx by lazy { dp2px(bottomBoundary) }
    private var isBallOnLeftSide = false
    private var isVisible = false
    private var isStashed = false
    private var isAnimatingStash = false

    // Position memory
    private var lastPositionX: Int? = null
    private var lastPositionY: Int? = null

    // Callbacks
    private var onStashStateChangedListener: ((Boolean) -> Unit)? = null
    private var onDragStateChangedListener: ((Boolean) -> Unit)? = null
    private var onMenuStateChangedListener: ((Boolean) -> Unit)? = null

    fun initialize() {
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
            layoutParams = FrameLayout.LayoutParams(ballSizePx, ballSizePx)
            background = ContextCompat.getDrawable(
                context,
                R.drawable.floating_ball_background
            )?.mutate()?.constantState?.newDrawable()
            isClickable = true
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            addView(imageView)
            setOnTouchListener(floatingBallTouchListener)
        }

        // Create menu overlay once during initialization
        menuOverlay = createMenuOverlay()
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

        // Restore last position if available
        restoreLastPosition()
    }

    fun hide() {
        if (!isVisible || floatingBall == null) return

        // Save current position
        saveCurrentPosition()

        try {
            windowManager.removeView(floatingBall)
            isVisible = false
            isStashed = false
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding floating ball", e)
        }
    }

    fun stash(animated: Boolean = true) {
        if (!isVisible || isStashed || isAnimatingStash || floatingBall == null) return

        val layoutParams = floatingBall!!.layoutParams as WindowManager.LayoutParams

        // Determine which edge to stash to and calculate target position
        val isOnLeftEdge = layoutParams.x < displayMetrics.widthPixels / 2
        val targetX = if (isOnLeftEdge) {
            -stashOffsetPx // Hide to the left (go negative to hide part of ball)
        } else {
            displayMetrics.widthPixels - ballSizePx + stashOffsetPx // Hide to the right (go beyond screen edge)
        }
        
        if (animated) {
            isAnimatingStash = true
            // Animate to stash position and make transparent
            animateToPosition(layoutParams.x, layoutParams.y, targetX, layoutParams.y) {
                isStashed = true
                isAnimatingStash = false
                onStashStateChangedListener?.invoke(true)
            }
            // Animate alpha to transparent
            animateAlpha(1.0f, 0.5f)
        } else {
            // Instantly move to stash position
            layoutParams.x = targetX
            windowManager.updateViewLayout(floatingBall, layoutParams)
            floatingBall?.alpha = 0.5f
            isStashed = true
            onStashStateChangedListener?.invoke(true)
        }
    }

    fun unstash(animated: Boolean = true) {
        if (!isVisible || floatingBall == null) return

        // Cancel any ongoing stash animation
        if (isAnimatingStash) {
            isAnimatingStash = false
        }

        val layoutParams = floatingBall!!.layoutParams as WindowManager.LayoutParams

        // Determine which edge to unstash from
        val isOnLeftEdge = layoutParams.x < displayMetrics.widthPixels / 2
        val targetX = if (isOnLeftEdge) {
            ballMarginPx // Show on left edge
        } else {
            displayMetrics.widthPixels - ballSizePx - ballMarginPx // Show on right edge
        }

        if (animated) {
            // Animate to unstash position and make opaque
            animateToPosition(layoutParams.x, layoutParams.y, targetX, layoutParams.y, 30) {
                isStashed = false
                onStashStateChangedListener?.invoke(false)
                openFloatingMenu()
            }
            // Animate alpha to opaque
            animateAlpha(0.5f, 1.0f, 30)
        } else {
            // Instantly move to unstash position
            layoutParams.x = targetX
            windowManager.updateViewLayout(floatingBall, layoutParams)
            floatingBall?.alpha = 1.0f
            isStashed = false
            onStashStateChangedListener?.invoke(false)
            openFloatingMenu()
        }
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
        layoutParams.y = layoutParams.y.coerceIn(
            topBoundaryPx,
            displayMetrics.heightPixels - ballSizePx - bottomBoundaryPx
        )

        windowManager.updateViewLayout(view, layoutParams)
    }

    fun isStashed(): Boolean = isStashed

    fun isVisible(): Boolean = isVisible

    fun isMenuOpen(): Boolean = floatingMenu?.isOpen() == true

    fun hideMenu(animated: Boolean = false) {
        hideMenuOverlay()
        floatingMenu?.let { menu ->
            if (menu.isOpen()) {
                if (menu.isAnimating()) {
                    waitForAnimationAndClose(menu, animated)
                } else {
                    menu.close(animated)
                }
            }
        }
    }
    
    private fun waitForAnimationAndClose(menu: FloatingActionMenu, animated: Boolean) {
        menu.setAnimationCompletionListener {
            if (menu.isOpen()) {
                menu.close(animated)
            }
            menu.setAnimationCompletionListener(null)
        }
    }

    private fun saveCurrentPosition() {
        val layoutParams = floatingBall?.layoutParams as? WindowManager.LayoutParams
        layoutParams?.let {
            lastPositionX = it.x
            lastPositionY = it.y
        }
    }

    private fun restoreLastPosition() {
        val savedX = lastPositionX
        val savedY = lastPositionY
        
        if (savedX != null && savedY != null && floatingBall != null) {
            val layoutParams = floatingBall!!.layoutParams as WindowManager.LayoutParams
            layoutParams.x = savedX
            layoutParams.y = savedY
            windowManager.updateViewLayout(floatingBall, layoutParams)
        }
    }

    private fun openFloatingMenu() {
        if (!isVisible) return
        
        floatingBall?.post {
            ensureMenuCreated()
            floatingMenu?.open(true)
        }
    }

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

        // Show overlay before menu items are added
        showMenuOverlay()
    }

    private fun createMenu() {
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
                    hideMenuOverlay()
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
        return WindowManager.LayoutParams().apply {
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
            }

            // Set flags
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

            // Set format
            format = PixelFormat.TRANSLUCENT

            // Set size
            width = ballSizePx
            height = ballSizePx

            // Set initial position (right edge, center vertically)
            x = displayMetrics.widthPixels - ballSizePx - ballMarginPx
            y = displayMetrics.heightPixels / 2 - ballSizePx / 2

            // Set gravity
            gravity = Gravity.TOP or Gravity.START
        }
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

    private fun animateAlpha(fromAlpha: Float, toAlpha: Float, animDuration: Long = 200) {
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

    private fun createMenuOverlay(): View {
        return View(context).apply {
            setBackgroundColor(Color.TRANSPARENT)
            isClickable = true
            isFocusable = false
            setOnTouchListener { v, event ->
                v.performClick()
                when (event.action) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_OUTSIDE -> {
                        hideMenu(true)
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun showMenuOverlay() {
        val overlay = menuOverlay ?: return
        if (overlay.parent != null) return

        try {
            val layoutParams = createMenuOverlayLayoutParams()
            windowManager.addView(overlay, layoutParams)
        } catch (e: Exception) {
            Log.e(TAG, "Error showing menu overlay", e)
        }
    }

    private fun hideMenuOverlay() {
        menuOverlay?.let { overlay ->
            try {
                if (overlay.parent != null) {
                    windowManager.removeView(overlay)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error hiding menu overlay", e)
            }
        }
    }

    private fun createMenuOverlayLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams().apply {
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
            }
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH

            format = PixelFormat.TRANSLUCENT
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            gravity = Gravity.TOP or Gravity.START
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
                            if (floatingMenu?.isOpen() == false) {
                                ensureMenuCreated()
                            }
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