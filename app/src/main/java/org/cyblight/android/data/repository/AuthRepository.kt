package org.cyblight.android.data.repository

import android.app.Activity
import org.cyblight.android.auth.PasskeyAuthException
import org.cyblight.android.auth.PasskeyAuthHelper
import org.cyblight.android.data.api.CybLightApi
import org.cyblight.android.data.api.MeUserDto
import org.cyblight.android.data.api.LoginRequest
import org.cyblight.android.data.api.PasskeyLoginRequest
import org.cyblight.android.data.api.PasskeyOptionsRequest
import org.cyblight.android.data.api.TwoFactorRequest
import org.cyblight.android.data.api.UserDto
import org.cyblight.android.data.extractAuthToken
import org.cyblight.android.data.session.SessionManager
import retrofit2.HttpException

sealed class AuthResult {
    data class Success(val user: UserDto) : AuthResult()
    data class Requires2FA(val userId: String) : AuthResult()
    data class Error(val code: String) : AuthResult()
}

enum class SessionRefreshResult {
    Valid,
    Expired,
    Offline,
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

            val user = resolveUser(body.user, api.me().user)
                ?: return AuthResult.Error("invalid_response")
            sessionManager.saveSession(token, user.id, user.login)
            AuthResult.Success(user)
        } catch (error: PasskeyAuthException) {
            AuthResult.Error(error.code)
        } catch (_: Exception) {
            AuthResult.Error("passkey_failed")
        }
    }

    suspend fun refreshSession(): SessionRefreshResult {
        val token = sessionManager.getToken()
        if (token.isNullOrBlank()) return SessionRefreshResult.Expired

        return try {
            val response = api.refreshSession()
            when {
                response.isSuccessful && response.body()?.ok == true -> SessionRefreshResult.Valid
                response.code() == 401 || response.code() == 403 -> {
                    sessionManager.clearAndNotifyExpired()
                    SessionRefreshResult.Expired
                }
                else -> SessionRefreshResult.Offline
            }
        } catch (error: HttpException) {
            if (error.code() == 401 || error.code() == 403) {
                sessionManager.clearAndNotifyExpired()
                SessionRefreshResult.Expired
            } else {
                SessionRefreshResult.Offline
            }
        } catch (_: Exception) {
            SessionRefreshResult.Offline
        }
    }

    suspend fun restoreSession(): UserDto? {
        val token = sessionManager.getToken() ?: return null
        if (token.isBlank()) return null

        return try {
            val me = api.me()
            when {
                me.ok && me.user != null -> UserDto(
                    id = me.user.id,
                    login = me.user.login,
                )
                me.ok -> readCachedUser()
                else -> {
                    sessionManager.clearAndNotifyExpired()
                    null
                }
            }
        } catch (error: HttpException) {
            if (error.code() == 401 || error.code() == 403) {
                sessionManager.clearAndNotifyExpired()
            }
            null
        } catch (_: Exception) {
            readCachedUser()
        }
    }

    private suspend fun readCachedUser(): UserDto? {
        val userId = sessionManager.getUserId()?.trim().orEmpty()
        val login = sessionManager.currentLogin()?.trim().orEmpty()
        if (userId.isBlank() || login.isBlank()) return null
        return UserDto(id = userId, login = login)
    }

    suspend fun logout() {
        runCatching { api.logout() }
        sessionManager.clear()
    }

    private fun resolveUser(partial: UserDto?, meUser: MeUserDto?): UserDto? {
        if (meUser != null && meUser.id.isNotBlank() && meUser.login.isNotBlank()) {
            return UserDto(id = meUser.id, login = meUser.login)
        }
        if (partial != null && partial.id.isNotBlank() && partial.login.isNotBlank()) {
            return partial
        }
        return null
    }

    private fun parseErrorBody(raw: String?): String {
        if (raw.isNullOrBlank()) return "unknown_error"
        val match = Regex(""""error"\s*:\s*"([^"]+)"""").find(raw)
        return match?.groupValues?.getOrNull(1) ?: "unknown_error"
    }
}
