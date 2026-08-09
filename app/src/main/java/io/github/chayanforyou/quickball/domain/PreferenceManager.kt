package io.github.chayanforyou.quickball.domain

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.chayanforyou.quickball.domain.models.MenuAction
import io.github.chayanforyou.quickball.domain.models.QuickBallMenuItem

object PreferenceManager {
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

    private val gson = Gson()
    private val menuItemListType = object : TypeToken<List<QuickBallMenuItem>>() {}.type

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isQuickBallEnabled(context: Context): Boolean {
        return getPreferences(context).getBoolean(KEY_QUICK_BALL_ENABLED, true)
    }

    fun setQuickBallEnabled(context: Context, enabled: Boolean) {
        getPreferences(context).edit {
            putBoolean(KEY_QUICK_BALL_ENABLED, enabled)
        }
    }

    fun getBallSize(context: Context): Float {
        return getPreferences(context).getFloat(KEY_BALL_SIZE, 45f)
    }

    fun setBallSize(context: Context, size: Float) {
        getPreferences(context).edit {
            putFloat(KEY_BALL_SIZE, size)
        }
    }

    fun isStickToEdgeEnabled(context: Context): Boolean {
        return getPreferences(context).getBoolean(KEY_STICK_TO_EDGE, true)
    }

    fun setStickToEdgeEnabled(context: Context, enabled: Boolean) {
        getPreferences(context).edit {
            putBoolean(KEY_STICK_TO_EDGE, enabled)
        }
    }

    fun isShowOnLockScreenEnabled(context: Context): Boolean {
        return getPreferences(context).getBoolean(KEY_SHOW_ON_LOCK_SCREEN, false)
    }

    fun setShowOnLockScreenEnabled(context: Context, enabled: Boolean) {
        getPreferences(context).edit {
            putBoolean(KEY_SHOW_ON_LOCK_SCREEN, enabled)
        }
    }

    fun isHideOnLandscapeEnabled(context: Context): Boolean {
        return getPreferences(context).getBoolean(KEY_HIDE_ON_LANDSCAPE, false)
    }

    fun setHideOnLandscapeEnabled(context: Context, enabled: Boolean) {
        getPreferences(context).edit {
            putBoolean(KEY_HIDE_ON_LANDSCAPE, enabled)
        }
    }

    fun getSelectedMenuItems(context: Context): List<QuickBallMenuItem> {
        val prefs = getPreferences(context)
        val json = prefs.getString(KEY_SELECTED_MENU_ITEMS, null)
        val defaultItems = getDefaultSelectedItems()

        if (json.isNullOrEmpty()) return defaultItems

        return try {
            val items: List<QuickBallMenuItem>? =
                gson.fromJson(json, menuItemListType)

            items?.mapNotNull { item ->
                item.packageName?.let {
                    // App shortcut → keep
                    item
                } ?: run {
                    // System shortcut → remap
                    QuickBallMenuItem.getMenuItemByAction(item.action)
                }
            } ?: defaultItems

        } catch (_: Exception) {
            prefs.edit { remove(KEY_SELECTED_MENU_ITEMS) }
            defaultItems
        }
    }

    fun updateMenuItemOrder(context: Context, reorderedItems: List<QuickBallMenuItem>) {
        try {
            val json = gson.toJson(reorderedItems)
            getPreferences(context).edit {
                putString(KEY_SELECTED_MENU_ITEMS, json)
            }
        } catch (e: Exception) {
            Log.e("PreferenceManager", "Failed to save menu items", e)
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

    fun setAutoHideApps(context: Context, selectedApps: Set<String>) {
        getPreferences(context).edit {
            putStringSet(KEY_SELECTED_APPS, selectedApps)
        }
    }

    fun getAutoHideApps(context: Context): Set<String> {
        return getPreferences(context).getStringSet(KEY_SELECTED_APPS, emptySet()) ?: emptySet()
    }

    fun addAutoHideApp(context: Context, packageName: String) {
        val currentSelected = getAutoHideApps(context).toMutableSet()
        currentSelected.add(packageName)
        setAutoHideApps(context, currentSelected)
    }

    fun removeAutoHideApp(context: Context, packageName: String) {
        val currentSelected = getAutoHideApps(context).toMutableSet()
        currentSelected.remove(packageName)
        setAutoHideApps(context, currentSelected)
    }

    fun getLanguage(context: Context): String {
        return getPreferences(context).getString(KEY_LANGUAGE, "en") ?: "en"
    }

    fun setLanguage(context: Context, language: String) {
        getPreferences(context).edit {
            putString(KEY_LANGUAGE, language)
        }
    }

    fun getPortraitIsOnRight(context: Context): Boolean {
        return getPreferences(context).getBoolean(KEY_PORTRAIT_IS_ON_RIGHT, true)
    }

    fun getPortraitYFraction(context: Context): Float {
        return getPreferences(context).getFloat(KEY_PORTRAIT_Y_FRACTION, 0.5f)
    }

    fun savePortraitPosition(context: Context, isOnRight: Boolean, yFraction: Float) {
        getPreferences(context).edit {
            putBoolean(KEY_PORTRAIT_IS_ON_RIGHT, isOnRight)
            putFloat(KEY_PORTRAIT_Y_FRACTION, yFraction)
        }
    }

    fun getLandscapeIsOnRight(context: Context): Boolean {
        return getPreferences(context).getBoolean(KEY_LANDSCAPE_IS_ON_RIGHT, true)
    }

    fun getLandscapeYFraction(context: Context): Float {
        return getPreferences(context).getFloat(KEY_LANDSCAPE_Y_FRACTION, 0.5f)
    }

    fun saveLandscapePosition(context: Context, isOnRight: Boolean, yFraction: Float) {
        getPreferences(context).edit {
            putBoolean(KEY_LANDSCAPE_IS_ON_RIGHT, isOnRight)
            putFloat(KEY_LANDSCAPE_Y_FRACTION, yFraction)
        }
    }
}