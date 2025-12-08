package io.github.chayanforyou.quickball

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors
import io.github.chayanforyou.quickball.helpers.AnalyticsHelper
import io.github.chayanforyou.quickball.utils.LanguageUtils

class MainApplication : Application() {
    
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base?.let { LanguageUtils.applyLanguage(it) })
    }
    
    override fun onCreate() {
        super.onCreate()
        AnalyticsHelper.initialize(applicationContext)

        if (DynamicColors.isDynamicColorAvailable().not()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }

        // Enables app wide dynamic theme on Android 12+ using the Material library.
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}