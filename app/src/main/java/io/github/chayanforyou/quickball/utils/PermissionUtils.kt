package io.github.chayanforyou.quickball.utils

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import io.github.chayanforyou.quickball.core.QuickBallService

object PermissionUtils {

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val manager = getSystemService(context, AccessibilityManager::class.java)
        val services = manager?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            ?: return false
        return services.any { it.resolveInfo.serviceInfo.packageName == context.packageName }
    }

    fun canModifySystemSettings(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.System.canWrite(context)
        } else true
    }

    fun openAccessibilitySettings(context: Context) {
        try {
            val key = ComponentName(context.packageName, QuickBallService::class.java.name).flattenToString()
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                putExtra(":settings:fragment_args_key", key)
                putExtra(
                    ":settings:show_fragment_args",
                    bundleOf(":settings:fragment_args_key" to key)
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Could not open accessibility settings", Toast.LENGTH_SHORT).show()
        }
    }

    fun openSystemSettingsPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = "package:${context.packageName}".toUri()
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }
}
