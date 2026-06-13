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
    val token: String? = null,
    val error: String? = null,
)

data class PasskeyRp(
    val name: String = "",
    val id: String = "",
)

data class PasskeyUserInfo(
    val id: String = "",
    val name: String = "",
    @SerializedName("displayName") val displayName: String = "",
)

data class PasskeyCredParam(
    val type: String = "public-key",
    val alg: Int = -7,
)

data class PasskeyAuthenticatorSelection(
    @SerializedName("authenticatorAttachment") val authenticatorAttachment: String? = null,
    @SerializedName("requireResidentKey") val requireResidentKey: Boolean? = null,
    @SerializedName("residentKey") val residentKey: String? = null,
    @SerializedName("userVerification") val userVerification: String? = null,
)

data class PasskeyCreationOptions(
    val challenge: String = "",
    val rp: PasskeyRp = PasskeyRp(),
    val user: PasskeyUserInfo = PasskeyUserInfo(),
    @SerializedName("pubKeyCredParams") val pubKeyCredParams: List<PasskeyCredParam> = emptyList(),
    val timeout: Long = 60_000,
    @SerializedName("excludeCredentials") val excludeCredentials: List<PasskeyAllowCredential>? = null,
    @SerializedName("authenticatorSelection") val authenticatorSelection: PasskeyAuthenticatorSelection? = null,
    val attestation: String = "none",
)

data class PasskeyRegisterOptionsResponse(
    val ok: Boolean = false,
    val options: PasskeyCreationOptions? = null,
    val error: String? = null,
)

data class PasskeyAttestationResponse(
    val clientDataJSON: String,
    val attestationObject: String,
)

data class PasskeyRegistrationPayload(
    val id: String,
    val rawId: String,
    val response: PasskeyAttestationResponse,
    val type: String = "public-key",
)

data class PasskeyRegisterRequest(
    val credential: PasskeyRegistrationPayload,
    val name: String? = null,
    val transports: List<String>? = null,
)

data class PasskeyRegisterData(
    val id: String = "",
    val name: String? = null,
)

data class DeletedData(
    val deleted: Boolean = false,
)

data class EasterFlagsDto(
    val strawberry: Boolean = false,
    @SerializedName("darkTrigger") val darkTrigger: Boolean = false,
    @SerializedName("profileMirror") val profileMirror: Boolean = false,
    @SerializedName("lightCatcher") val lightCatcher: Boolean = false,
    val postmaster: Boolean = false,
    @SerializedName("developerMode") val developerMode: Boolean = false,
    @SerializedName("themeFlux") val themeFlux: Boolean = false,
    @SerializedName("nightGuard") val nightGuard: Boolean = false,
    @SerializedName("trustedFingerprint") val trustedFingerprint: Boolean = false,
    val bridge: Boolean = false,
    @SerializedName("bridgeWebToday") val bridgeWebToday: Boolean = false,
    @SerializedName("bridgeAppToday") val bridgeAppToday: Boolean = false,
    val echo: Boolean = false,
    val archivist: Boolean = false,
)

data class EasterProgress(
    val biometricUnlockCount: Int = 0,
    val nightGuardSeconds: Int = 0,
    val archivistStepsCompleted: Int = 0,
    val bridgePlatformsToday: Int = 0,
)

data class EasterUnlockResponse(
    val ok: Boolean = false,
    val error: String? = null,
)

data class LightCatcherResponse(
    val ok: Boolean = false,
    @SerializedName("lightCatcher") val lightCatcher: Boolean = false,
    val error: String? = null,
)

data class MeUserDto(
    val id: String = "",
    val login: String = "",
    @SerializedName("createdAt") val createdAt: Long = 0L,
    val role: String? = null,
    val email: String? = null,
    @SerializedName("emailVerified") val emailVerified: Boolean = false,
    @SerializedName("pendingEmail") val pendingEmail: String? = null,
    @SerializedName("pendingEmailVerifiedAt") val pendingEmailVerifiedAt: Long? = null,
    @SerializedName("passChangedAt") val passChangedAt: Long? = null,
    @SerializedName("totpEnabled") val totpEnabled: Boolean = false,
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

data class PasskeyDto(
    val id: String = "",
    val name: String? = null,
    @SerializedName("createdAt") val createdAt: Long = 0L,
    @SerializedName("lastUsedAt") val lastUsedAt: Long? = null,
)

data class PasskeyListResponse(
    val ok: Boolean = false,
    val passkeys: List<PasskeyDto> = emptyList(),
    val error: String? = null,
)

data class TrustedDeviceDto(
    val id: String = "",
    @SerializedName("deviceFingerprint") val deviceFingerprint: String? = null,
    @SerializedName("userAgent") val userAgent: String? = null,
    @SerializedName("ipAddress") val ipAddress: String? = null,
    @SerializedName("createdAt") val createdAt: Long = 0L,
    @SerializedName("lastUsedAt") val lastUsedAt: Long? = null,
    @SerializedName("expiresAt") val expiresAt: Long = 0L,
)

data class TrustedDevicesResponse(
    val ok: Boolean = false,
    val devices: List<TrustedDeviceDto> = emptyList(),
    val error: String? = null,
)

data class LoginHistoryEntryDto(
    val id: String = "",
    val action: String = "",
    val ip: String? = null,
    @SerializedName("userAgent") val userAgent: String? = null,
    @SerializedName("createdAt") val createdAt: Long = 0L,
)

data class LoginHistoryPagination(
    val limit: Int = 0,
    val offset: Int = 0,
    val total: Int = 0,
    @SerializedName("hasMore") val hasMore: Boolean = false,
)

data class LoginHistoryResponse(
    val ok: Boolean = false,
    val history: List<LoginHistoryEntryDto> = emptyList(),
    val pagination: LoginHistoryPagination? = null,
    val error: String? = null,
)

data class FriendsListResponse(
    val ok: Boolean = false,
    val friends: List<FriendDto> = emptyList(),
    val error: String? = null,
)

data class PendingRequestsResponse(
    val ok: Boolean = false,
    @SerializedName("pendingRequests") val pendingRequests: List<FriendDto> = emptyList(),
    val error: String? = null,
)

data class SentRequestsResponse(
    val ok: Boolean = false,
    @SerializedName("sentRequests") val sentRequests: List<FriendDto> = emptyList(),
    val error: String? = null,
)

data class SearchUsersResponse(
    val ok: Boolean = false,
    val users: List<FriendDto> = emptyList(),
    val error: String? = null,
)

data class FriendActionResponse(
    val ok: Boolean = false,
    val error: String? = null,
    val message: String? = null,
)

data class AddFriendRequest(
    @SerializedName("friendUsername") val friendUsername: String,
)

data class FriendIdRequest(
    @SerializedName("friendId") val friendId: String,
)

data class FriendDto(
    val id: String,
    val username: String,
    val avatar: String? = null,
    val isOnline: Boolean = false,
    @SerializedName("lastSeenAt") val lastSeenAt: Long? = null,
    @SerializedName("createdAt") val createdAt: Long? = null,
)

data class FriendPresenceResponse(
    val ok: Boolean = false,
    val isOnline: Boolean = false,
    @SerializedName("lastSeenAt") val lastSeenAt: Long? = null,
    val error: String? = null,
)

data class ConversationPreviewDto(
    val preview: String = "",
    @SerializedName("latestAt") val latestAt: Long = 0L,
    val kind: String = "message",
)

data class UnreadDetailDto(
    @SerializedName("senderId") val senderId: String = "",
    @SerializedName("senderLogin") val senderLogin: String = "",
    @SerializedName("unreadCount") val unreadCount: Int = 0,
    val preview: String = "",
    @SerializedName("latestAt") val latestAt: Long = 0L,
)

data class UnreadSummaryResponse(
    val ok: Boolean = false,
    @SerializedName("totalUnread") val totalUnread: Int = 0,
    @SerializedName("unreadByUser") val unreadByUser: Map<String, Int> = emptyMap(),
    @SerializedName("unreadDetails") val unreadDetails: List<UnreadDetailDto> = emptyList(),
    @SerializedName("conversationPreviews") val conversationPreviews: Map<String, ConversationPreviewDto> = emptyMap(),
)

data class PushRegisterRequest(
    val token: String,
    val platform: String = "android",
)

data class PushUnregisterRequest(
    val token: String? = null,
)

data class PushActionResponse(
    val ok: Boolean = false,
    val error: String? = null,
)

data class PinnedMessageDto(
    @SerializedName("messageId") val messageId: String = "",
    val content: String = "",
    @SerializedName("senderId") val senderId: String = "",
    @SerializedName("pinnedByUserId") val pinnedByUserId: String = "",
    @SerializedName("updatedAt") val updatedAt: Long = 0L,
)

data class MessageReactionDto(
    val emoji: String = "",
    val count: Int = 0,
)

data class ReactMessageRequest(
    val emoji: String,
)

data class ReactMessageResponse(
    val ok: Boolean = false,
    val error: String? = null,
)

data class MarkReadResponse(
    val ok: Boolean = false,
    val error: String? = null,
)

data class MessagesResponse(
    val ok: Boolean = false,
    val messages: List<MessageDto> = emptyList(),
    val pinned: PinnedMessageDto? = null,
    val error: String? = null,
)

data class MessageDto(
    val id: String = "",
    @SerializedName(value = "senderId", alternate = ["sender_id"])
    val senderId: String = "",
    val content: String = "",
    val encryption: String = "plaintext",
    @SerializedName("signalType") val signalType: Int? = null,
    @SerializedName("registrationId") val registrationId: Int? = null,
    @SerializedName(value = "createdAt", alternate = ["created_at"])
    val createdAt: Long = 0L,
    @SerializedName(value = "readAt", alternate = ["read_at"])
    val readAt: Long? = null,
    @SerializedName(value = "editedAt", alternate = ["edited_at"])
    val editedAt: Long? = null,
    val reactions: List<MessageReactionDto> = emptyList(),
)

data class EditMessageRequest(
    val content: String,
    @SerializedName("signalType") val signalType: Int,
    @SerializedName("registrationId") val registrationId: Int,
)

data class PinMessageRequest(
    @SerializedName("forBoth") val forBoth: Boolean = false,
)

data class UnpinMessageRequest(
    @SerializedName("forBoth") val forBoth: Boolean = false,
)

data class MessageActionResponse(
    val ok: Boolean = false,
    val error: String? = null,
)

data class SendMessageRequest(
    @SerializedName("recipientId") val recipientId: String,
    val content: String,
    @SerializedName("signalType") val signalType: Int,
    @SerializedName("registrationId") val registrationId: Int,
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
