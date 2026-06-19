package org.cyblight.android.ui.messages

import android.content.Context
import org.cyblight.android.R

object MessagePreviewFormatter {
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

    fun formatReactionPreview(
        context: Context,
        currentUserId: String,
        actorId: String,
        actorLogin: String,
        emoji: String,
        messageSnippet: String,
    ): String {
        val trimmedEmoji = emoji.trim()
        return if (actorId == currentUserId) {
            if (trimmedEmoji.isEmpty()) {
                context.getString(R.string.chat_reaction_preview_you_no_emoji, messageSnippet)
            } else {
                context.getString(R.string.chat_reaction_preview_you, trimmedEmoji, messageSnippet)
            }
        } else {
            val actor = actorLogin.ifBlank { actorId }
            if (trimmedEmoji.isEmpty()) {
                context.getString(R.string.chat_reaction_preview_other_no_emoji, actor, messageSnippet)
            } else {
                context.getString(R.string.chat_reaction_preview_other, actor, trimmedEmoji, messageSnippet)
            }
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
        val withoutMetadata = ChatFormatUtils.stripMetadataTokens(content)
            .replace(Regex("""\[reply:[^\]]+]""", RegexOption.IGNORE_CASE), "")

        val withoutSpoilers = withoutMetadata.replace(Regex("""\|\|([\s\S]+?)\|\|"""), "…")
        return ChatInlineMarkdown.toPlainText(withoutSpoilers)
    }
}
