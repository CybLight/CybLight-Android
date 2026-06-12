package org.cyblight.android.update

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink

private val MARKDOWN_LINK = Regex("""\[([^\]]+)]\(([^)]+)\)""")
private val MARKDOWN_HEADING = Regex("""^#{1,6}\s*""", RegexOption.MULTILINE)
private val URL_PATTERN = Regex("""https?://[^\s\]\)>,]+""")

fun formatReleaseNotesForDisplay(raw: String): String {
    if (raw.isBlank()) return raw

    val withoutLinks = MARKDOWN_LINK.replace(raw) { match ->
        val label = match.groupValues[1].trim()
        val url = match.groupValues[2].trim()
        when {
            label.equals(url, ignoreCase = true) -> url
            label.startsWith("http://") || label.startsWith("https://") -> label
            else -> "$label: $url"
        }
    }

    return withoutLinks
        .replace(MARKDOWN_HEADING, "")
        .replace("**", "")
        .trim()
}

fun buildReleaseNotesAnnotatedString(raw: String, linkColor: Color): AnnotatedString {
    val text = formatReleaseNotesForDisplay(raw)
    if (text.isBlank()) return AnnotatedString("")

    return buildAnnotatedString {
        var lastIndex = 0
        URL_PATTERN.findAll(text).forEach { match ->
            if (match.range.first > lastIndex) {
                append(text.substring(lastIndex, match.range.first))
            }
            val url = match.value
            withLink(
                LinkAnnotation.Url(
                    url = url,
                    styles = TextLinkStyles(
                        style = SpanStyle(
                            color = linkColor,
                            textDecoration = TextDecoration.Underline,
                        ),
                    ),
                ),
            ) {
                append(url)
            }
            lastIndex = match.range.last + 1
        }
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }
}
