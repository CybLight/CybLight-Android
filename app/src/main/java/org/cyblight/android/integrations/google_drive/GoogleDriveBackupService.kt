package org.cyblight.android.integrations.google_drive

import android.content.Context
import android.content.Intent
import org.cyblight.android.crypto.backup.BACKUP_PAYLOAD_VERSION_V2
import org.cyblight.android.crypto.backup.BackupRestoreStats
import org.cyblight.android.crypto.backup.CyblightBackupManager
import org.cyblight.android.data.api.ChatsExportPayload
import org.cyblight.android.data.repository.ChatsImportStats
import org.cyblight.android.data.repository.MessagesRepository
import org.cyblight.android.crypto.SignalCryptoManager
import org.cyblight.android.data.ApiClient
import org.cyblight.android.data.session.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

typealias DriveBackupProgress = (percent: Int, labelKey: String) -> Unit

class GoogleDriveBackupService(
    context: Context,
    private val backupManager: CyblightBackupManager,
    private val messagesRepository: MessagesRepository,
    private val authManager: GoogleDriveAuthManager = GoogleDriveAuthManager(context),
    private val driveClient: GoogleDriveClient = GoogleDriveClient(),
) {
    fun isConfigured(): Boolean = GoogleDriveConfig.isConfigured()

    fun hasSession(): Boolean = authManager.hasSession()

    fun getAccountLabel(): String? = authManager.getAccountLabel()

    fun getAccountEmail(): String? = authManager.getAccountEmail()

    fun getDeviceGoogleAccountEmails(): List<String> = authManager.getDeviceGoogleAccountEmails()

    fun getSignInIntent(preferredEmail: String? = null): Intent = authManager.getSignInIntent(preferredEmail)

    suspend fun signOutIfCurrentNot(email: String) = authManager.signOutIfCurrentNot(email)

    suspend fun handleSignInResult(data: Intent?): Result<Unit> = authManager.handleSignInResult(data)

    suspend fun signOut() {
        authManager.signOut()
    }

    suspend fun fetchMetadata(userId: String): DriveBackupMetadata? = withContext(Dispatchers.IO) {
        if (!hasSession()) return@withContext null
        val accessToken = authManager.getAccessToken()
        val file = driveClient.findBackupFile(accessToken, userId) ?: return@withContext null
        DriveBackupMetadata(file = file)
    }

    suspend fun fetchStorageQuota(): GoogleDriveStorageQuota? = withContext(Dispatchers.IO) {
        if (!hasSession()) return@withContext null
        runCatching {
            driveClient.fetchStorageQuota(authManager.getAccessToken())
        }.getOrNull()
    }

    suspend fun uploadBackup(
        userId: String,
        login: String,
        password: String,
        onProgress: DriveBackupProgress = { _, _ -> },
    ): DriveBackupFile = withContext(Dispatchers.IO) {
        onProgress(2, "progress_auth")
        val accessToken = authManager.getAccessToken()
        onProgress(10, "progress_create")
        val chats = messagesRepository.fetchChatsExportPayload()
        val content = backupManager.createBackupFile(userId, password, chats)
        onProgress(48, "progress_upload")
        val file = driveClient.uploadBackupFile(accessToken, userId, login, content)
        onProgress(100, "progress_done")
        file
    }

    suspend fun restoreBackup(
        userId: String,
        password: String,
        onProgress: DriveBackupProgress = { _, _ -> },
    ): BackupRestoreStats = withContext(Dispatchers.IO) {
        onProgress(2, "progress_auth")
        val accessToken = authManager.getAccessToken()
        onProgress(10, "progress_find")
        val driveFile = driveClient.findBackupFile(accessToken, userId)
            ?: throw IllegalStateException("google_drive_no_backup")
        onProgress(18, "progress_download")
        val raw = driveClient.downloadBackupFile(accessToken, driveFile.id)
        onProgress(32, "progress_restore")
        
        // Decrypt and process
        val payload = backupManager.decryptBackupPayload(raw, password)
        backupManager.restorePayload(userId, payload, onProgress)
        
        var stats = BackupRestoreStats()
        val chats = payload.chats
        if (payload.version == BACKUP_PAYLOAD_VERSION_V2 && chats != null) {
            onProgress(80, "progress_chats")
            stats = importChats(chats)
        }
        
        // Clear references and hint GC for large backups
        if (raw.length > 1024 * 1024) {
            System.gc()
        }

        onProgress(100, "progress_done")
        stats
    }

    suspend fun deleteBackup(userId: String): Boolean = withContext(Dispatchers.IO) {
        val accessToken = authManager.getAccessToken()
        driveClient.deleteBackupFile(accessToken, userId)
    }

    private suspend fun importChats(chats: ChatsExportPayload): BackupRestoreStats {
        val result = messagesRepository.importChatsPayload(chats)
        return result.getOrElse { error ->
            val code = error.message?.substringBefore(':')?.trim()?.takeIf { it.isNotBlank() }
                ?: "chats_import_failed"
            throw IllegalStateException(code)
        }.let { stats ->
            BackupRestoreStats(
                chatsImported = stats.imported,
                chatsSkipped = stats.skipped,
                chatsErrors = stats.errors,
            )
        }
    }

    companion object {
        fun create(context: Context): GoogleDriveBackupService {
            val appContext = context.applicationContext
            val sessionManager = SessionManager(appContext)
            val api = ApiClient.create(sessionManager)
            val signalCrypto = SignalCryptoManager(appContext, api)
            val messagesRepository = MessagesRepository(appContext, api, signalCrypto) {
                sessionManager.getUserId()
            }
            return GoogleDriveBackupService(
                context = appContext,
                backupManager = CyblightBackupManager(appContext),
                messagesRepository = messagesRepository,
            )
        }
    }
}
