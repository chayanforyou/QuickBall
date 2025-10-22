package io.github.chayanforyou.quickball.domain.models

import androidx.annotation.DrawableRes
import io.github.chayanforyou.quickball.R
import io.github.chayanforyou.quickball.domain.handlers.MenuAction

data class MenuItemModel(
    val action: MenuAction,
    @DrawableRes val iconRes: Int,
    val title: String,
    val isSelected: Boolean = false
) {
    companion object {

        fun getAllMenuItems(): List<MenuItemModel> = listOf(
            // Navigation
            MenuItemModel(
                action = MenuAction.HOME,
                iconRes = R.drawable.ic_android,
                title = "Home",
            ),
            MenuItemModel(
                action = MenuAction.BACK,
                iconRes = R.drawable.ic_android,
                title = "Back",
            ),
            MenuItemModel(
                action = MenuAction.RECENT,
                iconRes = R.drawable.ic_android,
                title = "Recent Apps",
            ),

            // Display
            MenuItemModel(
                action = MenuAction.BRIGHTNESS_UP,
                iconRes = R.drawable.ic_brightness_up,
                title = "Brightness Up",
            ),
            MenuItemModel(
                action = MenuAction.BRIGHTNESS_DOWN,
                iconRes = R.drawable.ic_brightness_down,
                title = "Brightness Down",
            ),
            MenuItemModel(
                action = MenuAction.TORCH_TOGGLE,
                iconRes = R.drawable.ic_android,
                title = "Torch",
            ),

            // Volume & Sound
            MenuItemModel(
                action = MenuAction.VOLUME_UP,
                iconRes = R.drawable.ic_volume_up,
                title = "Volume Up",
            ),
            MenuItemModel(
                action = MenuAction.VOLUME_DOWN,
                iconRes = R.drawable.ic_volume_down,
                title = "Volume Down",
            ),
            MenuItemModel(
                action = MenuAction.SILENT_TOGGLE,
                iconRes = R.drawable.ic_android,
                title = "Silent Mode",
            ),

            // Connectivity
            MenuItemModel(
                action = MenuAction.WIFI_TOGGLE,
                iconRes = R.drawable.ic_android,
                title = "Wi-Fi",
            ),
            MenuItemModel(
                action = MenuAction.BLUETOOTH_TOGGLE,
                iconRes = R.drawable.ic_android,
                title = "Bluetooth",
            ),
            MenuItemModel(
                action = MenuAction.MOBILE_DATA_TOGGLE,
                iconRes = R.drawable.ic_android,
                title = "Mobile Data",
            ),

            // Utilities
            MenuItemModel(
                action = MenuAction.SCREENSHOT,
                iconRes = R.drawable.ic_screenshot,
                title = "Screenshot",
            ),
            MenuItemModel(
                action = MenuAction.LOCK_SCREEN,
                iconRes = R.drawable.ic_lock,
                title = "Lock Screen",
            )
        )
        
        fun getMenuItemByAction(action: MenuAction): MenuItemModel? {
            return getAllMenuItems().find { it.action == action }
        }
    }
}
