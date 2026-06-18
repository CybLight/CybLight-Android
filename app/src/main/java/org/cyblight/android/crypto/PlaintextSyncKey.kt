package org.cyblight.android.crypto

import android.content.Context
import android.util.Base64
import java.security.SecureRandom

class PlaintextSyncKey(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val secureRandom = SecureRandom()

    fun read(userId: String): ByteArray? {
        val encoded = prefs.getString(keyFor(userId), null) ?: return null
        return runCatching {
            val bytes = Base64.decode(encoded, Base64.NO_WRAP)
            if (bytes.size == KEY_BYTES) bytes else null
        }.getOrNull()
    }

    fun write(userId: String, key: ByteArray) {
        if (key.size != KEY_BYTES) {
            throw IllegalArgumentException("sync_key_invalid")
        }
        prefs.edit()
            .putString(keyFor(userId), Base64.encodeToString(key, Base64.NO_WRAP))
            .commit()
    }

    fun getOrCreate(userId: String): ByteArray {
        read(userId)?.let { return it }
        val key = ByteArray(KEY_BYTES).also(secureRandom::nextBytes)
        write(userId, key)
        return key
    }

    fun restoreFromBackup(userId: String, backupKeyBase64: String?) {
        val trimmed = backupKeyBase64?.trim().orEmpty()
        if (trimmed.isEmpty()) return
        val key = Base64.decode(trimmed, Base64.NO_WRAP)
        if (key.size != KEY_BYTES) {
            throw IllegalArgumentException("sync_key_invalid")
        }
        write(userId, key)
    }

    fun exportForBackup(userId: String): String? {
        val key = read(userId) ?: return null
        return Base64.encodeToString(key, Base64.NO_WRAP)
    }

    fun clearUser(userId: String) {
        prefs.edit().remove(keyFor(userId)).commit()
    }

    private fun keyFor(userId: String): String = "sync_key_$userId"

    companion object {
        private const val PREFS_NAME = "cyblight_plaintext_sync_key"
        private const val KEY_BYTES = 32
    }
}
