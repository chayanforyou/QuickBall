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
    private val windowManager: WindowManager by lazy {
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    private var floatingBall: View? = null
    private var floatingMenu: QuickBallFloatingMenu? = null

    private val ballSize = dp2px(45f)
    private val ballMargin = dp2px(5f)
    private val stashOffset = dp2px(24f)
    private val topBoundary = dp2px(100f)
    private val bottomBoundary = dp2px(100f)

    private var isVisible = false
    private var isStashed = false
    private var isAnimatingStash = false
    private var lastMenuSideOnLeft = false
    private var lastMenuItemsHash = 0

    private var portraitPosition: Pair<Int, Int>? = null
    private var landscapePosition: Pair<Int, Int>? = null

    /* --------------------------------------------------- */
    /* Callbacks                                           */
    /* --------------------------------------------------- */

    private var onStashStateChangedListener: ((Boolean) -> Unit)? = null
    private var onDragStateChangedListener: ((Boolean) -> Unit)? = null
    private var onMenuStateChangedListener: ((Boolean) -> Unit)? = null

    /* --------------------------------------------------- */
    /* Touch listener                                      */
    /* --------------------------------------------------- */

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

    /* --------------------------------------------------- */
    /* Initialization                                      */
    /* --------------------------------------------------- */

    fun initialize() {
        floatingBall = createFloatingBall()
    }

    /* --------------------------------------------------- */
    /* Visibility                                          */
    /* --------------------------------------------------- */

    fun show() {
        if (isVisible) return
        try {
            windowManager.addView(floatingBall, createLayoutParams())
            isVisible = true
            isStashed = false

            resetIconToDefault()
            restoreLastPosition()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing floating ball", e)
        }
    }

    fun hide() {
        if (!isVisible || floatingBall == null) return

        savePositionForCurrentOrientation()
        hideMenu()

        try {
            windowManager.removeView(floatingBall)
            isVisible = false
            isStashed = false
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding floating ball", e)
        }
    }

    fun isVisible(): Boolean = isVisible
    fun isStashed(): Boolean = isStashed
    fun isAnimatingStash(): Boolean = isAnimatingStash
    fun isMenuOpen(): Boolean = floatingMenu?.isOpen() == true

    /* --------------------------------------------------- */
    /* Drag                                                */
    /* --------------------------------------------------- */

    fun setDragging(isDragging: Boolean) {
        onDragStateChangedListener?.invoke(isDragging)
    }

    /* --------------------------------------------------- */
    /* Stash / Unstash                                     */
    /* --------------------------------------------------- */

    fun stash(animated: Boolean = false) {
        if (!isVisible || isStashed || isAnimatingStash || floatingBall == null) return

        try {
            val lp = floatingBall!!.layoutParams as WindowManager.LayoutParams
            val isOnLeft = lp.x < displayMetrics.widthPixels / 2
            val targetX = if (isOnLeft) -stashOffset
            else displayMetrics.widthPixels - ballSize + stashOffset

            if (animated) {
                isAnimatingStash = true
                animateToPosition(lp.x, lp.y, targetX, lp.y) {
                    isStashed = true
                    isAnimatingStash = false
                    onStashStateChangedListener?.invoke(true)
                }
                animateAlpha(1f, 0.4f)
            } else {
                lp.x = targetX
                windowManager.updateViewLayout(floatingBall, lp)
                floatingBall?.alpha = 0.4f
                isStashed = true
                onStashStateChangedListener?.invoke(true)
            }
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "View not attached to window manager, skipping stash", e)
            isAnimatingStash = false
        } catch (e: Exception) {
            Log.e(TAG, "Error stashing floating ball", e)
            isAnimatingStash = false
        }
    }

    fun unstash(animated: Boolean = true) {
        if (!isVisible || floatingBall == null) return

        if (isAnimatingStash) isAnimatingStash = false

        try {
            val lp = floatingBall!!.layoutParams as WindowManager.LayoutParams
            val isOnLeft = lp.x < displayMetrics.widthPixels / 2
            val targetX = if (isOnLeft) ballMargin
            else displayMetrics.widthPixels - ballSize - ballMargin

            if (animated) {
                animateToPosition(lp.x, lp.y, targetX, lp.y, 50L) {
                    isStashed = false
                    onStashStateChangedListener?.invoke(false)
                    openFloatingMenu()
                }
                animateAlpha(0.4f, 1f, 50L)
            } else {
                lp.x = targetX
                windowManager.updateViewLayout(floatingBall, lp)
                floatingBall?.alpha = 1f
                isStashed = false
                onStashStateChangedListener?.invoke(false)
                openFloatingMenu()
            }
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "View not attached to window manager, skipping unstash", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error unstashing floating ball", e)
        }
    }

    fun forceStash() {
        if (!isVisible || floatingBall == null) return

        try {
            val lp = floatingBall!!.layoutParams as WindowManager.LayoutParams
            val isOnLeft = lp.x < displayMetrics.widthPixels / 2
            lp.x = if (isOnLeft) -stashOffset
            else displayMetrics.widthPixels - ballSize + stashOffset

            windowManager.updateViewLayout(floatingBall, lp)
            floatingBall?.alpha = 0.4f
            isStashed = true
            onStashStateChangedListener?.invoke(true)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "View not attached to window manager, skipping force stash", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error force stashing floating ball", e)
        }
    }

    /* --------------------------------------------------- */
    /* Menu                                                */
    /* --------------------------------------------------- */

    fun hideMenu(animated: Boolean = false) {
        floatingMenu?.takeIf { it.isOpen() }?.apply {
            if (isAnimating()) {
                doOnAnimationEnd { if (it) {
                    close(animated)
                    resetIconToDefault()
                }}
            } else {
                close(animated)
                resetIconToDefault()
            }
        }
    }

    fun handleBallClick() {
        when {
            isStashed -> unstash()
            !isAnimatingStash -> {
                ensureMenuCreated()
                floatingMenu?.toggle(true)
            }
        }
    }

    private fun openFloatingMenu() {
        if (!isVisible) return
        floatingBall?.post {
            ensureMenuCreated()
            floatingMenu?.open(true)
        }
    }

    /* --------------------------------------------------- */
    /* Orientation                                         */
    /* --------------------------------------------------- */

    fun moveToLandscapePosition() {
        if (!isVisible || floatingBall == null) return

        val position = landscapePosition ?: Pair(
            displayMetrics.widthPixels - ballSize - ballMargin,
            displayMetrics.heightPixels / 2 - ballSize / 2
        )

        // Post to ensure view is stable
        floatingBall?.post {
            if (isVisible && floatingBall != null) {
                position.let { moveInstant(it.first, it.second) }
            }
            hideMenu()
            forceStash()
        }
    }

    fun moveToPortraitPosition() {
        if (!isVisible || floatingBall == null) return

        val position = portraitPosition ?: Pair(
            displayMetrics.widthPixels - ballSize - ballMargin,
            displayMetrics.heightPixels / 2 - ballSize / 2
        )

        // Post to ensure view is stable
        floatingBall?.post {
            if (isVisible && floatingBall != null) {
                position.let { moveInstant(it.first, it.second) }
            }
            hideMenu()
            forceStash()
        }
    }

    /* --------------------------------------------------- */
    /* Snap                                                */
    /* --------------------------------------------------- */

    fun snapToEdge(view: View) {
        if (!isViewAttached()) return
        
        try {
            val lp = view.layoutParams as WindowManager.LayoutParams

            lp.x = if (lp.x < displayMetrics.widthPixels / 2) {
                ballMargin
            } else {
                displayMetrics.widthPixels - ballSize - ballMargin
            }

            lp.y = lp.y.coerceIn(
                topBoundary,
                displayMetrics.heightPixels - ballSize - bottomBoundary
            )

            windowManager.updateViewLayout(view, lp)
            savePositionForCurrentOrientation()
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "View not attached to window manager, skipping snap to edge", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error snapping to edge", e)
        }
    }

    /* --------------------------------------------------- */
    /* Helpers                                             */
    /* --------------------------------------------------- */

    private fun isViewAttached(): Boolean {
        return isVisible && floatingBall != null && floatingBall?.isAttachedToWindow == true
    }

    private fun resetIconToDefault() {
        floatingBall?.let { ball ->
            val imageView = (ball as? FrameLayout)?.getChildAt(0) as? ImageView
            imageView?.setImageResource(R.drawable.ic_menu_open)
        }
    }

    private fun moveInstant(x: Int, y: Int) {
        if (!isViewAttached()) return
        
        try {
            val lp = floatingBall!!.layoutParams as WindowManager.LayoutParams
            lp.x = x
            lp.y = y
            windowManager.updateViewLayout(floatingBall, lp)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "View not attached to window manager, skipping position update", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating view layout", e)
        }
    }

    private fun savePositionForCurrentOrientation() {
        val lp = floatingBall?.layoutParams as? WindowManager.LayoutParams ?: return
        when (context.resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE ->
                if (!PreferenceManager.isHideOnLandscapeEnabled(context))
                    landscapePosition = Pair(lp.x, lp.y)
            else -> portraitPosition = Pair(lp.x, lp.y)
        }
    }

    private fun restoreLastPosition() {
        if (!isVisible || floatingBall == null) return
        
        try {
            val lp = floatingBall!!.layoutParams as WindowManager.LayoutParams
            val saved = if (context.resources.configuration.orientation ==
                Configuration.ORIENTATION_LANDSCAPE) landscapePosition else portraitPosition

            saved?.let {
                lp.x = it.first
                lp.y = it.second
                windowManager.updateViewLayout(floatingBall, lp)
            }
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "View not attached to window manager, skipping position restore", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring position", e)
        }
    }

    /* --------------------------------------------------- */
    /* View creation                                       */
    /* --------------------------------------------------- */

    private fun createFloatingBall(): FrameLayout {
        val margin = dp2px(9f)

        val icon = ImageView(context).apply {
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
            background = ContextCompat.getDrawable(context, R.drawable.floating_ball_background)
            isClickable = true
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            addView(icon)
            setOnTouchListener(floatingBallTouchListener)
        }
    }

    private fun createLayoutParams() = WindowManager.LayoutParams().apply {
        type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
        }

        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED

        format = PixelFormat.TRANSLUCENT
        width = ballSize
        height = ballSize
        gravity = Gravity.TOP or Gravity.START
        x = displayMetrics.widthPixels - ballSize - ballMargin
        y = displayMetrics.heightPixels / 2 - ballSize / 2
    }

    /* --------------------------------------------------- */
    /* Animations                                          */
    /* --------------------------------------------------- */

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
            addUpdateListener { animator ->
                if (!isViewAttached()) {
                    animator.cancel()
                    return@addUpdateListener
                }
                
                try {
                    val p = animator.animatedValue as Float
                    moveInstant(
                        (startX + (endX - startX) * p).toInt(),
                        (startY + (endY - startY) * p).toInt()
                    )
                } catch (e: IllegalArgumentException) {
                    Log.w(TAG, "View detached during animation update, cancelling", e)
                    animator.cancel()
                } catch (e: Exception) {
                    Log.e(TAG, "Error during position animation", e)
                    animator.cancel()
                }
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) = onEnd()
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
            if (!isViewAttached()) return
            
            ObjectAnimator.ofFloat(view, View.ALPHA, from, to).apply {
                this.duration = duration
                this.interpolator = interpolator
                start()
            }
        }
    }

    /* --------------------------------------------------- */
    /* Menu creation (unchanged logic)                     */
    /* --------------------------------------------------- */

    private fun ensureMenuCreated() {
        val currentMenuItems = PreferenceManager.getSelectedMenuItems(context)
        val currentHash = currentMenuItems.hashCode()
        val onLeft = isBallOnLeftSide()

        if (floatingMenu == null ||
            lastMenuSideOnLeft != onLeft ||
            lastMenuItemsHash != currentHash) {

            recreateMenu()
            lastMenuSideOnLeft = onLeft
            lastMenuItemsHash = currentHash
        }
    }

    private fun recreateMenu() {
        floatingMenu?.close(false)
        createMenu()
    }

    private fun createMenu() {
        val onLeft = isBallOnLeftSide()

        val startAngle = if (onLeft) 280 else 100
        val endAngle = if (onLeft) 80 else 260

        val menuItems = PreferenceManager.getSelectedMenuItems(context)
            .let { if (onLeft) it else it.reversed() }
            .map { QuickBallFloatingMenu.create(context, it) }

        floatingMenu = QuickBallFloatingMenu.attached(
            actionView = floatingBall!!,
            startAngle = startAngle,
            endAngle = endAngle,
            menuItems = menuItems,
            animationHelper = AnimationHelper(),
            stateChangeListener = object : QuickBallFloatingMenu.MenuStateChangeListener {
                override fun onMenuOpened(menu: QuickBallFloatingMenu) {
                    updateMenuIcon()
                    onMenuStateChangedListener?.invoke(true)
                }

                override fun onMenuClosed(menu: QuickBallFloatingMenu) {
                    updateMenuIcon()
                    onMenuStateChangedListener?.invoke(false)
                }
            },
            menuItemClickListener = object : MenuItemClickListener {
                override fun onMenuItemClick(menuItem: QuickBallMenuItemModel) {
                    menuActionHandler?.onMenuAction(menuItem)
                    if (menuItem.action !in setOf(
                            MenuAction.VOLUME_UP,
                            MenuAction.VOLUME_DOWN,
                            MenuAction.BRIGHTNESS_UP,
                            MenuAction.BRIGHTNESS_DOWN
                        )
                    ) {
                        floatingMenu?.close(true)
                    }
                }
            }
        )
    }

    private fun updateMenuIcon() {
        val image = (floatingBall as FrameLayout).getChildAt(0) as ImageView
        animateIconChange(
            image,
            if (isMenuOpen()) R.drawable.ic_menu_close else R.drawable.ic_menu_open
        )
    }

    private fun animateIconChange(
        imageView: ImageView,
        @DrawableRes icon: Int,
        duration: Long = 180L
    ) {
        ObjectAnimator.ofFloat(imageView, View.ROTATION, 0f, -90f).apply {
            this.duration = duration
            doOnEnd {
                if (isViewAttached()) {
                    imageView.setImageResource(icon)
                    ObjectAnimator.ofFloat(imageView, View.ROTATION, -90f, 0f).apply {
                        this.duration = duration
                        start()
                    }
                }
            }
            start()
        }
    }

    private fun isBallOnLeftSide(): Boolean {
        val lp = floatingBall?.layoutParams as? WindowManager.LayoutParams ?: return false
        return lp.x < displayMetrics.widthPixels / 2
    }

    /* --------------------------------------------------- */
    /* Listener setters                                    */
    /* --------------------------------------------------- */

    fun setOnStashStateChangedListener(listener: ((Boolean) -> Unit)?) {
        onStashStateChangedListener = listener
    }

    fun setOnDragStateChangedListener(listener: ((Boolean) -> Unit)?) {
        onDragStateChangedListener = listener
    }

    fun setOnMenuStateChangedListener(listener: ((Boolean) -> Unit)?) {
        onMenuStateChangedListener = listener
    }
}