package org.cyblight.android.data.repository

import android.content.Context
import org.cyblight.android.R
import org.cyblight.android.crypto.SignalCryptoManager
import org.cyblight.android.data.api.ConversationPreviewDto
import org.cyblight.android.data.api.LastMessageDto
import org.cyblight.android.data.api.MessageDto
import org.cyblight.android.ui.messages.MessagePreviewFormatter

object ConversationPreviewEnricher {
    suspend fun enrichPreviews(
        context: Context,
        userId: String,
        previews: Map<String, ConversationPreviewDto>,
        signalCrypto: SignalCryptoManager,
    ): Map<String, String> {
        if (userId.isBlank() || previews.isEmpty()) return emptyMap()

        val selfPrefix = { preview: String -> context.getString(R.string.chat_preview_you, preview) }
        val fallback = context.getString(R.string.message_preview_fallback)
        val decryptFailed = context.getString(R.string.message_preview_decrypt_failed)

        val pending = previews.mapNotNull { (friendId, entry) ->
            val wire = entry.lastMessage ?: return@mapNotNull null
            if (wire.encryption != "signal_v1") return@mapNotNull null
            Triple(friendId, entry, wire)
        }
        if (pending.isEmpty()) return emptyMap()

        signalCrypto.ensureRegistered(userId)
        val decryptedById = LinkedHashMap<String, String>()
        val sorted = pending.sortedBy { (_, entry, _) -> entry.latestAt }
        for ((_, _, wire) in sorted) {
            val key = wire.id.ifBlank { "${wire.senderId}:${wire.content}" }
            if (decryptedById.containsKey(key)) continue
            val text = runCatching {
                signalCrypto.decryptMessage(userId, wire.toMessageDto())
            }.getOrElse { decryptFailed }
            decryptedById[key] = text
        }

        return buildMap {
            pending.forEach { (friendId, entry, wire) ->
                val key = wire.id.ifBlank { "${wire.senderId}:${wire.content}" }
                val text = decryptedById[key] ?: return@forEach
                put(
                    friendId,
                    MessagePreviewFormatter.formatConversationPreview(
                        currentUserId = userId,
                        senderId = wire.senderId,
                        text = text,
                        previewYou = selfPrefix,
                        fallback = fallback,
                    ),
                )
            }
        }
    }
}

private fun LastMessageDto.toMessageDto(): MessageDto = MessageDto(
    id = id,
    senderId = senderId,
    content = content,
    encryption = encryption,
    signalType = signalType,
    registrationId = registrationId,
    createdAt = createdAt,
)
