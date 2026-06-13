package org.cyblight.android.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.SettingsApplications
import androidx.compose.material.icons.outlined.Swipe
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.cyblight.android.BuildConfig
import org.cyblight.android.R
import org.cyblight.android.util.ExternalLinks
import org.cyblight.android.data.preferences.AppLockTimeout
import org.cyblight.android.data.preferences.RootBackBehavior
import org.cyblight.android.data.preferences.SwipeBackEdgeWidth
import org.cyblight.android.data.preferences.SwipeBackSensitivity
import org.cyblight.android.data.preferences.ThemeMode
import org.cyblight.android.i18n.LocaleManager
import org.cyblight.android.ui.applock.PinSetupDialog
import org.cyblight.android.util.SystemSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    locale: String,
    themeMode: ThemeMode,
    notificationsEnabled: Boolean,
    loginAlertsEnabled: Boolean,
    messageAlertsEnabled: Boolean,
    appLockEnabled: Boolean,
    appLockBiometric: Boolean,
    appLockPinConfigured: Boolean,
    appLockTimeout: AppLockTimeout,
    biometricAvailable: Boolean,
    onBack: () -> Unit,
    onLocaleSelected: (String) -> Unit,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onNotificationsEnabledChange: (Boolean) -> Unit,
    onLoginAlertsEnabledChange: (Boolean) -> Unit,
    onMessageAlertsEnabledChange: (Boolean) -> Unit,
    onAppLockEnabledChange: (Boolean) -> Unit,
    onAppLockBiometricChange: (Boolean) -> Unit,
    onSetupAppLockPin: (String, Boolean) -> Unit,
    onAppLockTimeoutSelected: (AppLockTimeout) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onHelp: () -> Unit,
    onOpenLightCatcherGame: () -> Unit,
    swipeBackEnabled: Boolean,
    systemBackEnabled: Boolean,
    swipeBackSensitivity: SwipeBackSensitivity,
    swipeBackEdgeWidth: SwipeBackEdgeWidth,
    rootBackBehavior: RootBackBehavior,
    onSwipeBackEnabledChange: (Boolean) -> Unit,
    onSystemBackEnabledChange: (Boolean) -> Unit,
    onSwipeBackSensitivitySelected: (SwipeBackSensitivity) -> Unit,
    onSwipeBackEdgeWidthSelected: (SwipeBackEdgeWidth) -> Unit,
    onRootBackBehaviorSelected: (RootBackBehavior) -> Unit,
    accountLogin: String,
    onCreateSignalBackup: suspend (password: String) -> Result<String>,
    onRestoreSignalBackup: suspend (content: String, password: String) -> Result<Unit>,
    signalBackupErrorMessage: (String) -> String,
    onPickBackupFile: (onPicked: (String) -> Unit) -> Unit,
    focusSignalBackup: Boolean = false,
    onSignalBackupFocused: () -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var languageMenuExpanded by remember { mutableStateOf(false) }
    var themeMenuExpanded by remember { mutableStateOf(false) }
    var appLockTimeoutMenuExpanded by remember { mutableStateOf(false) }
    var swipeSensitivityMenuExpanded by remember { mutableStateOf(false) }
    var swipeEdgeMenuExpanded by remember { mutableStateOf(false) }
    var rootBackMenuExpanded by remember { mutableStateOf(false) }
    var systemNotificationsEnabled by remember {
        mutableStateOf(SystemSettings.areNotificationsEnabled(context))
    }
    var versionTapCount by remember { mutableStateOf(0) }
    var showPinSetup by remember { mutableStateOf(false) }
    var enableLockAfterPinSetup by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    var signalBackupScrollY by remember { mutableStateOf(0) }

    LaunchedEffect(focusSignalBackup, signalBackupScrollY) {
        if (focusSignalBackup && signalBackupScrollY > 0) {
            scrollState.animateScrollTo(signalBackupScrollY)
            onSignalBackupFocused()
        }
    }

    if (showPinSetup) {
        PinSetupDialog(
            title = stringResource(
                if (appLockPinConfigured) {
                    R.string.app_lock_change_pin
                } else {
                    R.string.app_lock_setup_pin
                },
            ),
            onDismiss = { showPinSetup = false },
            onConfirm = { pin ->
                onSetupAppLockPin(pin, enableLockAfterPinSetup)
                showPinSetup = false
                enableLockAfterPinSetup = false
            },
        )
    }

    LaunchedEffect(versionTapCount) {
        if (versionTapCount in 1..6) {
            delay(2500)
            versionTapCount = 0
        }
    }

    fun syncNotificationState() {
        val enabled = SystemSettings.areNotificationsEnabled(context)
        systemNotificationsEnabled = enabled
        if (notificationsEnabled != enabled) {
            onNotificationsEnabledChange(enabled)
        }
    }

    LaunchedEffect(Unit, notificationsEnabled) {
        syncNotificationState()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                syncNotificationState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
                .verticalScroll(scrollState),
        ) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.menu_help)) },
                supportingContent = { Text(stringResource(R.string.help_menu_hint)) },
                leadingContent = { Icon(Icons.Outlined.HelpOutline, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onHelp),
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = stringResource(R.string.settings_section_appearance),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            SettingsDropdownRow(
                expanded = themeMenuExpanded,
                onExpandedChange = { themeMenuExpanded = it },
                headline = stringResource(R.string.settings_theme),
                supporting = themeModeLabel(themeMode),
                leadingIcon = { Icon(Icons.Outlined.Palette, contentDescription = null) },
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

            SettingsDropdownRow(
                expanded = languageMenuExpanded,
                onExpandedChange = { languageMenuExpanded = it },
                headline = stringResource(R.string.language),
                supporting = LocaleManager.displayName(locale),
                leadingIcon = { Icon(Icons.Outlined.Language, contentDescription = null) },
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
                text = stringResource(R.string.settings_section_gestures),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_swipe_back)) },
                supportingContent = { Text(stringResource(R.string.settings_swipe_back_hint)) },
                leadingContent = { Icon(Icons.Outlined.Swipe, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = swipeBackEnabled,
                        onCheckedChange = onSwipeBackEnabledChange,
                    )
                },
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_system_back)) },
                supportingContent = { Text(stringResource(R.string.settings_system_back_hint)) },
                leadingContent = { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = systemBackEnabled,
                        onCheckedChange = onSystemBackEnabledChange,
                    )
                },
            )

            SettingsDropdownRow(
                expanded = swipeSensitivityMenuExpanded,
                onExpandedChange = { swipeSensitivityMenuExpanded = it },
                headline = stringResource(R.string.settings_swipe_sensitivity),
                supporting = swipeBackSensitivityLabel(swipeBackSensitivity),
                leadingIcon = { Icon(Icons.Outlined.Swipe, contentDescription = null) },
            ) {
                SwipeBackSensitivity.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(swipeBackSensitivityLabel(option)) },
                        onClick = {
                            swipeSensitivityMenuExpanded = false
                            onSwipeBackSensitivitySelected(option)
                        },
                    )
                }
            }

            SettingsDropdownRow(
                expanded = swipeEdgeMenuExpanded,
                onExpandedChange = { swipeEdgeMenuExpanded = it },
                headline = stringResource(R.string.settings_swipe_edge),
                supporting = swipeBackEdgeWidthLabel(swipeBackEdgeWidth),
                leadingIcon = { Icon(Icons.Outlined.Swipe, contentDescription = null) },
            ) {
                SwipeBackEdgeWidth.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(swipeBackEdgeWidthLabel(option)) },
                        onClick = {
                            swipeEdgeMenuExpanded = false
                            onSwipeBackEdgeWidthSelected(option)
                        },
                    )
                }
            }

            SettingsDropdownRow(
                expanded = rootBackMenuExpanded,
                onExpandedChange = { rootBackMenuExpanded = it },
                headline = stringResource(R.string.settings_root_back_behavior),
                supporting = rootBackBehaviorLabel(rootBackBehavior),
                leadingIcon = { Icon(Icons.Outlined.Swipe, contentDescription = null) },
            ) {
                RootBackBehavior.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(rootBackBehaviorLabel(option)) },
                        onClick = {
                            rootBackMenuExpanded = false
                            onRootBackBehaviorSelected(option)
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
                        if (systemNotificationsEnabled) {
                            stringResource(R.string.settings_notifications_enabled)
                        } else {
                            stringResource(R.string.settings_notifications_disabled)
                        },
                    )
                },
                leadingContent = { Icon(Icons.Outlined.Notifications, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = systemNotificationsEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                val alreadyAllowed = SystemSettings.areNotificationsEnabled(context)
                                if (alreadyAllowed) {
                                    onNotificationsEnabledChange(true)
                                    systemNotificationsEnabled = true
                                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS,
                                    ) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    onRequestNotificationPermission()
                                } else {
                                    SystemSettings.openAppNotificationSettings(context)
                                }
                            } else {
                                onNotificationsEnabledChange(false)
                                systemNotificationsEnabled = false
                                SystemSettings.openAppNotificationSettings(context)
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

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_login_alerts)) },
                supportingContent = { Text(stringResource(R.string.settings_login_alerts_hint)) },
                leadingContent = { Icon(Icons.Outlined.Notifications, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = loginAlertsEnabled && systemNotificationsEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && !systemNotificationsEnabled) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    onRequestNotificationPermission()
                                } else {
                                    SystemSettings.openAppNotificationSettings(context)
                                }
                            } else {
                                onLoginAlertsEnabledChange(enabled)
                            }
                        },
                        enabled = systemNotificationsEnabled,
                    )
                },
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_message_alerts)) },
                supportingContent = { Text(stringResource(R.string.settings_message_alerts_hint)) },
                leadingContent = { Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = messageAlertsEnabled && systemNotificationsEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && !systemNotificationsEnabled) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    onRequestNotificationPermission()
                                } else {
                                    SystemSettings.openAppNotificationSettings(context)
                                }
                            } else {
                                onMessageAlertsEnabledChange(enabled)
                            }
                        },
                        enabled = systemNotificationsEnabled,
                    )
                },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = stringResource(R.string.settings_section_security),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_app_lock)) },
                supportingContent = { Text(stringResource(R.string.settings_app_lock_hint)) },
                leadingContent = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = appLockEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                if (appLockPinConfigured) {
                                    onAppLockEnabledChange(true)
                                } else {
                                    enableLockAfterPinSetup = true
                                    showPinSetup = true
                                }
                            } else {
                                onAppLockEnabledChange(false)
                            }
                        },
                    )
                },
            )

            if (appLockPinConfigured) {
                SettingsDropdownRow(
                    expanded = appLockTimeoutMenuExpanded,
                    onExpandedChange = { appLockTimeoutMenuExpanded = it },
                    headline = stringResource(R.string.settings_app_lock_timeout),
                    supporting = appLockTimeoutLabel(appLockTimeout),
                    leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                ) {
                    AppLockTimeout.entries.forEach { timeout ->
                        DropdownMenuItem(
                            text = { Text(appLockTimeoutLabel(timeout)) },
                            onClick = {
                                appLockTimeoutMenuExpanded = false
                                onAppLockTimeoutSelected(timeout)
                            },
                        )
                    }
                }
            }

            if (biometricAvailable) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_app_lock_biometric)) },
                    leadingContent = { Icon(Icons.Outlined.Fingerprint, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = appLockBiometric,
                            onCheckedChange = onAppLockBiometricChange,
                            enabled = appLockPinConfigured,
                        )
                    },
                )
            }

            ListItem(
                headlineContent = { Text(stringResource(R.string.app_lock_change_pin)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        enableLockAfterPinSetup = false
                        showPinSetup = true
                    },
            )

            Box(
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    signalBackupScrollY = coordinates.positionInParent().y.toInt()
                },
            ) {
                SignalBackupSection(
                    accountLogin = accountLogin,
                    onCreateBackup = onCreateSignalBackup,
                    onRestoreBackup = onRestoreSignalBackup,
                    backupErrorMessage = signalBackupErrorMessage,
                    onPickBackupFile = onPickBackupFile,
                )
            }

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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.about_description),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(
                        R.string.about_creator,
                        stringResource(R.string.about_creator_value),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(R.string.about_website_link),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                    ),
                    modifier = Modifier.clickable {
                        ExternalLinks.openUrl(context, BuildConfig.WEBSITE_URL)
                    },
                )
            }

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_version)) },
                supportingContent = { Text(BuildConfig.VERSION_NAME) },
                leadingContent = { Icon(Icons.Outlined.Info, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        versionTapCount++
                        if (versionTapCount >= 7) {
                            versionTapCount = 0
                            scope.launch {
                                delay(200)
                                onOpenLightCatcherGame()
                            }
                        }
                    },
            )
        }
    }
}

@Composable
private fun SettingsDropdownRow(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    headline: String,
    supporting: String,
    leadingIcon: @Composable () -> Unit,
    menuContent: @Composable () -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text(headline) },
            supportingContent = { Text(supporting) },
            leadingContent = leadingIcon,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpandedChange(true) },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            menuContent()
        }
    }
}

@Composable
private fun themeModeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
    ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
    ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
}

@Composable
private fun appLockTimeoutLabel(timeout: AppLockTimeout): String = when (timeout) {
    AppLockTimeout.IMMEDIATE -> stringResource(R.string.settings_app_lock_timeout_immediate)
    AppLockTimeout.SEC_30 -> stringResource(R.string.settings_app_lock_timeout_30s)
    AppLockTimeout.MIN_1 -> stringResource(R.string.settings_app_lock_timeout_1m)
    AppLockTimeout.MIN_5 -> stringResource(R.string.settings_app_lock_timeout_5m)
    AppLockTimeout.MIN_15 -> stringResource(R.string.settings_app_lock_timeout_15m)
}

@Composable
private fun swipeBackSensitivityLabel(option: SwipeBackSensitivity): String = when (option) {
    SwipeBackSensitivity.LOW -> stringResource(R.string.settings_swipe_sensitivity_low)
    SwipeBackSensitivity.NORMAL -> stringResource(R.string.settings_swipe_sensitivity_normal)
    SwipeBackSensitivity.HIGH -> stringResource(R.string.settings_swipe_sensitivity_high)
}

@Composable
private fun swipeBackEdgeWidthLabel(option: SwipeBackEdgeWidth): String = when (option) {
    SwipeBackEdgeWidth.NARROW -> stringResource(R.string.settings_swipe_edge_narrow)
    SwipeBackEdgeWidth.NORMAL -> stringResource(R.string.settings_swipe_edge_normal)
    SwipeBackEdgeWidth.WIDE -> stringResource(R.string.settings_swipe_edge_wide)
}

@Composable
private fun rootBackBehaviorLabel(option: RootBackBehavior): String = when (option) {
    RootBackBehavior.HOME_THEN_EXIT -> stringResource(R.string.settings_root_back_home_then_exit)
    RootBackBehavior.EXIT_IMMEDIATELY -> stringResource(R.string.settings_root_back_exit)
    RootBackBehavior.MINIMIZE -> stringResource(R.string.settings_root_back_minimize)
}
