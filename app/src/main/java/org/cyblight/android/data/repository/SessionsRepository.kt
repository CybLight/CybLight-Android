package org.cyblight.android.data.repository

import org.cyblight.android.data.api.CybLightApi
import org.cyblight.android.data.api.RevokeSessionRequest
import org.cyblight.android.data.api.SessionDto

data class SessionsSnapshot(
    val currentSessionId: String?,
    val sessions: List<SessionDto>,
)

class SessionsRepository(private val api: CybLightApi) {
    suspend fun loadSessions(): Result<SessionsSnapshot> {
        return try {
            val response = api.sessions()
            val data = response.data
            if (response.ok && data != null) {
                Result.success(
                    SessionsSnapshot(
                        currentSessionId = data.current,
                        sessions = data.sessions,
                    ),
                )
            } else {
                Result.failure(Exception(response.error ?: "sessions_load_failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun revokeSession(sessionId: String): Result<Boolean> {
        return try {
            val response = api.revokeSession(RevokeSessionRequest(id = sessionId))
            val data = response.data
            if (response.ok && data != null && data.removed > 0) {
                Result.success(data.loggedOut)
            } else {
                Result.failure(Exception(response.error ?: "session_revoke_failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
