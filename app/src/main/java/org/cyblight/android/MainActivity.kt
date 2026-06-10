package org.cyblight.android

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.cyblight.android.update.ApkInstaller
import org.cyblight.android.ui.AppScreen
import org.cyblight.android.ui.AppViewModel
import org.cyblight.android.ui.login.LoginScreen
import org.cyblight.android.ui.login.TwoFactorScreen
import org.cyblight.android.ui.main.MainScreen
import org.cyblight.android.ui.theme.CybLightTheme
import org.cyblight.android.ui.update.UpdateDialog

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsState()

            CybLightTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    when (uiState.screen) {
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
                                locale = uiState.locale,
                                isSubmitting = uiState.isSubmitting,
                                errorCode = uiState.loginError,
                                onLocaleSelected = viewModel::setLocale,
                                onLogin = viewModel::login,
                            )
                        }
                        AppScreen.TwoFactor -> {
                            TwoFactorScreen(
                                locale = uiState.locale,
                                isSubmitting = uiState.isSubmitting,
                                errorCode = uiState.loginError,
                                onLocaleSelected = viewModel::setLocale,
                                onVerify = viewModel::verify2FA,
                            )
                        }
                        AppScreen.Main -> {
                            val user = uiState.user
                            if (user != null) {
                                MainScreen(
                                    user = user,
                                    locale = uiState.locale,
                                    friends = uiState.friends,
                                    conversations = uiState.conversations,
                                    friendsError = uiState.friendsError,
                                    messagesError = uiState.messagesError,
                                    chatFriendId = uiState.chatFriendId,
                                    chatFriendName = uiState.chatFriendName,
                                    chatMessages = uiState.chatMessages,
                                    isChatLoading = uiState.isChatLoading,
                                    isSending = uiState.isSending,
                                    onLocaleSelected = viewModel::setLocale,
                                    onLogout = viewModel::logout,
                                    onRefresh = viewModel::refreshSocialData,
                                    onOpenChat = viewModel::openChat,
                                    onCloseChat = viewModel::closeChat,
                                    onSendMessage = viewModel::sendMessage,
                                )
                            }
                        }
                    }

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
