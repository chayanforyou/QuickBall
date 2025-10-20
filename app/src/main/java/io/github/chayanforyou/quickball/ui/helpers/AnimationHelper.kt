package io.github.chayanforyou.quickball.ui.helpers

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.graphics.Point
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import io.github.chayanforyou.quickball.ui.floating.FloatingActionMenu

class AnimationHelper {

    companion object {
        private const val DURATION = 300L
        private const val LAG_BETWEEN_ITEMS = 5
    }

    private enum class ActionType { OPENING, CLOSING }

    private var animating = false
    private var menu: FloatingActionMenu? = null

    fun setMenu(menu: FloatingActionMenu) {
        this.menu = menu
    }

    fun animateMenuOpening(center: Point) {
        val currentMenu = menu
            ?: throw NullPointerException("Can't animate without a valid FloatingActionMenu.")

        setAnimating(true)

        var lastAnimation: Animator? = null
        for (i in currentMenu.getSubActionItems().indices) {
            val item = currentMenu.getSubActionItems()[i]

            // Enable hardware acceleration for smooth animations
            item.view.setLayerType(View.LAYER_TYPE_HARDWARE, null)

            item.view.scaleX = 0f
            item.view.scaleY = 0f
            item.view.alpha = 0f

            // For individual menu items, we animate from center to final position
            val startX = center.x - item.width / 2
            val startY = center.y - item.height / 2
            val endX = item.x
            val endY = item.y

            // Create position animator with throttled updates
            val positionAnimator = ValueAnimator.ofFloat(0f, 1f)
            positionAnimator.duration = DURATION
            positionAnimator.interpolator = OvershootInterpolator(0.9f)

            // Use throttled updates to prevent frame drops
            positionAnimator.addUpdateListener { anim ->
                val progress = anim.animatedValue as Float
                val currentX = (startX + (endX - startX) * progress).toInt()
                val currentY = (startY + (endY - startY) * progress).toInt()
                menu?.updateIndividualMenuItemPosition(item, currentX, currentY)
            }

            // Create visual effects animator
            val pvhR = PropertyValuesHolder.ofFloat(View.ROTATION, 720f)
            val pvhsX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f)
            val pvhsY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f)
            val pvhA = PropertyValuesHolder.ofFloat(View.ALPHA, 1f)

            val visualAnimation = ObjectAnimator.ofPropertyValuesHolder(item.view, pvhR, pvhsX, pvhsY, pvhA)
            visualAnimation.duration = DURATION
            visualAnimation.interpolator = OvershootInterpolator(0.9f)

            // Combine animations
            val animation = AnimatorSet().apply {
                playTogether(positionAnimator, visualAnimation)
                addListener(SubActionItemAnimationListener(item, ActionType.OPENING))
            }

            if (i == 0) {
                lastAnimation = animation
            }

            // Put a slight lag between each of the menu items to make it asymmetric
            animation.startDelay = ((currentMenu.getSubActionItems().size - i) * LAG_BETWEEN_ITEMS).toLong()
            animation.start()
        }

        lastAnimation?.addListener(LastAnimationListener())
    }

    fun animateMenuClosing(center: Point) {
        val currentMenu = menu
            ?: throw NullPointerException("Can't animate without a valid FloatingActionMenu.")

        setAnimating(true)

        var lastAnimation: Animator? = null
        for (i in currentMenu.getSubActionItems().indices) {
            val item = currentMenu.getSubActionItems()[i]

            // For individual menu items, we animate from current position to center
            val startX = item.x
            val startY = item.y
            val endX = center.x - item.width / 2
            val endY = center.y - item.height / 2

            // Create position animator with throttled updates
            val positionAnimator = ValueAnimator.ofFloat(0f, 1f)
            positionAnimator.duration = DURATION
            positionAnimator.interpolator = AccelerateDecelerateInterpolator()

            // Use throttled updates to prevent frame drops
            positionAnimator.addUpdateListener { anim ->
                val progress = anim.animatedValue as Float
                val currentX = (startX + (endX - startX) * progress).toInt()
                val currentY = (startY + (endY - startY) * progress).toInt()
                menu?.updateIndividualMenuItemPosition(item, currentX, currentY)
            }

            // Create visual effects animator
            val pvhR = PropertyValuesHolder.ofFloat(View.ROTATION, -720f)
            val pvhsX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0f)
            val pvhsY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0f)
            val pvhA = PropertyValuesHolder.ofFloat(View.ALPHA, 0f)

            val visualAnimation = ObjectAnimator.ofPropertyValuesHolder(item.view, pvhR, pvhsX, pvhsY, pvhA)
            visualAnimation.duration = DURATION
            visualAnimation.interpolator = AccelerateDecelerateInterpolator()

            // Combine animations
            val animation = AnimatorSet().apply {
                playTogether(positionAnimator, visualAnimation)
                addListener(SubActionItemAnimationListener(item, ActionType.CLOSING))
            }

            if (i == 0) {
                lastAnimation = animation
            }

            animation.startDelay = ((currentMenu.getSubActionItems().size - i) * LAG_BETWEEN_ITEMS).toLong()
            animation.start()
        }

        lastAnimation?.addListener(LastAnimationListener())
    }

    fun isAnimating(): Boolean = animating

    fun setAnimating(animating: Boolean) {
        this.animating = animating
    }

    fun setAnimationCompletionListener(listener: (() -> Unit)?) {
        this.animationCompletionListener = listener
    }

    private var animationCompletionListener: (() -> Unit)? = null

    private inner class SubActionItemAnimationListener(
        private val subActionItem: FloatingActionMenu.Item,
        private val actionType: ActionType
    ) : Animator.AnimatorListener {

        override fun onAnimationStart(animation: Animator) {}

        override fun onAnimationEnd(animation: Animator) {
            restoreSubActionViewAfterAnimation(subActionItem, actionType)
        }

        override fun onAnimationCancel(animation: Animator) {
            restoreSubActionViewAfterAnimation(subActionItem, actionType)
        }

        override fun onAnimationRepeat(animation: Animator) {}

        private fun restoreSubActionViewAfterAnimation(
            subActionItem: FloatingActionMenu.Item,
            actionType: ActionType
        ) {
            when (actionType) {
                ActionType.OPENING -> {
                    // For individual menu items, update position to final location
                    subActionItem.view.translationX = 0f
                    subActionItem.view.translationY = 0f
                    subActionItem.view.rotation = 0f
                    subActionItem.view.scaleX = 1f
                    subActionItem.view.scaleY = 1f
                    subActionItem.view.alpha = 1f

                    menu?.updateIndividualMenuItemPosition(subActionItem, subActionItem.x, subActionItem.y)
                }

                ActionType.CLOSING -> {
                    // For individual menu items, remove from window manager
                    subActionItem.view.alpha = 0f
                    subActionItem.view.scaleX = 0f
                    subActionItem.view.scaleY = 0f

                    subActionItem.view.post {
                        menu?.removeIndividualMenuItem(subActionItem)
                    }
                }
            }
        }
    }

    private inner class LastAnimationListener : Animator.AnimatorListener {

        override fun onAnimationStart(animation: Animator) {
            setAnimating(true)
        }

        override fun onAnimationEnd(animation: Animator) {
            setAnimating(false)
            animationCompletionListener?.invoke()
        }

        override fun onAnimationCancel(animation: Animator) {
            setAnimating(false)
            animationCompletionListener?.invoke()
        }

        override fun onAnimationRepeat(animation: Animator) {
            setAnimating(true)
        }
    }
}