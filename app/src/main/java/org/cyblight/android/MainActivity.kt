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
import org.cyblight.android.ui.easter.EasterEggsScreen
import org.cyblight.android.ui.easter.LightCatcherGameDialog
import org.cyblight.android.ui.profile.ProfileScreen
import org.cyblight.android.ui.security.SecurityScreen
import org.cyblight.android.ui.security.SessionsScreen
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
import org.cyblight.android.util.BugReport
import org.cyblight.android.util.SystemSettings

class MainActivity : AppCompatActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onResume() {
        super.onResume()
        viewModel.refreshSessionOnResume()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val context = LocalContext.current
            var pendingAutofillCommit by remember { mutableStateOf(false) }
            var showAbout by remember { mutableStateOf(false) }

            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
            ) { granted ->
                viewModel.setNotificationsEnabled(granted)
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
                                onBack = viewModel::navigateBack,
                                onLocaleSelected = viewModel::setLocale,
                                onThemeModeSelected = viewModel::setThemeMode,
                                onNotificationsEnabledChange = viewModel::setNotificationsEnabled,
                                onRequestNotificationPermission = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(
                                            Manifest.permission.POST_NOTIFICATIONS,
                                        )
                                    }
                                },
                                onHelp = viewModel::openHelp,
                                onSecurity = viewModel::openSecurity,
                                onEasterEggs = viewModel::openEasterEggs,
                                onOpenLightCatcherGame = viewModel::openLightCatcherGame,
                            )
                        }
                        DetailScreen.Help -> {
                            HelpScreen(onBack = viewModel::navigateBack)
                        }
                        DetailScreen.Security -> {
                            SecurityScreen(
                                onBack = viewModel::navigateBack,
                                onOpenSessions = viewModel::openSessions,
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
                        DetailScreen.EasterEggs -> {
                            EasterEggsScreen(
                                flags = uiState.easterFlags,
                                isLoading = uiState.isEasterLoading,
                                error = uiState.easterError?.let { code ->
                                    when (code) {
                                        "easter_load_failed" -> stringResource(R.string.error_load_easter)
                                        else -> code
                                    }
                                },
                                onBack = viewModel::navigateBack,
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
                                    conversations = uiState.conversations,
                                    friendsError = uiState.friendsError,
                                    messagesError = uiState.messagesError,
                                    chatFriendId = uiState.chatFriendId,
                                    chatFriendName = uiState.chatFriendName,
                                    chatMessages = uiState.chatMessages,
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
                                    onOpenChat = viewModel::openChat,
                                    onCloseChat = viewModel::closeChat,
                                    onSendMessage = viewModel::sendMessage,
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
