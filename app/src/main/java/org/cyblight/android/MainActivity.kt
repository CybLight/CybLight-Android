package org.cyblight.android

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.autofill.AutofillManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import org.cyblight.android.ui.AppViewModel
import org.cyblight.android.ui.login.LoginScreen
import org.cyblight.android.ui.login.TwoFactorScreen
import org.cyblight.android.ui.main.MainScreen
import org.cyblight.android.ui.components.AboutDialog
import org.cyblight.android.ui.help.HelpScreen
import org.cyblight.android.ui.settings.SettingsScreen
import org.cyblight.android.ui.theme.CybLightTheme
import org.cyblight.android.ui.update.UpdateCheckDialog
import org.cyblight.android.ui.update.UpdateDialog
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.launch
import org.cyblight.android.security.BiometricHelper
import org.cyblight.android.ui.applock.AppLockScreen
import org.cyblight.android.util.BugReport

class MainActivity : AppCompatActivity() {
    private val viewModel: AppViewModel by viewModels()

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
            var isAppLocked by rememberSaveable { mutableStateOf(false) }
            var isColdStart by rememberSaveable { mutableStateOf(true) }
            var appLockError by remember { mutableStateOf<String?>(null) }
            val lifecycleOwner = LocalLifecycleOwner.current
            val coroutineScope = rememberCoroutineScope()
            val biometricAvailable = remember { BiometricHelper.isAvailable(this@MainActivity) }

            DisposableEffect(lifecycleOwner, uiState.appLockEnabled, uiState.screen) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_STOP -> {
                            if (uiState.screen == AppScreen.Main && uiState.appLockEnabled) {
                                viewModel.onAppBackgrounded()
                            }
                        }
                        Lifecycle.Event.ON_START -> {
                            if (uiState.screen == AppScreen.Main &&
                                uiState.appLockEnabled &&
                                viewModel.shouldShowAppLock(isColdStart = false)
                            ) {
                                isAppLocked = true
                            }
                        }
                        else -> Unit
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            LaunchedEffect(uiState.screen, uiState.appLockEnabled) {
                if (uiState.screen == AppScreen.Main && uiState.appLockEnabled) {
                    if (viewModel.shouldShowAppLock(isColdStart = isColdStart)) {
                        isAppLocked = true
                    }
                    isColdStart = false
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
                    when (uiState.detailScreen) {
                        DetailScreen.Settings -> {
                            SettingsScreen(
                                locale = uiState.locale,
                                themeMode = uiState.themeMode,
                                notificationsEnabled = uiState.notificationsEnabled,
                                loginAlertsEnabled = uiState.loginAlertsEnabled,
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
                                    isChatLoading = uiState.isChatLoading,
                                    isSending = uiState.isSending,
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
                                    easterFlags = uiState.easterFlags,
                                    isEasterLoading = uiState.isEasterLoading,
                                    easterError = uiState.easterError?.let { code ->
                                        when (code) {
                                            "easter_load_failed" -> stringResource(R.string.error_load_easter)
                                            else -> code
                                        }
                                    },
                                    securityOverview = uiState.securityOverview,
                                    isSecurityLoading = uiState.isSecurityLoading,
                                    isSecurityRefreshing = uiState.isSecurityLoading &&
                                        uiState.securityOverview != null,
                                    onSecurityTabSelected = viewModel::refreshSecurityOverview,
                                    onRefreshSecurity = viewModel::forceRefreshSecurityOverview,
                                    onOpenSecurityCheck = viewModel::openSecurityCheck,
                                    onOpenAccountSecurity = { viewModel.openAccountSecurity(context) },
                                    onOpenPasskeys = viewModel::openPasskeys,
                                    onOpenTrustedDevices = viewModel::openTrustedDevices,
                                    onOpenLoginHistory = viewModel::openLoginHistory,
                                    onOpenSessions = viewModel::openSessions,
                                    onEasterTabSelected = viewModel::refreshEasterFlags,
                                )
                            }
                        }
                        }
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

                    if (isAppLocked && uiState.screen == AppScreen.Main && uiState.appLockEnabled) {
                        AppLockScreen(
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
                            onUnlockBiometric = {
                                BiometricHelper.showUnlockPrompt(
                                    activity = this@MainActivity,
                                    onSuccess = {
                                        isAppLocked = false
                                        appLockError = null
                                        viewModel.onAppUnlocked()
                                    },
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
}
