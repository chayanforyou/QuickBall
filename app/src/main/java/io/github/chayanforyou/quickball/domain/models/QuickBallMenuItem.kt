package io.github.chayanforyou.quickball.domain.models

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import androidx.annotation.StringRes
import com.google.gson.annotations.SerializedName

@Keep
data class QuickBallMenuItem(
    @SerializedName("action")
    val action: MenuAction = MenuAction.LAUNCH_APP,
    @SerializedName("isSelected")
    val isSelected: Boolean = false,
    @SerializedName("packageName")
    val packageName: String? = null,
    @SerializedName("appTitle")
    val appTitle: String? = null
) {
    val iconRes: Int
        @DrawableRes get() = action.iconRes

    val titleRes: Int
        @StringRes get() = action.titleRes

    companion object {
        fun getAllMenuItems(): List<QuickBallMenuItem> {
            return MenuAction.entries
                .filter { it != MenuAction.LAUNCH_APP }
                .map { QuickBallMenuItem(action = it) }
        }

        fun getMenuItemByAction(action: MenuAction): QuickBallMenuItem? {
            if (action == MenuAction.LAUNCH_APP) return null
            return QuickBallMenuItem(action = action)
        }

        fun createAppMenuItem(
            appName: String,
            packageName: String
        ): QuickBallMenuItem {
            return QuickBallMenuItem(
                action = MenuAction.LAUNCH_APP,
                packageName = packageName,
                appTitle = appName
            )
        }
    }

    fun getTitle(context: Context): String {
        return appTitle ?: if (titleRes != 0) {
            try {
                context.getString(titleRes)
            } catch (_: Exception) {
                ""
            }
        } else ""
    }
}

