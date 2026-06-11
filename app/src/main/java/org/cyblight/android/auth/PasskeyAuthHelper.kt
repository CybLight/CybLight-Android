package org.cyblight.android.auth

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import org.cyblight.android.data.api.PasskeyAllowCredential
import org.cyblight.android.data.api.PasskeyAssertionResponse
import org.cyblight.android.data.api.PasskeyCredentialPayload
import org.cyblight.android.data.api.PasskeyPublicKeyOptions
import org.json.JSONArray
import org.json.JSONObject

object PasskeyAuthHelper {
    suspend fun getCredential(
        activity: Activity,
        options: PasskeyPublicKeyOptions,
    ): PasskeyCredentialPayload {
        val requestJson = buildRequestJson(options)
        val credentialOption = GetPublicKeyCredentialOption(requestJson)
        val request = GetCredentialRequest(listOf(credentialOption))
        val credentialManager = CredentialManager.create(activity)

        val result = try {
            credentialManager.getCredential(activity, request)
        } catch (error: GetCredentialCancellationException) {
            throw PasskeyAuthException("cancelled")
        } catch (error: NoCredentialException) {
            throw PasskeyAuthException("no_passkey")
        } catch (error: GetCredentialException) {
            throw PasskeyAuthException("passkey_failed")
        }

        val publicKeyCredential = result.credential as? PublicKeyCredential
            ?: throw PasskeyAuthException("invalid_response")

        return parseAuthenticationResponse(publicKeyCredential.authenticationResponseJson)
    }

    private fun buildRequestJson(options: PasskeyPublicKeyOptions): String {
        val json = JSONObject()
        json.put("challenge", options.challenge)
        json.put("timeout", options.timeout)
        json.put("rpId", options.rpId)
        json.put("userVerification", options.userVerification)

        val allowCredentials = options.allowCredentials
        if (!allowCredentials.isNullOrEmpty()) {
            json.put("allowCredentials", JSONArray().apply {
                allowCredentials.forEach { credential ->
                    put(credential.toJson())
                }
            })
        }

        return json.toString()
    }

    private fun PasskeyAllowCredential.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("type", type)
        transports?.takeIf { it.isNotEmpty() }?.let { items ->
            put("transports", JSONArray().apply { items.forEach { put(it) } })
        }
    }

    private fun parseAuthenticationResponse(responseJson: String): PasskeyCredentialPayload {
        val root = JSONObject(responseJson)
        val response = root.getJSONObject("response")
        return PasskeyCredentialPayload(
            id = root.getString("id"),
            rawId = root.optString("rawId", root.getString("id")),
            response = PasskeyAssertionResponse(
                clientDataJSON = response.getString("clientDataJSON"),
                authenticatorData = response.getString("authenticatorData"),
                signature = response.getString("signature"),
                userHandle = response.optString("userHandle").takeIf { it.isNotBlank() },
            ),
            type = root.optString("type", "public-key"),
        )
    }
}

class PasskeyAuthException(val code: String) : Exception(code)
