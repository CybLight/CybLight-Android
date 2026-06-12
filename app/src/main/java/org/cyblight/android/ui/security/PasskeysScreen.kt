package org.cyblight.android.ui.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import org.cyblight.android.data.api.PasskeyDto
import org.cyblight.android.ui.components.DetailScaffold

@Composable
fun PasskeysScreen(
    passkeys: List<PasskeyDto>,
    isLoading: Boolean,
    error: String?,
    isRegistering: Boolean,
    registerError: String?,
    isDeleting: Boolean,
    deleteError: String?,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onAddPasskey: (String) -> Unit,
    onDeletePasskey: (String) -> Unit,
    onDismissRegisterError: () -> Unit,
    onDismissDeleteError: () -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var passkeyName by remember { mutableStateOf("") }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }

    val isBusy = isRegistering || isDeleting

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isBusy) {
                    showAddDialog = false
                    passkeyName = ""
                }
            },
            title = { Text(stringResource(R.string.security_passkeys_add_dialog_title)) },
            text = {
                OutlinedTextField(
                    value = passkeyName,
                    onValueChange = { passkeyName = it },
                    label = { Text(stringResource(R.string.security_passkeys_name_hint)) },
                    singleLine = true,
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onAddPasskey(passkeyName.trim())
                        showAddDialog = false
                        passkeyName = ""
                    },
                    enabled = !isBusy,
                ) {
                    Text(stringResource(R.string.security_passkeys_add))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddDialog = false
                        passkeyName = ""
                    },
                    enabled = !isBusy,
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    pendingDeleteId?.let { passkeyId ->
        AlertDialog(
            onDismissRequest = {
                if (!isDeleting) pendingDeleteId = null
            },
            title = { Text(stringResource(R.string.security_passkeys_remove_confirm_title)) },
            text = { Text(stringResource(R.string.security_passkeys_remove_confirm_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeletePasskey(passkeyId)
                        pendingDeleteId = null
                    },
                    enabled = !isDeleting,
                ) {
                    Text(stringResource(R.string.security_passkeys_remove))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingDeleteId = null },
                    enabled = !isDeleting,
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    registerError?.let { code ->
        AlertDialog(
            onDismissRequest = onDismissRegisterError,
            title = { Text(stringResource(R.string.security_passkeys_add)) },
            text = { Text(passkeyRegisterErrorMessage(code)) },
            confirmButton = {
                TextButton(onClick = onDismissRegisterError) {
                    Text(stringResource(R.string.ok))
                }
            },
        )
    }

    deleteError?.let {
        AlertDialog(
            onDismissRequest = onDismissDeleteError,
            title = { Text(stringResource(R.string.security_passkeys_remove)) },
            text = { Text(stringResource(R.string.error_passkey_remove_failed)) },
            confirmButton = {
                TextButton(onClick = onDismissDeleteError) {
                    Text(stringResource(R.string.ok))
                }
            },
        )
    }

    DetailScaffold(
        title = stringResource(R.string.security_passkeys_title),
        onBack = onBack,
    ) { padding ->
        when {
            isLoading && passkeys.isEmpty() -> {
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
            !error.isNullOrBlank() && passkeys.isEmpty() -> {
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
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Button(
                                onClick = { showAddDialog = true },
                                enabled = !isBusy,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                if (isRegistering) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.padding(end = 8.dp),
                                        strokeWidth = 2.dp,
                                    )
                                }
                                Text(stringResource(R.string.security_passkeys_add))
                            }
                        }
                    }

                    if (passkeys.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.security_passkeys_none),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        items(passkeys, key = { it.id }) { passkey ->
                            PasskeyCard(
                                passkey = passkey,
                                isDeleting = isDeleting,
                                onDelete = { pendingDeleteId = passkey.id },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun passkeyRegisterErrorMessage(code: String): String {
    return when (code) {
        "cancelled" -> stringResource(R.string.error_passkey_cancelled)
        "asset_links_failed" -> stringResource(R.string.error_passkey_asset_links_failed)
        "origin_mismatch" -> stringResource(R.string.error_passkey_origin_mismatch)
        "challenge_mismatch", "invalid_challenge" -> stringResource(R.string.error_passkey_challenge_mismatch)
        "invalid_client_data" -> stringResource(R.string.error_passkey_register_failed)
        "passkey_failed" -> stringResource(R.string.error_passkey_register_failed)
        else -> stringResource(R.string.error_passkey_register_failed)
    }
}

@Composable
private fun PasskeyCard(
    passkey: PasskeyDto,
    isDeleting: Boolean,
    onDelete: () -> Unit,
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
                text = passkey.name?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.security_passkeys_default_name),
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(
                    R.string.security_passkeys_created,
                    formatSecurityTimestamp(passkey.createdAt),
                ),
                style = MaterialTheme.typography.bodySmall,
            )
            passkey.lastUsedAt?.takeIf { it > 0L }?.let { lastUsed ->
                Text(
                    text = stringResource(
                        R.string.security_passkeys_last_used,
                        formatSecurityTimestamp(lastUsed),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            OutlinedButton(
                onClick = onDelete,
                enabled = !isDeleting,
            ) {
                Text(stringResource(R.string.security_passkeys_remove))
            }
        }
    }
}
