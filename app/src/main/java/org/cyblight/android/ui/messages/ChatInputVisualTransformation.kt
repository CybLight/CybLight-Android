package org.cyblight.android.ui.messages

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration

object ChatInputVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val source = text.text
        if (source.isEmpty()) {
            return TransformedText(AnnotatedString(""), OffsetMapping.Identity)
        }

        val builder = AnnotatedString.Builder()
        val originalToTransformed = IntArray(source.length + 1) { 0 }
        val transformedToOriginal = mutableListOf<Int>()

        fun mapSkippedRange(start: Int, end: Int) {
            val mapped = builder.length
            for (index in start until end.coerceAtMost(source.length)) {
                originalToTransformed[index] = mapped
            }
        }

        fun appendVisibleChar(originalIndex: Int, style: SpanStyle) {
            originalToTransformed[originalIndex] = builder.length
            val start = builder.length
            builder.pushStyle(style)
            builder.append(source[originalIndex])
            builder.pop()
            transformedToOriginal += originalIndex
            originalToTransformed[originalIndex + 1] = builder.length
            if (builder.length == start) {
                originalToTransformed[originalIndex + 1] = builder.length
            }
        }

        fun mergeStyles(base: SpanStyle, extra: SpanStyle): SpanStyle {
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

        fun renderRange(start: Int, end: Int, baseStyle: SpanStyle = SpanStyle()) {
            var cursor = start
            while (cursor < end) {
                val atLineStart = cursor == start && (cursor == 0 || source[cursor - 1] == '\n')

                if (atLineStart && source.startsWith("> ", cursor) && cursor + 2 <= end) {
                    mapSkippedRange(cursor, cursor + 2)
                    val lineEnd = source.indexOf('\n', cursor + 2).let { if (it == -1 || it > end) end else it }
                    renderRange(
                        start = cursor + 2,
                        end = lineEnd,
                        baseStyle = mergeStyles(
                            baseStyle,
                            SpanStyle(color = Color(0xFF8B9DC3), fontStyle = FontStyle.Italic),
                        ),
                    )
                    cursor = lineEnd
                    continue
                }

                data class TokenMatch(
                    val start: Int,
                    val end: Int,
                    val contentStart: Int,
                    val contentEnd: Int,
                    val style: SpanStyle,
                )

                val patterns = listOf(
                    Triple("```", "\n```", SpanStyle(fontFamily = FontFamily.Monospace)),
                    Triple("**", "**", SpanStyle(fontWeight = FontWeight.Bold)),
                    Triple("__", "__", SpanStyle(textDecoration = TextDecoration.Underline)),
                    Triple("~~", "~~", SpanStyle(textDecoration = TextDecoration.LineThrough)),
                    Triple("||", "||", SpanStyle(background = Color(0x55FFD54F))),
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

                if (earliest == null && source[cursor] == '[') {
                    val labelEnd = source.indexOf(']', cursor + 1)
                    if (labelEnd != -1 && labelEnd + 1 < end && source[labelEnd + 1] == '(') {
                        val urlStart = labelEnd + 2
                        val urlEnd = source.indexOf(')', urlStart)
                        if (urlEnd != -1 && urlEnd < end) {
                            earliest = TokenMatch(
                                start = cursor,
                                end = urlEnd + 1,
                                contentStart = cursor + 1,
                                contentEnd = labelEnd,
                                style = SpanStyle(
                                    color = Color(0xFF6FB6FF),
                                    textDecoration = TextDecoration.Underline,
                                ),
                            )
                        }
                    }
                }

                if (earliest != null) {
                    mapSkippedRange(earliest.start, earliest.contentStart)
                    renderRange(
                        start = earliest.contentStart,
                        end = earliest.contentEnd,
                        baseStyle = mergeStyles(baseStyle, earliest.style),
                    )
                    mapSkippedRange(earliest.contentEnd, earliest.end)
                    originalToTransformed[earliest.end.coerceAtMost(source.length)] = builder.length
                    cursor = earliest.end
                    continue
                }

                appendVisibleChar(cursor, baseStyle)
                cursor++
            }
        }

        renderRange(0, source.length)
        originalToTransformed[source.length] = builder.length

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                originalToTransformed[offset.coerceIn(0, source.length)]

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                if (transformedToOriginal.isEmpty()) return 0
                if (offset >= transformedToOriginal.size) return source.length
                return transformedToOriginal[offset.coerceIn(0, transformedToOriginal.lastIndex)]
            }
        }

        return TransformedText(builder.toAnnotatedString(), offsetMapping)
    }
}
