package io.github.chayanforyou.quickball.utils

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.DisplayMetrics
import android.view.Display
import android.view.WindowManager
import android.view.WindowMetrics
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
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
            if (appName.isBlank() || appName.equals(
                    appInfo.packageName,
                    ignoreCase = true
                )
            ) return@mapNotNull null
            val icon = packageManager.getApplicationIcon(appInfo)
            InstalledAppModel(
                appName = appName,
                packageName = appInfo.packageName,
                icon = icon,
                isSelected = autoHideApps.contains(appInfo.packageName)
            )
        }

    return if (sortBySelectedFirst) {
        apps.sortedWith(compareByDescending<InstalledAppModel> { it.isSelected }
            .thenBy { it.appName.lowercase() })
    } else {
        apps.sortedBy { it.appName.lowercase() }
    }
}

/**
 * Returns the actual screen size in pixels as a [Pair] of width and height.
 *
 * Compared to [DisplayMetrics], this provides more reliable dimensions
 * across orientation changes, multi-window mode, and different system UI
 * configurations.
 *
 * @return A [Pair] where:
 * - first = screen width in pixels
 * - second = screen height in pixels
 */
fun Context.getScreenSize(): Pair<Int, Int> {
    return getSystemService<WindowManager>()?.getScreenSize() ?: (0 to 0)
}

/**
 * Returns the actual screen size in pixels as a [Pair] of width and height.
 *
 * Uses [WindowMetrics] on Android R and above, and [Display.getRealSize]
 * on older Android versions for compatibility.
 *
 * @return A [Pair] where:
 * - first = screen width in pixels
 * - second = screen height in pixels
 */
fun WindowManager.getScreenSize(): Pair<Int, Int> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val bounds = currentWindowMetrics.bounds
        bounds.width() to bounds.height()
    } else {
        @Suppress("DEPRECATION")
        Point().apply {
            defaultDisplay.getRealSize(this)
        }.let {
            it.x to it.y
        }
    }
}