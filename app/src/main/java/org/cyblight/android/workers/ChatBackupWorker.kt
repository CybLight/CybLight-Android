package org.cyblight.android.workers

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import org.cyblight.android.data.preferences.AppPreferences
import org.cyblight.android.data.preferences.ChatBackupFrequency
import org.cyblight.android.data.session.SessionManager
import org.cyblight.android.integrations.google_drive.GoogleDriveBackupService
import org.cyblight.android.integrations.google_drive.GoogleDriveConfig
import org.cyblight.android.security.BackupPasswordStore
import java.util.concurrent.TimeUnit

class ChatBackupWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val appPreferences = AppPreferences(applicationContext)
        val frequency = appPreferences.getChatBackupFrequency()
        if (frequency == ChatBackupFrequency.OFF) {
            return Result.success()
        }
        if (!GoogleDriveConfig.isConfigured()) {
            return Result.success()
        }

        val sessionManager = SessionManager(applicationContext)
        val userId = sessionManager.getUserId()?.takeIf { it.isNotBlank() } ?: return Result.success()
        if (sessionManager.getToken().isNullOrBlank()) {
            return Result.success()
        }
        val login = sessionManager.currentLogin()?.takeIf { it.isNotBlank() } ?: return Result.success()

        val backupService = GoogleDriveBackupService.create(applicationContext)
        if (!backupService.hasSession()) {
            return Result.success()
        }

        val password = BackupPasswordStore(applicationContext).getPassword()
        if (password.isNullOrBlank()) {
            return Result.success()
        }

        val lastSuccess = appPreferences.getLastAutoBackupSuccessMs()
        val minInterval = frequency.intervalMs
        if (lastSuccess > 0 && System.currentTimeMillis() - lastSuccess < minInterval * 9 / 10) {
            return Result.success()
        }

        return try {
            backupService.uploadBackup(userId, login, password)
            appPreferences.setLastAutoBackupSuccessMs(System.currentTimeMillis())
            Result.success()
        } catch (_: IllegalStateException) {
            Result.success()
        } catch (_: IllegalArgumentException) {
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "chat_backup_periodic"

        fun schedule(context: Context, frequency: ChatBackupFrequency, overCellular: Boolean) {
            if (frequency == ChatBackupFrequency.OFF) {
                cancel(context)
                return
            }

            val networkType = if (overCellular) {
                NetworkType.CONNECTED
            } else {
                NetworkType.UNMETERED
            }
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(networkType)
                .build()

            val request = PeriodicWorkRequestBuilder<ChatBackupWorker>(
                frequency.periodDays,
                TimeUnit.DAYS,
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context.applicationContext).cancelUniqueWork(WORK_NAME)
        }
    }
}
