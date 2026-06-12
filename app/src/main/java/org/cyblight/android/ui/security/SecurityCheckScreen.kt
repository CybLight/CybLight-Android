package org.cyblight.android.ui.security

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.cyblight.android.R
import org.cyblight.android.data.repository.SecurityOverview
import org.cyblight.android.ui.components.DetailScaffold

@Composable
fun SecurityCheckScreen(
    overview: SecurityOverview?,
    onBack: () -> Unit,
    onOpenEmail: () -> Unit,
    onOpenTwoFactor: () -> Unit,
    onOpenPasskeys: () -> Unit,
) {
    DetailScaffold(
        title = stringResource(R.string.security_check_title),
        onBack = onBack,
    ) { padding ->
        if (overview == null) return@DetailScaffold

        val score = overview.securityScore
        val levelLabel = when {
            score >= 100 -> stringResource(R.string.security_level_good)
            score >= 50 -> stringResource(R.string.security_level_medium)
            else -> stringResource(R.string.security_level_low)
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.security_score_label),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "$score%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    LinearProgressIndicator(
                        progress = { score / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = levelLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }

            ChecklistItem(
                done = overview.emailVerified,
                doneText = stringResource(R.string.security_check_email_done),
                pendingText = stringResource(R.string.security_check_email_pending),
                onClick = if (!overview.emailVerified) onOpenEmail else null,
            )
            ChecklistItem(
                done = overview.totpEnabled,
                doneText = stringResource(R.string.security_check_2fa_done),
                pendingText = stringResource(R.string.security_check_2fa_pending),
                onClick = if (!overview.totpEnabled) onOpenTwoFactor else null,
            )
            ChecklistItem(
                done = overview.passkeyCount > 0,
                doneText = stringResource(R.string.security_check_passkey_done),
                pendingText = stringResource(R.string.security_check_passkey_pending),
                onClick = if (overview.passkeyCount <= 0) onOpenPasskeys else null,
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = if (score >= 100) {
                            stringResource(R.string.security_recommendation_ok_title)
                        } else {
                            stringResource(R.string.security_recommendation_title)
                        },
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = when {
                            score >= 100 -> stringResource(R.string.security_recommendation_ok)
                            score < 30 -> stringResource(R.string.security_recommendation_low)
                            score < 50 -> stringResource(R.string.security_recommendation_medium)
                            else -> stringResource(R.string.security_recommendation_high)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChecklistItem(
    done: Boolean,
    doneText: String,
    pendingText: String,
    onClick: (() -> Unit)?,
) {
    val modifier = if (!done && onClick != null) {
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    } else {
        Modifier.fillMaxWidth()
    }

    Card(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (done) doneText else pendingText,
                modifier = Modifier.weight(1f),
            )
            if (done) {
                Text(
                    text = stringResource(R.string.security_done),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                )
            } else if (onClick != null) {
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
