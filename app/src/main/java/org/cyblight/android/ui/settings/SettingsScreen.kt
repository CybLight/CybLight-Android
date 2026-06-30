package org.cyblight.android.ui.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.cyblight.android.R
import org.cyblight.android.data.preferences.AppLockTimeout
import org.cyblight.android.data.preferences.RootBackBehavior
import org.cyblight.android.data.preferences.SwipeBackEdgeWidth
import org.cyblight.android.data.preferences.SwipeBackSensitivity
import org.cyblight.android.data.preferences.ChatDefaultTheme
import org.cyblight.android.data.preferences.ChatFontSize
import org.cyblight.android.data.preferences.ThemeMode
import org.cyblight.android.data.repository.SecurityOverview
import org.cyblight.android.ui.security.SecurityScreen

@Composable
fun SettingsScreen(
    settingsSection: SettingsSection,
    onSettingsSectionChange: (SettingsSection) -> Unit,
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
    homeWhatsNewBannerHidden: Boolean,
    onHomeWhatsNewBannerHiddenChange: (Boolean) -> Unit,
    accountLogin: String,
    onCreateSignalBackup: suspend (password: String) -> Result<String>,
    onRestoreSignalBackup: suspend (content: String, password: String, onProgress: (Int, String) -> Unit) -> Result<Unit>,
    signalBackupErrorMessage: (String) -> String,
    onPickBackupFile: (onPicked: (String) -> Unit) -> Unit,
    onSaveBackupFile: (fileName: String, content: String, onResult: (Boolean?) -> Unit) -> Unit,
    chatFormatToolbarHidden: Boolean = false,
    chatDefaultTheme: ChatDefaultTheme = ChatDefaultTheme.SYSTEM,
    chatQuoteColor: Int? = null,
    chatSendWithEnter: Boolean = false,
    chatFontSize: ChatFontSize = ChatFontSize.MEDIUM,
    encryptionReminderHidden: Boolean = false,
    isChatsTransferBusy: Boolean = false,
    onChatFormatToolbarHiddenChange: (Boolean) -> Unit = {},
    onChatDefaultThemeSelected: (ChatDefaultTheme) -> Unit = {},
    onChatSendWithEnterChange: (Boolean) -> Unit = {},
    onChatFontSizeSelected: (ChatFontSize) -> Unit = {},
    onChatQuoteColorSelected: (Int?) -> Unit = {},
    onEncryptionReminderHiddenChange: (Boolean) -> Unit = {},
    onExportChats: () -> Unit = {},
    onImportChats: () -> Unit = {},
    focusChatBackup: Boolean = false,
    onChatBackupFocused: () -> Unit = {},
    isGoogleDriveConfigured: Boolean = false,
    googleDriveAccountLabel: String? = null,
    googleDriveAccountEmail: String? = null,
    googleDriveAccountEmails: List<String> = emptyList(),
    googleDriveBackupMetadata: org.cyblight.android.integrations.google_drive.DriveBackupMetadata? = null,
    googleDriveStorageQuota: org.cyblight.android.integrations.google_drive.GoogleDriveStorageQuota? = null,
    onOpenGoogleStorage: () -> Unit = {},
    onGoogleDriveSignIn: () -> Unit = {},
    onGoogleDriveAccountSelected: (String?) -> Unit = {},
    onRefreshGoogleDriveStatus: suspend () -> Unit = {},
    onUploadGoogleDriveBackup: suspend (password: String?, onProgress: (Int, String) -> Unit) -> Result<Unit> = { _, _ -> Result.success(Unit) },
    onRestoreGoogleDriveBackup: suspend (password: String?, onProgress: (Int, String) -> Unit) -> Result<org.cyblight.android.crypto.backup.BackupRestoreStats> = { _, _ -> Result.success(org.cyblight.android.crypto.backup.BackupRestoreStats()) },
    onDeleteGoogleDriveBackup: suspend () -> Result<Boolean> = { Result.success(false) },
    onGoogleDriveSignOut: suspend () -> Unit = {},
    chatBackupFrequency: org.cyblight.android.data.preferences.ChatBackupFrequency =
        org.cyblight.android.data.preferences.ChatBackupFrequency.OFF,
    chatBackupOverCellular: Boolean = false,
    chatBackupHasStoredPassword: Boolean = false,
    onChatBackupFrequencySelected: (org.cyblight.android.data.preferences.ChatBackupFrequency) -> Unit = {},
    onChatBackupOverCellularChange: (Boolean) -> Unit = {},
    onEnableAutoBackup: suspend (org.cyblight.android.data.preferences.ChatBackupFrequency, String) -> Result<Unit> =
        { _, _ -> Result.success(Unit) },
    onSaveBackupPassword: suspend (String) -> Result<Unit> = { Result.success(Unit) },
    onChangeBackupPassword: suspend (String, String) -> Result<Unit> = { _, _ -> Result.success(Unit) },
    onDisableBackupPassword: suspend () -> Result<Unit> = { Result.success(Unit) },
    onClearStoredBackupPassword: suspend () -> Result<Unit> = { Result.success(Unit) },
    securityOverview: SecurityOverview? = null,
    isSecurityLoading: Boolean = false,
    isSecurityRefreshing: Boolean = false,
    onRefreshSecurity: () -> Unit = {},
    onOpenSecurityCheck: () -> Unit = {},
    onOpenAccountSecurity: () -> Unit = {},
    onOpenPasskeys: () -> Unit = {},
    onOpenTrustedDevices: () -> Unit = {},
    onOpenLoginHistory: () -> Unit = {},
    onOpenSessions: () -> Unit = {},
) {
    LaunchedEffect(focusChatBackup) {
        if (focusChatBackup) {
            onSettingsSectionChange(SettingsSection.ChatBackup)
        }
    }

    LaunchedEffect(settingsSection) {
        if (settingsSection == SettingsSection.Security) {
            onRefreshSecurity()
        }
        if (settingsSection == SettingsSection.ChatBackup || settingsSection == SettingsSection.Chats) {
            onRefreshGoogleDriveStatus()
        }
    }

    val sectionBack: () -> Unit = {
        when (settingsSection) {
            SettingsSection.Hub -> onBack()
            SettingsSection.ChatBackup,
            SettingsSection.ChatTheme,
            SettingsSection.ChatFontSize,
            -> onSettingsSectionChange(SettingsSection.Chats)
            else -> onSettingsSectionChange(SettingsSection.Hub)
        }
    }

    val title = when (settingsSection) {
        SettingsSection.Hub -> stringResource(R.string.settings_title)
        SettingsSection.Security -> stringResource(R.string.nav_tab_security)
        SettingsSection.AppLock -> stringResource(R.string.settings_hub_app_lock_title)
        SettingsSection.Chats -> stringResource(R.string.settings_section_chats)
        SettingsSection.ChatTheme -> stringResource(R.string.settings_chats_default_theme)
        SettingsSection.ChatFontSize -> stringResource(R.string.settings_chats_font_size)
        SettingsSection.ChatBackup -> stringResource(R.string.settings_chats_backup_item)
        SettingsSection.Notifications -> stringResource(R.string.settings_section_notifications)
        SettingsSection.Appearance -> stringResource(R.string.settings_section_appearance)
        SettingsSection.Gestures -> stringResource(R.string.settings_section_gestures)
        SettingsSection.Background -> stringResource(R.string.settings_section_background)
        SettingsSection.About -> stringResource(R.string.settings_section_about)
    }

    SettingsSectionScaffold(
        title = title,
        onBack = sectionBack,
    ) { padding ->
        when (settingsSection) {
            SettingsSection.Hub -> {
                SettingsHubScreen(
                    locale = locale,
                    themeMode = themeMode,
                    notificationsEnabled = notificationsEnabled,
                    appLockEnabled = appLockEnabled,
                    swipeBackEnabled = swipeBackEnabled,
                    securityOverview = securityOverview,
                    onNavigate = onSettingsSectionChange,
                    onHelp = onHelp,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                )
            }
            SettingsSection.Security -> {
                SecurityScreen(
                    overview = securityOverview,
                    isLoading = isSecurityLoading,
                    isRefreshing = isSecurityRefreshing,
                    onRefresh = onRefreshSecurity,
                    onOpenSecurityCheck = onOpenSecurityCheck,
                    onOpenEmail = onOpenAccountSecurity,
                    onOpenPassword = onOpenAccountSecurity,
                    onOpenTwoFactor = onOpenAccountSecurity,
                    onOpenPasskeys = onOpenPasskeys,
                    onOpenTrustedDevices = onOpenTrustedDevices,
                    onOpenLoginHistory = onOpenLoginHistory,
                    onOpenSessions = onOpenSessions,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                )
            }
            SettingsSection.AppLock -> {
                SettingsAppLockSection(
                    appLockEnabled = appLockEnabled,
                    appLockBiometric = appLockBiometric,
                    appLockPinConfigured = appLockPinConfigured,
                    appLockTimeout = appLockTimeout,
                    biometricAvailable = biometricAvailable,
                    onAppLockEnabledChange = onAppLockEnabledChange,
                    onAppLockBiometricChange = onAppLockBiometricChange,
                    onAppLockTimeoutSelected = onAppLockTimeoutSelected,
                    onSetupAppLockPin = onSetupAppLockPin,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                )
            }
            SettingsSection.Chats -> {
                SettingsChatsSection(
                    chatDefaultTheme = chatDefaultTheme,
                    chatSendWithEnter = chatSendWithEnter,
                    chatFontSize = chatFontSize,
                    chatQuoteColor = chatQuoteColor,
                    chatFormatToolbarHidden = chatFormatToolbarHidden,
                    encryptionReminderHidden = encryptionReminderHidden,
                    isChatsTransferBusy = isChatsTransferBusy,
                    onOpenChatTheme = { onSettingsSectionChange(SettingsSection.ChatTheme) },
                    onChatSendWithEnterChange = onChatSendWithEnterChange,
                    onOpenChatFontSize = { onSettingsSectionChange(SettingsSection.ChatFontSize) },
                    onChatFormatToolbarHiddenChange = onChatFormatToolbarHiddenChange,
                    onEncryptionReminderHiddenChange = onEncryptionReminderHiddenChange,
                    onChatQuoteColorChange = onChatQuoteColorSelected,
                    onOpenChatBackup = { onSettingsSectionChange(SettingsSection.ChatBackup) },
                    onExportChats = onExportChats,
                    onImportChats = onImportChats,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                )
            }
            SettingsSection.ChatTheme -> {
                ChatDefaultThemeScreen(
                    selectedTheme = chatDefaultTheme,
                    onThemeSelected = onChatDefaultThemeSelected,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                )
            }
            SettingsSection.ChatFontSize -> {
                ChatFontSizeScreen(
                    selectedSize = chatFontSize,
                    onSizeSelected = onChatFontSizeSelected,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                )
            }
            SettingsSection.ChatBackup -> {
                LaunchedEffect(focusChatBackup) {
                    if (focusChatBackup) {
                        onChatBackupFocused()
                    }
                }
                ChatBackupSettingsScreen(
                    accountLogin = accountLogin,
                    isGoogleDriveConfigured = isGoogleDriveConfigured,
                    googleDriveAccountLabel = googleDriveAccountLabel,
                    googleDriveAccountEmail = googleDriveAccountEmail,
                    googleDriveAccountEmails = googleDriveAccountEmails,
                    googleDriveBackupMetadata = googleDriveBackupMetadata,
                    googleDriveStorageQuota = googleDriveStorageQuota,
                    onOpenGoogleStorage = onOpenGoogleStorage,
                    onRefreshGoogleDriveStatus = onRefreshGoogleDriveStatus,
                    onGoogleDriveSignIn = onGoogleDriveSignIn,
                    onGoogleDriveAccountSelected = onGoogleDriveAccountSelected,
                    onGoogleDriveSignOut = onGoogleDriveSignOut,
                    onUploadGoogleDriveBackup = onUploadGoogleDriveBackup,
                    onRestoreGoogleDriveBackup = onRestoreGoogleDriveBackup,
                    onDeleteGoogleDriveBackup = onDeleteGoogleDriveBackup,
                    chatBackupFrequency = chatBackupFrequency,
                    chatBackupOverCellular = chatBackupOverCellular,
                    chatBackupHasStoredPassword = chatBackupHasStoredPassword,
                    onChatBackupFrequencySelected = onChatBackupFrequencySelected,
                    onChatBackupOverCellularChange = onChatBackupOverCellularChange,
                    onEnableAutoBackup = onEnableAutoBackup,
                    onSaveBackupPassword = onSaveBackupPassword,
                    onChangeBackupPassword = onChangeBackupPassword,
                    onDisableBackupPassword = onDisableBackupPassword,
                    onClearStoredBackupPassword = onClearStoredBackupPassword,
                    onCreateSignalBackup = onCreateSignalBackup,
                    onRestoreSignalBackup = onRestoreSignalBackup,
                    signalBackupErrorMessage = signalBackupErrorMessage,
                    onPickBackupFile = onPickBackupFile,
                    onSaveBackupFile = onSaveBackupFile,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                )
            }
            SettingsSection.Notifications -> {
                SettingsNotificationsSection(
                    loginAlertsEnabled = loginAlertsEnabled,
                    messageAlertsEnabled = messageAlertsEnabled,
                    notificationsEnabled = notificationsEnabled,
                    onNotificationsEnabledChange = onNotificationsEnabledChange,
                    onLoginAlertsEnabledChange = onLoginAlertsEnabledChange,
                    onMessageAlertsEnabledChange = onMessageAlertsEnabledChange,
                    onRequestNotificationPermission = onRequestNotificationPermission,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                )
            }
            SettingsSection.Appearance -> {
                SettingsAppearanceSection(
                    locale = locale,
                    themeMode = themeMode,
                    homeWhatsNewBannerHidden = homeWhatsNewBannerHidden,
                    onHomeWhatsNewBannerHiddenChange = onHomeWhatsNewBannerHiddenChange,
                    onLocaleSelected = onLocaleSelected,
                    onThemeModeSelected = onThemeModeSelected,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                )
            }
            SettingsSection.Gestures -> {
                SettingsGesturesSection(
                    swipeBackEnabled = swipeBackEnabled,
                    systemBackEnabled = systemBackEnabled,
                    swipeBackSensitivity = swipeBackSensitivity,
                    swipeBackEdgeWidth = swipeBackEdgeWidth,
                    rootBackBehavior = rootBackBehavior,
                    onSwipeBackEnabledChange = onSwipeBackEnabledChange,
                    onSystemBackEnabledChange = onSystemBackEnabledChange,
                    onSwipeBackSensitivitySelected = onSwipeBackSensitivitySelected,
                    onSwipeBackEdgeWidthSelected = onSwipeBackEdgeWidthSelected,
                    onRootBackBehaviorSelected = onRootBackBehaviorSelected,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                )
            }
            SettingsSection.Background -> {
                SettingsBackgroundSection(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                )
            }
            SettingsSection.About -> {
                SettingsAboutSection(
                    onOpenLightCatcherGame = onOpenLightCatcherGame,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                )
            }
        }
    }
}
