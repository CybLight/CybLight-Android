package org.cyblight.android.data.repository

import android.text.Html
import com.google.gson.Gson
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

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

object LinkPreviewRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .writeTimeout(8, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
    private val gson = Gson()
    private val cache = ConcurrentHashMap<String, LinkPreviewData>()
    private val failedUrls = ConcurrentHashMap.newKeySet<String>()
    private val inFlight = ConcurrentHashMap<String, CompletableDeferred<LinkPreviewData?>>()

    suspend fun fetch(url: String): LinkPreviewData? = withContext(Dispatchers.IO) {
        cache[url]?.let { return@withContext it }
        if (failedUrls.contains(url)) return@withContext null

        val pending = inFlight[url]
        if (pending != null) return@withContext pending.await()

        val deferred = CompletableDeferred<LinkPreviewData?>()
        val existing = inFlight.putIfAbsent(url, deferred)
        if (existing != null) return@withContext existing.await()

        try {
            val preview = fetchMicrolink(url) ?: fetchOpenGraph(url)
            if (preview != null) {
                cache[url] = preview
            } else {
                failedUrls.add(url)
            }
            deferred.complete(preview)
            preview
        } catch (e: Exception) {
            failedUrls.add(url)
            deferred.complete(null)
            null
        } finally {
            inFlight.remove(url)
        }
    }

    private fun fetchMicrolink(url: String): LinkPreviewData? {
        val endpoint =
            "https://api.microlink.io/?url=${java.net.URLEncoder.encode(url, Charsets.UTF_8.name())}&meta=true&screenshot=false"
        val request = Request.Builder().url(endpoint).get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string().orEmpty()
            val payload = gson.fromJson(body, MicrolinkResponse::class.java)
            if (payload.status != "success" || payload.data == null) return null
            val data = payload.data
            return LinkPreviewData(
                url = data.url?.trim().orEmpty().ifBlank { url },
                title = data.title?.trim().orEmpty(),
                description = data.description?.trim().orEmpty(),
                imageUrl = data.image?.url?.trim()?.ifBlank { null }
                    ?: data.logo?.url?.trim()?.ifBlank { null },
                siteName = data.publisher?.trim().orEmpty().ifBlank {
                    data.author?.trim().orEmpty()
                },
            )
        }
    }

    private fun fetchOpenGraph(url: String): LinkPreviewData? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (compatible; CybLight/1.0; +https://cyblight.org)")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val html = response.body?.string()?.take(512_000).orEmpty()
            if (html.isBlank()) return null

            val title = extractMetaContent(html, "og:title")
                ?: extractMetaContent(html, "twitter:title")
                ?: extractTitleTag(html)
            val description = extractMetaContent(html, "og:description")
                ?: extractMetaContent(html, "twitter:description")
            val imageUrl = extractMetaContent(html, "og:image")
                ?: extractMetaContent(html, "twitter:image")
            val siteName = extractMetaContent(html, "og:site_name")

            if (title.isNullOrBlank() && description.isNullOrBlank() && imageUrl.isNullOrBlank()) {
                return null
            }

            return LinkPreviewData(
                url = url,
                title = decodeHtmlEntities(title.orEmpty()),
                description = decodeHtmlEntities(description.orEmpty()),
                imageUrl = imageUrl?.trim()?.ifBlank { null },
                siteName = decodeHtmlEntities(siteName.orEmpty()),
            )
        }
    }

    private fun extractMetaContent(html: String, property: String): String? {
        val escaped = Regex.escape(property)
        val patterns = listOf(
            """<meta\s+[^>]*property\s*=\s*["']$escaped["'][^>]*content\s*=\s*["']([^"']*)["']""".toRegex(RegexOption.IGNORE_CASE),
            """<meta\s+[^>]*content\s*=\s*["']([^"']*)["'][^>]*property\s*=\s*["']$escaped["']""".toRegex(RegexOption.IGNORE_CASE),
            """<meta\s+[^>]*name\s*=\s*["']$escaped["'][^>]*content\s*=\s*["']([^"']*)["']""".toRegex(RegexOption.IGNORE_CASE),
            """<meta\s+[^>]*content\s*=\s*["']([^"']*)["'][^>]*name\s*=\s*["']$escaped["']""".toRegex(RegexOption.IGNORE_CASE),
        )
        for (pattern in patterns) {
            val match = pattern.find(html)?.groupValues?.getOrNull(1)?.trim()
            if (!match.isNullOrBlank()) return match
        }
        return null
    }

    private fun extractTitleTag(html: String): String? {
        return """<title[^>]*>([^<]*)</title>""".toRegex(RegexOption.IGNORE_CASE)
            .find(html)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?.ifBlank { null }
    }

    private fun decodeHtmlEntities(value: String): String {
        if (value.isBlank()) return value
        return Html.fromHtml(value, Html.FROM_HTML_MODE_LEGACY).toString().trim()
    }
}
