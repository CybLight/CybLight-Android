package org.cyblight.android.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.cyblight.android.util.PresenceFormatter

@Composable
fun PresenceLabel(
    isOnline: Boolean,
    lastSeenAt: Long?,
    modifier: Modifier = Modifier,
) {
    val online = PresenceFormatter.isOnline(isOnline, lastSeenAt)
    Text(
        text = PresenceFormatter.label(isOnline, lastSeenAt),
        style = MaterialTheme.typography.bodySmall,
        color = if (online) {
            MaterialTheme.colorScheme.secondary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = modifier,
    )
}
