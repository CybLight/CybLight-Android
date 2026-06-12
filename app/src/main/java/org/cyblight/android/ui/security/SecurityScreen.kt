package org.cyblight.android.ui.security

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.cyblight.android.R
import org.cyblight.android.ui.components.DetailScaffold

@Composable
fun SecurityScreen(
    onBack: () -> Unit,
    onOpenSessions: () -> Unit,
) {
    DetailScaffold(
        title = stringResource(R.string.security_title),
        onBack = onBack,
    ) { padding ->
        ListItem(
            headlineContent = { Text(stringResource(R.string.sessions_title)) },
            supportingContent = { Text(stringResource(R.string.sessions_hint)) },
            leadingContent = {
                Icon(Icons.AutoMirrored.Outlined.Login, contentDescription = null)
            },
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth()
                .clickable(onClick = onOpenSessions),
        )
    }
}
