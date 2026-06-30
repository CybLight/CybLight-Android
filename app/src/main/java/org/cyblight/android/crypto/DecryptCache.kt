package org.cyblight.android.crypto

import android.content.Context

class DecryptCache(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun read(userId: String, messageId: String): String? {
        if (messageId.isBlank()) return null
        return prefs.getString(cacheKey(userId, messageId), null)
    }

    fun write(userId: String, messageId: String, plaintext: String) {
        if (messageId.isBlank()) return
        prefs.edit()
            .putString(cacheKey(userId, messageId), plaintext)
            .apply()
        trimIfNeeded(userId)
    }

    fun writeBatch(userId: String, entries: Map<String, String>) {
        if (entries.isEmpty()) return
        val editor = prefs.edit()
        entries.forEach { (id, text) ->
            if (id.isNotBlank()) {
                editor.putString(cacheKey(userId, id), text)
            }
        }
        editor.apply()
        trimIfNeeded(userId)
    }

    fun remove(userId: String, messageId: String) {
        if (messageId.isBlank()) return
        prefs.edit()
            .remove(cacheKey(userId, messageId))
            .apply()
    }

    fun readAllForUser(userId: String): Map<String, String> {
        val prefix = "decrypt_${userId}_"
        val out = linkedMapOf<String, String>()
        prefs.all.forEach { (key, value) ->
            if (key.startsWith(prefix) && value is String) {
                out[key.removePrefix(prefix)] = value
            }
        }
        return out
    }

    fun clearUser(userId: String) {
        val prefix = "decrypt_${userId}_"
        val editor = prefs.edit()
        prefs.all.keys.filter { it.startsWith(prefix) }.forEach(editor::remove)
        editor.apply()
    }

    fun replaceAllForUser(userId: String, entries: Map<String, String>) {
        clearUser(userId)
        if (entries.isEmpty()) return
        val editor = prefs.edit()
        entries.forEach { (messageId, plaintext) ->
            if (messageId.isNotBlank()) {
                editor.putString(cacheKey(userId, messageId), plaintext)
            }
        }
        editor.apply()
    }

    private fun cacheKey(userId: String, messageId: String): String =
        "decrypt_${userId}_$messageId"

    private fun trimIfNeeded(userId: String) {
        val prefix = "decrypt_${userId}_"
        val keys = prefs.all.keys.filter { it.startsWith(prefix) }
        if (keys.size <= MAX_ENTRIES_PER_USER) return
        val dropCount = keys.size - MAX_ENTRIES_PER_USER
        val editor = prefs.edit()
        keys.take(dropCount).forEach(editor::remove)
        editor.apply()
    }

    companion object {
        private const val PREFS_NAME = "cyblight_signal_decrypt_cache"
        private const val MAX_ENTRIES_PER_USER = 5_000
    }
}
