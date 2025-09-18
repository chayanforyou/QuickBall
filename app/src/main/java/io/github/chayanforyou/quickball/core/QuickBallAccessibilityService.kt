package io.github.chayanforyou.quickball.core

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import io.github.chayanforyou.quickball.domain.PreferenceManager
import io.github.chayanforyou.quickball.domain.handlers.QuickBallActionHandler
import io.github.chayanforyou.quickball.ui.floating.FloatingActionButton

class QuickBallAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "QuickBall"
        const val ACTION_ENABLE_QUICK_BALL = "io.github.chayanforyou.quickball.ENABLE_QUICK_BALL"
        const val ACTION_DISABLE_QUICK_BALL = "io.github.chayanforyou.quickball.DISABLE_QUICK_BALL"
    }

    private var floatingActionButton: FloatingActionButton? = null
    private var isDragging = false
    
    // Auto-stash functionality
    private val stashHandler = Handler(Looper.getMainLooper())
    private val stashRunnable = Runnable { stashBall() }
    private val stashDelay = 2500L

    override fun onServiceConnected() {
        super.onServiceConnected()
        
        initializeFloatingBall()
        
        // Only show the ball if Quick Ball is enabled in settings
        if (PreferenceManager.isQuickBallEnabled(this)) {
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

    override fun onDestroy() {
        super.onDestroy()
        
        hideFloatingBall()
        stashHandler.removeCallbacksAndMessages(null)
    }

    private fun initializeFloatingBall() {
        val actionHandler = QuickBallActionHandler(this)
        floatingActionButton = FloatingActionButton(this, actionHandler)
        floatingActionButton?.initialize()
        
        // Set up callbacks
        floatingActionButton?.setOnStashStateChangedListener { isStashed ->
            onStashStateChanged(isStashed)
        }
        
        floatingActionButton?.setOnDragStateChangedListener { isDragging ->
            onDragStateChanged(isDragging)
        }
        
        floatingActionButton?.setOnMenuStateChangedListener { isMenuOpen ->
            onMenuStateChanged(isMenuOpen)
        }
    }

    private fun showFloatingBall() {
        floatingActionButton?.show()
        stashBall()
        resetStashTimer()
    }

    private fun hideFloatingBall() {
        cancelStashTimer()
        floatingActionButton?.hide()
    }

    private fun stashBall() {
        if (!isDragging && floatingActionButton?.isMenuOpen() != true) {
            floatingActionButton?.stash()
        }
    }

    private fun unstashBall() {
        floatingActionButton?.unstash()
    }

    private fun resetStashTimer() {
        stashHandler.removeCallbacks(stashRunnable)
        stashHandler.postDelayed(stashRunnable, stashDelay)
    }

    private fun cancelStashTimer() {
        stashHandler.removeCallbacks(stashRunnable)
    }

    private fun onStashStateChanged(isStashed: Boolean) {
        Log.d(TAG, "Stash state changed: $isStashed")
        if (!isStashed) {
            // Reset timer when ball is unstashed
            resetStashTimer()
        }
    }
    
    private fun onDragStateChanged(isDragging: Boolean) {
        this.isDragging = isDragging
        Log.d(TAG, "Drag state changed: $isDragging")
        
        if (isDragging) {
            // Cancel stash timer when dragging starts
            cancelStashTimer()
        } else {
            // Reset stash timer when dragging ends
            resetStashTimer()
        }
    }

    private fun onMenuStateChanged(isMenuOpen: Boolean) {
        Log.d(TAG, "Menu state changed: $isMenuOpen")
        
        if (!isMenuOpen) {
            // Reset stash timer when menu closes
            resetStashTimer()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Handle accessibility events if needed
    }

    override fun onInterrupt() {
        // Handle service interruption,
    }
}

