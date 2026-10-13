package io.github.chayanforyou.quickball.domain

import io.github.chayanforyou.quickball.domain.models.HapticIntensity
import io.github.chayanforyou.quickball.domain.models.MenuAction
import io.github.chayanforyou.quickball.domain.models.MenuConnectorStyle

object AppDefaults {
    const val BALL_SIZE = 45f
    const val BALL_COLOR = 0xBF2C2C2C.toInt()
    const val BALL_ICON_COLOR = 0xFFFFFFFF.toInt()
    const val BALL_BORDER_COLOR = 0xFFFFFFFF.toInt()
    const val BALL_BORDER_WIDTH = 0f
    const val BORDER_WIDTH_MIN = 0f
    const val BORDER_WIDTH_MAX = 4f
    const val MENU_COLOR = 0xBF2C2C2C.toInt()
    const val MENU_ICON_COLOR = 0xFFFFFFFF.toInt()
    const val MENU_BORDER_COLOR = 0xFFFFFFFF.toInt()
    const val MENU_BORDER_WIDTH = 0f
    const val MENU_CONNECTOR_COLOR = 0x802C2C2C.toInt()
    val MENU_CONNECTOR_STYLE = MenuConnectorStyle.DOTTED.name
    const val MENU_CONNECTOR_WIDTH = 2f
    const val MENU_CONNECTOR_WIDTH_MIN = 1f
    const val MENU_CONNECTOR_WIDTH_MAX = 3f
    const val MENU_SIZE = 50f
    const val MENU_SIZE_MIN = 40f
    const val MENU_SIZE_MAX = 56f
    const val MENU_ICON_SIZE = 24f
    const val MENU_RADIUS = 90f
    const val MENU_RADIUS_MIN = 60f
    const val MENU_RADIUS_MAX = 120f
    const val MENU_ITEMS_MIN = 3
    const val MENU_ITEMS_MAX = 9
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
