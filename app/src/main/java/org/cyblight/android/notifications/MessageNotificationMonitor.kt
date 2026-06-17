package org.cyblight.android.notifications

import android.content.Context
import org.cyblight.android.crypto.SignalCryptoManager
import org.cyblight.android.data.ApiClient
import org.cyblight.android.data.repository.ConversationPreviewEnricher
import org.cyblight.android.data.preferences.AppPreferences
import org.cyblight.android.data.session.SessionManager
import org.cyblight.android.util.SystemSettings

class MessageNotificationMonitor(
    private val context: Context,
) {
    private val sessionManager = SessionManager(context)
    private val appPreferences = AppPreferences(context)

    suspend fun checkForNewMessages() {
        if (!SystemSettings.areNotificationsEnabled(context)) return
        if (!appPreferences.getMessageAlertsEnabled()) return
        if (sessionManager.getToken().isNullOrBlank()) return

        val api = ApiClient.create(sessionManager)
        val response = runCatching { api.unreadSummary() }.getOrNull() ?: return
        if (!response.ok) return

        val userId = sessionManager.getUserId()?.trim().orEmpty()
        val enrichedPreviews = if (userId.isNotBlank()) {
            ConversationPreviewEnricher.enrichPreviews(
                context = context,
                userId = userId,
                previews = response.conversationPreviews,
                signalCrypto = SignalCryptoManager(context, api),
            )
        } else {
            emptyMap()
        }

        val activeChatId = appPreferences.getActiveChatFriendId()
        val previous = appPreferences.getLastNotifiedUnreadCounts()
        val next = mutableMapOf<String, Int>()

        response.unreadDetails.forEach { detail ->
            val senderId = detail.senderId
            val unreadCount = detail.unreadCount.coerceAtLeast(0)
            next[senderId] = unreadCount

            if (senderId.isBlank() || unreadCount <= 0) return@forEach
            if (senderId == activeChatId) return@forEach

            val previousCount = previous[senderId] ?: 0
            if (unreadCount <= previousCount) return@forEach

            val preview = enrichedPreviews[senderId]
                ?: detail.preview.takeIf { it.isNotBlank() }

            NotificationHelper.showNewMessageAlert(
                context = context,
                friendId = senderId,
                friendName = detail.senderLogin,
                preview = preview.orEmpty(),
                unreadCount = unreadCount,
            )
        }

        appPreferences.setLastNotifiedUnreadCounts(next)
    }

    suspend fun syncBaselineFromServer() {
        if (sessionManager.getToken().isNullOrBlank()) return
        val api = ApiClient.create(sessionManager)
        val response = runCatching { api.unreadSummary() }.getOrNull() ?: return
        if (!response.ok) return

        val baseline = response.unreadDetails.associate { it.senderId to it.unreadCount.coerceAtLeast(0) }
        appPreferences.setLastNotifiedUnreadCounts(baseline)
    }

    suspend fun clearFriendNotification(friendId: String) {
        NotificationHelper.cancelMessageNotification(context, friendId)
        val current = appPreferences.getLastNotifiedUnreadCounts().toMutableMap()
        val api = ApiClient.create(sessionManager)
        val response = runCatching { api.unreadSummary() }.getOrNull()
        val unread = response?.unreadDetails?.find { it.senderId == friendId }?.unreadCount ?: 0
        current[friendId] = unread.coerceAtLeast(0)
        appPreferences.setLastNotifiedUnreadCounts(current)
    }
}
