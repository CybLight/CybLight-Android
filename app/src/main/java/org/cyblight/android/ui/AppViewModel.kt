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
import org.cyblight.android.auth.PasskeyAuthException
import org.cyblight.android.data.ApiClient
import android.content.Context
import org.cyblight.android.data.api.EasterFlagsDto
import org.cyblight.android.data.api.FriendDto
import org.cyblight.android.data.api.LoginHistoryEntryDto
import org.cyblight.android.data.api.MessageDto
import org.cyblight.android.data.api.PasskeyDto
import org.cyblight.android.data.api.ProfileDto
import org.cyblight.android.data.api.SessionDto
import org.cyblight.android.data.api.TrustedDeviceDto
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
import org.cyblight.android.workers.LoginNotificationWorker
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
    val appLockEnabled: Boolean = false,
    val appLockBiometric: Boolean = true,
    val appLockPinConfigured: Boolean = false,
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
                val appLockEnabled = appPreferences.getAppLockEnabled()
                val appLockBiometric = appPreferences.getAppLockBiometric()
                val appLockPinConfigured = appPreferences.hasAppLockPin()
                LocaleManager.apply(savedLocale)
                _uiState.value = _uiState.value.copy(
                    locale = savedLocale,
                    themeMode = savedTheme,
                    notificationsEnabled = notifications,
                    loginAlertsEnabled = loginAlerts,
                    appLockEnabled = appLockEnabled && appLockPinConfigured,
                    appLockBiometric = appLockBiometric,
                    appLockPinConfigured = appLockPinConfigured,
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
                        else -> {
                            refreshSocialData()
                            LoginNotificationWorker.schedule(getApplication())
                        }
                    }
                }

                checkForUpdate()
            } catch (_: Exception) {
                forceLogout()
            }
        }
    }

    private fun forceLogout() {
        LoginNotificationWorker.cancel(getApplication())
        _uiState.value = AppUiState(
            screen = AppScreen.Login,
            locale = _uiState.value.locale,
            themeMode = _uiState.value.themeMode,
            notificationsEnabled = _uiState.value.notificationsEnabled,
            loginAlertsEnabled = _uiState.value.loginAlertsEnabled,
            appLockEnabled = _uiState.value.appLockEnabled,
            appLockBiometric = _uiState.value.appLockBiometric,
            appLockPinConfigured = _uiState.value.appLockPinConfigured,
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
            if (enabled && _uiState.value.user != null) {
                LoginNotificationWorker.schedule(getApplication())
            } else if (!enabled) {
                LoginNotificationWorker.cancel(getApplication())
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
            _uiState.value = _uiState.value.copy(appLockEnabled = enabled)
        }
    }

    suspend fun verifyAppLockPin(pin: String): Boolean =
        appPreferences.verifyAppLockPin(pin)

    fun checkLoginNotifications() {
        if (_uiState.value.screen != AppScreen.Main || _uiState.value.user == null) return
        viewModelScope.launch {
            loginNotificationMonitor.checkForNewLogin()
        }
    }

    private fun onAuthenticated() {
        viewModelScope.launch {
            loginNotificationMonitor.markOwnLoginGracePeriod()
            LoginNotificationWorker.schedule(getApplication())
        }
    }

    fun openSecurityCheck() {
        _uiState.value = _uiState.value.copy(detailScreen = DetailScreen.SecurityCheck)
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

    private fun loadPasskeys() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPasskeysLoading = true, passkeysError = null)
            securityRepository.loadPasskeys()
                .onSuccess { passkeys ->
                    _uiState.value = _uiState.value.copy(
                        passkeys = passkeys,
                        isPasskeysLoading = false,
                    )
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
                    val current = _uiState.value.easterFlags
                    _uiState.value.user?.login?.let(EasterLogger::logLightCatcher)
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
                appLockEnabled = _uiState.value.appLockEnabled,
                appLockBiometric = _uiState.value.appLockBiometric,
                appLockPinConfigured = _uiState.value.appLockPinConfigured,
            )
            LoginNotificationWorker.cancel(getApplication())
        }
    }

    fun refreshSessionOnResume() {
        if (_uiState.value.screen != AppScreen.Main || _uiState.value.user == null) return
        checkLoginNotifications()
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
