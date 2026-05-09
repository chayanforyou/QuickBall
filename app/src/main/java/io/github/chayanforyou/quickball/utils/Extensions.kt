package io.github.chayanforyou.quickball.utils

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.WindowManager
import androidx.core.content.ContextCompat
import io.github.chayanforyou.quickball.domain.PreferenceManager
import io.github.chayanforyou.quickball.domain.models.InstalledAppModel

/**
 * Get the application icon for a given package name.
 * Returns the app's icon drawable or a default app icon if the package is not found.
 *
 * @param packageName The package name of the application
 * @return The app's icon drawable, or default icon if package not found
 */
fun Context.getAppIcon(packageName: String): Drawable? {
    return try {
        packageManager.getApplicationIcon(packageName)
    } catch (_: PackageManager.NameNotFoundException) {
        ContextCompat.getDrawable(this, android.R.drawable.sym_def_app_icon)
    }
}

/**
 * Load all installed applications that have a launcher intent.
 * Filters out the current app and apps without proper names.
 *
 * @param sortBySelectedFirst If true, selected apps (from auto-hide list) appear first,
 *                            then sorted alphabetically. If false, all apps sorted alphabetically.
 * @return List of installed apps with their name, package, icon, and selection state
 */
fun Context.loadInstalledApps(sortBySelectedFirst: Boolean = false): List<InstalledAppModel> {
    val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
    val autoHideApps = PreferenceManager.getAutoHideApps(this)

    val apps = installedApps
        .filter { appInfo ->
            packageManager.getLaunchIntentForPackage(appInfo.packageName) != null &&
                    appInfo.packageName != packageName
        }
        .mapNotNull { appInfo ->
            val appName = packageManager.getApplicationLabel(appInfo).toString()
            if (appName.isBlank() || appName.equals(appInfo.packageName, ignoreCase = true)) return@mapNotNull null
            val icon = packageManager.getApplicationIcon(appInfo)
            InstalledAppModel(
                appName = appName,
                packageName = appInfo.packageName,
                icon = icon,
                isSelected = autoHideApps.contains(appInfo.packageName)
            )
        }

    return if (sortBySelectedFirst) {
        apps.sortedWith(compareByDescending<InstalledAppModel> { it.isSelected }.thenBy { it.appName.lowercase() })
    } else {
        apps.sortedBy { it.appName.lowercase() }
    }
}

/**
 * Get the actual screen width accounting for current orientation and system UI.
 * This is more reliable than DisplayMetrics, especially after orientation changes.
 *
 * @return The actual screen width in pixels
 */
fun WindowManager.getActualScreenWidth(): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        currentWindowMetrics.bounds.width()
    } else {
        @Suppress("DEPRECATION")
        val display = defaultDisplay
        val realSize = Point()
        @Suppress("DEPRECATION")
        display.getRealSize(realSize)
        realSize.x
    }
}

/**
 * Get the actual screen height accounting for current orientation and system UI.
 * This is more reliable than DisplayMetrics, especially after orientation changes.
 *
 * @return The actual screen height in pixels
 */
fun WindowManager.getActualScreenHeight(): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        currentWindowMetrics.bounds.height()
    } else {
        @Suppress("DEPRECATION")
        val display = defaultDisplay
        val realSize = Point()
        @Suppress("DEPRECATION")
        display.getRealSize(realSize)
        realSize.y
    }
}
