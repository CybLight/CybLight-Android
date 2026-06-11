package org.cyblight.android.ui

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.cyblight.android.data.ApiClient
import org.cyblight.android.data.api.FriendDto
import org.cyblight.android.data.api.MessageDto
import org.cyblight.android.data.api.UserDto
import org.cyblight.android.data.preferences.AppPreferences
import org.cyblight.android.data.preferences.ThemeMode
import org.cyblight.android.data.repository.AuthRepository
import org.cyblight.android.data.repository.AuthResult
import org.cyblight.android.data.repository.ConversationPreview
import org.cyblight.android.data.repository.FriendsRepository
import org.cyblight.android.data.repository.MessagesRepository
import org.cyblight.android.data.session.SessionManager
import org.cyblight.android.i18n.LocaleManager
import org.cyblight.android.update.AppUpdateInfo
import org.cyblight.android.update.ManualUpdateCheckState
import org.cyblight.android.update.UpdatePreferences
import org.cyblight.android.update.UpdateRepository
import org.cyblight.android.update.UpdateStatus
import org.cyblight.android.update.UpdateUiState
import java.io.File

enum class AppScreen {
    Loading,
    Login,
    TwoFactor,
    Main,
}

data class AppUiState(
    val screen: AppScreen = AppScreen.Loading,
    val user: UserDto? = null,
    val locale: String = "ru",
    val loginError: String? = null,
    val isSubmitting: Boolean = false,
    val pending2FAUserId: String? = null,
    val friends: List<FriendDto> = emptyList(),
    val conversations: List<ConversationPreview> = emptyList(),
    val friendsError: String? = null,
    val messagesError: String? = null,
    val chatMessages: List<MessageDto> = emptyList(),
    val chatFriendId: String? = null,
    val chatFriendName: String? = null,
    val isChatLoading: Boolean = false,
    val isSending: Boolean = false,
    val update: UpdateUiState = UpdateUiState(),
    val manualUpdateCheck: ManualUpdateCheckState = ManualUpdateCheckState(),
    val showSettings: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val notificationsEnabled: Boolean = true,
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val sessionManager = SessionManager(application)
    private val appPreferences = AppPreferences(application)
    private val api = ApiClient.create(sessionManager)
    private val authRepository = AuthRepository(api, sessionManager)
    private val friendsRepository = FriendsRepository(api)
    private val messagesRepository = MessagesRepository(api)
    private val updateRepository = UpdateRepository(application)
    private val updatePreferences = UpdatePreferences(application)
    private var pendingUpdate: AppUpdateInfo? = null

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    val locale: StateFlow<String> = sessionManager.savedLocale.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        "ru",
    )

    init {
        viewModelScope.launch {
            val savedLocale = sessionManager.getLocale()
            val savedTheme = appPreferences.getThemeMode()
            val savedNotifications = appPreferences.getNotificationsEnabled()
            LocaleManager.apply(savedLocale)
            _uiState.value = _uiState.value.copy(
                locale = savedLocale,
                themeMode = savedTheme,
                notificationsEnabled = savedNotifications,
            )

            val user = authRepository.restoreSession()
            _uiState.value = if (user != null) {
                _uiState.value.copy(screen = AppScreen.Main, user = user)
            } else {
                _uiState.value.copy(screen = AppScreen.Login)
            }

            if (user != null) {
                refreshSocialData()
            }

            checkForUpdate()
        }
    }

    fun checkForUpdate() {
        viewModelScope.launch {
            updateRepository.checkForUpdate()
                .onSuccess { info ->
                    if (info == null) return@onSuccess

                    val dismissed = updatePreferences.getDismissedVersion()
                    if (dismissed == info.versionName) return@onSuccess

                    showUpdateDialog(info)
                }
        }
    }

    fun checkForUpdatesManual() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                manualUpdateCheck = ManualUpdateCheckState(visible = true, checking = true),
            )

            updateRepository.checkForUpdate()
                .onSuccess { info ->
                    if (info == null) {
                        _uiState.value = _uiState.value.copy(
                            manualUpdateCheck = ManualUpdateCheckState(visible = true, upToDate = true),
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            manualUpdateCheck = ManualUpdateCheckState(visible = false),
                        )
                        showUpdateDialog(info)
                    }
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        manualUpdateCheck = ManualUpdateCheckState(
                            visible = true,
                            errorMessage = error.message,
                        ),
                    )
                }
        }
    }

    fun dismissManualUpdateCheck() {
        _uiState.value = _uiState.value.copy(
            manualUpdateCheck = ManualUpdateCheckState(visible = false),
        )
    }

    private fun showUpdateDialog(info: AppUpdateInfo) {
        pendingUpdate = info
        val alreadyDownloaded = updateRepository.hasDownloadedApk(info.versionName)

        _uiState.value = _uiState.value.copy(
            update = UpdateUiState(
                visible = true,
                versionName = info.versionName,
                releaseNotes = info.releaseNotes,
                downloadUrl = info.downloadUrl,
                status = if (alreadyDownloaded) {
                    UpdateStatus.ReadyToInstall
                } else {
                    UpdateStatus.Available
                },
            ),
        )
    }

    fun downloadUpdate() {
        val info = pendingUpdate ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                update = _uiState.value.update.copy(
                    status = UpdateStatus.Downloading,
                    progress = 0f,
                    errorMessage = null,
                ),
            )

            updateRepository.downloadApk(info) { progress ->
                _uiState.value = _uiState.value.copy(
                    update = _uiState.value.update.copy(progress = progress),
                )
            }.onSuccess {
                _uiState.value = _uiState.value.copy(
                    update = _uiState.value.update.copy(
                        status = UpdateStatus.ReadyToInstall,
                        progress = 1f,
                        errorMessage = null,
                    ),
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    update = _uiState.value.update.copy(
                        status = UpdateStatus.Error,
                        errorMessage = error.message,
                    ),
                )
            }
        }
    }

    fun dismissUpdate() {
        viewModelScope.launch {
            val version = _uiState.value.update.versionName
            if (version.isNotBlank()) {
                updatePreferences.dismissVersion(version)
            }
            _uiState.value = _uiState.value.copy(
                update = _uiState.value.update.copy(visible = false),
            )
        }
    }

    fun downloadedApkFile(): File? {
        val version = _uiState.value.update.versionName
        if (version.isBlank()) return null
        val file = updateRepository.apkFileForVersion(version)
        return file.takeIf { it.exists() && it.length() > 0 }
    }

    fun setLocale(locale: String) {
        viewModelScope.launch {
            sessionManager.saveLocale(locale)
            LocaleManager.apply(locale)
            _uiState.value = _uiState.value.copy(locale = locale)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            appPreferences.setThemeMode(mode)
            _uiState.value = _uiState.value.copy(themeMode = mode)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setNotificationsEnabled(enabled)
            _uiState.value = _uiState.value.copy(notificationsEnabled = enabled)
        }
    }

    fun openSettings() {
        _uiState.value = _uiState.value.copy(showSettings = true)
    }

    fun closeSettings() {
        _uiState.value = _uiState.value.copy(showSettings = false)
    }

    fun loginWithPasskey(activity: Activity, login: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, loginError = null)
            when (val result = authRepository.loginWithPasskey(activity, login)) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        screen = AppScreen.Main,
                        user = result.user,
                        isSubmitting = false,
                        pending2FAUserId = null,
                    )
                    refreshSocialData()
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        loginError = result.code,
                    )
                }
                else -> Unit
            }
        }
    }

    fun login(login: String, password: String, turnstileToken: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, loginError = null)
            when (val result = authRepository.login(login, password, turnstileToken)) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        screen = AppScreen.Main,
                        user = result.user,
                        isSubmitting = false,
                        pending2FAUserId = null,
                    )
                    refreshSocialData()
                }
                is AuthResult.Requires2FA -> {
                    _uiState.value = _uiState.value.copy(
                        screen = AppScreen.TwoFactor,
                        isSubmitting = false,
                        pending2FAUserId = result.userId,
                    )
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        loginError = result.code,
                    )
                }
            }
        }
    }

    fun verify2FA(code: String, rememberDevice: Boolean) {
        val userId = _uiState.value.pending2FAUserId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, loginError = null)
            when (val result = authRepository.verify2FA(userId, code, rememberDevice)) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        screen = AppScreen.Main,
                        user = result.user,
                        isSubmitting = false,
                        pending2FAUserId = null,
                    )
                    refreshSocialData()
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        loginError = result.code,
                    )
                }
                else -> Unit
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.value = AppUiState(
                screen = AppScreen.Login,
                locale = _uiState.value.locale,
                themeMode = _uiState.value.themeMode,
                notificationsEnabled = _uiState.value.notificationsEnabled,
            )
        }
    }

    fun refreshSocialData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(friendsError = null, messagesError = null)

            friendsRepository.loadFriends()
                .onSuccess { friends ->
                    _uiState.value = _uiState.value.copy(friends = friends)
                    messagesRepository.loadConversations(friends)
                        .onSuccess { conversations ->
                            _uiState.value = _uiState.value.copy(conversations = conversations)
                        }
                        .onFailure {
                            _uiState.value = _uiState.value.copy(messagesError = "unread_load_failed")
                        }
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(friendsError = "friends_load_failed")
                }
        }
    }

    fun openChat(friendId: String, friendName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                chatFriendId = friendId,
                chatFriendName = friendName,
                isChatLoading = true,
                messagesError = null,
                chatMessages = emptyList(),
            )

            messagesRepository.loadMessages(friendId)
                .onSuccess { messages ->
                    if (_uiState.value.chatFriendId != friendId) return@onSuccess
                    _uiState.value = _uiState.value.copy(
                        chatMessages = messages,
                        isChatLoading = false,
                    )
                    refreshSocialData()
                }
                .onFailure {
                    if (_uiState.value.chatFriendId != friendId) return@onFailure
                    _uiState.value = _uiState.value.copy(
                        isChatLoading = false,
                        messagesError = "messages_load_failed",
                    )
                }
        }
    }

    fun closeChat() {
        _uiState.value = _uiState.value.copy(
            chatFriendId = null,
            chatFriendName = null,
            chatMessages = emptyList(),
            messagesError = null,
        )
        refreshSocialData()
    }

    fun sendMessage(content: String) {
        val friendId = _uiState.value.chatFriendId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSending = true, messagesError = null)
            messagesRepository.sendMessage(friendId, content)
                .onSuccess {
                    messagesRepository.loadMessages(friendId)
                        .onSuccess { messages ->
                            _uiState.value = _uiState.value.copy(
                                chatMessages = messages,
                                isSending = false,
                            )
                        }
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isSending = false,
                        messagesError = "send_failed",
                    )
                }
        }
    }
}
