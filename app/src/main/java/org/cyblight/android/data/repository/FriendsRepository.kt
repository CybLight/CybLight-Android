package org.cyblight.android.data.repository

import org.cyblight.android.data.api.AddFriendRequest
import org.cyblight.android.data.api.CybLightApi
import org.cyblight.android.data.api.FriendDto
import org.cyblight.android.data.api.FriendIdRequest

class FriendsRepository(private val api: CybLightApi) {
    suspend fun loadFriends(): Result<List<FriendDto>> {
        return try {
            val response = api.friendsList()
            if (response.ok) {
                Result.success(response.friends)
            } else {
                Result.failure(Exception(response.error ?: "friends_load_failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadPendingRequests(): Result<List<FriendDto>> {
        return try {
            val response = api.pendingFriends()
            if (response.ok) {
                Result.success(response.pendingRequests)
            } else {
                Result.failure(Exception(response.error ?: "pending_load_failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadSentRequests(): Result<List<FriendDto>> {
        return try {
            val response = api.sentFriends()
            if (response.ok) {
                Result.success(response.sentRequests)
            } else {
                Result.failure(Exception(response.error ?: "sent_load_failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadFriendPresence(userId: String): Result<FriendDto> {
        return try {
            val response = api.friendPresence(userId)
            if (response.ok) {
                Result.success(
                    FriendDto(
                        id = userId,
                        username = "",
                        isOnline = response.isOnline,
                        lastSeenAt = response.lastSeenAt,
                    ),
                )
            } else {
                Result.failure(Exception(response.error ?: "presence_load_failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchUsers(query: String): Result<List<FriendDto>> {
        return try {
            val response = api.searchUsers(query.trim())
            if (response.ok) {
                Result.success(response.users)
            } else {
                Result.failure(Exception(response.error ?: "search_failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addFriend(username: String): Result<Unit> {
        return friendAction {
            api.addFriend(AddFriendRequest(friendUsername = username))
        }
    }

    suspend fun acceptFriend(friendId: String): Result<Unit> {
        return friendAction {
            api.acceptFriend(FriendIdRequest(friendId = friendId))
        }
    }

    suspend fun rejectFriend(friendId: String): Result<Unit> {
        return friendAction {
            api.rejectFriend(FriendIdRequest(friendId = friendId))
        }
    }

    suspend fun removeFriend(friendId: String): Result<Unit> {
        return friendAction {
            api.removeFriend(FriendIdRequest(friendId = friendId))
        }
    }

    private suspend fun friendAction(call: suspend () -> org.cyblight.android.data.api.FriendActionResponse): Result<Unit> {
        return try {
            val response = call()
            if (response.ok) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.error ?: "friend_action_failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
