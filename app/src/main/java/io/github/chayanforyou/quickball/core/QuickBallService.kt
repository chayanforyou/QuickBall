package io.github.chayanforyou.quickball.core

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import io.github.chayanforyou.quickball.domain.PreferenceManager
import io.github.chayanforyou.quickball.domain.handlers.QuickBallActionHandler
import io.github.chayanforyou.quickball.ui.floating.QuickBallFloatingButton
import io.github.chayanforyou.quickball.utils.ToastUtil

@SuppressLint("AccessibilityPolicy")
class QuickBallService : AccessibilityService() {

    companion object {
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
    }

    private var floatingBall: QuickBallFloatingButton? = null
    private var isDragging = false
    private var lastForegroundPackage = ""

    private val handler = Handler(Looper.getMainLooper())
    private val inactivityDelay = 2500L
    private val inactivityRunnable = Runnable { onInactivityTimeout() }

    private val keyguard by lazy {
        getSystemService(KEYGUARD_SERVICE) as KeyguardManager
    }

    /* -------------------- Derived state -------------------- */

    private val isEnabled get() = PreferenceManager.isQuickBallEnabled(this)
    private val autoHideApps get() = PreferenceManager.getAutoHideApps(this)
    private val isLocked get() = keyguard.isKeyguardLocked

    private val showOnLockScreen: Boolean
        get() = PreferenceManager.isShowOnLockScreenEnabled(this)

    private val hideForLandscape: Boolean
        get() = PreferenceManager.isHideOnLandscapeEnabled(this) &&
                resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    /* -------------------- Lifecycle -------------------- */

    override fun onServiceConnected() {
        super.onServiceConnected()
        initFloatingBall()
        registerScreenReceiver()
        updateBallVisibility()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_ENABLE -> showBall()
            ACTION_DISABLE -> hideBall()
            ACTION_STASH -> stashBall()
            ACTION_UNSTASH -> unstashBall()
            ACTION_UPDATE_SIZE -> updateBallSize()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        hideBall()
        unregisterReceiverSafe(screenReceiver)
        handler.removeCallbacksAndMessages(null)
        ToastUtil.destroy()
        super.onDestroy()
    }
    /* -------------------- Initialization -------------------- */

    private fun initFloatingBall() {
        val actionHandler = QuickBallActionHandler(this) {
            floatingBall?.apply {
                hideMenuIfOpen()
                stash()
            }
        }

        floatingBall = QuickBallFloatingButton(this, actionHandler).apply {
            initialize()
            onInteractionEnded = ::resetInactivityTimer
            onDragStateChanged = ::onDragStateChanged
        }
    }

    /* -------------------- Visibility engine -------------------- */

    private fun updateBallVisibility() {
        val ball = floatingBall ?: return

        if (!isEnabled) {
            hideBall()
            return
        }

        // Lock state
        if (isLocked) {
            if (showOnLockScreen) {
                ball.hideMenuIfOpen()
                showBall()
                ball.stash(animated = false)
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

    /* -------------------- Ball actions -------------------- */

    private fun showBall() {
        floatingBall?.takeUnless { it.isVisible() }?.apply {
            show()
            stash(animated = false)
            startInactivityTimer()
        }
    }

    private fun hideBall() {
        floatingBall?.apply {
            hide()
            cancelInactivityTimer()
        }
    }

    private fun stashBall() {
        floatingBall?.takeIf {
            it.isVisible()
        }?.forceStash()
    }

    private fun unstashBall() {
        floatingBall?.takeIf {
            it.isVisible()
        }?.unstash()
    }

    private fun updateBallSize() {
        floatingBall?.updateSize()
    }

    private fun onInactivityTimeout() {
        floatingBall?.takeIf {
            !isDragging && !it.isMenuOpen()
        }?.stash()
    }

    /* -------------------- Timers -------------------- */

    private fun startInactivityTimer() {
        handler.postDelayed(inactivityRunnable, inactivityDelay)
    }

    private fun cancelInactivityTimer() {
        handler.removeCallbacks(inactivityRunnable)
    }

    private fun resetInactivityTimer() {
        cancelInactivityTimer()
        startInactivityTimer()
    }

    /* -------------------- Drag -------------------- */

    private fun onDragStateChanged(dragging: Boolean) {
        isDragging = dragging
        if (dragging) cancelInactivityTimer() else resetInactivityTimer()
    }

    /* -------------------- Accessibility -------------------- */

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Extract and validate package name
        val packageName = event.packageName?.toString() ?: return

        // Skip own package or app is excluded
        if (!shouldHandlePackage(packageName)) {
            return
        }

        try {
            onForegroundPackageChanged(packageName)
        } catch (_: Exception) {
            println("Error processing package locking for $packageName")
        }
    }

    private fun shouldHandlePackage(packageName: String): Boolean {
        // Skip excluded packages
        if (packageName == APP_PACKAGE_PREFIX ||
            packageName in EXCLUDED_APPS
        ) {
            return false
        }

        // Skip known recents classes
        return true
    }

    private fun onForegroundPackageChanged(packageName: String) {
        val currentForegroundPackage = packageName
        val triggeringPackage = lastForegroundPackage
        lastForegroundPackage = currentForegroundPackage

        // Skip if triggering package is excluded
        if (triggeringPackage in autoHideApps) {
            return
        }

        updateBallVisibility()
    }

    override fun onInterrupt() {}

    /* -------------------- Screen receiver -------------------- */

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateBallVisibility()
        }
    }

    /* -------------------- Orientation -------------------- */

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateBallVisibility()
        adjustPosition()
    }

    /* -------------------- Helpers -------------------- */

    private fun adjustPosition() {
        val ball = floatingBall ?: return

        handler.post {
            if (ball.isVisible() && !isDragging) {
                when (resources.configuration.orientation) {
                    Configuration.ORIENTATION_LANDSCAPE -> ball.moveToLandscapePosition()
                    else -> ball.moveToPortraitPosition()
                }
            }
        }
    }


    private fun QuickBallFloatingButton.hideMenuIfOpen() {
        if (isMenuOpen()) hideMenu()
    }

    /* -------------------- Receiver utils -------------------- */

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
}
