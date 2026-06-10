package org.cyblight.android.update

import android.content.Context
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.cyblight.android.BuildConfig
import java.io.File
import java.io.FileOutputStream

class UpdateRepository(
    private val context: Context,
    private val httpClient: OkHttpClient = OkHttpClient(),
) {
    private val gson = com.google.gson.Gson()
    private val updatesDir: File
        get() = File(context.cacheDir, "updates").also { it.mkdirs() }

    suspend fun checkForUpdate(): Result<AppUpdateInfo?> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(BuildConfig.GITHUB_RELEASES_URL)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "CybLight-Android/${BuildConfig.VERSION_NAME}")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("github_http_${response.code}")
                }

                val body = response.body?.string() ?: error("empty_response")
                val release = gson.fromJson(body, GitHubRelease::class.java)
                val tag = release.tagName ?: return@runCatching null
                val remoteVersion = VersionUtils.normalize(tag)
                val currentVersion = BuildConfig.VERSION_NAME

                if (!VersionUtils.isNewer(remoteVersion, currentVersion)) {
                    return@runCatching null
                }

                val apkAsset = release.assets
                    ?.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
                    ?: error("apk_asset_not_found")

                val downloadUrl = apkAsset.browserDownloadUrl ?: error("apk_url_missing")

                AppUpdateInfo(
                    versionName = remoteVersion,
                    releaseNotes = release.body?.trim().orEmpty(),
                    downloadUrl = downloadUrl,
                    assetName = apkAsset.name,
                )
            }
        }
    }

    suspend fun downloadApk(
        info: AppUpdateInfo,
        onProgress: (Float) -> Unit,
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val target = apkFileForVersion(info.versionName)
            if (target.exists() && target.length() > 0) {
                onProgress(1f)
                return@runCatching target
            }

            val request = Request.Builder()
                .url(info.downloadUrl)
                .header("Accept", "application/octet-stream")
                .header("User-Agent", "CybLight-Android/${BuildConfig.VERSION_NAME}")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("download_http_${response.code}")
                }

                val body = response.body ?: error("empty_body")
                val total = body.contentLength().coerceAtLeast(0L)
                val tempFile = File(updatesDir, "cyblight-update.tmp")
                body.byteStream().use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var downloaded = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            if (total > 0) {
                                onProgress((downloaded.toFloat() / total).coerceIn(0f, 1f))
                            }
                        }
                        output.flush()
                    }
                }

                if (target.exists()) target.delete()
                if (!tempFile.renameTo(target)) {
                    tempFile.copyTo(target, overwrite = true)
                    tempFile.delete()
                }
                onProgress(1f)
                target
            }
        }
    }

    fun apkFileForVersion(versionName: String): File =
        File(updatesDir, "cyblight-${VersionUtils.normalize(versionName)}.apk")

    fun hasDownloadedApk(versionName: String): Boolean {
        val file = apkFileForVersion(versionName)
        return file.exists() && file.length() > 0
    }

    private data class GitHubRelease(
        @SerializedName("tag_name") val tagName: String?,
        val body: String?,
        val assets: List<GitHubAsset>?,
    )

    private data class GitHubAsset(
        val name: String,
        @SerializedName("browser_download_url") val browserDownloadUrl: String?,
    )
}
