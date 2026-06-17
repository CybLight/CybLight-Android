package org.cyblight.android.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface CybLightApi {
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): Response<ApiEnvelope<LoginData>>

    @POST("auth/2fa/verify")
    suspend fun verifyTwoFactor(@Body body: TwoFactorRequest): Response<TwoFactorVerifyResponse>

    @GET("auth/me")
    suspend fun me(): MeResponse

    @POST("auth/session/refresh")
    suspend fun refreshSession(): Response<ApiEnvelope<Unit>>

    @POST("auth/logout")
    suspend fun logout(): ApiEnvelope<Map<String, Any>>

    @GET("profile/me")
    suspend fun myProfile(): ProfileResponse

    @GET("profile/{username}")
    suspend fun profile(@Path("username") username: String): ProfileResponse

    @GET("auth/sessions")
    suspend fun sessions(): ApiEnvelope<SessionsData>

    @POST("auth/sessions/revoke")
    suspend fun revokeSession(@Body body: RevokeSessionRequest): ApiEnvelope<RevokeSessionData>

    @GET("friends/list")
    suspend fun friendsList(): FriendsListResponse

    @GET("friends/pending")
    suspend fun pendingFriends(): PendingRequestsResponse

    @GET("friends/sent")
    suspend fun sentFriends(): SentRequestsResponse

    @GET("friends/presence/{userId}")
    suspend fun friendPresence(@Path("userId") userId: String): FriendPresenceResponse

    @GET("search/users")
    suspend fun searchUsers(@Query("q") query: String): SearchUsersResponse

    @POST("friends/add")
    suspend fun addFriend(@Body body: AddFriendRequest): FriendActionResponse

    @POST("friends/accept")
    suspend fun acceptFriend(@Body body: FriendIdRequest): FriendActionResponse

    @POST("friends/reject")
    suspend fun rejectFriend(@Body body: FriendIdRequest): FriendActionResponse

    @POST("friends/remove")
    suspend fun removeFriend(@Body body: FriendIdRequest): FriendActionResponse

    @GET("messages/unread-summary")
    suspend fun unreadSummary(): UnreadSummaryResponse

    @POST("messages/{messageId}/react")
    suspend fun reactToMessage(
        @Path("messageId") messageId: String,
        @Body body: ReactMessageRequest,
    ): ReactMessageResponse

    @POST("messages/{friendId}/mark-read")
    suspend fun markMessagesAsRead(
        @Path("friendId") friendId: String,
    ): MarkReadResponse

    @GET("messages/{friendId}")
    suspend fun messages(
        @Path("friendId") friendId: String,
    ): MessagesResponse

    @POST("messages/send")
    suspend fun sendMessage(@Body body: SendMessageRequest): SendMessageResponse

    @PATCH("messages/{messageId}")
    suspend fun editMessage(
        @Path("messageId") messageId: String,
        @Body body: EditMessageRequest,
    ): MessageActionResponse

    @DELETE("messages/{messageId}")
    suspend fun deleteMessage(@Path("messageId") messageId: String): MessageActionResponse

    @POST("messages/{messageId}/pin")
    suspend fun pinMessage(
        @Path("messageId") messageId: String,
        @Body body: PinMessageRequest,
    ): MessageActionResponse

    @HTTP(method = "DELETE", path = "messages/{messageId}/pin", hasBody = true)
    suspend fun unpinMessage(
        @Path("messageId") messageId: String,
        @Body body: UnpinMessageRequest,
    ): MessageActionResponse

    @GET("messages/export")
    suspend fun exportChats(): ChatsExportResponse

    @POST("messages/import")
    suspend fun importChats(@Body body: ChatsImportRequest): ChatsImportResponse

    @GET("crypto/keys/status")
    suspend fun signalKeyStatus(): SignalKeyStatusResponse

    @POST("crypto/keys/register")
    suspend fun registerSignalKeys(@Body body: SignalRegisterKeysRequest): SignalKeyActionResponse

    @POST("crypto/keys/prekeys")
    suspend fun replenishSignalPreKeys(@Body body: SignalReplenishPreKeysRequest): SignalKeyActionResponse

    @GET("crypto/keys/bundle/{userId}")
    suspend fun signalKeyBundle(@Path("userId") userId: String): SignalKeyBundleResponse

    @POST("auth/passkey/login/options")
    suspend fun passkeyLoginOptions(@Body body: PasskeyOptionsRequest): PasskeyOptionsResponse

    @POST("auth/passkey/login")
    suspend fun passkeyLogin(@Body body: PasskeyLoginRequest): Response<PasskeyLoginResponse>

    @POST("auth/easter/light-catcher")
    suspend fun unlockLightCatcher(): LightCatcherResponse

    @POST("auth/easter/night-guard")
    suspend fun unlockNightGuard(): EasterUnlockResponse

    @POST("auth/easter/trusted-fingerprint")
    suspend fun unlockTrustedFingerprint(): EasterUnlockResponse

    @POST("auth/easter/echo")
    suspend fun unlockEcho(): EasterUnlockResponse

    @POST("auth/easter/archivist")
    suspend fun unlockArchivist(): EasterUnlockResponse

    @GET("auth/passkey/list")
    suspend fun passkeyList(): PasskeyListResponse

    @POST("auth/passkey/register/options")
    suspend fun passkeyRegisterOptions(): PasskeyRegisterOptionsResponse

    @POST("auth/passkey/register")
    suspend fun passkeyRegister(@Body body: PasskeyRegisterRequest): ApiEnvelope<PasskeyRegisterData>

    @DELETE("auth/passkey/{id}")
    suspend fun deletePasskey(@Path("id") id: String): ApiEnvelope<DeletedData>

    @GET("auth/trusted-devices")
    suspend fun trustedDevices(): TrustedDevicesResponse

    @DELETE("auth/trusted-devices/{id}")
    suspend fun removeTrustedDevice(@Path("id") id: String): ApiEnvelope<Unit>

    @GET("auth/login-history")
    suspend fun loginHistory(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
    ): LoginHistoryResponse

    @POST("push/register")
    suspend fun registerPushToken(@Body body: PushRegisterRequest): PushActionResponse

    @HTTP(method = "DELETE", path = "push/unregister", hasBody = true)
    suspend fun unregisterPushToken(@Body body: PushUnregisterRequest): PushActionResponse
}
