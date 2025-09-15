package io.github.chayanforyou.quickball.animation

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.graphics.Point
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import io.github.chayanforyou.quickball.FloatingActionMenu

class AnimationHandler {

    companion object {
        private const val DURATION = 200
        private const val LAG_BETWEEN_ITEMS = 20
    }

    private enum class ActionType { OPENING, CLOSING }

    private var animating = false
    private var menu: FloatingActionMenu? = null

    fun setMenu(menu: FloatingActionMenu) {
        this.menu = menu
    }

    fun animateMenuOpening(center: Point) {
        val currentMenu = menu
            ?: throw NullPointerException("AnimationHandler cannot animate without a valid FloatingActionMenu.")

        setAnimating(true)

        var lastAnimation: Animator? = null
        for (i in currentMenu.getSubActionItems().indices) {
            val item = currentMenu.getSubActionItems()[i]

            item.view.scaleX = 0f
            item.view.scaleY = 0f
            item.view.alpha = 0f

            val pvhX = PropertyValuesHolder.ofFloat(View.TRANSLATION_X, item.x - center.x + item.width / 2f)
            val pvhY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, item.y - center.y + item.height / 2f)
            val pvhR = PropertyValuesHolder.ofFloat(View.ROTATION, 720f)
            val pvhsX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f)
            val pvhsY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f)
            val pvhA = PropertyValuesHolder.ofFloat(View.ALPHA, 1f)

            val animation = ObjectAnimator.ofPropertyValuesHolder(item.view, pvhX, pvhY, pvhR, pvhsX, pvhsY, pvhA)
            animation.duration = DURATION.toLong()
            animation.interpolator = OvershootInterpolator(0.9f)
            animation.addListener(SubActionItemAnimationListener(item, ActionType.OPENING))

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
            ?: throw NullPointerException("AnimationHandler cannot animate without a valid FloatingActionMenu.")

        setAnimating(true)

        var lastAnimation: Animator? = null
        for (i in currentMenu.getSubActionItems().indices) {
            val item = currentMenu.getSubActionItems()[i]

            val pvhX = PropertyValuesHolder.ofFloat(View.TRANSLATION_X, -(item.x - center.x + item.width / 2f))
            val pvhY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, -(item.y - center.y + item.height / 2f))
            val pvhR = PropertyValuesHolder.ofFloat(View.ROTATION, -720f)
            val pvhsX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0f)
            val pvhsY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0f)
            val pvhA = PropertyValuesHolder.ofFloat(View.ALPHA, 0f)

            val animation = ObjectAnimator.ofPropertyValuesHolder(item.view, pvhX, pvhY, pvhR, pvhsX, pvhsY, pvhA)
            animation.duration = DURATION.toLong()
            animation.interpolator = AccelerateDecelerateInterpolator()
            animation.addListener(SubActionItemAnimationListener(item, ActionType.CLOSING))

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
            val params = subActionItem.view.layoutParams
            subActionItem.view.translationX = 0f
            subActionItem.view.translationY = 0f
            subActionItem.view.rotation = 0f
            subActionItem.view.scaleX = 1f
            subActionItem.view.scaleY = 1f
            subActionItem.view.alpha = 1f

            when (actionType) {
                ActionType.OPENING -> {
                    val lp = params as FrameLayout.LayoutParams
                    val overlayParams = menu?.getOverlayContainer()?.layoutParams as? WindowManager.LayoutParams
                    lp.setMargins(
                        subActionItem.x - (overlayParams?.x ?: 0),
                        subActionItem.y - (overlayParams?.y ?: 0),
                        0, 0
                    )
                    subActionItem.view.layoutParams = lp
                }

                ActionType.CLOSING -> {
                    val center = menu?.getActionViewCenter()
                    val lp = params as FrameLayout.LayoutParams
                    val overlayParams = menu?.getOverlayContainer()?.layoutParams as? WindowManager.LayoutParams

                    if (center != null) {
                        lp.setMargins(
                            center.x - (overlayParams?.x ?: 0) - subActionItem.width / 2,
                            center.y - (overlayParams?.y ?: 0) - subActionItem.height / 2,
                            0, 0
                        )
                    }
                    subActionItem.view.layoutParams = lp
                    menu?.removeViewFromCurrentContainer(subActionItem.view)

                    // When all the views are removed from the overlay container, detach it
                    if (menu?.getOverlayContainer()?.childCount == 0) {
                        menu?.detachOverlayContainer()
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
        }

        override fun onAnimationCancel(animation: Animator) {
            setAnimating(false)
        }

        override fun onAnimationRepeat(animation: Animator) {
            setAnimating(true)
        }
    }
}
