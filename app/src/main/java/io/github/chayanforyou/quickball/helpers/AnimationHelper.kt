package io.github.chayanforyou.quickball.helpers

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.graphics.Point
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import io.github.chayanforyou.quickball.ui.floating.QuickBallFloatingMenu

class AnimationHelper {

    companion object {
        private const val DURATION = 220L
        private const val LAG_BETWEEN_ITEMS = 20L
        private val INTERP_OPEN = OvershootInterpolator(0.9f)
        private val INTERP_CLOSE = AccelerateDecelerateInterpolator()

//        private val INTERP_OPEN = SpringInterpolator(0.6f, 0.57f)
//        private val INTERP_CLOSE = SpringInterpolator(0.99f, 0.3f)
    }

    private enum class ActionType { OPENING, CLOSING }

    private var animating = false
    private var menu: QuickBallFloatingMenu? = null
    private var animationCompletionListener: (() -> Unit)? = null

    fun setMenu(menu: QuickBallFloatingMenu) {
        this.menu = menu
    }

    fun animateMenuOpening(center: Point) {
        val currentMenu = menu ?: return
        if (animating) return

        setAnimating(true)

        val subItems = currentMenu.getSubActionItems()
        if (subItems.isEmpty()) {
            setAnimating(false)
            return
        }

        var lastAnimation: Animator? = null

        subItems.forEachIndexed { i, item ->
            val view = item.view

            // Initial state
            view.scaleX = 0f
            view.scaleY = 0f
            view.alpha = 0f

            // Target translation: from center to final position
            val targetX = item.x - center.x + item.width / 2f
            val targetY = item.y - center.y + item.height / 2f

            val animation = ObjectAnimator.ofPropertyValuesHolder(
                view,
                PropertyValuesHolder.ofFloat(View.TRANSLATION_X, targetX),
                PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, targetY),
                PropertyValuesHolder.ofFloat(View.ROTATION, 720f),
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f),
                PropertyValuesHolder.ofFloat(View.ALPHA, 1f)
            ).apply {
                duration = DURATION
                interpolator = INTERP_OPEN
                startDelay = (subItems.size - i) * LAG_BETWEEN_ITEMS
                addListener(SubActionItemAnimationListener(item, ActionType.OPENING))
            }

            if (i == 0) {
                lastAnimation = animation
            }

            animation.start()
        }

        lastAnimation?.addListener(LastAnimationListener())
    }

    fun animateMenuClosing(center: Point) {
        val currentMenu = menu ?: return
        if (animating) return

        setAnimating(true)

        val subItems = currentMenu.getSubActionItems()
        if (subItems.isEmpty()) {
            setAnimating(false)
            return
        }

        var lastAnimation: Animator? = null

        subItems.forEachIndexed { i, item ->
            val view = item.view

            // Reverse translation: from current position back to center
            val reverseX = -(item.x - center.x + item.width / 2f)
            val reverseY = -(item.y - center.y + item.height / 2f)

            val animation = ObjectAnimator.ofPropertyValuesHolder(
                view,
                PropertyValuesHolder.ofFloat(View.TRANSLATION_X, reverseX),
                PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, reverseY),
                PropertyValuesHolder.ofFloat(View.ROTATION, -720f),
                PropertyValuesHolder.ofFloat(View.SCALE_X, 0f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 0f),
                PropertyValuesHolder.ofFloat(View.ALPHA, 0f)
            ).apply {
                duration = DURATION
                interpolator = INTERP_CLOSE
                startDelay = (subItems.size - i) * LAG_BETWEEN_ITEMS
                addListener(SubActionItemAnimationListener(item, ActionType.CLOSING))
            }

            if (i == 0) {
                lastAnimation = animation
            }

            animation.start()
        }

        lastAnimation?.addListener(LastAnimationListener())
    }

    fun isAnimating(): Boolean = animating

    private fun setAnimating(value: Boolean) {
        animating = value
    }

    fun setAnimationCompletionListener(listener: (() -> Unit)?) {
        animationCompletionListener = listener
    }

    private inner class SubActionItemAnimationListener(
        private val subActionItem: QuickBallFloatingMenu.Item,
        private val actionType: ActionType
    ) : AnimatorListenerAdapter() {

        override fun onAnimationEnd(animation: Animator) = handleEnd()
        override fun onAnimationCancel(animation: Animator) = handleEnd()

        private fun handleEnd() {
            val view = subActionItem.view
            val params = view.layoutParams as FrameLayout.LayoutParams
            val overlayParams = menu?.getOverlayContainer()?.layoutParams as? WindowManager.LayoutParams

            // Reset transformation properties
            view.translationX = 0f
            view.translationY = 0f
            view.rotation = 0f
            view.scaleX = 1f
            view.scaleY = 1f
            view.alpha = 1f

            when (actionType) {
                ActionType.OPENING -> {
                    // Set final position via margins
                    val offsetX = overlayParams?.x ?: 0
                    val offsetY = overlayParams?.y ?: 0
                    params.setMargins(
                        subActionItem.x - offsetX,
                        subActionItem.y - offsetY,
                        0, 0
                    )
                    view.layoutParams = params
                }

                ActionType.CLOSING -> {
                    val center = menu?.getActionViewCenter() ?: return
                    val offsetX = overlayParams?.x ?: 0
                    val offsetY = overlayParams?.y ?: 0

                    // Snap to center before removal
                    params.setMargins(
                        center.x - offsetX - subActionItem.width / 2,
                        center.y - offsetY - subActionItem.height / 2,
                        0, 0
                    )
                    view.layoutParams = params

                    // Remove from overlay
                    menu?.removeViewFromCurrentContainer(view)

                    // Detach overlay if empty
                    if (menu?.getOverlayContainer()?.childCount == 0) {
                        menu?.detachOverlayContainer()
                    }
                }
            }
        }
    }

    private inner class LastAnimationListener : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
            setAnimating(false)
            animationCompletionListener?.invoke()
        }

        override fun onAnimationCancel(animation: Animator) {
            setAnimating(false)
            animationCompletionListener?.invoke()
        }
    }
}