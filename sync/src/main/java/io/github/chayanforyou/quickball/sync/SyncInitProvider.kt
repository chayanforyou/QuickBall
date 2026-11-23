package io.github.chayanforyou.quickball.sync

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import androidx.core.content.ContextCompat

/**
 * Module entry point. Exists so we don't touch the host's `Application.onCreate`:
 * the provider is created at process start, and the service is started right here.
 * While sync is off, the provider only remembers the context.
 */
class SyncInitProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        val ctx = context ?: return true
        appCtx = ctx.applicationContext
        SyncEngine.appContext = appCtx
        if (!SyncState.enabled(ctx)) return true
        runCatching {
            ContextCompat.startForegroundService(ctx, Intent(ctx, SyncService::class.java))
        }
        return true
    }

    override fun query(uri: Uri, p: Array<out String>?, s: String?, a: Array<out String>?, o: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, s: String?, a: Array<out String>?): Int = 0
    override fun update(uri: Uri, v: ContentValues?, s: String?, a: Array<out String>?): Int = 0

    companion object {
        /** App context, valid while the process lives: the engine and commands use it. */
        @Volatile
        var appCtx: Context? = null
            private set
    }
}
