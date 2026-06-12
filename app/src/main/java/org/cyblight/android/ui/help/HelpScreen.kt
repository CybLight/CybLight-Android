package org.cyblight.android.ui.help

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.cyblight.android.R
import org.cyblight.android.ui.components.DetailScaffold

@Composable
fun HelpScreen(onBack: () -> Unit) {
    DetailScaffold(
        title = stringResource(R.string.help_title),
        onBack = onBack,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.help_intro),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HelpSection(
                title = stringResource(R.string.help_section_main),
                items = listOf(
                    stringResource(R.string.help_item_logo),
                    stringResource(R.string.help_item_menu),
                    stringResource(R.string.help_item_tabs),
                    stringResource(R.string.help_item_friends),
                    stringResource(R.string.help_item_messages),
                    stringResource(R.string.help_item_security),
                    stringResource(R.string.help_item_easter),
                ),
            )

            HelpSection(
                title = stringResource(R.string.help_section_settings),
                items = listOf(
                    stringResource(R.string.help_item_settings_open),
                    stringResource(R.string.help_item_appearance),
                    stringResource(R.string.help_item_notifications),
                    stringResource(R.string.help_item_background),
                    stringResource(R.string.help_item_about),
                ),
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Text(
                    text = stringResource(R.string.help_footer_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun HelpSection(
    title: String,
    items: List<String>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        items.forEach { item ->
            Text(
                text = "• $item",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}
