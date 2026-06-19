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

internal object ChatInlineMarkdown {
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
            )
        }
    }

    fun toPlainText(text: String): String {
        if (text.isEmpty()) return ""
        val builder = StringBuilder()
        extractPlainRange(text, 0, text.length, builder)
        return builder.toString()
    }

    private fun extractPlainRange(source: String, start: Int, end: Int, out: StringBuilder) {
        var cursor = start
        while (cursor < end) {
            val atLineStart = cursor == start && (cursor == 0 || source[cursor - 1] == '\n')
            if (atLineStart && source.startsWith("> ", cursor) && cursor + 2 <= end) {
                val lineEnd = source.indexOf('\n', cursor + 2).let { if (it == -1 || it > end) end else it }
                extractPlainRange(source, cursor + 2, lineEnd, out)
                cursor = lineEnd
                continue
            }

            val match = findEarliestMatch(source, cursor, end)
            if (match == null) {
                out.append(source[cursor])
                cursor++
            } else {
                extractPlainRange(source, match.contentStart, match.contentEnd, out)
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
    ) {
        var cursor = start
        while (cursor < end) {
            val atLineStart = cursor == start && (cursor == 0 || source[cursor - 1] == '\n')
            if (atLineStart && source.startsWith("> ", cursor) && cursor + 2 <= end) {
                val lineEnd = source.indexOf('\n', cursor + 2).let { if (it == -1 || it > end) end else it }
                renderRange(
                    source = source,
                    start = cursor + 2,
                    end = lineEnd,
                    linkColor = linkColor,
                    baseStyle = mergeStyles(
                        baseStyle,
                        SpanStyle(color = Color(0xFF8B9DC3), fontStyle = FontStyle.Italic),
                    ),
                    builder = builder,
                )
                cursor = lineEnd
                continue
            }

            val match = findEarliestMatch(source, cursor, end)
            if (match != null) {
                val nestedStyle = when {
                    match.linkUrl != null -> mergeStyles(
                        baseStyle,
                        SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
                    )
                    match.style != null -> mergeStyles(baseStyle, match.style)
                    else -> baseStyle
                }
                if (match.linkUrl != null) {
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

    private fun findEarliestMatch(source: String, cursor: Int, end: Int): TokenMatch? {
        val patterns = listOf(
            Triple("**", "**", SpanStyle(fontWeight = FontWeight.Bold)),
            Triple("__", "__", SpanStyle(textDecoration = TextDecoration.Underline)),
            Triple("~~", "~~", SpanStyle(textDecoration = TextDecoration.LineThrough)),
            Triple("`", "`", SpanStyle(fontFamily = FontFamily.Monospace)),
            Triple("_", "_", SpanStyle(fontStyle = FontStyle.Italic)),
        )

        var earliest: TokenMatch? = null
        for ((open, close, style) in patterns) {
            if (!source.startsWith(open, cursor)) continue
            val contentStart = cursor + open.length
            if (contentStart > end) continue
            val contentEnd = source.indexOf(close, contentStart)
            if (contentEnd == -1 || contentEnd > end) continue
            val tokenEnd = contentEnd + close.length
            val match = TokenMatch(cursor, tokenEnd, contentStart, contentEnd, style)
            if (earliest == null || match.start < earliest.start) {
                earliest = match
            }
        }

        if (source[cursor] == '[') {
            val labelEnd = source.indexOf(']', cursor + 1)
            if (labelEnd != -1 && labelEnd + 1 < end && source[labelEnd + 1] == '(') {
                val urlStart = labelEnd + 2
                val urlEnd = source.indexOf(')', urlStart)
                if (urlEnd != -1 && urlEnd < end) {
                    val url = source.substring(urlStart, urlEnd)
                    val match = TokenMatch(
                        start = cursor,
                        end = urlEnd + 1,
                        contentStart = cursor + 1,
                        contentEnd = labelEnd,
                        style = null,
                        linkUrl = url,
                    )
                    if (earliest == null || match.start < earliest.start) {
                        earliest = match
                    }
                }
            }
        }

        val urlMatch = Regex("""https?://[^\s]+""").find(source, cursor)?.takeIf { it.range.last < end }
        if (urlMatch != null) {
            val url = urlMatch.value.trimEnd(',', '.', ')', '!', '?')
            val match = TokenMatch(
                start = urlMatch.range.first,
                end = urlMatch.range.first + url.length,
                contentStart = urlMatch.range.first,
                contentEnd = urlMatch.range.first + url.length,
                style = null,
                linkUrl = url,
            )
            if (earliest == null || match.start < earliest.start) {
                earliest = match
            }
        }

        return earliest
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
        )
    }
}
