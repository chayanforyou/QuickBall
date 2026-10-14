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
    ARC(R.string.connector_style_arc),
    @SerializedName("LINE_RING")
    LINE_RING(R.string.connector_style_line_ring),
    @SerializedName("DOT_RING")
    DOT_RING(R.string.connector_style_dot_ring),
    @SerializedName("CURVE")
    CURVE(R.string.connector_style_curve),
    @SerializedName("FADE")
    FADE(R.string.connector_style_fade),
    @SerializedName("DASH")
    DASH(R.string.connector_style_dash),
    @SerializedName("TAPER")
    TAPER(R.string.connector_style_taper),
    @SerializedName("WAVE")
    WAVE(R.string.connector_style_wave),
    @SerializedName("BUBBLE")
    BUBBLE(R.string.connector_style_bubble),
    @SerializedName("ZIGZAG")
    ZIGZAG(R.string.connector_style_zigzag),
    @SerializedName("DOUBLE")
    DOUBLE(R.string.connector_style_double),
    @SerializedName("GLOW")
    GLOW(R.string.connector_style_glow)
}
