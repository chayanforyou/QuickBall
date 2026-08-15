package io.github.chayanforyou.quickball.domain.models

import androidx.annotation.Keep
import androidx.annotation.StringRes
import com.google.gson.annotations.SerializedName
import io.github.chayanforyou.quickball.R

@Keep
enum class PillGesture(@field:StringRes val titleRes: Int) {
    @SerializedName("DOUBLE_TAP")
    DOUBLE_TAP(R.string.double_tap_title),
    @SerializedName("TRIPLE_TAP")
    TRIPLE_TAP(R.string.triple_tap_title),
    @SerializedName("LONG_PRESS")
    LONG_PRESS(R.string.long_press_title),
    @SerializedName("SWIPE_UP")
    SWIPE_UP(R.string.swipe_up_title),
    @SerializedName("SWIPE_DOWN")
    SWIPE_DOWN(R.string.swipe_down_title)
}