package org.cyblight.android.ui.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.cyblight.android.R
import org.cyblight.android.data.api.TrustedDeviceDto
import org.cyblight.android.ui.components.DetailScaffold

@Composable
fun TrustedDevicesScreen(
    devices: List<TrustedDeviceDto>,
    isLoading: Boolean,
    error: String?,
    isRemoving: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onRemove: (String) -> Unit,
) {
    var pendingRemoveId by remember { mutableStateOf<String?>(null) }

    if (pendingRemoveId != null) {
        AlertDialog(
            onDismissRequest = { pendingRemoveId = null },
            title = { Text(stringResource(R.string.security_trusted_remove_confirm_title)) },
            text = { Text(stringResource(R.string.security_trusted_remove_confirm_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingRemoveId?.let(onRemove)
                        pendingRemoveId = null
                    },
                ) {
                    Text(stringResource(R.string.security_trusted_remove))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoveId = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    DetailScaffold(
        title = stringResource(R.string.security_trusted_devices_title),
        onBack = onBack,
    ) { padding ->
        when {
            isLoading && devices.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            !error.isNullOrBlank() && devices.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                    OutlinedButton(onClick = onRefresh, modifier = Modifier.padding(top = 12.dp)) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }
            devices.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = stringResource(R.string.security_trusted_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(devices, key = { it.id }) { device ->
                        TrustedDeviceCard(
                            device = device,
                            isRemoving = isRemoving,
                            onRemove = { pendingRemoveId = device.id },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrustedDeviceCard(
    device: TrustedDeviceDto,
    isRemoving: Boolean,
    onRemove: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.security_trusted_device_title),
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(
                    R.string.security_trusted_added,
                    formatSecurityTimestamp(device.createdAt),
                ),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(
                    R.string.security_trusted_last_used,
                    if (device.lastUsedAt != null && device.lastUsedAt > 0L) {
                        formatSecurityTimestamp(device.lastUsedAt)
                    } else {
                        stringResource(R.string.security_trusted_never_used)
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
            )
            device.ipAddress?.takeIf { it.isNotBlank() }?.let { ip ->
                Text(
                    text = stringResource(R.string.security_ip_label, ip),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            device.userAgent?.takeIf { it.isNotBlank() }?.let { ua ->
                Text(
                    text = stringResource(R.string.security_device_label, ua),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            OutlinedButton(
                onClick = onRemove,
                enabled = !isRemoving,
            ) {
                Text(stringResource(R.string.security_trusted_remove))
            }
        }
    }
}
