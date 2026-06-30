package org.cyblight.android.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.Alignment
import org.cyblight.android.ui.components.CybOutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.cyblight.android.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SignalBackupSection(
    accountLogin: String,
    onCreateBackup: suspend (password: String) -> Result<String>,
    onRestoreBackup: suspend (content: String, password: String, onProgress: (Int, String) -> Unit) -> Result<Unit>,
    backupErrorMessage: (String) -> String,
    onPickBackupFile: (onPicked: (String) -> Unit) -> Unit,
    onSaveBackupFile: (fileName: String, content: String, onResult: (Boolean?) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var pendingImportContent by remember { mutableStateOf<String?>(null) }
    var exportPassword by remember { mutableStateOf("") }
    var exportPasswordConfirm by remember { mutableStateOf("") }
    var importPassword by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var statusIsError by remember { mutableStateOf(false) }

    var importProgress by remember { mutableIntStateOf(0) }
    var importProgressKey by remember { mutableStateOf<String?>(null) }

    fun progressText(key: String): String = when (key) {
        "progress_auth" -> "Авторизация..."
        "progress_create" -> "Создание копии..."
        "progress_upload" -> "Загрузка в облако..."
        "progress_find" -> "Поиск копии..."
        "progress_download" -> "Скачивание..."
        "progress_restore" -> "Восстановление данных..."
        "progress_chats" -> "Импорт чатов..."
        "progress_done" -> "Готово"
        else -> "Обработка..."
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.settings_signal_backup_section),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        Text(
            text = stringResource(R.string.settings_signal_backup_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_signal_backup_export)) },
            leadingContent = { Icon(Icons.Outlined.CloudDownload, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            trailingContent = {
                TextButton(onClick = {
                    exportPassword = ""
                    exportPasswordConfirm = ""
                    showExportDialog = true
                }) {
                    Text(stringResource(R.string.settings_signal_backup_action))
                }
            },
        )

        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_signal_backup_import)) },
            leadingContent = { Icon(Icons.Outlined.CloudUpload, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            trailingContent = {
                TextButton(onClick = {
                    onPickBackupFile { content ->
                        pendingImportContent = content
                        showImportDialog = true
                        importPassword = ""
                    }
                }) {
                    Text(stringResource(R.string.settings_signal_backup_action))
                }
            },
        )

    }

    statusMessage?.let { message ->
        BackupResultDialog(
            message = message,
            isError = statusIsError,
            onDismiss = { statusMessage = null },
        )
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { if (!busy) showExportDialog = false },
            title = { Text(stringResource(R.string.settings_signal_backup_export)) },
            text = {
                Column {
                    CybOutlinedTextField(
                        value = exportPassword,
                        onValueChange = { exportPassword = it },
                        label = stringResource(R.string.settings_signal_backup_password),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        showPasswordToggle = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = stringResource(R.string.settings_signal_backup_password_export_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp),
                    )
                    CybOutlinedTextField(
                        value = exportPasswordConfirm,
                        onValueChange = { exportPasswordConfirm = it },
                        label = stringResource(R.string.settings_signal_backup_password_confirm),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        showPasswordToggle = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !busy,
                    onClick = {
                        if (exportPassword.length < 8) {
                            statusMessage = context.getString(R.string.settings_signal_backup_password_short)
                            statusIsError = true
                            return@TextButton
                        }
                        if (exportPassword != exportPasswordConfirm) {
                            statusMessage = context.getString(R.string.settings_signal_backup_password_mismatch)
                            statusIsError = true
                            return@TextButton
                        }
                        busy = true
                        scope.launch {
                            val result = onCreateBackup(exportPassword)
                            busy = false
                            result.onSuccess { content ->
                                val fileName = buildBackupFileName(accountLogin)
                                onSaveBackupFile(fileName, content) { saved ->
                                    when (saved) {
                                        true -> {
                                            statusMessage = context.getString(R.string.settings_signal_backup_export_done)
                                            statusIsError = false
                                            showExportDialog = false
                                        }
                                        false -> {
                                            statusMessage = backupErrorMessage("backup_save_failed")
                                            statusIsError = true
                                        }
                                        null -> Unit
                                    }
                                }
                            }.onFailure { error ->
                                statusMessage = backupErrorMessage(extractBackupErrorCode(error))
                                statusIsError = true
                            }
                        }
                    },
                ) {
                    Text(stringResource(R.string.settings_signal_backup_action))
                }
            },
            dismissButton = {
                TextButton(enabled = !busy, onClick = { showExportDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { if (!busy) showImportDialog = false },
            title = { Text(stringResource(R.string.settings_signal_backup_import)) },
            text = {
                Column {
                    CybOutlinedTextField(
                        value = importPassword,
                        onValueChange = { importPassword = it },
                        label = stringResource(R.string.settings_signal_backup_password),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        showPasswordToggle = true,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = stringResource(R.string.settings_signal_backup_password_import_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp),
                    )

                    if (busy && importProgressKey != null) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = progressText(importProgressKey!!),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = "$importProgress%",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { importProgress / 100f },
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                        }
                    } else if (busy) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = stringResource(R.string.settings_chat_backup_busy),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !busy,
                    onClick = {
                        val content = pendingImportContent
                        if (content.isNullOrBlank() || importPassword.isBlank()) {
                            statusMessage = context.getString(R.string.settings_signal_backup_password_required)
                            statusIsError = true
                            return@TextButton
                        }
                        busy = true
                        importProgress = 0
                        importProgressKey = "progress_restore"
                        scope.launch {
                            val backupContent = pendingImportContent ?: ""
                            pendingImportContent = null // Clear original reference ASAP
                            
                            val result = onRestoreBackup(backupContent, importPassword) { p, k ->
                                importProgress = p
                                importProgressKey = k
                            }
                            busy = false
                            result.onSuccess {
                                statusMessage = context.getString(R.string.settings_signal_backup_import_done)
                                statusIsError = false
                                showImportDialog = false
                            }.onFailure { error ->
                                statusMessage = backupErrorMessage(extractBackupErrorCode(error))
                                statusIsError = true
                            }
                        }
                    },
                ) {
                    Text(stringResource(R.string.settings_signal_backup_action))
                }
            },
            dismissButton = {
                TextButton(enabled = !busy, onClick = { showImportDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

private fun extractBackupErrorCode(error: Throwable): String {
    var current: Throwable? = error
    while (current != null) {
        val message = current.message?.substringBefore(':')?.trim()
        if (!message.isNullOrBlank()) {
            return message
        }
        current = current.cause
    }
    return "backup_failed"
}

private fun buildBackupFileName(login: String): String {
    val safeLogin = login.replace(Regex("[^\\w.-]+"), "_").ifBlank { "user" }
    val stamp = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    return "cyblight-$safeLogin-$stamp.cyblight-backup"
}
