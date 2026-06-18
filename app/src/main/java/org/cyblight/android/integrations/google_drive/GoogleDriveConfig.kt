package org.cyblight.android.integrations.google_drive

import org.cyblight.android.BuildConfig

object GoogleDriveConfig {
    const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"
    const val DRIVE_METADATA_READONLY_SCOPE = "https://www.googleapis.com/auth/drive.metadata.readonly"
    const val DRIVE_READONLY_SCOPE = "https://www.googleapis.com/auth/drive.readonly"
    const val DRIVE_API_BASE = "https://www.googleapis.com/drive/v3"
    const val DRIVE_UPLOAD_BASE = "https://www.googleapis.com/upload/drive/v3"
    const val GOOGLE_STORAGE_MANAGEMENT_URL = "https://one.google.com/storage"

    val webClientId: String = BuildConfig.GOOGLE_DRIVE_WEB_CLIENT_ID.trim()
    val androidClientId: String = BuildConfig.GOOGLE_DRIVE_ANDROID_CLIENT_ID.trim()

    fun isConfigured(): Boolean = webClientId.isNotEmpty() && androidClientId.isNotEmpty()

    fun buildBackupFileName(login: String): String {
        val safeLogin = login.replace(Regex("[^\\w.-]+"), "_").ifBlank { "user" }
        return "cyblight-$safeLogin.cyblight-backup"
    }
}

data class DriveBackupFile(
    val id: String,
    val name: String,
    val modifiedTime: String,
    val size: String? = null,
)

data class DriveBackupMetadata(
    val file: DriveBackupFile,
)

data class GoogleDriveStorageQuota(
    val usageBytes: Long,
    val limitBytes: Long?,
)
