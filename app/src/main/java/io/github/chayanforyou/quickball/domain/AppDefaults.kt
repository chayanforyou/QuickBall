package io.github.chayanforyou.quickball.domain

import io.github.chayanforyou.quickball.domain.models.HapticIntensity
import io.github.chayanforyou.quickball.domain.models.MenuAction

object AppDefaults {
    const val BALL_SIZE = 45f
    const val BALL_COLOR = 0xBF2C2C2C.toInt()
    const val BALL_ICON_COLOR = 0xFFFFFFFF.toInt()
    const val MENU_COLOR = 0xBF2C2C2C.toInt()
    const val MENU_ICON_COLOR = 0xFFFFFFFF.toInt()
    const val MENU_SIZE = 53f
    const val MENU_ICON_SIZE = 22f
    const val MENU_RADIUS = 96f
    const val TOAST_BG_COLOR = 0xBF2C2C2C.toInt()
    const val TOAST_FG_COLOR = 0xFFFFFFFF.toInt()
    const val PILL_COLOR = 0xCC777777.toInt()
    const val PILL_HEIGHT = 48f
    const val PILL_THICKNESS = 3f
    const val PILL_TOUCH_WIDTH = 25f
    const val PILL_ARC_ANGLE = 40f
    const val GESTURE_ENABLED = false
    val DOUBLE_TAP_ACTION = MenuAction.LOCK_SCREEN.name
    val TRIPLE_TAP_ACTION = MenuAction.SCREENSHOT.name
    val LONG_PRESS_ACTION = MenuAction.POWER_DIALOG.name
    val SWIPE_UP_ACTION = MenuAction.RECENT.name
    val SWIPE_DOWN_ACTION = MenuAction.NOTIFICATION.name
    const val HAPTIC_FEEDBACK_ENABLED = true
    val HAPTIC_INTENSITY = HapticIntensity.LIGHT.name
}
