package org.cyblight.android.util

import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.cyblight.android.BuildConfig
import org.cyblight.android.data.api.EasterFlagsDto
import org.cyblight.android.data.preferences.AppPreferences
import java.util.TimeZone
import java.util.concurrent.TimeUnit

object EasterLogger {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val logUrl: String
        get() = "${BuildConfig.WEBSITE_URL.trimEnd('/')}/e-log"

    private val androidEggFlags: List<Pair<String, (EasterFlagsDto) -> Boolean>> = listOf(
        EasterLogKey.LIGHT_CATCHER to EasterFlagsDto::lightCatcher,
        EasterLogKey.NIGHT_GUARD to EasterFlagsDto::nightGuard,
        EasterLogKey.TRUSTED_FINGERPRINT to EasterFlagsDto::trustedFingerprint,
        EasterLogKey.ECHO to EasterFlagsDto::echo,
        EasterLogKey.ARCHIVIST to EasterFlagsDto::archivist,
        EasterLogKey.BRIDGE to EasterFlagsDto::bridge,
    )

    suspend fun syncLoggedFromServerFlags(
        appPreferences: AppPreferences,
        userName: String,
        flags: EasterFlagsDto,
    ) {
        androidEggFlags.forEach { (eggKey, isUnlocked) ->
            if (isUnlocked(flags)) {
                appPreferences.markEasterTelegramLogged(userName, eggKey)
            }
        }
    }

    fun logLightCatcher(appPreferences: AppPreferences, userName: String) {
        logOnce(appPreferences, userName, EasterLogKey.LIGHT_CATCHER) {
            log(
                userName = userName,
                type = "light_catcher",
                source = "android_version_seven_taps",
                route = "android/settings",
                page = "cyblight-android://settings/light-catcher",
            )
        }
    }

    fun logNightGuard(appPreferences: AppPreferences, userName: String) {
        logOnce(appPreferences, userName, EasterLogKey.NIGHT_GUARD) {
            log(
                userName = userName,
                type = "night_guard",
                source = "android_dark_theme_after_midnight",
                route = "android/main",
                page = "cyblight-android://main/night-guard",
            )
        }
    }

    fun logTrustedFingerprint(appPreferences: AppPreferences, userName: String) {
        logOnce(appPreferences, userName, EasterLogKey.TRUSTED_FINGERPRINT) {
            log(
                userName = userName,
                type = "trusted_fingerprint",
                source = "android_biometric_unlock_100",
                route = "android/app-lock",
                page = "cyblight-android://app-lock/trusted-fingerprint",
            )
        }
    }

    fun logEcho(appPreferences: AppPreferences, userName: String) {
        logOnce(appPreferences, userName, EasterLogKey.ECHO) {
            log(
                userName = userName,
                type = "echo",
                source = "android_message_at_2359",
                route = "android/chat",
                page = "cyblight-android://chat/echo",
            )
        }
    }

    fun logArchivist(appPreferences: AppPreferences, userName: String) {
        logOnce(appPreferences, userName, EasterLogKey.ARCHIVIST) {
            log(
                userName = userName,
                type = "archivist",
                source = "android_chat_tools_combo",
                route = "android/chat",
                page = "cyblight-android://chat/archivist",
            )
        }
    }

    fun logBridge(appPreferences: AppPreferences, userName: String) {
        logOnce(appPreferences, userName, EasterLogKey.BRIDGE) {
            log(
                userName = userName,
                type = "bridge",
                source = "android_app_web_same_day",
                route = "android/bridge",
                page = "cyblight-android://bridge",
            )
        }
    }

    private fun logOnce(
        appPreferences: AppPreferences,
        userName: String,
        eggKey: String,
        send: () -> Unit,
    ) {
        scope.launch {
            if (appPreferences.isEasterTelegramLogged(userName, eggKey)) return@launch
            send()
            appPreferences.markEasterTelegramLogged(userName, eggKey)
        }
    }

    private fun log(
        userName: String,
        type: String,
        source: String,
        route: String,
        page: String,
    ) {
        val payload = mapOf(
            "type" to type,
            "userName" to userName,
            "source" to source,
            "route" to route,
            "page" to page,
            "timezone" to TimeZone.getDefault().id,
            "ua" to AppUserAgent.build(),
            "referrer" to null,
        )

        runCatching {
            val body = gson.toJson(payload)
                .toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(logUrl)
                .post(body)
                .header("Origin", BuildConfig.WEBSITE_URL)
                .header("User-Agent", AppUserAgent.build())
                .build()
            client.newCall(request).execute().close()
        }
    }
}

private object EasterLogKey {
    const val LIGHT_CATCHER = "light_catcher"
    const val NIGHT_GUARD = "night_guard"
    const val TRUSTED_FINGERPRINT = "trusted_fingerprint"
    const val ECHO = "echo"
    const val ARCHIVIST = "archivist"
    const val BRIDGE = "bridge"
}
