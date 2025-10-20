package io.github.chayanforyou.quickball.domain

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.chayanforyou.quickball.domain.handlers.MenuAction
import io.github.chayanforyou.quickball.domain.models.MenuItemModel

object PreferenceManager {
    private const val PREFS_NAME = "quick_ball_prefs"
    private const val KEY_QUICK_BALL_ENABLED = "quick_ball_enabled"
    private const val KEY_SELECTED_MENU_ITEMS = "selected_menu_items"
    
    private val gson = Gson()
    
    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isQuickBallEnabled(context: Context): Boolean {
        val prefs = getPreferences(context)
        return prefs.getBoolean(KEY_QUICK_BALL_ENABLED, false)
    }

    fun setQuickBallEnabled(context: Context, enabled: Boolean) {
        val prefs = getPreferences(context)
        prefs.edit { putBoolean(KEY_QUICK_BALL_ENABLED, enabled) }
    }
    
    fun getSelectedMenuItems(context: Context): List<MenuItemModel> {
        val prefs = getPreferences(context)
        val json = prefs.getString(KEY_SELECTED_MENU_ITEMS, null)
        
        return if (json.isNullOrEmpty()) {
            getDefaultSelectedItems()
        } else {
            try {
                val type = object : TypeToken<List<MenuItemModel>>() {}.type
                gson.fromJson<List<MenuItemModel>>(json, type) ?: getDefaultSelectedItems()
            } catch (_: Exception) {
                getDefaultSelectedItems()
            }
        }
    }
    
    fun updateMenuItemOrder(context: Context, reorderedItems: List<MenuItemModel>) {
        val prefs = getPreferences(context)
        val json = gson.toJson(reorderedItems)
        prefs.edit { putString(KEY_SELECTED_MENU_ITEMS, json) }
    }
    
    private fun getDefaultSelectedItems(): List<MenuItemModel> {
        val defaultActions = listOf(
            MenuAction.VOLUME_UP,
            MenuAction.VOLUME_DOWN,
            MenuAction.BRIGHTNESS_UP,
            MenuAction.BRIGHTNESS_DOWN,
            MenuAction.LOCK_SCREEN
        )
        
        return defaultActions.mapNotNull { action ->
            MenuItemModel.getMenuItemByAction(action)
        }
    }
}