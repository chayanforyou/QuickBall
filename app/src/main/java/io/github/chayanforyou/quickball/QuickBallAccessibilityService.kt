package io.github.chayanforyou.quickball

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.util.Log

class QuickBallAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "QuickBall"
    }

    private var floatingActionButton: FloatingActionButton? = null
    private var isDragging = false
    
    // Auto-stash functionality
    private val stashHandler = Handler(Looper.getMainLooper())
    private val stashRunnable = Runnable { stashBall() }
    private val stashDelay = 3000L // 3 seconds

    override fun onServiceConnected() {
        super.onServiceConnected()
        initializeFloatingBall()
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

        showFloatingBall()
        stashBall()
    }

    private fun showFloatingBall() {
        floatingActionButton?.show()
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

    private fun onBallTapped() {
        Log.d(TAG, "Ball tapped")
        // Add your tap functionality here
        // For example, show a menu, open an app, etc.
        
        // Reset stash timer after tap
        resetStashTimer()
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

    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) {
        // Handle accessibility events if needed
    }

    override fun onInterrupt() {
        // Handle service interruption
    }
}

