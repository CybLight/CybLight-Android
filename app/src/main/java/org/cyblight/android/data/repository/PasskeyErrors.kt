package org.cyblight.android.data.repository

import com.google.gson.Gson
import org.cyblight.android.auth.PasskeyAuthException
import org.cyblight.android.data.api.ApiEnvelope
import retrofit2.HttpException

private val gson = Gson()

fun Throwable.toPasskeyErrorCode(): String {
    if (this is PasskeyAuthException) {
        return code
    }
    if (this is IllegalStateException && !message.isNullOrBlank()) {
        return message!!
    }
    if (this is HttpException) {
        val body = response()?.errorBody()?.string()
        if (!body.isNullOrBlank()) {
            runCatching {
                val envelope = gson.fromJson(body, ApiEnvelope::class.java)
                if (!envelope.error.isNullOrBlank()) {
                    return envelope.error!!
                }
            }
        }
    }
    return "passkey_register_failed"
}
