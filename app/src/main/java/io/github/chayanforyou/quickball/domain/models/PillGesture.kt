package io.github.chayanforyou.quickball.domain.models

import androidx.annotation.StringRes
import io.github.chayanforyou.quickball.R

enum class PillGesture(@field:StringRes val titleRes: Int) {
    DOUBLE_TAP(R.string.double_tap_title),
    TRIPLE_TAP(R.string.triple_tap_title),
    LONG_PRESS(R.string.long_press_title),
    SWIPE_UP(R.string.swipe_up_title),
    SWIPE_DOWN(R.string.swipe_down_title)
}
