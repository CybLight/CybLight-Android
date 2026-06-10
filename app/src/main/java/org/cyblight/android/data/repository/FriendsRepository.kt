package org.cyblight.android.data.repository

import org.cyblight.android.data.api.CybLightApi
import org.cyblight.android.data.api.FriendDto

class FriendsRepository(private val api: CybLightApi) {
    suspend fun loadFriends(): Result<List<FriendDto>> {
        val response = api.friendsList()
        return if (response.ok) {
            Result.success(response.friends)
        } else {
            Result.failure(Exception(response.error ?: "friends_load_failed"))
        }
    }
}
