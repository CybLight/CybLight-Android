package org.cyblight.android.data.repository

import org.cyblight.android.data.api.CybLightApi
import org.cyblight.android.data.api.FriendDto
import org.cyblight.android.data.api.EditMessageRequest
import org.cyblight.android.data.api.MessageDto
import org.cyblight.android.data.api.PinnedMessageDto
import org.cyblight.android.data.api.PinMessageRequest
import org.cyblight.android.data.api.UnpinMessageRequest

data class ConversationPreview(
    val friend: FriendDto,
    val unreadCount: Int,
)

data class ChatThread(
    val messages: List<MessageDto>,
    val pinned: PinnedMessageDto?,
)

class MessagesRepository(private val api: CybLightApi) {
    suspend fun loadConversations(friends: List<FriendDto>): Result<List<ConversationPreview>> {
        return try {
            val unread = api.unreadSummary()
            if (!unread.ok) {
                return Result.failure(Exception("unread_load_failed"))
            }

            val previews = friends.map { friend ->
                ConversationPreview(
                    friend = friend,
                    unreadCount = unread.unreadByUser[friend.id] ?: 0,
                )
            }.sortedWith(
                compareByDescending<ConversationPreview> { it.unreadCount }
                    .thenBy { it.friend.username.lowercase() },
            )

            Result.success(previews)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadMessages(friendId: String): Result<ChatThread> {
        return try {
            val response = api.messages(friendId)
            if (response.ok) {
                Result.success(
                    ChatThread(
                        messages = MessageNormalizer.normalize(response.messages),
                        pinned = response.pinned?.takeIf { it.messageId.isNotBlank() && it.content.isNotBlank() },
                    ),
                )
            } else {
                Result.failure(Exception(response.error ?: "messages_load_failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun editMessage(messageId: String, content: String): Result<Unit> {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return Result.failure(Exception("empty_message"))

        return try {
            val response = api.editMessage(messageId, EditMessageRequest(trimmed))
            if (response.ok) Result.success(Unit) else Result.failure(Exception(response.error ?: "edit_failed"))
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

    suspend fun sendMessage(recipientId: String, content: String): Result<Unit> {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return Result.failure(Exception("empty_message"))

        return try {
            val response = api.sendMessage(
                org.cyblight.android.data.api.SendMessageRequest(
                    recipientId = recipientId,
                    content = trimmed,
                ),
            )
            if (response.ok) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.error ?: "send_failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
