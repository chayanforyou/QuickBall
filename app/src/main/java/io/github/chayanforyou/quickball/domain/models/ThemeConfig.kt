package io.github.chayanforyou.quickball.domain.models

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParseException

data class ThemeConfig(
    val format: String? = FORMAT,
    val version: Int? = VERSION,
    val ball: Ball? = null,
    val menu: Menu? = null,
    val connector: Connector? = null,
    val pill: Pill? = null,
    val toast: Toast? = null
) {

    data class Border(
        val enabled: Boolean? = null,
        val color: String? = null,
        val width: Float? = null
    )

    data class Ball(
        val color: String? = null,
        val iconColor: String? = null,
        val size: Float? = null,
        val border: Border? = null
    )

    data class Menu(
        val color: String? = null,
        val iconColor: String? = null,
        val size: Float? = null,
        val iconSize: Float? = null,
        val radius: Float? = null,
        val border: Border? = null
    )

    data class Connector(
        val style: String? = null,
        val color: String? = null,
        val width: Float? = null
    )

    data class Pill(
        val color: String? = null,
        val height: Float? = null,
        val thickness: Float? = null,
        val touchWidth: Float? = null
    )

    data class Toast(
        val backgroundColor: String? = null,
        val textColor: String? = null
    )

    fun toJson(): String {
        val root = JsonObject().apply {
            addProperty("format", format)
            addProperty("version", version)
            ball?.let { ball ->
                add("ball", JsonObject().apply {
                    addProperty("color", ball.color)
                    addProperty("iconColor", ball.iconColor)
                    addProperty("size", ball.size)
                    ball.border?.let { add("border", it.toJsonObject()) }
                })
            }
            menu?.let { menu ->
                add("menu", JsonObject().apply {
                    addProperty("color", menu.color)
                    addProperty("iconColor", menu.iconColor)
                    addProperty("size", menu.size)
                    addProperty("iconSize", menu.iconSize)
                    addProperty("radius", menu.radius)
                    menu.border?.let { add("border", it.toJsonObject()) }
                })
            }
            connector?.let { connector ->
                add("connector", JsonObject().apply {
                    addProperty("style", connector.style)
                    addProperty("color", connector.color)
                    addProperty("width", connector.width)
                })
            }
            pill?.let { pill ->
                add("pill", JsonObject().apply {
                    addProperty("color", pill.color)
                    addProperty("height", pill.height)
                    addProperty("thickness", pill.thickness)
                    addProperty("touchWidth", pill.touchWidth)
                })
            }
            toast?.let { toast ->
                add("toast", JsonObject().apply {
                    addProperty("backgroundColor", toast.backgroundColor)
                    addProperty("textColor", toast.textColor)
                })
            }
        }
        return gson.toJson(root)
    }

    private fun Border.toJsonObject() = JsonObject().apply {
        addProperty("enabled", enabled)
        addProperty("color", color)
        addProperty("width", width)
    }

    companion object {
        const val FORMAT = "quickball-theme"
        const val VERSION = 1

        private val gson = GsonBuilder().setPrettyPrinting().create()

        fun fromJson(json: String): ThemeConfig? {
            val config = try {
                gson.fromJson(json, ThemeConfig::class.java)
            } catch (_: JsonParseException) {
                null
            }
            return config?.takeIf { it.format == FORMAT }
        }

        fun Int.toHex(): String = "#%08X".format(this)

        fun parseColor(hex: String?): Int? {
            val digits = hex?.trim()?.removePrefix("#") ?: return null
            val value = digits.toLongOrNull(16) ?: return null
            return when (digits.length) {
                8 -> value.toInt()
                6 -> (0xFF000000 or value).toInt()
                else -> null
            }
        }
    }
}
