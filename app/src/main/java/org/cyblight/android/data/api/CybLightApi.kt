package org.cyblight.android.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path

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

    @Headers("Origin: https://cyblight.org")
    @POST("auth/sessions/revoke")
    suspend fun revokeSession(@Body body: RevokeSessionRequest): ApiEnvelope<RevokeSessionData>

    @GET("friends/list")
    suspend fun friendsList(): FriendsListResponse

    @GET("messages/unread-summary")
    suspend fun unreadSummary(): UnreadSummaryResponse

    @GET("messages/{friendId}")
    suspend fun messages(
        @Path("friendId") friendId: String,
    ): MessagesResponse

    @POST("messages/send")
    suspend fun sendMessage(@Body body: SendMessageRequest): SendMessageResponse

    @POST("auth/passkey/login/options")
    suspend fun passkeyLoginOptions(@Body body: PasskeyOptionsRequest): PasskeyOptionsResponse

    @Headers("Origin: https://cyblight.org")
    @POST("auth/passkey/login")
    suspend fun passkeyLogin(@Body body: PasskeyLoginRequest): Response<PasskeyLoginResponse>

    @POST("auth/easter/light-catcher")
    suspend fun unlockLightCatcher(): LightCatcherResponse
}
