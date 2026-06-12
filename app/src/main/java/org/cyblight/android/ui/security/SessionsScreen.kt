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
import org.cyblight.android.data.api.SessionDto
import org.cyblight.android.ui.components.DetailScaffold
import java.text.DateFormat
import java.util.Date

@Composable
fun SessionsScreen(
    sessions: List<SessionDto>,
    currentSessionId: String?,
    isLoading: Boolean,
    error: String?,
    isRevoking: Boolean,
    onBack: () -> Unit,
    onRevoke: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    DetailScaffold(
        title = stringResource(R.string.sessions_title),
        onBack = onBack,
    ) { padding ->
        when {
            isLoading && sessions.isEmpty() -> {
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
            !error.isNullOrBlank() && sessions.isEmpty() -> {
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
                    items(sessions, key = { it.id }) { session ->
                        SessionCard(
                            session = session,
                            isCurrent = session.id == currentSessionId,
                            isRevoking = isRevoking,
                            onRevoke = onRevoke,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionCard(
    session: SessionDto,
    isCurrent: Boolean,
    isRevoking: Boolean,
    onRevoke: (String) -> Unit,
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
                text = listOfNotNull(session.browser, session.os).joinToString(" · ").ifBlank {
                    stringResource(R.string.sessions_unknown_device)
                },
                fontWeight = FontWeight.SemiBold,
            )
            if (isCurrent) {
                Text(
                    text = stringResource(R.string.sessions_current),
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            session.city?.takeIf { it.isNotBlank() }?.let { city ->
                val location = listOfNotNull(city, session.country).joinToString(", ")
                Text(location, style = MaterialTheme.typography.bodySmall)
            }
            if (session.lastSeenAt > 0L) {
                val seen = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                    .format(Date(session.lastSeenAt))
                Text(
                    text = stringResource(R.string.sessions_last_seen, seen),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!isCurrent) {
                OutlinedButton(
                    onClick = { onRevoke(session.id) },
                    enabled = !isRevoking,
                ) {
                    Text(stringResource(R.string.sessions_revoke))
                }
            }
        }
    }
}
