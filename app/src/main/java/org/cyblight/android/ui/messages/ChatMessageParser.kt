package org.cyblight.android.ui.messages

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle

data class ParsedChatMessage(
    val parts: List<ChatMessagePart>,
    val previewUrl: String?,
    val previewSuppressed: Boolean,
)

sealed interface ChatMessagePart {
    data class Text(val content: AnnotatedString) : ChatMessagePart
    data class CodeBlock(val language: String, val code: String) : ChatMessagePart
    data class Spoiler(val id: Int, val text: String) : ChatMessagePart
}

object ChatMessageParser {
    fun parseWithSpoilers(raw: String, linkColor: Color): ParsedChatMessage {
        val clean = ChatFormatUtils.stripMetadataTokens(raw)
        val previewUrl = ChatFormatUtils.extractFirstUrl(raw)
        val previewSuppressed = previewUrl?.let { ChatFormatUtils.isPreviewSuppressed(raw, it) } == true
        val parts = mutableListOf<ChatMessagePart>()
        val codeRegex = Regex("""```(\w*)\n([\s\S]*?)\n```""")
        var lastIndex = 0
        var spoilerCounter = 0

        codeRegex.findAll(clean).forEach { match ->
            if (match.range.first > lastIndex) {
                val segmentParts = splitSpoilers(
                    clean.substring(lastIndex, match.range.first),
                    linkColor,
                    spoilerCounter,
                )
                parts.addAll(segmentParts.first)
                spoilerCounter = segmentParts.second
            }
            parts += ChatMessagePart.CodeBlock(match.groupValues[1], match.groupValues[2])
            lastIndex = match.range.last + 1
        }
        if (lastIndex < clean.length) {
            val segmentParts = splitSpoilers(clean.substring(lastIndex), linkColor, spoilerCounter)
            parts.addAll(segmentParts.first)
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

    private fun splitSpoilers(
        text: String,
        linkColor: Color,
        spoilerStart: Int,
    ): Pair<List<ChatMessagePart>, Int> {
        val parts = mutableListOf<ChatMessagePart>()
        val spoilerRegex = Regex("""\|\|([^|]+)\|\|""")
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

    private sealed interface InlinePart {
        data class Plain(val text: String) : InlinePart
        data class Styled(val text: String, val style: SpanStyle) : InlinePart
        data class Link(val label: String, val url: String) : InlinePart
    }

    private data class PatternMatch(
        val range: IntRange,
        val part: InlinePart,
    )

    private fun buildInlineAnnotatedString(text: String, linkColor: Color): AnnotatedString {
        val parts = parseInlineParts(text)
        return buildAnnotatedString {
            parts.forEach { part ->
                when (part) {
                    is InlinePart.Plain -> append(part.text)
                    is InlinePart.Styled -> withStyle(part.style) { append(part.text) }
                    is InlinePart.Link -> {
                        withLink(
                            LinkAnnotation.Url(
                                url = part.url,
                                styles = TextLinkStyles(
                                    style = SpanStyle(
                                        color = linkColor,
                                        textDecoration = TextDecoration.Underline,
                                    ),
                                ),
                            ),
                        ) {
                            append(part.label)
                        }
                    }
                }
            }
        }
    }

    private fun parseInlineParts(text: String): List<InlinePart> {
        if (text.isEmpty()) return emptyList()

        val patterns = listOf(
            Regex("""\[([^\]]+)]\(([^)]+)\)""") to { m: MatchResult ->
                InlinePart.Link(m.groupValues[1], m.groupValues[2])
            },
            Regex("""\*\*([^*]+)\*\*""") to { m: MatchResult ->
                InlinePart.Styled(m.groupValues[1], SpanStyle(fontWeight = FontWeight.Bold))
            },
            Regex("""_([^_]+)_""") to { m: MatchResult ->
                InlinePart.Styled(m.groupValues[1], SpanStyle(fontStyle = FontStyle.Italic))
            },
            Regex("""~~([^~]+)~~""") to { m: MatchResult ->
                InlinePart.Styled(m.groupValues[1], SpanStyle(textDecoration = TextDecoration.LineThrough))
            },
            Regex("""`([^`]+)`""") to { m: MatchResult ->
                InlinePart.Styled(m.groupValues[1], SpanStyle(fontFamily = FontFamily.Monospace))
            },
            Regex("""https?://[^\s]+""") to { m: MatchResult ->
                val url = m.value.trimEnd(',', '.', ')', '!', '?')
                InlinePart.Link(url, url)
            },
        )

        var earliest: PatternMatch? = null
        for ((regex, factory) in patterns) {
            val match = regex.find(text) ?: continue
            if (earliest == null || match.range.first < earliest.range.first) {
                earliest = PatternMatch(match.range, factory(match))
            }
        }

        if (earliest == null) {
            return listOf(InlinePart.Plain(text))
        }

        val before = text.substring(0, earliest.range.first)
        val after = text.substring(earliest.range.last + 1)
        val middle = when (val part = earliest.part) {
            is InlinePart.Link -> part
            is InlinePart.Styled -> part
            is InlinePart.Plain -> part
        }

        val prefixParts = if (before.isEmpty()) emptyList() else parseInlineParts(before)
        val suffixParts = if (after.isEmpty()) emptyList() else parseInlineParts(after)
        return prefixParts + middle + suffixParts
    }
}
