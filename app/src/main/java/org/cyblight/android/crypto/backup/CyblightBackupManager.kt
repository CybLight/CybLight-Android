package org.cyblight.android.crypto.backup

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.cyblight.android.crypto.DecryptCache
import org.cyblight.android.crypto.SignalStoreManifest
import org.cyblight.android.crypto.SignalStorePersistence

class CyblightBackupManager(context: Context) {
    private val persistence = SignalStorePersistence(context)
    private val decryptCache = DecryptCache(context)
    private val gson = Gson()

    fun collectPayload(userId: String): CyblightBackupPayload? {
        val manifest = persistence.readManifest(userId) ?: return null
        val manifestMap = gson.fromJson<Map<String, Any?>>(
            gson.toJson(manifest),
            object : TypeToken<Map<String, Any?>>() {}.type,
        )

        val preKeys = linkedMapOf<String, String>()
        val signedPreKeys = linkedMapOf<String, String>()
        val kyberPreKeys = linkedMapOf<String, String>()
        val sessions = linkedMapOf<String, String>()

        manifest.preKeyIds.forEach { keyId ->
            persistence.readPreKeyRecord(userId, keyId)?.let { preKeys[keyId.toString()] = it }
        }
        manifest.signedPreKeyIds.forEach { keyId ->
            persistence.readSignedPreKeyRecord(userId, keyId)?.let { signedPreKeys[keyId.toString()] = it }
        }
        manifest.kyberPreKeyIds.forEach { keyId ->
            persistence.readKyberPreKeyRecord(userId, keyId)?.let { kyberPreKeys[keyId.toString()] = it }
        }
        manifest.sessionKeys.forEach { sessionKey ->
            persistence.readSessionRecord(userId, sessionKey)?.let { sessions[sessionKey] = it }
        }

        return CyblightBackupPayload(
            userId = userId,
            createdAt = System.currentTimeMillis(),
            signal = CyblightBackupSignal(manifest = manifestMap),
            records = CyblightBackupRecords(
                preKeys = preKeys,
                signedPreKeys = signedPreKeys,
                kyberPreKeys = kyberPreKeys,
                sessions = sessions,
            ),
            decryptCache = decryptCache.readAllForUser(userId),
        )
    }

    fun restorePayload(expectedUserId: String, payload: CyblightBackupPayload) {
        if (payload.userId != expectedUserId) {
            throw IllegalArgumentException("backup_user_mismatch")
        }

        val manifest = gson.fromJson(
            gson.toJson(payload.signal.manifest),
            SignalStoreManifest::class.java,
        )
        if (manifest.registrationId <= 0 || manifest.identitySerialized.isBlank()) {
            throw IllegalArgumentException("backup_payload_invalid")
        }

        persistence.clearUser(expectedUserId)
        decryptCache.clearUser(expectedUserId)

        persistence.writeManifest(expectedUserId, manifest)
        payload.records.preKeys.forEach { (keyId, value) ->
            persistence.writePreKeyRecord(expectedUserId, keyId, value)
        }
        payload.records.signedPreKeys.forEach { (keyId, value) ->
            persistence.writeSignedPreKeyRecord(expectedUserId, keyId, value)
        }
        payload.records.kyberPreKeys.forEach { (keyId, value) ->
            persistence.writeKyberPreKeyRecord(expectedUserId, keyId, value)
        }
        payload.records.sessions.forEach { (sessionKey, value) ->
            persistence.writeSessionRecord(expectedUserId, sessionKey, value)
        }
        decryptCache.replaceAllForUser(expectedUserId, payload.decryptCache)
    }

    fun createBackupFile(userId: String, password: String): String {
        val payload = collectPayload(userId) ?: throw IllegalArgumentException("backup_no_local_keys")
        val file = BackupCrypto.encryptPayload(payload, password)
        return BackupCrypto.serializeFile(file)
    }

    fun importBackupFile(userId: String, rawFile: String, password: String) {
        val file = BackupCrypto.parseFile(rawFile)
        val payload = BackupCrypto.decryptPayload(file, password)
        restorePayload(userId, payload)
    }

    fun errorMessage(code: String): String = when (code) {
        "backup_no_local_keys" -> "Нет локальных ключей шифрования для резервной копии."
        "backup_password_invalid" -> "Неверный пароль резервной копии."
        "backup_user_mismatch" -> "Эта копия создана для другого аккаунта."
        "backup_file_invalid", "backup_payload_invalid", "backup_format_unsupported" ->
            "Некорректный файл резервной копии."
        else -> "Не удалось обработать резервную копию."
    }
}
