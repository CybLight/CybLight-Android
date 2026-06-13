package org.cyblight.android.ui.messages

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.cyblight.android.R

@Composable
fun EncryptionReminderBanner(
    compact: Boolean,
    onOpenSecurityBackup: () -> Unit,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)),
    ) {
        Box {
            if (compact && onDismiss != null) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.TopEnd),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.close),
                        modifier = Modifier.padding(2.dp),
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = if (compact) 14.dp else 16.dp,
                        end = if (compact) 36.dp else 16.dp,
                        top = if (compact) 12.dp else 14.dp,
                        bottom = if (compact) 12.dp else 14.dp,
                    ),
                horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = "🔐",
                    fontSize = if (compact) 18.sp else 22.sp,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.encryption_reminder_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(
                            if (compact) {
                                R.string.encryption_reminder_body_compact
                            } else {
                                R.string.encryption_reminder_body
                            },
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                        modifier = Modifier.padding(top = 6.dp, bottom = if (compact) 8.dp else 10.dp),
                    )
                    TextButton(
                        onClick = onOpenSecurityBackup,
                        modifier = Modifier.padding(start = 0.dp),
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.encryption_reminder_link),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}
