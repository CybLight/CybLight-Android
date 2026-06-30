package org.cyblight.android.data.repository

import android.content.Context
import org.cyblight.android.crypto.SignalCryptoManager
import org.cyblight.android.data.api.CybLightApi
import org.cyblight.android.data.api.ChatsExportPayload
import org.cyblight.android.data.api.EditMessageRequest
import org.cyblight.android.data.api.FriendDto
import org.cyblight.android.data.api.MessageDto
import org.cyblight.android.data.api.PinnedMessageDto
import org.cyblight.android.data.api.PinMessageRequest
import org.cyblight.android.data.api.ReactMessageRequest
import org.cyblight.android.data.api.SendMessageRequest
import org.cyblight.android.data.api.UnpinMessageRequest

data class ConversationPreview(
    val friend: FriendDto,
    val unreadCount: Int,
    val preview: String? = null,
    val latestAt: Long = 0L,
)

data class ChatThread(
    val messages: List<MessageDto>,
    val pinned: PinnedMessageDto?,
)

class MessagesRepository(
    private val appContext: Context,
    private val api: CybLightApi,
    private val signalCrypto: SignalCryptoManager,
    private val userIdProvider: suspend () -> String?,
) {
    suspend fun loadConversations(friends: List<FriendDto>): Result<List<ConversationPreview>> {
        return try {
            val unread = api.unreadSummary()
            if (!unread.ok) {
                return Result.failure(Exception("unread_load_failed"))
            }

            val userId = userIdProvider()?.trim().orEmpty()
            val enrichedPreviews = if (userId.isNotBlank()) {
                ConversationPreviewEnricher.enrichPreviews(
                    context = appContext,
                    userId = userId,
                    previews = unread.conversationPreviews,
                    signalCrypto = signalCrypto,
                )
            } else {
                emptyMap()
            }

            val previews = friends.map { friend ->
                val conversation = unread.conversationPreviews[friend.id]
                ConversationPreview(
                    friend = friend,
                    unreadCount = unread.unreadByUser[friend.id] ?: 0,
                    preview = enrichedPreviews[friend.id]
                        ?: conversation?.preview?.takeIf { it.isNotBlank() },
                    latestAt = conversation?.latestAt ?: 0L,
                )
            }.sortedWith(
                compareByDescending<ConversationPreview> { it.latestAt }
                    .thenByDescending { it.unreadCount }
                    .thenBy { it.friend.username.lowercase() },
            )

            Result.success(previews)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadMessages(friendId: String): Result<ChatThread> {
        return try {
            val userId = userIdProvider()?.trim().orEmpty()
            if (userId.isBlank()) return Result.failure(Exception("not_authenticated"))

            val response = api.messages(friendId)
            if (response.ok) {
                val allMessages = response.messages
                val limited = if (allMessages.size > 1000) allMessages.takeLast(1000) else allMessages
                val normalized = MessageNormalizer.normalize(limited)
                val decrypted = signalCrypto.decryptMessages(userId, normalized)
                val pinned = response.pinned?.takeIf { it.messageId.isNotBlank() && it.content.isNotBlank() }
                    ?.let { pin ->
                        val source = normalized.find { it.id == pin.messageId }
                        val decryptedContent = runCatching {
                            signalCrypto.decryptMessage(
                                userId,
                                source ?: MessageDto(
                                    senderId = pin.senderId,
                                    content = pin.content,
                                    encryption = "signal_v1",
                                ),
                            )
                        }.getOrElse { "🔒 Закреплённое сообщение" }
                        pin.copy(content = decryptedContent)
                    }

                Result.success(
                    ChatThread(
                        messages = decrypted,
                        pinned = pinned,
                    ),
                )
            } else {
                Result.failure(Exception(response.error ?: "messages_load_failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun editMessage(messageId: String, recipientId: String, content: String): Result<Unit> {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return Result.failure(Exception("empty_message"))

        return try {
            val userId = userIdProvider()?.trim().orEmpty()
            if (userId.isBlank()) return Result.failure(Exception("not_authenticated"))

            val encrypted = signalCrypto.encryptMessage(userId, recipientId, trimmed)
            val response = api.editMessage(
                messageId,
                EditMessageRequest(
                    content = encrypted.content,
                    signalType = encrypted.signalType,
                    registrationId = encrypted.registrationId,
                ),
            )
            if (response.ok) {
                signalCrypto.cacheSentPlaintext(userId, messageId, trimmed)
                Result.success(Unit)
            } else Result.failure(Exception(response.error ?: "edit_failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteMessage(messageId: String): Result<Unit> {
        return try {
            val response = api.deleteMessage(messageId)
            if (response.ok) Result.success(Unit) else Result.failure(Exception(response.error ?: "delete_failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteMessages(messageIds: List<String>): Result<Int> {
        var deleted = 0
        for (messageId in messageIds) {
            deleteMessage(messageId).onSuccess { deleted++ }
        }
        return if (deleted > 0) Result.success(deleted) else Result.failure(Exception("delete_failed"))
    }

    suspend fun pinMessage(messageId: String, forBoth: Boolean = false): Result<Unit> {
        return try {
            val response = api.pinMessage(messageId, PinMessageRequest(forBoth))
            if (response.ok) Result.success(Unit) else Result.failure(Exception(response.error ?: "pin_failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unpinMessage(messageId: String, forBoth: Boolean = false): Result<Unit> {
        return try {
            val response = api.unpinMessage(messageId, UnpinMessageRequest(forBoth))
            if (response.ok) Result.success(Unit) else Result.failure(Exception(response.error ?: "unpin_failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reactToMessage(messageId: String, emoji: String): Result<Unit> {
        val trimmed = emoji.trim()
        if (trimmed.isEmpty()) return Result.failure(Exception("empty_reaction"))

        return try {
            val response = api.reactToMessage(messageId, ReactMessageRequest(trimmed))
            if (response.ok) Result.success(Unit) else Result.failure(Exception(response.error ?: "reaction_failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markMessagesAsRead(friendId: String): Result<Unit> {
        return try {
            val response = api.markMessagesAsRead(friendId)
            if (response.ok) Result.success(Unit) else Result.failure(Exception(response.error ?: "mark_read_failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendMessage(recipientId: String, content: String): Result<String?> {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return Result.failure(Exception("empty_message"))

        return try {
            val userId = userIdProvider()?.trim().orEmpty()
            if (userId.isBlank()) return Result.failure(Exception("not_authenticated"))

            val encrypted = signalCrypto.encryptMessage(userId, recipientId, trimmed)
            val response = api.sendMessage(
                SendMessageRequest(
                    recipientId = recipientId,
                    content = encrypted.content,
                    signalType = encrypted.signalType,
                    registrationId = encrypted.registrationId,
                ),
            )
            if (response.ok) {
                val messageId = response.message?.id?.trim().orEmpty().ifBlank { null }
                messageId?.let { signalCrypto.cacheSentPlaintext(userId, it, trimmed) }
                Result.success(messageId)
            } else {
                Result.failure(Exception(response.error ?: "send_failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun ensureSignalKeys(userId: String) {
        signalCrypto.ensureRegistered(userId)
    }

    suspend fun exportChatsJson(): Result<String> {
        return try {
            val response = api.exportChats()
            val export = response.export
            if (!response.ok || export == null) {
                Result.failure(Exception(response.error ?: "export_failed"))
            } else {
                val gson = com.google.gson.Gson()
                Result.success(gson.toJson(mapOf("export" to export)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchChatsExportPayload(): ChatsExportPayload? {
        return try {
            val response = api.exportChats()
            val export = response.export
            if (!response.ok || export == null) null
            else if (export.format != "cyblight-chats" || export.version != 1) null
            else export
        } catch (_: Exception) {
            null
        }
    }

    suspend fun importChatsPayload(payload: ChatsExportPayload): Result<ChatsImportStats> {
        return try {
            val response = api.importChats(org.cyblight.android.data.api.ChatsImportRequest(payload))
            if (response.ok) {
                Result.success(
                    ChatsImportStats(
                        imported = response.imported,
                        skipped = response.skipped,
                        errors = response.errors,
                    ),
                )
            } else {
                Result.failure(Exception(response.error ?: "import_failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importChatsJson(json: String): Result<ChatsImportStats> {
        return try {
            val gson = org.cyblight.android.data.api.createApiGson()
            val root = gson.fromJson(json, com.google.gson.JsonObject::class.java)
            val exportElement = root.get("export") ?: root
            val payload = gson.fromJson(exportElement, ChatsExportPayload::class.java)
            importChatsPayload(payload)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class ChatsImportStats(
    val imported: Int,
    val skipped: Int,
    val errors: Int,
)
