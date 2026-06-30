package org.cyblight.android.ui

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import org.cyblight.android.auth.PasskeyAuthException
import org.cyblight.android.data.ApiClient
import org.cyblight.android.data.realtime.ChatWebSocketClient
import org.cyblight.android.data.realtime.ChatWsEvent
import android.content.Context
import org.cyblight.android.BuildConfig
import org.cyblight.android.data.api.EasterFlagsDto
import org.cyblight.android.data.api.EasterProgress
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
import org.cyblight.android.data.preferences.RootBackBehavior
import org.cyblight.android.data.preferences.SwipeBackEdgeWidth
import org.cyblight.android.data.preferences.SwipeBackSensitivity
import org.cyblight.android.data.preferences.ChatDefaultTheme
import org.cyblight.android.data.preferences.ChatFontSize
import org.cyblight.android.data.preferences.ThemeMode
import org.cyblight.android.data.home.ChangelogLocalizedNotes
import org.cyblight.android.data.home.ChangelogRelease
import org.cyblight.android.data.home.HomeContent
import org.cyblight.android.data.home.HomeContentRepository
import org.cyblight.android.crypto.SignalCryptoManager
import org.cyblight.android.crypto.backup.CyblightBackupManager
import org.cyblight.android.integrations.google_drive.DriveBackupMetadata
import org.cyblight.android.integrations.google_drive.GoogleDriveStorageQuota
import org.cyblight.android.integrations.google_drive.GoogleDriveBackupService
import org.cyblight.android.integrations.google_drive.GoogleDriveConfig
import android.content.Intent
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
import org.cyblight.android.workers.ChatBackupWorker
import org.cyblight.android.workers.LoginNotificationWorker
import org.cyblight.android.workers.MessageNotificationWorker
import org.cyblight.android.R
import org.cyblight.android.data.preferences.ChatBackupFrequency
import org.cyblight.android.security.BackupPasswordStore
import org.cyblight.android.util.SystemSettings
import org.cyblight.android.ui.easter.EasterCelebrationKind
import org.cyblight.android.ui.easter.V010EasterUnlockHelper
import org.cyblight.android.ui.messages.ChatEditTarget
import org.cyblight.android.ui.messages.ChatFormatUtils
import org.cyblight.android.ui.messages.ChatReplyTarget
import org.cyblight.android.ui.main.MainTab
import java.io.File
import java.util.ArrayDeque
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
    Changelog,
}

private const val CHAT_MESSAGE_EXIT_ANIM_MS = 260L

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
    val chatDrafts: Map<String, String> = emptyMap(),
    val friendsError: String? = null,
    val messagesError: String? = null,
    val chatMessages: List<MessageDto> = emptyList(),
    val chatExitingMessageIds: Set<String> = emptySet(),
    val chatPinnedMessage: PinnedMessageDto? = null,
    val chatReplyTarget: ChatReplyTarget? = null,
    val chatEditTarget: ChatEditTarget? = null,
    val chatDraftText: String = "",
    val chatFormatToolbarHidden: Boolean = false,
    val chatDefaultTheme: ChatDefaultTheme = ChatDefaultTheme.SYSTEM,
    val chatQuoteColor: Int? = null,
    val chatSendWithEnter: Boolean = false,
    val chatFontSize: ChatFontSize = ChatFontSize.MEDIUM,
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
    val easterProgress: EasterProgress = EasterProgress(),
    val isEasterLoading: Boolean = false,
    val easterError: String? = null,
    val showLightCatcherGame: Boolean = false,
    val lightCatcherUnlocking: Boolean = false,
    val easterCelebration: EasterCelebrationKind? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val notificationsEnabled: Boolean = true,
    val loginAlertsEnabled: Boolean = true,
    val messageAlertsEnabled: Boolean = true,
    val appLockEnabled: Boolean = false,
    val appLockBiometric: Boolean = true,
    val appLockPinConfigured: Boolean = false,
    val appLockTimeout: AppLockTimeout = AppLockTimeout.IMMEDIATE,
    val appLockSettingsLoaded: Boolean = false,
    val pendingAppLock: Boolean = false,
    val selectedMainTab: MainTab = MainTab.Home,
    val friendsSubTab: Int = 0,
    val homeContent: HomeContent? = null,
    val isHomeLoading: Boolean = false,
    val homeError: String? = null,
    val changelogReleases: List<ChangelogRelease> = emptyList(),
    val isChangelogLoading: Boolean = false,
    val changelogError: String? = null,
    val swipeBackEnabled: Boolean = true,
    val systemBackEnabled: Boolean = true,
    val swipeBackSensitivity: SwipeBackSensitivity = SwipeBackSensitivity.NORMAL,
    val swipeBackEdgeWidth: SwipeBackEdgeWidth = SwipeBackEdgeWidth.NORMAL,
    val rootBackBehavior: RootBackBehavior = RootBackBehavior.HOME_THEN_EXIT,
    val encryptionReminderChatDismissed: Boolean = false,
    val homeWhatsNewBannerHidden: Boolean = false,
    val settingsFocusChatBackup: Boolean = false,
    val googleDriveAccountLabel: String? = null,
    val googleDriveAccountEmail: String? = null,
    val googleDriveAccountEmails: List<String> = emptyList(),
    val googleDriveBackupMetadata: org.cyblight.android.integrations.google_drive.DriveBackupMetadata? = null,
    val googleDriveStorageQuota: org.cyblight.android.integrations.google_drive.GoogleDriveStorageQuota? = null,
    val chatBackupFrequency: org.cyblight.android.data.preferences.ChatBackupFrequency =
        org.cyblight.android.data.preferences.ChatBackupFrequency.OFF,
    val chatBackupOverCellular: Boolean = false,
    val chatBackupHasStoredPassword: Boolean = false,
    val driveRestoreConfirmMetadata: org.cyblight.android.integrations.google_drive.DriveBackupMetadata? = null,
    val driveRestorePasswordOpen: Boolean = false,
    val driveRestoreBusy: Boolean = false,
    val driveRestoreProgress: Int = 0,
    val driveRestoreProgressKey: String? = null,
    val driveRestoreToast: String? = null,
    val driveRestoreToastIsError: Boolean = false,
    val settingsSection: org.cyblight.android.ui.settings.SettingsSection =
        org.cyblight.android.ui.settings.SettingsSection.Hub,
    val settingsReturnAfterDetail: Boolean = false,
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val sessionManager = SessionManager(application)
    private val appPreferences = AppPreferences(application)
    private val api = ApiClient.create(sessionManager)
    private val authRepository = AuthRepository(api, sessionManager)
    private val friendsRepository = FriendsRepository(api)
    private val signalCrypto = SignalCryptoManager(application, api)
    private val backupManager = CyblightBackupManager(application)
    private val googleDriveBackupService = GoogleDriveBackupService.create(application)
    private val backupPasswordStore = BackupPasswordStore(application)
    private val messagesRepository = MessagesRepository(application, api, signalCrypto) {
        sessionManager.getUserId()
    }
    private val profileRepository = ProfileRepository(api)
    private val sessionsRepository = SessionsRepository(api)
    private val securityRepository = SecurityRepository(api)
    private val updateRepository = UpdateRepository(application)
    private val updatePreferences = UpdatePreferences(application)
    private val homeContentRepository = HomeContentRepository()
    private var pendingUpdate: AppUpdateInfo? = null
    private val loginNotificationMonitor = LoginNotificationMonitor(application)
    private val messageNotificationMonitor = MessageNotificationMonitor(application)
    private var chatPresenceJob: Job? = null
    private var chatRefreshJob: Job? = null
    private var nightGuardJob: Job? = null
    private var chatWebSocketConnected = false
    private val chatWebSocketClient = ChatWebSocketClient(
        sessionManager = sessionManager,
        onEvent = { event -> handleChatWebSocketEvent(event) },
        onConnectionChanged = { connected -> chatWebSocketConnected = connected },
    )
    private var archivistTracker: ArchivistTracker? = null
    private var carouselWatcherJob: Job? = null
    private val pendingEasterCelebrations = ArrayDeque<EasterCelebrationKind>()
    private val shownEasterCelebrations = mutableSetOf<EasterCelebrationKind>()
    private var bridgeCelebrationEligible = false
    private val v010EasterHelper = V010EasterUnlockHelper(
        profileRepository = profileRepository,
        appPreferences = appPreferences,
        currentLogin = { _uiState.value.user?.login },
        currentFlags = { _uiState.value.easterFlags },
        currentLocale = { _uiState.value.locale },
        currentFontSize = { _uiState.value.chatFontSize },
        isAppLockEnabled = { _uiState.value.appLockEnabled },
        onFlagsRefreshed = { refreshEasterFlagsFromServer() },
        onUnlocked = ::celebrateEasterUnlock,
    )
    private var appLockBackgroundedAtMs: Long? = null
    private var skipAppLockOnce = false
    private var unlockedThisSession = false
    private var consumedInitialLockCheck = false

    private val _driveRestoreGoogleSignInRequest = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val driveRestoreGoogleSignInRequest: SharedFlow<Unit> = _driveRestoreGoogleSignInRequest.asSharedFlow()

    private var authSetupCompletedForUser: String? = null
    private var pendingDriveRestoreGoogleSignIn = false

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private val uiStateMutex = kotlinx.coroutines.sync.Mutex()
    private suspend fun updateUiState(transform: (AppUiState) -> AppUiState) {
        uiStateMutex.withLock {
            val old = _uiState.value
            val new = transform(old)
            if (old !== new) {
                _uiState.value = new
            }
        }
    }

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
                appLockBackgroundedAtMs = appPreferences.getAppLockBackgroundedAtMs()
                val swipeBackEnabled = appPreferences.getSwipeBackEnabled()
                val systemBackEnabled = appPreferences.getSystemBackEnabled()
                val swipeBackSensitivity = appPreferences.getSwipeBackSensitivity()
                val swipeBackEdgeWidth = appPreferences.getSwipeBackEdgeWidth()
                val rootBackBehavior = appPreferences.getRootBackBehavior()
                val encryptionReminderChatDismissed = appPreferences.getEncryptionReminderChatDismissed()
                val chatFormatToolbarHidden = appPreferences.getChatFormatToolbarHidden()
                val chatDefaultTheme = appPreferences.getChatDefaultTheme()
                val chatSendWithEnter = appPreferences.getChatSendWithEnter()
                val chatFontSize = appPreferences.getChatFontSize()
                val chatQuoteColor = appPreferences.getChatQuoteColor()
                val homeWhatsNewBannerHidden = appPreferences.getHomeWhatsNewBannerHidden()
                val chatBackupFrequency = appPreferences.getChatBackupFrequency()
                val chatBackupOverCellular = appPreferences.getChatBackupOverCellular()
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
                    appLockSettingsLoaded = true,
                    swipeBackEnabled = swipeBackEnabled,
                    systemBackEnabled = systemBackEnabled,
                    swipeBackSensitivity = swipeBackSensitivity,
                    swipeBackEdgeWidth = swipeBackEdgeWidth,
                    rootBackBehavior = rootBackBehavior,
                    encryptionReminderChatDismissed = encryptionReminderChatDismissed,
                    chatFormatToolbarHidden = chatFormatToolbarHidden,
                    chatDefaultTheme = chatDefaultTheme,
                    chatSendWithEnter = chatSendWithEnter,
                    chatFontSize = chatFontSize,
                    chatQuoteColor = chatQuoteColor,
                    homeWhatsNewBannerHidden = homeWhatsNewBannerHidden,
                    chatBackupFrequency = chatBackupFrequency,
                    chatBackupOverCellular = chatBackupOverCellular,
                    chatBackupHasStoredPassword = backupPasswordStore.hasPassword(),
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
                            val restoreStarted = maybeStartDriveRestoreOnLogin(user.id)
                            if (!restoreStarted) {
                                completeAuthSetup(user.id)
                            } else {
                                refreshHomeContent()
                                startChatWebSocket()
                            }
                        }
                    }
                }
            } catch (error: Exception) {
                android.util.Log.e("AppViewModel", "Startup failed", error)
                _uiState.value = _uiState.value.copy(screen = AppScreen.Login, user = null)
            } finally {
                checkForUpdate(force = true)
            }
        }
    }

    private fun forceLogout() {
        stopChatWebSocket()
        LoginNotificationWorker.cancel(getApplication())
        MessageNotificationWorker.cancel(getApplication())
        ChatBackupWorker.cancel(getApplication())
        PushTokenRegistrar.unregisterCurrentToken(getApplication())
        authSetupCompletedForUser = null
        viewModelScope.launch {
            appPreferences.setActiveChatFriendId(null)
            appPreferences.clearDriveRestorePromptDismissed()
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
            appLockSettingsLoaded = _uiState.value.appLockSettingsLoaded,
        )
        appLockBackgroundedAtMs = null
        skipAppLockOnce = false
        unlockedThisSession = false
        consumedInitialLockCheck = false
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
                releaseNotes = ChangelogLocalizedNotes.resolve(
                    version = info.versionName,
                    locale = _uiState.value.locale,
                    githubFallback = info.releaseNotes,
                ),
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
            refreshHomeContent()
            if (_uiState.value.detailScreen == DetailScreen.Changelog) {
                refreshChangelog()
            }
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
        _uiState.value = _uiState.value.copy(
            detailScreen = DetailScreen.Settings,
            settingsSection = org.cyblight.android.ui.settings.SettingsSection.Hub,
            settingsFocusChatBackup = false,
        )
    }

    fun setSettingsSection(section: org.cyblight.android.ui.settings.SettingsSection) {
        _uiState.value = _uiState.value.copy(settingsSection = section)
        if (section == org.cyblight.android.ui.settings.SettingsSection.Security) {
            refreshSecurityOverview()
        }
    }

    fun openChatBackup() {
        if (_uiState.value.chatFriendId != null) {
            closeChat()
        }
        _uiState.value = _uiState.value.copy(
            detailScreen = DetailScreen.Settings,
            settingsSection = org.cyblight.android.ui.settings.SettingsSection.ChatBackup,
            settingsFocusChatBackup = false,
        )
    }

    fun clearSettingsFocusChatBackup() {
        if (!_uiState.value.settingsFocusChatBackup) return
        _uiState.value = _uiState.value.copy(settingsFocusChatBackup = false)
    }

    fun setEncryptionReminderChatDismissed(dismissed: Boolean) {
        viewModelScope.launch {
            appPreferences.setEncryptionReminderChatDismissed(dismissed)
            _uiState.value = _uiState.value.copy(encryptionReminderChatDismissed = dismissed)
        }
    }

    fun dismissEncryptionReminderChat() {
        setEncryptionReminderChatDismissed(true)
    }

    fun setChatFormatToolbarHidden(hidden: Boolean) {
        viewModelScope.launch {
            appPreferences.setChatFormatToolbarHidden(hidden)
            _uiState.value = _uiState.value.copy(chatFormatToolbarHidden = hidden)
        }
    }

    fun setChatDefaultTheme(theme: ChatDefaultTheme) {
        viewModelScope.launch {
            appPreferences.setChatDefaultTheme(theme)
            _uiState.value = _uiState.value.copy(chatDefaultTheme = theme)
        }
    }

    fun setChatSendWithEnter(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setChatSendWithEnter(enabled)
            _uiState.value = _uiState.value.copy(chatSendWithEnter = enabled)
        }
    }

    fun setChatFontSize(size: ChatFontSize) {
        viewModelScope.launch {
            appPreferences.setChatFontSize(size)
            _uiState.value = _uiState.value.copy(chatFontSize = size)
        }
    }

    fun setChatQuoteColor(color: Int?) {
        viewModelScope.launch {
            appPreferences.setChatQuoteColor(color)
            _uiState.value = _uiState.value.copy(chatQuoteColor = color)
        }
    }

    fun setHomeWhatsNewBannerHidden(hidden: Boolean) {
        viewModelScope.launch {
            appPreferences.setHomeWhatsNewBannerHidden(hidden)
            _uiState.value = _uiState.value.copy(homeWhatsNewBannerHidden = hidden)
        }
    }

    fun exportChats(
        onReady: (fileName: String, json: String, chatCount: Int, messageCount: Int) -> Unit,
        onError: () -> Unit,
    ) {
        viewModelScope.launch {
            messagesRepository.exportChatsJson()
                .onSuccess { json ->
                    val login = _uiState.value.user?.login.orEmpty().ifBlank { "user" }
                    val safeLogin = login.replace(Regex("[^\\w.-]+"), "_")
                    val stamp = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                        .format(java.util.Date())
                    val fileName = "cyblight-chats-$safeLogin-$stamp.cyblight-chats.json"
                    val gson = com.google.gson.Gson()
                    val root = gson.fromJson(json, com.google.gson.JsonObject::class.java)
                    val exportElement = root.get("export")
                    val chats = exportElement?.asJsonObject?.getAsJsonArray("chats")
                    val chatCount = chats?.size() ?: 0
                    var messageCount = 0
                    chats?.forEach { chat ->
                        chat.asJsonObject.getAsJsonArray("messages")?.let { messages ->
                            messageCount += messages.size()
                        }
                    }
                    onReady(fileName, json, chatCount, messageCount)
                }
                .onFailure { onError() }
        }
    }

    fun importChats(json: String, onResult: (org.cyblight.android.data.repository.ChatsImportStats?) -> Unit) {
        viewModelScope.launch {
            messagesRepository.importChatsJson(json)
                .onSuccess { stats ->
                    refreshSocialData()
                    _uiState.value.chatFriendId?.let { friendId -> reloadChat(friendId) }
                    v010EasterHelper.onGoogleDriveRestoreSuccess() // Award Synchronist
                    onResult(stats)
                }
                .onFailure { onResult(null) }
        }
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
            val lockEnabled = enableLock || _uiState.value.appLockEnabled
            _uiState.value = _uiState.value.copy(
                appLockPinConfigured = true,
                appLockEnabled = lockEnabled,
                pendingAppLock = lockEnabled,
            )
        }
    }

    fun consumePendingAppLock(): Boolean {
        val pending = _uiState.value.pendingAppLock
        if (pending) {
            _uiState.value = _uiState.value.copy(pendingAppLock = false)
        }
        return pending
    }

    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled && !appPreferences.hasAppLockPin()) return@launch
            appPreferences.setAppLockEnabled(enabled)
            if (!enabled) {
                appLockBackgroundedAtMs = null
                appPreferences.clearAppLockBackgroundedAtMs()
            }
            _uiState.value = _uiState.value.copy(
                appLockEnabled = enabled,
                pendingAppLock = enabled,
            )
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
            unlockedThisSession = false
            val now = System.currentTimeMillis()
            appLockBackgroundedAtMs = now
            viewModelScope.launch {
                appPreferences.setAppLockBackgroundedAtMs(now)
            }
        }
    }

    fun onMainScreenBackgrounded() {
        if (_uiState.value.screen != AppScreen.Main || _uiState.value.user == null) return
        stopChatWebSocket()
        viewModelScope.launch {
            appPreferences.setActiveChatFriendId(null)
        }
        MessageNotificationWorker.scheduleImmediate(getApplication())
    }

    fun onAppUnlocked() {
        appLockBackgroundedAtMs = null
        unlockedThisSession = true
        viewModelScope.launch {
            appPreferences.clearAppLockBackgroundedAtMs()
        }
    }

    fun shouldShowAppLockOnLaunch(): Boolean {
        if (consumedInitialLockCheck) return false
        consumedInitialLockCheck = true
        return shouldRequireAppLock(treatAsFreshLaunch = true)
    }

    fun shouldShowAppLockOnResume(): Boolean =
        shouldRequireAppLock(treatAsFreshLaunch = false)

    private fun shouldRequireAppLock(treatAsFreshLaunch: Boolean): Boolean {
        val state = _uiState.value
        if (!state.appLockEnabled || !state.appLockPinConfigured || !state.appLockSettingsLoaded) {
            return false
        }
        if (skipAppLockOnce) {
            skipAppLockOnce = false
            unlockedThisSession = true
            return false
        }

        val backgroundedAt = appLockBackgroundedAtMs
        if (backgroundedAt == null) {
            return treatAsFreshLaunch && !unlockedThisSession
        }

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
        if (_uiState.value.screen != AppScreen.Main || _uiState.value.user == null) return
        v010EasterHelper.markOpenedChatFromNotification()
        openChat(friendId, friendName.ifBlank { friendId })
        viewModelScope.launch {
            v010EasterHelper.onOpenedChatFromNotificationHandled()
        }
    }

    fun onDraftFormattedViaMenu() {
        v010EasterHelper.markDraftFormattedViaMenu()
    }

    fun onSpoilerRevealed() {
        viewModelScope.launch { v010EasterHelper.onSpoilerRevealed() }
    }

    fun onGoogleDriveAccountPickerInteraction() {
        viewModelScope.launch { v010EasterHelper.onGoogleAccountPicked() }
    }

    fun trackHomeCarouselSeconds(seconds: Int) {
        viewModelScope.launch { v010EasterHelper.addCarouselWatchSeconds(seconds) }
    }

    fun openSessionsFromNotification() {
        if (_uiState.value.screen != AppScreen.Main || _uiState.value.user == null) return
        if (_uiState.value.chatFriendId != null) {
            closeChat()
        }
        clearOverlayNavigation()
        openSessions()
    }

    private fun clearOverlayNavigation() {
        _uiState.value = _uiState.value.copy(
            detailScreen = DetailScreen.None,
            settingsSection = org.cyblight.android.ui.settings.SettingsSection.Hub,
            settingsReturnAfterDetail = false,
            settingsFocusChatBackup = false,
            helpReturnToSettings = false,
            profileUsername = null,
            profile = null,
            profileError = null,
            isProfileLoading = false,
            showLightCatcherGame = false,
        )
    }

    private fun onAuthenticated() {
        skipAppLockOnce = true
        unlockedThisSession = true
        consumedInitialLockCheck = false
        viewModelScope.launch {
            val userId = _uiState.value.user?.id ?: return@launch
            val restoreStarted = maybeStartDriveRestoreOnLogin(userId)
            if (!restoreStarted) {
                completeAuthSetup(userId)
            } else {
                refreshHomeContent()
            }
        }
        checkForUpdate(force = true)
    }

    private suspend fun completeAuthSetup(userId: String) {
        if (authSetupCompletedForUser == userId) return
        authSetupCompletedForUser = userId
        runCatching { messagesRepository.ensureSignalKeys(userId) }
            .onFailure { error ->
                android.util.Log.e("SignalCrypto", "Key registration failed", error)
            }
        loginNotificationMonitor.markOwnLoginGracePeriod()
        messageNotificationMonitor.syncBaselineFromServer()
        LoginNotificationWorker.schedule(getApplication())
        MessageNotificationWorker.schedule(getApplication())
        syncChatBackupScheduling()
        PushTokenRegistrar.registerCurrentToken(getApplication())
        startChatWebSocket()
        refreshSocialData()
        refreshHomeContent()
    }

    private fun startChatWebSocket() {
        chatWebSocketClient.start(viewModelScope)
    }

    private fun stopChatWebSocket() {
        chatWebSocketClient.stop()
        chatWebSocketConnected = false
    }

    private fun handleChatWebSocketEvent(event: ChatWsEvent) {
        when (event.type) {
            "message.new" -> {
                val openChatId = _uiState.value.chatFriendId
                viewModelScope.launch {
                    if (openChatId != null && (event.peerId == openChatId || event.senderId == openChatId)) {
                        reloadChat(openChatId)
                        v010EasterHelper.onWebSocketMessageReceived()
                    } else {
                        refreshConversationsOnly()
                    }
                    delay(500)
                    refreshEasterFlagsFromServer()
                }
            }
            "message.deleted" -> {
                val openChatId = _uiState.value.chatFriendId
                if (openChatId != null && (event.peerId == openChatId || event.senderId == openChatId)) {
                    removeChatMessagesWithAnimation(setOf(event.messageId))
                } else {
                    refreshConversationsOnly()
                }
            }
            "message.edited" -> {
                val openChatId = _uiState.value.chatFriendId
                if (openChatId != null && (event.peerId == openChatId || event.senderId == openChatId)) {
                    viewModelScope.launch {
                        reloadChat(openChatId, refreshSocial = false)
                        delay(500)
                        refreshEasterFlagsFromServer()
                    }
                } else {
                    refreshConversationsOnly()
                    viewModelScope.launch {
                        delay(500)
                        refreshEasterFlagsFromServer()
                    }
                }
            }
        }
    }

    private suspend fun maybeStartDriveRestoreOnLogin(userId: String): Boolean {
        if (!GoogleDriveConfig.isConfigured()) return false
        if (appPreferences.isDriveRestorePromptDismissed(userId)) return false
        if (backupManager.hasLocalBackupKeys(userId)) return false

        if (!googleDriveBackupService.hasSession()) {
            pendingDriveRestoreGoogleSignIn = true
            _driveRestoreGoogleSignInRequest.tryEmit(Unit)
            return true
        }
        return presentDriveRestorePromptIfNeeded(userId)
    }

    private suspend fun presentDriveRestorePromptIfNeeded(userId: String): Boolean {
        val metadata = runCatching { googleDriveBackupService.fetchMetadata(userId) }.getOrNull()
            ?: return false
        refreshGoogleDriveStatusSync()
        _uiState.value = _uiState.value.copy(
            driveRestoreConfirmMetadata = metadata,
            driveRestorePasswordOpen = false,
            driveRestoreBusy = false,
        )
        return true
    }

    private suspend fun continueDriveRestoreAfterGoogleSignIn() {
        val userId = _uiState.value.user?.id ?: return
        if (backupManager.hasLocalBackupKeys(userId)) {
            completeAuthSetup(userId)
            return
        }
        if (!presentDriveRestorePromptIfNeeded(userId)) {
            completeAuthSetup(userId)
        }
    }

    fun onDriveRestoreGoogleSignInCancelled() {
        viewModelScope.launch {
            val userId = _uiState.value.user?.id ?: return@launch
            appPreferences.setDriveRestorePromptDismissed(userId)
            clearDriveRestoreUi()
            completeAuthSetup(userId)
        }
    }

    fun skipDriveRestore() {
        viewModelScope.launch {
            val userId = _uiState.value.user?.id ?: return@launch
            appPreferences.setDriveRestorePromptDismissed(userId)
            clearDriveRestoreUi()
            completeAuthSetup(userId)
        }
    }

    fun beginDriveRestore() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(driveRestoreConfirmMetadata = null)
            val storedPassword = backupPasswordStore.getPassword()
            if (!storedPassword.isNullOrBlank()) {
                submitDriveRestorePassword(storedPassword, fromStoredPassword = true)
                if (_uiState.value.driveRestoreConfirmMetadata == null &&
                    !_uiState.value.driveRestorePasswordOpen &&
                    !_uiState.value.driveRestoreBusy
                ) {
                    return@launch
                }
            }
            _uiState.value = _uiState.value.copy(driveRestorePasswordOpen = true)
        }
    }

    fun dismissDriveRestorePasswordDialog() {
        if (_uiState.value.driveRestoreBusy) return
        _uiState.value = _uiState.value.copy(driveRestorePasswordOpen = false)
    }

    fun submitDriveRestorePassword(
        password: String,
        fromStoredPassword: Boolean = false,
    ) {
        viewModelScope.launch {
            if (password.isBlank()) return@launch
            _uiState.value = _uiState.value.copy(
                driveRestoreBusy = true,
                driveRestoreProgress = 0,
                driveRestorePasswordOpen = false,
            )
            restoreGoogleDriveBackup(password) { value, key ->
                _uiState.value = _uiState.value.copy(
                    driveRestoreProgress = value,
                    driveRestoreProgressKey = key,
                )
            }.onSuccess { stats ->
                val userId = _uiState.value.user?.id
                if (userId != null) {
                    appPreferences.clearDriveRestorePromptDismissed()
                    appPreferences.setLastAutoBackupSuccessMs(System.currentTimeMillis())
                    authSetupCompletedForUser = null
                }
                v010EasterHelper.onGoogleDriveRestoreSuccess()
                clearDriveRestoreUi()
                userId?.let { completeAuthSetup(it) }
                refreshGoogleDriveStatusSync()
                _uiState.value = _uiState.value.copy(
                    driveRestoreToast = buildDriveRestoreSuccessMessage(stats),
                )
            }.onFailure { error ->
                if (fromStoredPassword) {
                    _uiState.value = _uiState.value.copy(
                        driveRestoreBusy = false,
                        driveRestoreProgress = 0,
                        driveRestoreProgressKey = null,
                        driveRestorePasswordOpen = true,
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        driveRestoreBusy = false,
                        driveRestoreProgress = 0,
                        driveRestoreProgressKey = null,
                        driveRestoreToast = signalBackupErrorMessage(error),
                        driveRestoreToastIsError = true,
                    )
                }
            }
        }
    }

    fun consumeDriveRestoreToast(): Pair<String, Boolean>? {
        val message = _uiState.value.driveRestoreToast ?: return null
        val isError = _uiState.value.driveRestoreToastIsError
        _uiState.value = _uiState.value.copy(
            driveRestoreToast = null,
            driveRestoreToastIsError = false,
        )
        return message to isError
    }

    private fun clearDriveRestoreUi() {
        _uiState.value = _uiState.value.copy(
            driveRestoreConfirmMetadata = null,
            driveRestorePasswordOpen = false,
            driveRestoreBusy = false,
            driveRestoreProgress = 0,
            driveRestoreProgressKey = null,
        )
    }

    private fun buildDriveRestoreSuccessMessage(
        stats: org.cyblight.android.crypto.backup.BackupRestoreStats,
    ): String {
        val context = getApplication<Application>()
        val base = context.getString(R.string.settings_google_drive_restore_done)
        if (stats.chatsImported + stats.chatsSkipped + stats.chatsErrors <= 0) return base
        return "$base ${context.getString(
            R.string.settings_google_drive_restore_chats_stats,
            stats.chatsImported,
            stats.chatsSkipped,
        )}"
    }

    fun driveRestoreProgressLabel(key: String?): String? {
        if (key.isNullOrBlank()) return null
        val context = getApplication<Application>()
        return when (key) {
            "progress_auth" -> context.getString(R.string.settings_google_drive_progress_auth)
            "progress_find" -> context.getString(R.string.settings_google_drive_progress_find)
            "progress_download" -> context.getString(R.string.settings_google_drive_progress_download)
            "progress_restore" -> context.getString(R.string.settings_google_drive_progress_restore)
            "progress_chats" -> context.getString(R.string.settings_google_drive_progress_chats)
            "progress_done" -> context.getString(R.string.settings_google_drive_progress_done)
            else -> context.getString(R.string.settings_google_drive_progress_restore)
        }
    }

    fun openSecurityCheck() {
        val returnToSettings = _uiState.value.detailScreen == DetailScreen.Settings
        _uiState.value = _uiState.value.copy(
            detailScreen = DetailScreen.SecurityCheck,
            settingsReturnAfterDetail = returnToSettings,
        )
        forceRefreshSecurityOverview()
    }

    fun openAccountSecurity(context: Context) {
        val locale = _uiState.value.locale
        ExternalLinks.openUrl(context, "${BuildConfig.LOGIN_URL}/$locale/account-security/")
    }

    fun openSignup(context: Context) {
        val locale = _uiState.value.locale
        ExternalLinks.openUrl(context, "${BuildConfig.LOGIN_URL}/$locale/signup")
    }

    fun openPasskeys() {
        val returnToSettings = _uiState.value.detailScreen == DetailScreen.Settings
        _uiState.value = _uiState.value.copy(
            detailScreen = DetailScreen.Passkeys,
            settingsReturnAfterDetail = returnToSettings,
            passkeysError = null,
            isPasskeysLoading = true,
        )
        loadPasskeys()
    }

    fun openTrustedDevices() {
        val returnToSettings = _uiState.value.detailScreen == DetailScreen.Settings
        _uiState.value = _uiState.value.copy(
            detailScreen = DetailScreen.TrustedDevices,
            settingsReturnAfterDetail = returnToSettings,
            trustedDevicesError = null,
            isTrustedDevicesLoading = true,
        )
        loadTrustedDevices()
    }

    fun openLoginHistory() {
        val returnToSettings = _uiState.value.detailScreen == DetailScreen.Settings
        _uiState.value = _uiState.value.copy(
            detailScreen = DetailScreen.LoginHistory,
            settingsReturnAfterDetail = returnToSettings,
            loginHistoryError = null,
            isLoginHistoryLoading = true,
        )
        loadLoginHistory()
    }

    fun openSessions() {
        val returnToSettings = _uiState.value.detailScreen == DetailScreen.Settings
        _uiState.value = _uiState.value.copy(
            detailScreen = DetailScreen.Sessions,
            settingsReturnAfterDetail = returnToSettings,
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
        refreshEasterProgressInState()
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
            DetailScreen.Changelog -> {
                _uiState.value = _uiState.value.copy(
                    detailScreen = DetailScreen.None,
                    changelogError = null,
                )
            }
            DetailScreen.Sessions -> {
                val returnToSettings = _uiState.value.settingsReturnAfterDetail
                _uiState.value = _uiState.value.copy(
                    detailScreen = if (returnToSettings) DetailScreen.Settings else DetailScreen.None,
                    settingsReturnAfterDetail = false,
                    settingsSection = if (returnToSettings) {
                        org.cyblight.android.ui.settings.SettingsSection.Security
                    } else {
                        _uiState.value.settingsSection
                    },
                    sessionsError = null,
                )
            }
            DetailScreen.SecurityCheck,
            DetailScreen.LoginHistory,
            DetailScreen.TrustedDevices,
            DetailScreen.Passkeys,
            -> {
                val returnToSettings = _uiState.value.settingsReturnAfterDetail
                _uiState.value = _uiState.value.copy(
                    detailScreen = if (returnToSettings) DetailScreen.Settings else DetailScreen.None,
                    settingsReturnAfterDetail = false,
                    settingsSection = if (returnToSettings) {
                        org.cyblight.android.ui.settings.SettingsSection.Security
                    } else {
                        _uiState.value.settingsSection
                    },
                    passkeysError = null,
                    passkeyRegisterError = null,
                    passkeyDeleteError = null,
                    trustedDevicesError = null,
                    loginHistoryError = null,
                )
            }
            DetailScreen.Settings -> {
                if (_uiState.value.settingsSection != org.cyblight.android.ui.settings.SettingsSection.Hub) {
                    _uiState.value = _uiState.value.copy(
                        settingsSection = org.cyblight.android.ui.settings.SettingsSection.Hub,
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        detailScreen = DetailScreen.None,
                        settingsSection = org.cyblight.android.ui.settings.SettingsSection.Hub,
                        settingsFocusChatBackup = false,
                    )
                }
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

    fun selectMainTab(tab: MainTab) {
        _uiState.value = _uiState.value.copy(selectedMainTab = tab)
        when (tab) {
            MainTab.Home -> refreshHomeContent()
            MainTab.Easter -> refreshEasterFlags()
            else -> Unit
        }
    }

    fun setFriendsSubTab(tab: Int) {
        _uiState.value = _uiState.value.copy(friendsSubTab = tab.coerceIn(0, 2))
    }

    fun refreshHomeContent() {
        viewModelScope.launch {
            val locale = _uiState.value.locale
            _uiState.value = _uiState.value.copy(isHomeLoading = true, homeError = null)
            homeContentRepository.loadHomeContent(getApplication(), locale)
                .onSuccess { content ->
                    _uiState.value = _uiState.value.copy(
                        homeContent = content,
                        isHomeLoading = false,
                        homeError = null,
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isHomeLoading = false,
                        homeError = "home_load_failed",
                    )
                }
        }
    }

    fun openChangelog() {
        _uiState.value = _uiState.value.copy(
            detailScreen = DetailScreen.Changelog,
            changelogError = null,
            isChangelogLoading = true,
        )
        refreshChangelog()
    }

    fun refreshChangelog() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isChangelogLoading = true, changelogError = null)
            homeContentRepository.loadChangelog(_uiState.value.locale)
                .onSuccess { releases ->
                    _uiState.value = _uiState.value.copy(
                        changelogReleases = releases,
                        isChangelogLoading = false,
                        changelogError = null,
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isChangelogLoading = false,
                        changelogError = "changelog_load_failed",
                    )
                }
        }
    }

    fun openWebsiteUrl(context: Context, url: String) {
        ExternalLinks.openUrl(context, url)
    }

    sealed class BackAction {
        data object Handled : BackAction()
        data object ExitApp : BackAction()
        data object MinimizeApp : BackAction()
        data object NotHandled : BackAction()
    }

    fun handleBackNavigation(): BackAction {
        val state = _uiState.value

        if (state.detailScreen != DetailScreen.None) {
            navigateBack()
            return BackAction.Handled
        }

        if (!state.chatFriendId.isNullOrBlank()) {
            closeChat()
            return BackAction.Handled
        }

        if (state.friendsSubTab != 0) {
            setFriendsSubTab(0)
            return BackAction.Handled
        }

        if (state.selectedMainTab != MainTab.Home) {
            if (state.rootBackBehavior == RootBackBehavior.EXIT_IMMEDIATELY) {
                return BackAction.ExitApp
            }
            selectMainTab(MainTab.Home)
            return BackAction.Handled
        }

        return when (state.rootBackBehavior) {
            RootBackBehavior.EXIT_IMMEDIATELY -> BackAction.ExitApp
            RootBackBehavior.MINIMIZE -> BackAction.MinimizeApp
            RootBackBehavior.HOME_THEN_EXIT -> BackAction.ExitApp
        }
    }

    fun setSwipeBackEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setSwipeBackEnabled(enabled)
            _uiState.value = _uiState.value.copy(swipeBackEnabled = enabled)
        }
    }

    fun setSystemBackEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setSystemBackEnabled(enabled)
            _uiState.value = _uiState.value.copy(systemBackEnabled = enabled)
        }
    }

    fun setSwipeBackSensitivity(sensitivity: SwipeBackSensitivity) {
        viewModelScope.launch {
            appPreferences.setSwipeBackSensitivity(sensitivity)
            _uiState.value = _uiState.value.copy(swipeBackSensitivity = sensitivity)
        }
    }

    fun setSwipeBackEdgeWidth(edgeWidth: SwipeBackEdgeWidth) {
        viewModelScope.launch {
            appPreferences.setSwipeBackEdgeWidth(edgeWidth)
            _uiState.value = _uiState.value.copy(swipeBackEdgeWidth = edgeWidth)
        }
    }

    fun setRootBackBehavior(behavior: RootBackBehavior) {
        viewModelScope.launch {
            appPreferences.setRootBackBehavior(behavior)
            _uiState.value = _uiState.value.copy(rootBackBehavior = behavior)
        }
    }

    suspend fun createSignalBackup(password: String): Result<String> = runCatching {
        val userId = sessionManager.getUserId().orEmpty()
        if (userId.isBlank()) throw IllegalArgumentException("backup_user_missing")
        val chats = messagesRepository.fetchChatsExportPayload()
        backupManager.createBackupFile(userId, password, chats)
    }

    suspend fun restoreSignalBackup(
        content: String,
        password: String,
        onProgress: (Int, String) -> Unit = { _, _ -> },
    ): Result<Unit> = runCatching {
        val userId = sessionManager.getUserId().orEmpty()
        if (userId.isBlank()) throw IllegalArgumentException("backup_user_missing")
        
        onProgress(10, "progress_restore")
        val payload = backupManager.decryptBackupPayload(content, password)
        
        backupManager.restorePayload(userId, payload, onProgress)
        signalCrypto.invalidateUserCache(userId)
        authSetupCompletedForUser = null
        
        v010EasterHelper.onGoogleDriveRestoreSuccess()
        
        val chats = payload.chats
        if (payload.version == org.cyblight.android.crypto.backup.BACKUP_PAYLOAD_VERSION_V2 && chats != null) {
            onProgress(60, "progress_chats")
            messagesRepository.importChatsPayload(chats).getOrElse {
                throw IllegalStateException("chats_import_failed")
            }
        }
        
        payload.chats = null // Clear memory
        
        onProgress(95, "progress_restore")
        if (content.length > 1024 * 1024) {
            System.gc()
        }
        onProgress(100, "progress_done")
    }

    fun signalBackupErrorMessage(code: String): String = backupManager.errorMessage(code)

    fun signalBackupErrorMessage(error: Throwable): String =
        signalBackupErrorMessage(backupErrorCode(error))

    private fun backupErrorCode(error: Throwable): String {
        var current: Throwable? = error
        while (current != null) {
            val message = current.message?.substringBefore(':')?.trim()
            if (!message.isNullOrBlank()) {
                if (message.startsWith("google_drive_") ||
                    message.startsWith("backup_") ||
                    message.startsWith("import_") ||
                    message == "chats_import_failed" ||
                    message == "invalid_export_format" ||
                    message == "sync_key_invalid" ||
                    message == "signal_store_missing" ||
                    message == "signal_user_not_registered" ||
                    message == "signal_invalid_identity_key"
                ) {
                    return message
                }
            }
            current = current.cause
        }
        android.util.Log.e("AppViewModel", "Backup error trace", error)
        return error.message?.take(100) ?: "backup_failed"
    }

    fun isGoogleDriveConfigured(): Boolean = googleDriveBackupService.isConfigured()

    fun getGoogleDriveSignInIntent(): Intent = googleDriveBackupService.getSignInIntent()

    suspend fun prepareGoogleDriveSignInIntent(
        preferredEmail: String? = null,
        forceAccountPicker: Boolean = false,
    ): Intent {
        val email = preferredEmail?.trim()?.takeIf { it.isNotEmpty() }
        when {
            forceAccountPicker -> googleDriveBackupService.signOut()
            email != null -> googleDriveBackupService.signOutIfCurrentNot(email)
        }
        return googleDriveBackupService.getSignInIntent(email)
    }

    fun getGoogleDriveAccountEmails(): List<String> = googleDriveBackupService.getDeviceGoogleAccountEmails()

    fun handleGoogleDriveSignInResult(data: Intent?) {
        viewModelScope.launch {
            if (data == null) {
                if (pendingDriveRestoreGoogleSignIn) {
                    pendingDriveRestoreGoogleSignIn = false
                    onDriveRestoreGoogleSignInCancelled()
                }
                return@launch
            }

            val userId = _uiState.value.user?.id
            val shouldContinueRestore = pendingDriveRestoreGoogleSignIn ||
                (userId != null &&
                    !backupManager.hasLocalBackupKeys(userId) &&
                    !appPreferences.isDriveRestorePromptDismissed(userId))

            googleDriveBackupService.handleSignInResult(data)
                .onSuccess {
                    pendingDriveRestoreGoogleSignIn = false
                    refreshGoogleDriveStatusSync()
                    syncChatBackupScheduling()
                    if (shouldContinueRestore) {
                        continueDriveRestoreAfterGoogleSignIn()
                    }
                }
                .onFailure {
                    pendingDriveRestoreGoogleSignIn = false
                    if (shouldContinueRestore) {
                        onDriveRestoreGoogleSignInCancelled()
                    }
                }
        }
    }

    fun refreshGoogleDriveStatus() {
        viewModelScope.launch {
            refreshGoogleDriveStatusSync()
        }
    }

    suspend fun refreshGoogleDriveStatusSync() {
        val label = if (googleDriveBackupService.hasSession()) {
            googleDriveBackupService.getAccountLabel()
        } else {
            null
        }
        val accountEmail = googleDriveBackupService.getAccountEmail()
        val accountEmails = googleDriveBackupService.getDeviceGoogleAccountEmails()
        val userId = sessionManager.getUserId().orEmpty()
        val metadata: DriveBackupMetadata? = if (userId.isNotBlank() && label != null) {
            runCatching { googleDriveBackupService.fetchMetadata(userId) }.getOrNull()
        } else {
            null
        }
        val storageQuota: GoogleDriveStorageQuota? = if (label != null) {
            googleDriveBackupService.fetchStorageQuota()
        } else {
            null
        }
        _uiState.value = _uiState.value.copy(
            googleDriveAccountLabel = label,
            googleDriveAccountEmail = accountEmail,
            googleDriveAccountEmails = accountEmails,
            googleDriveBackupMetadata = metadata,
            googleDriveStorageQuota = storageQuota,
        )
    }

    fun openGoogleStorageManagement(context: Context) {
        ExternalLinks.openUrl(context, GoogleDriveConfig.GOOGLE_STORAGE_MANAGEMENT_URL)
    }

    suspend fun uploadGoogleDriveBackup(
        password: String? = null,
        onProgress: (Int, String) -> Unit,
    ): Result<Unit> = runCatching {
        val userId = sessionManager.getUserId().orEmpty()
        val login = _uiState.value.user?.login.orEmpty().ifBlank { "user" }
        if (userId.isBlank()) throw IllegalArgumentException("backup_user_missing")
        val resolvedPassword = password?.trim()?.takeIf { it.isNotEmpty() }
            ?: backupPasswordStore.getPassword()
            ?: throw IllegalArgumentException("backup_password_required")
        googleDriveBackupService.uploadBackup(userId, login, resolvedPassword, onProgress)
        appPreferences.setLastAutoBackupSuccessMs(System.currentTimeMillis())
        v010EasterHelper.onGoogleDriveBackupSuccess()
        backupPasswordStore.savePassword(resolvedPassword)
        _uiState.value = _uiState.value.copy(chatBackupHasStoredPassword = true)
        Unit
    }

    fun setChatBackupOverCellular(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setChatBackupOverCellular(enabled)
            _uiState.value = _uiState.value.copy(chatBackupOverCellular = enabled)
            syncChatBackupScheduling()
        }
    }

    fun setChatBackupFrequency(frequency: ChatBackupFrequency) {
        viewModelScope.launch {
            runCatching { setChatBackupFrequencyInternal(frequency) }
        }
    }

    suspend fun enableAutoBackup(frequency: ChatBackupFrequency, password: String): Result<Unit> = runCatching {
        if (frequency == ChatBackupFrequency.OFF) throw IllegalArgumentException("invalid_frequency")
        if (password.length < 8) throw IllegalArgumentException("backup_password_short")
        backupPasswordStore.savePassword(password)
        setChatBackupFrequencyInternal(frequency)
    }

    private suspend fun setChatBackupFrequencyInternal(frequency: ChatBackupFrequency) {
        if (frequency != ChatBackupFrequency.OFF && !backupPasswordStore.hasPassword()) {
            throw IllegalStateException("backup_password_required")
        }
        appPreferences.setChatBackupFrequency(frequency)
        if (frequency == ChatBackupFrequency.OFF) {
            ChatBackupWorker.cancel(getApplication())
        } else {
            syncChatBackupScheduling(frequency, appPreferences.getChatBackupOverCellular())
        }
        _uiState.value = _uiState.value.copy(
            chatBackupFrequency = frequency,
            chatBackupHasStoredPassword = backupPasswordStore.hasPassword(),
        )
    }

    private suspend fun syncChatBackupScheduling() {
        val frequency = appPreferences.getChatBackupFrequency()
        val overCellular = appPreferences.getChatBackupOverCellular()
        ChatBackupWorker.schedule(getApplication(), frequency, overCellular)
    }

    private suspend fun syncChatBackupScheduling(frequency: ChatBackupFrequency, overCellular: Boolean) {
        ChatBackupWorker.schedule(getApplication(), frequency, overCellular)
    }

    suspend fun restoreGoogleDriveBackup(
        password: String? = null,
        onProgress: (Int, String) -> Unit,
    ): Result<org.cyblight.android.crypto.backup.BackupRestoreStats> = runCatching {
        val userId = sessionManager.getUserId().orEmpty()
        if (userId.isBlank()) throw IllegalArgumentException("backup_user_missing")
        val resolvedPassword = password?.trim()?.takeIf { it.isNotEmpty() }
            ?: backupPasswordStore.getPassword()
            ?: throw IllegalArgumentException("backup_password_required")
        val stats = googleDriveBackupService.restoreBackup(userId, resolvedPassword, onProgress)
        signalCrypto.invalidateUserCache(userId)
        authSetupCompletedForUser = null
        backupPasswordStore.savePassword(resolvedPassword)
        
        v010EasterHelper.onGoogleDriveRestoreSuccess()

        _uiState.value = _uiState.value.copy(chatBackupHasStoredPassword = true)
        stats
    }

    suspend fun changeBackupPassword(currentPassword: String, newPassword: String): Result<Unit> = runCatching {
        val stored = backupPasswordStore.getPassword()
            ?: throw IllegalArgumentException("backup_password_required")
        if (!stored.contentEquals(currentPassword)) {
            throw IllegalArgumentException("backup_password_current_invalid")
        }
        if (newPassword.length < 8) throw IllegalArgumentException("backup_password_short")
        if (newPassword.contentEquals(stored)) {
            throw IllegalArgumentException("backup_password_unchanged")
        }
        backupPasswordStore.savePassword(newPassword)
        _uiState.value = _uiState.value.copy(chatBackupHasStoredPassword = true)
    }

    suspend fun saveBackupPassword(password: String): Result<Unit> = runCatching {
        if (password.length < 8) throw IllegalArgumentException("backup_password_short")
        backupPasswordStore.savePassword(password)
        _uiState.value = _uiState.value.copy(chatBackupHasStoredPassword = true)
    }

    suspend fun clearStoredBackupPassword(): Result<Unit> = runCatching {
        backupPasswordStore.clearPassword()
        _uiState.value = _uiState.value.copy(chatBackupHasStoredPassword = false)
    }

    suspend fun disableBackupPassword(): Result<Unit> = runCatching {
        if (_uiState.value.chatBackupFrequency != ChatBackupFrequency.OFF) {
            appPreferences.setChatBackupFrequency(ChatBackupFrequency.OFF)
            ChatBackupWorker.cancel(getApplication())
        }
        backupPasswordStore.clearPassword()
        _uiState.value = _uiState.value.copy(
            chatBackupFrequency = ChatBackupFrequency.OFF,
            chatBackupHasStoredPassword = false,
        )
    }

    suspend fun deleteGoogleDriveBackup(): Result<Boolean> = runCatching {
        val userId = sessionManager.getUserId().orEmpty()
        if (userId.isBlank()) throw IllegalArgumentException("backup_user_missing")
        googleDriveBackupService.deleteBackup(userId)
    }

    suspend fun signOutGoogleDrive() {
        googleDriveBackupService.signOut()
        _uiState.value = _uiState.value.copy(
            googleDriveAccountLabel = null,
            googleDriveBackupMetadata = null,
            googleDriveStorageQuota = null,
        )
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
                    _uiState.value.user?.login?.let { login ->
                        EasterLogger.syncLoggedFromServerFlags(appPreferences, login, flags)
                    }
                    appPreferences.syncV010EasterProgressWithServerFlags(flags)
                    syncShownEasterCelebrations(flags)
                    _uiState.value = _uiState.value.copy(
                        easterFlags = flags,
                        isEasterLoading = false,
                        easterProgress = buildEasterProgress(flags),
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

    private fun celebrateEasterUnlock(kind: EasterCelebrationKind) {
        viewModelScope.launch {
            if (isEasterAlreadyUnlocked(kind)) return@launch
            if (!shownEasterCelebrations.add(kind)) return@launch
            markEasterFlagLocallyUnlocked(kind)
            if (kind != EasterCelebrationKind.BRIDGE) {
                bridgeCelebrationEligible = true
            }
            updateUiState { state ->
                if (state.easterCelebration == null) {
                    state.copy(easterCelebration = kind)
                } else if (state.easterCelebration != kind) {
                    pendingEasterCelebrations.addLast(kind)
                    state
                } else {
                    state
                }
            }
        }
    }

    private suspend fun markEasterFlagLocallyUnlocked(kind: EasterCelebrationKind) {
        updateUiState { state ->
            val flags = state.easterFlags ?: return@updateUiState state
            val updated = when (kind) {
                EasterCelebrationKind.LIGHT_CATCHER -> flags.copy(lightCatcher = true)
                EasterCelebrationKind.NIGHT_GUARD -> flags.copy(nightGuard = true)
                EasterCelebrationKind.TRUSTED_FINGERPRINT -> flags.copy(trustedFingerprint = true)
                EasterCelebrationKind.ECHO -> flags.copy(echo = true)
                EasterCelebrationKind.ARCHIVIST -> flags.copy(archivist = true)
                EasterCelebrationKind.BRIDGE -> flags.copy(bridge = true)
                EasterCelebrationKind.FORMAT_MIRROR -> flags.copy(formatMirror = true)
                EasterCelebrationKind.TYPOGRAPHER -> flags.copy(typographer = true)
                EasterCelebrationKind.SPOILER_HUNTER -> flags.copy(spoilerHunter = true)
                EasterCelebrationKind.NO_MARKERS -> flags.copy(noMarkers = true)
                EasterCelebrationKind.ENTER_MASTER -> flags.copy(enterMaster = true)
                EasterCelebrationKind.FONT_EXTREMES -> flags.copy(fontExtremes = true)
                EasterCelebrationKind.CLOUD_KEEPER -> flags.copy(cloudKeeper = true)
                EasterCelebrationKind.DRIVE_PILOT -> flags.copy(drivePilot = true)
                EasterCelebrationKind.LIVE_WIRE -> flags.copy(liveWire = true)
                EasterCelebrationKind.FROM_SHADOW -> flags.copy(fromShadow = true)
                EasterCelebrationKind.WATCHMAN -> flags.copy(watchman = true)
                EasterCelebrationKind.CAROUSEL_WATCHER -> flags.copy(carouselWatcher = true)
                EasterCelebrationKind.SYNCHRONIST -> flags.copy(synchronist = true)
                EasterCelebrationKind.QUOTE_DAY -> flags.copy(quoteDay = true)
                EasterCelebrationKind.MIDNIGHT_EDITOR -> flags.copy(midnightEditor = true)
                EasterCelebrationKind.POLYGLOT_FRIEND -> flags.copy(polyglotFriend = true)
                EasterCelebrationKind.SILENCE -> flags.copy(silence = true)
                EasterCelebrationKind.REACTION_STREAK -> flags.copy(reactionStreak = true)
            }
            state.copy(easterFlags = updated)
        }
    }

    private fun isEasterAlreadyUnlocked(kind: EasterCelebrationKind): Boolean {
        if (shownEasterCelebrations.contains(kind)) return true
        val flags = _uiState.value.easterFlags ?: return false
        return when (kind) {
            EasterCelebrationKind.LIGHT_CATCHER -> flags.lightCatcher
            EasterCelebrationKind.NIGHT_GUARD -> flags.nightGuard
            EasterCelebrationKind.TRUSTED_FINGERPRINT -> flags.trustedFingerprint
            EasterCelebrationKind.ECHO -> flags.echo
            EasterCelebrationKind.ARCHIVIST -> flags.archivist
            EasterCelebrationKind.BRIDGE -> flags.bridge
            EasterCelebrationKind.FORMAT_MIRROR -> flags.formatMirror
            EasterCelebrationKind.TYPOGRAPHER -> flags.typographer
            EasterCelebrationKind.SPOILER_HUNTER -> flags.spoilerHunter
            EasterCelebrationKind.NO_MARKERS -> flags.noMarkers
            EasterCelebrationKind.ENTER_MASTER -> flags.enterMaster
            EasterCelebrationKind.FONT_EXTREMES -> flags.fontExtremes
            EasterCelebrationKind.CLOUD_KEEPER -> flags.cloudKeeper
            EasterCelebrationKind.DRIVE_PILOT -> flags.drivePilot
            EasterCelebrationKind.LIVE_WIRE -> flags.liveWire
            EasterCelebrationKind.FROM_SHADOW -> flags.fromShadow
            EasterCelebrationKind.WATCHMAN -> flags.watchman
            EasterCelebrationKind.CAROUSEL_WATCHER -> flags.carouselWatcher
            EasterCelebrationKind.SYNCHRONIST -> flags.synchronist
            EasterCelebrationKind.QUOTE_DAY -> flags.quoteDay
            EasterCelebrationKind.MIDNIGHT_EDITOR -> flags.midnightEditor
            EasterCelebrationKind.POLYGLOT_FRIEND -> flags.polyglotFriend
            EasterCelebrationKind.SILENCE -> flags.silence
            EasterCelebrationKind.REACTION_STREAK -> flags.reactionStreak
        }
    }

    fun dismissEasterCelebration() {
        viewModelScope.launch {
            val next = pendingEasterCelebrations.pollFirst()
            updateUiState { it.copy(easterCelebration = next) }
        }
    }

    fun openEasterTabFromCelebration() {
        dismissEasterCelebration()
        if (_uiState.value.chatFriendId != null) {
            closeChat()
        }
        clearOverlayNavigation()
        selectMainTab(MainTab.Easter)
    }

    fun onLightCatcherGameWon() {
        if (isEasterAlreadyUnlocked(EasterCelebrationKind.LIGHT_CATCHER)) {
            dismissLightCatcherGame()
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(lightCatcherUnlocking = true)
            profileRepository.unlockLightCatcher()
                .onSuccess {
                    _uiState.value.user?.login?.let { login ->
                        EasterLogger.logLightCatcher(appPreferences, login)
                    }
                    _uiState.value = _uiState.value.copy(
                        showLightCatcherGame = false,
                        lightCatcherUnlocking = false,
                    )
                    celebrateEasterUnlock(EasterCelebrationKind.LIGHT_CATCHER)
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
        stopChatWebSocket()
        chatPresenceJob?.cancel()
        chatPresenceJob = null
        chatRefreshJob?.cancel()
        chatRefreshJob = null
        pendingEasterCelebrations.clear()
        shownEasterCelebrations.clear()
        bridgeCelebrationEligible = false
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
                appLockSettingsLoaded = _uiState.value.appLockSettingsLoaded,
            )
            appLockBackgroundedAtMs = null
            skipAppLockOnce = false
            unlockedThisSession = false
            consumedInitialLockCheck = false
            authSetupCompletedForUser = null
            viewModelScope.launch {
                appPreferences.clearAppLockBackgroundedAtMs()
                appPreferences.clearDriveRestorePromptDismissed()
            }
            LoginNotificationWorker.cancel(getApplication())
            MessageNotificationWorker.cancel(getApplication())
            ChatBackupWorker.cancel(getApplication())
            PushTokenRegistrar.unregisterCurrentToken(getApplication())
        }
    }

    fun onAppResumed() {
        checkForUpdate()
        if (_uiState.value.screen != AppScreen.Main || _uiState.value.user == null) return
        startChatWebSocket()
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

    private var isRefreshingSocialData = false
    fun refreshSocialData() {
        if (isRefreshingSocialData) return
        isRefreshingSocialData = true
        
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    friendsError = null,
                    messagesError = null,
                    isFriendsLoading = true,
                )

                val friendsDeferred = async { friendsRepository.loadFriends() }
                val pendingDeferred = async { friendsRepository.loadPendingRequests() }
                val sentDeferred = async { friendsRepository.loadSentRequests() }
                val draftsDeferred = async { appPreferences.getAllChatDrafts() }

                val friendsResult = friendsDeferred.await()
                val pendingResult = pendingDeferred.await()
                val sentResult = sentDeferred.await()
                val drafts = draftsDeferred.await()

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
                                _uiState.value = _uiState.value.copy(
                                    conversations = conversations.take(200), // Limit UI to top 200 chats
                                    chatDrafts = drafts,
                                )
                                refreshEasterFlagsFromServer()
                            }
                            .onFailure {
                                _uiState.value = _uiState.value.copy(messagesError = "unread_load_failed")
                                refreshEasterFlagsFromServer()
                            }
                    }
                    .onFailure {
                        _uiState.value = _uiState.value.copy(
                            friendsError = "friends_load_failed",
                            isFriendsLoading = false,
                        )
                    }
            } finally {
                isRefreshingSocialData = false
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
        clearOverlayNavigation()
        chatPresenceJob?.cancel()
        chatRefreshJob?.cancel()
        val cachedFriend = _uiState.value.friends.find { it.id == friendId }
        viewModelScope.launch {
            appPreferences.setActiveChatFriendId(friendId)
            messageNotificationMonitor.clearFriendNotification(friendId)
            val savedDraft = appPreferences.getChatDraft(friendId)
            _uiState.value = _uiState.value.copy(
                selectedMainTab = MainTab.Messages,
                chatFriendId = friendId,
                chatFriendName = friendName,
                chatFriendIsOnline = cachedFriend?.isOnline ?: false,
                chatFriendLastSeenAt = cachedFriend?.lastSeenAt,
                isChatLoading = true,
                messagesError = null,
                chatMessages = emptyList(),
                chatExitingMessageIds = emptySet(),
                chatPinnedMessage = null,
                chatReplyTarget = null,
                chatEditTarget = null,
                chatDraftText = savedDraft,
            )
            val saved = appPreferences.getArchivistProgress()
            archivistTracker = if (saved != null && saved.chatId == friendId) {
                ArchivistTracker(
                    chatId = friendId,
                    pinned = saved.pinned,
                    edited = saved.edited,
                    reacted = saved.reacted,
                    forwarded = saved.forwarded,
                )
            } else {
                ArchivistTracker(friendId)
            }
            refreshEasterProgressInState()
            refreshChatFriendPresence(friendId)
            chatPresenceJob = viewModelScope.launch {
                while (isActive) {
                    delay(5_000)
                    refreshChatFriendPresence(friendId)
                }
            }
            chatRefreshJob = viewModelScope.launch {
                while (isActive) {
                    val fallbackDelayMs = if (chatWebSocketClient.isConnected) 60_000L else 30_000L
                    delay(fallbackDelayMs)
                    if (!chatWebSocketClient.isConnected) {
                        reloadChat(friendId)
                    }
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
            chatExitingMessageIds = emptySet(),
            chatPinnedMessage = null,
            chatReplyTarget = null,
            chatEditTarget = null,
            chatDraftText = "",
            messagesError = null,
        )
        refreshSocialData()
    }

    fun updateChatDraft(text: String) {
        val friendId = _uiState.value.chatFriendId ?: return
        if (_uiState.value.chatEditTarget != null) return
        viewModelScope.launch {
            appPreferences.setChatDraft(friendId, text)
            _uiState.value = _uiState.value.copy(
                chatDraftText = text,
                chatDrafts = chatDraftsWith(friendId, text),
            )
        }
    }

    private fun chatDraftsWith(friendId: String, text: String): Map<String, String> {
        val next = _uiState.value.chatDrafts.toMutableMap()
        if (text.isBlank()) {
            next.remove(friendId)
        } else {
            next[friendId] = text
        }
        return next
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

        val targetMessage = _uiState.value.chatMessages.find { it.id == messageId }

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
            val userId = sessionManager.getUserId().orEmpty()
            val isIncomingReaction = targetMessage?.senderId?.let { it != userId } == true

            messagesRepository.reactToMessage(messageId, trimmed)
                .onSuccess {
                    reloadChat(friendId)
                    markArchivistAction(friendId) { it.reacted = true }
                    if (isIncomingReaction) {
                        v010EasterHelper.onReactionAdded(friendId)
                    }
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
        removeChatMessagesWithAnimation(setOf(messageId))
        viewModelScope.launch {
            messagesRepository.deleteMessage(messageId)
                .onFailure {
                    reloadChat(friendId)
                    _uiState.value = _uiState.value.copy(messagesError = "delete_failed")
                }
            refreshConversationsOnly()
        }
    }

    fun deleteChatMessages(messageIds: List<String>) {
        val friendId = _uiState.value.chatFriendId ?: return
        val ids = messageIds.toSet()
        if (ids.isEmpty()) return
        removeChatMessagesWithAnimation(ids)
        viewModelScope.launch {
            messagesRepository.deleteMessages(messageIds)
                .onFailure {
                    reloadChat(friendId)
                    _uiState.value = _uiState.value.copy(messagesError = "delete_failed")
                }
            refreshConversationsOnly()
        }
    }

    private fun removeChatMessagesWithAnimation(messageIds: Set<String>) {
        if (messageIds.isEmpty()) return
        val presentIds = messageIds.filter { id ->
            _uiState.value.chatMessages.any { it.id == id }
        }.toSet()
        if (presentIds.isEmpty()) return

        _uiState.value = _uiState.value.copy(
            chatExitingMessageIds = _uiState.value.chatExitingMessageIds + presentIds,
        )
        viewModelScope.launch {
            delay(CHAT_MESSAGE_EXIT_ANIM_MS)
            val pinned = _uiState.value.chatPinnedMessage
            _uiState.value = _uiState.value.copy(
                chatMessages = _uiState.value.chatMessages.filter { it.id !in presentIds },
                chatExitingMessageIds = _uiState.value.chatExitingMessageIds - presentIds,
                chatPinnedMessage = pinned?.takeUnless { it.messageId in presentIds },
            )
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

    private suspend fun reloadChat(friendId: String, refreshSocial: Boolean = true) {
        messagesRepository.loadMessages(friendId)
            .onSuccess { thread ->
                if (_uiState.value.chatFriendId != friendId) return
                _uiState.value = _uiState.value.copy(
                    chatMessages = thread.messages,
                    chatExitingMessageIds = emptySet(),
                    chatPinnedMessage = thread.pinned,
                    messagesError = null,
                )
                if (refreshSocial) {
                    refreshSocialData()
                }
            }
    }

    private fun refreshConversationsOnly() {
        viewModelScope.launch {
            val friends = _uiState.value.friends
            if (friends.isEmpty()) return@launch
            messagesRepository.loadConversations(friends)
                .onSuccess { conversations ->
                    _uiState.value = _uiState.value.copy(conversations = conversations)
                }
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

    fun sendMessage(content: String, sentViaEnter: Boolean = false) {
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
            try {
                updateUiState { it.copy(isSending = true, messagesError = null) }
                
                if (editTarget != null) {
                    messagesRepository.editMessage(editTarget.messageId, friendId, finalContent)
                        .onSuccess {
                            reloadChat(friendId)
                            markArchivistAction(friendId) { it.edited = true }
                            updateUiState { state ->
                                state.copy(
                                    isSending = false,
                                    chatReplyTarget = null,
                                    chatEditTarget = null,
                                )
                            }
                        }
                        .onFailure {
                            updateUiState { it.copy(isSending = false, messagesError = "edit_failed") }
                        }
                    return@launch
                }

                messagesRepository.sendMessage(friendId, finalContent)
                    .onSuccess { sentMessageId ->
                        appPreferences.setChatDraft(friendId, "")
                        val userId = sessionManager.getUserId().orEmpty()
                        
                        updateUiState { state ->
                            val newMessages = if (sentMessageId != null && userId.isNotBlank()) {
                                val optimisticMessage = MessageDto(
                                    id = sentMessageId,
                                    senderId = userId,
                                    content = finalContent,
                                    encryption = "signal_v1",
                                    createdAt = System.currentTimeMillis(),
                                )
                                state.chatMessages + optimisticMessage
                            } else {
                                state.chatMessages
                            }
                            
                            state.copy(
                                isSending = false,
                                chatReplyTarget = null,
                                chatEditTarget = null,
                                chatDraftText = "",
                                chatDrafts = chatDraftsWith(friendId, ""),
                                chatMessages = newMessages
                            )
                        }

                        if (sentMessageId == null || userId.isBlank()) {
                            viewModelScope.launch { reloadChat(friendId, refreshSocial = false) }
                        }
                        
                        maybeUnlockEchoEaster()
                        v010EasterHelper.onMessageSent(finalContent, sentViaEnter)
                        refreshConversationsOnly()
                    }
                    .onFailure {
                        updateUiState { it.copy(isSending = false, messagesError = "send_failed") }
                    }
            } catch (e: Exception) {
                android.util.Log.e("AppViewModel", "Failed to send message", e)
                updateUiState { it.copy(isSending = false, messagesError = "send_failed") }
            }
        }
    }

    private var isNightGuardUnlocking = false

    fun trackNightGuardConditions(isDarkTheme: Boolean, isMainScreen: Boolean) {
        nightGuardJob?.cancel()
        nightGuardJob = null

        val flags = _uiState.value.easterFlags ?: return
        val login = _uiState.value.user?.login
        
        // Quick check before starting any logic
        val isUnlockedOnBackend = flags.nightGuard
        if (isNightGuardUnlocking || isUnlockedOnBackend || isEasterAlreadyUnlocked(EasterCelebrationKind.NIGHT_GUARD)) {
            viewModelScope.launch {
                if (appPreferences.getNightGuardElapsedMs() > 0) {
                    appPreferences.clearNightGuardElapsedMs()
                    refreshEasterProgressInState()
                }
            }
            return
        }
        
        // Extra safety: check if we already logged this for Telegram
        if (login != null) {
            viewModelScope.launch {
                if (appPreferences.isEasterTelegramLogged(login, "night_guard")) {
                    appPreferences.clearNightGuardElapsedMs()
                    shownEasterCelebrations.add(EasterCelebrationKind.NIGHT_GUARD)
                    refreshEasterProgressInState()
                    return@launch
                }
            }
        }

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (hour >= 6) {
            viewModelScope.launch {
                appPreferences.clearNightGuardElapsedMs()
                refreshEasterProgressInState()
            }
            return
        }

        if (!isMainScreen || !isDarkTheme) return

        nightGuardJob = viewModelScope.launch {
            val requiredMs = 30_000L
            while (isActive) {
                if (_uiState.value.easterFlags?.nightGuard == true || isEasterAlreadyUnlocked(EasterCelebrationKind.NIGHT_GUARD)) break

                val hourNow = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                if (hourNow >= 6) {
                    appPreferences.clearNightGuardElapsedMs()
                    refreshEasterProgressInState()
                    break
                }

                delay(1_000)
                val elapsed = appPreferences.addNightGuardElapsedMs(1_000)
                refreshEasterProgressInState()

                if (elapsed >= requiredMs) {
                    if (_uiState.value.easterFlags?.nightGuard == true || isEasterAlreadyUnlocked(EasterCelebrationKind.NIGHT_GUARD)) {
                        appPreferences.clearNightGuardElapsedMs()
                        refreshEasterProgressInState()
                        break
                    }
                    isNightGuardUnlocking = true
                    profileRepository.unlockNightGuard()
                        .onSuccess {
                            if (isEasterAlreadyUnlocked(EasterCelebrationKind.NIGHT_GUARD)) {
                                isNightGuardUnlocking = false
                                return@onSuccess
                            }
                            _uiState.value.user?.login?.let { login ->
                                EasterLogger.logNightGuard(appPreferences, login)
                            }
                            appPreferences.clearNightGuardElapsedMs()
                            celebrateEasterUnlock(EasterCelebrationKind.NIGHT_GUARD)
                            refreshEasterFlagsFromServer()
                            isNightGuardUnlocking = false
                        }
                        .onFailure {
                            isNightGuardUnlocking = false
                        }
                    break
                }
            }
        }
    }

    fun onBiometricUnlockSuccess() {
        if (isEasterAlreadyUnlocked(EasterCelebrationKind.TRUSTED_FINGERPRINT)) return
        viewModelScope.launch {
            val count = appPreferences.incrementBiometricUnlockCount()
            refreshEasterProgressInState()
            if (count >= 100) {
                profileRepository.unlockTrustedFingerprint()
                    .onSuccess {
                        _uiState.value.user?.login?.let { login ->
                            EasterLogger.logTrustedFingerprint(appPreferences, login)
                        }
                        celebrateEasterUnlock(EasterCelebrationKind.TRUSTED_FINGERPRINT)
                        refreshEasterFlagsFromServer()
                    }
            }
        }
    }

    private fun maybeUnlockEchoEaster() {
        if (isEasterAlreadyUnlocked(EasterCelebrationKind.ECHO)) return
        val calendar = Calendar.getInstance()
        if (calendar.get(Calendar.HOUR_OF_DAY) != 23 || calendar.get(Calendar.MINUTE) != 59) return
        viewModelScope.launch {
            profileRepository.unlockEcho()
                .onSuccess {
                    _uiState.value.user?.login?.let { login ->
                        EasterLogger.logEcho(appPreferences, login)
                    }
                    celebrateEasterUnlock(EasterCelebrationKind.ECHO)
                    refreshEasterFlagsFromServer()
                }
        }
    }

    private fun markArchivistAction(chatId: String, mark: (ArchivistTracker) -> Unit) {
        if (isEasterAlreadyUnlocked(EasterCelebrationKind.ARCHIVIST)) return
        val tracker = archivistTracker ?: return
        if (tracker.chatId != chatId) return
        mark(tracker)
        viewModelScope.launch {
            appPreferences.saveArchivistProgress(
                chatId = tracker.chatId,
                pinned = tracker.pinned,
                edited = tracker.edited,
                reacted = tracker.reacted,
                forwarded = tracker.forwarded,
            )
            refreshEasterProgressInState()
            if (!tracker.isComplete()) return@launch
            profileRepository.unlockArchivist()
                .onSuccess {
                    _uiState.value.user?.login?.let { login ->
                        EasterLogger.logArchivist(appPreferences, login)
                    }
                    celebrateEasterUnlock(EasterCelebrationKind.ARCHIVIST)
                    refreshEasterFlagsFromServer()
                }
        }
    }

    private suspend fun buildEasterProgress(flags: EasterFlagsDto? = _uiState.value.easterFlags): EasterProgress {
        val archivist = appPreferences.getArchivistProgress()
        val archivistCount = if (archivist != null) {
            listOf(archivist.pinned, archivist.edited, archivist.reacted, archivist.forwarded).count { it }
        } else {
            0
        }
        val bridgeCount = (if (flags?.bridgeWebToday == true) 1 else 0) +
            (if (flags?.bridgeAppToday == true) 1 else 0)
        val formatMirrorCount = (if (flags?.formatMirrorWebToday == true) 1 else 0) +
            (if (flags?.formatMirrorAppToday == true) 1 else 0)
        val v010 = appPreferences.getV010EasterProgress()
        return EasterProgress(
            biometricUnlockCount = appPreferences.getBiometricUnlockCount().coerceIn(0, 100),
            nightGuardSeconds = (appPreferences.getNightGuardElapsedMs() / 1_000).toInt().coerceIn(0, 30),
            archivistStepsCompleted = archivistCount.coerceIn(0, 4),
            bridgePlatformsToday = bridgeCount.coerceIn(0, 2),
            spoilerReveals = v010.spoilerReveals.coerceIn(0, 5),
            enterSendCount = v010.enterSendCount.coerceIn(0, 10),
            driveAccountPicks = v010.driveAccountPicks.coerceIn(0, 3),
            watchmanOpens = v010.watchmanOpens.coerceIn(0, 3),
            carouselSeconds = v010.carouselSeconds.coerceIn(0, 30),
            quoteCount = v010.quoteCount.coerceIn(0, 3),
            reactionStreak = v010.reactionStreak.coerceIn(0, 10),
            polyglotLocalesCount = v010.polyglotLocales.size.coerceIn(0, 3),
            formatMirrorPlatformsToday = formatMirrorCount.coerceIn(0, 2),
        )
    }

    private fun refreshEasterProgressInState() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(easterProgress = buildEasterProgress())
        }
    }

    private fun refreshEasterFlagsFromServer() {
        viewModelScope.launch {
            val previous = _uiState.value.easterFlags
            val hadBridge = previous?.bridge == true
            val hadFormatMirror = previous?.formatMirror == true
            profileRepository.loadEasterFlags()
                .onSuccess { flags ->
                    val login = _uiState.value.user?.login
                    if (previous != null && !hadBridge && flags.bridge && login != null) {
                        EasterLogger.logBridge(appPreferences, login)
                    }
                    if (previous != null && !hadFormatMirror && flags.formatMirror && login != null) {
                        EasterLogger.logFormatMirror(appPreferences, login)
                    }
                    maybeCelebrateBridgeFromRefresh(previous, flags)
                    maybeCelebrateFormatMirrorFromRefresh(previous, flags)
                    if (login != null) {
                        EasterLogger.syncLoggedFromServerFlags(appPreferences, login, flags)
                    }
                    appPreferences.syncV010EasterProgressWithServerFlags(flags)
                    syncShownEasterCelebrations(flags)
                    _uiState.value = _uiState.value.copy(
                        easterFlags = flags,
                        easterError = null,
                        easterProgress = buildEasterProgress(flags),
                    )
                }
        }
    }

    private fun maybeCelebrateBridgeFromRefresh(previous: EasterFlagsDto?, flags: EasterFlagsDto) {
        if (previous == null || previous.bridge || !flags.bridge || !bridgeCelebrationEligible) return
        bridgeCelebrationEligible = false
        celebrateEasterUnlock(EasterCelebrationKind.BRIDGE)
    }

    private fun maybeCelebrateFormatMirrorFromRefresh(previous: EasterFlagsDto?, flags: EasterFlagsDto) {
        if (previous == null || previous.formatMirror || !flags.formatMirror) return
        celebrateEasterUnlock(EasterCelebrationKind.FORMAT_MIRROR)
    }

    private fun syncShownEasterCelebrations(flags: EasterFlagsDto) {
        if (flags.lightCatcher) shownEasterCelebrations.add(EasterCelebrationKind.LIGHT_CATCHER)
        if (flags.nightGuard) shownEasterCelebrations.add(EasterCelebrationKind.NIGHT_GUARD)
        if (flags.trustedFingerprint) shownEasterCelebrations.add(EasterCelebrationKind.TRUSTED_FINGERPRINT)
        if (flags.echo) shownEasterCelebrations.add(EasterCelebrationKind.ECHO)
        if (flags.archivist) shownEasterCelebrations.add(EasterCelebrationKind.ARCHIVIST)
        if (flags.bridge) shownEasterCelebrations.add(EasterCelebrationKind.BRIDGE)
        if (flags.formatMirror) shownEasterCelebrations.add(EasterCelebrationKind.FORMAT_MIRROR)
        if (flags.typographer) shownEasterCelebrations.add(EasterCelebrationKind.TYPOGRAPHER)
        if (flags.spoilerHunter) shownEasterCelebrations.add(EasterCelebrationKind.SPOILER_HUNTER)
        if (flags.noMarkers) shownEasterCelebrations.add(EasterCelebrationKind.NO_MARKERS)
        if (flags.enterMaster) shownEasterCelebrations.add(EasterCelebrationKind.ENTER_MASTER)
        if (flags.fontExtremes) shownEasterCelebrations.add(EasterCelebrationKind.FONT_EXTREMES)
        if (flags.cloudKeeper) shownEasterCelebrations.add(EasterCelebrationKind.CLOUD_KEEPER)
        if (flags.drivePilot) shownEasterCelebrations.add(EasterCelebrationKind.DRIVE_PILOT)
        if (flags.liveWire) shownEasterCelebrations.add(EasterCelebrationKind.LIVE_WIRE)
        if (flags.fromShadow) shownEasterCelebrations.add(EasterCelebrationKind.FROM_SHADOW)
        if (flags.watchman) shownEasterCelebrations.add(EasterCelebrationKind.WATCHMAN)
        if (flags.carouselWatcher) shownEasterCelebrations.add(EasterCelebrationKind.CAROUSEL_WATCHER)
        if (flags.synchronist) shownEasterCelebrations.add(EasterCelebrationKind.SYNCHRONIST)
        if (flags.quoteDay) shownEasterCelebrations.add(EasterCelebrationKind.QUOTE_DAY)
        if (flags.midnightEditor) shownEasterCelebrations.add(EasterCelebrationKind.MIDNIGHT_EDITOR)
        if (flags.polyglotFriend) shownEasterCelebrations.add(EasterCelebrationKind.POLYGLOT_FRIEND)
        if (flags.silence) shownEasterCelebrations.add(EasterCelebrationKind.SILENCE)
        if (flags.reactionStreak) shownEasterCelebrations.add(EasterCelebrationKind.REACTION_STREAK)
    }
}
