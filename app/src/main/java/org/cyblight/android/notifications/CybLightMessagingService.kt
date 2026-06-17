package org.cyblight.android.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.runBlocking
import org.cyblight.android.data.preferences.AppPreferences
import org.cyblight.android.util.SystemSettings

class CybLightMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        PushTokenRegistrar.registerToken(applicationContext, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        if (data["type"] != "chat_message") return

        val friendId = data["friendId"]?.trim().orEmpty()
        if (friendId.isEmpty()) return

        if (!SystemSettings.areNotificationsEnabled(applicationContext)) return
        if (!runBlocking { AppPreferences(applicationContext).getMessageAlertsEnabled() }) return

        val activeChatId = runBlocking {
            AppPreferences(applicationContext).getActiveChatFriendId()
        }
        if (activeChatId == friendId) return

        val friendName = data["friendName"]?.trim().orEmpty().ifBlank { friendId }
        val preview = runBlocking {
            PushMessagePreviewResolver.resolvePreview(applicationContext, data)
        }

        NotificationHelper.showNewMessageAlert(
            context = applicationContext,
            friendId = friendId,
            friendName = friendName,
            preview = preview,
        )
    }
}
