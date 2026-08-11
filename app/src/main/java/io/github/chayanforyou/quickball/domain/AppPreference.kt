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
        get() = prefs.getFloat(KEY_BALL_SIZE, 45f)
        set(value) = prefs.edit { putFloat(KEY_BALL_SIZE, value) }

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
            val defaultItems = getDefaultSelectedItems()

            if (json.isNullOrEmpty()) return defaultItems

            return try {
                val items: List<QuickBallMenuItem>? = gson.fromJson(json, menuItemListType)
                items?.mapNotNull { item ->
                    item.packageName?.let { item }
                        ?: QuickBallMenuItem.getMenuItemByAction(item.action)
                } ?: defaultItems
            } catch (_: Exception) {
                prefs.edit { remove(KEY_SELECTED_MENU_ITEMS) }
                defaultItems
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