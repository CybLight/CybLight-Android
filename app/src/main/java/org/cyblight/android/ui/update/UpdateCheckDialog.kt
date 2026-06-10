package org.cyblight.android.ui.update

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.cyblight.android.R
import org.cyblight.android.update.ManualUpdateCheckState

@Composable
fun UpdateCheckDialog(
    state: ManualUpdateCheckState,
    onDismiss: () -> Unit,
) {
    if (!state.visible) return

    AlertDialog(
        onDismissRequest = {
            if (!state.checking) onDismiss()
        },
        title = {
            Text(
                text = stringResource(R.string.update_check_title),
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            when {
                state.checking -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                state.upToDate -> {
                    Text(
                        text = stringResource(R.string.update_up_to_date),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                state.errorMessage != null -> {
                    Text(
                        text = state.errorMessage.ifBlank {
                            stringResource(R.string.update_check_error)
                        },
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        confirmButton = {
            if (!state.checking) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.close))
                }
            }
        },
    )
}
