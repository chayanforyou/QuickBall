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
import androidx.core.content.getSystemService
import io.github.chayanforyou.quickball.domain.PreferenceManager
import io.github.chayanforyou.quickball.domain.models.MenuAction
import io.github.chayanforyou.quickball.domain.handlers.QuickBallActionHandler
import io.github.chayanforyou.quickball.domain.models.QuickBallMenuItem
import io.github.chayanforyou.quickball.ui.floating.QuickBallFloatingButton
import io.github.chayanforyou.quickball.ui.floating.QuickBallFloatingMenu
import io.github.chayanforyou.quickball.ui.floating.QuickBallPillView
import io.github.chayanforyou.quickball.utils.DensityUtils
import io.github.chayanforyou.quickball.utils.getScreenSize
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
        const val ACTION_UPDATE_SIZE = "io.github.chayanforyou.quickball.action.UPDATE_SIZE"

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
    }

    // Window Managers & Views
    private var windowManager: WindowManager? = null
    private var fabView: QuickBallFloatingButton? = null
    private var fabParams: WindowManager.LayoutParams? = null
    private var pillView: QuickBallPillView? = null
    private var pillParams: WindowManager.LayoutParams? = null
    private var menuView: QuickBallFloatingMenu? = null
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

    private var currentPosition: EdgePosition
        get() = if (isLandscape) landscapePosition else portraitPosition
        set(value) {
            if (isLandscape) {
                landscapePosition = value
                PreferenceManager.saveLandscapePosition(this, value.isOnRight, value.yFraction)
            } else {
                portraitPosition = value
                PreferenceManager.savePortraitPosition(this, value.isOnRight, value.yFraction)
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
    private var isStashed = false
    private var stashAlpha = 0.4f
    private var isStashing = false
    private var fabAnimator: ValueAnimator? = null
    private val stashHandler = Handler(Looper.getMainLooper())
    private val stashRunnable = Runnable { stashFab() }
    private var lastForegroundPackage = ""

    // System Services & State
    private val keyguard by lazy { getSystemService<KeyguardManager>() as KeyguardManager }
    private val isLocked get() = keyguard.isKeyguardLocked

    // Preference Getters
    private val floatingBallSize get() = PreferenceManager.getBallSize(this)
    private val isStickToEdge get() = PreferenceManager.isStickToEdgeEnabled(this)
    private val isEnabled get() = PreferenceManager.isQuickBallEnabled(this)
    private val autoHideApps get() = PreferenceManager.getAutoHideApps(this)
    private val showOnLockScreen get() = PreferenceManager.isShowOnLockScreenEnabled(this)
    private val hideForLandscape get() = PreferenceManager.isHideOnLandscapeEnabled(this) && isLandscape

    /* -------------------- Lifecycle -------------------- */

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        initFloatingBall()
        registerScreenReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_ENABLE -> showBall()
            ACTION_DISABLE -> hideBall()
            ACTION_STASH -> stashFab()
            ACTION_UNSTASH -> unstashFab()
            ACTION_UPDATE_SIZE -> updateBallSize()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopInactivityTimer()
        removePill()
        removeFabWindow()
        removeMenuWindow()
        unregisterReceiverSafe(screenReceiver)
        stashHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    /* -------------------- Initialization -------------------- */

    private fun initFloatingBall() {
        portraitPosition = EdgePosition(
            isOnRight = PreferenceManager.getPortraitIsOnRight(this),
            yFraction = PreferenceManager.getPortraitYFraction(this)
        )
        landscapePosition = EdgePosition(
            isOnRight = PreferenceManager.getLandscapeIsOnRight(this),
            yFraction = PreferenceManager.getLandscapeYFraction(this)
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
        refreshBallVisibility()
        recalculatePositionForNewScreenSize()
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
        removeMenuWindow()
    }

    private fun updateBallSize() {
        val newSize = fabSizePx
        fabParams?.let { params ->
            params.width = newSize
            params.height = newSize
        }
        recalculatePositionForNewScreenSize()
    }

    /* -------------------- FAB Window & Gestures -------------------- */

    @SuppressLint("ClickableViewAccessibility")
    private fun createFabWindow() {
        if (fabView != null) return
        val wm = windowManager ?: return

        fabX = getEdgeX()
        fabY = getEdgeY()

        val params = createSystemWindowParams(
            width = fabSizePx,
            height = fabSizePx,
        ).apply {
            x = fabX
            y = fabY
        }
        fabParams = params

        var initialWindowX = 0
        var initialWindowY = 0

        val button = QuickBallFloatingButton(this).apply {
            setExpanded(isExpanded, animate = false)

            onTouchDownListener = {
                if (!isStashing) {
                    stopInactivityTimer()
                    fabAnimator?.cancel()
                    removePill()
                    isStashed = false
                    alpha = 1.0f
                    initialWindowX = fabParams?.x ?: 0
                    initialWindowY = fabParams?.y ?: 0
                }
            }

            onDragMoveListener = { dx, dy ->
                val (screenW, screenH) = getScreenSize()

                fabX = (initialWindowX + dx).roundToInt().coerceIn(0, screenW - fabSizePx)
                fabY = (initialWindowY + dy).roundToInt()
                    .coerceIn(topBoundary, screenH - fabSizePx - bottomBoundary)

                fabParams?.let { p ->
                    p.x = fabX
                    p.y = fabY
                    updateFabViewLayout(this, p)
                }
            }

            onClickListener = {
                if (!isStashing) expandMenu()
            }

            onDragEndListener = {
                snapToEdge()
            }
        }

        wm.addView(button, params)
        fabView = button

        resetInactivityTimer()
    }

    private fun removeFabWindow() {
        fabView?.let {
            try {
                windowManager?.removeView(it)
            } catch (_: IllegalArgumentException) {
            }
            fabView = null
        }
    }

    private fun snapToEdge() {
        val (screenW, screenH) = getScreenSize()

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
                animateFabX(fabX, 250L, alpha = stashAlpha)
            } else {
                view.alpha = stashAlpha
            }
            return
        }
        if (isStashed) return

        val (screenW, _) = getScreenSize()

        val targetX = if (isOnRight) {
            screenW
        } else {
            -fabSizePx
        }

        fabAnimator?.cancel()

        if (animated) {
            isStashing = true
            animateFabX(targetX, 250L, alpha = stashAlpha) {
                isStashing = false
                isStashed = true
                showPill()
            }
        } else {
            view.alpha = stashAlpha
            fabX = targetX
            params.x = targetX
            updateFabViewLayout(view, params)
            isStashed = true
            showPill()
        }
    }

    private fun unstashFab(animated: Boolean = true, onFinished: (() -> Unit)? = null) {
        if (!isStashed) {
            onFinished?.invoke()
            return
        }
        val view = fabView ?: return
        val params = fabParams ?: return

        val (screenW, _) = getScreenSize()
        val targetX = getEdgeX(screenW)

        isStashing = false
        removePill()
        fabAnimator?.cancel()

        if (animated) {
            params.x = if (isOnRight) screenW - fabSizePx else 0
            updateFabViewLayout(view, params)
            fabX = params.x

            animateFabX(targetX, 50L, alpha = 1.0f) {
                isStashed = false
                resetInactivityTimer()
                onFinished?.invoke()
            }
        } else {
            view.alpha = 1.0f
            fabX = targetX
            params.x = targetX
            updateFabViewLayout(view, params)
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
                updateFabViewLayout(view, params)

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

    private fun updateFabViewLayout(view: QuickBallFloatingButton, params: WindowManager.LayoutParams) {
        val wm = windowManager ?: return
        try {
            wm.updateViewLayout(view, params)
        } catch (_: Exception) {
        }
    }

    /* -------------------- Stashed Pill Management -------------------- */

    @SuppressLint("ClickableViewAccessibility")
    private fun showPill() {
        if (pillView != null) return
        val wm = windowManager ?: return

        val pillWidth = DensityUtils.dp2px(25f)
        val pillHeight = fabSizePx

        val pill = QuickBallPillView(this).apply {
            onRight = isOnRight
            onSingleTapListener = {
                removePill()
                val targetX = getEdgeX()
                unstashFab()
                expandMenu(targetX)
            }
            onDoubleTapListener = {
                performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            }
            onTripleTapListener = {
                performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
            }
            onLongPressListener = {
                performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
            }
            onSwipeUpListener = {
                performGlobalAction(GLOBAL_ACTION_RECENTS)
            }
            onSwipeDownListener = {
                performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
            }
        }

        val (screenW, _) = getScreenSize()
        val targetX = if (isOnRight) screenW - pillWidth else 0
        val targetY = fabY

        val params = createSystemWindowParams(
            width = pillWidth,
            height = pillHeight,
        ).apply {
            x = targetX
            y = targetY
        }

        try {
            wm.addView(pill, params)
            pillView = pill
            pillParams = params
        } catch (_: Exception) {
        }
    }

    private fun removePill() {
        pillView?.let {
            try {
                windowManager?.removeView(it)
            } catch (_: Exception) {
            }
            pillView = null
        }
        pillParams = null
    }

    /* -------------------- Menu Window Management -------------------- */

    private fun expandMenu(targetX: Int = fabX) {
        val wm = windowManager ?: return

        isExpanded = true
        fabView?.setExpanded(true)
        stopInactivityTimer()

        val existingView = menuView
        val existingParams = menuParams
        if (existingView != null && existingParams != null) {
            existingParams.flags =
                existingParams.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
            try {
                wm.updateViewLayout(existingView, existingParams)
            } catch (_: Exception) {
            }
            existingView.animateExpand()
            return
        }

        val params = createSystemWindowParams(
            width = WindowManager.LayoutParams.MATCH_PARENT,
            height = WindowManager.LayoutParams.MATCH_PARENT,
        ).apply {
            x = 0
            y = 0
        }
        menuParams = params

        val overlay = QuickBallFloatingMenu(
            context = this,
            fabSize = fabSizePx,
            items = getMenuItems(),
            fabX = targetX,
            fabY = fabY,
            onDismiss = {
                startCollapsingMenu()
            },
            onDismissFinished = {
                removeMenuWindow()
                resetInactivityTimer()
            },
            onMenuItemClicked = { menuItem ->
                actionHandler?.onMenuAction(menuItem)
                if (menuItem.action !in setOf(
                        MenuAction.VOLUME_UP,
                        MenuAction.VOLUME_DOWN,
                        MenuAction.BRIGHTNESS_UP,
                        MenuAction.BRIGHTNESS_DOWN
                    )
                ) {
                    startCollapsingMenu()
                }
            }
        )

        wm.addView(overlay, params)
        menuView = overlay

        overlay.animateExpand()
    }

    private fun startCollapsingMenu() {
        isExpanded = false
        fabView?.setExpanded(false)

        val wm = windowManager ?: return
        val view = menuView ?: return
        val params = menuParams ?: return

        params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        try {
            wm.updateViewLayout(view, params)
        } catch (_: Exception) {
        }

        view.animateCollapse()
    }

    private fun removeMenuWindow() {
        isExpanded = false
        fabView?.setExpanded(false)

        menuView?.let {
            try {
                windowManager?.removeView(it)
            } catch (_: IllegalArgumentException) {
            }
            menuView = null
        }
        menuParams = null
    }

    private fun getMenuItems(): List<QuickBallMenuItem> {
        return PreferenceManager.getSelectedMenuItems(this)
    }

    /* -------------------- Timers & Helpers -------------------- */

    private fun resetInactivityTimer() {
        stashHandler.removeCallbacks(stashRunnable)
        if (!isExpanded && !isStashed) {
            stashHandler.postDelayed(stashRunnable, STASH_DELAY_MS)
        }
    }

    private fun stopInactivityTimer() {
        stashHandler.removeCallbacks(stashRunnable)
    }

    private fun recalculatePositionForNewScreenSize() {
        if (isExpanded) {
            removeMenuWindow()
        }

        val (screenW, screenH) = getScreenSize()

        if (isStashed) {
            fabX = getEdgeX(screenW)
            fabY = getEdgeY(screenH)

            fabParams?.let { p ->
                p.x = if (isOnRight) screenW else -fabSizePx
                p.y = fabY
                fabView?.let { v ->
                    v.alpha = stashAlpha
                    updateFabViewLayout(v, p)
                }
            }

            removePill()
            showPill()
        } else {
            removePill()

            fabX = getEdgeX(screenW)
            fabY = getEdgeY(screenH)

            fabParams?.let { p ->
                p.x = fabX
                p.y = fabY
                fabView?.let { v ->
                    v.alpha = 1.0f
                    updateFabViewLayout(v, p)
                }
            }
            isStashed = false
            resetInactivityTimer()
        }
    }

    private fun getEdgeX(screenW: Int = getScreenSize().first): Int {
        return if (isOnRight) screenW - fabSizePx - edgePaddingPx else edgePaddingPx
    }

    private fun getEdgeY(screenH: Int = getScreenSize().second): Int {
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
