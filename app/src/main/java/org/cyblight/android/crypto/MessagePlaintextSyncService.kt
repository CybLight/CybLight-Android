package org.cyblight.android.crypto

import android.content.Context
import android.util.Base64
import org.cyblight.android.data.api.CybLightApi
import org.cyblight.android.data.api.FetchPlaintextSyncRequest
import org.cyblight.android.data.api.PlaintextSyncEntryDto
import org.cyblight.android.data.api.PushPlaintextSyncRequest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class MessagePlaintextSyncService(
    context: Context,
    private val api: CybLightApi,
) {
    private val syncKey = PlaintextSyncKey(context)
    private val secureRandom = SecureRandom()

    suspend fun push(userId: String, messageId: String, plaintext: String) {
        if (messageId.isBlank() || plaintext.isEmpty()) return
        val encrypted = encrypt(userId, plaintext) ?: return
        runCatching {
            api.pushPlaintextSync(
                PushPlaintextSyncRequest(
                    entries = listOf(
                        PlaintextSyncEntryDto(
                            messageId = messageId,
                            iv = encrypted.first,
                            ciphertext = encrypted.second,
                        ),
                    ),
                ),
            )
        }
    }

    suspend fun fetchBatch(userId: String, messageIds: List<String>): Map<String, String> {
        val key = syncKey.read(userId) ?: return emptyMap()
        val ids = messageIds.filter { it.isNotBlank() }.distinct().take(MAX_BATCH)
        if (ids.isEmpty()) return emptyMap()

        val response = runCatching {
            api.fetchPlaintextSync(FetchPlaintextSyncRequest(messageIds = ids))
        }.getOrNull() ?: return emptyMap()
        if (!response.ok) return emptyMap()

        val out = linkedMapOf<String, String>()
        response.entries.forEach { entry ->
            val text = decrypt(key, entry.iv, entry.ciphertext) ?: return@forEach
            out[entry.messageId] = text
        }
        return out
    }

    fun restoreSyncKeyFromBackup(userId: String, backupKeyBase64: String?) {
        syncKey.restoreFromBackup(userId, backupKeyBase64)
    }

    fun exportSyncKeyForBackup(userId: String): String? = syncKey.exportForBackup(userId)

    private fun encrypt(userId: String, plaintext: String): Pair<String, String>? {
        val key = syncKey.getOrCreate(userId)
        val iv = ByteArray(12).also(secureRandom::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv, Base64.NO_WRAP) to Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    private fun decrypt(key: ByteArray, ivBase64: String, ciphertextBase64: String): String? {
        return runCatching {
            val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
            val ciphertext = Base64.decode(ciphertextBase64, Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        }.getOrNull()
    }

    companion object {
        private const val MAX_BATCH = 200
    }
}
