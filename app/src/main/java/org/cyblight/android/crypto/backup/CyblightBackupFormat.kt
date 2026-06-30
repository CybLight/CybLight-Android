package org.cyblight.android.crypto.backup

import com.google.gson.annotations.SerializedName
import org.cyblight.android.crypto.SignalStoreManifest
import org.cyblight.android.data.api.ChatsExportPayload

const val BACKUP_FILE_FORMAT = "cyblight-backup"
const val BACKUP_PAYLOAD_FORMAT = "cyblight-backup-payload"
const val BACKUP_PAYLOAD_VERSION_V1 = 1
const val BACKUP_PAYLOAD_VERSION_V2 = 2
const val BACKUP_VERSION = 1
const val BACKUP_KDF = "pbkdf2-sha256"
const val BACKUP_ITERATIONS = 310_000
const val BACKUP_FILE_EXTENSION = ".cyblight-backup"

data class CyblightBackupRecords(
    @SerializedName("pre_keys", alternate = ["preKeys"])
    val preKeys: Map<String, String> = emptyMap(),
    @SerializedName("signed_pre_keys", alternate = ["signedPreKeys"])
    val signedPreKeys: Map<String, String> = emptyMap(),
    @SerializedName("kyber_pre_keys", alternate = ["kyberPreKeys"])
    val kyberPreKeys: Map<String, String> = emptyMap(),
    @SerializedName("sessions")
    val sessions: Map<String, String> = emptyMap(),
)

data class CyblightBackupSignal(
    @SerializedName("manifest")
    val manifest: SignalStoreManifest? = null,
)

data class CyblightBackupPayload(
    val format: String = BACKUP_PAYLOAD_FORMAT,
    val version: Int = BACKUP_PAYLOAD_VERSION_V1,
    @SerializedName("userId", alternate = ["user_id", "owner_user_id", "ownerUserId"])
    val userId: String?,
    @SerializedName("createdAt", alternate = ["created_at"])
    val createdAt: Long = 0L,
    @SerializedName("signal")
    var signal: CyblightBackupSignal? = null,
    @SerializedName("manifest", alternate = ["signal_store", "signalStore", "signal_manifest", "signalManifest"])
    var manifest: SignalStoreManifest? = null,
    @SerializedName("records", alternate = ["signal_records", "signalRecords"])
    var records: CyblightBackupRecords? = null,
    @SerializedName("decryptCache", alternate = ["decrypt_cache"])
    var decryptCache: Map<String, String> = emptyMap(),
    @SerializedName("plaintextSyncKey", alternate = ["plaintext_sync_key"])
    val plaintextSyncKey: String? = null,
    var chats: ChatsExportPayload? = null,

    // Flattened manifest fields for browser backups
    @SerializedName("registrationId", alternate = ["registration_id"])
    val registrationId: Int = 0,
    @SerializedName("identitySerialized", alternate = ["identity_serialized", "identityKey", "identity_key"])
    val identitySerialized: String = "",
    @SerializedName("preKeyIds", alternate = ["pre_key_ids"])
    val preKeyIds: List<Int> = emptyList(),
    @SerializedName("signedPreKeyIds", alternate = ["signed_pre_key_ids"])
    val signedPreKeyIds: List<Int> = emptyList(),
    @SerializedName("kyberPreKeyIds", alternate = ["kyber_pre_key_ids"])
    val kyberPreKeyIds: List<Int> = emptyList(),
    @SerializedName("sessionKeys", alternate = ["session_keys"])
    val sessionKeys: List<String> = emptyList(),

    // Flattened records fields for browser backups
    @SerializedName("preKeys", alternate = ["pre_keys"])
    val preKeysFlat: Map<String, String>? = null,
    @SerializedName("signedPreKeys", alternate = ["signed_pre_keys"])
    val signedPreKeysFlat: Map<String, String>? = null,
    @SerializedName("kyberPreKeys", alternate = ["kyber_pre_keys"])
    val kyberPreKeysFlat: Map<String, String>? = null,
    @SerializedName("sessions")
    val sessionsFlat: Map<String, String>? = null,
)

data class BackupRestoreStats(
    val chatsImported: Int = 0,
    val chatsSkipped: Int = 0,
    val chatsErrors: Int = 0,
)

data class CyblightBackupFile(
    val format: String = BACKUP_FILE_FORMAT,
    val version: Int = BACKUP_VERSION,
    val kdf: String = BACKUP_KDF,
    val iterations: Int = BACKUP_ITERATIONS,
    @SerializedName("salt", alternate = ["saltBase64"])
    val saltBase64: String,
    @SerializedName("iv", alternate = ["ivBase64"])
    val ivBase64: String,
    val ciphertext: String,
)
