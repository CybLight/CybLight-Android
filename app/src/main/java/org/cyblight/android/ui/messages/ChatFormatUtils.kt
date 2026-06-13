package org.cyblight.android.ui.messages

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class ReplyMeta(
    val messageId: String,
    val author: String,
    val text: String,
)

object ChatFormatUtils {
    private val urlRegex = Regex("""https?://[^\s<]+""", RegexOption.IGNORE_CASE)
    private val noPreviewRegex = Regex("""\[\[CYBLIGHT_NO_PREVIEW:([^\]]+)]]""")
    private val replyRegex = Regex("""\n?\[\[CYBLIGHT_REPLY:[^\]]+]]""")
    private val replyMetaRegex = Regex("""\[\[CYBLIGHT_REPLY:([^:\]]+):([^:\]]*):([^\]]*)]]""")
    private val replyMetaLegacyRegex = Regex("""\[\[CYBLIGHT_REPLY:([^:\]]+):([^\]]*)]]""")

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
            decodeToken(encoded)
        }.toList()
    }

    fun isPreviewSuppressed(text: String, url: String): Boolean {
        val target = normalizeUrl(url)
        return extractNoPreviewUrls(text).any { normalizeUrl(it) == target }
    }

    fun appendReplyToken(content: String, messageId: String, author: String, preview: String): String {
        val safeAuthor = encodeToken(author)
        val safePreview = encodeToken(
            preview.replace(Regex("""\s+"""), " ").trim().take(220),
        )
        return "$content\n[[CYBLIGHT_REPLY:$messageId:$safeAuthor:$safePreview]]"
    }

    fun appendNoPreviewToken(content: String, url: String): String {
        val encoded = encodeToken(url)
        return "$content\n[[CYBLIGHT_NO_PREVIEW:$encoded]]"
    }

    fun extractReplyMeta(text: String): ReplyMeta? {
        replyMetaRegex.find(text)?.let { match ->
            val messageId = match.groupValues[1].trim()
            if (messageId.isNotEmpty()) {
                return ReplyMeta(
                    messageId = messageId,
                    author = decodeToken(match.groupValues[2]).ifBlank { "Собеседник" },
                    text = decodeToken(match.groupValues[3]),
                )
            }
        }
        replyMetaLegacyRegex.find(text)?.let { match ->
            val messageId = match.groupValues[1].trim()
            if (messageId.isNotEmpty()) {
                return ReplyMeta(
                    messageId = messageId,
                    author = "Собеседник",
                    text = decodeToken(match.groupValues[2]),
                )
            }
        }
        return null
    }

    private fun encodeToken(value: String): String {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.name())
            .replace("+", "%20")
    }

    private fun decodeToken(value: String): String {
        if (value.isBlank()) return ""
        return runCatching {
            java.net.URLDecoder.decode(value.replace("+", "%20"), StandardCharsets.UTF_8.name())
        }.getOrDefault(value.replace("+", " "))
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
