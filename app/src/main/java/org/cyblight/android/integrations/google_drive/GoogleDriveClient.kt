package org.cyblight.android.integrations.google_drive

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private data class DriveListResponse(
    val files: List<DriveBackupFileDto>? = null,
)

private data class DriveBackupFileDto(
    val id: String = "",
    val name: String = "",
    @SerializedName("modifiedTime") val modifiedTime: String = "",
    val size: String? = null,
)

private data class DriveAboutResponse(
    @SerializedName("storageQuota") val storageQuota: DriveStorageQuotaDto? = null,
)

private data class DriveStorageQuotaDto(
    val limit: String? = null,
    val usage: String? = null,
)

class GoogleDriveClient(
    private val httpClient: OkHttpClient = OkHttpClient(),
    private val gson: Gson = Gson(),
) {
    suspend fun fetchStorageQuota(accessToken: String): GoogleDriveStorageQuota? {
        val fields = URLEncoder.encode("storageQuota(limit,usage)", StandardCharsets.UTF_8.name())
        val url = "${GoogleDriveConfig.DRIVE_API_BASE}/about?fields=$fields"
        val request = authorizedRequest(accessToken, url).build()
        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) return null
        val body = response.body?.string().orEmpty()
        val quota = gson.fromJson(body, DriveAboutResponse::class.java).storageQuota ?: return null
        val usageBytes = quota.usage?.toLongOrNull() ?: return null
        val limitRaw = quota.limit?.toLongOrNull()
        val limitBytes = limitRaw?.takeIf { it > 0L }
        return GoogleDriveStorageQuota(usageBytes = usageBytes, limitBytes = limitBytes)
    }

    suspend fun findBackupFile(accessToken: String, userId: String): DriveBackupFile? =
        listBackupFiles(accessToken, userId).firstOrNull()

    suspend fun listBackupFiles(accessToken: String, userId: String): List<DriveBackupFile> {
        val query = listOf(
            "appProperties has { key='cyblightBackup' and value='1' }",
            "appProperties has { key='cyblightUserId' and value='${escapeQueryValue(userId)}' }",
            "trashed = false",
        ).joinToString(" and ")

        val url = buildString {
            append(GoogleDriveConfig.DRIVE_API_BASE)
            append("/files?q=")
            append(URLEncoder.encode(query, StandardCharsets.UTF_8.name()))
            append("&fields=")
            append(URLEncoder.encode("files(id,name,modifiedTime,size)", StandardCharsets.UTF_8.name()))
            append("&orderBy=modifiedTime desc&pageSize=50")
        }

        val request = authorizedRequest(accessToken, url).build()
        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) throw IllegalStateException("google_drive_list_failed")
        val body = response.body?.string().orEmpty()
        val data = gson.fromJson(body, DriveListResponse::class.java)
        return data.files.orEmpty().map { it.toModel() }
    }

    suspend fun downloadBackupFile(accessToken: String, fileId: String): String {
        val url = "${GoogleDriveConfig.DRIVE_API_BASE}/files/${encodePath(fileId)}?alt=media"
        val request = authorizedRequest(accessToken, url).build()
        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) throw IllegalStateException("google_drive_download_failed")
        return response.body?.string().orEmpty()
    }

    suspend fun uploadBackupFile(
        accessToken: String,
        userId: String,
        login: String,
        content: String,
    ): DriveBackupFile {
        val existing = findBackupFile(accessToken, userId)
        val mediaType = "application/json".toMediaType()

        if (existing != null) {
            val url = "${GoogleDriveConfig.DRIVE_UPLOAD_BASE}/files/${encodePath(existing.id)}?uploadType=media"
            val request = authorizedRequest(accessToken, url)
                .patch(content.toRequestBody(mediaType))
                .build()
        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IllegalStateException("google_drive_upload_failed")
        }
        val updated = gson.fromJson(response.body?.string().orEmpty(), DriveBackupFileDto::class.java)
            return DriveBackupFile(
                id = updated.id.ifBlank { existing.id },
                name = updated.name.ifBlank { existing.name },
                modifiedTime = updated.modifiedTime.ifBlank { existing.modifiedTime },
                size = updated.size ?: existing.size,
            )
        }

        val boundary = "cyblight_backup_boundary"
        val metadata = mapOf(
            "name" to GoogleDriveConfig.buildBackupFileName(login),
            "mimeType" to "application/json",
            "appProperties" to mapOf(
                "cyblightBackup" to "1",
                "cyblightUserId" to userId,
            ),
        )
        val body = buildString {
            append("--").append(boundary).append("\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            append(gson.toJson(metadata)).append("\r\n")
            append("--").append(boundary).append("\r\n")
            append("Content-Type: application/json\r\n\r\n")
            append(content).append("\r\n")
            append("--").append(boundary).append("--\r\n")
        }

        val url = "${GoogleDriveConfig.DRIVE_UPLOAD_BASE}/files?uploadType=multipart"
        val request = authorizedRequest(accessToken, url)
            .post(
                body.toRequestBody("multipart/related; boundary=$boundary".toMediaType()),
            )
            .build()
        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) throw IllegalStateException("google_drive_upload_failed")
        val created = gson.fromJson(response.body?.string().orEmpty(), DriveBackupFileDto::class.java)
        if (created.id.isBlank()) throw IllegalStateException("google_drive_upload_failed")
        return created.toModel()
    }

    suspend fun deleteBackupFile(accessToken: String, userId: String): Boolean {
        val files = listBackupFiles(accessToken, userId)
        if (files.isEmpty()) return false
        files.forEach { file ->
            val url = "${GoogleDriveConfig.DRIVE_API_BASE}/files/${encodePath(file.id)}"
            val request = authorizedRequest(accessToken, url).delete().build()
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful && response.code != 404) {
                throw IllegalStateException("google_drive_delete_failed")
            }
        }
        return true
    }

    private fun authorizedRequest(accessToken: String, url: String): Request.Builder =
        Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")

    private fun DriveBackupFileDto.toModel(): DriveBackupFile =
        DriveBackupFile(id = id, name = name, modifiedTime = modifiedTime, size = size)

    private fun escapeQueryValue(value: String): String =
        value.replace("\\", "\\\\").replace("'", "\\'")

    private fun encodePath(value: String): String = value
}
