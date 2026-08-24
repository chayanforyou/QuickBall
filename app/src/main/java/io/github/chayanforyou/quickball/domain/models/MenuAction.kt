package io.github.chayanforyou.quickball.domain.models

import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import androidx.annotation.StringRes
import com.google.gson.annotations.SerializedName
import io.github.chayanforyou.quickball.R

@Keep
enum class MenuAction(
    @field:DrawableRes val iconRes: Int = 0,
    @field:StringRes val titleRes: Int = 0
) {
    // Navigation
    @SerializedName("HOME")
    HOME(R.drawable.ic_home, R.string.menu_home),
    @SerializedName("BACK")
    BACK(R.drawable.ic_back, R.string.menu_back),
    @SerializedName("RECENT")
    RECENT(R.drawable.ic_recent, R.string.menu_recent),

    // Media & Sound
    @SerializedName("VOLUME_UP")
    VOLUME_UP(R.drawable.ic_volume_up, R.string.menu_volume_up),
    @SerializedName("VOLUME_DOWN")
    VOLUME_DOWN(R.drawable.ic_volume_down, R.string.menu_volume_down),
    @SerializedName(value = "DND_TOGGLE", alternate = ["SILENT_TOGGLE"])
    DND_TOGGLE(R.drawable.ic_dnd, R.string.menu_dnd),
    @SerializedName("VIBRATE_TOGGLE")
    VIBRATE_TOGGLE(R.drawable.ic_vibrate, R.string.menu_vibration),
    @SerializedName("MEDIA_PLAY_PAUSE")
    MEDIA_PLAY_PAUSE(R.drawable.ic_play_pause, R.string.menu_play_pause),
    @SerializedName("MEDIA_NEXT")
    MEDIA_NEXT(R.drawable.ic_next_track, R.string.menu_next_track),
    @SerializedName("MEDIA_PREVIOUS")
    MEDIA_PREVIOUS(R.drawable.ic_previous_track, R.string.menu_previous_track),
    @SerializedName("VOLUME_BAR")
    VOLUME_BAR(R.drawable.ic_volume_up, R.string.menu_sys_volume_bar),
    @SerializedName("VOLUME_PANEL")
    VOLUME_PANEL(R.drawable.ic_volume_panel, R.string.menu_sys_volume_panel),

    // Display
    @SerializedName("BRIGHTNESS_UP")
    BRIGHTNESS_UP(R.drawable.ic_brightness_up, R.string.menu_brightness_up),
    @SerializedName("BRIGHTNESS_DOWN")
    BRIGHTNESS_DOWN(R.drawable.ic_brightness_down, R.string.menu_brightness_down),
    @SerializedName("TORCH_TOGGLE")
    TORCH_TOGGLE(R.drawable.ic_torch, R.string.menu_flashlight),
    @SerializedName("AUTO_ROTATE_TOGGLE")
    AUTO_ROTATE_TOGGLE(R.drawable.ic_screen_rotation, R.string.menu_auto_rotate),

    // Connectivity
    @SerializedName("WIFI_TOGGLE")
    WIFI_TOGGLE(R.drawable.ic_wifi, R.string.menu_wifi),
    @SerializedName("BLUETOOTH_TOGGLE")
    BLUETOOTH_TOGGLE(R.drawable.ic_bluetooth, R.string.menu_bluetooth),
    @SerializedName("MOBILE_DATA_TOGGLE")
    MOBILE_DATA_TOGGLE(R.drawable.ic_mobile_data, R.string.menu_mobile_data),
    @SerializedName("AIRPLANE_MODE_TOGGLE")
    AIRPLANE_MODE_TOGGLE(R.drawable.ic_airplane_mode, R.string.menu_airplane_mode),

    // Utilities
    @SerializedName("SCREENSHOT")
    SCREENSHOT(R.drawable.ic_screenshot, R.string.menu_screenshot),
    @SerializedName("LOCK_SCREEN")
    LOCK_SCREEN(R.drawable.ic_lock, R.string.menu_lock_screen),
    @SerializedName("NOTIFICATION")
    NOTIFICATION(R.drawable.ic_notification, R.string.menu_notification),
    @SerializedName("QUICK_SETTINGS")
    QUICK_SETTINGS(R.drawable.ic_quick_settings, R.string.menu_quick_settings),
    @SerializedName("POWER_DIALOG")
    POWER_DIALOG(R.drawable.ic_power_menu, R.string.menu_power_dialog),

    // Apps
    @SerializedName("LAUNCH_APP")
    LAUNCH_APP(0, 0);

    companion object {
        fun fromName(name: String?): MenuAction? {
            if (name == null) return null
            if (name == "SILENT_TOGGLE") return DND_TOGGLE
            return runCatching { valueOf(name) }.getOrNull()
        }
    }
}