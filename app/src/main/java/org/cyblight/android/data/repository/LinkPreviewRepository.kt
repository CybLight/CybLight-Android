package org.cyblight.android.data.repository

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap

data class LinkPreviewData(
    val url: String,
    val title: String,
    val description: String,
    val imageUrl: String?,
    val siteName: String,
)

private data class MicrolinkResponse(
    val status: String? = null,
    val data: MicrolinkData? = null,
)

private data class MicrolinkData(
    val url: String? = null,
    val title: String? = null,
    val description: String? = null,
    val publisher: String? = null,
    val author: String? = null,
    val image: MicrolinkImage? = null,
    val logo: MicrolinkImage? = null,
)

private data class MicrolinkImage(
    val url: String? = null,
)

class LinkPreviewRepository {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val cache = ConcurrentHashMap<String, LinkPreviewData?>()
    suspend fun fetch(url: String): LinkPreviewData? = withContext(Dispatchers.IO) {
        cache[url]?.let { return@withContext it }

        val endpoint =
            "https://api.microlink.io/?url=${java.net.URLEncoder.encode(url, Charsets.UTF_8.name())}&meta=true&screenshot=false"
        runCatching {
            val request = Request.Builder().url(endpoint).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    cache[url] = null
                    return@withContext null
                }
                val body = response.body?.string().orEmpty()
                val payload = gson.fromJson(body, MicrolinkResponse::class.java)
                if (payload.status != "success" || payload.data == null) {
                    cache[url] = null
                    return@withContext null
                }
                val data = payload.data
                val preview = LinkPreviewData(
                    url = data.url?.trim().orEmpty().ifBlank { url },
                    title = data.title?.trim().orEmpty(),
                    description = data.description?.trim().orEmpty(),
                    imageUrl = data.image?.url?.trim()?.ifBlank { null }
                        ?: data.logo?.url?.trim()?.ifBlank { null },
                    siteName = data.publisher?.trim().orEmpty().ifBlank {
                        data.author?.trim().orEmpty()
                    },
                )
                cache[url] = preview
                preview
            }
        }.getOrElse {
            cache[url] = null
            null
        }
    }
}
