package org.cyblight.android.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class BackupPasswordStore(context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        context.applicationContext,
        PREFS_NAME,
        MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun savePassword(password: String) {
        prefs.edit().putString(KEY_PASSWORD, password).apply()
    }

    fun getPassword(): String? = prefs.getString(KEY_PASSWORD, null)?.takeIf { it.isNotBlank() }

    fun hasPassword(): Boolean = !getPassword().isNullOrBlank()

    fun clearPassword() {
        prefs.edit().remove(KEY_PASSWORD).apply()
    }

    companion object {
        private const val PREFS_NAME = "cyblight_backup_secrets"
        private const val KEY_PASSWORD = "backup_password"
    }
}
