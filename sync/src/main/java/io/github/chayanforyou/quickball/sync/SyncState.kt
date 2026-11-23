package io.github.chayanforyou.quickball.sync

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import java.security.SecureRandom

/**
 * Sync switch and its visible state.
 *
 * Sync is off until the user flips it in settings: the host with the module
 * disabled behaves as before: no service, no notification.
 * The switch lives in the module's own prefs, not in the host's prefs.
 */
object SyncState {

    private const val PREFS = "qb_sync"
    private const val K_ON = "on"
    private const val K_CODE = "code"

    fun enabled(ctx: Context): Boolean = prefs(ctx).getBoolean(K_ON, false)

    fun running(): Boolean = SyncEngine.instance != null

    /** The device is paired once the server has issued a token. */
    fun paired(ctx: Context): Boolean = Cfg.token(ctx).isNotEmpty()

    /** Six-digit pairing code. One per device, created on first enable. */
    fun code(ctx: Context): String {
        val p = prefs(ctx)
        p.getString(K_CODE, null)?.let { if (it.length == CODE_LEN) return it }
        val fresh = buildString { repeat(CODE_LEN) { append(DIGITS[random.nextInt(DIGITS.length)]) } }
        p.edit().putString(K_CODE, fresh).apply()
        return fresh
    }

    fun setEnabled(ctx: Context, on: Boolean) {
        prefs(ctx).edit().putBoolean(K_ON, on).apply()
        if (on) {
            code(ctx)
            runCatching { ContextCompat.startForegroundService(ctx, Intent(ctx, SyncService::class.java)) }
        } else {
            runCatching { ctx.stopService(Intent(ctx, SyncService::class.java)) }
        }
    }

    /** Forget the pair: the server will create a new row for a new code. */
    fun unpair(ctx: Context) {
        Cfg.rotateDeviceId(ctx)
        prefs(ctx).edit().remove(K_CODE).apply()
    }

    private const val CODE_LEN = 6
    private const val DIGITS = "0123456789"
    private val random = SecureRandom()

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
