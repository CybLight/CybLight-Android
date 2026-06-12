package org.cyblight.android.data.repository

import org.cyblight.android.data.api.MessageDto

object MessageNormalizer {
    fun normalize(messages: List<MessageDto>): List<MessageDto> {
        val seen = mutableSetOf<String>()
        return messages.mapIndexedNotNull { index, message ->
            runCatching {
                val safeId = safeString(message.id)
                val safeSenderId = safeString(message.senderId)
                val safeContent = safeString(message.content)
                val id = safeId.ifBlank { "msg-$index-${message.createdAt}" }
                val uniqueId = if (seen.add(id)) id else "$id-$index"
                message.copy(
                    id = uniqueId,
                    senderId = safeSenderId,
                    content = safeContent.ifBlank { " " },
                    createdAt = normalizeTimestamp(message.createdAt),
                    readAt = message.readAt?.let { normalizeTimestamp(it) },
                    editedAt = message.editedAt?.let { normalizeTimestamp(it) },
                    reactions = message.reactions
                        .mapNotNull { reaction ->
                            val emoji = reaction.emoji.trim()
                            if (emoji.isEmpty()) null else reaction.copy(emoji = emoji, count = reaction.count.coerceAtLeast(1))
                        },
                )
            }.getOrNull()
        }
    }

    // Gson may still deliver nulls for non-null Kotlin fields at runtime.
    private fun safeString(value: String?): String = value?.trim().orEmpty()

    private fun normalizeTimestamp(value: Long): Long {
        if (value <= 0L) return System.currentTimeMillis()
        // API may return seconds (web client handles both).
        return if (value < 10_000_000_000L) value * 1000L else value
    }
}
