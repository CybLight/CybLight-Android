package org.cyblight.android.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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
    private val appLockEnabledKey = booleanPreferencesKey("app_lock_enabled")
    private val appLockBiometricKey = booleanPreferencesKey("app_lock_biometric")
    private val appLockPinSaltKey = stringPreferencesKey("app_lock_pin_salt")
    private val appLockPinHashKey = stringPreferencesKey("app_lock_pin_hash")
    private val appLockTimeoutKey = longPreferencesKey("app_lock_timeout_ms")
    private val lastSeenLoginEventIdKey = stringPreferencesKey("last_seen_login_event_id")
    private val ignoreLoginEventsUntilKey = longPreferencesKey("ignore_login_events_until")

    val themeMode: Flow<ThemeMode> = context.appPreferencesStore.data.map { prefs ->
        ThemeMode.entries.firstOrNull { it.name == prefs[themeKey] } ?: ThemeMode.SYSTEM
    }

    val notificationsEnabled: Flow<Boolean> = context.appPreferencesStore.data.map { prefs ->
        prefs[notificationsKey] ?: true
    }

    val loginAlertsEnabled: Flow<Boolean> = context.appPreferencesStore.data.map { prefs ->
        prefs[loginAlertsKey] ?: true
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
}
