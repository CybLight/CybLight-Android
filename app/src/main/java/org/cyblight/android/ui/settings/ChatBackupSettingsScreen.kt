package org.cyblight.android.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.cyblight.android.R
import org.cyblight.android.crypto.backup.BackupRestoreStats
import org.cyblight.android.data.preferences.ChatBackupFrequency
import org.cyblight.android.integrations.google_drive.DriveBackupMetadata
import org.cyblight.android.ui.components.CybOutlinedTextField
import java.text.DateFormat
import java.util.Locale

@Composable
fun ChatBackupSettingsScreen(
    accountLogin: String,
    isGoogleDriveConfigured: Boolean,
    googleDriveAccountLabel: String?,
    googleDriveBackupMetadata: DriveBackupMetadata?,
    onRefreshGoogleDriveStatus: suspend () -> Unit,
    onGoogleDriveSignIn: () -> Unit,
    onGoogleDriveSignOut: suspend () -> Unit,
    onUploadGoogleDriveBackup: suspend (password: String, onProgress: (Int, String) -> Unit) -> Result<Unit>,
    onRestoreGoogleDriveBackup: suspend (password: String, onProgress: (Int, String) -> Unit) -> Result<BackupRestoreStats>,
    onDeleteGoogleDriveBackup: suspend () -> Result<Boolean>,
    chatBackupFrequency: ChatBackupFrequency = ChatBackupFrequency.OFF,
    chatBackupOverCellular: Boolean = false,
    chatBackupHasStoredPassword: Boolean = false,
    onChatBackupFrequencySelected: (ChatBackupFrequency) -> Unit = {},
    onChatBackupOverCellularChange: (Boolean) -> Unit = {},
    onEnableAutoBackup: suspend (ChatBackupFrequency, String) -> Result<Unit> = { _, _ -> Result.success(Unit) },
    onCreateSignalBackup: suspend (password: String) -> Result<String>,
    onRestoreSignalBackup: suspend (content: String, password: String) -> Result<Unit>,
    signalBackupErrorMessage: (String) -> String,
    onPickBackupFile: (onPicked: (String) -> Unit) -> Unit,
    onSaveBackupFile: (fileName: String, content: String, onResult: (Boolean?) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var progressLabel by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var statusIsError by remember { mutableStateOf(false) }
    var showUploadDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showAutoPasswordDialog by remember { mutableStateOf(false) }
    var pendingAutoFrequency by remember { mutableStateOf<ChatBackupFrequency?>(null) }
    var autoBackupMenuExpanded by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var passwordConfirm by remember { mutableStateOf("") }

    LaunchedEffect(isGoogleDriveConfigured, googleDriveAccountLabel) {
        if (isGoogleDriveConfigured) {
            onRefreshGoogleDriveStatus()
        }
    }

    fun progressText(key: String): String = when (key) {
        "progress_auth" -> context.getString(R.string.settings_google_drive_progress_auth)
        "progress_create" -> context.getString(R.string.settings_google_drive_progress_create)
        "progress_upload" -> context.getString(R.string.settings_google_drive_progress_upload)
        "progress_find" -> context.getString(R.string.settings_google_drive_progress_find)
        "progress_download" -> context.getString(R.string.settings_google_drive_progress_download)
        "progress_restore" -> context.getString(R.string.settings_google_drive_progress_restore)
        "progress_chats" -> context.getString(R.string.settings_google_drive_progress_chats)
        "progress_done" -> context.getString(R.string.settings_chat_backup_progress_creating, progress)
        else -> context.getString(R.string.settings_google_drive_progress_create)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = stringResource(R.string.settings_chat_backup_section_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Text(
            text = stringResource(R.string.settings_chat_backup_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        if (googleDriveBackupMetadata != null) {
            val whenText = formatDriveTime(context, googleDriveBackupMetadata.file.modifiedTime)
            val sizeText = googleDriveBackupMetadata.file.size?.toLongOrNull()?.let { formatSize(it) }
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    text = if (sizeText != null) {
                        stringResource(R.string.settings_chat_backup_last_with_size, whenText, sizeText)
                    } else {
                        stringResource(R.string.settings_google_drive_last_backup, whenText)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "🔒 ${stringResource(R.string.settings_chat_backup_encrypted_badge)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        } else if (googleDriveAccountLabel != null) {
            Text(
                text = stringResource(R.string.settings_google_drive_no_backup),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }

        if (busy && progressLabel != null) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = stringResource(R.string.settings_chat_backup_progress_creating, progress),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }
        }

        if (!isGoogleDriveConfigured) {
            Text(
                text = stringResource(R.string.settings_google_drive_not_configured),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        } else {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_google_drive_account)) },
                supportingContent = {
                    Text(
                        googleDriveAccountLabel ?: stringResource(R.string.settings_google_drive_not_signed_in),
                    )
                },
                leadingContent = { Icon(Icons.Outlined.Cloud, contentDescription = null) },
            )

            if (googleDriveAccountLabel == null) {
                Button(
                    onClick = onGoogleDriveSignIn,
                    enabled = !busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(stringResource(R.string.settings_google_drive_sign_in))
                }
            } else {
                Button(
                    onClick = {
                        password = ""
                        passwordConfirm = ""
                        showUploadDialog = true
                    },
                    enabled = !busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(stringResource(R.string.settings_chat_backup_create_now))
                }

                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_google_drive_restore)) },
                    leadingContent = { Icon(Icons.Outlined.CloudDownload, contentDescription = null) },
                    trailingContent = {
                        TextButton(
                            enabled = !busy,
                            onClick = {
                                password = ""
                                showRestoreDialog = true
                            },
                        ) {
                            Text(stringResource(R.string.settings_signal_backup_action))
                        }
                    },
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_chat_backup_auto)) },
                        supportingContent = {
                            Text(chatBackupFrequencyLabel(chatBackupFrequency))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { autoBackupMenuExpanded = true },
                    )
                    DropdownMenu(
                        expanded = autoBackupMenuExpanded,
                        onDismissRequest = { autoBackupMenuExpanded = false },
                    ) {
                        ChatBackupFrequency.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(chatBackupFrequencyLabel(option)) },
                                onClick = {
                                    autoBackupMenuExpanded = false
                                    when {
                                        option != ChatBackupFrequency.OFF && googleDriveAccountLabel == null -> {
                                            statusMessage = context.getString(R.string.settings_google_drive_sign_in_hint)
                                            statusIsError = true
                                        }
                                        option == ChatBackupFrequency.OFF -> {
                                            onChatBackupFrequencySelected(ChatBackupFrequency.OFF)
                                        }
                                        chatBackupHasStoredPassword -> {
                                            onChatBackupFrequencySelected(option)
                                        }
                                        else -> {
                                            pendingAutoFrequency = option
                                            password = ""
                                            passwordConfirm = ""
                                            showAutoPasswordDialog = true
                                        }
                                    }
                                },
                            )
                        }
                    }
                }

                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_chat_backup_over_cellular)) },
                    supportingContent = {
                        Text(stringResource(R.string.settings_chat_backup_over_cellular_hint))
                    },
                    trailingContent = {
                        Switch(
                            checked = chatBackupOverCellular,
                            onCheckedChange = onChatBackupOverCellularChange,
                            enabled = chatBackupFrequency != ChatBackupFrequency.OFF,
                        )
                    },
                )

                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_google_drive_delete)) },
                    leadingContent = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                    trailingContent = {
                        TextButton(
                            enabled = !busy,
                            onClick = {
                                busy = true
                                scope.launch {
                                    onDeleteGoogleDriveBackup()
                                        .onSuccess { deleted ->
                                            statusMessage = context.getString(
                                                if (deleted) {
                                                    R.string.settings_google_drive_delete_done
                                                } else {
                                                    R.string.settings_google_drive_no_backup
                                                },
                                            )
                                            statusIsError = false
                                            onRefreshGoogleDriveStatus()
                                        }
                                        .onFailure { error ->
                                            statusMessage = formatBackupError(error, signalBackupErrorMessage)
                                            statusIsError = true
                                        }
                                    busy = false
                                }
                            },
                        ) {
                            Text(stringResource(R.string.settings_signal_backup_action))
                        }
                    },
                )

                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_google_drive_sign_out)) },
                    leadingContent = { Icon(Icons.Outlined.CloudOff, contentDescription = null) },
                    trailingContent = {
                        TextButton(
                            enabled = !busy,
                            onClick = {
                                scope.launch {
                                    onGoogleDriveSignOut()
                                    onRefreshGoogleDriveStatus()
                                }
                            },
                        ) {
                            Text(stringResource(R.string.settings_signal_backup_action))
                        }
                    },
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        Text(
            text = stringResource(R.string.settings_chat_backup_e2e_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Text(
            text = stringResource(R.string.settings_chat_backup_e2e_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_chat_backup_e2e_item)) },
            supportingContent = { Text(stringResource(R.string.settings_chat_backup_e2e_on)) },
            leadingContent = { Icon(Icons.Outlined.Lock, contentDescription = null) },
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        SignalBackupSection(
            accountLogin = accountLogin,
            onCreateBackup = onCreateSignalBackup,
            onRestoreBackup = onRestoreSignalBackup,
            backupErrorMessage = signalBackupErrorMessage,
            onPickBackupFile = onPickBackupFile,
            onSaveBackupFile = onSaveBackupFile,
        )

        statusMessage?.let { message ->
            Text(
                text = message,
                color = if (statusIsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }

    if (showUploadDialog) {
        BackupPasswordDialog(
            title = stringResource(R.string.settings_chat_backup_create_now),
            password = password,
            passwordConfirm = passwordConfirm,
            onPasswordChange = { password = it },
            onPasswordConfirmChange = { passwordConfirm = it },
            showConfirm = true,
            busy = busy,
            onDismiss = { if (!busy) showUploadDialog = false },
            onConfirm = {
                when {
                    password.length < 8 -> {
                        statusMessage = context.getString(R.string.settings_signal_backup_password_short)
                        statusIsError = true
                    }
                    password != passwordConfirm -> {
                        statusMessage = context.getString(R.string.settings_signal_backup_password_mismatch)
                        statusIsError = true
                    }
                    else -> {
                        busy = true
                        progress = 0
                        scope.launch {
                            onUploadGoogleDriveBackup(password) { value, key ->
                                progress = value
                                progressLabel = progressText(key)
                            }.onSuccess {
                                statusMessage = context.getString(R.string.settings_google_drive_upload_done)
                                statusIsError = false
                                showUploadDialog = false
                                onRefreshGoogleDriveStatus()
                            }.onFailure { error ->
                                statusMessage = formatBackupError(error, signalBackupErrorMessage)
                                statusIsError = true
                            }
                            busy = false
                            progressLabel = null
                        }
                    }
                }
            },
        )
    }

    if (showRestoreDialog) {
        BackupPasswordDialog(
            title = stringResource(R.string.settings_google_drive_restore),
            password = password,
            passwordConfirm = "",
            onPasswordChange = { password = it },
            onPasswordConfirmChange = {},
            showConfirm = false,
            busy = busy,
            onDismiss = { if (!busy) showRestoreDialog = false },
            onConfirm = {
                if (password.isBlank()) {
                    statusMessage = context.getString(R.string.settings_signal_backup_password_required)
                    statusIsError = true
                } else {
                    busy = true
                    progress = 0
                    scope.launch {
                        onRestoreGoogleDriveBackup(password) { value, key ->
                            progress = value
                            progressLabel = progressText(key)
                        }.onSuccess { stats ->
                            statusMessage = buildRestoreDoneMessage(context, stats)
                            statusIsError = false
                            showRestoreDialog = false
                            onRefreshGoogleDriveStatus()
                        }.onFailure { error ->
                            statusMessage = formatBackupError(error, signalBackupErrorMessage)
                            statusIsError = true
                        }
                        busy = false
                        progressLabel = null
                    }
                }
            },
        )
    }

    if (showAutoPasswordDialog && pendingAutoFrequency != null) {
        BackupPasswordDialog(
            title = stringResource(R.string.settings_chat_backup_auto_password_title),
            password = password,
            passwordConfirm = passwordConfirm,
            onPasswordChange = { password = it },
            onPasswordConfirmChange = { passwordConfirm = it },
            showConfirm = true,
            busy = busy,
            onDismiss = {
                if (!busy) {
                    showAutoPasswordDialog = false
                    pendingAutoFrequency = null
                }
            },
            onConfirm = {
                val frequency = pendingAutoFrequency ?: return@BackupPasswordDialog
                when {
                    password.length < 8 -> {
                        statusMessage = context.getString(R.string.settings_signal_backup_password_short)
                        statusIsError = true
                    }
                    password != passwordConfirm -> {
                        statusMessage = context.getString(R.string.settings_signal_backup_password_mismatch)
                        statusIsError = true
                    }
                    else -> {
                        val frequencyLabel = context.getString(chatBackupFrequencyLabelRes(frequency))
                        busy = true
                        scope.launch {
                            onEnableAutoBackup(frequency, password)
                                .onSuccess {
                                    statusMessage = context.getString(
                                        R.string.settings_chat_backup_auto_enabled,
                                        frequencyLabel,
                                    )
                                    statusIsError = false
                                    showAutoPasswordDialog = false
                                    pendingAutoFrequency = null
                                }
                                .onFailure { error ->
                                    statusMessage = signalBackupErrorMessage(
                                        (error as? IllegalArgumentException)?.message ?: "backup_failed",
                                    )
                                    statusIsError = true
                                }
                            busy = false
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun BackupPasswordDialog(
    title: String,
    password: String,
    passwordConfirm: String,
    onPasswordChange: (String) -> Unit,
    onPasswordConfirmChange: (String) -> Unit,
    showConfirm: Boolean,
    busy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                CybOutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = stringResource(R.string.settings_signal_backup_password),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    showPasswordToggle = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (showConfirm) {
                    Text(
                        text = stringResource(R.string.settings_signal_backup_password_export_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp),
                    )
                    CybOutlinedTextField(
                        value = passwordConfirm,
                        onValueChange = onPasswordConfirmChange,
                        label = stringResource(R.string.settings_signal_backup_password_confirm),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        showPasswordToggle = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Text(
                        text = stringResource(R.string.settings_signal_backup_password_import_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(enabled = !busy, onClick = onConfirm) {
                Text(stringResource(R.string.settings_signal_backup_action))
            }
        },
        dismissButton = {
            TextButton(enabled = !busy, onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

private fun formatBackupError(error: Throwable, mapCode: (String) -> String): String {
    var current: Throwable? = error
    while (current != null) {
        val message = current.message?.substringBefore(':')?.trim()
        if (!message.isNullOrBlank()) {
            return mapCode(message)
        }
        current = current.cause
    }
    return mapCode("backup_failed")
}

private fun buildRestoreDoneMessage(context: android.content.Context, stats: BackupRestoreStats): String {
    val base = context.getString(R.string.settings_google_drive_restore_done)
    if (stats.chatsImported + stats.chatsSkipped + stats.chatsErrors <= 0) return base
    return "$base ${context.getString(
        R.string.settings_google_drive_restore_chats_stats,
        stats.chatsImported,
        stats.chatsSkipped,
    )}"
}

private fun formatDriveTime(context: android.content.Context, iso: String): String {
    val timestamp = runCatching { java.time.Instant.parse(iso).toEpochMilli() }.getOrNull() ?: return iso
    val formatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
    return formatter.format(timestamp)
}

private fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.getDefault(), "%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format(Locale.getDefault(), "%.1f MB", mb)
}

@Composable
private fun chatBackupFrequencyLabel(frequency: ChatBackupFrequency): String =
    stringResource(chatBackupFrequencyLabelRes(frequency))

private fun chatBackupFrequencyLabelRes(frequency: ChatBackupFrequency): Int = when (frequency) {
    ChatBackupFrequency.OFF -> R.string.settings_chat_backup_auto_off
    ChatBackupFrequency.DAILY -> R.string.settings_chat_backup_auto_daily
    ChatBackupFrequency.WEEKLY -> R.string.settings_chat_backup_auto_weekly
    ChatBackupFrequency.MONTHLY -> R.string.settings_chat_backup_auto_monthly
}
