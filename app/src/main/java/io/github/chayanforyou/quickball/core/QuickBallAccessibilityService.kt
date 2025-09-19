package io.github.chayanforyou.quickball.core

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
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
    private var isScreenLocked = false
    
    // Auto-stash functionality
    private val stashHandler = Handler(Looper.getMainLooper())
    private val stashRunnable = Runnable { stashBall() }
    private val stashDelay = 2500L
    
    // Screen lock detection
    private val keyguardManager: KeyguardManager by lazy {
        getSystemService(KEYGUARD_SERVICE) as KeyguardManager
    }
    
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    handleScreenLocked()
                }
                Intent.ACTION_USER_PRESENT -> {
                    handleScreenUnlocked()
                }
                Intent.ACTION_SCREEN_ON -> {
                    if (keyguardManager.isKeyguardLocked) {
                        handleScreenLocked()
                    } else {
                        handleScreenUnlocked()
                    }
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        
        initializeFloatingBall()
        registerScreenReceiver()
        
        // Only show the ball if Quick Ball is enabled in settings and screen is not locked
        if (PreferenceManager.isQuickBallEnabled(this) && !isScreenCurrentlyLocked()) {
            showFloatingBall()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && !intent.action.isNullOrEmpty()) {
            when (intent.action) {
                ACTION_ENABLE_QUICK_BALL -> {
                    showFloatingBall()
                }
                ACTION_DISABLE_QUICK_BALL -> {
                    hideFloatingBall()
                }
            }
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
        unregisterScreenReceiver()
        stashHandler.removeCallbacksAndMessages(null)
    }

    private fun initializeFloatingBall() {
        val actionHandler = QuickBallActionHandler(this)
        floatingBall = FloatingActionButton(this, actionHandler)
        floatingBall?.initialize()
        
        // Set up callbacks
        floatingBall?.setOnStashStateChangedListener { isStashed ->
            onStashStateChanged(isStashed)
        }
        
        floatingBall?.setOnDragStateChangedListener { isDragging ->
            onDragStateChanged(isDragging)
        }
        
        floatingBall?.setOnMenuStateChangedListener { isMenuOpen ->
            onMenuStateChanged(isMenuOpen)
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
        if (!isStashed) {
            resetStashTimer()
        }
    }
    
    private fun onDragStateChanged(isDragging: Boolean) {
        this.isDragging = isDragging
        if (isDragging) {
            cancelStashTimer()
        } else {
            resetStashTimer()
        }
    }

    private fun onMenuStateChanged(isMenuOpen: Boolean) {
        if (!isMenuOpen) {
            resetStashTimer()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Handle accessibility events if needed
    }

    override fun onInterrupt() {
        // Handle service interruption,
    }
    
    private fun registerScreenReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(screenReceiver, filter)
    }
    
    private fun unregisterScreenReceiver() {
        try {
            unregisterReceiver(screenReceiver)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }
    
    private fun isScreenCurrentlyLocked(): Boolean {
        return keyguardManager.isKeyguardLocked
    }
    
    private fun handleScreenLocked() {
        if (!isScreenLocked) {
            isScreenLocked = true

            if (floatingBall?.isVisible() == true) {
                hideFloatingBall()
            }
        }
    }
    
    private fun handleScreenUnlocked() {
        if (isScreenLocked) {
            isScreenLocked = false

            if (PreferenceManager.isQuickBallEnabled(this)) {
                showFloatingBall()
                // Apply current orientation positioning
                applyCurrentOrientationPositioning()
            }
        }
    }
    
    private fun applyCurrentOrientationPositioning() {
        if (floatingBall?.isVisible() == true && !isDragging) {
            val currentOrientation = resources.configuration.orientation
            when (currentOrientation) {
                Configuration.ORIENTATION_LANDSCAPE -> {
                    floatingBall?.moveToLandscapePosition()
                    floatingBall?.forceStash()
                }
                Configuration.ORIENTATION_PORTRAIT -> {
                    floatingBall?.moveToPortraitPosition()
                    floatingBall?.forceStash()
                }
                else -> {
                    floatingBall?.moveToPortraitPosition()
                    floatingBall?.forceStash()
                }
            }
        }
    }
}

