package org.cyblight.android.data.api

import com.google.gson.annotations.SerializedName

data class ApiEnvelope<T>(
    val ok: Boolean = false,
    val data: T? = null,
    val error: String? = null,
)

data class LoginRequest(
    val login: String,
    val password: String,
    val turnstileToken: String,
)

data class TwoFactorRequest(
    val userId: String,
    val code: String,
    val rememberDevice: Boolean = false,
)

data class LoginData(
    val ok: Boolean? = null,
    val requires2FA: Boolean? = null,
    val userId: String? = null,
    val user: UserDto? = null,
)

data class UserDto(
    val id: String,
    val login: String,
    @SerializedName("publicId") val publicId: Long? = null,
    val role: String? = null,
)

data class TwoFactorVerifyResponse(
    val ok: Boolean = false,
    val user: UserDto? = null,
    val error: String? = null,
)

data class MeResponse(
    val ok: Boolean = false,
    val user: UserDto? = null,
    val error: String? = null,
)

data class FriendsListResponse(
    val ok: Boolean = false,
    val friends: List<FriendDto> = emptyList(),
    val error: String? = null,
)

data class FriendDto(
    val id: String,
    val username: String,
    val avatar: String? = null,
    val isOnline: Boolean = false,
    @SerializedName("lastSeenAt") val lastSeenAt: Long? = null,
)

data class UnreadSummaryResponse(
    val ok: Boolean = false,
    @SerializedName("totalUnread") val totalUnread: Int = 0,
    @SerializedName("unreadByUser") val unreadByUser: Map<String, Int> = emptyMap(),
)

data class MessagesResponse(
    val ok: Boolean = false,
    val messages: List<MessageDto> = emptyList(),
    val error: String? = null,
)

data class MessageDto(
    val id: String,
    @SerializedName("senderId") val senderId: String,
    val content: String,
    @SerializedName("createdAt") val createdAt: Long,
    @SerializedName("readAt") val readAt: Long? = null,
)

data class SendMessageRequest(
    @SerializedName("recipientId") val recipientId: String,
    val content: String,
)

data class SendMessageResponse(
    val ok: Boolean = false,
    val message: SentMessageDto? = null,
    val error: String? = null,
)

data class SentMessageDto(
    val id: String,
    @SerializedName("created_at") val createdAt: Long? = null,
)
