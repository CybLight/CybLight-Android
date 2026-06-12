package org.cyblight.android.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import org.cyblight.android.notifications.MessageNotificationMonitor
import java.util.concurrent.TimeUnit

class MessageNotificationWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            MessageNotificationMonitor(applicationContext).checkForNewMessages()
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "message_notification_poll"
        private const val IMMEDIATE_WORK_NAME = "message_notification_immediate"

        fun schedule(context: Context) {
            val periodic = PeriodicWorkRequestBuilder<MessageNotificationWorker>(15, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodic,
            )
        }

        fun scheduleImmediate(context: Context) {
            val immediate = OneTimeWorkRequestBuilder<MessageNotificationWorker>()
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                androidx.work.ExistingWorkPolicy.REPLACE,
                immediate,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            WorkManager.getInstance(context).cancelUniqueWork(IMMEDIATE_WORK_NAME)
        }
    }
}
