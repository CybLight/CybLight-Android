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
import org.cyblight.android.util.AppUserAgent
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

    fun logLightCatcher(userName: String) {
        val payload = mapOf(
            "type" to "light_catcher",
            "userName" to userName,
            "source" to "android_version_seven_taps",
            "route" to "android/settings",
            "page" to "cyblight-android://settings/light-catcher",
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
}
