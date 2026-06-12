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

    fun log(
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

        scope.launch {
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

    fun logLightCatcher(userName: String) = log(
        userName = userName,
        type = "light_catcher",
        source = "android_version_seven_taps",
        route = "android/settings",
        page = "cyblight-android://settings/light-catcher",
    )

    fun logNightGuard(userName: String) = log(
        userName = userName,
        type = "night_guard",
        source = "android_dark_theme_after_midnight",
        route = "android/main",
        page = "cyblight-android://main/night-guard",
    )

    fun logTrustedFingerprint(userName: String) = log(
        userName = userName,
        type = "trusted_fingerprint",
        source = "android_biometric_unlock_100",
        route = "android/app-lock",
        page = "cyblight-android://app-lock/trusted-fingerprint",
    )

    fun logEcho(userName: String) = log(
        userName = userName,
        type = "echo",
        source = "android_message_at_2359",
        route = "android/chat",
        page = "cyblight-android://chat/echo",
    )

    fun logArchivist(userName: String) = log(
        userName = userName,
        type = "archivist",
        source = "android_chat_tools_combo",
        route = "android/chat",
        page = "cyblight-android://chat/archivist",
    )

    fun logBridge(userName: String) = log(
        userName = userName,
        type = "bridge",
        source = "android_app_web_same_day",
        route = "android/bridge",
        page = "cyblight-android://bridge",
    )
}
