package io.github.chayanforyou.quickball.sync

import android.content.Context
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

/**
 * Snapshot of the host's user settings.
 *
 * QuickBall settings live in `quick_ball_prefs`, i.e. in a plain XML
 * SharedPreferences. Sync therefore doesn't hook into the host's keys: the file
 * is read as is and returned the same way, and keys missing from the new version
 * simply stay in the file.
 *
 * Our own file (`qb_sync`) is not part of the snapshot — the token and device_id
 * must not travel to the second device.
 */
object SyncPrefs {

    private const val DIR = "shared_prefs"
    private const val OWN = "qb_sync.xml"
    private const val LIMIT = 256 * 1024

    /** JSON {file name: base64(xml)}, empty object if there are no settings yet. */
    fun dump(ctx: Context): String {
        val out = JSONObject()
        val dir = java.io.File(ctx.applicationInfo.dataDir, DIR)
        val files = dir.listFiles() ?: return out.toString()
        var budget = LIMIT
        for (f in files.sortedBy { it.name }) {
            if (!f.isFile || f.name == OWN || f.length() == 0L || f.length() > budget) continue
            val raw = runCatching { f.readBytes() }.getOrNull() ?: continue
            budget -= raw.size
            out.put(f.name, android.util.Base64.encodeToString(raw, android.util.Base64.NO_WRAP))
        }
        return out.toString()
    }

    /**
     * Merges a snapshot into SharedPreferences. Returns the number of files written.
     * A broken or foreign file is skipped silently: sync must not break
     * an app that is still running.
     */
    fun apply(ctx: Context, snapshot: String): Int {
        val root = runCatching { JSONObject(snapshot) }.getOrNull() ?: return 0
        var applied = 0
        for (name in root.keys()) {
            val raw = runCatching {
                android.util.Base64.decode(root.optString(name), android.util.Base64.NO_WRAP)
            }.getOrNull() ?: continue
            if (runCatching { merge(ctx, name.removeSuffix(".xml"), raw) }.getOrDefault(false)) applied++
        }
        return applied
    }

    private fun merge(ctx: Context, prefsName: String, xml: ByteArray): Boolean {
        if (prefsName.isBlank() || prefsName == OWN.removeSuffix(".xml")) return false
        val body = if (xml.size >= 3 && xml[0] == 0xEF.toByte() && xml[1] == 0xBB.toByte() && xml[2] == 0xBF.toByte())
            xml.copyOfRange(3, xml.size) else xml
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(body.inputStream(), "utf-8")
        val prefs = ctx.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        var changed = false
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name != "map") {
                val key = parser.getAttributeValue(null, "name") ?: ""
                when {
                    key.isEmpty() -> Unit
                    parser.name == "set" -> {
                        val items = mutableSetOf<String>()
                        while (parser.next() != XmlPullParser.END_DOCUMENT) {
                            if (parser.eventType == XmlPullParser.END_TAG && parser.name == "set") break
                            if (parser.eventType == XmlPullParser.START_TAG) items.add(parser.nextText())
                        }
                        if (write(editor, prefs, "set", key, items)) changed = true
                    }
                    parser.name == "string" ->
                        if (write(editor, prefs, "string", key, parser.nextText())) changed = true
                    else ->
                        if (write(editor, prefs, parser.name, key, parser.getAttributeValue(null, "value") ?: ""))
                            changed = true
                }
            }
            event = parser.next()
        }
        if (changed) editor.commit()
        return changed
    }

    /**
     * Writes the value using the type already stored in prefs: between app versions
     * a key may have changed type (color from string to int), and a foreign write
     * would otherwise crash the read with ClassCastException.
     */
    private fun write(
        editor: android.content.SharedPreferences.Editor,
        prefs: android.content.SharedPreferences,
        tag: String,
        key: String,
        raw: Any,
    ): Boolean = runCatching {
        when (prefs.all[key]) {
            is Int -> editor.putInt(key, toInt(raw))
            is Long -> editor.putLong(key, raw.toString().toLong())
            is Float -> editor.putFloat(key, raw.toString().toFloat())
            is Boolean -> editor.putBoolean(key, raw.toString().toBoolean())
            is Set<*> -> editor.putStringSet(key, (raw as? Set<*>)?.map { it.toString() }?.toSet() ?: setOf(raw.toString()))
            is String -> editor.putString(key, raw.toString())
            null -> when (tag) {
                "int" -> editor.putInt(key, toInt(raw))
                "long" -> editor.putLong(key, raw.toString().toLong())
                "float" -> editor.putFloat(key, raw.toString().toFloat())
                "boolean" -> editor.putBoolean(key, raw.toString().toBoolean())
                "set" -> editor.putStringSet(key, (raw as? Set<*>)?.map { it.toString() }?.toSet().orEmpty())
                else -> editor.putString(key, raw.toString())
            }
            else -> return@runCatching false
        }
        true
    }.getOrDefault(false)

    private fun toInt(raw: Any): Int =
        if (raw is Int) raw
        else raw.toString().toIntOrNull() ?: android.graphics.Color.parseColor(raw.toString())
}
