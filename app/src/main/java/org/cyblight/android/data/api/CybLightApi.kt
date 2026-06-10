package org.cyblight.android.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface CybLightApi {
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): Response<ApiEnvelope<LoginData>>

    @POST("auth/2fa/verify")
    suspend fun verifyTwoFactor(@Body body: TwoFactorRequest): Response<ApiEnvelope<LoginData>>

    @GET("auth/me")
    suspend fun me(): MeResponse

    @POST("auth/logout")
    suspend fun logout(): ApiEnvelope<Map<String, Any>>

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
}
