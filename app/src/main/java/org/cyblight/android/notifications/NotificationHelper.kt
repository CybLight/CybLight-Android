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
    const val NOTIFICATION_LOGIN = 1001

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_SECURITY,
            context.getString(R.string.notification_channel_security),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notification_channel_security_desc)
        }
        manager.createNotificationChannel(channel)
    }

    fun showNewLoginAlert(
        context: Context,
        ip: String?,
        userAgent: String?,
    ) {
        ensureChannels(context)
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
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
}
