package org.cyblight.android.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Swipe
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import org.cyblight.android.R
import org.cyblight.android.data.preferences.ThemeMode
import org.cyblight.android.data.repository.SecurityOverview
import org.cyblight.android.i18n.LocaleManager

@Composable
fun SettingsHubScreen(
    locale: String,
    themeMode: ThemeMode,
    notificationsEnabled: Boolean,
    appLockEnabled: Boolean,
    swipeBackEnabled: Boolean,
    securityOverview: SecurityOverview?,
    onNavigate: (SettingsSection) -> Unit,
    onHelp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        item {
            SettingsHubItem(
                icon = Icons.Outlined.Shield,
                title = stringResource(R.string.nav_tab_security),
                subtitle = securityHubSubtitle(securityOverview),
                onClick = { onNavigate(SettingsSection.Security) },
            )
        }
        item {
            SettingsHubItem(
                icon = Icons.Outlined.Lock,
                title = stringResource(R.string.settings_hub_app_lock_title),
                subtitle = if (appLockEnabled) {
                    stringResource(R.string.settings_hub_app_lock_enabled)
                } else {
                    stringResource(R.string.settings_hub_app_lock_disabled)
                },
                onClick = { onNavigate(SettingsSection.AppLock) },
            )
        }
        item {
            SettingsHubItem(
                icon = Icons.AutoMirrored.Outlined.Chat,
                title = stringResource(R.string.settings_section_chats),
                subtitle = stringResource(R.string.settings_hub_chats_subtitle),
                onClick = { onNavigate(SettingsSection.Chats) },
            )
        }
        item {
            SettingsHubItem(
                icon = Icons.Outlined.Notifications,
                title = stringResource(R.string.settings_section_notifications),
                subtitle = if (notificationsEnabled) {
                    stringResource(R.string.settings_notifications_enabled)
                } else {
                    stringResource(R.string.settings_notifications_disabled)
                },
                onClick = { onNavigate(SettingsSection.Notifications) },
            )
        }
        item {
            SettingsHubItem(
                icon = Icons.Outlined.Palette,
                title = stringResource(R.string.settings_section_appearance),
                subtitle = stringResource(
                    R.string.settings_hub_appearance_subtitle,
                    themeModeLabel(themeMode),
                    LocaleManager.displayName(locale),
                ),
                onClick = { onNavigate(SettingsSection.Appearance) },
            )
        }
        item {
            SettingsHubItem(
                icon = Icons.Outlined.Swipe,
                title = stringResource(R.string.settings_section_gestures),
                subtitle = if (swipeBackEnabled) {
                    stringResource(R.string.settings_hub_gestures_enabled)
                } else {
                    stringResource(R.string.settings_hub_gestures_disabled)
                },
                onClick = { onNavigate(SettingsSection.Gestures) },
            )
        }
        item {
            SettingsHubItem(
                icon = Icons.Outlined.BatteryChargingFull,
                title = stringResource(R.string.settings_section_background),
                subtitle = stringResource(R.string.settings_hub_background_subtitle),
                onClick = { onNavigate(SettingsSection.Background) },
            )
        }
        item {
            SettingsHubItem(
                icon = Icons.AutoMirrored.Outlined.HelpOutline,
                title = stringResource(R.string.menu_help),
                subtitle = stringResource(R.string.help_menu_hint),
                onClick = onHelp,
            )
        }
        item {
            SettingsHubItem(
                icon = Icons.Outlined.Info,
                title = stringResource(R.string.settings_section_about),
                subtitle = stringResource(R.string.settings_hub_about_subtitle),
                onClick = { onNavigate(SettingsSection.About) },
            )
        }
    }
}

@Composable
private fun SettingsHubItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = {
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    )
}

@Composable
private fun securityHubSubtitle(overview: SecurityOverview?): String {
    if (overview == null) return stringResource(R.string.security_hint)
    val score = overview.securityScore
    return when {
        score >= 100 -> stringResource(R.string.security_check_subtitle_protected)
        score >= 50 -> stringResource(R.string.security_level_medium)
        else -> stringResource(R.string.security_check_subtitle_recommendations)
    }
}