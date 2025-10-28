package io.github.chayanforyou.quickball.domain.models

import androidx.annotation.DrawableRes
import io.github.chayanforyou.quickball.R
import io.github.chayanforyou.quickball.domain.handlers.MenuAction

data class QuickBallMenuItemModel(
    val action: MenuAction,
    @DrawableRes val iconRes: Int,
    val title: String,
    val isSelected: Boolean = false,
    val packageName: String? = null
) {
    companion object {

        fun getAllMenuItems(): List<QuickBallMenuItemModel> = listOf(
            // Navigation
            QuickBallMenuItemModel(
                action = MenuAction.HOME,
                iconRes = R.drawable.ic_home,
                title = "Home",
            ),
            QuickBallMenuItemModel(
                action = MenuAction.BACK,
                iconRes = R.drawable.ic_back,
                title = "Back",
            ),
            QuickBallMenuItemModel(
                action = MenuAction.RECENT,
                iconRes = R.drawable.ic_recent,
                title = "Recent Apps",
            ),

            // Display
            QuickBallMenuItemModel(
                action = MenuAction.BRIGHTNESS_UP,
                iconRes = R.drawable.ic_brightness_up,
                title = "Brightness Up",
            ),
            QuickBallMenuItemModel(
                action = MenuAction.BRIGHTNESS_DOWN,
                iconRes = R.drawable.ic_brightness_down,
                title = "Brightness Down",
            ),
            QuickBallMenuItemModel(
                action = MenuAction.TORCH_TOGGLE,
                iconRes = R.drawable.ic_torch,
                title = "Torch",
            ),

            // Volume & Sound
            QuickBallMenuItemModel(
                action = MenuAction.VOLUME_UP,
                iconRes = R.drawable.ic_volume_up,
                title = "Volume Up",
            ),
            QuickBallMenuItemModel(
                action = MenuAction.VOLUME_DOWN,
                iconRes = R.drawable.ic_volume_down,
                title = "Volume Down",
            ),
            QuickBallMenuItemModel(
                action = MenuAction.SILENT_TOGGLE,
                iconRes = R.drawable.ic_silent,
                title = "Silent",
            ),
            QuickBallMenuItemModel(
                action = MenuAction.VIBRATE_TOGGLE,
                iconRes = R.drawable.ic_vibrate,
                title = "Vibration",
            ),

            // Connectivity
            QuickBallMenuItemModel(
                action = MenuAction.WIFI_TOGGLE,
                iconRes = R.drawable.ic_wifi,
                title = "Wi-Fi",
            ),
            QuickBallMenuItemModel(
                action = MenuAction.BLUETOOTH_TOGGLE,
                iconRes = R.drawable.ic_bluetooth,
                title = "Bluetooth",
            ),
            QuickBallMenuItemModel(
                action = MenuAction.MOBILE_DATA_TOGGLE,
                iconRes = R.drawable.ic_mobile_data,
                title = "Mobile Data",
            ),

            // Utilities
            QuickBallMenuItemModel(
                action = MenuAction.SCREENSHOT,
                iconRes = R.drawable.ic_screenshot,
                title = "Screenshot",
            ),
            QuickBallMenuItemModel(
                action = MenuAction.LOCK_SCREEN,
                iconRes = R.drawable.ic_lock,
                title = "Lock Screen",
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
                title = appName,
                packageName = packageName
            )
        }
    }
}
