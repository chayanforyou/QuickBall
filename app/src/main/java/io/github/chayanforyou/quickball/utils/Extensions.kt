package io.github.chayanforyou.quickball.utils

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.TypedValue
import androidx.core.content.ContextCompat
import io.github.chayanforyou.quickball.domain.PreferenceManager
import io.github.chayanforyou.quickball.domain.models.InstalledAppModel

fun Context.getAppIcon(packageName: String): Drawable? {
    return try {
        packageManager.getApplicationIcon(packageName)
    } catch (e: PackageManager.NameNotFoundException) {
        ContextCompat.getDrawable(this, android.R.drawable.sym_def_app_icon)
    }
}

fun Context.loadInstalledApps(sortBySelectedFirst: Boolean = false): List<InstalledAppModel> {
    val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
    val selectedApps = PreferenceManager.getSelectedApps(this)

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
                isSelected = selectedApps.contains(appInfo.packageName)
            )
        }

    return if (sortBySelectedFirst) {
        apps.sortedWith(compareByDescending<InstalledAppModel> { it.isSelected }.thenBy { it.appName.lowercase() })
    } else {
        apps.sortedBy { it.appName.lowercase() }
    }
}
