package io.github.chayanforyou.quickball.domain.models

import androidx.annotation.Keep
import androidx.annotation.StringRes
import com.google.gson.annotations.SerializedName
import io.github.chayanforyou.quickball.R

@Keep
enum class HapticIntensity(@field:StringRes val titleRes: Int) {
    @SerializedName("LIGHT")
    LIGHT(R.string.haptic_light),
    @SerializedName("MEDIUM")
    MEDIUM(R.string.haptic_medium),
    @SerializedName("STRONG")
    STRONG(R.string.haptic_strong)
}