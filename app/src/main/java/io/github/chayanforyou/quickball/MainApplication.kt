package io.github.chayanforyou.quickball

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors
import io.github.chayanforyou.quickball.helpers.AnalyticsHelper
import io.github.chayanforyou.quickball.utils.LanguageUtils

class MainApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        AnalyticsHelper.initialize(applicationContext)

        // Apply saved language setting using modern approach
        LanguageUtils.applyLanguage(this)

        // Follow system theme setting
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        // Enables app wide dynamic theme on Android 12+ using the Material library.
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}