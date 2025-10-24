package io.github.chayanforyou.quickball.utils

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.chayanforyou.quickball.R

object DialogUtil {

    fun showAccessibilityPermissionDialog(
        context: Context,
        onAccept: () -> Unit,
        onQuit: () -> Unit
    ) {
        showPermissionDialog(
            context = context,
            title = context.getString(R.string.accessibility_dialog_title),
            message = context.getString(R.string.accessibility_dialog_message),
            onAccept = onAccept,
            onQuit = onQuit
        )
    }

    fun showSystemSettingsPermissionDialog(
        context: Context,
        onAccept: () -> Unit,
        onQuit: () -> Unit
    ) {
        showPermissionDialog(
            context = context,
            title = context.getString(R.string.system_settings_dialog_title),
            message = context.getString(R.string.system_settings_dialog_message),
            onAccept = onAccept,
            onQuit = onQuit
        )
    }

    private fun showPermissionDialog(
        context: Context,
        title: String,
        message: String,
        onAccept: () -> Unit,
        onQuit: () -> Unit
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(false)
            .setNegativeButton(context.getString(R.string.dialog_button_quit)) { dialog, _ ->
                dialog.dismiss()
                onQuit()
            }
            .setPositiveButton(context.getString(R.string.dialog_button_accept)) { dialog, _ ->
                dialog.dismiss()
                onAccept()
            }
            .show()
    }
}