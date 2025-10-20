package io.github.chayanforyou.quickball.domain.models

import androidx.annotation.DrawableRes
import io.github.chayanforyou.quickball.R
import io.github.chayanforyou.quickball.domain.handlers.MenuAction

data class MenuItemModel(
    val action: MenuAction,
    @DrawableRes val iconRes: Int,
    val title: String
) {
    companion object {

        fun getAllMenuItems(): List<MenuItemModel> = listOf(
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
                action = MenuAction.LOCK_SCREEN,
                iconRes = R.drawable.ic_lock,
                title = "Lock Screen",
            ),
            MenuItemModel(
                action = MenuAction.TAKE_SCREENSHOT,
                iconRes = R.drawable.ic_screenshot,
                title = "Screenshot",
            )
        )
        
        fun getMenuItemByAction(action: MenuAction): MenuItemModel? {
            return getAllMenuItems().find { it.action == action }
        }
    }
}
