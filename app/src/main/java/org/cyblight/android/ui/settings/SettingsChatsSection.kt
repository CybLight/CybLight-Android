package org.cyblight.android.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.cyblight.android.R
import org.cyblight.android.data.preferences.ChatDefaultTheme
import org.cyblight.android.data.preferences.ChatFontSize

@Composable
internal fun SettingsChatsSection(
    chatDefaultTheme: ChatDefaultTheme,
    chatSendWithEnter: Boolean,
    chatFontSize: ChatFontSize,
    chatFormatToolbarHidden: Boolean,
    encryptionReminderHidden: Boolean,
    isChatsTransferBusy: Boolean,
    onOpenChatTheme: () -> Unit,
    onChatSendWithEnterChange: (Boolean) -> Unit,
    onOpenChatFontSize: () -> Unit,
    onChatFormatToolbarHiddenChange: (Boolean) -> Unit,
    onEncryptionReminderHiddenChange: (Boolean) -> Unit,
    onOpenChatBackup: () -> Unit,
    onExportChats: () -> Unit,
    onImportChats: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
