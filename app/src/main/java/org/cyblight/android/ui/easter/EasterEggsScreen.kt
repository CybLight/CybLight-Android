package org.cyblight.android.ui.easter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.cyblight.android.R
import org.cyblight.android.data.api.EasterFlagsDto
import org.cyblight.android.ui.components.DetailScaffold

@Composable
fun EasterEggsScreen(
    flags: EasterFlagsDto?,
    isLoading: Boolean,
    error: String?,
    onBack: () -> Unit,
) {
    DetailScaffold(
        title = stringResource(R.string.easter_eggs_title),
        onBack = onBack,
    ) { padding ->
        when {
            isLoading && flags == null -> {
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
            !error.isNullOrBlank() && flags == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.easter_eggs_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    EasterEggCard(
                        emoji = "🍓",
                        title = stringResource(R.string.easter_strawberry_title),
                        unlocked = flags?.strawberry == true,
                    )
                    EasterEggCard(
                        emoji = "🌑",
                        title = stringResource(R.string.easter_dark_trigger_title),
                        unlocked = flags?.darkTrigger == true,
                    )
                    EasterEggCard(
                        emoji = "🪞",
                        title = stringResource(R.string.easter_profile_mirror_title),
                        unlocked = flags?.profileMirror == true,
                    )
                    EasterEggCard(
                        emoji = "💡",
                        title = stringResource(R.string.easter_light_catcher_title),
                        unlocked = flags?.lightCatcher == true,
                    )
                }
            }
        }
    }
}

@Composable
private fun EasterEggCard(
    emoji: String,
    title: String,
    unlocked: Boolean,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = emoji, style = MaterialTheme.typography.headlineMedium)
            Text(text = title, fontWeight = FontWeight.SemiBold)
            Text(
                text = if (unlocked) {
                    stringResource(R.string.easter_unlocked)
                } else {
                    stringResource(R.string.easter_locked)
                },
                color = if (unlocked) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}
