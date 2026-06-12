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
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.cyblight.android.auth.PasskeyAuthException
import org.cyblight.android.data.ApiClient
import android.content.Context
import org.cyblight.android.data.api.EasterFlagsDto
import org.cyblight.android.data.api.FriendDto
import org.cyblight.android.data.api.LoginHistoryEntryDto
import org.cyblight.android.data.api.MessageDto
import org.cyblight.android.data.api.MessageReactionDto
import org.cyblight.android.data.api.PinnedMessageDto
import org.cyblight.android.data.api.PasskeyDto
import org.cyblight.android.data.api.ProfileDto
import org.cyblight.android.data.api.SessionDto
import org.cyblight.android.data.api.TrustedDeviceDto
import org.cyblight.android.data.api.UserDto
import org.cyblight.android.data.preferences.AppPreferences
import org.cyblight.android.data.preferences.AppLockTimeout
import org.cyblight.android.data.preferences.ThemeMode
import org.cyblight.android.data.repository.AuthRepository
import org.cyblight.android.data.repository.AuthResult
import org.cyblight.android.data.repository.SessionRefreshResult
import org.cyblight.android.data.repository.ConversationPreview
import org.cyblight.android.data.repository.FriendsRepository
import org.cyblight.android.data.repository.MessagesRepository
import org.cyblight.android.data.repository.ProfileRepository
import org.cyblight.android.data.repository.SecurityOverview
import org.cyblight.android.data.repository.SecurityRepository
import org.cyblight.android.data.repository.SessionsRepository
import org.cyblight.android.data.session.SessionManager
import org.cyblight.android.util.EasterLogger
import org.cyblight.android.util.ExternalLinks
import org.cyblight.android.i18n.LocaleManager
import org.cyblight.android.update.AppUpdateInfo
import org.cyblight.android.update.ManualUpdateCheckState
import org.cyblight.android.update.UpdatePreferences
import org.cyblight.android.update.UpdateRepository
import org.cyblight.android.update.UpdateStatus
import org.cyblight.android.update.UpdateUiState
import org.cyblight.android.notifications.LoginNotificationMonitor
import org.cyblight.android.notifications.MessageNotificationMonitor
import org.cyblight.android.notifications.PushTokenRegistrar
import org.cyblight.android.workers.LoginNotificationWorker
import org.cyblight.android.workers.MessageNotificationWorker
import org.cyblight.android.util.SystemSettings
import org.cyblight.android.ui.messages.ChatEditTarget
import org.cyblight.android.ui.messages.ChatFormatUtils
import org.cyblight.android.ui.messages.ChatReplyTarget
import java.io.File
import java.util.Calendar

private data class ArchivistTracker(
    val chatId: String,
    var pinned: Boolean = false,
    var edited: Boolean = false,
    var reacted: Boolean = false,
    var forwarded: Boolean = false,
) {
    fun isComplete(): Boolean = pinned && edited && reacted && forwarded
}

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
    SecurityCheck,
    LoginHistory,
    TrustedDevices,
    Passkeys,
    Sessions,
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
    val pendingRequests: List<FriendDto> = emptyList(),
    val sentRequests: List<FriendDto> = emptyList(),
    val isFriendsLoading: Boolean = false,
    val friendSearchResults: List<FriendDto> = emptyList(),
    val isFriendSearchLoading: Boolean = false,
    val friendSearchError: String? = null,
    val friendsActionMessage: String? = null,
    val friendsActionError: String? = null,
    val conversations: List<ConversationPreview> = emptyList(),
    val friendsError: String? = null,
    val messagesError: String? = null,
    val chatMessages: List<MessageDto> = emptyList(),
    val chatPinnedMessage: PinnedMessageDto? = null,
    val chatReplyTarget: ChatReplyTarget? = null,
    val chatEditTarget: ChatEditTarget? = null,
    val chatFriendId: String? = null,
    val chatFriendName: String? = null,
    val chatFriendIsOnline: Boolean = false,
    val chatFriendLastSeenAt: Long? = null,
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
    val securityOverview: SecurityOverview? = null,
    val isSecurityLoading: Boolean = false,
    val securityError: String? = null,
    val passkeys: List<PasskeyDto> = emptyList(),
    val isPasskeysLoading: Boolean = false,
    val passkeysError: String? = null,
    val isPasskeyRegistering: Boolean = false,
    val passkeyRegisterError: String? = null,
    val isPasskeyDeleting: Boolean = false,
    val passkeyDeleteError: String? = null,
    val trustedDevices: List<TrustedDeviceDto> = emptyList(),
    val isTrustedDevicesLoading: Boolean = false,
    val trustedDevicesError: String? = null,
    val isTrustedDeviceRemoving: Boolean = false,
    val loginHistory: List<LoginHistoryEntryDto> = emptyList(),
    val isLoginHistoryLoading: Boolean = false,
    val loginHistoryError: String? = null,
    val easterFlags: EasterFlagsDto? = null,
    val isEasterLoading: Boolean = false,
    val easterError: String? = null,
    val showLightCatcherGame: Boolean = false,
    val lightCatcherUnlocking: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val notificationsEnabled: Boolean = true,
    val loginAlertsEnabled: Boolean = true,
    val messageAlertsEnabled: Boolean = true,
    val appLockEnabled: Boolean = false,
    val appLockBiometric: Boolean = true,
    val appLockPinConfigured: Boolean = false,
    val appLockTimeout: AppLockTimeout = AppLockTimeout.IMMEDIATE,
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
    private val securityRepository = SecurityRepository(api)
    private val updateRepository = UpdateRepository(application)
    private val updatePreferences = UpdatePreferences(application)
    private var pendingUpdate: AppUpdateInfo? = null
    private val loginNotificationMonitor = LoginNotificationMonitor(application)
    private val messageNotificationMonitor = MessageNotificationMonitor(application)
    private var chatPresenceJob: Job? = null
    private var chatRefreshJob: Job? = null
    private var nightGuardJob: Job? = null
    private var archivistTracker: ArchivistTracker? = null
    private var appLockBackgroundedAtMs: Long? = null
    private var skipAppLockOnce = false

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
                val loginAlerts = appPreferences.getLoginAlertsEnabled()
                val messageAlerts = appPreferences.getMessageAlertsEnabled()
                val appLockEnabled = appPreferences.getAppLockEnabled()
                val appLockBiometric = appPreferences.getAppLockBiometric()
                val appLockPinConfigured = appPreferences.hasAppLockPin()
                val appLockTimeout = appPreferences.getAppLockTimeout()
                LocaleManager.apply(savedLocale)
                _uiState.value = _uiState.value.copy(
                    locale = savedLocale,
                    themeMode = savedTheme,
                    notificationsEnabled = notifications,
                    loginAlertsEnabled = loginAlerts,
                    messageAlertsEnabled = messageAlerts,
                    appLockEnabled = appLockEnabled && appLockPinConfigured,
                    appLockBiometric = appLockBiometric,
                    appLockPinConfigured = appLockPinConfigured,
                    appLockTimeout = appLockTimeout,
                )

                val user = authRepository.restoreSession()
                _uiState.value = if (user != null) {
                    _uiState.value.copy(screen = AppScreen.Main, user = user)
                } else {
                    _uiState.value.copy(screen = AppScreen.Login)
                }

                if (user != null) {
                    when (authRepository.refreshSession()) {
                        SessionRefreshResult.Expired -> forceLogout()
                        else -> {
                            refreshSocialData()
                            LoginNotificationWorker.schedule(getApplication())
                            MessageNotificationWorker.schedule(getApplication())
                            PushTokenRegistrar.registerCurrentToken(getApplication())
                        }
                    }
                }
            } catch (_: Exception) {
                forceLogout()
            } finally {
                checkForUpdate(force = true)
            }
        }
    }

    private fun forceLogout() {
        LoginNotificationWorker.cancel(getApplication())
        MessageNotificationWorker.cancel(getApplication())
        PushTokenRegistrar.unregisterCurrentToken(getApplication())
        viewModelScope.launch {
            appPreferences.setActiveChatFriendId(null)
        }
        _uiState.value = AppUiState(
            screen = AppScreen.Login,
            locale = _uiState.value.locale,
            themeMode = _uiState.value.themeMode,
            notificationsEnabled = _uiState.value.notificationsEnabled,
            loginAlertsEnabled = _uiState.value.loginAlertsEnabled,
            messageAlertsEnabled = _uiState.value.messageAlertsEnabled,
            appLockEnabled = _uiState.value.appLockEnabled,
            appLockBiometric = _uiState.value.appLockBiometric,
            appLockPinConfigured = _uiState.value.appLockPinConfigured,
            appLockTimeout = _uiState.value.appLockTimeout,
        )
    }

    fun checkForUpdate(force: Boolean = false) {
        viewModelScope.launch {
            if (!force && !updatePreferences.shouldAutoCheckNow()) return@launch

            updateRepository.checkForUpdate()
                .onSuccess { info ->
                    updatePreferences.markAutoChecked()
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
            if (resolved && _uiState.value.user != null) {
                if (_uiState.value.loginAlertsEnabled) {
                    LoginNotificationWorker.schedule(getApplication())
                }
                if (_uiState.value.messageAlertsEnabled) {
                    MessageNotificationWorker.schedule(getApplication())
                    PushTokenRegistrar.registerCurrentToken(getApplication())
                }
            } else if (!resolved) {
                LoginNotificationWorker.cancel(getApplication())
                MessageNotificationWorker.cancel(getApplication())
            }
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

    fun refreshSecurityOverview() {
        if (_uiState.value.isSecurityLoading) return
        loadSecurityOverview()
    }

    fun forceRefreshSecurityOverview() {
        loadSecurityOverview()
    }

    fun setLoginAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setLoginAlertsEnabled(enabled)
            _uiState.value = _uiState.value.copy(loginAlertsEnabled = enabled)
            if (enabled && _uiState.value.user != null && _uiState.value.notificationsEnabled) {
                LoginNotificationWorker.schedule(getApplication())
            } else if (!enabled) {
                LoginNotificationWorker.cancel(getApplication())
            }
        }
    }

    fun setMessageAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setMessageAlertsEnabled(enabled)
            _uiState.value = _uiState.value.copy(messageAlertsEnabled = enabled)
            if (enabled && _uiState.value.user != null && _uiState.value.notificationsEnabled) {
                MessageNotificationWorker.schedule(getApplication())
                PushTokenRegistrar.registerCurrentToken(getApplication())
            } else if (!enabled) {
                MessageNotificationWorker.cancel(getApplication())
                PushTokenRegistrar.unregisterCurrentToken(getApplication())
            }
        }
    }

    fun setAppLockBiometric(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setAppLockBiometric(enabled)
            _uiState.value = _uiState.value.copy(appLockBiometric = enabled)
        }
    }

    fun setupAppLockPin(pin: String, enableLock: Boolean) {
        viewModelScope.launch {
            appPreferences.setAppLockPin(pin)
            if (enableLock) {
                appPreferences.setAppLockEnabled(true)
            }
            _uiState.value = _uiState.value.copy(
                appLockPinConfigured = true,
                appLockEnabled = enableLock || _uiState.value.appLockEnabled,
            )
        }
    }

    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled && !appPreferences.hasAppLockPin()) return@launch
            appPreferences.setAppLockEnabled(enabled)
            if (!enabled) {
                appLockBackgroundedAtMs = null
            }
            _uiState.value = _uiState.value.copy(appLockEnabled = enabled)
        }
    }

    fun setAppLockTimeout(timeout: AppLockTimeout) {
        viewModelScope.launch {
            appPreferences.setAppLockTimeout(timeout)
            _uiState.value = _uiState.value.copy(appLockTimeout = timeout)
        }
    }

    fun onAppBackgrounded() {
        if (_uiState.value.appLockEnabled) {
            appLockBackgroundedAtMs = System.currentTimeMillis()
        }
    }

    fun onMainScreenBackgrounded() {
        if (_uiState.value.screen != AppScreen.Main || _uiState.value.user == null) return
        viewModelScope.launch {
            appPreferences.setActiveChatFriendId(null)
        }
        MessageNotificationWorker.scheduleImmediate(getApplication())
    }

    fun onAppUnlocked() {
        appLockBackgroundedAtMs = null
    }

    fun shouldShowAppLock(isColdStart: Boolean): Boolean {
        val state = _uiState.value
        if (!state.appLockEnabled || !state.appLockPinConfigured) return false
        if (skipAppLockOnce) {
            skipAppLockOnce = false
            return false
        }
        if (isColdStart) return true

        val backgroundedAt = appLockBackgroundedAtMs ?: return false
        val timeout = state.appLockTimeout.millis
        if (timeout == 0L) return true
        return System.currentTimeMillis() - backgroundedAt >= timeout
    }

    suspend fun verifyAppLockPin(pin: String): Boolean =
        appPreferences.verifyAppLockPin(pin)

    fun checkLoginNotifications() {
        if (_uiState.value.screen != AppScreen.Main || _uiState.value.user == null) return
        viewModelScope.launch {
            loginNotificationMonitor.checkForNewLogin()
        }
    }

    fun checkMessageNotifications() {
        if (_uiState.value.screen != AppScreen.Main || _uiState.value.user == null) return
        viewModelScope.launch {
            messageNotificationMonitor.checkForNewMessages()
        }
    }

    fun openChatFromNotification(friendId: String, friendName: String) {
        if (friendId.isBlank()) return
        openChat(friendId, friendName.ifBlank { friendId })
    }

    private fun onAuthenticated() {
        skipAppLockOnce = true
        viewModelScope.launch {
            loginNotificationMonitor.markOwnLoginGracePeriod()
            messageNotificationMonitor.syncBaselineFromServer()
            LoginNotificationWorker.schedule(getApplication())
            MessageNotificationWorker.schedule(getApplication())
            PushTokenRegistrar.registerCurrentToken(getApplication())
        }
        checkForUpdate(force = true)
    }

    fun openSecurityCheck() {
        _uiState.value = _uiState.value.copy(detailScreen = DetailScreen.SecurityCheck)
        forceRefreshSecurityOverview()
    }

    fun openAccountSecurity(context: Context) {
        val locale = _uiState.value.locale
        ExternalLinks.openUrl(context, "https://cyblight.org/$locale/account-security/")
    }

    fun openPasskeys() {
        _uiState.value = _uiState.value.copy(
            detailScreen = DetailScreen.Passkeys,
            passkeysError = null,
            isPasskeysLoading = true,
        )
        loadPasskeys()
    }

    fun openTrustedDevices() {
        _uiState.value = _uiState.value.copy(
            detailScreen = DetailScreen.TrustedDevices,
            trustedDevicesError = null,
            isTrustedDevicesLoading = true,
        )
        loadTrustedDevices()
    }

    fun openLoginHistory() {
        _uiState.value = _uiState.value.copy(
            detailScreen = DetailScreen.LoginHistory,
            loginHistoryError = null,
            isLoginHistoryLoading = true,
        )
        loadLoginHistory()
    }

    fun openSessions() {
        _uiState.value = _uiState.value.copy(
            detailScreen = DetailScreen.Sessions,
            sessionsError = null,
            isSessionsLoading = true,
        )
        loadSessions()
    }

    fun refreshPasskeys() {
        loadPasskeys()
    }

    fun registerPasskey(activity: Activity, name: String) {
        val resolvedName = name.ifBlank {
            getApplication<Application>().getString(org.cyblight.android.R.string.security_passkeys_default_name)
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isPasskeyRegistering = true,
                passkeyRegisterError = null,
            )
            securityRepository.registerPasskey(activity, resolvedName)
                .onSuccess {
                    loadPasskeys()
                    loadSecurityOverview()
                    _uiState.value = _uiState.value.copy(
                        isPasskeyRegistering = false,
                        passkeyRegisterError = null,
                    )
                }
                .onFailure { error ->
                    val code = when (error) {
                        is PasskeyAuthException -> error.code
                        is IllegalStateException -> error.message ?: "passkey_register_failed"
                        else -> "passkey_register_failed"
                    }
                    _uiState.value = _uiState.value.copy(
                        isPasskeyRegistering = false,
                        passkeyRegisterError = code,
                    )
                }
        }
    }

    fun clearPasskeyRegisterError() {
        _uiState.value = _uiState.value.copy(passkeyRegisterError = null)
    }

    fun deletePasskey(passkeyId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isPasskeyDeleting = true,
                passkeyDeleteError = null,
            )
            securityRepository.deletePasskey(passkeyId)
                .onSuccess {
                    loadPasskeys()
                    loadSecurityOverview()
                    _uiState.value = _uiState.value.copy(
                        isPasskeyDeleting = false,
                        passkeyDeleteError = null,
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isPasskeyDeleting = false,
                        passkeyDeleteError = "passkey_remove_failed",
                    )
                }
        }
    }

    fun clearPasskeyDeleteError() {
        _uiState.value = _uiState.value.copy(passkeyDeleteError = null)
    }

    fun refreshTrustedDevices() {
        loadTrustedDevices()
    }

    fun refreshLoginHistory() {
        loadLoginHistory()
    }

    fun removeTrustedDevice(deviceId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isTrustedDeviceRemoving = true,
                trustedDevicesError = null,
            )
            securityRepository.removeTrustedDevice(deviceId)
                .onSuccess {
                    loadTrustedDevices()
                    _uiState.value = _uiState.value.copy(isTrustedDeviceRemoving = false)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isTrustedDeviceRemoving = false,
                        trustedDevicesError = "trusted_device_remove_failed",
                    )
                }
        }
    }

    fun refreshEasterFlags() {
        val state = _uiState.value
        if (state.isEasterLoading) return
        if (state.easterFlags != null && state.easterError == null) return
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
            DetailScreen.Sessions -> {
                _uiState.value = _uiState.value.copy(
                    detailScreen = DetailScreen.None,
                    sessionsError = null,
                )
            }
            DetailScreen.SecurityCheck,
            DetailScreen.LoginHistory,
            DetailScreen.TrustedDevices,
            DetailScreen.Passkeys,
            -> {
                _uiState.value = _uiState.value.copy(
                    detailScreen = DetailScreen.None,
                    passkeysError = null,
                    passkeyRegisterError = null,
                    passkeyDeleteError = null,
                    trustedDevicesError = null,
                    loginHistoryError = null,
                )
            }
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

    private fun loadSecurityOverview() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSecurityLoading = true, securityError = null)
            securityRepository.loadOverview()
                .onSuccess { overview ->
                    _uiState.value = _uiState.value.copy(
                        securityOverview = overview,
                        isSecurityLoading = false,
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isSecurityLoading = false,
                        securityError = "security_load_failed",
                    )
                }
        }
    }

    private fun syncOverviewPasskeyCount(count: Int) {
        val overview = _uiState.value.securityOverview ?: return
        if (overview.passkeyCount == count) return
        _uiState.value = _uiState.value.copy(
            securityOverview = overview.copy(passkeyCount = count),
        )
    }

    private fun loadPasskeys() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPasskeysLoading = true, passkeysError = null)
            securityRepository.loadPasskeys()
                .onSuccess { passkeys ->
                    _uiState.value = _uiState.value.copy(
                        passkeys = passkeys,
                        isPasskeysLoading = false,
                    )
                    syncOverviewPasskeyCount(passkeys.size)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isPasskeysLoading = false,
                        passkeysError = "passkeys_load_failed",
                    )
                }
        }
    }

    private fun loadTrustedDevices() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isTrustedDevicesLoading = true,
                trustedDevicesError = null,
            )
            securityRepository.loadTrustedDevices()
                .onSuccess { devices ->
                    _uiState.value = _uiState.value.copy(
                        trustedDevices = devices,
                        isTrustedDevicesLoading = false,
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isTrustedDevicesLoading = false,
                        trustedDevicesError = "trusted_devices_load_failed",
                    )
                }
        }
    }

    private fun loadLoginHistory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoginHistoryLoading = true,
                loginHistoryError = null,
            )
            securityRepository.loadLoginHistory()
                .onSuccess { history ->
                    _uiState.value = _uiState.value.copy(
                        loginHistory = history,
                        isLoginHistoryLoading = false,
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isLoginHistoryLoading = false,
                        loginHistoryError = "login_history_load_failed",
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
                    _uiState.value.user?.login?.let(EasterLogger::logLightCatcher)
                    _uiState.value = _uiState.value.copy(
                        showLightCatcherGame = false,
                        lightCatcherUnlocking = false,
                    )
                    refreshEasterFlagsFromServer()
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
                    onAuthenticated()
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
                    onAuthenticated()
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
                    onAuthenticated()
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
                loginAlertsEnabled = _uiState.value.loginAlertsEnabled,
                messageAlertsEnabled = _uiState.value.messageAlertsEnabled,
                appLockEnabled = _uiState.value.appLockEnabled,
                appLockBiometric = _uiState.value.appLockBiometric,
                appLockPinConfigured = _uiState.value.appLockPinConfigured,
                appLockTimeout = _uiState.value.appLockTimeout,
            )
            appLockBackgroundedAtMs = null
            skipAppLockOnce = false
            LoginNotificationWorker.cancel(getApplication())
            MessageNotificationWorker.cancel(getApplication())
            PushTokenRegistrar.unregisterCurrentToken(getApplication())
        }
    }

    fun onAppResumed() {
        checkForUpdate()
        if (_uiState.value.screen != AppScreen.Main || _uiState.value.user == null) return
        viewModelScope.launch {
            _uiState.value.chatFriendId?.let { appPreferences.setActiveChatFriendId(it) }
        }
        checkLoginNotifications()
        checkMessageNotifications()
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
            _uiState.value = _uiState.value.copy(
                friendsError = null,
                messagesError = null,
                isFriendsLoading = true,
            )

            val friendsDeferred = async { friendsRepository.loadFriends() }
            val pendingDeferred = async { friendsRepository.loadPendingRequests() }
            val sentDeferred = async { friendsRepository.loadSentRequests() }

            val friendsResult = friendsDeferred.await()
            val pendingResult = pendingDeferred.await()
            val sentResult = sentDeferred.await()

            friendsResult
                .onSuccess { friends ->
                    _uiState.value = _uiState.value.copy(
                        friends = friends,
                        pendingRequests = pendingResult.getOrElse { emptyList() },
                        sentRequests = sentResult.getOrElse { emptyList() },
                        isFriendsLoading = false,
                    )
                    messagesRepository.loadConversations(friends)
                        .onSuccess { conversations ->
                            _uiState.value = _uiState.value.copy(conversations = conversations)
                        }
                        .onFailure {
                            _uiState.value = _uiState.value.copy(messagesError = "unread_load_failed")
                        }
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        friendsError = "friends_load_failed",
                        isFriendsLoading = false,
                    )
                }
        }
    }

    fun searchUsers(query: String) {
        viewModelScope.launch {
            val trimmed = query.trim()
            if (trimmed.length < 2) {
                _uiState.value = _uiState.value.copy(
                    friendSearchResults = emptyList(),
                    friendSearchError = if (trimmed.isEmpty()) null else "search_query_too_short",
                    isFriendSearchLoading = false,
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                isFriendSearchLoading = true,
                friendSearchError = null,
            )

            friendsRepository.searchUsers(trimmed)
                .onSuccess { users ->
                    _uiState.value = _uiState.value.copy(
                        friendSearchResults = users,
                        isFriendSearchLoading = false,
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        friendSearchResults = emptyList(),
                        isFriendSearchLoading = false,
                        friendSearchError = "search_failed",
                    )
                }
        }
    }

    fun clearFriendSearch() {
        _uiState.value = _uiState.value.copy(
            friendSearchResults = emptyList(),
            friendSearchError = null,
            isFriendSearchLoading = false,
        )
    }

    fun clearFriendsActionFeedback() {
        _uiState.value = _uiState.value.copy(
            friendsActionMessage = null,
            friendsActionError = null,
        )
    }

    fun addFriend(username: String) {
        viewModelScope.launch {
            friendsRepository.addFriend(username)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(friendsActionMessage = "friend_request_sent")
                    refreshSocialData()
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(friendsActionError = "friend_action_failed")
                }
        }
    }

    fun acceptFriendRequest(friendId: String, username: String) {
        viewModelScope.launch {
            friendsRepository.acceptFriend(friendId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        friendsActionMessage = "friend_accepted:$username",
                    )
                    refreshSocialData()
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(friendsActionError = "friend_action_failed")
                }
        }
    }

    fun rejectFriendRequest(friendId: String) {
        viewModelScope.launch {
            friendsRepository.rejectFriend(friendId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(friendsActionMessage = "friend_request_rejected")
                    refreshSocialData()
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(friendsActionError = "friend_action_failed")
                }
        }
    }

    fun removeFriend(friendId: String, username: String) {
        viewModelScope.launch {
            friendsRepository.removeFriend(friendId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        friendsActionMessage = "friend_removed:$username",
                    )
                    refreshSocialData()
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(friendsActionError = "friend_action_failed")
                }
        }
    }

    fun cancelFriendRequest(friendId: String) {
        viewModelScope.launch {
            friendsRepository.removeFriend(friendId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(friendsActionMessage = "request_cancelled")
                    refreshSocialData()
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(friendsActionError = "friend_action_failed")
                }
        }
    }

    fun openChat(friendId: String, friendName: String) {
        chatPresenceJob?.cancel()
        chatRefreshJob?.cancel()
        val cachedFriend = _uiState.value.friends.find { it.id == friendId }
        viewModelScope.launch {
            appPreferences.setActiveChatFriendId(friendId)
            messageNotificationMonitor.clearFriendNotification(friendId)
            _uiState.value = _uiState.value.copy(
                chatFriendId = friendId,
                chatFriendName = friendName,
                chatFriendIsOnline = cachedFriend?.isOnline ?: false,
                chatFriendLastSeenAt = cachedFriend?.lastSeenAt,
                isChatLoading = true,
                messagesError = null,
                chatMessages = emptyList(),
                chatPinnedMessage = null,
                chatReplyTarget = null,
                chatEditTarget = null,
            )
            archivistTracker = ArchivistTracker(friendId)
            refreshChatFriendPresence(friendId)
            chatPresenceJob = viewModelScope.launch {
                while (isActive) {
                    delay(5_000)
                    refreshChatFriendPresence(friendId)
                }
            }
            chatRefreshJob = viewModelScope.launch {
                while (isActive) {
                    delay(8_000)
                    reloadChat(friendId)
                }
            }

            messagesRepository.loadMessages(friendId)
                .onSuccess { thread ->
                    if (_uiState.value.chatFriendId != friendId) return@onSuccess
                    _uiState.value = _uiState.value.copy(
                        chatMessages = thread.messages,
                        chatPinnedMessage = thread.pinned,
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
        chatPresenceJob?.cancel()
        chatPresenceJob = null
        chatRefreshJob?.cancel()
        chatRefreshJob = null
        nightGuardJob?.cancel()
        nightGuardJob = null
        archivistTracker = null
        viewModelScope.launch {
            appPreferences.setActiveChatFriendId(null)
        }
        _uiState.value = _uiState.value.copy(
            chatFriendId = null,
            chatFriendName = null,
            chatFriendIsOnline = false,
            chatFriendLastSeenAt = null,
            chatMessages = emptyList(),
            chatPinnedMessage = null,
            chatReplyTarget = null,
            chatEditTarget = null,
            messagesError = null,
        )
        refreshSocialData()
    }

    fun clearChatReply() {
        _uiState.value = _uiState.value.copy(chatReplyTarget = null)
    }

    fun clearChatEdit() {
        _uiState.value = _uiState.value.copy(chatEditTarget = null)
    }

    fun startChatReply(message: MessageDto) {
        val friendName = _uiState.value.chatFriendName.orEmpty()
        val isMine = message.senderId == _uiState.value.user?.id
        val author = if (isMine) {
            getApplication<Application>().getString(org.cyblight.android.R.string.chat_reply_you)
        } else {
            friendName.ifBlank { getApplication<Application>().getString(org.cyblight.android.R.string.chat_reply_peer) }
        }
        val preview = ChatFormatUtils.stripMetadataTokens(message.content)
            .replace(Regex("""\s+"""), " ")
            .trim()
            .take(220)
        _uiState.value = _uiState.value.copy(
            chatReplyTarget = ChatReplyTarget(message.id, author, preview),
            chatEditTarget = null,
        )
    }

    fun startChatEdit(message: MessageDto) {
        _uiState.value = _uiState.value.copy(
            chatEditTarget = ChatEditTarget(message.id, message.content),
            chatReplyTarget = null,
        )
    }

    fun reactToChatMessage(messageId: String, emoji: String) {
        val friendId = _uiState.value.chatFriendId ?: return
        val trimmed = emoji.trim()
        if (trimmed.isEmpty()) return

        val optimisticMessages = _uiState.value.chatMessages.map { message ->
            if (message.id != messageId) return@map message
            val existing = message.reactions.find { it.emoji == trimmed }
            val updatedReactions = if (existing != null) {
                if (existing.count <= 1) {
                    message.reactions.filter { it.emoji != trimmed }
                } else {
                    message.reactions.map { reaction ->
                        if (reaction.emoji == trimmed) reaction.copy(count = reaction.count - 1) else reaction
                    }
                }
            } else {
                message.reactions + MessageReactionDto(emoji = trimmed, count = 1)
            }
            message.copy(reactions = updatedReactions)
        }
        _uiState.value = _uiState.value.copy(chatMessages = optimisticMessages)

        viewModelScope.launch {
            messagesRepository.reactToMessage(messageId, trimmed)
                .onSuccess {
                    reloadChat(friendId)
                    markArchivistAction(friendId) { it.reacted = true }
                }
                .onFailure {
                    reloadChat(friendId)
                }
        }
    }

    fun pinChatMessage(message: MessageDto) {
        val friendId = _uiState.value.chatFriendId ?: return
        viewModelScope.launch {
            messagesRepository.pinMessage(message.id)
                .onSuccess {
                    reloadChat(friendId)
                    markArchivistAction(friendId) { it.pinned = true }
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(messagesError = "pin_failed")
                }
        }
    }

    fun unpinChatMessage(message: MessageDto) {
        val friendId = _uiState.value.chatFriendId ?: return
        viewModelScope.launch {
            messagesRepository.unpinMessage(message.id)
                .onSuccess { reloadChat(friendId) }
                .onFailure {
                    _uiState.value = _uiState.value.copy(messagesError = "unpin_failed")
                }
        }
    }

    fun deleteChatMessage(messageId: String) {
        val friendId = _uiState.value.chatFriendId ?: return
        viewModelScope.launch {
            messagesRepository.deleteMessage(messageId)
                .onSuccess { reloadChat(friendId) }
                .onFailure {
                    _uiState.value = _uiState.value.copy(messagesError = "delete_failed")
                }
        }
    }

    fun deleteChatMessages(messageIds: List<String>) {
        val friendId = _uiState.value.chatFriendId ?: return
        viewModelScope.launch {
            messagesRepository.deleteMessages(messageIds)
                .onSuccess { reloadChat(friendId) }
                .onFailure {
                    _uiState.value = _uiState.value.copy(messagesError = "delete_failed")
                }
        }
    }

    fun forwardChatMessage(targetFriendId: String, content: String) {
        val text = ChatFormatUtils.stripMetadataTokens(content).trim()
        if (text.isEmpty()) return
        val sourceChatId = _uiState.value.chatFriendId
        viewModelScope.launch {
            messagesRepository.sendMessage(targetFriendId, text)
                .onSuccess {
                    sourceChatId?.let { chatId ->
                        markArchivistAction(chatId) { it.forwarded = true }
                    }
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(messagesError = "send_failed")
                }
        }
    }

    private suspend fun reloadChat(friendId: String) {
        messagesRepository.loadMessages(friendId)
            .onSuccess { thread ->
                if (_uiState.value.chatFriendId != friendId) return
                _uiState.value = _uiState.value.copy(
                    chatMessages = thread.messages,
                    chatPinnedMessage = thread.pinned,
                    messagesError = null,
                )
                refreshSocialData()
            }
    }

    private fun refreshChatFriendPresence(friendId: String) {
        viewModelScope.launch {
            friendsRepository.loadFriendPresence(friendId)
                .onSuccess { presence ->
                    if (_uiState.value.chatFriendId != friendId) return@onSuccess
                    _uiState.value = _uiState.value.copy(
                        chatFriendIsOnline = presence.isOnline,
                        chatFriendLastSeenAt = presence.lastSeenAt,
                    )
                }
        }
    }

    fun sendMessage(content: String) {
        val friendId = _uiState.value.chatFriendId ?: return
        val editTarget = _uiState.value.chatEditTarget
        val replyTarget = _uiState.value.chatReplyTarget
        var finalContent = content.trim()
        if (finalContent.isEmpty()) return
        if (editTarget == null && replyTarget != null) {
            finalContent = ChatFormatUtils.appendReplyToken(
                finalContent,
                replyTarget.messageId,
                replyTarget.author,
                replyTarget.preview,
            )
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSending = true, messagesError = null)
            val action = if (editTarget != null) {
                messagesRepository.editMessage(editTarget.messageId, finalContent)
            } else {
                messagesRepository.sendMessage(friendId, finalContent)
            }
            action
                .onSuccess {
                    reloadChat(friendId)
                    if (editTarget != null) {
                        markArchivistAction(friendId) { it.edited = true }
                    } else {
                        maybeUnlockEchoEaster()
                    }
                    _uiState.value = _uiState.value.copy(
                        isSending = false,
                        chatReplyTarget = null,
                        chatEditTarget = null,
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isSending = false,
                        messagesError = if (editTarget != null) "edit_failed" else "send_failed",
                    )
                }
        }
    }

    fun trackNightGuardConditions(isDarkTheme: Boolean, isMainScreen: Boolean) {
        nightGuardJob?.cancel()
        nightGuardJob = null
        if (!isMainScreen || !isDarkTheme || _uiState.value.easterFlags?.nightGuard == true) return

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (hour >= 6) return

        nightGuardJob = viewModelScope.launch {
            delay(30_000)
            if (_uiState.value.easterFlags?.nightGuard == true) return@launch
            profileRepository.unlockNightGuard()
                .onSuccess {
                    _uiState.value.user?.login?.let(EasterLogger::logNightGuard)
                    refreshEasterFlagsFromServer()
                }
        }
    }

    fun onBiometricUnlockSuccess() {
        if (_uiState.value.easterFlags?.trustedFingerprint == true) return
        viewModelScope.launch {
            val count = appPreferences.incrementBiometricUnlockCount()
            if (count >= 100) {
                profileRepository.unlockTrustedFingerprint()
                    .onSuccess {
                        _uiState.value.user?.login?.let(EasterLogger::logTrustedFingerprint)
                        refreshEasterFlagsFromServer()
                    }
            }
        }
    }

    private fun maybeUnlockEchoEaster() {
        if (_uiState.value.easterFlags?.echo == true) return
        val calendar = Calendar.getInstance()
        if (calendar.get(Calendar.HOUR_OF_DAY) != 23 || calendar.get(Calendar.MINUTE) != 59) return
        viewModelScope.launch {
            profileRepository.unlockEcho()
                .onSuccess {
                    _uiState.value.user?.login?.let(EasterLogger::logEcho)
                    refreshEasterFlagsFromServer()
                }
        }
    }

    private fun markArchivistAction(chatId: String, mark: (ArchivistTracker) -> Unit) {
        if (_uiState.value.easterFlags?.archivist == true) return
        val tracker = archivistTracker ?: return
        if (tracker.chatId != chatId) return
        mark(tracker)
        if (!tracker.isComplete()) return
        viewModelScope.launch {
            profileRepository.unlockArchivist()
                .onSuccess {
                    _uiState.value.user?.login?.let(EasterLogger::logArchivist)
                    refreshEasterFlagsFromServer()
                }
        }
    }

    private fun refreshEasterFlagsFromServer() {
        viewModelScope.launch {
            val previous = _uiState.value.easterFlags
            val hadBridge = previous?.bridge == true
            profileRepository.loadEasterFlags()
                .onSuccess { flags ->
                    if (previous != null && !hadBridge && flags.bridge) {
                        _uiState.value.user?.login?.let(EasterLogger::logBridge)
                    }
                    _uiState.value = _uiState.value.copy(
                        easterFlags = flags,
                        easterError = null,
                    )
                }
        }
    }
}
