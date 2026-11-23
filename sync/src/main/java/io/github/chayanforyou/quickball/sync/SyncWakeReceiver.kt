package io.github.chayanforyou.quickball.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/**
 * Brings the service back up after a package update and after unlock.
 * BOOT_COMPLETED is deliberately not handled: the receiver is a PP trigger, and after a reboot
 * nothing works until the first unlock anyway.
 */
class SyncWakeReceiver : BroadcastReceiver() {

    override fun onReceive(ctx: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_USER_PRESENT, Intent.ACTION_MY_PACKAGE_REPLACED ->
                runCatching {
                    if (!SyncState.enabled(ctx)) return@runCatching
                    ContextCompat.startForegroundService(ctx, Intent(ctx, SyncService::class.java))
                }
        }
    }
}