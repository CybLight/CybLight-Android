package org.cyblight.android.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import org.cyblight.android.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SignalBackupSection(
    accountLogin: String,
    onCreateBackup: suspend (password: String) -> Result<String>,
    onRestoreBackup: suspend (content: String, password: String) -> Result<Unit>,
    backupErrorMessage: (String) -> String,
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

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                pendingImportContent = stream.bufferedReader().readText()
                showImportDialog = true
                importPassword = ""
            }
        }.onFailure {
            statusMessage = backupErrorMessage("backup_file_invalid")
            statusIsError = true
        }
    }

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
                importLauncher.launch(arrayOf("*/*"))
            }) {
                Text(stringResource(R.string.settings_signal_backup_action))
            }
        },
    )

    statusMessage?.let { message ->
        Text(
            text = message,
            color = if (statusIsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { if (!busy) showExportDialog = false },
            title = { Text(stringResource(R.string.settings_signal_backup_export)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = exportPassword,
                        onValueChange = { exportPassword = it },
                        label = { Text(stringResource(R.string.settings_signal_backup_password)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = exportPasswordConfirm,
                        onValueChange = { exportPasswordConfirm = it },
                        label = { Text(stringResource(R.string.settings_signal_backup_password_confirm)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
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
                                shareBackupFile(context, content, accountLogin)
                                statusMessage = context.getString(R.string.settings_signal_backup_export_done)
                                statusIsError = false
                                showExportDialog = false
                            }.onFailure { error ->
                                val code = (error as? IllegalArgumentException)?.message ?: "backup_failed"
                                statusMessage = backupErrorMessage(code)
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
                OutlinedTextField(
                    value = importPassword,
                    onValueChange = { importPassword = it },
                    label = { Text(stringResource(R.string.settings_signal_backup_password)) },
                    modifier = Modifier.fillMaxWidth(),
                )
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
                        scope.launch {
                            val result = onRestoreBackup(content, importPassword)
                            busy = false
                            result.onSuccess {
                                statusMessage = context.getString(R.string.settings_signal_backup_import_done)
                                statusIsError = false
                                showImportDialog = false
                                pendingImportContent = null
                            }.onFailure { error ->
                                val code = (error as? IllegalArgumentException)?.message ?: "backup_failed"
                                statusMessage = backupErrorMessage(code)
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

private fun shareBackupFile(context: android.content.Context, content: String, login: String) {
    val safeLogin = login.replace(Regex("[^\\w.-]+"), "_").ifBlank { "user" }
    val stamp = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    val fileName = "cyblight-$safeLogin-$stamp.cyblight-backup"
    val file = File(context.cacheDir, fileName)
    file.writeText(content)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, fileName))
}
