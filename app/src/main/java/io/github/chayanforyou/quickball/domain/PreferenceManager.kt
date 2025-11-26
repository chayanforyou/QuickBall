package io.github.chayanforyou.quickball.domain

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.chayanforyou.quickball.domain.handlers.MenuAction
import io.github.chayanforyou.quickball.domain.models.QuickBallMenuItemModel

object PreferenceManager {
    private const val PREFS_NAME = "quick_ball_prefs"
    private const val KEY_QUICK_BALL_ENABLED = "quick_ball_enabled"
    private const val KEY_SHOW_ON_LOCK_SCREEN = "show_on_lock_screen"
    private const val KEY_SELECTED_MENU_ITEMS = "selected_menu_items"
    private const val KEY_SELECTED_APPS = "selected_apps"
    
    private val gson = Gson()
    
    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isQuickBallEnabled(context: Context): Boolean {
        return getPreferences(context).getBoolean(KEY_QUICK_BALL_ENABLED, false)
    }

    fun setQuickBallEnabled(context: Context, enabled: Boolean) {
        getPreferences(context).edit { 
            putBoolean(KEY_QUICK_BALL_ENABLED, enabled) 
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
    
    fun getSelectedMenuItems(context: Context): List<QuickBallMenuItemModel> {
        val prefs = getPreferences(context)
        val json = prefs.getString(KEY_SELECTED_MENU_ITEMS, null)
        
        return if (json.isNullOrEmpty()) {
            getDefaultSelectedItems()
        } else {
            try {
                val type = object : TypeToken<List<QuickBallMenuItemModel>>() {}.type
                gson.fromJson<List<QuickBallMenuItemModel>>(json, type) ?: getDefaultSelectedItems()
            } catch (e: Exception) {
                android.util.Log.w("PreferenceManager", "Failed to deserialize menu items", e)
                getDefaultSelectedItems()
            }
        }
    }
    
    fun updateMenuItemOrder(context: Context, reorderedItems: List<QuickBallMenuItemModel>) {
        try {
            val json = gson.toJson(reorderedItems)
            getPreferences(context).edit { 
                putString(KEY_SELECTED_MENU_ITEMS, json) 
            }
        } catch (e: Exception) {
            android.util.Log.e("PreferenceManager", "Failed to save menu items", e)
        }
    }
    
    private fun getDefaultSelectedItems(): List<QuickBallMenuItemModel> {
        val defaultActions = listOf(
            MenuAction.VOLUME_UP,
            MenuAction.VOLUME_DOWN,
            MenuAction.BRIGHTNESS_UP,
            MenuAction.BRIGHTNESS_DOWN,
            MenuAction.LOCK_SCREEN
        )
        
        return defaultActions.mapNotNull { action ->
            QuickBallMenuItemModel.getMenuItemByAction(action)
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
}