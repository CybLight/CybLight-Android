package org.cyblight.android.data.repository

import android.app.Activity
import org.cyblight.android.auth.PasskeyAuthException
import org.cyblight.android.auth.PasskeyAuthHelper
import org.cyblight.android.data.api.CybLightApi
import org.cyblight.android.data.api.LoginRequest
import org.cyblight.android.data.api.PasskeyLoginRequest
import org.cyblight.android.data.api.PasskeyOptionsRequest
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
                deviceToken = sessionManager.getDeviceToken(),
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

    suspend fun verify2FA(userId: String, code: String, rememberDevice: Boolean): AuthResult {
        val response = api.verifyTwoFactor(
            TwoFactorRequest(
                userId = userId,
                code = code.trim(),
                rememberDevice = rememberDevice,
            ),
        )

        if (!response.isSuccessful) {
            return AuthResult.Error(parseErrorBody(response.errorBody()?.string()))
        }

        val body = response.body()
        if (body?.ok != true) {
            return AuthResult.Error(body?.error ?: "unknown_error")
        }

        val user = body.user ?: return AuthResult.Error("invalid_response")
        val token = extractAuthToken(response.headers().values("Set-Cookie"))
            ?: return AuthResult.Error("missing_token")

        sessionManager.saveSession(token, user.id, user.login)
        body.deviceToken?.takeIf { it.isNotBlank() }?.let { sessionManager.saveDeviceToken(it) }
        return AuthResult.Success(user)
    }

    suspend fun loginWithPasskey(activity: Activity, login: String? = null): AuthResult {
        return try {
            val optionsResponse = api.passkeyLoginOptions(
                PasskeyOptionsRequest(login = login?.trim()?.takeIf { it.isNotEmpty() }),
            )
            if (!optionsResponse.ok) {
                return AuthResult.Error(optionsResponse.error ?: "passkey_failed")
            }

            val challengeId = optionsResponse.challengeId
            val options = optionsResponse.options
            if (challengeId.isNullOrBlank() || options == null) {
                return AuthResult.Error("passkey_failed")
            }

            val credential = PasskeyAuthHelper.getCredential(activity, options)
            val response = api.passkeyLogin(
                PasskeyLoginRequest(
                    challengeId = challengeId,
                    credential = credential,
                ),
            )

            if (!response.isSuccessful) {
                return AuthResult.Error(parseErrorBody(response.errorBody()?.string()))
            }

            val body = response.body()
            if (body?.ok != true) {
                return AuthResult.Error(body?.error ?: "passkey_failed")
            }

            val token = extractAuthToken(response.headers().values("Set-Cookie"))
                ?: return AuthResult.Error("missing_token")

            val user = body.user ?: api.me().user ?: return AuthResult.Error("invalid_response")
            sessionManager.saveSession(token, user.id, user.login)
            AuthResult.Success(user)
        } catch (error: PasskeyAuthException) {
            AuthResult.Error(error.code)
        } catch (_: Exception) {
            AuthResult.Error("passkey_failed")
        }
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
