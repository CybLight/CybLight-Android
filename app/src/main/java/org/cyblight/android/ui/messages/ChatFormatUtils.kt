package org.cyblight.android.ui.messages

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object ChatFormatUtils {
    private val urlRegex = Regex("""https?://[^\s<]+""", RegexOption.IGNORE_CASE)
    private val noPreviewRegex = Regex("""\[\[CYBLIGHT_NO_PREVIEW:([^\]]+)]]""")
    private val replyRegex = Regex("""\n?\[\[CYBLIGHT_REPLY:[^\]]+]]""")

    fun stripMetadataTokens(text: String): String {
        return text
            .replace(noPreviewRegex, "")
            .replace(replyRegex, "")
            .trimEnd()
    }

    fun extractFirstUrl(text: String): String? {
        val match = urlRegex.find(text) ?: return null
        return match.value.trimEnd(',', '.', ')', '!', '?')
    }

    fun extractNoPreviewUrls(text: String): List<String> {
        return noPreviewRegex.findAll(text).mapNotNull { match ->
            val encoded = match.groupValues.getOrNull(1)?.trim().orEmpty()
            if (encoded.isEmpty()) return@mapNotNull null
            runCatching { java.net.URLDecoder.decode(encoded, StandardCharsets.UTF_8.name()) }
                .getOrDefault(encoded)
        }.toList()
    }

    fun isPreviewSuppressed(text: String, url: String): Boolean {
        val target = normalizeUrl(url)
        return extractNoPreviewUrls(text).any { normalizeUrl(it) == target }
    }

    fun appendNoPreviewToken(content: String, url: String): String {
        val encoded = URLEncoder.encode(url, StandardCharsets.UTF_8.name())
        return "$content\n[[CYBLIGHT_NO_PREVIEW:$encoded]]"
    }

    private fun normalizeUrl(value: String): String {
        return runCatching {
            val uri = java.net.URI(value)
            val builder = StringBuilder()
            builder.append(uri.scheme ?: "https")
            builder.append("://")
            builder.append(uri.host ?: "")
            if (uri.port != -1) builder.append(':').append(uri.port)
            builder.append(uri.path ?: "")
            if (!uri.query.isNullOrBlank()) builder.append('?').append(uri.query)
            builder.toString()
        }.getOrDefault(value.trim())
    }
}
