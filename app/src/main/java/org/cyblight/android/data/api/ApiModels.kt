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
    val deviceToken: String? = null,
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
    val deviceToken: String? = null,
)

data class PasskeyOptionsRequest(
    val login: String? = null,
)

data class PasskeyAllowCredential(
    val id: String,
    val type: String = "public-key",
    val transports: List<String>? = null,
)

data class PasskeyPublicKeyOptions(
    val challenge: String,
    val timeout: Long = 60_000,
    val rpId: String,
    val allowCredentials: List<PasskeyAllowCredential>? = null,
    val userVerification: String = "required",
)

data class PasskeyOptionsResponse(
    val ok: Boolean = false,
    val challengeId: String? = null,
    val options: PasskeyPublicKeyOptions? = null,
    val error: String? = null,
)

data class PasskeyAssertionResponse(
    val clientDataJSON: String,
    val authenticatorData: String,
    val signature: String,
    val userHandle: String? = null,
)

data class PasskeyCredentialPayload(
    val id: String,
    val rawId: String,
    val response: PasskeyAssertionResponse,
    val type: String = "public-key",
)

data class PasskeyLoginRequest(
    val challengeId: String,
    val credential: PasskeyCredentialPayload,
)

data class PasskeyLoginResponse(
    val ok: Boolean = false,
    val user: UserDto? = null,
    val error: String? = null,
)

data class EasterFlagsDto(
    val strawberry: Boolean = false,
    @SerializedName("darkTrigger") val darkTrigger: Boolean = false,
    @SerializedName("profileMirror") val profileMirror: Boolean = false,
    @SerializedName("lightCatcher") val lightCatcher: Boolean = false,
)

data class LightCatcherResponse(
    val ok: Boolean = false,
    @SerializedName("lightCatcher") val lightCatcher: Boolean = false,
    val error: String? = null,
)

data class MeUserDto(
    val id: String = "",
    val login: String = "",
    val easter: EasterFlagsDto? = null,
)

data class MeResponse(
    val ok: Boolean = false,
    val user: MeUserDto? = null,
    val error: String? = null,
)

data class ProfileDto(
    val id: String = "",
    val username: String = "",
    val avatar: String? = null,
    @SerializedName("avatarUrl") val avatarUrl: String? = null,
    val bio: String? = null,
    @SerializedName("aboutMe") val aboutMe: String? = null,
    val gender: String? = null,
    @SerializedName("dateOfBirth") val dateOfBirth: String? = null,
    @SerializedName("createdAt") val createdAt: Long = 0L,
    @SerializedName("friendsCount") val friendsCount: Int = 0,
    val verified: Boolean = false,
    val role: String? = null,
    @SerializedName("isOnline") val isOnline: Boolean? = null,
    @SerializedName("lastSeenAt") val lastSeenAt: Long? = null,
)

data class ProfileResponse(
    val ok: Boolean = false,
    val profile: ProfileDto? = null,
    val error: String? = null,
)

data class SessionDto(
    val id: String = "",
    @SerializedName("created_at") val createdAt: Long = 0L,
    @SerializedName("expires_at") val expiresAt: Long = 0L,
    @SerializedName("last_seen_at") val lastSeenAt: Long = 0L,
    @SerializedName("user_agent") val userAgent: String? = null,
    val browser: String? = null,
    val os: String? = null,
    @SerializedName("deviceType") val deviceType: String? = null,
    val country: String? = null,
    val city: String? = null,
)

data class SessionsData(
    val current: String? = null,
    val sessions: List<SessionDto> = emptyList(),
)

data class RevokeSessionRequest(
    val id: String,
)

data class RevokeSessionData(
    val removed: Int = 0,
    @SerializedName("loggedOut") val loggedOut: Boolean = false,
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
    val id: String = "",
    @SerializedName(value = "senderId", alternate = ["sender_id"])
    val senderId: String = "",
    val content: String = "",
    @SerializedName(value = "createdAt", alternate = ["created_at"])
    val createdAt: Long = 0L,
    @SerializedName(value = "readAt", alternate = ["read_at"])
    val readAt: Long? = null,
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
