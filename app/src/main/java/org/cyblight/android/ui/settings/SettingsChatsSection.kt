package org.cyblight.android.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.cyblight.android.R
import org.cyblight.android.data.preferences.ChatDefaultTheme
import org.cyblight.android.data.preferences.ChatFontSize
import org.cyblight.android.data.preferences.resolveChatThemePalette

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SettingsChatsSection(
    chatDefaultTheme: ChatDefaultTheme,
    chatSendWithEnter: Boolean,
    chatFontSize: ChatFontSize,
    chatFormatToolbarHidden: Boolean,
    encryptionReminderHidden: Boolean,
    chatQuoteColor: Int?,
    isChatsTransferBusy: Boolean,
    onOpenChatTheme: () -> Unit,
    onChatSendWithEnterChange: (Boolean) -> Unit,
    onOpenChatFontSize: () -> Unit,
    onChatFormatToolbarHiddenChange: (Boolean) -> Unit,
    onEncryptionReminderHiddenChange: (Boolean) -> Unit,
    onChatQuoteColorChange: (Int?) -> Unit,
    onOpenChatBackup: () -> Unit,
    onExportChats: () -> Unit,
    onImportChats: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isQuoteColorExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        SettingsSectionHeader(stringResource(R.string.settings_chats_section_screen))

        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_chats_default_theme)) },
            supportingContent = { Text(chatThemeLabel(chatDefaultTheme)) },
            leadingContent = { Icon(Icons.Outlined.Palette, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenChatTheme),
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        SettingsSectionHeader(stringResource(R.string.settings_chats_section_chat_settings))

        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_chats_send_with_enter)) },
            supportingContent = {
                Text(stringResource(R.string.settings_chats_send_with_enter_hint))
            },
            trailingContent = {
                Switch(
                    checked = chatSendWithEnter,
                    onCheckedChange = onChatSendWithEnterChange,
                )
            },
        )

        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_chats_font_size)) },
            supportingContent = { Text(chatFontSizeLabel(chatFontSize)) },
            leadingContent = { Icon(Icons.Outlined.TextFields, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenChatFontSize),
        )

        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_chats_quote_color)) },
            leadingContent = { Icon(Icons.Outlined.Palette, contentDescription = null) },
            trailingContent = {
                val themePalette = resolveChatThemePalette(chatDefaultTheme)
                val currentColor = chatQuoteColor?.let { Color(it) } ?: themePalette.quoteBar
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(currentColor)
                        .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isQuoteColorExpanded = !isQuoteColorExpanded }
        )

        AnimatedVisibility(
            visible = isQuoteColorExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            val themePalette = resolveChatThemePalette(chatDefaultTheme)
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val colors = listOf(
                    null to themePalette.quoteBar, // Default
                    0xFF0EA5E9.toInt() to Color(0xFF0EA5E9L), // Sky Blue
                    0xFF3B82F6.toInt() to Color(0xFF3B82F6L), // Blue
                    0xFFEF4444.toInt() to Color(0xFFEF4444L), // Red
                    0xFF10B981.toInt() to Color(0xFF10B981L), // Green
                    0xFFF59E0B.toInt() to Color(0xFFF59E0BL), // Amber
                    0xFF8B5CF6.toInt() to Color(0xFF8B5CF6L), // Violet
                    0xFFEC4899.toInt() to Color(0xFFEC4899L), // Pink
                )
                colors.forEach { (colorInt, color) ->
                    val isSelected = chatQuoteColor == colorInt
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 2.5.dp else 1.2.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.45f),
                                shape = CircleShape
                            )
                            .clickable { onChatQuoteColorChange(colorInt) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }

        ListItem(
            headlineContent = { Text(stringResource(R.string.messages_format_toolbar_hidden)) },
            supportingContent = {
                Text(stringResource(R.string.settings_chats_format_toolbar_hint))
            },
            trailingContent = {
                Switch(
                    checked = chatFormatToolbarHidden,
                    onCheckedChange = onChatFormatToolbarHiddenChange,
                )
            },
        )

        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_chats_hide_backup_reminder)) },
            supportingContent = {
                Text(stringResource(R.string.settings_chats_hide_backup_reminder_hint))
            },
            trailingContent = {
                Switch(
                    checked = encryptionReminderHidden,
                    onCheckedChange = onEncryptionReminderHiddenChange,
                )
            },
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        SettingsSectionHeader(stringResource(R.string.settings_chats_section_backup))

        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_chats_backup_item)) },
            supportingContent = {
                Text(stringResource(R.string.settings_chats_backup_item_hint))
            },
            leadingContent = {
                Icon(Icons.Outlined.Backup, contentDescription = null)
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenChatBackup),
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        SettingsSectionHeader(stringResource(R.string.messages_export_import_title))

        Text(
            text = stringResource(R.string.messages_export_import_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        ListItem(
            headlineContent = { Text(stringResource(R.string.messages_export_chats)) },
            leadingContent = { Icon(Icons.Outlined.CloudUpload, contentDescription = null) },
            trailingContent = {
                TextButton(onClick = onExportChats, enabled = !isChatsTransferBusy) {
                    Text(stringResource(R.string.settings_signal_backup_action))
                }
            },
        )

        ListItem(
            headlineContent = { Text(stringResource(R.string.messages_import_chats)) },
            leadingContent = { Icon(Icons.Outlined.CloudDownload, contentDescription = null) },
            trailingContent = {
                TextButton(onClick = onImportChats, enabled = !isChatsTransferBusy) {
                    Text(stringResource(R.string.settings_signal_backup_action))
                }
            },
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}
