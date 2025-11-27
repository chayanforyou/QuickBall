package io.github.chayanforyou.quickball.domain.models

import android.graphics.drawable.Drawable

data class InstalledAppModel(
    val packageName: String,
    val appName: String,
    val icon: Drawable,
    var isSelected: Boolean = false
)
