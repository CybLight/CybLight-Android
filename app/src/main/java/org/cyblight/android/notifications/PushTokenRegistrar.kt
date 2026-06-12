package org.cyblight.android.notifications

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.cyblight.android.data.ApiClient
import org.cyblight.android.data.api.PushRegisterRequest
import org.cyblight.android.data.api.PushUnregisterRequest
import org.cyblight.android.data.preferences.AppPreferences
import org.cyblight.android.data.session.SessionManager
import org.cyblight.android.util.SystemSettings

object PushTokenRegistrar {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun isFirebaseConfigured(): Boolean = runCatching {
        FirebaseApp.getInstance()
        true
    }.getOrDefault(false)

    fun registerCurrentToken(context: Context) {
        if (!isFirebaseConfigured()) return
        scope.launch {
            runCatching {
                val token = FirebaseMessaging.getInstance().token.await()
                registerToken(context, token)
            }
        }
    }

    fun registerToken(context: Context, token: String) {
        if (!isFirebaseConfigured()) return
        val trimmed = token.trim()
        if (trimmed.isEmpty()) return

        scope.launch {
            val sessionManager = SessionManager(context)
            if (sessionManager.getToken().isNullOrBlank()) return@launch
            if (!SystemSettings.areNotificationsEnabled(context)) return@launch
            if (!AppPreferences(context).getMessageAlertsEnabled()) return@launch

            runCatching {
                val api = ApiClient.create(sessionManager)
                api.registerPushToken(PushRegisterRequest(token = trimmed, platform = "android"))
            }
        }
    }

    fun unregisterCurrentToken(context: Context) {
        if (!isFirebaseConfigured()) return
        scope.launch {
            val sessionManager = SessionManager(context)
            if (sessionManager.getToken().isNullOrBlank()) return@launch

            val token = runCatching {
                FirebaseMessaging.getInstance().token.await()
            }.getOrNull()

            runCatching {
                val api = ApiClient.create(sessionManager)
                api.unregisterPushToken(PushUnregisterRequest(token = token))
            }
        }
    }
}
