package io.github.chayanforyou.quickball.core

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.*
import android.content.res.Configuration
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import io.github.chayanforyou.quickball.domain.PreferenceManager
import io.github.chayanforyou.quickball.domain.handlers.QuickBallActionHandler
import io.github.chayanforyou.quickball.ui.floating.QuickBallFloatingButton
import io.github.chayanforyou.quickball.utils.ToastUtil

class QuickBallService : AccessibilityService() {

    companion object {
        const val ACTION_ENABLE_QUICK_BALL = "io.github.chayanforyou.quickball.ENABLE_QUICK_BALL"
        const val ACTION_DISABLE_QUICK_BALL = "io.github.chayanforyou.quickball.DISABLE_QUICK_BALL"
    }

    private var floatingBall: QuickBallFloatingButton? = null
    private var isDragging = false
    private var lastPackage: String? = null

    private val handler = Handler(Looper.getMainLooper())
    private val stashDelay = 2500L
    private val stashRunnable = Runnable { stashBall() }

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
            ACTION_ENABLE_QUICK_BALL -> showBall()
            ACTION_DISABLE_QUICK_BALL -> hideBall()
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
                stash(animated = true)
            }
        }

        floatingBall = QuickBallFloatingButton(this, actionHandler).apply {
            initialize()
            setOnStashStateChangedListener { if (!it) resetStashTimer() }
            setOnDragStateChangedListener(::onDragStateChanged)
            setOnMenuStateChangedListener { if (!it) resetStashTimer() }
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
                ball.stash()
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
        val pkg = getCurrentAppPackage()
        return pkg == null || pkg in autoHideApps
    }

    /* -------------------- Ball actions -------------------- */

    private fun showBall() {
        floatingBall?.takeUnless { it.isVisible() }?.apply {
            show()
            stash()
            resetStashTimer()
        }
    }

    private fun hideBall() {
        floatingBall?.apply {
            hideMenuIfOpen()
            hide()
            cancelStashTimer()
        }
    }

    private fun stashBall() {
        floatingBall?.takeIf {
            !isDragging && !it.isMenuOpen()
        }?.stash(animated = true)
    }

    /* -------------------- Timers -------------------- */

    private fun resetStashTimer() {
        handler.removeCallbacks(stashRunnable)
        handler.postDelayed(stashRunnable, stashDelay)
    }

    private fun cancelStashTimer() {
        handler.removeCallbacks(stashRunnable)
    }

    /* -------------------- Drag -------------------- */

    private fun onDragStateChanged(dragging: Boolean) {
        isDragging = dragging
        if (dragging) cancelStashTimer() else resetStashTimer()
    }

    /* -------------------- Accessibility -------------------- */

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = getCurrentAppPackage() ?: return

        // Skip System UI completely
        if (pkg == "com.android.systemui") return

        if (pkg == lastPackage) return
        lastPackage = pkg

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

        val ball = floatingBall ?: return
        if (!ball.isVisible() || isDragging) return

        adjustPosition()
    }

    /* -------------------- Helpers -------------------- */

    private fun adjustPosition() {
        val ball = floatingBall ?: return

        handler.post {
            when (resources.configuration.orientation) {
                Configuration.ORIENTATION_LANDSCAPE -> ball.moveToLandscapePosition()
                else -> ball.moveToPortraitPosition()
            }
        }

//        handler.post {
//            if (ball.isVisible() && !isDragging) {
//                when (resources.configuration.orientation) {
//                    Configuration.ORIENTATION_LANDSCAPE -> ball.moveToLandscapePosition()
//                    else -> ball.moveToPortraitPosition()
//                }
//                ball.forceStash()
//            }
//        }
    }


    private fun getCurrentAppPackage(): String? {
        return runCatching {
            rootInActiveWindow?.packageName?.toString()
        }.getOrNull() ?: lastPackage
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
