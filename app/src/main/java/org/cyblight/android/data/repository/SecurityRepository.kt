package org.cyblight.android.data.repository

import android.app.Activity
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.cyblight.android.auth.PasskeyAuthException
import org.cyblight.android.auth.PasskeyAuthHelper
import org.cyblight.android.data.api.CybLightApi
import org.cyblight.android.data.api.LoginHistoryEntryDto
import org.cyblight.android.data.api.PasskeyDto
import org.cyblight.android.data.api.PasskeyRegisterRequest
import org.cyblight.android.data.api.TrustedDeviceDto

data class SecurityOverview(
    val email: String?,
    val emailVerified: Boolean,
    val pendingEmail: String?,
    val pendingEmailVerifiedAt: Long?,
    val passChangedAt: Long?,
    val totpEnabled: Boolean,
    val passkeyCount: Int,
) {
    val securityScore: Int
        get() = (if (emailVerified) 30 else 0) +
            (if (totpEnabled) 40 else 0) +
            (if (passkeyCount > 0) 30 else 0)
}

class SecurityRepository(private val api: CybLightApi) {
    suspend fun loadOverview(): Result<SecurityOverview> = runCatching {
        coroutineScope {
            val meDeferred = async { api.me() }
            val passkeysDeferred = async { api.passkeyList() }

            val me = meDeferred.await()
            if (!me.ok || me.user == null) {
                throw IllegalStateException(me.error ?: "me_failed")
            }

            val passkeys = passkeysDeferred.await()
            val passkeyCount = if (passkeys.ok) passkeys.passkeys.size else 0
            val user = me.user

            SecurityOverview(
                email = user.email,
                emailVerified = user.emailVerified,
                pendingEmail = user.pendingEmail,
                pendingEmailVerifiedAt = user.pendingEmailVerifiedAt,
                passChangedAt = user.passChangedAt,
                totpEnabled = user.totpEnabled,
                passkeyCount = passkeyCount,
            )
        }
    }

    suspend fun loadPasskeys(): Result<List<PasskeyDto>> = runCatching {
        val response = api.passkeyList()
        if (!response.ok) {
            throw IllegalStateException(response.error ?: "passkeys_failed")
        }
        response.passkeys
    }

    suspend fun registerPasskey(activity: Activity, name: String): Result<Unit> {
        return try {
            val optionsResponse = api.passkeyRegisterOptions()
            if (!optionsResponse.ok || optionsResponse.options == null) {
                return Result.failure(IllegalStateException(optionsResponse.error ?: "passkey_failed"))
            }

            val credential = PasskeyAuthHelper.createCredential(activity, optionsResponse.options)
            val registerResponse = api.passkeyRegister(
                PasskeyRegisterRequest(
                    credential = credential,
                    name = name.ifBlank { null },
                ),
            )
            if (!registerResponse.ok) {
                return Result.failure(IllegalStateException(registerResponse.error ?: "passkey_failed"))
            }
            Result.success(Unit)
        } catch (error: PasskeyAuthException) {
            Result.failure(error)
        } catch (error: Exception) {
            Result.failure(IllegalStateException(error.toPasskeyErrorCode()))
        }
    }

    suspend fun deletePasskey(passkeyId: String): Result<Unit> = runCatching {
        val response = api.deletePasskey(passkeyId)
        if (!response.ok) {
            throw IllegalStateException(response.error ?: "passkey_remove_failed")
        }
        Unit
    }

    suspend fun loadTrustedDevices(): Result<List<TrustedDeviceDto>> = runCatching {
        val response = api.trustedDevices()
        if (!response.ok) {
            throw IllegalStateException(response.error ?: "trusted_devices_failed")
        }
        response.devices
    }

    suspend fun removeTrustedDevice(deviceId: String): Result<Unit> = runCatching {
        val response = api.removeTrustedDevice(deviceId)
        if (!response.ok) {
            throw IllegalStateException(response.error ?: "trusted_device_remove_failed")
        }
    }

    suspend fun loadLoginHistory(): Result<List<LoginHistoryEntryDto>> = runCatching {
        val response = api.loginHistory()
        if (!response.ok) {
            throw IllegalStateException(response.error ?: "login_history_failed")
        }
        response.history
    }
}
