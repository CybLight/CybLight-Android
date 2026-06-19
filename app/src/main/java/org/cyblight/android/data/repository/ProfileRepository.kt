package org.cyblight.android.data.repository

import org.cyblight.android.data.api.CybLightApi
import org.cyblight.android.data.api.EasterFlagsDto
import org.cyblight.android.data.api.FormatMirrorTouchResponse
import org.cyblight.android.data.api.FriendDto
import org.cyblight.android.data.api.ProfileDto
import org.cyblight.android.data.api.ProfileResponse
import retrofit2.HttpException

class ProfileRepository(private val api: CybLightApi) {
    suspend fun loadOwnProfile(): Result<ProfileDto> {
        loadProfileOrNull { api.myProfile() }?.let { return Result.success(it) }
        return loadOwnProfileFallback()
    }

    suspend fun loadProfile(username: String): Result<ProfileDto> {
        val trimmed = username.trim()
        loadProfileOrNull { api.profile(trimmed) }?.let { return Result.success(it) }
        return loadPublicProfileFallback(trimmed)
    }

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

    suspend fun unlockLightCatcher(): Result<Unit> = unlockEaster { api.unlockLightCatcher().ok }

    suspend fun unlockNightGuard(): Result<Unit> = unlockEaster { api.unlockNightGuard().ok }

    suspend fun unlockTrustedFingerprint(): Result<Unit> = unlockEaster { api.unlockTrustedFingerprint().ok }

    suspend fun unlockEcho(): Result<Unit> = unlockEaster { api.unlockEcho().ok }

    suspend fun unlockArchivist(): Result<Unit> = unlockEaster { api.unlockArchivist().ok }

    suspend fun unlockTypographer(): Result<Unit> = unlockEaster { api.unlockTypographer().ok }

    suspend fun unlockSpoilerHunter(): Result<Unit> = unlockEaster { api.unlockSpoilerHunter().ok }

    suspend fun unlockNoMarkers(): Result<Unit> = unlockEaster { api.unlockNoMarkers().ok }

    suspend fun unlockEnterMaster(): Result<Unit> = unlockEaster { api.unlockEnterMaster().ok }

    suspend fun unlockFontExtremes(): Result<Unit> = unlockEaster { api.unlockFontExtremes().ok }

    suspend fun unlockCloudKeeper(): Result<Unit> = unlockEaster { api.unlockCloudKeeper().ok }

    suspend fun unlockDrivePilot(): Result<Unit> = unlockEaster { api.unlockDrivePilot().ok }

    suspend fun unlockLiveWire(): Result<Unit> = unlockEaster { api.unlockLiveWire().ok }

    suspend fun unlockFromShadow(): Result<Unit> = unlockEaster { api.unlockFromShadow().ok }

    suspend fun unlockWatchman(): Result<Unit> = unlockEaster { api.unlockWatchman().ok }

    suspend fun unlockCarouselWatcher(): Result<Unit> = unlockEaster { api.unlockCarouselWatcher().ok }

    suspend fun unlockSynchronist(): Result<Unit> = unlockEaster { api.unlockSynchronist().ok }

    suspend fun unlockQuoteDay(): Result<Unit> = unlockEaster { api.unlockQuoteDay().ok }

    suspend fun unlockMidnightEditor(): Result<Unit> = unlockEaster { api.unlockMidnightEditor().ok }

    suspend fun unlockPolyglotFriend(): Result<Unit> = unlockEaster { api.unlockPolyglotFriend().ok }

    suspend fun unlockSilence(): Result<Unit> = unlockEaster { api.unlockSilence().ok }

    suspend fun unlockReactionStreak(): Result<Unit> = unlockEaster { api.unlockReactionStreak().ok }

    suspend fun touchFormatApp(): Result<FormatMirrorTouchResponse> {
        return try {
            val response = api.touchFormatApp()
            if (response.ok) Result.success(response) else Result.failure(Exception("format_touch_failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun unlockEaster(call: suspend () -> Boolean): Result<Unit> {
        return try {
            if (call()) Result.success(Unit) else Result.failure(Exception("easter_unlock_failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun loadProfileOrNull(
        request: suspend () -> ProfileResponse,
    ): ProfileDto? {
        return try {
            val response = request()
            if (response.ok) response.profile else null
        } catch (e: HttpException) {
            if (e.code() == 404) null else throw e
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun loadOwnProfileFallback(): Result<ProfileDto> {
        return try {
            val me = api.me()
            val user = me.user
            if (!me.ok || user == null) {
                return Result.failure(Exception(me.error ?: "profile_load_failed"))
            }

            val friendsCount = runCatching { api.friendsList().friends.size }.getOrDefault(0)

            Result.success(
                ProfileDto(
                    id = user.id,
                    username = user.login,
                    createdAt = user.createdAt,
                    friendsCount = friendsCount,
                    verified = user.emailVerified,
                    role = user.role,
                ),
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun loadPublicProfileFallback(username: String): Result<ProfileDto> {
        return try {
            val friends = api.friendsList()
            friends.friends
                .firstOrNull { it.username.equals(username, ignoreCase = true) }
                ?.let { return Result.success(it.toProfileDto()) }

            if (username.length >= 2) {
                val search = api.searchUsers(username)
                search.users
                    .firstOrNull { it.username.equals(username, ignoreCase = true) }
                    ?.let { return Result.success(it.toProfileDto()) }
            }

            Result.failure(Exception("profile_load_failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun FriendDto.toProfileDto(): ProfileDto = ProfileDto(
        id = id,
        username = username,
        avatar = avatar,
        createdAt = createdAt ?: 0L,
        isOnline = isOnline,
        lastSeenAt = lastSeenAt,
    )
}
