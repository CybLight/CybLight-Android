package org.cyblight.android.ui.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.cyblight.android.R
import org.cyblight.android.data.api.LoginHistoryEntryDto
import org.cyblight.android.ui.components.DetailScaffold

@Composable
fun LoginHistoryScreen(
    history: List<LoginHistoryEntryDto>,
    isLoading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
) {
    DetailScaffold(
        title = stringResource(R.string.security_login_history_title),
        onBack = onBack,
    ) { padding ->
        when {
            isLoading && history.isEmpty() -> {
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
            !error.isNullOrBlank() && history.isEmpty() -> {
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
            history.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = stringResource(R.string.security_history_empty),
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
                    items(history, key = { it.id }) { entry ->
                        HistoryCard(entry = entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(entry: LoginHistoryEntryDto) {
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
                text = loginHistoryActionLabel(entry.action),
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = formatSecurityTimestamp(entry.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            entry.ip?.takeIf { it.isNotBlank() }?.let { ip ->
                Text(
                    text = stringResource(R.string.security_ip_label, ip),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            entry.userAgent?.takeIf { it.isNotBlank() }?.let { ua ->
                Text(
                    text = stringResource(R.string.security_device_label, ua),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun loginHistoryActionLabel(action: String): String {
    val resId = when (action) {
        "login_success" -> R.string.security_history_action_login_success
        "login_failed" -> R.string.security_history_action_login_failed
        "login_2fa" -> R.string.security_history_action_login_2fa
        "logout" -> R.string.security_history_action_logout
        "password_changed", "auth.password.change" -> R.string.security_history_action_password_changed
        "2fa_enabled" -> R.string.security_history_action_2fa_enabled
        "2fa_disabled" -> R.string.security_history_action_2fa_disabled
        "passkey_added" -> R.string.security_history_action_passkey_added
        "passkey_removed" -> R.string.security_history_action_passkey_removed
        "passkey_login" -> R.string.security_history_action_passkey_login
        "account_created" -> R.string.security_history_action_account_created
        "trusted_device_added" -> R.string.security_history_action_trusted_device_added
        "trusted_device_removed" -> R.string.security_history_action_trusted_device_removed
        else -> null
    }
    return if (resId != null) stringResource(resId) else action
}
