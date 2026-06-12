package org.cyblight.android.data.repository

import org.cyblight.android.data.api.CybLightApi
import org.cyblight.android.data.api.EasterFlagsDto
import org.cyblight.android.data.api.ProfileDto

class ProfileRepository(private val api: CybLightApi) {
    suspend fun loadOwnProfile(): Result<ProfileDto> = loadProfile { api.myProfile() }

    suspend fun loadProfile(username: String): Result<ProfileDto> =
        loadProfile { api.profile(username.trim()) }

    suspend fun loadEasterFlags(): Result<EasterFlagsDto> {
        return try {
            val response = api.me()
            if (response.ok) {
                Result.success(response.user?.easter ?: EasterFlagsDto())
            } else {
                Result.failure(Exception(response.error ?: "easter_load_failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unlockLightCatcher(): Result<Unit> {
        return try {
            val response = api.unlockLightCatcher()
            if (response.ok && response.lightCatcher) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.error ?: "easter_unlock_failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun loadProfile(
        request: suspend () -> org.cyblight.android.data.api.ProfileResponse,
    ): Result<ProfileDto> {
        return try {
            val response = request()
            val profile = response.profile
            if (response.ok && profile != null) {
                Result.success(profile)
            } else {
                Result.failure(Exception(response.error ?: "profile_load_failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
