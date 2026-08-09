package io.github.chayanforyou.quickball.domain.models

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.github.chayanforyou.quickball.R

enum class MenuAction(
    @field:DrawableRes val iconRes: Int = 0,
    @field:StringRes val titleRes: Int = 0
) {
    // Navigation
    HOME(R.drawable.ic_home, R.string.menu_home),
    BACK(R.drawable.ic_back, R.string.menu_back),
    RECENT(R.drawable.ic_recent, R.string.menu_recent),

    // Media & Sound
    VOLUME_UP(R.drawable.ic_volume_up, R.string.menu_volume_up),
    VOLUME_DOWN(R.drawable.ic_volume_down, R.string.menu_volume_down),
    SILENT_TOGGLE(R.drawable.ic_silent, R.string.menu_silent),
    VIBRATE_TOGGLE(R.drawable.ic_vibrate, R.string.menu_vibration),
    MEDIA_PLAY_PAUSE(R.drawable.ic_play_pause, R.string.menu_play_pause),
    MEDIA_NEXT(R.drawable.ic_next_track, R.string.menu_next_track),
    MEDIA_PREVIOUS(R.drawable.ic_previous_track, R.string.menu_previous_track),
    VOLUME_PANEL(R.drawable.ic_volume_panel, R.string.menu_volume_panel),
    SHOW_VOLUME(R.drawable.ic_volume_up, R.string.menu_show_volume),

    // Display
    BRIGHTNESS_UP(R.drawable.ic_brightness_up, R.string.menu_brightness_up),
    BRIGHTNESS_DOWN(R.drawable.ic_brightness_down, R.string.menu_brightness_down),
    TORCH_TOGGLE(R.drawable.ic_torch, R.string.menu_torch),
    AUTO_ROTATE_TOGGLE(R.drawable.ic_screen_rotation, R.string.menu_auto_rotate),

    // Connectivity
    WIFI_TOGGLE(R.drawable.ic_wifi, R.string.menu_wifi),
    BLUETOOTH_TOGGLE(R.drawable.ic_bluetooth, R.string.menu_bluetooth),
    MOBILE_DATA_TOGGLE(R.drawable.ic_mobile_data, R.string.menu_mobile_data),
    AIRPLANE_MODE_TOGGLE(R.drawable.ic_airplane_mode, R.string.menu_airplane_mode),

    // Utilities
    SCREENSHOT(R.drawable.ic_screenshot, R.string.menu_screenshot),
    LOCK_SCREEN(R.drawable.ic_lock, R.string.menu_lock_screen),
    NOTIFICATION(R.drawable.ic_notification, R.string.menu_notification),
    QUICK_SETTINGS(R.drawable.ic_quick_settings, R.string.menu_quick_settings),
    POWER_DIALOG(R.drawable.ic_power_menu, R.string.menu_power_dialog),

    // Apps
    LAUNCH_APP(0, 0)
}
