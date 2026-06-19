package org.cyblight.android.ui.messages

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString

data class ParsedChatMessage(
    val parts: List<ChatMessagePart>,
    val previewUrl: String?,
    val previewSuppressed: Boolean,
)

sealed interface ChatMessagePart {
    data class Text(val content: AnnotatedString) : ChatMessagePart
    data class CodeBlock(val language: String, val code: String) : ChatMessagePart
    data class Spoiler(val id: Int, val text: String) : ChatMessagePart
    data class Quote(val parts: List<ChatMessagePart>) : ChatMessagePart
}

private data class QuoteChunk(
    val quoted: Boolean,
    val text: String,
)

object ChatMessageParser {
    fun parseWithSpoilers(raw: String, linkColor: Color): ParsedChatMessage {
        val clean = ChatFormatUtils.stripMetadataTokens(raw)
        val previewUrl = ChatFormatUtils.extractFirstUrl(raw)
        val previewSuppressed = previewUrl?.let { ChatFormatUtils.isPreviewSuppressed(raw, it) } == true
        val parts = mutableListOf<ChatMessagePart>()
        var spoilerCounter = 0

        splitQuoteChunks(clean).forEach { chunk ->
            val segmentParts = parseSegment(chunk.text, linkColor, spoilerCounter)
            spoilerCounter = segmentParts.second
            if (chunk.quoted && segmentParts.first.isNotEmpty()) {
                parts += ChatMessagePart.Quote(segmentParts.first)
            } else {
                parts.addAll(segmentParts.first)
            }
        }

        if (parts.isEmpty()) {
            parts += ChatMessagePart.Text(buildInlineAnnotatedString(clean.ifBlank { " " }, linkColor))
        }

        return ParsedChatMessage(
            parts = parts,
            previewUrl = if (previewSuppressed) null else previewUrl,
            previewSuppressed = previewSuppressed,
        )
    }

    private fun splitQuoteChunks(text: String): List<QuoteChunk> {
        if (text.isEmpty()) return listOf(QuoteChunk(quoted = false, text = ""))

        val lines = text.split('\n')
        val chunks = mutableListOf<QuoteChunk>()
        var currentQuoted = false
        val currentLines = mutableListOf<String>()

        fun flush() {
            if (currentLines.isEmpty()) return
            chunks += QuoteChunk(
                quoted = currentQuoted,
                text = currentLines.joinToString("\n"),
            )
            currentLines.clear()
        }

        for (line in lines) {
            val quoted = line.startsWith("> ")
            val content = if (quoted) line.drop(2) else line
            if (currentLines.isNotEmpty() && quoted != currentQuoted) {
                flush()
            }
            currentQuoted = quoted
            currentLines += content
        }
        flush()
        return chunks
    }

    private fun parseSegment(
        text: String,
        linkColor: Color,
        spoilerStart: Int,
    ): Pair<List<ChatMessagePart>, Int> {
        val parts = mutableListOf<ChatMessagePart>()
        val codeRegex = Regex("""```(\w*)\n([\s\S]*?)\n```""")
        var lastIndex = 0
        var spoilerCounter = spoilerStart

        codeRegex.findAll(text).forEach { match ->
            if (match.range.first > lastIndex) {
                val segmentParts = splitSpoilers(
                    text.substring(lastIndex, match.range.first),
                    linkColor,
                    spoilerCounter,
                )
                parts.addAll(segmentParts.first)
                spoilerCounter = segmentParts.second
            }
            parts += ChatMessagePart.CodeBlock(match.groupValues[1], match.groupValues[2])
            lastIndex = match.range.last + 1
        }
        if (lastIndex < text.length) {
            val segmentParts = splitSpoilers(text.substring(lastIndex), linkColor, spoilerCounter)
            parts.addAll(segmentParts.first)
            spoilerCounter = segmentParts.second
        }
        if (parts.isEmpty() && text.isNotEmpty()) {
            parts += ChatMessagePart.Text(buildInlineAnnotatedString(text, linkColor))
        }
        return parts to spoilerCounter
    }

    private fun splitSpoilers(
        text: String,
        linkColor: Color,
        spoilerStart: Int,
    ): Pair<List<ChatMessagePart>, Int> {
        val parts = mutableListOf<ChatMessagePart>()
        val spoilerRegex = Regex("""\|\|([\s\S]+?)\|\|""")
        var last = 0
        var spoilerId = spoilerStart

        spoilerRegex.findAll(text).forEach { match ->
            if (match.range.first > last) {
                parts += ChatMessagePart.Text(
                    buildInlineAnnotatedString(text.substring(last, match.range.first), linkColor),
                )
            }
            parts += ChatMessagePart.Spoiler(spoilerId++, match.groupValues[1])
            last = match.range.last + 1
        }
        if (last < text.length) {
            parts += ChatMessagePart.Text(buildInlineAnnotatedString(text.substring(last), linkColor))
        }
        if (parts.isEmpty()) {
            parts += ChatMessagePart.Text(buildInlineAnnotatedString(text, linkColor))
        }
        return parts to spoilerId
    }

    private fun buildInlineAnnotatedString(text: String, linkColor: Color): AnnotatedString =
        ChatInlineMarkdown.parse(text, linkColor)
}
