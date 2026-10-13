package io.github.chayanforyou.quickball.domain.models

import androidx.annotation.Keep
import androidx.annotation.StringRes
import com.google.gson.annotations.SerializedName
import io.github.chayanforyou.quickball.R

@Keep
enum class MenuConnectorStyle(@field:StringRes val titleRes: Int) {
    @SerializedName("NONE")
    NONE(R.string.connector_style_none),
    @SerializedName("LINE")
    LINE(R.string.connector_style_line),
    @SerializedName("DOTTED")
    DOTTED(R.string.connector_style_dotted),
    @SerializedName("ARC")
    ARC(R.string.connector_style_arc)
}
