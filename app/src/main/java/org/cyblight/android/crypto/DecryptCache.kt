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
    }

    private fun cacheKey(userId: String, messageId: String): String =
        "decrypt_${userId}_$messageId"

    companion object {
        private const val PREFS_NAME = "cyblight_signal_decrypt_cache"
    }
}
