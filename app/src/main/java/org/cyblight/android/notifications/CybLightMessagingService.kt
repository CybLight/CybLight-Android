package org.cyblight.android.notifications

import android.content.Context
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.cyblight.android.data.preferences.AppPreferences
import kotlinx.coroutines.runBlocking

class CybLightMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        PushTokenRegistrar.registerToken(applicationContext, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        if (data["type"] != "chat_message") return

        val friendId = data["friendId"]?.trim().orEmpty()
        if (friendId.isEmpty()) return

        val friendName = data["friendName"]?.trim().orEmpty().ifBlank { friendId }
        val preview = message.notification?.body
            ?: data["body"]?.trim().orEmpty()
            ?: message.notification?.title
            ?: ""

        val activeChatId = runBlocking {
            AppPreferences(applicationContext).getActiveChatFriendId()
        }
        if (activeChatId == friendId) return

        NotificationHelper.showNewMessageAlert(
            context = applicationContext,
            friendId = friendId,
            friendName = friendName,
            preview = preview,
        )
    }
}
