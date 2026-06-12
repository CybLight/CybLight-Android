package org.cyblight.android.auth

import android.app.Activity
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import org.cyblight.android.data.api.PasskeyAllowCredential
import org.cyblight.android.data.api.PasskeyAssertionResponse
import org.cyblight.android.data.api.PasskeyAttestationResponse
import org.cyblight.android.data.api.PasskeyCreationOptions
import org.cyblight.android.data.api.PasskeyCredentialPayload
import org.cyblight.android.data.api.PasskeyPublicKeyOptions
import org.cyblight.android.data.api.PasskeyRegistrationPayload
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

    suspend fun createCredential(
        activity: Activity,
        options: PasskeyCreationOptions,
    ): PasskeyRegistrationPayload {
        val requestJson = buildCreationRequestJson(options)
        val request = CreatePublicKeyCredentialRequest(requestJson)
        val credentialManager = CredentialManager.create(activity)

        val result = try {
            credentialManager.createCredential(activity, request)
        } catch (error: CreateCredentialCancellationException) {
            throw PasskeyAuthException("cancelled")
        } catch (error: CreateCredentialException) {
            throw PasskeyAuthException("passkey_failed")
        }

        val publicKeyCredential = result as? CreatePublicKeyCredentialResponse
            ?: throw PasskeyAuthException("invalid_response")

        return parseRegistrationResponse(publicKeyCredential.registrationResponseJson)
    }

    private fun buildCreationRequestJson(options: PasskeyCreationOptions): String {
        val json = JSONObject()
        json.put("challenge", options.challenge)
        json.put("timeout", options.timeout)
        json.put("attestation", options.attestation)

        json.put("rp", JSONObject().apply {
            put("name", options.rp.name)
            put("id", options.rp.id)
        })

        json.put("user", JSONObject().apply {
            put("id", options.user.id)
            put("name", options.user.name)
            put("displayName", options.user.displayName)
        })

        json.put("pubKeyCredParams", JSONArray().apply {
            options.pubKeyCredParams.forEach { param ->
                put(JSONObject().apply {
                    put("type", param.type)
                    put("alg", param.alg)
                })
            }
        })

        options.excludeCredentials?.takeIf { it.isNotEmpty() }?.let { credentials ->
            json.put("excludeCredentials", JSONArray().apply {
                credentials.forEach { put(it.toJson()) }
            })
        }

        options.authenticatorSelection?.let { selection ->
            json.put("authenticatorSelection", JSONObject().apply {
                selection.authenticatorAttachment?.let { put("authenticatorAttachment", it) }
                selection.requireResidentKey?.let { put("requireResidentKey", it) }
                selection.residentKey?.let { put("residentKey", it) }
                selection.userVerification?.let { put("userVerification", it) }
            })
        }

        return json.toString()
    }

    private fun parseRegistrationResponse(responseJson: String): PasskeyRegistrationPayload {
        val root = JSONObject(responseJson)
        val response = root.getJSONObject("response")
        return PasskeyRegistrationPayload(
            id = root.getString("id"),
            rawId = root.optString("rawId", root.getString("id")),
            response = PasskeyAttestationResponse(
                clientDataJSON = response.getString("clientDataJSON"),
                attestationObject = response.getString("attestationObject"),
            ),
            type = root.optString("type", "public-key"),
        )
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
