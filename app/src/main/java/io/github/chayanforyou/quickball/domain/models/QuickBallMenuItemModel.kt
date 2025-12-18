package io.github.chayanforyou.quickball.domain.models

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.github.chayanforyou.quickball.R
import io.github.chayanforyou.quickball.domain.handlers.MenuAction

data class QuickBallMenuItemModel(
    val action: MenuAction,
    @DrawableRes val iconRes: Int,
    @StringRes val titleRes: Int,
    val isSelected: Boolean = false,
    val packageName: String? = null,
    val appTitle: String? = null
) {

    companion object {
        fun getAllMenuItems(): List<QuickBallMenuItemModel> = listOf(
            // Navigation
            QuickBallMenuItemModel(
                action = MenuAction.HOME,
                iconRes = R.drawable.ic_home,
                titleRes = R.string.menu_home,
            ),
            QuickBallMenuItemModel(
                action = MenuAction.BACK,
                iconRes = R.drawable.ic_back,
                titleRes = R.string.menu_back,
            ),
            QuickBallMenuItemModel(
                action = MenuAction.RECENT,
                iconRes = R.drawable.ic_recent,
                titleRes = R.string.menu_recent,
            ),

            // Display
            QuickBallMenuItemModel(
                action = MenuAction.BRIGHTNESS_UP,
                iconRes = R.drawable.ic_brightness_up,
                titleRes = R.string.menu_brightness_up,
            ),
            QuickBallMenuItemModel(
                action = MenuAction.BRIGHTNESS_DOWN,
                iconRes = R.drawable.ic_brightness_down,
                titleRes = R.string.menu_brightness_down,
            ),
            QuickBallMenuItemModel(
                action = MenuAction.TORCH_TOGGLE,
                iconRes = R.drawable.ic_torch,
                titleRes = R.string.menu_torch,
            ),

            // Volume & Sound
            QuickBallMenuItemModel(
                action = MenuAction.VOLUME_UP,
                iconRes = R.drawable.ic_volume_up,
                titleRes = R.string.menu_volume_up,
            ),
            QuickBallMenuItemModel(
                action = MenuAction.VOLUME_DOWN,
                iconRes = R.drawable.ic_volume_down,
                titleRes = R.string.menu_volume_down,
            ),
            QuickBallMenuItemModel(
                action = MenuAction.SILENT_TOGGLE,
                iconRes = R.drawable.ic_silent,
                titleRes = R.string.menu_silent,
            ),
            QuickBallMenuItemModel(
                action = MenuAction.VIBRATE_TOGGLE,
                iconRes = R.drawable.ic_vibrate,
                titleRes = R.string.menu_vibration,
            ),

            // Connectivity
            QuickBallMenuItemModel(
                action = MenuAction.WIFI_TOGGLE,
                iconRes = R.drawable.ic_wifi,
                titleRes = R.string.menu_wifi,
            ),
            QuickBallMenuItemModel(
                action = MenuAction.BLUETOOTH_TOGGLE,
                iconRes = R.drawable.ic_bluetooth,
                titleRes = R.string.menu_bluetooth,
            ),
            QuickBallMenuItemModel(
                action = MenuAction.MOBILE_DATA_TOGGLE,
                iconRes = R.drawable.ic_mobile_data,
                titleRes = R.string.menu_mobile_data,
            ),

            // Utilities
            QuickBallMenuItemModel(
                action = MenuAction.SCREENSHOT,
                iconRes = R.drawable.ic_screenshot,
                titleRes = R.string.menu_screenshot,
            ),
            QuickBallMenuItemModel(
                action = MenuAction.LOCK_SCREEN,
                iconRes = R.drawable.ic_lock,
                titleRes = R.string.menu_lock_screen,
            )
        )

        fun getMenuItemByAction(action: MenuAction): QuickBallMenuItemModel? {
            return getAllMenuItems().find { it.action == action }
        }

        fun createAppMenuItem(
            appName: String,
            packageName: String,
            @DrawableRes iconRes: Int,
        ): QuickBallMenuItemModel {
            return QuickBallMenuItemModel(
                action = MenuAction.LAUNCH_APP,
                iconRes = iconRes,
                titleRes = 0,
                packageName = packageName,
                appTitle = appName
            )
        }
    }

    fun getTitle(context: Context): String {
        return appTitle ?: if (titleRes != 0) context.getString(titleRes) else ""
    }
}
