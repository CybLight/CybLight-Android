package org.cyblight.android.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.cyblight.android.R
import org.cyblight.android.data.preferences.ChatDefaultTheme
import org.cyblight.android.data.preferences.ChatThemeDefinitions

@Composable
internal fun ChatDefaultThemeScreen(
    selectedTheme: ChatDefaultTheme,
    onThemeSelected: (ChatDefaultTheme) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.settings_chats_theme_section),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(ChatDefaultTheme.entries) { theme ->
                ChatThemePreviewCard(
                    theme = theme,
                    selected = theme == selectedTheme,
                    onClick = { onThemeSelected(theme) },
                )
            }
        }
        Text(
            text = stringResource(R.string.settings_chats_theme_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun ChatThemePreviewCard(
    theme: ChatDefaultTheme,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val palette = if (theme == ChatDefaultTheme.SYSTEM) {
        ChatThemeDefinitions.palette(ChatDefaultTheme.CYBLIGHT).copy(
            wallpaper = MaterialTheme.colorScheme.background,
            outgoingBubble = MaterialTheme.colorScheme.primary,
            incomingBubble = MaterialTheme.colorScheme.surfaceVariant,
        )
    } else {
        ChatThemeDefinitions.palette(theme)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (selected) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.1f)
                .clip(RoundedCornerShape(10.dp))
                .background(palette.wallpaper)
                .padding(10.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.incomingBubble)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.68f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.outgoingBubble)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    )
                }
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 4.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        Text(
            text = chatThemeLabel(theme),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
internal fun chatThemeLabel(theme: ChatDefaultTheme): String = when (theme) {
    ChatDefaultTheme.SYSTEM -> stringResource(R.string.settings_chats_theme_system)
    ChatDefaultTheme.CYBLIGHT -> stringResource(R.string.settings_chats_theme_cyblight)
    ChatDefaultTheme.CLASSIC -> stringResource(R.string.settings_chats_theme_classic)
    ChatDefaultTheme.MIDNIGHT -> stringResource(R.string.settings_chats_theme_midnight)
    ChatDefaultTheme.OCEAN -> stringResource(R.string.settings_chats_theme_ocean)
    ChatDefaultTheme.SUNSET -> stringResource(R.string.settings_chats_theme_sunset)
    ChatDefaultTheme.FOREST -> stringResource(R.string.settings_chats_theme_forest)
    ChatDefaultTheme.LAVENDER -> stringResource(R.string.settings_chats_theme_lavender)
}
