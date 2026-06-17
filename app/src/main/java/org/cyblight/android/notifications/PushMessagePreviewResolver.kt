package org.cyblight.android.notifications

import android.content.Context
import org.cyblight.android.R
import org.cyblight.android.crypto.SignalCryptoManager
import org.cyblight.android.data.ApiClient
import org.cyblight.android.data.api.MessageDto
import org.cyblight.android.data.session.SessionManager
import org.cyblight.android.ui.messages.MessagePreviewFormatter

object PushMessagePreviewResolver {
    suspend fun resolvePreview(context: Context, data: Map<String, String>): String {
        val fallback = context.getString(R.string.message_preview_fallback)
        val encryptedFallback = context.getString(R.string.message_preview_encrypted)
        val decryptFailed = context.getString(R.string.message_preview_decrypt_failed)
        val previewYou = { preview: String -> context.getString(R.string.chat_preview_you, preview) }

        val encrypted = data["encrypted"] == "1"
        val rawBody = data["body"]?.trim().orEmpty()
        if (!encrypted) {
            return MessagePreviewFormatter.truncatePreviewText(
                content = rawBody,
                fallback = fallback,
            )
        }

        val userId = SessionManager(context).getUserId()?.trim().orEmpty()
        if (userId.isBlank()) return encryptedFallback

        val friendId = data["friendId"]?.trim().orEmpty()
        val messageId = data["messageId"]?.trim().orEmpty()
        val senderId = data["senderId"]?.trim().orEmpty().ifBlank { friendId }

        val message = wireMessageFromPush(data) ?: fetchMessage(context, friendId, messageId)
        if (message == null) return encryptedFallback

        val api = ApiClient.create(SessionManager(context))
        val signalCrypto = SignalCryptoManager(context, api)
        val decrypted = runCatching {
            signalCrypto.ensureRegistered(userId)
            signalCrypto.decryptMessage(userId, message)
        }.getOrElse { decryptFailed }

        return MessagePreviewFormatter.formatConversationPreview(
            currentUserId = userId,
            senderId = senderId,
            text = decrypted,
            previewYou = previewYou,
            fallback = fallback,
        )
    }

    private fun wireMessageFromPush(data: Map<String, String>): MessageDto? {
        val content = data["content"]?.trim().orEmpty()
        if (content.isEmpty()) return null
        return MessageDto(
            id = data["messageId"]?.trim().orEmpty(),
            senderId = data["senderId"]?.trim().orEmpty().ifBlank { data["friendId"].orEmpty() },
            content = content,
            encryption = data["encryption"]?.trim().orEmpty().ifBlank { "signal_v1" },
            signalType = data["signalType"]?.toIntOrNull(),
            registrationId = data["registrationId"]?.toIntOrNull(),
        )
    }

    private suspend fun fetchMessage(
        context: Context,
        friendId: String,
        messageId: String,
    ): MessageDto? {
        if (friendId.isBlank() || messageId.isBlank()) return null
        val sessionManager = SessionManager(context)
        if (sessionManager.getToken().isNullOrBlank()) return null

        val api = ApiClient.create(sessionManager)
        val signalCrypto = SignalCryptoManager(context, api)
        val userId = sessionManager.getUserId()?.trim().orEmpty()
        if (userId.isBlank()) return null

        return runCatching {
            val response = api.messages(friendId)
            if (!response.ok) return null
            val normalized = org.cyblight.android.data.repository.MessageNormalizer.normalize(response.messages)
            normalized.find { it.id == messageId }
        }.getOrNull()
    }
}
