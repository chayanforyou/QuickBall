package io.github.chayanforyou.quickball.domain.handlers

import io.github.chayanforyou.quickball.domain.models.QuickBallMenuItemModel

interface QuickBallMenuActionHandler {
    fun onMenuAction(menuItem: QuickBallMenuItemModel)
}

enum class MenuAction {
    // Navigation
    HOME,
    BACK,
    RECENT,

    // Volume / Sound
    VOLUME_UP,
    VOLUME_DOWN,
    SILENT_TOGGLE,
    VIBRATE_TOGGLE,

    // Display
    BRIGHTNESS_UP,
    BRIGHTNESS_DOWN,
    TORCH_TOGGLE,

    // Connectivity
    WIFI_TOGGLE,
    BLUETOOTH_TOGGLE,
    MOBILE_DATA_TOGGLE,

    // Utilities
    SCREENSHOT,
    LOCK_SCREEN,
    
    // Apps
    LAUNCH_APP
}