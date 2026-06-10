package org.cyblight.android.update

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.updateDataStore: DataStore<Preferences> by preferencesDataStore(name = "cyblight_updates")

class UpdatePreferences(private val context: Context) {
    private val dismissedVersionKey = stringPreferencesKey("dismissed_version")

    suspend fun getDismissedVersion(): String? =
        context.updateDataStore.data.map { it[dismissedVersionKey] }.first()

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
