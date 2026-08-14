package io.github.chayanforyou.quickball.domain.models

import androidx.annotation.StringRes
import io.github.chayanforyou.quickball.R

enum class HapticIntensity(@field:StringRes val titleRes: Int) {
    LIGHT(R.string.haptic_light),
    MEDIUM(R.string.haptic_medium),
    STRONG(R.string.haptic_strong)
}
