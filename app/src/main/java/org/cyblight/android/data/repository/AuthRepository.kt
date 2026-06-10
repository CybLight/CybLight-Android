package org.cyblight.android.data.repository

import org.cyblight.android.data.api.CybLightApi
import org.cyblight.android.data.api.LoginRequest
import org.cyblight.android.data.api.TwoFactorRequest
import org.cyblight.android.data.api.UserDto
import org.cyblight.android.data.extractAuthToken
import org.cyblight.android.data.session.SessionManager

sealed class AuthResult {
    data class Success(val user: UserDto) : AuthResult()
    data class Requires2FA(val userId: String) : AuthResult()
    data class Error(val code: String) : AuthResult()
}

class AuthRepository(
    private val api: CybLightApi,
    private val sessionManager: SessionManager,
) {
    suspend fun login(login: String, password: String, turnstileToken: String): AuthResult {
        val response = api.login(
            LoginRequest(
                login = login.trim(),
                password = password,
                turnstileToken = turnstileToken,
            ),
        )

        if (!response.isSuccessful) {
            return AuthResult.Error(parseErrorBody(response.errorBody()?.string()))
        }

        val body = response.body()
        if (body?.ok != true) {
            return AuthResult.Error(body?.error ?: "unknown_error")
        }

        val data = body.data
        if (data?.requires2FA == true && !data.userId.isNullOrBlank()) {
            return AuthResult.Requires2FA(data.userId)
        }

        val user = data?.user ?: return AuthResult.Error("invalid_response")
        val token = extractAuthToken(response.headers().values("Set-Cookie"))
            ?: return AuthResult.Error("missing_token")

        sessionManager.saveSession(token, user.id, user.login)
        return AuthResult.Success(user)
    }

    suspend fun verify2FA(userId: String, code: String): AuthResult {
        val response = api.verifyTwoFactor(
            TwoFactorRequest(userId = userId, code = code.trim()),
        )

        if (!response.isSuccessful) {
            return AuthResult.Error(parseErrorBody(response.errorBody()?.string()))
        }

        val body = response.body()
        if (body?.ok != true) {
            return AuthResult.Error(body?.error ?: "unknown_error")
        }

        val user = body.data?.user ?: return AuthResult.Error("invalid_response")
        val token = extractAuthToken(response.headers().values("Set-Cookie"))
            ?: return AuthResult.Error("missing_token")

        sessionManager.saveSession(token, user.id, user.login)
        return AuthResult.Success(user)
    }

    suspend fun restoreSession(): UserDto? {
        val token = sessionManager.getToken() ?: return null
        if (token.isBlank()) return null

        val me = api.me()
        return if (me.ok) me.user else {
            sessionManager.clear()
            null
        }
    }

    suspend fun logout() {
        runCatching { api.logout() }
        sessionManager.clear()
    }

    private fun parseErrorBody(raw: String?): String {
        if (raw.isNullOrBlank()) return "unknown_error"
        val match = Regex(""""error"\s*:\s*"([^"]+)"""").find(raw)
        return match?.groupValues?.getOrNull(1) ?: "unknown_error"
    }
}
