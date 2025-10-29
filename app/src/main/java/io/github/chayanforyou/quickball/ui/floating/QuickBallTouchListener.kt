package io.github.chayanforyou.quickball.ui.floating

import android.annotation.SuppressLint
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import kotlin.math.abs

class QuickBallTouchListener(
    private val displayMetrics: DisplayMetrics,
    private val windowManager: WindowManager,
    private val ballSize: Int,
    private val topBoundary: Int,
    private val bottomBoundary: Int,
    private val floatingButton: QuickBallFloatingButton
) : View.OnTouchListener {

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(view: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (floatingButton.isMenuOpen()) return true

                if (floatingButton.isStashed() || floatingButton.isAnimatingStash()) {
                    isDragging = false
                    return true
                }

                val layoutParams = view.layoutParams as WindowManager.LayoutParams
                initialX = layoutParams.x
                initialY = layoutParams.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isDragging = false
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (floatingButton.isMenuOpen()) return true
                if (floatingButton.isStashed() || floatingButton.isAnimatingStash()) return true

                val deltaX = (event.rawX - initialTouchX).toInt()
                val deltaY = (event.rawY - initialTouchY).toInt()

                if ((abs(deltaX) > 10 || abs(deltaY) > 10) && !isDragging) {
                    isDragging = true
                    floatingButton.setDragging(true)
                }

                if (isDragging) {
                    updateBallPosition(view, deltaX, deltaY)
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                if (isDragging) {
                    floatingButton.setDragging(false)
                    floatingButton.snapToEdge(view)
                } else {
                    floatingButton.handleBallClick()
                }
                return true
            }
        }
        return false
    }

    private fun updateBallPosition(view: View, deltaX: Int, deltaY: Int) {
        val layoutParams = view.layoutParams as WindowManager.LayoutParams

        val newX = (initialX + deltaX).coerceIn(0, displayMetrics.widthPixels - ballSize)
        val newY = (initialY + deltaY).coerceIn(topBoundary, displayMetrics.heightPixels - ballSize - bottomBoundary)

        if (layoutParams.x != newX || layoutParams.y != newY) {
            layoutParams.x = newX
            layoutParams.y = newY
            windowManager.updateViewLayout(view, layoutParams)
        }
    }
}
