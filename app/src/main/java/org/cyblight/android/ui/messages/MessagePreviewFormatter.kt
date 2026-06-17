package org.cyblight.android.ui.messages

object MessagePreviewFormatter {
    private const val SPOILER_MARKER = "[[spoiler]]"
    private const val DEFAULT_MAX_LEN = 120

    fun formatConversationPreview(
        currentUserId: String,
        senderId: String,
        text: String,
        previewYou: (String) -> String,
        fallback: String,
    ): String {
        val preview = truncatePreviewText(text, fallback)
        return if (senderId == currentUserId) {
            previewYou(preview)
        } else {
            preview
        }
    }

    fun truncatePreviewText(content: String, fallback: String, maxLen: Int = DEFAULT_MAX_LEN): String {
        val plain = stripPreviewFormatting(content)
            .replace(Regex("""\s+"""), " ")
            .trim()
        if (plain.isEmpty()) return fallback
        if (plain.length <= maxLen) return plain
        return plain.take(maxLen - 1) + "…"
    }

    private fun stripPreviewFormatting(content: String): String {
        return maskSpoilersForPreview(ChatFormatUtils.stripMetadataTokens(content))
            .replace(Regex("""\[reply:[^\]]+]""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\*\*(.+?)\*\*"""), "$1")
            .replace(Regex("""__(.+?)__"""), "$1")
            .replace(Regex("""~~(.+?)~~"""), "$1")
            .replace(Regex("""`(.+?)`"""), "$1")
            .replace(Regex("""(?m)^>\s?"""), "")
    }

    private fun maskSpoilersForPreview(content: String): String {
        return content.replace(Regex("""\|\|([^|]+)\|\|"""), SPOILER_MARKER)
    }
}
