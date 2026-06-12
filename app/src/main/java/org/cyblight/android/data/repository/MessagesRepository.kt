package org.cyblight.android.data.repository

import org.cyblight.android.data.api.CybLightApi
import org.cyblight.android.data.api.FriendDto
import org.cyblight.android.data.api.MessageDto

data class ConversationPreview(
    val friend: FriendDto,
    val unreadCount: Int,
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

    suspend fun loadMessages(friendId: String): Result<List<MessageDto>> {
        return try {
            val response = api.messages(friendId)
            if (response.ok) {
                Result.success(MessageNormalizer.normalize(response.messages))
            } else {
                Result.failure(Exception(response.error ?: "messages_load_failed"))
            }
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
