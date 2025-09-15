package io.github.chayanforyou.quickball.utils

import android.content.res.Resources
import android.view.View
import android.view.ViewGroup

object WidgetUtil {
    fun dp2px(dpValue: Float): Int {
        var scale = Resources.getSystem().displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    fun height(v: View, height: Int) {
        val params = v.layoutParams ?: ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.height = height
        v.layoutParams = params
    }

    fun widthAndHeight(v: View, width: Int, height: Int) {
        val params = v.layoutParams ?: ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.width = width
        params.height = height
        v.layoutParams = params
    }
}