package io.github.chayanforyou.quickball.helpers

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.graphics.Point
import android.view.View
import android.view.animation.PathInterpolator
import io.github.chayanforyou.quickball.ui.floating.QuickBallFloatingMenu

object AnimationHelper {

    private const val DURATION_OPEN = 250L
    private const val DURATION_CLOSE = 300L
    private const val DELAY_PER_ITEM_OPEN = 5L
    private const val DELAY_PER_ITEM_CLOSE = 5L

    private val INTERP_OPEN = PathInterpolator(0.5f, 0.0f, 0.3f, 0.9f)
    private val INTERP_CLOSE = PathInterpolator(0.4f, 0.0f, 0.3f, 1.0f)

//    private val INTERP_OPEN = SpringInterpolator(0.6f, 0.57f)
//    private val INTERP_CLOSE = SpringInterpolator(0.99f, 0.3f)

    private enum class ActionType { OPENING, CLOSING }

    private var animating = false
    private var animationCompletionListener: (() -> Unit)? = null

    // ------------------------------------------------------------
    // OPENING
    // ------------------------------------------------------------
    fun animateMenuOpening(menu: QuickBallFloatingMenu, center: Point) {
        val subItems = menu.getSubActionItems()
        if (subItems.isEmpty()) {
            animationCompletionListener?.invoke()
            return
        }

        setAnimating(true)
        val animators = subItems.mapIndexed { index, item ->
            val view = item.view
            val itemWidth = item.width
            val itemHeight = item.height

            val centerX = center.x - itemWidth / 2
            val centerY = center.y - itemHeight / 2

            // ---- initial state (scale = 0, alpha = 0) ----
            view.apply {
                scaleX = 0f
                scaleY = 0f
                alpha = 0f
                setLayerType(View.LAYER_TYPE_HARDWARE, null)
            }

            val finalX = item.x
            val finalY = item.y

            ValueAnimator.ofFloat(0f, 1f).apply {
                duration = DURATION_OPEN
                startDelay = index * DELAY_PER_ITEM_OPEN
                interpolator = INTERP_OPEN

                addUpdateListener { anim ->
                    val p = anim.animatedValue as Float
                    val x = (centerX + (finalX - centerX) * p).toInt()
                    val y = (centerY + (finalY - centerY) * p).toInt()
                    menu.updateIndividualMenuItemPosition(item, x, y)

                    view.scaleX = p          // 0 to 1
                    view.scaleY = p
                    view.alpha = p
                }

                addListener(SubActionItemAnimationListener(menu, item, ActionType.OPENING))
            }
        }

        startAnimatorSet(animators)
    }

    // ------------------------------------------------------------
    // CLOSING
    // ------------------------------------------------------------
    fun animateMenuClosing(menu: QuickBallFloatingMenu, center: Point) {
        val subItems = menu.getSubActionItems()
        if (subItems.isEmpty()) {
            animationCompletionListener?.invoke()
            return
        }

        setAnimating(true)
        val animators = subItems.mapIndexed { index, item ->
            val view = item.view
            val itemWidth = item.width
            val itemHeight = item.height

            val startX = item.x
            val startY = item.y
            val endX = center.x - itemWidth / 2
            val endY = center.y - itemHeight / 2

            view.setLayerType(View.LAYER_TYPE_HARDWARE, null)

            ValueAnimator.ofFloat(0f, 1f).apply {
                duration = DURATION_CLOSE
                startDelay = index * DELAY_PER_ITEM_CLOSE
                interpolator = INTERP_CLOSE

                addUpdateListener { anim ->
                    val p = anim.animatedValue as Float
                    val x = (startX + (endX - startX) * p).toInt()
                    val y = (startY + (endY - startY) * p).toInt()
                    menu.updateIndividualMenuItemPosition(item, x, y)

                    view.scaleX = 1f - p   // 1 to 0
                    view.scaleY = 1f - p
                    view.alpha = 1f - p
                }

                addListener(SubActionItemAnimationListener(menu, item, ActionType.CLOSING))
            }
        }

        startAnimatorSet(animators)
    }

    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------
    private fun startAnimatorSet(animators: List<ValueAnimator>) {
        AnimatorSet().apply {
            playTogether(animators)
            addListener(LastAnimationListener())
            start()
        }
    }

    fun isAnimating(): Boolean = animating

    private fun setAnimating(value: Boolean) { animating = value }

    fun setAnimationCompletionListener(listener: (() -> Unit)?) {
        animationCompletionListener = listener
    }

    private class SubActionItemAnimationListener(
        private val menu: QuickBallFloatingMenu,
        private val item: QuickBallFloatingMenu.Item,
        private val type: ActionType
    ) : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) = cleanup()
        override fun onAnimationCancel(animation: Animator) = cleanup()

        private fun cleanup() {
            when (type) {
                ActionType.OPENING -> {
                    item.view.apply {
                        scaleX = 1f; scaleY = 1f; alpha = 1f
                        setLayerType(View.LAYER_TYPE_NONE, null)
                    }
                }
                ActionType.CLOSING -> {
                    item.view.apply {
                        alpha = 0f; scaleX = 0f; scaleY = 0f
                        setLayerType(View.LAYER_TYPE_NONE, null)
                        post { menu.removeIndividualMenuItem(item) }
                    }
                }
            }
        }
    }

    private class LastAnimationListener : AnimatorListenerAdapter() {
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