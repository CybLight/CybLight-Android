package org.cyblight.android.ui.settings

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.SettingsApplications
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.cyblight.android.R
import org.cyblight.android.data.preferences.ThemeMode
import org.cyblight.android.i18n.LocaleManager
import org.cyblight.android.util.SystemSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    locale: String,
    themeMode: ThemeMode,
    notificationsEnabled: Boolean,
    onBack: () -> Unit,
    onLocaleSelected: (String) -> Unit,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onNotificationsEnabledChange: (Boolean) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onAbout: () -> Unit,
) {
    val context = LocalContext.current
    var languageMenuExpanded by remember { mutableStateOf(false) }
    var themeMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = stringResource(R.string.settings_section_appearance),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_theme)) },
                supportingContent = { Text(themeModeLabel(themeMode)) },
                leadingContent = { Icon(Icons.Outlined.Palette, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { themeMenuExpanded = true },
            )
            DropdownMenu(
                expanded = themeMenuExpanded,
                onDismissRequest = { themeMenuExpanded = false },
            ) {
                ThemeMode.entries.forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(themeModeLabel(mode)) },
                        onClick = {
                            themeMenuExpanded = false
                            onThemeModeSelected(mode)
                        },
                    )
                }
            }

            ListItem(
                headlineContent = { Text(stringResource(R.string.language)) },
                supportingContent = { Text(LocaleManager.displayName(locale)) },
                leadingContent = { Icon(Icons.Outlined.Language, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { languageMenuExpanded = true },
            )
            DropdownMenu(
                expanded = languageMenuExpanded,
                onDismissRequest = { languageMenuExpanded = false },
            ) {
                LocaleManager.supported.forEach { code ->
                    DropdownMenuItem(
                        text = { Text(LocaleManager.displayName(code)) },
                        onClick = {
                            languageMenuExpanded = false
                            onLocaleSelected(code)
                        },
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = stringResource(R.string.settings_section_notifications),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_notifications)) },
                supportingContent = {
                    Text(
                        if (SystemSettings.areNotificationsEnabled(context)) {
                            stringResource(R.string.settings_notifications_enabled)
                        } else {
                            stringResource(R.string.settings_notifications_disabled)
                        },
                    )
                },
                leadingContent = { Icon(Icons.Outlined.Notifications, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                onRequestNotificationPermission()
                            } else {
                                onNotificationsEnabledChange(enabled)
                                if (enabled) {
                                    SystemSettings.openAppNotificationSettings(context)
                                }
                            }
                        },
                    )
                },
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_open_notification_settings)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { SystemSettings.openAppNotificationSettings(context) },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = stringResource(R.string.settings_section_background),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_battery_optimization)) },
                supportingContent = {
                    Text(
                        if (SystemSettings.isIgnoringBatteryOptimizations(context)) {
                            stringResource(R.string.settings_battery_disabled)
                        } else {
                            stringResource(R.string.settings_battery_hint)
                        },
                    )
                },
                leadingContent = { Icon(Icons.Outlined.BatteryChargingFull, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { SystemSettings.openBatteryOptimizationSettings(context) },
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_app_permissions)) },
                supportingContent = { Text(stringResource(R.string.settings_app_permissions_hint)) },
                leadingContent = { Icon(Icons.Outlined.SettingsApplications, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { SystemSettings.openAppDetailsSettings(context) },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = stringResource(R.string.settings_section_about),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.menu_about)) },
                leadingContent = { Icon(Icons.Outlined.Info, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onAbout),
            )
        }
    }
}

@Composable
private fun themeModeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
    ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
    ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
}
