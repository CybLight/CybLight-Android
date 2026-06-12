package org.cyblight.android.data

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.cyblight.android.BuildConfig
import org.cyblight.android.data.api.CybLightApi
import org.cyblight.android.data.api.createApiGson
import org.cyblight.android.data.session.SessionManager
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val MOBILE_CLIENT_HEADER = "X-CybLight-Client"
    private const val MOBILE_CLIENT_VALUE = "android"

    fun create(sessionManager: SessionManager): CybLightApi {
        val authInterceptor = Interceptor { chain ->
            val token = runCatching {
                kotlinx.coroutines.runBlocking { sessionManager.getToken() }
            }.getOrNull()

            val builder = chain.request().newBuilder()
                .addHeader(MOBILE_CLIENT_HEADER, MOBILE_CLIENT_VALUE)
                .addHeader("Origin", org.cyblight.android.BuildConfig.WEBSITE_URL)

            val request = if (!token.isNullOrBlank()) {
                builder.addHeader("Authorization", "Bearer $token").build()
            } else {
                builder.build()
            }
            chain.proceed(request)
        }

        val sessionInterceptor = Interceptor { chain ->
            val response = chain.proceed(chain.request())

            extractAuthToken(response.headers("Set-Cookie"))
                ?.takeIf { it.isNotBlank() }
                ?.let { newToken ->
                    runCatching {
                        kotlinx.coroutines.runBlocking { sessionManager.updateToken(newToken) }
                    }
                }

            val path = chain.request().url.encodedPath
            val isLoginAttempt = path.contains("/auth/login") ||
                path.contains("/auth/2fa") ||
                path.contains("/auth/passkey")

            if (!isLoginAttempt && (response.code == 401 || response.code == 403)) {
                runCatching {
                    kotlinx.coroutines.runBlocking { sessionManager.clearAndNotifyExpired() }
                }
            }
            response
        }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(sessionInterceptor)
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(ensureTrailingSlash(BuildConfig.API_BASE_URL))
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(createApiGson()))
            .build()
            .create(CybLightApi::class.java)
    }

    private fun ensureTrailingSlash(url: String): String =
        if (url.endsWith("/")) url else "$url/"
}

fun extractAuthToken(setCookieHeaders: List<String>): String? {
    for (header in setCookieHeaders) {
        val part = header.split(";").firstOrNull { it.trim().startsWith("cyb_auth=") } ?: continue
        return part.substringAfter("cyb_auth=").trim()
    }
    return null
}
