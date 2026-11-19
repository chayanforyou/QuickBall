package io.github.chayanforyou.quickball.core

import android.accessibilityservice.AccessibilityService
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import androidx.core.content.getSystemService
import io.github.chayanforyou.quickball.domain.AppPreference
import io.github.chayanforyou.quickball.domain.models.MenuAction
import io.github.chayanforyou.quickball.domain.handlers.QuickBallActionHandler
import io.github.chayanforyou.quickball.domain.models.QuickBallMenuItem
import io.github.chayanforyou.quickball.ui.floating.GestureListener
import io.github.chayanforyou.quickball.ui.floating.FloatTouchView
import io.github.chayanforyou.quickball.ui.floating.FloatPanelView
import io.github.chayanforyou.quickball.ui.floating.SideKickView
import io.github.chayanforyou.quickball.utils.DensityUtils
import io.github.chayanforyou.quickball.utils.getScreenSize
import io.github.chayanforyou.quickball.utils.performHapticFeedback
import kotlin.math.abs
import kotlin.math.roundToInt

@SuppressLint("AccessibilityPolicy")
class QuickBallService : AccessibilityService() {

    companion object {
        private const val TAG = "QuickBallService"

        const val ACTION_ENABLE = "io.github.chayanforyou.quickball.action.ENABLE"
        const val ACTION_DISABLE = "io.github.chayanforyou.quickball.action.DISABLE"
        const val ACTION_STASH = "io.github.chayanforyou.quickball.action.STASH"
        const val ACTION_UNSTASH = "io.github.chayanforyou.quickball.action.UNSTASH"
        const val ACTION_UPDATE_BALL = "io.github.chayanforyou.quickball.action.UPDATE_BALL"
        const val ACTION_UPDATE_PILL = "io.github.chayanforyou.quickball.action.UPDATE_PILL"

        private const val APP_PACKAGE_PREFIX = "io.github.chayanforyou.quickball"
        private val EXCLUDED_APPS = setOf(
            "com.android.systemui",
            "com.android.intentresolver",
            "com.google.android.permissioncontroller",
            "android.uid.system:1000",
            "com.google.android.googlequicksearchbox",
            "android",
            "com.google.android.gms",
            "com.google.android.webview"
        )

        const val EDGE_PADDING_DP = 6f
        const val STASH_DELAY_MS = 2500L
        private const val STASH_ALPHA = 0.4f

        private val KEEP_MENU_OPEN_ACTIONS = setOf(
            MenuAction.VOLUME_UP,
            MenuAction.VOLUME_DOWN,
            MenuAction.BRIGHTNESS_UP,
            MenuAction.BRIGHTNESS_DOWN
        )
    }

    // Window Managers & Views
    private var windowManager: WindowManager? = null
    private var screenSize = 0 to 0
    private var fabView: FloatTouchView? = null
    private var fabParams: WindowManager.LayoutParams? = null
    private var pillView: SideKickView? = null
    private var pillParams: WindowManager.LayoutParams? = null
    private var menuWindow: FrameLayout? = null
    private var menuView: FloatPanelView? = null
    private var menuParams: WindowManager.LayoutParams? = null
    private var actionHandler: QuickBallActionHandler? = null

    // Layout Boundaries & Sizing
    private val fabSizePx get() = DensityUtils.dp2px(floatingBallSize)
    private val edgePaddingPx by lazy { DensityUtils.dp2px(EDGE_PADDING_DP) }
    private val topBoundary by lazy { DensityUtils.dp2px(100f) }
    private val bottomBoundary by lazy { DensityUtils.dp2px(100f) }

    // Position State (Portrait vs Landscape)
    private data class EdgePosition(
        val isOnRight: Boolean = true,
        val yFraction: Float = 0.5f
    )

    private var portraitPosition = EdgePosition()
    private var landscapePosition = EdgePosition()

    private val isLandscape: Boolean
        get() = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    private val prefs by lazy { AppPreference.getInstance(this) }

    private var currentPosition: EdgePosition
        get() = if (isLandscape) landscapePosition else portraitPosition
        set(value) {
            if (isLandscape) {
                landscapePosition = value
                prefs.saveLandscapePosition(value.isOnRight, value.yFraction)
            } else {
                portraitPosition = value
                prefs.savePortraitPosition(value.isOnRight, value.yFraction)
            }
        }

    private var isOnRight: Boolean
        get() = currentPosition.isOnRight
        set(value) {
            currentPosition = currentPosition.copy(isOnRight = value)
        }

    private var savedYFraction: Float
        get() = currentPosition.yFraction
        set(value) {
            currentPosition = currentPosition.copy(yFraction = value)
        }

    // State Variables
    private var isExpanded = false
    private var fabX = 0
    private var fabY = 0

    // Stash / Auto-Hide State
    private var isDragging = false
    private var isStashed = false
    private var isStashing = false
    private var fabAnimator: ValueAnimator? = null
    private val stashHandler = Handler(Looper.getMainLooper())
    private val stashRunnable = Runnable { onInactivityTimeout() }
    private var lastForegroundPackage = ""

    // System Services & State
    private val keyguard by lazy { getSystemService<KeyguardManager>() as KeyguardManager }
    private val isLocked get() = keyguard.isKeyguardLocked

    // Preference Getters
    private val floatingBallSize get() = prefs.ballSize
    private val isStickToEdge get() = prefs.isStickToEdgeEnabled
    private val isEnabled get() = prefs.isQuickBallEnabled
    private val autoHideApps get() = prefs.autoHideApps
    private val showOnLockScreen get() = prefs.isShowOnLockScreenEnabled
    private val hideForLandscape get() = prefs.isHideOnLandscapeEnabled && isLandscape

    /* -------------------- Lifecycle -------------------- */

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        updateScreenSize()
        initFloatingBall()
        registerScreenReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_ENABLE -> showBall()
            ACTION_DISABLE -> hideBall()
            ACTION_STASH -> stashFab()
            ACTION_UNSTASH -> unstashFab()
            ACTION_UPDATE_BALL -> updateBall()
            ACTION_UPDATE_PILL -> updatePill()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        fabAnimator?.cancel()
        stopInactivityTimer()
        removePill()
        removeFabWindow()
        destroyMenuWindow()
        unregisterReceiverSafe(screenReceiver)
        stashHandler.removeCallbacksAndMessages(null)
        actionHandler?.cleanup()
        super.onDestroy()
    }

    /* -------------------- Initialization -------------------- */

    private fun initFloatingBall() {
        portraitPosition = EdgePosition(
            isOnRight = prefs.portraitIsOnRight,
            yFraction = prefs.portraitYFraction
        )
        landscapePosition = EdgePosition(
            isOnRight = prefs.landscapeIsOnRight,
            yFraction = prefs.landscapeYFraction
        )

        actionHandler = QuickBallActionHandler(this) {
            startCollapsingMenu()
            stashFab()
        }

        createFabWindow()
    }

    /* -------------------- Accessibility & System Events -------------------- */

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return

        if (!shouldHandlePackage(packageName)) {
            return
        }

        try {
            onForegroundPackageChanged(packageName)
        } catch (e: Exception) {
            Log.e(TAG, "Error processing foreground package change", e)
        }
    }

    override fun onInterrupt() {}

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateScreenSize()
        refreshBallVisibility()
        recalculatePosition()
    }

    private fun shouldHandlePackage(packageName: String): Boolean {
        return packageName != APP_PACKAGE_PREFIX && packageName !in EXCLUDED_APPS
    }

    private fun onForegroundPackageChanged(packageName: String) {
        val triggeringPackage = lastForegroundPackage
        lastForegroundPackage = packageName

        if (triggeringPackage in autoHideApps) {
            return
        }

        refreshBallVisibility()
    }

    /* -------------------- Visibility Engine -------------------- */

    private fun refreshBallVisibility() {
        if (!isEnabled) {
            hideBall()
            return
        }

        // Lock state
        if (isLocked) {
            if (showOnLockScreen) {
                startCollapsingMenu()
                showBall()
                stashFab(animated = false)
            } else {
                hideBall()
            }
            return
        }

        // Unlock state
        if (hideForLandscape || isAutoHideApp()) {
            hideBall()
            return
        }

        showBall()
    }

    private fun isAutoHideApp(): Boolean {
        val pkg = lastForegroundPackage
        return pkg in autoHideApps
    }

    private fun showBall() {
        if (fabView == null) {
            createFabWindow()
            isStashed = false
            stashFab(animated = false)
            resetInactivityTimer()
        }
    }

    private fun hideBall() {
        stopInactivityTimer()
        removePill()
        removeFabWindow()
        destroyMenuWindow()
    }

    private fun updateBall() {
        val fab = fabView ?: return
        val params = fabParams ?: return

        params.width = fabSizePx
        params.height = fabSizePx

        fab.update()

        recalculatePosition()
    }

    private fun updatePill() {
        val pill = pillView ?: return
        val params = pillParams ?: return
        val wm = windowManager ?: return

        applyPillLayout(pill, params)
        pill.update()

        runCatching { wm.updateViewLayout(pill, params) }
    }

    private fun applyPillLayout(pill: SideKickView, params: WindowManager.LayoutParams) {
        val pillWidth = DensityUtils.dp2px(prefs.pillTouchWidth)
        val pillHeight = DensityUtils.dp2px(prefs.pillHeight)
        val (screenW, _) = screenSize

        params.width = pillWidth
        params.height = pillHeight
        params.x = if (isOnRight) screenW - pillWidth else 0
        params.y = fabY + (fabSizePx - pillHeight) / 2
        pill.onRight = isOnRight
    }

    private open inner class QuickBallGestureListener : GestureListener {
        override fun onSingleTap() {
            if (prefs.isGestureEnabled) {
                executeGestureAction(prefs.singleTapAction)
            } else {
                performHapticFeedback()
                openMenu()
            }
        }

        override fun onDoubleTap() = executeGestureAction(prefs.doubleTapAction)
        override fun onTripleTap() = executeGestureAction(prefs.tripleTapAction)
        override fun onLongPress() = executeGestureAction(prefs.longPressAction)
        override fun onSwipeUp() = executeGestureAction(prefs.swipeUpAction)
        override fun onSwipeDown() = executeGestureAction(prefs.swipeDownAction)
    }

    private fun executeGestureAction(actionName: String) {
        if (!prefs.isGestureEnabled) return
        performHapticFeedback()
        val action = MenuAction.fromName(actionName) ?: return
        if (action == MenuAction.OPEN_MENU) {
            openMenu()
            return
        }
        actionHandler?.onMenuAction(QuickBallMenuItem(action = action))
        resetInactivityTimer()
    }

    private fun openMenu() {
        if (isStashed || isStashing) {
            unstashAndExpandMenu()
        } else {
            expandMenu()
        }
    }

    /* -------------------- FAB Window & Gestures -------------------- */

    @SuppressLint("ClickableViewAccessibility")
    private fun createFabWindow() {
        if (fabView != null) return
        val wm = windowManager ?: return

        fabX = getEdgeX()
        fabY = getEdgeY()

        var initialWindowX = 0
        var initialWindowY = 0
        var screenW = 0
        var screenH = 0

        fabParams = createSystemWindowParams(
            width = fabSizePx,
            height = fabSizePx,
        ).apply {
            x = fabX
            y = fabY
        }

        fabView = FloatTouchView(this).apply {
            setExpanded(isExpanded, animate = false)

            listener = object : QuickBallGestureListener() {
                override fun onTouchDown() {
                    if (isStashing) return
                    stopInactivityTimer()
                    fabAnimator?.cancel()
                    isStashed = false
                    initialWindowX = fabX
                    initialWindowY = fabY

                    val (sw, sh) = screenSize
                    screenW = sw
                    screenH = sh
                }

                override fun onTouchCancel() {
                    isDragging = false
                    resetInactivityTimer()
                }

                override fun onDragMove(dx: Float, dy: Float) {
                    if (isStashing) return
                    isDragging = true
                    stopInactivityTimer()
                    alpha = 1.0f

                    fabX = (initialWindowX + dx).roundToInt().coerceIn(0, screenW - fabSizePx)
                    fabY = (initialWindowY + dy).roundToInt()
                        .coerceIn(topBoundary, screenH - fabSizePx - bottomBoundary)

                    fabParams?.x = fabX
                    fabParams?.y = fabY
                    updateFabViewLayout()
                }

                override fun onDragEnd() {
                    isDragging = false
                    if (!isStashing) snapToEdge()
                }

                override fun onSingleTap() {
                    isDragging = false
                    if (!isStickToEdge) alpha = 1.0f
                    super.onSingleTap()
                }
            }
        }

        wm.addView(fabView, fabParams)

        resetInactivityTimer()
    }

    private fun removeFabWindow() {
        fabView?.let {
            runCatching { windowManager?.removeView(it) }
            fabView = null
        }
    }

    private fun snapToEdge() {
        val (screenW, screenH) = screenSize

        isOnRight = (fabX + fabSizePx / 2) > screenW / 2
        savedYFraction = if (screenH > 0) (fabY.toFloat() / screenH) else 0.5f

        val targetX = getEdgeX(screenW)

        val distance = abs(targetX - fabX)
        val duration = (distance.toFloat() / screenW * 350f).coerceIn(180f, 320f).toLong()

        animateFabX(targetX, duration) {
            resetInactivityTimer()
        }
    }

    private fun stashFab(animated: Boolean = true) {
        if (isExpanded) return

        val view = fabView ?: return
        val params = fabParams ?: return

        if (!isStickToEdge) {
            if (animated) {
                animateFabX(fabX, 250L, alpha = STASH_ALPHA)
            } else {
                view.alpha = STASH_ALPHA
            }
            return
        }
        if (isStashed) return

        val (screenW, _) = screenSize

        val targetX = if (isOnRight) {
            screenW
        } else {
            -fabSizePx
        }

        fabAnimator?.cancel()

        if (animated) {
            isStashing = true
            animateFabX(targetX, 250L, alpha = STASH_ALPHA) {
                isStashing = false
                isStashed = true
                showPill()
            }
        } else {
            view.alpha = STASH_ALPHA
            fabX = targetX
            params.x = targetX
            updateFabViewLayout()
            isStashed = true
            showPill()
        }
    }

    private fun unstashFab(animated: Boolean = true, onFinished: (() -> Unit)? = null) {
        if (!isStashed && !isStashing) {
            onFinished?.invoke()
            return
        }
        val view = fabView ?: return
        val params = fabParams ?: return

        val (screenW, _) = screenSize
        val targetX = getEdgeX(screenW)

        isStashing = false
        hidePill()
        fabAnimator?.cancel()

        if (animated) {
            fabX = params.x

            animateFabX(targetX, 220L, alpha = 1.0f) {
                isStashed = false
                resetInactivityTimer()
                onFinished?.invoke()
            }
        } else {
            view.alpha = 1.0f
            fabX = targetX
            params.x = targetX
            updateFabViewLayout()
            isStashed = false
            resetInactivityTimer()
            onFinished?.invoke()
        }
    }

    private fun animateFabX(
        targetX: Int,
        duration: Long,
        alpha: Float? = null,
        onEnd: (() -> Unit)? = null
    ) {
        val view = fabView ?: return
        val params = fabParams ?: return

        val startAlpha = view.alpha

        fabAnimator?.cancel()
        val animator = ValueAnimator.ofInt(fabX, targetX).apply {
            this.duration = duration
            interpolator = PathInterpolator(0.4f, 0f, 0.2f, 1f)
            addUpdateListener { anim ->
                val currentX = anim.animatedValue as Int
                fabX = currentX
                params.x = currentX
                updateFabViewLayout()

                if (alpha != null) {
                    val fraction = anim.animatedFraction
                    view.alpha = startAlpha + (alpha - startAlpha) * fraction
                }
            }
            var isCancelled = false
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationCancel(animation: Animator) {
                    isCancelled = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (!isCancelled) {
                        onEnd?.invoke()
                    }
                }
            })
        }
        fabAnimator = animator
        animator.start()
    }

    private fun updateFabViewLayout() {
        val wm = windowManager ?: return
        val view = fabView ?: return
        runCatching { wm.updateViewLayout(view, fabParams) }
    }

    /* -------------------- Stashed Pill Management -------------------- */

    @SuppressLint("ClickableViewAccessibility")
    private fun showPill() {
        pillView?.let {
            updatePill()
            it.visibility = View.VISIBLE
            return
        }
        val wm = windowManager ?: return

        val pill = SideKickView(this).apply {
            listener = QuickBallGestureListener()
        }
        val params = createSystemWindowParams(0, 0)
        applyPillLayout(pill, params)

        try {
            wm.addView(pill, params)
        } catch (_: WindowManager.BadTokenException) {
            return
        }
        pillView = pill
        pillParams = params
    }

    private fun hidePill() {
        pillView?.visibility = View.GONE
    }

    private fun removePill() {
        pillView?.let {
            runCatching { windowManager?.removeView(it) }
            pillView = null
        }
        pillParams = null
    }

    /* -------------------- Menu Window Management -------------------- */

    private fun expandMenu() {
        val wm = windowManager ?: return

        isExpanded = true
        fabView?.setExpanded(true)
        stopInactivityTimer()

        val window = menuWindow ?: createMenuWindow(wm) ?: return
        val params = menuParams ?: return

        params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        runCatching { wm.updateViewLayout(window, params) }
        window.visibility = View.VISIBLE

        val existingView = menuView
        if (existingView != null) {
            existingView.animateExpand()
            return
        }

        val panel = FloatPanelView(
            context = this,
            fabX = getEdgeX(),
            fabY = fabY,
            fabSize = fabSizePx,
            screenSize = screenSize,
            items = prefs.selectedMenuItems,
            onDismiss = {
                startCollapsingMenu()
            },
            onDismissFinished = {
                removeMenuWindow()
                resetInactivityTimer()
            },
            onMenuItemClicked = { menuItem ->
                performHapticFeedback()
                actionHandler?.onMenuAction(menuItem)
                if (menuItem.action !in KEEP_MENU_OPEN_ACTIONS) {
                    startCollapsingMenu()
                }
            }
        )
        menuView = panel
        window.addView(panel)
        panel.animateExpand()
    }

    private fun unstashAndExpandMenu() {
        expandMenu()
        menuView?.hideConnectors()
        menuView?.showConnectors()
        unstashFab()
    }

    private fun createMenuWindow(wm: WindowManager): FrameLayout? {
        val window = FrameLayout(this)
        val params = createSystemWindowParams(
            width = WindowManager.LayoutParams.MATCH_PARENT,
            height = WindowManager.LayoutParams.MATCH_PARENT,
        ).apply {
            x = 0
            y = 0
        }
        try {
            wm.addView(window, params)
        } catch (_: WindowManager.BadTokenException) {
            isExpanded = false
            fabView?.setExpanded(false)
            return null
        }
        menuWindow = window
        menuParams = params
        return window
    }

    private fun startCollapsingMenu() {
        isExpanded = false
        fabView?.setExpanded(false)

        val wm = windowManager ?: return
        val window = menuWindow ?: return
        val view = menuView ?: return
        val params = menuParams ?: return

        params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        runCatching { wm.updateViewLayout(window, params) }

        view.animateCollapse()
    }

    private fun removeMenuWindow() {
        isExpanded = false
        fabView?.setExpanded(false)

        menuWindow?.let {
            it.removeAllViews()
            it.visibility = View.GONE
        }
        menuView = null
    }

    private fun destroyMenuWindow() {
        removeMenuWindow()
        menuWindow?.let {
            runCatching { windowManager?.removeView(it) }
            menuWindow = null
        }
        menuParams = null
    }

    /* -------------------- Timers & Helpers -------------------- */

    private fun onInactivityTimeout() {
        if (!isDragging && !isExpanded) {
            stashFab()
        }
    }

    private fun resetInactivityTimer() {
        stashHandler.removeCallbacks(stashRunnable)
        if (!isExpanded && !isStashed) {
            stashHandler.postDelayed(stashRunnable, STASH_DELAY_MS)
        }
    }

    private fun stopInactivityTimer() {
        stashHandler.removeCallbacks(stashRunnable)
    }

    private fun recalculatePosition() {
        if (isExpanded) {
            removeMenuWindow()
        }

        val (screenW, screenH) = screenSize
        fabX = getEdgeX(screenW)
        fabY = getEdgeY(screenH)

        if (isStashed) {
            moveFab(if (isOnRight) screenW else -fabSizePx, STASH_ALPHA)
            showPill()
        } else {
            hidePill()
            moveFab(fabX, 1.0f)
            resetInactivityTimer()
        }
    }

    private fun moveFab(x: Int, alpha: Float) {
        val view = fabView ?: return
        val params = fabParams ?: return
        params.x = x
        params.y = fabY
        view.alpha = alpha
        updateFabViewLayout()
    }

    private fun updateScreenSize() {
        windowManager?.let { screenSize = it.getScreenSize() }
    }

    private fun getEdgeX(screenW: Int = screenSize.first): Int {
        return if (isOnRight) screenW - fabSizePx - edgePaddingPx else edgePaddingPx
    }

    private fun getEdgeY(screenH: Int = screenSize.second): Int {
        return (savedYFraction * screenH).roundToInt()
            .coerceIn(topBoundary, screenH - fabSizePx - bottomBoundary)
    }

    private fun registerScreenReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, filter, RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(screenReceiver, filter)
        }
    }

    private fun unregisterReceiverSafe(receiver: BroadcastReceiver) {
        runCatching { unregisterReceiver(receiver) }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshBallVisibility()
        }
    }

    private fun createSystemWindowParams(
        width: Int,
        height: Int,
    ): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
        }

        return WindowManager.LayoutParams(
            width,
            height,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                fitInsetsTypes = 0
            } else {
                @Suppress("DEPRECATION")
                systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            }
        }
    }
}
