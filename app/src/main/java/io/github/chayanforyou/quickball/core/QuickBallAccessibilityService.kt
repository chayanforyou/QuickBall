package io.github.chayanforyou.quickball.core

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import io.github.chayanforyou.quickball.domain.PreferenceManager
import io.github.chayanforyou.quickball.domain.handlers.QuickBallActionHandler
import io.github.chayanforyou.quickball.ui.floating.FloatingActionButton

class QuickBallAccessibilityService : AccessibilityService() {

    companion object {
        const val ACTION_ENABLE_QUICK_BALL = "io.github.chayanforyou.quickball.ENABLE_QUICK_BALL"
        const val ACTION_DISABLE_QUICK_BALL = "io.github.chayanforyou.quickball.DISABLE_QUICK_BALL"
    }

    private var floatingBall: FloatingActionButton? = null
    private var isDragging = false
    private var lastEventTimeStamp = 0L

    private val debounceDelay = 100L
    private val stashDelay = 2500L

    private val stashHandler by lazy { Handler(Looper.getMainLooper()) }
    private val stashRunnable = Runnable { stashBall() }

    private val keyguardManager: KeyguardManager by lazy {
        getSystemService(KEYGUARD_SERVICE) as KeyguardManager
    }
    
    private inline val isKeyguardLocked: Boolean
        get() = keyguardManager.isKeyguardLocked

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> handleScreenLocked()
                Intent.ACTION_USER_PRESENT -> handleScreenUnlocked()
                Intent.ACTION_SCREEN_ON ->
                    if (isKeyguardLocked) handleScreenLocked() else handleScreenUnlocked()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        initializeFloatingBall()
        registerScreenReceiver()

        if (PreferenceManager.isQuickBallEnabled(this) && !isKeyguardLocked) {
            showFloatingBall()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_ENABLE_QUICK_BALL -> showFloatingBall()
            ACTION_DISABLE_QUICK_BALL -> hideFloatingBall()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyCurrentOrientationPositioning()
    }

    override fun onDestroy() {
        super.onDestroy()
        hideFloatingBall()
        unregisterReceiverSafe(screenReceiver)
        stashHandler.removeCallbacksAndMessages(null)
    }

    private fun initializeFloatingBall() {
        val actionHandler = QuickBallActionHandler(this) {
            floatingBall?.takeIf { it.isMenuOpen() }?.forceStash()
        }

        floatingBall = FloatingActionButton(this, actionHandler).apply {
            initialize()
            setOnStashStateChangedListener(::onStashStateChanged)
            setOnDragStateChangedListener(::onDragStateChanged)
            setOnMenuStateChangedListener(::onMenuStateChanged)
        }
    }

    private fun showFloatingBall() {
        floatingBall?.show()
        stashBall(false)
        resetStashTimer()
    }

    private fun hideFloatingBall() {
        cancelStashTimer()
        floatingBall?.hide()
    }

    private fun stashBall(animated: Boolean = true) {
        if (!isDragging && floatingBall?.isMenuOpen() != true) {
            floatingBall?.stash(animated)
        }
    }

    private fun unstashBall() {
        floatingBall?.unstash()
    }

    private fun resetStashTimer() {
        stashHandler.removeCallbacks(stashRunnable)
        stashHandler.postDelayed(stashRunnable, stashDelay)
    }

    private fun cancelStashTimer() {
        stashHandler.removeCallbacks(stashRunnable)
    }

    private fun onStashStateChanged(isStashed: Boolean) {
        if (!isStashed) resetStashTimer()
    }

    private fun onDragStateChanged(isDragging: Boolean) {
        this.isDragging = isDragging
        if (isDragging) cancelStashTimer() else resetStashTimer()
    }

    private fun onMenuStateChanged(isMenuOpen: Boolean) {
        if (!isMenuOpen) resetStashTimer()
    }

    private inline fun withDebounce(action: () -> Unit) {
        val currentTime = SystemClock.uptimeMillis()
        if (currentTime - lastEventTimeStamp > debounceDelay) {
            action()
            lastEventTimeStamp = currentTime
        }
    }

    private fun handleMonitoredAppEvent() = withDebounce {
        if (floatingBall?.isVisible() == true) hideFloatingBall()
    }

    private fun handleNonMonitoredAppEvent() = withDebounce {
        if (PreferenceManager.isQuickBallEnabled(this) && !isKeyguardLocked) {
            if (floatingBall?.isVisible() != true) showFloatingBall()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.source == null) return
        val root = rootInActiveWindow ?: return
        val pkg = root.packageName?.toString() ?: return

        val selectedApps = PreferenceManager.getSelectedApps(this)
        if (pkg in selectedApps) handleMonitoredAppEvent() else handleNonMonitoredAppEvent()
    }

    override fun onInterrupt() {}

    private fun registerScreenReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(screenReceiver, filter)
        }
    }

    private fun unregisterReceiverSafe(receiver: BroadcastReceiver) {
        runCatching { unregisterReceiver(receiver) }
    }

    private fun handleScreenLocked() {
        if (floatingBall?.isVisible() == true) {
            hideFloatingBall()
        }
    }

    private fun handleScreenUnlocked() {
        if (PreferenceManager.isQuickBallEnabled(this)) {
            showFloatingBall()
            applyCurrentOrientationPositioning()
        }
    }

    private fun applyCurrentOrientationPositioning() {
        val ball = floatingBall ?: return
        if (!ball.isVisible() || isDragging) return

        when (resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> ball.moveToLandscapePosition()
            else -> ball.moveToPortraitPosition()
        }
        ball.forceStash()
    }
}
