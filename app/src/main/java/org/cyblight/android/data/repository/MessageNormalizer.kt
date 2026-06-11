package org.cyblight.android.data.repository

import org.cyblight.android.data.api.MessageDto

object MessageNormalizer {
    fun normalize(messages: List<MessageDto>): List<MessageDto> {
        val seen = mutableSetOf<String>()
        return messages.mapIndexed { index, message ->
            val id = message.id.trim().ifBlank { "msg-$index-${message.createdAt}" }
            val uniqueId = if (seen.add(id)) id else "$id-$index"
            message.copy(
                id = uniqueId,
                senderId = message.senderId.trim(),
                content = message.content.ifBlank { " " },
                createdAt = normalizeTimestamp(message.createdAt),
            )
        }
    }

    private fun normalizeTimestamp(value: Long): Long {
        if (value <= 0L) return System.currentTimeMillis()
        // API may return seconds (web client handles both).
        return if (value < 10_000_000_000L) value * 1000L else value
    }
}
