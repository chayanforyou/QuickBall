package io.github.chayanforyou.quickball.domain

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.chayanforyou.quickball.domain.models.MenuAction
import io.github.chayanforyou.quickball.domain.models.QuickBallMenuItem

class AppPreference private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "quick_ball_prefs"
        private const val KEY_QUICK_BALL_ENABLED = "quick_ball_enabled"
        private const val KEY_BALL_SIZE = "ball_size"
        private const val KEY_BALL_COLOR = "ball_color"
        private const val KEY_BALL_ICON_COLOR = "ball_icon_color"
        private const val KEY_MENU_COLOR = "menu_color"
        private const val KEY_MENU_ICON_COLOR = "menu_icon_color"
        private const val KEY_MENU_SIZE = "menu_size"
        private const val KEY_PILL_COLOR = "pill_color"
        private const val KEY_PILL_HEIGHT = "pill_height"
        private const val KEY_PILL_THICKNESS = "pill_thickness"
        private const val KEY_PILL_TOUCH_WIDTH = "pill_touch_width"
        private const val KEY_PILL_ARC_ANGLE = "pill_arc_angle"
        private const val KEY_PILL_GESTURE_ENABLED = "pill_gesture_enabled"
        private const val KEY_PILL_DOUBLE_TAP = "pill_double_tap"
        private const val KEY_PILL_TRIPLE_TAP = "pill_triple_tap"
        private const val KEY_PILL_LONG_PRESS = "pill_long_press"
        private const val KEY_PILL_SWIPE_UP = "pill_swipe_up"
        private const val KEY_PILL_SWIPE_DOWN = "pill_swipe_down"
        private const val KEY_STICK_TO_EDGE = "stick_to_edge"
        private const val KEY_SHOW_ON_LOCK_SCREEN = "show_on_lock_screen"
        private const val KEY_HIDE_ON_LANDSCAPE = "hide_on_landscape"
        private const val KEY_SELECTED_MENU_ITEMS = "selected_menu_items"
        private const val KEY_SELECTED_APPS = "selected_apps"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_PORTRAIT_IS_ON_RIGHT = "portrait_is_on_right"
        private const val KEY_PORTRAIT_Y_FRACTION = "portrait_y_fraction"
        private const val KEY_LANDSCAPE_IS_ON_RIGHT = "landscape_is_on_right"
        private const val KEY_LANDSCAPE_Y_FRACTION = "landscape_y_fraction"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_HAPTIC_FEEDBACK_ENABLED = "haptic_feedback_enabled"
        private const val KEY_HAPTIC_INTENSITY = "haptic_intensity"

        private val gson = Gson()
        private val menuItemListType = object : TypeToken<List<QuickBallMenuItem>>() {}.type

        @Volatile
        private var INSTANCE: AppPreference? = null

        fun getInstance(context: Context): AppPreference {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppPreference(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    var isOnboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) = prefs.edit { putBoolean(KEY_ONBOARDING_COMPLETED, value) }

    var isQuickBallEnabled: Boolean
        get() = prefs.getBoolean(KEY_QUICK_BALL_ENABLED, true)
        set(value) = prefs.edit { putBoolean(KEY_QUICK_BALL_ENABLED, value) }

    var ballSize: Float
        get() = prefs.getFloat(KEY_BALL_SIZE, AppDefaults.BALL_SIZE)
        set(value) = prefs.edit { putFloat(KEY_BALL_SIZE, value) }

    var ballColor: Int
        get() = prefs.getInt(KEY_BALL_COLOR, AppDefaults.BALL_COLOR)
        set(value) = prefs.edit { putInt(KEY_BALL_COLOR, value) }

    var ballIconColor: Int
        get() = prefs.getInt(KEY_BALL_ICON_COLOR, AppDefaults.BALL_ICON_COLOR)
        set(value) = prefs.edit { putInt(KEY_BALL_ICON_COLOR, value) }

    var menuColor: Int
        get() = prefs.getInt(KEY_MENU_COLOR, AppDefaults.MENU_COLOR)
        set(value) = prefs.edit { putInt(KEY_MENU_COLOR, value) }

    var menuIconColor: Int
        get() = prefs.getInt(KEY_MENU_ICON_COLOR, AppDefaults.MENU_ICON_COLOR)
        set(value) = prefs.edit { putInt(KEY_MENU_ICON_COLOR, value) }

    var menuSize: Float
        get() = prefs.getFloat(KEY_MENU_SIZE, AppDefaults.MENU_SIZE)
        set(value) = prefs.edit { putFloat(KEY_MENU_SIZE, value) }

    var pillColor: Int
        get() = prefs.getInt(KEY_PILL_COLOR, AppDefaults.PILL_COLOR)
        set(value) = prefs.edit { putInt(KEY_PILL_COLOR, value) }

    var pillHeight: Float
        get() = prefs.getFloat(KEY_PILL_HEIGHT, AppDefaults.PILL_HEIGHT)
        set(value) = prefs.edit { putFloat(KEY_PILL_HEIGHT, value) }

    var pillThickness: Float
        get() = prefs.getFloat(KEY_PILL_THICKNESS, AppDefaults.PILL_THICKNESS)
        set(value) = prefs.edit { putFloat(KEY_PILL_THICKNESS, value) }

    var pillTouchWidth: Float
        get() = prefs.getFloat(KEY_PILL_TOUCH_WIDTH, AppDefaults.PILL_TOUCH_WIDTH)
        set(value) = prefs.edit { putFloat(KEY_PILL_TOUCH_WIDTH, value) }

    var pillArcAngle: Float
        get() = prefs.getFloat(KEY_PILL_ARC_ANGLE, AppDefaults.PILL_ARC_ANGLE)
        set(value) = prefs.edit { putFloat(KEY_PILL_ARC_ANGLE, value) }

    var isPillGestureEnabled: Boolean
        get() = prefs.getBoolean(KEY_PILL_GESTURE_ENABLED, AppDefaults.PILL_GESTURE_ENABLED)
        set(value) = prefs.edit { putBoolean(KEY_PILL_GESTURE_ENABLED, value) }

    var pillDoubleTapAction: String
        get() = prefs.getString(KEY_PILL_DOUBLE_TAP, AppDefaults.PILL_DOUBLE_TAP_ACTION) ?: AppDefaults.PILL_DOUBLE_TAP_ACTION
        set(value) = prefs.edit { putString(KEY_PILL_DOUBLE_TAP, value) }

    var pillTripleTapAction: String
        get() = prefs.getString(KEY_PILL_TRIPLE_TAP, AppDefaults.PILL_TRIPLE_TAP_ACTION) ?: AppDefaults.PILL_TRIPLE_TAP_ACTION
        set(value) = prefs.edit { putString(KEY_PILL_TRIPLE_TAP, value) }

    var pillLongPressAction: String
        get() = prefs.getString(KEY_PILL_LONG_PRESS, AppDefaults.PILL_LONG_PRESS_ACTION) ?: AppDefaults.PILL_LONG_PRESS_ACTION
        set(value) = prefs.edit { putString(KEY_PILL_LONG_PRESS, value) }

    var pillSwipeUpAction: String
        get() = prefs.getString(KEY_PILL_SWIPE_UP, AppDefaults.PILL_SWIPE_UP_ACTION) ?: AppDefaults.PILL_SWIPE_UP_ACTION
        set(value) = prefs.edit { putString(KEY_PILL_SWIPE_UP, value) }

    var pillSwipeDownAction: String
        get() = prefs.getString(KEY_PILL_SWIPE_DOWN, AppDefaults.PILL_SWIPE_DOWN_ACTION) ?: AppDefaults.PILL_SWIPE_DOWN_ACTION
        set(value) = prefs.edit { putString(KEY_PILL_SWIPE_DOWN, value) }

    var isHapticFeedbackEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTIC_FEEDBACK_ENABLED, AppDefaults.HAPTIC_FEEDBACK_ENABLED)
        set(value) = prefs.edit { putBoolean(KEY_HAPTIC_FEEDBACK_ENABLED, value) }

    var hapticIntensity: String
        get() = prefs.getString(KEY_HAPTIC_INTENSITY, AppDefaults.HAPTIC_INTENSITY) ?: AppDefaults.HAPTIC_INTENSITY
        set(value) = prefs.edit { putString(KEY_HAPTIC_INTENSITY, value) }

    var isStickToEdgeEnabled: Boolean
        get() = prefs.getBoolean(KEY_STICK_TO_EDGE, true)
        set(value) = prefs.edit { putBoolean(KEY_STICK_TO_EDGE, value) }

    var isShowOnLockScreenEnabled: Boolean
        get() = prefs.getBoolean(KEY_SHOW_ON_LOCK_SCREEN, false)
        set(value) = prefs.edit { putBoolean(KEY_SHOW_ON_LOCK_SCREEN, value) }

    var isHideOnLandscapeEnabled: Boolean
        get() = prefs.getBoolean(KEY_HIDE_ON_LANDSCAPE, false)
        set(value) = prefs.edit { putBoolean(KEY_HIDE_ON_LANDSCAPE, value) }

    var language: String
        get() = prefs.getString(KEY_LANGUAGE, "en") ?: "en"
        set(value) = prefs.edit { putString(KEY_LANGUAGE, value) }

    var autoHideApps: Set<String>
        get() = prefs.getStringSet(KEY_SELECTED_APPS, emptySet()) ?: emptySet()
        set(value) = prefs.edit { putStringSet(KEY_SELECTED_APPS, value) }

    var selectedMenuItems: List<QuickBallMenuItem>
        get() {
            val json = prefs.getString(KEY_SELECTED_MENU_ITEMS, null)
            if (json.isNullOrEmpty()) return getDefaultSelectedItems()

            return try {
                val items: List<QuickBallMenuItem>? = gson.fromJson(json, menuItemListType)
                val validItems = items?.mapNotNull { item ->
                    if (item.packageName != null) {
                        item.copy(action = MenuAction.LAUNCH_APP)
                    } else {
                        val action = item.action ?: return@mapNotNull null
                        QuickBallMenuItem.getMenuItemByAction(action)
                    }
                }
                if (validItems != null && validItems.size >= 2) validItems else getDefaultSelectedItems()
            } catch (_: Exception) {
                prefs.edit { remove(KEY_SELECTED_MENU_ITEMS) }
                getDefaultSelectedItems()
            }
        }
        set(value) {
            try {
                val json = gson.toJson(value)
                prefs.edit { putString(KEY_SELECTED_MENU_ITEMS, json) }
            } catch (e: Exception) {
                Log.e("PreferenceManager", "Failed to save menu items", e)
            }
        }

    var portraitIsOnRight: Boolean
        get() = prefs.getBoolean(KEY_PORTRAIT_IS_ON_RIGHT, true)
        set(value) = prefs.edit { putBoolean(KEY_PORTRAIT_IS_ON_RIGHT, value) }

    var portraitYFraction: Float
        get() = prefs.getFloat(KEY_PORTRAIT_Y_FRACTION, 0.5f)
        set(value) = prefs.edit { putFloat(KEY_PORTRAIT_Y_FRACTION, value) }

    fun savePortraitPosition(isOnRight: Boolean, yFraction: Float) {
        prefs.edit {
            putBoolean(KEY_PORTRAIT_IS_ON_RIGHT, isOnRight)
            putFloat(KEY_PORTRAIT_Y_FRACTION, yFraction)
        }
    }

    var landscapeIsOnRight: Boolean
        get() = prefs.getBoolean(KEY_LANDSCAPE_IS_ON_RIGHT, true)
        set(value) = prefs.edit { putBoolean(KEY_LANDSCAPE_IS_ON_RIGHT, value) }

    var landscapeYFraction: Float
        get() = prefs.getFloat(KEY_LANDSCAPE_Y_FRACTION, 0.5f)
        set(value) = prefs.edit { putFloat(KEY_LANDSCAPE_Y_FRACTION, value) }

    fun saveLandscapePosition(isOnRight: Boolean, yFraction: Float) {
        prefs.edit {
            putBoolean(KEY_LANDSCAPE_IS_ON_RIGHT, isOnRight)
            putFloat(KEY_LANDSCAPE_Y_FRACTION, yFraction)
        }
    }

    private fun getDefaultSelectedItems(): List<QuickBallMenuItem> {
        val defaultActions = listOf(
            MenuAction.VOLUME_UP,
            MenuAction.VOLUME_DOWN,
            MenuAction.BRIGHTNESS_UP,
            MenuAction.BRIGHTNESS_DOWN,
            MenuAction.LOCK_SCREEN
        )

        return defaultActions.mapNotNull { action ->
            QuickBallMenuItem.getMenuItemByAction(action)
        }
    }
}