package org.cyblight.android.data.repository

import org.cyblight.android.data.api.MessageDto
import org.cyblight.android.data.api.MessageReactionDto

object MessageNormalizer {
    fun normalize(messages: List<MessageDto>): List<MessageDto> {
        return messages.mapIndexedNotNull { index, message ->
            runCatching {
                val safeId = safeString(message.id)
                val safeSenderId = safeString(message.senderId)
                val safeContent = safeString(message.content)
                val id = safeId.ifBlank { "msg-$index-${message.createdAt}" }
                message.copy(
                    id = id,
                    senderId = safeSenderId,
                    content = safeContent.ifBlank { " " },
                    createdAt = normalizeTimestamp(message.createdAt),
                    readAt = message.readAt?.let { normalizeTimestamp(it) },
                    editedAt = message.editedAt?.let { normalizeTimestamp(it) },
                    reactions = normalizeReactions(message.reactions),
                )
            }.getOrNull()
        }
    }

    private fun normalizeReactions(reactions: List<MessageReactionDto>?): List<MessageReactionDto> {
        return reactions.orEmpty().mapNotNull { reaction ->
            val emoji = reaction.emoji.trim()
            if (emoji.isEmpty()) return@mapNotNull null
            val count = reaction.count.coerceAtLeast(1)
            MessageReactionDto(emoji = emoji, count = count)
        }
    }

    private fun safeString(value: String?): String = value?.trim().orEmpty()

    private fun normalizeTimestamp(value: Long): Long {
        if (value <= 0L) return System.currentTimeMillis()
        return if (value < 10_000_000_000L) value * 1000L else value
    }
}
