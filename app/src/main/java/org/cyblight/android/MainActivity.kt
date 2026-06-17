package org.cyblight.android

import android.app.Activity
import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.autofill.AutofillManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import org.cyblight.android.ui.AppScreen
import org.cyblight.android.ui.DetailScreen
import org.cyblight.android.ui.easter.LightCatcherGameDialog
import org.cyblight.android.ui.profile.ProfileScreen
import org.cyblight.android.ui.security.LoginHistoryScreen
import org.cyblight.android.ui.security.PasskeysScreen
import org.cyblight.android.ui.security.SecurityCheckScreen
import org.cyblight.android.ui.security.SessionsScreen
import org.cyblight.android.ui.security.TrustedDevicesScreen
import org.cyblight.android.update.ApkInstaller
import org.cyblight.android.i18n.AppLocaleProvider
import org.cyblight.android.notifications.NotificationHelper
import org.cyblight.android.ui.AppViewModel
import org.cyblight.android.ui.login.LoginScreen
import org.cyblight.android.ui.login.TwoFactorScreen
import org.cyblight.android.ui.home.ChangelogScreen
import org.cyblight.android.ui.main.MainScreen
import org.cyblight.android.ui.navigation.SwipeBackContainer
import org.cyblight.android.ui.components.AboutDialog
import org.cyblight.android.ui.help.HelpScreen
import org.cyblight.android.ui.settings.DriveRestoreConfirmDialog
import org.cyblight.android.ui.settings.DriveRestorePasswordDialog
import org.cyblight.android.ui.settings.SettingsScreen
import org.cyblight.android.ui.theme.CybLightTheme
import org.cyblight.android.ui.update.UpdateCheckDialog
import org.cyblight.android.ui.update.UpdateDialog
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.launch
import org.cyblight.android.data.preferences.ThemeMode
import org.cyblight.android.security.BiometricHelper
import org.cyblight.android.ui.applock.AppLockScreen
import org.cyblight.android.util.BugReport

class MainActivity : AppCompatActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        dispatchNotificationIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        viewModel.onAppResumed()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val context = LocalContext.current
            var pendingAutofillCommit by remember { mutableStateOf(false) }
            var showAbout by remember { mutableStateOf(false) }
            var isAppLocked by remember { mutableStateOf(false) }
            var appLockError by remember { mutableStateOf<String?>(null) }
            var appLockSession by remember { mutableIntStateOf(0) }
            val lifecycleOwner = LocalLifecycleOwner.current
            val coroutineScope = rememberCoroutineScope()
            val biometricAvailable = remember { BiometricHelper.isAvailable(this@MainActivity) }
            val isDarkTheme = when (uiState.themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            LaunchedEffect(isDarkTheme, uiState.screen, uiState.easterFlags?.nightGuard) {
                viewModel.trackNightGuardConditions(
                    isDarkTheme = isDarkTheme,
                    isMainScreen = uiState.screen == AppScreen.Main,
                )
            }

            LaunchedEffect(uiState.screen) {
                if (uiState.screen != AppScreen.Main) return@LaunchedEffect
                dispatchNotificationIntent(intent)
            }

            DisposableEffect(lifecycleOwner, uiState.appLockEnabled, uiState.screen, uiState.appLockSettingsLoaded) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_STOP -> {
                            if (uiState.screen == AppScreen.Main) {
                                viewModel.onMainScreenBackgrounded()
                            }
                            if (uiState.screen == AppScreen.Main && uiState.appLockEnabled) {
                                viewModel.onAppBackgrounded()
                            }
                        }
                        Lifecycle.Event.ON_START -> {
                            if (uiState.screen == AppScreen.Main &&
                                uiState.appLockEnabled &&
                                uiState.appLockSettingsLoaded &&
                                viewModel.shouldShowAppLockOnResume()
                            ) {
                                isAppLocked = true
                                appLockSession++
                            }
                        }
                        else -> Unit
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            LaunchedEffect(uiState.screen, uiState.appLockEnabled, uiState.appLockSettingsLoaded) {
                if (uiState.screen == AppScreen.Main &&
                    uiState.appLockEnabled &&
                    uiState.appLockSettingsLoaded
                ) {
                    if (viewModel.shouldShowAppLockOnLaunch()) {
                        isAppLocked = true
                        appLockSession++
                    }
                }
            }

            LaunchedEffect(uiState.pendingAppLock) {
                if (uiState.pendingAppLock && viewModel.consumePendingAppLock()) {
                    isAppLocked = true
                    appLockSession++
                }
            }

            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
            ) { granted ->
                viewModel.setNotificationsEnabled(granted)
                if (!granted && !org.cyblight.android.util.SystemSettings.areNotificationsEnabled(context)) {
                    org.cyblight.android.util.SystemSettings.openAppNotificationSettings(context)
                }
            }

            var backupImportConsumer by remember { mutableStateOf<((String) -> Unit)?>(null) }
            val backupFilePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument(),
            ) { uri ->
                val consumer = backupImportConsumer
                backupImportConsumer = null
                if (uri == null || consumer == null) return@rememberLauncherForActivityResult
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        consumer(stream.bufferedReader().readText())
                    }
                }
            }
            val pickBackupFile: (onPicked: (String) -> Unit) -> Unit = { onPicked ->
                backupImportConsumer = onPicked
                backupFilePickerLauncher.launch(arrayOf("*/*"))
            }

            var backupExportPayload by remember { mutableStateOf<Pair<String, String>?>(null) }
            var backupExportResult by remember { mutableStateOf<((Boolean?) -> Unit)?>(null) }
            val backupSaveLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("application/json"),
            ) { uri ->
                val payload = backupExportPayload
                val onResult = backupExportResult
                backupExportPayload = null
                backupExportResult = null
                if (payload == null || onResult == null) return@rememberLauncherForActivityResult
                if (uri == null) {
                    onResult(null)
                    return@rememberLauncherForActivityResult
                }
                val saved = runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(payload.second.toByteArray(Charsets.UTF_8))
                    } ?: error("backup_save_stream_missing")
                }.isSuccess
                onResult(saved)
            }
            val saveBackupFile: (String, String, (Boolean?) -> Unit) -> Unit = { fileName, content, onResult ->
                backupExportPayload = fileName to content
                backupExportResult = onResult
                backupSaveLauncher.launch(fileName)
            }

            val googleDriveSignInLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult(),
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    viewModel.handleGoogleDriveSignInResult(result.data)
                } else {
                    viewModel.handleGoogleDriveSignInResult(null)
                }
            }

            LaunchedEffect(Unit) {
                viewModel.driveRestoreGoogleSignInRequest.collect {
                    googleDriveSignInLauncher.launch(viewModel.getGoogleDriveSignInIntent())
                }
            }

            var driveRestorePassword by remember { mutableStateOf("") }
            LaunchedEffect(uiState.driveRestoreToast) {
                val toast = viewModel.consumeDriveRestoreToast() ?: return@LaunchedEffect
                Toast.makeText(context, toast.first, Toast.LENGTH_LONG).show()
            }

            var chatsImportConsumer by remember { mutableStateOf<((String) -> Unit)?>(null) }
            val chatsImportLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument(),
            ) { uri ->
                val consumer = chatsImportConsumer
                chatsImportConsumer = null
                if (uri == null || consumer == null) return@rememberLauncherForActivityResult
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        consumer(stream.bufferedReader().readText())
                    }
                }
            }
            val pickChatsFile: (onPicked: (String) -> Unit) -> Unit = { onPicked ->
                chatsImportConsumer = onPicked
                chatsImportLauncher.launch(arrayOf("application/json", "*/*"))
            }

            var chatsExportPayload by remember { mutableStateOf<Pair<String, String>?>(null) }
            var chatsExportResult by remember { mutableStateOf<((Boolean?) -> Unit)?>(null) }
            val chatsSaveLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("application/json"),
            ) { uri ->
                val payload = chatsExportPayload
                val onResult = chatsExportResult
                chatsExportPayload = null
                chatsExportResult = null
                if (payload == null || onResult == null) return@rememberLauncherForActivityResult
                if (uri == null) {
                    onResult(null)
                    return@rememberLauncherForActivityResult
                }
                val saved = runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(payload.second.toByteArray(Charsets.UTF_8))
                    } ?: error("chats_save_stream_missing")
                }.isSuccess
                onResult(saved)
            }
            val saveChatsFile: (String, String, (Boolean?) -> Unit) -> Unit = { fileName, content, onResult ->
                chatsExportPayload = fileName to content
                chatsExportResult = onResult
                chatsSaveLauncher.launch(fileName)
            }

            var chatsTransferBusy by remember { mutableStateOf(false) }

            LaunchedEffect(uiState.screen) {
                if (uiState.screen == AppScreen.Login || uiState.screen == AppScreen.TwoFactor) {
                    pendingAutofillCommit = true
                }
            }

            LaunchedEffect(uiState.screen, uiState.user, uiState.isSubmitting) {
                if (pendingAutofillCommit &&
                    uiState.screen == AppScreen.Main &&
                    uiState.user != null &&
                    !uiState.isSubmitting
                ) {
                    val autofillManager = context.getSystemService(AutofillManager::class.java)
                    if (autofillManager?.isAutofillSupported == true) {
                        autofillManager.commit()
                    }
                    pendingAutofillCommit = false
                }
            }

            AppLocaleProvider(localeTag = uiState.locale) {
            CybLightTheme(themeMode = uiState.themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    val navigationReady = uiState.screen == AppScreen.Main &&
                        !isAppLocked &&
                        uiState.appLockSettingsLoaded
                    val mainContentReady = uiState.screen != AppScreen.Main || uiState.appLockSettingsLoaded

                    val performBackNavigation: () -> Unit = {
                        when (viewModel.handleBackNavigation()) {
                            AppViewModel.BackAction.ExitApp -> finish()
                            AppViewModel.BackAction.MinimizeApp -> moveTaskToBack(true)
                            else -> Unit
                        }
                    }

                    BackHandler(
                        enabled = isAppLocked && uiState.screen == AppScreen.Main,
                    ) {
                        // Consume back while locked so the app cannot be dismissed without unlocking.
                    }

                    BackHandler(
                        enabled = navigationReady && uiState.systemBackEnabled,
                        onBack = performBackNavigation,
                    )

                    if (mainContentReady) {
                    SwipeBackContainer(
                        enabled = navigationReady && uiState.swipeBackEnabled,
                        edgeWidth = uiState.swipeBackEdgeWidth,
                        sensitivity = uiState.swipeBackSensitivity,
                        onSwipeBack = performBackNavigation,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                    when (uiState.detailScreen) {
                        DetailScreen.Settings -> {
                            SettingsScreen(
                                settingsSection = uiState.settingsSection,
                                onSettingsSectionChange = viewModel::setSettingsSection,
                                locale = uiState.locale,
                                themeMode = uiState.themeMode,
                                notificationsEnabled = uiState.notificationsEnabled,
                                loginAlertsEnabled = uiState.loginAlertsEnabled,
                                messageAlertsEnabled = uiState.messageAlertsEnabled,
                                appLockEnabled = uiState.appLockEnabled,
                                appLockBiometric = uiState.appLockBiometric,
                                appLockPinConfigured = uiState.appLockPinConfigured,
                                appLockTimeout = uiState.appLockTimeout,
                                biometricAvailable = biometricAvailable,
                                onBack = viewModel::navigateBack,
                                onLocaleSelected = viewModel::setLocale,
                                onThemeModeSelected = viewModel::setThemeMode,
                                onNotificationsEnabledChange = viewModel::setNotificationsEnabled,
                                onLoginAlertsEnabledChange = viewModel::setLoginAlertsEnabled,
                                onMessageAlertsEnabledChange = viewModel::setMessageAlertsEnabled,
                                onAppLockEnabledChange = viewModel::setAppLockEnabled,
                                onAppLockBiometricChange = viewModel::setAppLockBiometric,
                                onSetupAppLockPin = viewModel::setupAppLockPin,
                                onAppLockTimeoutSelected = viewModel::setAppLockTimeout,
                                onRequestNotificationPermission = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(
                                            Manifest.permission.POST_NOTIFICATIONS,
                                        )
                                    }
                                },
                                onHelp = viewModel::openHelp,
                                onOpenLightCatcherGame = viewModel::openLightCatcherGame,
                                swipeBackEnabled = uiState.swipeBackEnabled,
                                systemBackEnabled = uiState.systemBackEnabled,
                                swipeBackSensitivity = uiState.swipeBackSensitivity,
                                swipeBackEdgeWidth = uiState.swipeBackEdgeWidth,
                                rootBackBehavior = uiState.rootBackBehavior,
                                onSwipeBackEnabledChange = viewModel::setSwipeBackEnabled,
                                onSystemBackEnabledChange = viewModel::setSystemBackEnabled,
                                onSwipeBackSensitivitySelected = viewModel::setSwipeBackSensitivity,
                                onSwipeBackEdgeWidthSelected = viewModel::setSwipeBackEdgeWidth,
                                onRootBackBehaviorSelected = viewModel::setRootBackBehavior,
                                homeWhatsNewBannerHidden = uiState.homeWhatsNewBannerHidden,
                                onHomeWhatsNewBannerHiddenChange = viewModel::setHomeWhatsNewBannerHidden,
                                accountLogin = uiState.user?.login.orEmpty().ifBlank { "user" },
                                onCreateSignalBackup = viewModel::createSignalBackup,
                                onRestoreSignalBackup = viewModel::restoreSignalBackup,
                                signalBackupErrorMessage = viewModel::signalBackupErrorMessage,
                                onPickBackupFile = pickBackupFile,
                                onSaveBackupFile = saveBackupFile,
                                focusChatBackup = uiState.settingsFocusChatBackup,
                                onChatBackupFocused = viewModel::clearSettingsFocusChatBackup,
                                chatFormatToolbarHidden = uiState.chatFormatToolbarHidden,
                                encryptionReminderHidden = uiState.encryptionReminderChatDismissed,
                                isChatsTransferBusy = chatsTransferBusy,
                                onChatFormatToolbarHiddenChange = viewModel::setChatFormatToolbarHidden,
                                onEncryptionReminderHiddenChange = viewModel::setEncryptionReminderChatDismissed,
                                onExportChats = {
                                    if (!chatsTransferBusy) {
                                        chatsTransferBusy = true
                                        viewModel.exportChats(
                                            onReady = { fileName, json, chatCount, messageCount ->
                                                saveChatsFile(fileName, json) { saved ->
                                                    chatsTransferBusy = false
                                                    val message = when (saved) {
                                                        true -> context.getString(
                                                            R.string.messages_export_success,
                                                            chatCount,
                                                            messageCount,
                                                        )
                                                        false -> context.getString(R.string.messages_export_failed)
                                                        null -> return@saveChatsFile
                                                    }
                                                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                                }
                                            },
                                            onError = {
                                                chatsTransferBusy = false
                                                Toast.makeText(
                                                    context,
                                                    context.getString(R.string.messages_export_failed),
                                                    Toast.LENGTH_LONG,
                                                ).show()
                                            },
                                        )
                                    }
                                },
                                onImportChats = {
                                    if (!chatsTransferBusy) {
                                        pickChatsFile { json ->
                                            chatsTransferBusy = true
                                            viewModel.importChats(json) { stats ->
                                                chatsTransferBusy = false
                                                val message = if (stats != null) {
                                                    context.getString(
                                                        R.string.messages_import_success,
                                                        stats.imported,
                                                        stats.skipped,
                                                        stats.errors,
                                                    )
                                                } else {
                                                    context.getString(R.string.messages_import_failed)
                                                }
                                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                },
                                isGoogleDriveConfigured = viewModel.isGoogleDriveConfigured(),
                                googleDriveAccountLabel = uiState.googleDriveAccountLabel,
                                googleDriveBackupMetadata = uiState.googleDriveBackupMetadata,
                                onGoogleDriveSignIn = {
                                    googleDriveSignInLauncher.launch(viewModel.getGoogleDriveSignInIntent())
                                },
                                onRefreshGoogleDriveStatus = viewModel::refreshGoogleDriveStatusSync,
                                onUploadGoogleDriveBackup = viewModel::uploadGoogleDriveBackup,
                                onRestoreGoogleDriveBackup = viewModel::restoreGoogleDriveBackup,
                                onDeleteGoogleDriveBackup = viewModel::deleteGoogleDriveBackup,
                                onGoogleDriveSignOut = viewModel::signOutGoogleDrive,
                                chatBackupFrequency = uiState.chatBackupFrequency,
                                chatBackupOverCellular = uiState.chatBackupOverCellular,
                                chatBackupHasStoredPassword = uiState.chatBackupHasStoredPassword,
                                onChatBackupFrequencySelected = viewModel::setChatBackupFrequency,
                                onChatBackupOverCellularChange = viewModel::setChatBackupOverCellular,
                                onEnableAutoBackup = viewModel::enableAutoBackup,
                                securityOverview = uiState.securityOverview,
                                isSecurityLoading = uiState.isSecurityLoading,
                                isSecurityRefreshing = uiState.isSecurityLoading &&
                                    uiState.securityOverview != null,
                                onRefreshSecurity = viewModel::forceRefreshSecurityOverview,
                                onOpenSecurityCheck = viewModel::openSecurityCheck,
                                onOpenAccountSecurity = { viewModel.openAccountSecurity(context) },
                                onOpenPasskeys = viewModel::openPasskeys,
                                onOpenTrustedDevices = viewModel::openTrustedDevices,
                                onOpenLoginHistory = viewModel::openLoginHistory,
                                onOpenSessions = viewModel::openSessions,
                            )
                        }
                        DetailScreen.Help -> {
                            HelpScreen(onBack = viewModel::navigateBack)
                        }
                        DetailScreen.SecurityCheck -> {
                            SecurityCheckScreen(
                                overview = uiState.securityOverview,
                                onBack = viewModel::navigateBack,
                                onOpenEmail = { viewModel.openAccountSecurity(context) },
                                onOpenTwoFactor = { viewModel.openAccountSecurity(context) },
                                onOpenPasskeys = viewModel::openPasskeys,
                            )
                        }
                        DetailScreen.LoginHistory -> {
                            LoginHistoryScreen(
                                history = uiState.loginHistory,
                                isLoading = uiState.isLoginHistoryLoading,
                                error = uiState.loginHistoryError?.let { code ->
                                    when (code) {
                                        "login_history_load_failed" ->
                                            stringResource(R.string.error_load_login_history)
                                        else -> code
                                    }
                                },
                                onBack = viewModel::navigateBack,
                                onRefresh = viewModel::refreshLoginHistory,
                            )
                        }
                        DetailScreen.TrustedDevices -> {
                            TrustedDevicesScreen(
                                devices = uiState.trustedDevices,
                                isLoading = uiState.isTrustedDevicesLoading,
                                error = uiState.trustedDevicesError?.let { code ->
                                    when (code) {
                                        "trusted_devices_load_failed" ->
                                            stringResource(R.string.error_load_trusted_devices)
                                        "trusted_device_remove_failed" ->
                                            stringResource(R.string.error_remove_trusted_device)
                                        else -> code
                                    }
                                },
                                isRemoving = uiState.isTrustedDeviceRemoving,
                                onBack = viewModel::navigateBack,
                                onRefresh = viewModel::refreshTrustedDevices,
                                onRemove = viewModel::removeTrustedDevice,
                            )
                        }
                        DetailScreen.Passkeys -> {
                            PasskeysScreen(
                                passkeys = uiState.passkeys,
                                isLoading = uiState.isPasskeysLoading,
                                error = uiState.passkeysError?.let { code ->
                                    when (code) {
                                        "passkeys_load_failed" ->
                                            stringResource(R.string.error_load_passkeys)
                                        else -> code
                                    }
                                },
                                isRegistering = uiState.isPasskeyRegistering,
                                registerError = uiState.passkeyRegisterError,
                                isDeleting = uiState.isPasskeyDeleting,
                                deleteError = uiState.passkeyDeleteError,
                                onBack = viewModel::navigateBack,
                                onRefresh = viewModel::refreshPasskeys,
                                onAddPasskey = { name ->
                                    viewModel.registerPasskey(this@MainActivity, name)
                                },
                                onDeletePasskey = viewModel::deletePasskey,
                                onDismissRegisterError = viewModel::clearPasskeyRegisterError,
                                onDismissDeleteError = viewModel::clearPasskeyDeleteError,
                            )
                        }
                        DetailScreen.Sessions -> {
                            SessionsScreen(
                                sessions = uiState.sessions,
                                currentSessionId = uiState.currentSessionId,
                                isLoading = uiState.isSessionsLoading,
                                error = uiState.sessionsError?.let { code ->
                                    when (code) {
                                        "sessions_load_failed" -> stringResource(R.string.error_load_sessions)
                                        "sessions_revoke_failed" -> stringResource(R.string.error_revoke_session)
                                        else -> code
                                    }
                                },
                                isRevoking = uiState.isSessionRevoking,
                                onBack = viewModel::navigateBack,
                                onRevoke = viewModel::revokeSession,
                                onRefresh = viewModel::refreshSessions,
                            )
                        }
                        DetailScreen.OwnProfile -> {
                            ProfileScreen(
                                title = stringResource(R.string.profile_title),
                                profile = uiState.profile,
                                isOwnProfile = true,
                                isLoading = uiState.isProfileLoading,
                                error = uiState.profileError?.let { code ->
                                    when (code) {
                                        "profile_load_failed" -> stringResource(R.string.error_load_profile)
                                        else -> code
                                    }
                                },
                                onBack = viewModel::navigateBack,
                            )
                        }
                        DetailScreen.FriendProfile -> {
                            ProfileScreen(
                                title = uiState.profileUsername.orEmpty(),
                                profile = uiState.profile,
                                isOwnProfile = false,
                                isLoading = uiState.isProfileLoading,
                                error = uiState.profileError?.let { code ->
                                    when (code) {
                                        "profile_load_failed" -> stringResource(R.string.error_load_profile)
                                        else -> code
                                    }
                                },
                                onBack = viewModel::navigateBack,
                            )
                        }
                        DetailScreen.Changelog -> {
                            ChangelogScreen(
                                releases = uiState.changelogReleases,
                                isLoading = uiState.isChangelogLoading,
                                error = uiState.changelogError?.let { code ->
                                    when (code) {
                                        "changelog_load_failed" -> stringResource(R.string.changelog_load_failed)
                                        else -> code
                                    }
                                },
                                onBack = viewModel::navigateBack,
                                onRefresh = viewModel::refreshChangelog,
                            )
                        }
                        DetailScreen.None -> when (uiState.screen) {
                        AppScreen.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                        AppScreen.Login -> {
                            LoginScreen(
                                isSubmitting = uiState.isSubmitting,
                                errorCode = uiState.loginError,
                                onSettings = viewModel::openSettings,
                                onHelp = viewModel::openHelp,
                                onAbout = { showAbout = true },
                                onCheckUpdates = viewModel::checkForUpdatesManual,
                                onReportBug = { BugReport.open(context) },
                                onDonate = { viewModel.openDonate(context) },
                                onLogin = viewModel::login,
                                onPasskeyLogin = { login ->
                                    viewModel.loginWithPasskey(this@MainActivity, login)
                                },
                                onRegister = { viewModel.openSignup(context) },
                            )
                        }
                        AppScreen.TwoFactor -> {
                            TwoFactorScreen(
                                isSubmitting = uiState.isSubmitting,
                                errorCode = uiState.loginError,
                                onSettings = viewModel::openSettings,
                                onHelp = viewModel::openHelp,
                                onAbout = { showAbout = true },
                                onCheckUpdates = viewModel::checkForUpdatesManual,
                                onReportBug = { BugReport.open(context) },
                                onDonate = { viewModel.openDonate(context) },
                                onVerify = viewModel::verify2FA,
                            )
                        }
                        AppScreen.Main -> {
                            val user = uiState.user
                            if (user != null) {
                                MainScreen(
                                    user = user,
                                    selectedTab = uiState.selectedMainTab,
                                    friendsSubTab = uiState.friendsSubTab,
                                    friends = uiState.friends,
                                    pendingRequests = uiState.pendingRequests,
                                    sentRequests = uiState.sentRequests,
                                    isFriendsLoading = uiState.isFriendsLoading,
                                    friendSearchResults = uiState.friendSearchResults,
                                    isFriendSearchLoading = uiState.isFriendSearchLoading,
                                    friendSearchError = uiState.friendSearchError,
                                    friendsActionMessage = uiState.friendsActionMessage,
                                    friendsActionError = uiState.friendsActionError,
                                    conversations = uiState.conversations,
                                    friendsError = uiState.friendsError,
                                    messagesError = uiState.messagesError,
                                    chatFriendId = uiState.chatFriendId,
                                    chatFriendName = uiState.chatFriendName,
                                    chatFriendIsOnline = uiState.chatFriendIsOnline,
                                    chatFriendLastSeenAt = uiState.chatFriendLastSeenAt,
                                    chatMessages = uiState.chatMessages,
                                    chatPinnedMessage = uiState.chatPinnedMessage,
                                    chatReplyTarget = uiState.chatReplyTarget,
                                    chatEditTarget = uiState.chatEditTarget,
                                    chatDraftText = uiState.chatDraftText,
                                    chatFormatToolbarHidden = uiState.chatFormatToolbarHidden,
                                    localeTag = uiState.locale,
                                    isChatLoading = uiState.isChatLoading,
                                    isSending = uiState.isSending,
                                    homeContent = uiState.homeContent,
                                    isHomeLoading = uiState.isHomeLoading,
                                    homeError = uiState.homeError?.let { code ->
                                        when (code) {
                                            "home_load_failed" -> stringResource(R.string.home_load_failed)
                                            else -> code
                                        }
                                    },
                                    onSettings = viewModel::openSettings,
                                    onHelp = viewModel::openHelp,
                                    onAbout = { showAbout = true },
                                    onCheckUpdates = viewModel::checkForUpdatesManual,
                                    onReportBug = { BugReport.open(context) },
                                    onDonate = { viewModel.openDonate(context) },
                                    onLogout = viewModel::logout,
                                    onOpenProfile = viewModel::openOwnProfile,
                                    onOpenFriendProfile = viewModel::openFriendProfile,
                                    onRefresh = viewModel::refreshSocialData,
                                    onSearchUsers = viewModel::searchUsers,
                                    onClearFriendSearch = viewModel::clearFriendSearch,
                                    onClearFriendsActionFeedback = viewModel::clearFriendsActionFeedback,
                                    onAddFriend = viewModel::addFriend,
                                    onAcceptFriendRequest = viewModel::acceptFriendRequest,
                                    onRejectFriendRequest = viewModel::rejectFriendRequest,
                                    onRemoveFriend = viewModel::removeFriend,
                                    onCancelFriendRequest = viewModel::cancelFriendRequest,
                                    onOpenChat = viewModel::openChat,
                                    onCloseChat = viewModel::closeChat,
                                    onSendMessage = viewModel::sendMessage,
                                    onClearChatReply = viewModel::clearChatReply,
                                    onClearChatEdit = viewModel::clearChatEdit,
                                    onStartChatReply = viewModel::startChatReply,
                                    onStartChatEdit = viewModel::startChatEdit,
                                    onPinChatMessage = viewModel::pinChatMessage,
                                    onUnpinChatMessage = viewModel::unpinChatMessage,
                                    onDeleteChatMessage = viewModel::deleteChatMessage,
                                    onDeleteChatMessages = viewModel::deleteChatMessages,
                                    onForwardChatMessage = viewModel::forwardChatMessage,
                                    onReactChatMessage = viewModel::reactToChatMessage,
                                    onUpdateChatDraft = viewModel::updateChatDraft,
                                    onChatFormatToolbarHiddenChange = viewModel::setChatFormatToolbarHidden,
                                    easterFlags = uiState.easterFlags,
                                    easterProgress = uiState.easterProgress,
                                    isEasterLoading = uiState.isEasterLoading,
                                    easterError = uiState.easterError?.let { code ->
                                        when (code) {
                                            "easter_load_failed" -> stringResource(R.string.error_load_easter)
                                            else -> code
                                        }
                                    },
                                    onEasterTabSelected = viewModel::refreshEasterFlags,
                                    onSelectTab = viewModel::selectMainTab,
                                    onFriendsSubTabChange = viewModel::setFriendsSubTab,
                                    onRefreshHome = viewModel::refreshHomeContent,
                                    onOpenUrl = { url -> viewModel.openWebsiteUrl(context, url) },
                                    onOpenChangelog = viewModel::openChangelog,
                                    homeWhatsNewBannerHidden = uiState.homeWhatsNewBannerHidden,
                                    onHomeWhatsNewBannerHiddenChange = viewModel::setHomeWhatsNewBannerHidden,
                                    encryptionReminderChatDismissed = uiState.encryptionReminderChatDismissed,
                                    onDismissEncryptionReminderChat = viewModel::dismissEncryptionReminderChat,
                                    onOpenSecurityBackup = viewModel::openChatBackup,
                                )
                            }
                        }
                        }
                    }
                    }

                    uiState.driveRestoreConfirmMetadata?.let { metadata ->
                        if (!uiState.driveRestorePasswordOpen) {
                            DriveRestoreConfirmDialog(
                                metadata = metadata,
                                onRestore = {
                                    driveRestorePassword = ""
                                    viewModel.beginDriveRestore()
                                },
                                onSkip = viewModel::skipDriveRestore,
                            )
                        }
                    }

                    if (uiState.driveRestorePasswordOpen) {
                        DriveRestorePasswordDialog(
                            password = driveRestorePassword,
                            onPasswordChange = { driveRestorePassword = it },
                            busy = uiState.driveRestoreBusy,
                            progress = uiState.driveRestoreProgress,
                            progressLabel = viewModel.driveRestoreProgressLabel(uiState.driveRestoreProgressKey),
                            onDismiss = viewModel::dismissDriveRestorePasswordDialog,
                            onConfirm = {
                                viewModel.submitDriveRestorePassword(driveRestorePassword)
                            },
                        )
                    }

                    if (showAbout) {
                        AboutDialog(onDismiss = { showAbout = false })
                    }

                    if (uiState.showLightCatcherGame) {
                        LightCatcherGameDialog(
                            isUnlocking = uiState.lightCatcherUnlocking,
                            onDismiss = viewModel::dismissLightCatcherGame,
                            onWin = viewModel::onLightCatcherGameWon,
                        )
                    }

                    UpdateCheckDialog(
                        state = uiState.manualUpdateCheck,
                        onDismiss = viewModel::dismissManualUpdateCheck,
                    )

                    UpdateDialog(
                        state = uiState.update,
                        onDownload = viewModel::downloadUpdate,
                        onInstall = ::installUpdate,
                        onDismiss = viewModel::dismissUpdate,
                    )

                    if (isAppLocked && uiState.screen == AppScreen.Main && uiState.appLockEnabled && uiState.appLockSettingsLoaded) {
                        AppLockScreen(
                            sessionId = appLockSession,
                            biometricAvailable = biometricAvailable,
                            biometricEnabled = uiState.appLockBiometric,
                            errorMessage = appLockError,
                            onUnlockPin = { pin ->
                                coroutineScope.launch {
                                    val ok = viewModel.verifyAppLockPin(pin)
                                    if (ok) {
                                        isAppLocked = false
                                        appLockError = null
                                        viewModel.onAppUnlocked()
                                    } else {
                                        appLockError = context.getString(R.string.app_lock_wrong_pin)
                                    }
                                }
                            },
                            onUnlockBiometric = { onSuccess, onFailed, onError ->
                                BiometricHelper.showUnlockPrompt(
                                    activity = this@MainActivity,
                                    onSuccess = {
                                        isAppLocked = false
                                        appLockError = null
                                        viewModel.onAppUnlocked()
                                        viewModel.onBiometricUnlockSuccess()
                                        onSuccess()
                                    },
                                    onFailed = onFailed,
                                    onError = onError,
                                )
                            },
                        )
                    }
                    }
                }
                }
            }
            }
        }
    }

    private fun installUpdate() {
        val apkFile = viewModel.downloadedApkFile()
        if (apkFile == null) {
            viewModel.downloadUpdate()
            return
        }

        if (!ApkInstaller.canInstall(this)) {
            Toast.makeText(
                this,
                getString(R.string.update_error_permission),
                Toast.LENGTH_LONG,
            ).show()
            ApkInstaller.openInstallPermissionSettings(this)
            return
        }

        ApkInstaller.install(this, apkFile)
    }

    private fun dispatchNotificationIntent(intent: Intent?) {
        val data = intent ?: return
        if (data.getBooleanExtra(NotificationHelper.EXTRA_OPEN_SESSIONS, false)) {
            data.removeExtra(NotificationHelper.EXTRA_OPEN_SESSIONS)
            viewModel.openSessionsFromNotification()
            return
        }
        val friendId = data.getStringExtra(NotificationHelper.EXTRA_CHAT_FRIEND_ID)?.trim().orEmpty()
        if (friendId.isEmpty()) return
        val friendName = data.getStringExtra(NotificationHelper.EXTRA_CHAT_FRIEND_NAME)?.trim().orEmpty()
        data.removeExtra(NotificationHelper.EXTRA_CHAT_FRIEND_ID)
        data.removeExtra(NotificationHelper.EXTRA_CHAT_FRIEND_NAME)
        viewModel.openChatFromNotification(friendId, friendName)
    }
}
