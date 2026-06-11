package org.cyblight.android.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

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

    val themeMode: Flow<ThemeMode> = context.appPreferencesStore.data.map { prefs ->
        ThemeMode.entries.firstOrNull { it.name == prefs[themeKey] } ?: ThemeMode.SYSTEM
    }

    val notificationsEnabled: Flow<Boolean> = context.appPreferencesStore.data.map { prefs ->
        prefs[notificationsKey] ?: true
    }

    suspend fun getThemeMode(): ThemeMode =
        themeMode.first()

    suspend fun getNotificationsEnabled(): Boolean =
        notificationsEnabled.first()

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
}
