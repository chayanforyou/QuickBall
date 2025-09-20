package io.github.chayanforyou.quickball.domain.handlers

interface MenuActionHandler {
    fun onMenuAction(action: MenuAction)
}

enum class MenuAction {
    VOLUME_UP,
    VOLUME_DOWN,
    BRIGHTNESS_UP,
    BRIGHTNESS_DOWN,
    LOCK_SCREEN,
    TAKE_SCREENSHOT
}