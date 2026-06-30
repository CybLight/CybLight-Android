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
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle

internal object ChatInlineMarkdown {
    private val URL_REGEX = Regex("""https?://[^\s]+""")
    private val MARKDOWN_PATTERNS = listOf(
        Triple("**", "**", SpanStyle(fontWeight = FontWeight.Bold)),
        Triple("__", "__", SpanStyle(textDecoration = TextDecoration.Underline)),
        Triple("~~", "~~", SpanStyle(textDecoration = TextDecoration.LineThrough)),
        Triple("`", "`", SpanStyle(fontFamily = FontFamily.Monospace)),
        Triple("_", "_", SpanStyle(fontStyle = FontStyle.Italic, textGeometricTransform = TextGeometricTransform(1.0f, -0.15f))),
    )

    private data class TokenMatch(
        val start: Int,
        val end: Int,
        val contentStart: Int,
        val contentEnd: Int,
        val style: SpanStyle?,
        val linkUrl: String? = null,
    )

    fun parse(text: String, linkColor: Color, baseStyle: SpanStyle = SpanStyle()): AnnotatedString {
        if (text.isEmpty()) return AnnotatedString("")
        return buildAnnotatedString {
            renderRange(
                source = text,
                start = 0,
                end = text.length,
                linkColor = linkColor,
                baseStyle = baseStyle,
                builder = this,
                depth = 0,
            )
        }
    }

    fun toPlainText(text: String): String {
        if (text.isEmpty()) return ""
        val builder = StringBuilder()
        extractPlainRange(text, 0, text.length, builder, 0)
        return builder.toString()
    }

    private fun extractPlainRange(source: String, start: Int, end: Int, out: StringBuilder, depth: Int) {
        if (depth > 20) { // Safety limit
            if (start < end) out.append(source.substring(start, end))
            return
        }
        var cursor = start
        while (cursor < end) {
            val match = findMatchAt(source, cursor, end)
            if (match == null) {
                out.append(source[cursor])
                cursor++
            } else {
                extractPlainRange(source, match.contentStart, match.contentEnd, out, depth + 1)
                cursor = match.end
            }
        }
    }

    private fun renderRange(
        source: String,
        start: Int,
        end: Int,
        linkColor: Color,
        baseStyle: SpanStyle,
        builder: AnnotatedString.Builder,
        depth: Int,
    ) {
        if (depth > 20) { // Safety limit for nested formatting
            if (start < end) builder.append(source.substring(start, end))
            return
        }
        var cursor = start
        while (cursor < end) {
            val match = findMatchAt(source, cursor, end)
            if (match != null) {
                val nestedStyle = when {
                    match.linkUrl != null -> mergeStyles(
                        baseStyle,
                        SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
                    )
                    match.style != null -> mergeStyles(baseStyle, match.style)
                    else -> baseStyle
                }
                if (match.linkUrl != null && isValidLinkUrl(match.linkUrl)) {
                    builder.withLink(
                        LinkAnnotation.Url(
                            url = match.linkUrl,
                            styles = TextLinkStyles(
                                style = SpanStyle(
                                    color = linkColor,
                                    textDecoration = TextDecoration.Underline,
                                ),
                            ),
                        ),
                    ) {
                        renderRange(
                            source = source,
                            start = match.contentStart,
                            end = match.contentEnd,
                            linkColor = linkColor,
                            baseStyle = nestedStyle,
                            builder = this,
                            depth = depth + 1,
                        )
                    }
                } else {
                    builder.withStyle(nestedStyle) {
                        renderRange(
                            source = source,
                            start = match.contentStart,
                            end = match.contentEnd,
                            linkColor = linkColor,
                            baseStyle = nestedStyle,
                            builder = this,
                            depth = depth + 1,
                        )
                    }
                }
                cursor = match.end
                continue
            }

            builder.append(source[cursor])
            cursor++
        }
    }

    private fun findMatchAt(source: String, cursor: Int, end: Int): TokenMatch? {
        // Markdown patterns
        for ((open, close, style) in MARKDOWN_PATTERNS) {
            if (source.startsWith(open, cursor)) {
                val contentStart = cursor + open.length
                if (contentStart > end) continue
                val contentEnd = source.indexOf(close, contentStart)
                if (contentEnd != -1 && contentEnd < end) {
                    return TokenMatch(cursor, contentEnd + close.length, contentStart, contentEnd, style)
                }
            }
        }

        // Markdown Link: [label](url)
        if (source[cursor] == '[') {
            val labelEnd = source.indexOf(']', cursor + 1)
            if (labelEnd != -1 && labelEnd + 2 < end && source[labelEnd + 1] == '(') {
                val urlEnd = source.indexOf(')', labelEnd + 2)
                if (urlEnd != -1 && urlEnd < end) {
                    val url = source.substring(labelEnd + 2, urlEnd)
                    return TokenMatch(cursor, urlEnd + 1, cursor + 1, labelEnd, null, url)
                }
            }
        }

        // Auto-link URL
        val urlMatch = URL_REGEX.find(source, cursor)
        if (urlMatch != null && urlMatch.range.first == cursor && urlMatch.range.last < end) {
            val url = urlMatch.value.trimEnd(',', '.', ')', '!', '?', ';')
            if (isValidLinkUrl(url)) {
                return TokenMatch(cursor, cursor + url.length, cursor, cursor + url.length, null, url)
            }
        }

        return null
    }

    private fun isValidLinkUrl(url: String): Boolean {
        if (url.isBlank()) return false
        return runCatching {
            val scheme = android.net.Uri.parse(url).scheme?.lowercase()
            scheme == "http" || scheme == "https"
        }.getOrDefault(false)
    }

    private fun mergeStyles(base: SpanStyle, extra: SpanStyle): SpanStyle {
        return SpanStyle(
            color = extra.color ?: base.color,
            fontSize = extra.fontSize,
            fontWeight = extra.fontWeight ?: base.fontWeight,
            fontStyle = extra.fontStyle ?: base.fontStyle,
            fontFamily = extra.fontFamily ?: base.fontFamily,
            textDecoration = when {
                extra.textDecoration != null && base.textDecoration != null ->
                    TextDecoration.combine(listOf(base.textDecoration!!, extra.textDecoration!!))
                extra.textDecoration != null -> extra.textDecoration
                else -> base.textDecoration
            },
            background = extra.background ?: base.background,
            textGeometricTransform = extra.textGeometricTransform ?: base.textGeometricTransform,
        )
    }
}
