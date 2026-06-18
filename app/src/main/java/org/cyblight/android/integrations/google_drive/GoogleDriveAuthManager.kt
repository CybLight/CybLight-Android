package org.cyblight.android.integrations.google_drive

import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GoogleDriveAuthManager(private val context: Context) {
    private val signInOptions: GoogleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(GoogleDriveConfig.webClientId)
        .requestEmail()
        .requestScopes(Scope(GoogleDriveConfig.DRIVE_FILE_SCOPE))
        .requestScopes(Scope(GoogleDriveConfig.DRIVE_METADATA_READONLY_SCOPE))
        .requestScopes(Scope(GoogleDriveConfig.DRIVE_READONLY_SCOPE))
        .build()

    private val signInClient: GoogleSignInClient = GoogleSignIn.getClient(context, signInOptions)

    fun getSignInIntent(preferredEmail: String? = null): Intent {
        val options = preferredEmail?.trim()?.takeIf { it.isNotEmpty() }?.let { email ->
            GoogleSignInOptions.Builder(signInOptions)
                .setAccountName(email)
                .build()
        } ?: signInOptions
        return GoogleSignIn.getClient(context, options).signInIntent
    }

    fun getDeviceGoogleAccountEmails(): List<String> {
        @Suppress("DEPRECATION")
        val fromDevice = AccountManager.get(context)
            .getAccountsByType("com.google")
            .mapNotNull { account -> account.name?.trim()?.takeIf { it.isNotEmpty() } }
        val current = getAccountEmail()?.trim().orEmpty()
        return (fromDevice + listOfNotNull(current.takeIf { it.isNotEmpty() }))
            .distinct()
            .sortedBy { it.lowercase() }
    }

    suspend fun signOutIfCurrentNot(email: String) {
        val current = getAccountEmail()?.trim().orEmpty()
        if (current.isNotEmpty() && !current.equals(email.trim(), ignoreCase = true)) {
            signOut()
        }
    }

    fun getSignInIntent(): Intent = getSignInIntent(preferredEmail = null)

    fun hasSession(): Boolean = GoogleSignIn.getLastSignedInAccount(context) != null

    fun getAccountEmail(): String? = GoogleSignIn.getLastSignedInAccount(context)?.email

    fun getAccountLabel(): String? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        return account.displayName?.takeIf { it.isNotBlank() } ?: account.email
    }

    suspend fun handleSignInResult(data: Intent?): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            Tasks.await(task)
        }.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { error ->
                if (error is ApiException && error.statusCode == 12501) {
                    Result.failure(IllegalStateException("google_drive_auth_denied"))
                } else {
                    Result.failure(IllegalStateException("google_drive_auth_failed"))
                }
            },
        )
    }

    suspend fun getAccessToken(): String = withContext(Dispatchers.IO) {
        if (!GoogleDriveConfig.isConfigured()) {
            throw IllegalStateException("google_drive_not_configured")
        }
        val account = GoogleSignIn.getLastSignedInAccount(context)
            ?: throw IllegalStateException("google_drive_auth_failed")
        val scopes = buildString {
            append("oauth2:")
            append(GoogleDriveConfig.DRIVE_FILE_SCOPE)
            append(" ")
            append(GoogleDriveConfig.DRIVE_METADATA_READONLY_SCOPE)
            append(" ")
            append(GoogleDriveConfig.DRIVE_READONLY_SCOPE)
            append(" https://www.googleapis.com/auth/userinfo.email")
            append(" https://www.googleapis.com/auth/userinfo.profile")
        }
        val googleAccount = account.account
        val email = account.email?.takeIf { it.isNotBlank() }
        if (googleAccount == null && email == null) {
            throw IllegalStateException("google_drive_auth_failed")
        }
        try {
            when {
                googleAccount != null -> GoogleAuthUtil.getToken(context, googleAccount, scopes)
                else -> GoogleAuthUtil.getToken(context, email!!, scopes)
            }
        } catch (error: UserRecoverableAuthException) {
            throw IllegalStateException("google_drive_auth_failed", error)
        } catch (error: GoogleAuthException) {
            throw IllegalStateException("google_drive_auth_failed", error)
        }
    }

    suspend fun signOut() {
        withContext(Dispatchers.IO) {
            Tasks.await(signInClient.signOut())
        }
    }
}
