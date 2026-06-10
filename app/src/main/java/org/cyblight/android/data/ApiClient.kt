package org.cyblight.android.data

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.cyblight.android.BuildConfig
import org.cyblight.android.data.api.CybLightApi
import org.cyblight.android.data.session.SessionManager
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    fun create(sessionManager: SessionManager): CybLightApi {
        val authInterceptor = Interceptor { chain ->
            val token = runCatching {
                kotlinx.coroutines.runBlocking { sessionManager.getToken() }
            }.getOrNull()

            val request = if (!token.isNullOrBlank()) {
                chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            } else {
                chain.request()
            }
            chain.proceed(request)
        }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(ensureTrailingSlash(BuildConfig.API_BASE_URL))
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
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
