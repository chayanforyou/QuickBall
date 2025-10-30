package io.github.chayanforyou.quickball

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        if (DynamicColors.isDynamicColorAvailable().not()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }

        // Enables app wide dynamic theme on Android 12+ using the Material library.
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}