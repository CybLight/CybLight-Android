package org.cyblight.android.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.cyblight.android.R
import org.cyblight.android.data.preferences.ChatFontSize

@Composable
internal fun ChatFontSizeScreen(
    selectedSize: ChatFontSize,
    onSizeSelected: (ChatFontSize) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = stringResource(R.string.settings_chats_font_size_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
        ChatFontSize.entries.forEach { size ->
            ListItem(
                headlineContent = {
                    Text(
                        text = chatFontSizeLabel(size),
                        fontSize = (16.sp * size.scale),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                trailingContent = {
                    if (size == selectedSize) {
                        Icon(Icons.Filled.Check, contentDescription = null)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSizeSelected(size) },
            )
        }
    }
}

@Composable
internal fun chatFontSizeLabel(size: ChatFontSize): String = when (size) {
    ChatFontSize.SMALL -> stringResource(R.string.settings_chats_font_size_small)
    ChatFontSize.MEDIUM -> stringResource(R.string.settings_chats_font_size_medium)
    ChatFontSize.LARGE -> stringResource(R.string.settings_chats_font_size_large)
    ChatFontSize.EXTRA_LARGE -> stringResource(R.string.settings_chats_font_size_extra_large)
}
