package io.github.chayanforyou.quickball.domain.models

import android.graphics.drawable.Drawable

data class AppModel(
    val packageName: String,
    val appName: String,
    val icon: Drawable,
    val isSelected: Boolean = false
)
