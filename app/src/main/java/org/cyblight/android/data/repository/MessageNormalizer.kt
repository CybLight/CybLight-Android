package org.cyblight.android.data.repository

import org.cyblight.android.data.api.MessageDto
import org.cyblight.android.data.api.MessageReactionDto

object MessageNormalizer {
    fun normalize(messages: List<MessageDto>): List<MessageDto> {
        return messages.map { message ->
            val id = message.id.takeIf { it.isNotBlank() } ?: "msg-gen-${message.createdAt}"
            val createdAt = normalizeTimestamp(message.createdAt)
            
            if (message.id == id && message.createdAt == createdAt && message.content.isNotBlank()) {
                message
            } else {
                message.copy(
                    id = id,
                    content = message.content.takeIf { it.isNotBlank() } ?: " ",
                    createdAt = createdAt,
                    readAt = message.readAt?.let { normalizeTimestamp(it) },
                    editedAt = message.editedAt?.let { normalizeTimestamp(it) },
                    reactions = normalizeReactions(message.reactions),
                )
            }
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

    private fun normalizeTimestamp(value: Long): Long {
        if (value <= 0L) return System.currentTimeMillis()
        return if (value < 10_000_000_000L) value * 1000L else value
    }
}
