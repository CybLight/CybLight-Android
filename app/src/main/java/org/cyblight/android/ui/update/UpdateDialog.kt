package org.cyblight.android.ui.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.cyblight.android.R
import org.cyblight.android.update.UpdateStatus
import org.cyblight.android.update.UpdateUiState

@Composable
fun UpdateDialog(
    state: UpdateUiState,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!state.visible) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.update_title),
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.update_version_available, state.versionName),
                    style = MaterialTheme.typography.bodyLarge,
                )

                if (state.releaseNotes.isNotBlank()) {
                    Text(
                        text = state.releaseNotes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                when (state.status) {
                    UpdateStatus.Downloading -> {
                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = stringResource(
                                R.string.update_downloading_percent,
                                (state.progress * 100).toInt(),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    UpdateStatus.Error -> {
                        Text(
                            text = state.errorMessage ?: stringResource(R.string.update_error_generic),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    else -> Unit
                }
            }
        },
        confirmButton = {
            when (state.status) {
                UpdateStatus.ReadyToInstall -> {
                    TextButton(onClick = onInstall) {
                        Text(stringResource(R.string.update_install))
                    }
                }
                UpdateStatus.Downloading -> Unit
                UpdateStatus.Error -> {
                    TextButton(onClick = onDownload) {
                        Text(stringResource(R.string.update_retry))
                    }
                }
                else -> {
                    TextButton(
                        onClick = onDownload,
                        enabled = state.downloadUrl.isNotBlank(),
                    ) {
                        Text(stringResource(R.string.update_download))
                    }
                }
            }
        },
        dismissButton = {
            if (state.status != UpdateStatus.Downloading) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.update_later))
                }
            }
        },
    )
}
