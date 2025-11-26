package io.github.chayanforyou.quickball.ui.floating

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.Interpolator
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import io.github.chayanforyou.quickball.R
import io.github.chayanforyou.quickball.domain.PreferenceManager
import io.github.chayanforyou.quickball.domain.handlers.MenuAction
import io.github.chayanforyou.quickball.domain.handlers.QuickBallMenuActionHandler
import io.github.chayanforyou.quickball.domain.models.QuickBallMenuItemModel
import io.github.chayanforyou.quickball.helpers.AnimationHelper
import io.github.chayanforyou.quickball.ui.floating.QuickBallFloatingMenu.MenuItemClickListener
import io.github.chayanforyou.quickball.utils.WidgetUtil.dp2px

class QuickBallFloatingButton(
    private val context: Context,
    private val menuActionHandler: QuickBallMenuActionHandler? = null
) {

    companion object {
        private const val TAG = "FloatingBallView"
        private val STASH_INTERPOLATOR = PathInterpolator(0.4f, 0f, 0.2f, 1f)
    }

    private val displayMetrics: DisplayMetrics by lazy { context.resources.displayMetrics }
    private val windowManager: WindowManager by lazy { context.getSystemService(Context.WINDOW_SERVICE) as WindowManager }
    private var floatingBall: View? = null
    private var floatingMenu: QuickBallFloatingMenu? = null

    // Ball properties
    private val ballSize by lazy { dp2px(45f) }
    private val ballMargin by lazy { dp2px(5f) }
    private val stashOffset by lazy { dp2px(24f) }
    private val topBoundary by lazy { dp2px(100f) }
    private val bottomBoundary by lazy { dp2px(100f) }

    private var isVisible = false
    private var isStashed = false
    private var isAnimatingStash = false
    private var isBallOnLeftSide = false
    private var lastMenuItemsHash: Int = 0

    // Orientation-specific position
    private var portraitPosition: Pair<Int, Int>? = null
    private var landscapePosition: Pair<Int, Int>? = null
    private var currentOrientation = Configuration.ORIENTATION_UNDEFINED

    // Callbacks
    private var onStashStateChangedListener: ((Boolean) -> Unit)? = null
    private var onDragStateChangedListener: ((Boolean) -> Unit)? = null
    private var onMenuStateChangedListener: ((Boolean) -> Unit)? = null

    private fun getFloatingMenuItems(): List<QuickBallFloatingMenu.Item> {
        val selectedMenuItems = PreferenceManager.getSelectedMenuItems(context)

        return selectedMenuItems.map { menuItem ->
            QuickBallFloatingMenu.create(context, menuItem)
        }
    }

    private val floatingBallTouchListener by lazy {
        QuickBallTouchListener(
            displayMetrics = displayMetrics,
            windowManager = windowManager,
            ballSize = ballSize,
            topBoundary = topBoundary,
            bottomBoundary = bottomBoundary,
            floatingButton = this
        )
    }

    fun initialize() {
        // Initialize current orientation
        currentOrientation = context.resources.configuration.orientation

        // Create floating ball
        floatingBall = createFloatingBall()
    }

    private fun createFloatingBall(): FrameLayout {
        val margin = dp2px(9f)

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

        return FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(ballSize, ballSize)
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

    private fun createFloatingBallLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams().apply {
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
            }

            // Set flags
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED

            // Set format
            format = PixelFormat.TRANSLUCENT

            // Set size
            width = ballSize
            height = ballSize

            // Set initial position (right edge, center vertically)
            x = displayMetrics.widthPixels - ballSize - ballMargin
            y = displayMetrics.heightPixels / 2 - ballSize / 2

            // Set gravity
            gravity = Gravity.TOP or Gravity.START
        }
    }

    private fun addFloatingBallToWindow() {
        val overlay = floatingBall ?: return

        try {
            val layoutParams = createFloatingBallLayoutParams()
            windowManager.addView(overlay, layoutParams)
            isVisible = true
            isStashed = false
        } catch (e: Exception) {
            Log.e(TAG, "Error showing floating ball", e)
        }
    }

    fun isStashed(): Boolean = isStashed

    fun isAnimatingStash(): Boolean = isAnimatingStash

    fun show() {
        if (isVisible) return

        // Add quick ball overlay
        addFloatingBallToWindow()

        // Restore last position if available
        restoreLastPosition()
    }

    fun hide() {
        if (!isVisible || floatingBall == null) return

        // Save current position for the current orientation
        savePositionForCurrentOrientation()

        // Hide and cleanup menu
        hideMenu()

        try {
            floatingBall?.let { ball ->
                if (ball.parent != null) {
                    windowManager.removeView(ball)
                }
            }

            isVisible = false
            isStashed = false
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding floating ball", e)
        }
    }

    fun stash(animated: Boolean = true) {
        if (!isVisible || isStashed || isAnimatingStash || floatingBall == null) return

        val layoutParams = floatingBall!!.layoutParams as WindowManager.LayoutParams
        val isOnLeftEdge = layoutParams.x < displayMetrics.widthPixels / 2
        val targetX = if (isOnLeftEdge) {
            -stashOffset
        } else {
            displayMetrics.widthPixels - ballSize + stashOffset
        }

        if (animated) {
            isAnimatingStash = true

            animateToPosition(
                startX = layoutParams.x,
                startY = layoutParams.y,
                endX = targetX,
                endY = layoutParams.y,
            ) {
                isStashed = true
                isAnimatingStash = false
                onStashStateChangedListener?.invoke(true)
            }

            animateAlpha(
                from = 1.0f,
                to = 0.4f,
            )
        } else {
            layoutParams.x = targetX
            windowManager.updateViewLayout(floatingBall, layoutParams)
            floatingBall?.alpha = 0.4f
            isStashed = true
            onStashStateChangedListener?.invoke(true)
        }
    }

    fun unstash(animated: Boolean = true) {
        if (!isVisible || floatingBall == null) return

        if (isAnimatingStash) {
            isAnimatingStash = false
        }

        val layoutParams = floatingBall!!.layoutParams as WindowManager.LayoutParams
        val isOnLeftEdge = layoutParams.x < displayMetrics.widthPixels / 2
        val targetX = if (isOnLeftEdge) {
            ballMargin
        } else {
            displayMetrics.widthPixels - ballSize - ballMargin
        }

        if (animated) {
            animateToPosition(
                startX = layoutParams.x,
                startY = layoutParams.y,
                endX = targetX,
                endY = layoutParams.y,
                duration = 50L,
            ) {
                isStashed = false
                onStashStateChangedListener?.invoke(false)
                openFloatingMenu()
            }

            animateAlpha(
                from = 0.4f,
                to = 1.0f,
                duration = 50L,
            )
        } else {
            layoutParams.x = targetX
            windowManager.updateViewLayout(floatingBall, layoutParams)
            floatingBall?.alpha = 1.0f
            isStashed = false
            onStashStateChangedListener?.invoke(false)
            openFloatingMenu()
        }
    }

    fun forceStash() {
        if (!isVisible || floatingBall == null) return

        val layoutParams = floatingBall!!.layoutParams as WindowManager.LayoutParams

        // Determine which edge to stash to and calculate target position
        val isOnLeftEdge = layoutParams.x < displayMetrics.widthPixels / 2
        val targetX = if (isOnLeftEdge) {
            -stashOffset // Hide to the left (go negative to hide part of ball)
        } else {
            displayMetrics.widthPixels - ballSize + stashOffset // Hide to the right (go beyond screen edge)
        }

        // Force stash instantly without checks
        layoutParams.x = targetX
        windowManager.updateViewLayout(floatingBall, layoutParams)
        floatingBall?.alpha = 0.4f
        isStashed = true
        onStashStateChangedListener?.invoke(true)
    }

    fun isVisible(): Boolean = isVisible

    fun isMenuOpen(): Boolean = floatingMenu?.isOpen() == true

    fun setDragging(isDragging: Boolean) {
        onDragStateChangedListener?.invoke(isDragging)
    }

    fun hideMenu(animated: Boolean = false) {
        floatingMenu?.takeIf { it.isOpen() }?.apply {
            if (isAnimating()) {
                doOnAnimationEnd { isOpen ->
                    if (isOpen) close(animated)
                }
            } else {
                close(animated)
            }
        }
    }

    private fun restoreLastPosition() {
        if (floatingBall == null) return

        val layoutParams = floatingBall!!.layoutParams as WindowManager.LayoutParams

        // Try to restore orientation-specific position
        val savedPosition = when (currentOrientation) {
            Configuration.ORIENTATION_PORTRAIT -> portraitPosition
            Configuration.ORIENTATION_LANDSCAPE -> landscapePosition
            else -> portraitPosition
        }

        savedPosition?.let { (x, y) ->
            layoutParams.x = x
            layoutParams.y = y
            windowManager.updateViewLayout(floatingBall, layoutParams)
        }
    }

    private fun openFloatingMenu() {
        if (!isVisible) return

        floatingBall?.post {
            ensureMenuCreated()
//            showMenuOverlay()
            floatingMenu?.open(true)
        }
    }

    private fun updateMenuIcon() {
        val imageView = (floatingBall as? FrameLayout)?.getChildAt(0) as? ImageView ?: return
        val newIcon = if (isMenuOpen()) R.drawable.ic_menu_close else R.drawable.ic_menu_open
        animateIconChange(imageView, newIcon)
    }

    private fun animateIconChange(
        imageView: ImageView,
        @DrawableRes newIconRes: Int,
        duration: Long = 180L,
        reverseAngle: Float = -90f
    ) {
        val newDrawable = ContextCompat.getDrawable(imageView.context, newIconRes) ?: return

        ObjectAnimator.ofFloat(imageView, View.ROTATION, 0f, reverseAngle).apply {
            this.duration = duration
            interpolator = PathInterpolator(0.4f, 0f, 0.2f, 1f)
            doOnEnd {
                imageView.setImageDrawable(newDrawable)
                ObjectAnimator.ofFloat(imageView, View.ROTATION, reverseAngle, 0f).apply {
                    this.duration = duration
                    interpolator = PathInterpolator(0.4f, 0f, 0.2f, 1f)
                    start()
                }
            }
        }.start()
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
        val currentMenuItems = PreferenceManager.getSelectedMenuItems(context)
        val currentMenuItemsHash = currentMenuItems.hashCode()

        if (floatingMenu == null ||
            isBallOnLeftSide != currentlyOnLeft ||
            lastMenuItemsHash != currentMenuItemsHash) {

            recreateMenu()
            isBallOnLeftSide = currentlyOnLeft
            lastMenuItemsHash = currentMenuItemsHash
        }
    }

    private fun createMenu() {
        val startAngle = getMenuStartAngle()
        val endAngle = getMenuEndAngle()
        val menuItems = if (isBallOnLeftSide) getFloatingMenuItems() else getFloatingMenuItems().reversed()

        floatingMenu = QuickBallFloatingMenu.attached(
            actionView = floatingBall!!,
            startAngle = startAngle,
            endAngle = endAngle,
            menuItems = menuItems,
            animationHelper = AnimationHelper(),
            stateChangeListener = object : QuickBallFloatingMenu.MenuStateChangeListener {
                override fun onMenuOpened(menu: QuickBallFloatingMenu) {
                    floatingBall?.post { updateMenuIcon() }
                    onMenuStateChangedListener?.invoke(true)
                }

                override fun onMenuClosed(menu: QuickBallFloatingMenu) {
//                    hideMenuOverlay()
                    floatingBall?.post { updateMenuIcon() }
                    onMenuStateChangedListener?.invoke(false)
                }
            },
            menuItemClickListener = object : MenuItemClickListener {
                override fun onMenuItemClick(menuItem: QuickBallMenuItemModel) {
                    menuActionHandler?.onMenuAction(menuItem)
                    val shouldClose = when (menuItem.action) {
                        MenuAction.VOLUME_UP,
                        MenuAction.VOLUME_DOWN,
                        MenuAction.BRIGHTNESS_UP,
                        MenuAction.BRIGHTNESS_DOWN -> false
                        else -> true
                    }

                    if (shouldClose) floatingMenu?.close(true)
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

    private fun animateToPosition(
        startX: Int, startY: Int,
        endX: Int, endY: Int,
        duration: Long = 200L,
        interpolator: Interpolator = STASH_INTERPOLATOR,
        onEnd: () -> Unit = {}
    ) {
        ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = duration
            this.interpolator = interpolator

            addUpdateListener { anim ->
                val progress = anim.animatedValue as Float
                val x = (startX + (endX - startX) * progress).toInt()
                val y = (startY + (endY - startY) * progress).toInt()

                val lp = floatingBall?.layoutParams as? WindowManager.LayoutParams ?: return@addUpdateListener
                lp.x = x
                lp.y = y
                try {
                    windowManager.updateViewLayout(floatingBall, lp)
                } catch (_: Exception) {  }
            }

            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onEnd()
                }
            })

            start()
        }
    }

    private fun animateAlpha(
        from: Float,
        to: Float,
        duration: Long = 200L,
        interpolator: Interpolator = STASH_INTERPOLATOR
    ) {
        floatingBall?.let { view ->
            ObjectAnimator.ofFloat(view, View.ALPHA, from, to).apply {
                this.duration = duration
                this.interpolator = interpolator
                start()
            }
        }
    }

    fun snapToEdge(view: View) {
        val layoutParams = view.layoutParams as WindowManager.LayoutParams

        // Snap to left or right edge based on current position
        if (layoutParams.x < displayMetrics.widthPixels / 2) {
            layoutParams.x = ballMargin
        } else {
            layoutParams.x = displayMetrics.widthPixels - ballSize - ballMargin
        }

        // Keep Y position within bounds (respect top and bottom boundaries)
        layoutParams.y = layoutParams.y.coerceIn(
            topBoundary,
            displayMetrics.heightPixels - ballSize - bottomBoundary
        )

        windowManager.updateViewLayout(view, layoutParams)

        // Save the new position for current orientation
        savePositionForCurrentOrientation()
    }

    fun handleBallClick() {
        when {
            isStashed -> unstash()
            !isAnimatingStash -> {
                floatingMenu?.takeUnless { it.isOpen() }?.let {
                    ensureMenuCreated()
//                    showMenuOverlay()
                }
                floatingMenu?.toggle(true)
            }
        }
    }

    fun moveToLandscapePosition() {
        if (!isVisible || floatingBall == null) return

        val layoutParams = floatingBall!!.layoutParams as WindowManager.LayoutParams

        // Use saved landscape position or default to top-right corner
        val (targetX, targetY) = landscapePosition ?: Pair(
            displayMetrics.widthPixels - ballSize - ballMargin,
            topBoundary + ballMargin
        )

        // Move instantly to new position
        layoutParams.x = targetX
        layoutParams.y = targetY
        windowManager.updateViewLayout(floatingBall, layoutParams)

        // Update current orientation
        currentOrientation = Configuration.ORIENTATION_LANDSCAPE
    }

    fun moveToPortraitPosition() {
        if (!isVisible || floatingBall == null) return

        val layoutParams = floatingBall!!.layoutParams as WindowManager.LayoutParams

        // Use saved portrait position or default to center-right
        val (targetX, targetY) = portraitPosition ?: Pair(
            displayMetrics.widthPixels - ballSize - ballMargin,
            displayMetrics.heightPixels / 2 - ballSize / 2
        )

        // Move instantly to new position
        layoutParams.x = targetX
        layoutParams.y = targetY
        windowManager.updateViewLayout(floatingBall, layoutParams)

        // Update current orientation
        currentOrientation = Configuration.ORIENTATION_PORTRAIT
    }

    private fun savePositionForCurrentOrientation() {
        val layoutParams = floatingBall?.layoutParams as? WindowManager.LayoutParams ?: return

        when (currentOrientation) {
            Configuration.ORIENTATION_PORTRAIT -> {
                portraitPosition = Pair(layoutParams.x, layoutParams.y)
            }
            Configuration.ORIENTATION_LANDSCAPE -> {
                landscapePosition = Pair(layoutParams.x, layoutParams.y)
            }
            else -> {
                portraitPosition = Pair(layoutParams.x, layoutParams.y)
            }
        }
    }
}