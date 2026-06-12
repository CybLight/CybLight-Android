package org.cyblight.android.update

private val MARKDOWN_LINK = Regex("""\[([^\]]+)]\(([^)]+)\)""")
private val MARKDOWN_HEADING = Regex("""^#{1,6}\s*""", RegexOption.MULTILINE)

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
