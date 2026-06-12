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
import android.content.Context
import org.cyblight.android.data.api.EasterFlagsDto
import org.cyblight.android.data.api.FriendDto
import org.cyblight.android.data.api.MessageDto
import org.cyblight.android.data.api.ProfileDto
import org.cyblight.android.data.api.SessionDto
import org.cyblight.android.data.api.UserDto
import org.cyblight.android.data.preferences.AppPreferences
import org.cyblight.android.data.preferences.ThemeMode
import org.cyblight.android.data.repository.AuthRepository
import org.cyblight.android.data.repository.AuthResult
import org.cyblight.android.data.repository.SessionRefreshResult
import org.cyblight.android.data.repository.ConversationPreview
import org.cyblight.android.data.repository.FriendsRepository
import org.cyblight.android.data.repository.MessagesRepository
import org.cyblight.android.data.repository.ProfileRepository
import org.cyblight.android.data.repository.SessionsRepository
import org.cyblight.android.data.session.SessionManager
import org.cyblight.android.util.ExternalLinks
import org.cyblight.android.i18n.LocaleManager
import org.cyblight.android.update.AppUpdateInfo
import org.cyblight.android.update.ManualUpdateCheckState
import org.cyblight.android.update.UpdatePreferences
import org.cyblight.android.update.UpdateRepository
import org.cyblight.android.update.UpdateStatus
import org.cyblight.android.update.UpdateUiState
import org.cyblight.android.util.SystemSettings
import java.io.File

enum class AppScreen {
    Loading,
    Login,
    TwoFactor,
    Main,
}

enum class DetailScreen {
    None,
    Settings,
    Help,
    Security,
    Sessions,
    EasterEggs,
    OwnProfile,
    FriendProfile,
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
    val detailScreen: DetailScreen = DetailScreen.None,
    val helpReturnToSettings: Boolean = false,
    val profileUsername: String? = null,
    val profile: ProfileDto? = null,
    val isProfileLoading: Boolean = false,
    val profileError: String? = null,
    val sessions: List<SessionDto> = emptyList(),
    val currentSessionId: String? = null,
    val isSessionsLoading: Boolean = false,
    val sessionsError: String? = null,
    val isSessionRevoking: Boolean = false,
    val easterFlags: EasterFlagsDto? = null,
    val isEasterLoading: Boolean = false,
    val easterError: String? = null,
    val showLightCatcherGame: Boolean = false,
    val lightCatcherUnlocking: Boolean = false,
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
    private val profileRepository = ProfileRepository(api)
    private val sessionsRepository = SessionsRepository(api)
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
            sessionManager.sessionExpired.collect {
                forceLogout()
            }
        }

        viewModelScope.launch {
            try {
                val savedLocale = sessionManager.getLocale()
                val savedTheme = appPreferences.getThemeMode()
                val notifications = resolveNotificationsEnabled()
                LocaleManager.apply(savedLocale)
                _uiState.value = _uiState.value.copy(
                    locale = savedLocale,
                    themeMode = savedTheme,
                    notificationsEnabled = notifications,
                )

                val user = authRepository.restoreSession()
                _uiState.value = if (user != null) {
                    _uiState.value.copy(screen = AppScreen.Main, user = user)
                } else {
                    _uiState.value.copy(screen = AppScreen.Login)
                }

                if (user != null) {
                    when (authRepository.refreshSession()) {
                        SessionRefreshResult.Expired -> {
                            forceLogout()
                            return@launch
                        }
                        else -> refreshSocialData()
                    }
                }

                checkForUpdate()
            } catch (_: Exception) {
                forceLogout()
            }
        }
    }

    private fun forceLogout() {
        _uiState.value = AppUiState(
            screen = AppScreen.Login,
            locale = _uiState.value.locale,
            themeMode = _uiState.value.themeMode,
            notificationsEnabled = _uiState.value.notificationsEnabled,
        )
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
            val resolved = enabled && SystemSettings.areNotificationsEnabled(getApplication())
            appPreferences.setNotificationsEnabled(resolved)
            _uiState.value = _uiState.value.copy(notificationsEnabled = resolved)
        }
    }

    private suspend fun resolveNotificationsEnabled(): Boolean {
        val systemEnabled = SystemSettings.areNotificationsEnabled(getApplication())
        val saved = appPreferences.getNotificationsEnabled()
        val resolved = saved && systemEnabled
        if (saved != resolved) {
            appPreferences.setNotificationsEnabled(resolved)
        }
        return resolved
    }

    fun openSettings() {
        _uiState.value = _uiState.value.copy(detailScreen = DetailScreen.Settings)
    }

    fun openHelp() {
        val returnToSettings = _uiState.value.detailScreen == DetailScreen.Settings
        _uiState.value = _uiState.value.copy(
            detailScreen = DetailScreen.Help,
            helpReturnToSettings = returnToSettings,
        )
    }

    fun openSecurity() {
        _uiState.value = _uiState.value.copy(detailScreen = DetailScreen.Security)
    }

    fun openSessions() {
        _uiState.value = _uiState.value.copy(
            detailScreen = DetailScreen.Sessions,
            sessionsError = null,
            isSessionsLoading = true,
        )
        loadSessions()
    }

    fun openEasterEggs() {
        _uiState.value = _uiState.value.copy(
            detailScreen = DetailScreen.EasterEggs,
            easterError = null,
            isEasterLoading = true,
        )
        loadEasterFlags()
    }

    fun openOwnProfile() {
        _uiState.value = _uiState.value.copy(
            detailScreen = DetailScreen.OwnProfile,
            profileUsername = null,
            profile = null,
            profileError = null,
            isProfileLoading = true,
        )
        loadOwnProfile()
    }

    fun openFriendProfile(username: String) {
        _uiState.value = _uiState.value.copy(
            detailScreen = DetailScreen.FriendProfile,
            profileUsername = username,
            profile = null,
            profileError = null,
            isProfileLoading = true,
        )
        loadFriendProfile(username)
    }

    fun navigateBack() {
        when (_uiState.value.detailScreen) {
            DetailScreen.Sessions -> _uiState.value = _uiState.value.copy(detailScreen = DetailScreen.Security)
            DetailScreen.Help -> {
                _uiState.value = if (_uiState.value.helpReturnToSettings) {
                    _uiState.value.copy(
                        detailScreen = DetailScreen.Settings,
                        helpReturnToSettings = false,
                    )
                } else {
                    _uiState.value.copy(
                        detailScreen = DetailScreen.None,
                        helpReturnToSettings = false,
                    )
                }
            }
            DetailScreen.Security, DetailScreen.EasterEggs -> {
                _uiState.value = _uiState.value.copy(detailScreen = DetailScreen.Settings)
            }
            else -> {
                _uiState.value = _uiState.value.copy(
                    detailScreen = DetailScreen.None,
                    profileUsername = null,
                    profile = null,
                    profileError = null,
                    sessionsError = null,
                    easterError = null,
                )
            }
        }
    }

    fun openDonate(context: Context) {
        val locale = _uiState.value.locale
        ExternalLinks.openUrl(context, "https://cyblight.org/$locale/donate/")
    }

    fun refreshSessions() {
        loadSessions()
    }

    fun revokeSession(sessionId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSessionRevoking = true, sessionsError = null)
            sessionsRepository.revokeSession(sessionId)
                .onSuccess { loggedOut ->
                    if (loggedOut) {
                        forceLogout()
                    } else {
                        loadSessions()
                        _uiState.value = _uiState.value.copy(isSessionRevoking = false)
                    }
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isSessionRevoking = false,
                        sessionsError = "sessions_revoke_failed",
                    )
                }
        }
    }

    private fun loadOwnProfile() {
        viewModelScope.launch {
            profileRepository.loadOwnProfile()
                .onSuccess { profile ->
                    _uiState.value = _uiState.value.copy(
                        profile = profile,
                        isProfileLoading = false,
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isProfileLoading = false,
                        profileError = "profile_load_failed",
                    )
                }
        }
    }

    private fun loadFriendProfile(username: String) {
        viewModelScope.launch {
            profileRepository.loadProfile(username)
                .onSuccess { profile ->
                    if (_uiState.value.profileUsername != username) return@onSuccess
                    _uiState.value = _uiState.value.copy(
                        profile = profile,
                        isProfileLoading = false,
                    )
                }
                .onFailure {
                    if (_uiState.value.profileUsername != username) return@onFailure
                    _uiState.value = _uiState.value.copy(
                        isProfileLoading = false,
                        profileError = "profile_load_failed",
                    )
                }
        }
    }

    private fun loadSessions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSessionsLoading = true, sessionsError = null)
            sessionsRepository.loadSessions()
                .onSuccess { snapshot ->
                    _uiState.value = _uiState.value.copy(
                        sessions = snapshot.sessions,
                        currentSessionId = snapshot.currentSessionId,
                        isSessionsLoading = false,
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isSessionsLoading = false,
                        sessionsError = "sessions_load_failed",
                    )
                }
        }
    }

    private fun loadEasterFlags() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isEasterLoading = true, easterError = null)
            profileRepository.loadEasterFlags()
                .onSuccess { flags ->
                    _uiState.value = _uiState.value.copy(
                        easterFlags = flags,
                        isEasterLoading = false,
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isEasterLoading = false,
                        easterError = "easter_load_failed",
                    )
                }
        }
    }

    fun openLightCatcherGame() {
        _uiState.value = _uiState.value.copy(showLightCatcherGame = true)
    }

    fun dismissLightCatcherGame() {
        _uiState.value = _uiState.value.copy(showLightCatcherGame = false)
    }

    fun onLightCatcherGameWon() {
        val flags = _uiState.value.easterFlags
        if (flags?.lightCatcher == true) {
            dismissLightCatcherGame()
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(lightCatcherUnlocking = true)
            profileRepository.unlockLightCatcher()
                .onSuccess {
                    val current = _uiState.value.easterFlags
                    _uiState.value = _uiState.value.copy(
                        showLightCatcherGame = false,
                        lightCatcherUnlocking = false,
                        easterFlags = current?.copy(lightCatcher = true)
                            ?: EasterFlagsDto(lightCatcher = true),
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(lightCatcherUnlocking = false)
                }
        }
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

    fun refreshSessionOnResume() {
        if (_uiState.value.screen != AppScreen.Main || _uiState.value.user == null) return
        viewModelScope.launch {
            when (authRepository.refreshSession()) {
                SessionRefreshResult.Valid -> refreshSocialData()
                SessionRefreshResult.Expired -> forceLogout()
                SessionRefreshResult.Offline -> Unit
            }
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
