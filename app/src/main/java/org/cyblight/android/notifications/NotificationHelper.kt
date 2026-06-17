package org.cyblight.android.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.cyblight.android.MainActivity
import org.cyblight.android.R

object NotificationHelper {
    const val CHANNEL_SECURITY = "cyblight_security"
    const val CHANNEL_MESSAGES = "cyblight_messages"
    const val NOTIFICATION_LOGIN = 1001
    const val NOTIFICATION_MESSAGE_BASE = 2000

    const val EXTRA_CHAT_FRIEND_ID = "extra_chat_friend_id"
    const val EXTRA_CHAT_FRIEND_NAME = "extra_chat_friend_name"
    const val EXTRA_OPEN_SESSIONS = "extra_open_sessions"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val securityChannel = NotificationChannel(
            CHANNEL_SECURITY,
            context.getString(R.string.notification_channel_security),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notification_channel_security_desc)
        }

        val messagesChannel = NotificationChannel(
            CHANNEL_MESSAGES,
            context.getString(R.string.notification_channel_messages),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notification_channel_messages_desc)
        }

        manager.createNotificationChannel(securityChannel)
        manager.createNotificationChannel(messagesChannel)
    }

    fun showNewLoginAlert(
        context: Context,
        ip: String?,
        userAgent: String?,
    ) {
        ensureChannels(context)
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_SESSIONS, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_LOGIN,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val device = userAgent?.take(80)?.ifBlank { null }
            ?: context.getString(R.string.notification_login_unknown_device)
        val ipText = ip?.takeIf { it.isNotBlank() } ?: "—"

        val notification = NotificationCompat.Builder(context, CHANNEL_SECURITY)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.notification_login_title))
            .setContentText(context.getString(R.string.notification_login_body, ipText))
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    context.getString(R.string.notification_login_big_text, ipText, device),
                ),
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_LOGIN, notification)
    }

    fun showNewMessageAlert(
        context: Context,
        friendId: String,
        friendName: String,
        preview: String,
        unreadCount: Int = 1,
    ) {
        ensureChannels(context)
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_CHAT_FRIEND_ID, friendId)
            putExtra(EXTRA_CHAT_FRIEND_NAME, friendName)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            friendId.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title = friendName.ifBlank { context.getString(R.string.notification_message_title_fallback) }
        val body = if (unreadCount > 1) {
            context.getString(R.string.notification_message_body_many, unreadCount, preview)
        } else {
            preview.ifBlank { context.getString(R.string.notification_message_body_fallback) }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .build()

        NotificationManagerCompat.from(context).notify(notificationIdForFriend(friendId), notification)
    }

    fun cancelMessageNotification(context: Context, friendId: String) {
        NotificationManagerCompat.from(context).cancel(notificationIdForFriend(friendId))
    }

    fun notificationIdForFriend(friendId: String): Int =
        NOTIFICATION_MESSAGE_BASE + (friendId.hashCode() and 0x7FFF)
}
