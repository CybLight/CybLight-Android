package org.cyblight.android.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.cyblight.android.security.AppLockHelper

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

private val Context.appPreferencesStore: DataStore<Preferences> by preferencesDataStore(
    name = "cyblight_preferences",
)

class AppPreferences(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme_mode")
    private val notificationsKey = booleanPreferencesKey("notifications_enabled")
    private val loginAlertsKey = booleanPreferencesKey("login_alerts_enabled")
    private val messageAlertsKey = booleanPreferencesKey("message_alerts_enabled")
    private val activeChatFriendIdKey = stringPreferencesKey("active_chat_friend_id")
    private val lastNotifiedUnreadCountsKey = stringPreferencesKey("last_notified_unread_counts")
    private val appLockEnabledKey = booleanPreferencesKey("app_lock_enabled")
    private val appLockBiometricKey = booleanPreferencesKey("app_lock_biometric")
    private val appLockPinSaltKey = stringPreferencesKey("app_lock_pin_salt")
    private val appLockPinHashKey = stringPreferencesKey("app_lock_pin_hash")
    private val appLockTimeoutKey = longPreferencesKey("app_lock_timeout_ms")
    private val appLockBackgroundedAtKey = longPreferencesKey("app_lock_backgrounded_at_ms")
    private val lastSeenLoginEventIdKey = stringPreferencesKey("last_seen_login_event_id")
    private val ignoreLoginEventsUntilKey = longPreferencesKey("ignore_login_events_until")
    private val biometricUnlockCountKey = intPreferencesKey("biometric_unlock_count")
    private val nightGuardElapsedKey = longPreferencesKey("night_guard_elapsed_ms")
    private val archivistProgressKey = stringPreferencesKey("archivist_progress")
    private val swipeBackEnabledKey = booleanPreferencesKey("swipe_back_enabled")
    private val systemBackEnabledKey = booleanPreferencesKey("system_back_enabled")
    private val swipeBackSensitivityKey = stringPreferencesKey("swipe_back_sensitivity")
    private val swipeBackEdgeWidthKey = stringPreferencesKey("swipe_back_edge_width")
    private val rootBackBehaviorKey = stringPreferencesKey("root_back_behavior")
    private val encryptionReminderChatDismissedKey = booleanPreferencesKey("encryption_reminder_chat_dismissed")
    private val chatFormatToolbarHiddenKey = booleanPreferencesKey("chat_format_toolbar_hidden")
    private val homeWhatsNewBannerHiddenKey = booleanPreferencesKey("home_whats_new_banner_hidden")
    private val chatBackupFrequencyKey = stringPreferencesKey("chat_backup_frequency")
    private val chatBackupOverCellularKey = booleanPreferencesKey("chat_backup_over_cellular")
    private val lastAutoBackupSuccessMsKey = longPreferencesKey("last_auto_backup_success_ms")
    private val driveRestoreDismissedUserKey = stringPreferencesKey("drive_restore_dismissed_user")
    private val easterTelegramLoggedKey = stringSetPreferencesKey("easter_telegram_logged")

    private fun chatDraftKey(friendId: String) = stringPreferencesKey("chat_draft_$friendId")

    val themeMode: Flow<ThemeMode> = context.appPreferencesStore.data.map { prefs ->
        ThemeMode.entries.firstOrNull { it.name == prefs[themeKey] } ?: ThemeMode.SYSTEM
    }

    val notificationsEnabled: Flow<Boolean> = context.appPreferencesStore.data.map { prefs ->
        prefs[notificationsKey] ?: true
    }

    val loginAlertsEnabled: Flow<Boolean> = context.appPreferencesStore.data.map { prefs ->
        prefs[loginAlertsKey] ?: true
    }

    val messageAlertsEnabled: Flow<Boolean> = context.appPreferencesStore.data.map { prefs ->
        prefs[messageAlertsKey] ?: true
    }

    val appLockEnabled: Flow<Boolean> = context.appPreferencesStore.data.map { prefs ->
        prefs[appLockEnabledKey] ?: false
    }

    val appLockBiometric: Flow<Boolean> = context.appPreferencesStore.data.map { prefs ->
        prefs[appLockBiometricKey] ?: true
    }

    suspend fun getThemeMode(): ThemeMode =
        themeMode.first()

    suspend fun getNotificationsEnabled(): Boolean =
        notificationsEnabled.first()

    suspend fun getLoginAlertsEnabled(): Boolean =
        loginAlertsEnabled.first()

    suspend fun getMessageAlertsEnabled(): Boolean =
        messageAlertsEnabled.first()

    suspend fun getAppLockEnabled(): Boolean =
        appLockEnabled.first()

    suspend fun getAppLockBiometric(): Boolean =
        appLockBiometric.first()

    suspend fun getAppLockTimeout(): AppLockTimeout {
        val millis = context.appPreferencesStore.data.first()[appLockTimeoutKey]
            ?: AppLockTimeout.IMMEDIATE.millis
        return AppLockTimeout.fromMillis(millis)
    }

    suspend fun setAppLockTimeout(timeout: AppLockTimeout) {
        context.appPreferencesStore.edit { prefs ->
            prefs[appLockTimeoutKey] = timeout.millis
        }
    }

    suspend fun getAppLockBackgroundedAtMs(): Long? {
        val value = context.appPreferencesStore.data.first()[appLockBackgroundedAtKey] ?: return null
        return value.takeIf { it > 0L }
    }

    suspend fun setAppLockBackgroundedAtMs(timestampMs: Long) {
        context.appPreferencesStore.edit { prefs ->
            prefs[appLockBackgroundedAtKey] = timestampMs
        }
    }

    suspend fun clearAppLockBackgroundedAtMs() {
        context.appPreferencesStore.edit { prefs ->
            prefs.remove(appLockBackgroundedAtKey)
        }
    }

    suspend fun hasAppLockPin(): Boolean {
        val prefs = context.appPreferencesStore.data.first()
        return !prefs[appLockPinHashKey].isNullOrBlank() && !prefs[appLockPinSaltKey].isNullOrBlank()
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.appPreferencesStore.edit { prefs ->
            prefs[themeKey] = mode.name
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.appPreferencesStore.edit { prefs ->
            prefs[notificationsKey] = enabled
        }
    }

    suspend fun setLoginAlertsEnabled(enabled: Boolean) {
        context.appPreferencesStore.edit { prefs ->
            prefs[loginAlertsKey] = enabled
        }
    }

    suspend fun setMessageAlertsEnabled(enabled: Boolean) {
        context.appPreferencesStore.edit { prefs ->
            prefs[messageAlertsKey] = enabled
        }
    }

    suspend fun getActiveChatFriendId(): String? =
        context.appPreferencesStore.data.first()[activeChatFriendIdKey]

    suspend fun setActiveChatFriendId(friendId: String?) {
        context.appPreferencesStore.edit { prefs ->
            if (friendId.isNullOrBlank()) {
                prefs.remove(activeChatFriendIdKey)
            } else {
                prefs[activeChatFriendIdKey] = friendId
            }
        }
    }

    suspend fun getLastNotifiedUnreadCounts(): Map<String, Int> {
        val raw = context.appPreferencesStore.data.first()[lastNotifiedUnreadCountsKey] ?: return emptyMap()
        return raw.split('\n')
            .mapNotNull { line ->
                val parts = line.split('=', limit = 2)
                if (parts.size != 2) return@mapNotNull null
                val count = parts[1].toIntOrNull() ?: return@mapNotNull null
                parts[0] to count
            }
            .toMap()
    }

    suspend fun setLastNotifiedUnreadCounts(counts: Map<String, Int>) {
        val encoded = counts.entries.joinToString("\n") { "${it.key}=${it.value}" }
        context.appPreferencesStore.edit { prefs ->
            prefs[lastNotifiedUnreadCountsKey] = encoded
        }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.appPreferencesStore.edit { prefs ->
            prefs[appLockEnabledKey] = enabled
        }
    }

    suspend fun setAppLockBiometric(enabled: Boolean) {
        context.appPreferencesStore.edit { prefs ->
            prefs[appLockBiometricKey] = enabled
        }
    }

    suspend fun setAppLockPin(pin: String) {
        val salt = AppLockHelper.generateSalt()
        val hash = AppLockHelper.hashPin(pin, salt)
        context.appPreferencesStore.edit { prefs ->
            prefs[appLockPinSaltKey] = salt
            prefs[appLockPinHashKey] = hash
        }
    }

    suspend fun verifyAppLockPin(pin: String): Boolean {
        val prefs = context.appPreferencesStore.data.first()
        val salt = prefs[appLockPinSaltKey] ?: return false
        val hash = prefs[appLockPinHashKey] ?: return false
        return AppLockHelper.verifyPin(pin, salt, hash)
    }

    suspend fun clearAppLockPin() {
        context.appPreferencesStore.edit { prefs ->
            prefs.remove(appLockPinSaltKey)
            prefs.remove(appLockPinHashKey)
            prefs[appLockEnabledKey] = false
            prefs.remove(appLockBackgroundedAtKey)
        }
    }

    suspend fun getLastSeenLoginEventId(): String? =
        context.appPreferencesStore.data.first()[lastSeenLoginEventIdKey]

    suspend fun setLastSeenLoginEventId(id: String) {
        context.appPreferencesStore.edit { prefs ->
            prefs[lastSeenLoginEventIdKey] = id
        }
    }

    suspend fun getIgnoreLoginEventsUntil(): Long =
        context.appPreferencesStore.data.first()[ignoreLoginEventsUntilKey] ?: 0L

    suspend fun setIgnoreLoginEventsUntil(timestampMs: Long) {
        context.appPreferencesStore.edit { prefs ->
            prefs[ignoreLoginEventsUntilKey] = timestampMs
        }
    }

    suspend fun getBiometricUnlockCount(): Int =
        context.appPreferencesStore.data.first()[biometricUnlockCountKey] ?: 0

    suspend fun incrementBiometricUnlockCount(): Int {
        var next = 0
        context.appPreferencesStore.edit { prefs ->
            next = (prefs[biometricUnlockCountKey] ?: 0) + 1
            prefs[biometricUnlockCountKey] = next
        }
        return next
    }

    suspend fun getNightGuardElapsedMs(): Long =
        context.appPreferencesStore.data.first()[nightGuardElapsedKey] ?: 0L

    suspend fun addNightGuardElapsedMs(deltaMs: Long): Long {
        var total = 0L
        context.appPreferencesStore.edit { prefs ->
            total = (prefs[nightGuardElapsedKey] ?: 0L) + deltaMs
            prefs[nightGuardElapsedKey] = total
        }
        return total
    }

    suspend fun clearNightGuardElapsedMs() {
        context.appPreferencesStore.edit { prefs ->
            prefs.remove(nightGuardElapsedKey)
        }
    }

    suspend fun getChatDraft(friendId: String): String =
        context.appPreferencesStore.data.first()[chatDraftKey(friendId)].orEmpty()

    suspend fun setChatDraft(friendId: String, text: String) {
        context.appPreferencesStore.edit { prefs ->
            val key = chatDraftKey(friendId)
            if (text.isBlank()) {
                prefs.remove(key)
            } else {
                prefs[key] = text
            }
        }
    }

    suspend fun getArchivistProgress(): ArchivistProgressSnapshot? {
        val raw = context.appPreferencesStore.data.first()[archivistProgressKey] ?: return null
        val parts = raw.split('\t')
        if (parts.size != 5 || parts[0].isBlank()) return null
        return ArchivistProgressSnapshot(
            chatId = parts[0],
            pinned = parts[1] == "1",
            edited = parts[2] == "1",
            reacted = parts[3] == "1",
            forwarded = parts[4] == "1",
        )
    }

    suspend fun saveArchivistProgress(
        chatId: String,
        pinned: Boolean,
        edited: Boolean,
        reacted: Boolean,
        forwarded: Boolean,
    ) {
        val encoded = listOf(
            chatId,
            if (pinned) "1" else "0",
            if (edited) "1" else "0",
            if (reacted) "1" else "0",
            if (forwarded) "1" else "0",
        ).joinToString("\t")
        context.appPreferencesStore.edit { prefs ->
            prefs[archivistProgressKey] = encoded
        }
    }

    suspend fun getSwipeBackEnabled(): Boolean =
        context.appPreferencesStore.data.first()[swipeBackEnabledKey] ?: true

    suspend fun getSystemBackEnabled(): Boolean =
        context.appPreferencesStore.data.first()[systemBackEnabledKey] ?: true

    suspend fun getSwipeBackSensitivity(): SwipeBackSensitivity =
        SwipeBackSensitivity.fromName(context.appPreferencesStore.data.first()[swipeBackSensitivityKey])

    suspend fun getSwipeBackEdgeWidth(): SwipeBackEdgeWidth =
        SwipeBackEdgeWidth.fromName(context.appPreferencesStore.data.first()[swipeBackEdgeWidthKey])

    suspend fun getRootBackBehavior(): RootBackBehavior =
        RootBackBehavior.fromName(context.appPreferencesStore.data.first()[rootBackBehaviorKey])

    suspend fun setSwipeBackEnabled(enabled: Boolean) {
        context.appPreferencesStore.edit { prefs ->
            prefs[swipeBackEnabledKey] = enabled
        }
    }

    suspend fun setSystemBackEnabled(enabled: Boolean) {
        context.appPreferencesStore.edit { prefs ->
            prefs[systemBackEnabledKey] = enabled
        }
    }

    suspend fun setSwipeBackSensitivity(sensitivity: SwipeBackSensitivity) {
        context.appPreferencesStore.edit { prefs ->
            prefs[swipeBackSensitivityKey] = sensitivity.name
        }
    }

    suspend fun setSwipeBackEdgeWidth(edgeWidth: SwipeBackEdgeWidth) {
        context.appPreferencesStore.edit { prefs ->
            prefs[swipeBackEdgeWidthKey] = edgeWidth.name
        }
    }

    suspend fun setRootBackBehavior(behavior: RootBackBehavior) {
        context.appPreferencesStore.edit { prefs ->
            prefs[rootBackBehaviorKey] = behavior.name
        }
    }

    suspend fun getEncryptionReminderChatDismissed(): Boolean =
        context.appPreferencesStore.data.first()[encryptionReminderChatDismissedKey] ?: false

    suspend fun setEncryptionReminderChatDismissed(dismissed: Boolean) {
        context.appPreferencesStore.edit { prefs ->
            prefs[encryptionReminderChatDismissedKey] = dismissed
        }
    }

    suspend fun getChatFormatToolbarHidden(): Boolean =
        context.appPreferencesStore.data.first()[chatFormatToolbarHiddenKey] ?: false

    suspend fun setChatFormatToolbarHidden(hidden: Boolean) {
        context.appPreferencesStore.edit { prefs ->
            prefs[chatFormatToolbarHiddenKey] = hidden
        }
    }

    suspend fun getHomeWhatsNewBannerHidden(): Boolean =
        context.appPreferencesStore.data.first()[homeWhatsNewBannerHiddenKey] ?: false

    suspend fun setHomeWhatsNewBannerHidden(hidden: Boolean) {
        context.appPreferencesStore.edit { prefs ->
            prefs[homeWhatsNewBannerHiddenKey] = hidden
        }
    }

    suspend fun getChatBackupFrequency(): ChatBackupFrequency {
        val name = context.appPreferencesStore.data.first()[chatBackupFrequencyKey]
        return ChatBackupFrequency.fromName(name)
    }

    suspend fun setChatBackupFrequency(frequency: ChatBackupFrequency) {
        context.appPreferencesStore.edit { prefs ->
            if (frequency == ChatBackupFrequency.OFF) {
                prefs.remove(chatBackupFrequencyKey)
            } else {
                prefs[chatBackupFrequencyKey] = frequency.name
            }
        }
    }

    suspend fun getChatBackupOverCellular(): Boolean =
        context.appPreferencesStore.data.first()[chatBackupOverCellularKey] ?: false

    suspend fun setChatBackupOverCellular(enabled: Boolean) {
        context.appPreferencesStore.edit { prefs ->
            prefs[chatBackupOverCellularKey] = enabled
        }
    }

    suspend fun getLastAutoBackupSuccessMs(): Long =
        context.appPreferencesStore.data.first()[lastAutoBackupSuccessMsKey] ?: 0L

    suspend fun setLastAutoBackupSuccessMs(timestampMs: Long) {
        context.appPreferencesStore.edit { prefs ->
            if (timestampMs <= 0L) {
                prefs.remove(lastAutoBackupSuccessMsKey)
            } else {
                prefs[lastAutoBackupSuccessMsKey] = timestampMs
            }
        }
    }

    suspend fun isDriveRestorePromptDismissed(userId: String): Boolean =
        context.appPreferencesStore.data.first()[driveRestoreDismissedUserKey] == userId

    suspend fun setDriveRestorePromptDismissed(userId: String) {
        context.appPreferencesStore.edit { prefs ->
            prefs[driveRestoreDismissedUserKey] = userId
        }
    }

    suspend fun clearDriveRestorePromptDismissed() {
        context.appPreferencesStore.edit { prefs ->
            prefs.remove(driveRestoreDismissedUserKey)
        }
    }

    suspend fun isEasterTelegramLogged(userName: String, eggKey: String): Boolean {
        val entry = easterTelegramEntryKey(userName, eggKey)
        return context.appPreferencesStore.data.first()[easterTelegramLoggedKey]?.contains(entry) == true
    }

    suspend fun markEasterTelegramLogged(userName: String, eggKey: String) {
        val entry = easterTelegramEntryKey(userName, eggKey)
        context.appPreferencesStore.edit { prefs ->
            val current = prefs[easterTelegramLoggedKey] ?: emptySet()
            prefs[easterTelegramLoggedKey] = current + entry
        }
    }

    private fun easterTelegramEntryKey(userName: String, eggKey: String): String =
        "${userName.trim().lowercase()}:$eggKey"
}

data class ArchivistProgressSnapshot(
    val chatId: String,
    val pinned: Boolean = false,
    val edited: Boolean = false,
    val reacted: Boolean = false,
    val forwarded: Boolean = false,
)
