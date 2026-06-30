package org.cyblight.android.crypto.backup

import android.content.Context
import com.google.gson.Gson
import org.cyblight.android.crypto.DecryptCache
import org.cyblight.android.crypto.PlaintextSyncKey
import org.cyblight.android.crypto.SignalStoreManifest
import org.cyblight.android.crypto.SignalStorePersistence
import org.cyblight.android.data.api.ChatsExportPayload
import org.cyblight.android.data.api.createApiGson
import org.signal.libsignal.protocol.IdentityKeyPair

class CyblightBackupManager(context: Context) {
    private val persistence = SignalStorePersistence(context)
    private val decryptCache = DecryptCache(context)
    private val plaintextSyncKey = PlaintextSyncKey(context)
    private val gson = createApiGson()

    fun hasLocalBackupKeys(userId: String): Boolean = collectPayload(userId) != null

    fun collectPayload(
        userId: String,
        chats: ChatsExportPayload? = null,
    ): CyblightBackupPayload? {
        val manifest = persistence.readManifest(userId) ?: return null

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

        val base = CyblightBackupPayload(
            userId = userId,
            createdAt = System.currentTimeMillis(),
            signal = CyblightBackupSignal(manifest = manifest),
            records = CyblightBackupRecords(
                preKeys = preKeys,
                signedPreKeys = signedPreKeys,
                kyberPreKeys = kyberPreKeys,
                sessions = sessions,
            ),
            decryptCache = decryptCache.readAllForUser(userId),
            plaintextSyncKey = plaintextSyncKey.exportForBackup(userId),
        )

        return if (chats != null) {
            base.copy(version = BACKUP_PAYLOAD_VERSION_V2, chats = chats)
        } else {
            base.copy(version = BACKUP_PAYLOAD_VERSION_V1)
        }
    }

    fun restorePayload(
        expectedUserId: String,
        payload: CyblightBackupPayload,
        onProgress: (Int, String) -> Unit = { _, _ -> },
    ) {
        val payloadUserId = payload.userId?.trim()
        if (payloadUserId != null && payloadUserId.lowercase() != expectedUserId.trim().lowercase()) {
            throw IllegalArgumentException("backup_user_mismatch")
        }

        // Handle nested, flat-field, or root-level manifest structures
        onProgress(42, "progress_restore")
        val manifest = payload.manifest ?: payload.signal?.manifest ?: if (payload.identitySerialized.isNotBlank()) {
            SignalStoreManifest(
                registrationId = payload.registrationId,
                identitySerialized = payload.identitySerialized,
                preKeyIds = payload.preKeyIds,
                signedPreKeyIds = payload.signedPreKeyIds,
                kyberPreKeyIds = payload.kyberPreKeyIds,
                sessionKeys = payload.sessionKeys,
            )
        } else null

        if (manifest != null && !isValidManifest(manifest)) {
            throw IllegalArgumentException("backup_payload_invalid")
        }

        onProgress(45, "progress_restore")
        val records = payload.records ?: CyblightBackupRecords(
            preKeys = payload.preKeysFlat ?: emptyMap(),
            signedPreKeys = payload.signedPreKeysFlat ?: emptyMap(),
            kyberPreKeys = payload.kyberPreKeysFlat ?: emptyMap(),
            sessions = payload.sessionsFlat ?: emptyMap(),
        )
        val decryptEntries = payload.decryptCache ?: emptyMap()

        persistence.clearUser(expectedUserId)
        decryptCache.clearUser(expectedUserId)
        plaintextSyncKey.clearUser(expectedUserId)

        onProgress(48, "progress_restore")
        persistence.writeRecordsBatch(
            userId = expectedUserId,
            manifest = manifest,
            preKeys = records.preKeys,
            signedPreKeys = records.signedPreKeys,
            kyberPreKeys = records.kyberPreKeys,
            sessions = records.sessions,
        )

        onProgress(55, "progress_restore")
        if (decryptEntries.isNotEmpty()) {
            decryptCache.replaceAllForUser(expectedUserId, decryptEntries)
        }
        
        onProgress(58, "progress_restore")
        plaintextSyncKey.restoreFromBackup(expectedUserId, payload.plaintextSyncKey)
        
        // Help GC for large payloads
        payload.records = null
        payload.decryptCache = emptyMap()
    }

    fun createBackupFile(
        userId: String,
        password: String,
        chats: ChatsExportPayload? = null,
    ): String {
        val payload = collectPayload(userId, chats) ?: throw IllegalArgumentException("backup_no_local_keys")
        val file = BackupCrypto.encryptPayload(payload, password)
        return BackupCrypto.serializeFile(file)
    }

    fun decryptBackupPayload(rawFile: String, password: String): CyblightBackupPayload {
        val file = BackupCrypto.parseFile(rawFile)
        val payload = BackupCrypto.decryptPayload(file, password)

        // Post-process to ensure all data is correctly normalized regardless of source
        if (payload.manifest == null && payload.signal?.manifest == null && payload.identitySerialized.isNotBlank()) {
            payload.manifest = SignalStoreManifest(
                registrationId = payload.registrationId,
                identitySerialized = payload.identitySerialized,
                preKeyIds = payload.preKeyIds,
                signedPreKeyIds = payload.signedPreKeyIds,
                kyberPreKeyIds = payload.kyberPreKeyIds,
                sessionKeys = payload.sessionKeys,
            )
        }

        if (payload.records == null) {
            val preKeys = payload.preKeysFlat ?: emptyMap()
            val signedPreKeys = payload.signedPreKeysFlat ?: emptyMap()
            val kyberPreKeys = payload.kyberPreKeysFlat ?: emptyMap()
            val sessions = payload.sessionsFlat ?: emptyMap()

            if (preKeys.isNotEmpty() || signedPreKeys.isNotEmpty() || kyberPreKeys.isNotEmpty() || sessions.isNotEmpty()) {
                payload.records = CyblightBackupRecords(
                    preKeys = preKeys,
                    signedPreKeys = signedPreKeys,
                    kyberPreKeys = kyberPreKeys,
                    sessions = sessions,
                )
            }
        }

        return payload
    }

    fun importBackupFile(userId: String, rawFile: String, password: String): BackupRestoreStats {
        val payload = decryptBackupPayload(rawFile, password)
        restorePayload(userId, payload)
        return BackupRestoreStats()
    }

    private fun isValidManifest(manifest: SignalStoreManifest): Boolean {
        if (manifest.identitySerialized.isBlank()) return false
        return try {
            SignalStorePersistence.base64Decode(manifest.identitySerialized)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun errorMessage(code: String): String = when (code) {
        "backup_no_local_keys" -> "Нет локальных ключей шифрования для резервной копии."
        "backup_password_invalid" -> "Неверный пароль резервной копии."
        "backup_password_required" -> "Введите пароль резервной копии."
        "backup_password_short" -> "Пароль должен содержать не менее 8 символов."
        "backup_password_current_invalid" -> "Неверный текущий пароль."
        "backup_password_unchanged" -> "Новый пароль не может совпадать с текущим."
        "backup_user_missing" -> "Войдите в аккаунт, чтобы восстановить резервную копию."
        "backup_user_mismatch" -> "Эта копия создана для другого аккаунта."
        "backup_file_invalid", "backup_payload_invalid", "backup_format_unsupported", "backup_kdf_unsupported" ->
            "Некорректный файл резервной копии."
        "backup_share_failed", "backup_save_failed" -> "Не удалось сохранить резервную копию."
        "sync_key_invalid" -> "Некорректный ключ синхронизации в резервной копии."
        "chats_import_failed", "import_failed", "invalid_export_format" ->
            "Не удалось импортировать чаты из резервной копии."
        "signal_user_not_registered", "signal_store_missing" ->
            "Ошибка инициализации Signal в резервной копии."
        "signal_invalid_identity_key" -> "Некорректный ключ личности в резервной копии."
        "google_drive_not_configured" -> "Google Drive не настроен в приложении."
        "google_drive_auth_failed", "google_drive_auth_denied" -> "Не удалось войти через Google."
        "google_drive_no_backup" -> "В Google Drive нет резервной копии для этого аккаунта."
        "google_drive_upload_failed" -> "Не удалось сохранить резервную копию в Google Drive."
        "google_drive_list_failed" -> "Не удалось получить доступ к Google Drive."
        "google_drive_network_failed" -> "Нет подключения к Google Drive. Проверьте интернет."
        "google_drive_download_failed" -> "Не удалось скачать резервную копию из Google Drive."
        "google_drive_delete_failed" -> "Не удалось удалить резервную копию из Google Drive."
        "backup_failed" -> "Не удалось обработать резервную копию. Возможно, файл повреждён."
        else -> if (code.length > 50 || code.contains(" ")) code else "Ошибка: $code"
    }
}
