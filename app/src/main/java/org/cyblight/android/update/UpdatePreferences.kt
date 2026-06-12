package org.cyblight.android.update

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.updateDataStore: DataStore<Preferences> by preferencesDataStore(name = "cyblight_updates")

class UpdatePreferences(private val context: Context) {
    private val dismissedVersionKey = stringPreferencesKey("dismissed_version")
    private val lastAutoCheckKey = longPreferencesKey("last_auto_check_at")

    private companion object {
        const val AUTO_CHECK_INTERVAL_MS = 30 * 60 * 1000L
    }

    suspend fun getDismissedVersion(): String? =
        context.updateDataStore.data.map { it[dismissedVersionKey] }.first()

    suspend fun shouldAutoCheckNow(): Boolean {
        val lastCheck = context.updateDataStore.data.map { it[lastAutoCheckKey] ?: 0L }.first()
        return System.currentTimeMillis() - lastCheck >= AUTO_CHECK_INTERVAL_MS
    }

    suspend fun markAutoChecked() {
        context.updateDataStore.edit { prefs ->
            prefs[lastAutoCheckKey] = System.currentTimeMillis()
        }
    }

    suspend fun dismissVersion(versionName: String) {
        context.updateDataStore.edit { prefs ->
            prefs[dismissedVersionKey] = versionName
        }
    }

    suspend fun clearDismissed() {
        context.updateDataStore.edit { prefs ->
            prefs.remove(dismissedVersionKey)
        }
    }
}
