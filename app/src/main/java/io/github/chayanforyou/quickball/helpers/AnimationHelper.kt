package io.github.chayanforyou.quickball.helpers

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.graphics.Point
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import io.github.chayanforyou.quickball.ui.floating.QuickBallFloatingMenu

class AnimationHelper {

    companion object {
        private const val DURATION = 300L
        private const val LAG_BETWEEN_ITEMS = 5L
        private val INTERP_OVERSHOOT = OvershootInterpolator(0.9f)
        private val INTERP_ACCEL = AccelerateDecelerateInterpolator()
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

        val animations = ArrayList<Animator>(subItems.size)
        subItems.forEachIndexed { i, item ->
            item.view.apply {
                setLayerType(View.LAYER_TYPE_HARDWARE, null)
                scaleX = 0f
                scaleY = 0f
                alpha = 0f
            }

            val startX = center.x - item.width / 2
            val startY = center.y - item.height / 2
            val endX = item.x
            val endY = item.y

            val positionAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = DURATION
                interpolator = INTERP_OVERSHOOT
                addUpdateListener {
                    val p = it.animatedValue as Float
                    currentMenu.updateIndividualMenuItemPosition(
                        item,
                        (startX + (endX - startX) * p).toInt(),
                        (startY + (endY - startY) * p).toInt()
                    )
                }
            }

            val visualAnimator = ObjectAnimator.ofPropertyValuesHolder(
                item.view,
                PropertyValuesHolder.ofFloat(View.ROTATION, 720f),
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f),
                PropertyValuesHolder.ofFloat(View.ALPHA, 1f)
            ).apply {
                duration = DURATION
                interpolator = INTERP_OVERSHOOT
            }

            AnimatorSet().apply {
                playTogether(positionAnimator, visualAnimator)
                startDelay = (subItems.size - i) * LAG_BETWEEN_ITEMS
                addListener(SubActionItemAnimationListener(item, ActionType.OPENING))
                animations += this
            }
        }

        AnimatorSet().apply {
            playTogether(animations)
            addListener(LastAnimationListener())
            start()
        }
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

        val animations = ArrayList<Animator>(subItems.size)
        subItems.forEachIndexed { i, item ->
            val startX = item.x
            val startY = item.y
            val endX = center.x - item.width / 2
            val endY = center.y - item.height / 2

            val positionAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = DURATION
                interpolator = INTERP_ACCEL
                addUpdateListener {
                    val p = it.animatedValue as Float
                    currentMenu.updateIndividualMenuItemPosition(
                        item,
                        (startX + (endX - startX) * p).toInt(),
                        (startY + (endY - startY) * p).toInt()
                    )
                }
            }

            val visualAnimator = ObjectAnimator.ofPropertyValuesHolder(
                item.view,
                PropertyValuesHolder.ofFloat(View.ROTATION, -720f),
                PropertyValuesHolder.ofFloat(View.SCALE_X, 0f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 0f),
                PropertyValuesHolder.ofFloat(View.ALPHA, 0f)
            ).apply {
                duration = DURATION
                interpolator = INTERP_ACCEL
            }

            AnimatorSet().apply {
                playTogether(positionAnimator, visualAnimator)
                startDelay = (subItems.size - i) * LAG_BETWEEN_ITEMS
                addListener(SubActionItemAnimationListener(item, ActionType.CLOSING))
                animations += this
            }
        }

        AnimatorSet().apply {
            playTogether(animations)
            addListener(LastAnimationListener())
            start()
        }
    }

    fun isAnimating(): Boolean {
        return animating
    }

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
            when (actionType) {
                ActionType.OPENING -> {
                    subActionItem.view.apply {
                        rotation = 0f
                        scaleX = 1f
                        scaleY = 1f
                        alpha = 1f
                        setLayerType(View.LAYER_TYPE_NONE, null)
                    }
                }

                ActionType.CLOSING -> {
                    subActionItem.view.apply {
                        alpha = 0f
                        scaleX = 0f
                        scaleY = 0f
                        setLayerType(View.LAYER_TYPE_NONE, null)
                        post { menu?.removeIndividualMenuItem(subActionItem) }
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