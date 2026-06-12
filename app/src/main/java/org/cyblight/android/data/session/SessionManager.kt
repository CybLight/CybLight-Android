package org.cyblight.android.data.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cyblight_session")

class SessionManager(private val context: Context) {
    private val tokenKey = stringPreferencesKey("auth_token")
    private val userIdKey = stringPreferencesKey("user_id")
    private val loginKey = stringPreferencesKey("login")
    private val localeKey = stringPreferencesKey("locale")
    private val deviceTokenKey = stringPreferencesKey("device_token")
    private val sessionExpiredEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    val authToken: Flow<String?> = context.dataStore.data.map { it[tokenKey] }
    val sessionExpired: SharedFlow<Unit> = sessionExpiredEvents.asSharedFlow()

    val isLoggedIn: Flow<Boolean> = authToken.map { !it.isNullOrBlank() }

    val savedLocale: Flow<String> = context.dataStore.data.map { it[localeKey] ?: "ru" }

    suspend fun getToken(): String? = context.dataStore.data.first()[tokenKey]

    suspend fun getUserId(): String? = context.dataStore.data.first()[userIdKey]

    suspend fun getLocale(): String = context.dataStore.data.first()[localeKey] ?: "ru"

    suspend fun getDeviceToken(): String? = context.dataStore.data.first()[deviceTokenKey]

    suspend fun saveDeviceToken(token: String) {
        context.dataStore.edit { prefs ->
            prefs[deviceTokenKey] = token
        }
    }

    suspend fun clearDeviceToken() {
        context.dataStore.edit { prefs ->
            prefs.remove(deviceTokenKey)
        }
    }

    suspend fun saveSession(token: String, userId: String, login: String) {
        context.dataStore.edit { prefs ->
            prefs[tokenKey] = token
            prefs[userIdKey] = userId
            prefs[loginKey] = login
        }
    }

    suspend fun updateToken(token: String) {
        if (token.isBlank()) return
        context.dataStore.edit { prefs ->
            prefs[tokenKey] = token
        }
    }

    suspend fun saveLocale(locale: String) {
        context.dataStore.edit { prefs ->
            prefs[localeKey] = locale
        }
    }

    suspend fun clear() {
        context.dataStore.edit { prefs ->
            prefs.remove(tokenKey)
            prefs.remove(userIdKey)
            prefs.remove(loginKey)
            prefs.remove(deviceTokenKey)
        }
    }

    suspend fun clearAndNotifyExpired() {
        clear()
        sessionExpiredEvents.tryEmit(Unit)
    }

    suspend fun currentLogin(): String? = context.dataStore.data.first()[loginKey]
}
