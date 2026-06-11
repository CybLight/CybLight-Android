package org.cyblight.android

import android.os.Bundle
import android.view.autofill.AutofillManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import org.cyblight.android.ui.AppScreen
import org.cyblight.android.update.ApkInstaller
import org.cyblight.android.i18n.AppLocaleProvider
import org.cyblight.android.ui.AppViewModel
import org.cyblight.android.ui.login.LoginScreen
import org.cyblight.android.ui.login.TwoFactorScreen
import org.cyblight.android.ui.main.MainScreen
import org.cyblight.android.ui.components.AboutDialog
import org.cyblight.android.ui.settings.SettingsScreen
import org.cyblight.android.ui.theme.CybLightTheme
import org.cyblight.android.ui.update.UpdateCheckDialog
import org.cyblight.android.ui.update.UpdateDialog
import org.cyblight.android.util.BugReport

class MainActivity : AppCompatActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val context = LocalContext.current
            var pendingAutofillCommit by remember { mutableStateOf(false) }
            var showAbout by remember { mutableStateOf(false) }

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
                    when {
                        uiState.showSettings -> {
                            SettingsScreen(
                                locale = uiState.locale,
                                themeMode = uiState.themeMode,
                                notificationsEnabled = uiState.notificationsEnabled,
                                onBack = viewModel::closeSettings,
                                onLocaleSelected = viewModel::setLocale,
                                onThemeModeSelected = viewModel::setThemeMode,
                                onNotificationsEnabledChange = viewModel::setNotificationsEnabled,
                                onAbout = { showAbout = true },
                            )
                        }
                        uiState.screen == AppScreen.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                        uiState.screen == AppScreen.Login -> {
                            LoginScreen(
                                isSubmitting = uiState.isSubmitting,
                                errorCode = uiState.loginError,
                                onSettings = viewModel::openSettings,
                                onAbout = { showAbout = true },
                                onCheckUpdates = viewModel::checkForUpdatesManual,
                                onReportBug = { BugReport.open(context) },
                                onLogin = viewModel::login,
                                onPasskeyLogin = { login ->
                                    viewModel.loginWithPasskey(this@MainActivity, login)
                                },
                            )
                        }
                        uiState.screen == AppScreen.TwoFactor -> {
                            TwoFactorScreen(
                                isSubmitting = uiState.isSubmitting,
                                errorCode = uiState.loginError,
                                onSettings = viewModel::openSettings,
                                onAbout = { showAbout = true },
                                onCheckUpdates = viewModel::checkForUpdatesManual,
                                onReportBug = { BugReport.open(context) },
                                onVerify = viewModel::verify2FA,
                            )
                        }
                        uiState.screen == AppScreen.Main -> {
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
                                    onAbout = { showAbout = true },
                                    onCheckUpdates = viewModel::checkForUpdatesManual,
                                    onReportBug = { BugReport.open(context) },
                                    onLogout = viewModel::logout,
                                    onRefresh = viewModel::refreshSocialData,
                                    onOpenChat = viewModel::openChat,
                                    onCloseChat = viewModel::closeChat,
                                    onSendMessage = viewModel::sendMessage,
                                )
                            }
                        }
                    }

                    if (showAbout) {
                        AboutDialog(onDismiss = { showAbout = false })
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
