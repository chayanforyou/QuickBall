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
    private var lastPackageName: String? = null
    private val ignoredPackages = setOf("com.android.systemui", "android")

    private val stashHandler = Handler(Looper.getMainLooper())
    private val stashRunnable = Runnable { stashBall() }
    private val stashDelay = 2500L

    private val keyguardManager: KeyguardManager by lazy {
        getSystemService(KEYGUARD_SERVICE) as KeyguardManager
    }

    private val isKeyguardLocked: Boolean
        get() = keyguardManager.isKeyguardLocked

    private val selectedApps: Set<String>
        get() = PreferenceManager.getSelectedApps(this)

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
            showBall()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_ENABLE_QUICK_BALL -> showBall()
            ACTION_DISABLE_QUICK_BALL -> hideBall()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        adjustPosition()
    }

    override fun onDestroy() {
        super.onDestroy()
        hideBall()
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

    private fun showBall() {
        floatingBall?.show()
        stashBall(false)
        resetStashTimer()
    }

    private fun hideBall() {
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

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.source == null) return
        val pkg = getCurrentAppPackage() ?: return

        if (isKeyguardLocked || pkg in ignoredPackages || pkg == lastPackageName) return

        lastPackageName = pkg
        if (pkg in selectedApps) handleMonitoredAppEvent()
        else handleNonMonitoredAppEvent()
    }

    override fun onInterrupt() {}

    private fun registerScreenReceiver() {
        IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }.also {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(screenReceiver, it, RECEIVER_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                registerReceiver(screenReceiver, it)
            }
        }
    }

    private fun unregisterReceiverSafe(receiver: BroadcastReceiver) {
        runCatching { unregisterReceiver(receiver) }
    }

    private fun handleMonitoredAppEvent() {
        floatingBall?.takeIf { it.isVisible() }?.let { hideBall() }
    }

    private fun handleNonMonitoredAppEvent() {
        if (!PreferenceManager.isQuickBallEnabled(this)) return
        showBall()
    }

    private fun handleScreenLocked() {
        floatingBall?.takeIf { it.isVisible() }?.let { hideBall() }
    }

    private fun handleScreenUnlocked() {
        if (!PreferenceManager.isQuickBallEnabled(this)) return

        getCurrentAppPackage()?.let { pkg ->
            if (pkg in selectedApps) return
        }

        showBall()
        adjustPosition()
    }

    private fun getCurrentAppPackage(): String? {
        return try {
            val root = rootInActiveWindow
            root?.packageName?.toString()
        } catch (_: Exception) {
            null
        }
    }

    private fun adjustPosition() {
        val ball = floatingBall ?: return
        if (!ball.isVisible() || isDragging) return

        when (resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> ball.moveToLandscapePosition()
            else -> ball.moveToPortraitPosition()
        }
        ball.forceStash()
    }
}
