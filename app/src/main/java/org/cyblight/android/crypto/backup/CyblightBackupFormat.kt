package org.cyblight.android.crypto.backup

import com.google.gson.annotations.SerializedName

const val BACKUP_FILE_FORMAT = "cyblight-backup"
const val BACKUP_PAYLOAD_FORMAT = "cyblight-backup-payload"
const val BACKUP_VERSION = 1
const val BACKUP_KDF = "pbkdf2-sha256"
const val BACKUP_ITERATIONS = 310_000
const val BACKUP_FILE_EXTENSION = ".cyblight-backup"

data class CyblightBackupRecords(
    val preKeys: Map<String, String> = emptyMap(),
    val signedPreKeys: Map<String, String> = emptyMap(),
    val kyberPreKeys: Map<String, String> = emptyMap(),
    val sessions: Map<String, String> = emptyMap(),
)

data class CyblightBackupSignal(
    val manifest: Map<String, Any?>,
)

data class CyblightBackupPayload(
    val format: String = BACKUP_PAYLOAD_FORMAT,
    val version: Int = BACKUP_VERSION,
    val userId: String,
    val createdAt: Long,
    val signal: CyblightBackupSignal,
    val records: CyblightBackupRecords,
    val decryptCache: Map<String, String> = emptyMap(),
)

data class CyblightBackupFile(
    val format: String = BACKUP_FILE_FORMAT,
    val version: Int = BACKUP_VERSION,
    val kdf: String = BACKUP_KDF,
    val iterations: Int = BACKUP_ITERATIONS,
    @SerializedName("salt") val saltBase64: String,
    @SerializedName("iv") val ivBase64: String,
    val ciphertext: String,
)
