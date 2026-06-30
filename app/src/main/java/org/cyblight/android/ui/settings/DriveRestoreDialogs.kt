package org.cyblight.android.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.cyblight.android.R
import org.cyblight.android.integrations.google_drive.DriveBackupMetadata
import org.cyblight.android.ui.components.CybOutlinedTextField
import java.text.DateFormat
import java.util.Locale

@Composable
fun BackupResultDialog(
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (isError) {
                        R.string.settings_backup_result_error_title
                    } else {
                        R.string.settings_backup_result_success_title
                    },
                ),
            )
        },
        text = {
            Text(
                text = message,
                color = if (isError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        },
    )
}

@Composable
fun DriveRestoreConfirmDialog(
    metadata: DriveBackupMetadata,
    onRestore: () -> Unit,
    onSkip: () -> Unit,
) {
    val whenText = formatDriveRestoreTime(metadata.file.modifiedTime)
    AlertDialog(
        onDismissRequest = onSkip,
        title = { Text(stringResource(R.string.drive_restore_confirm_title)) },
        text = {
            Text(stringResource(R.string.drive_restore_confirm_message, whenText))
        },
        confirmButton = {
            TextButton(onClick = onRestore) {
                Text(stringResource(R.string.drive_restore_restore))
            }
        },
        dismissButton = {
            TextButton(onClick = onSkip) {
                Text(stringResource(R.string.drive_restore_skip))
            }
        },
    )
}

@Composable
fun DriveRestoreProgressDialog(
    progress: Int,
    progressLabel: String?,
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.settings_google_drive_restore)) },
        text = {
            Column {
                Text(
                    text = progressLabel ?: stringResource(R.string.settings_google_drive_progress_restore),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )
            }
        },
        confirmButton = {},
    )
}

@Composable
fun DriveRestorePasswordDialog(
    password: String,
    onPasswordChange: (String) -> Unit,
    busy: Boolean,
    progress: Int,
    progressLabel: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.drive_restore_password_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.settings_signal_backup_password_import_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                CybOutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = stringResource(R.string.settings_signal_backup_password),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    showPasswordToggle = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (busy && progressLabel != null) {
                    Text(
                        text = progressLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(enabled = !busy, onClick = onConfirm) {
                Text(stringResource(R.string.drive_restore_restore))
            }
        },
        dismissButton = {
            TextButton(enabled = !busy, onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

private fun formatDriveRestoreTime(iso: String): String {
    val timestamp = runCatching { java.time.Instant.parse(iso).toEpochMilli() }.getOrNull() ?: return iso
    val formatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
    return formatter.format(timestamp)
}
