package org.cyblight.android.notifications

import android.content.Context
import org.cyblight.android.data.ApiClient
import org.cyblight.android.data.preferences.AppPreferences
import org.cyblight.android.data.session.SessionManager
import org.cyblight.android.util.SystemSettings

class LoginNotificationMonitor(
    private val context: Context,
) {
    private val sessionManager = SessionManager(context)
    private val appPreferences = AppPreferences(context)

    suspend fun checkForNewLogin() {
        if (!SystemSettings.areNotificationsEnabled(context)) return
        if (!appPreferences.getLoginAlertsEnabled()) return
        if (sessionManager.getToken().isNullOrBlank()) return

        val api = ApiClient.create(sessionManager)
        val response = runCatching { api.loginHistory(limit = 10) }.getOrNull() ?: return
        if (!response.ok) return

        val latestLogin = response.history.firstOrNull { entry ->
            entry.action == "login_success" ||
                entry.action == "login_2fa" ||
                entry.action == "passkey_login"
        } ?: return

        val ignoreUntil = appPreferences.getIgnoreLoginEventsUntil()
        if (System.currentTimeMillis() < ignoreUntil) {
            appPreferences.setLastSeenLoginEventId(latestLogin.id)
            return
        }

        val lastSeenId = appPreferences.getLastSeenLoginEventId()
        if (lastSeenId == null) {
            appPreferences.setLastSeenLoginEventId(latestLogin.id)
            return
        }
        if (latestLogin.id == lastSeenId) return

        appPreferences.setLastSeenLoginEventId(latestLogin.id)
        NotificationHelper.showNewLoginAlert(
            context = context,
            ip = latestLogin.ip,
            userAgent = latestLogin.userAgent,
        )
    }

    suspend fun markOwnLoginGracePeriod() {
        appPreferences.setIgnoreLoginEventsUntil(System.currentTimeMillis() + OWN_LOGIN_GRACE_MS)
        runCatching {
            val api = ApiClient.create(sessionManager)
            val response = api.loginHistory(limit = 5)
            if (response.ok) {
                val latest = response.history.firstOrNull { it.action == "login_success" ||
                    it.action == "login_2fa" ||
                    it.action == "passkey_login"
                }
                if (latest != null) {
                    appPreferences.setLastSeenLoginEventId(latest.id)
                }
            }
        }
    }

    companion object {
        private const val OWN_LOGIN_GRACE_MS = 20_000L
    }
}
