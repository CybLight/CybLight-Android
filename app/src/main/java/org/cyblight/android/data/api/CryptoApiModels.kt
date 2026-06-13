package org.cyblight.android.data.api

import com.google.gson.annotations.SerializedName

data class SignalPublicPreKeyDto(
    @SerializedName("keyId") val keyId: Int = 0,
    @SerializedName("publicKey") val publicKey: String = "",
)

data class SignalSignedPreKeyDto(
    @SerializedName("keyId") val keyId: Int = 0,
    @SerializedName("publicKey") val publicKey: String = "",
    val signature: String = "",
)

data class SignalKeyBundleDto(
    @SerializedName("userId") val userId: String = "",
    @SerializedName("deviceId") val deviceId: Int = 1,
    @SerializedName("registrationId") val registrationId: Int = 0,
    @SerializedName("identityKey") val identityKey: String = "",
    @SerializedName("signedPreKey") val signedPreKey: SignalSignedPreKeyDto = SignalSignedPreKeyDto(),
    @SerializedName("kyberPreKey") val kyberPreKey: SignalSignedPreKeyDto? = null,
    @SerializedName("oneTimePreKey") val oneTimePreKey: SignalPublicPreKeyDto? = null,
)

data class SignalKeyBundleResponse(
    val ok: Boolean = false,
    val bundle: SignalKeyBundleDto? = null,
    val error: String? = null,
)

data class SignalKeyStatusResponse(
    val ok: Boolean = false,
    val registered: Boolean = false,
    @SerializedName("registrationId") val registrationId: Int? = null,
    @SerializedName("identityKeyPublic") val identityKeyPublic: String? = null,
    @SerializedName("signedPreKeyId") val signedPreKeyId: Int? = null,
    @SerializedName("kyberPreKeyId") val kyberPreKeyId: Int? = null,
    @SerializedName("unusedOneTimePreKeys") val unusedOneTimePreKeys: Int = 0,
    @SerializedName("oldestUnusedPreKeyId") val oldestUnusedPreKeyId: Int? = null,
    @SerializedName("newestUnusedPreKeyId") val newestUnusedPreKeyId: Int? = null,
    val error: String? = null,
)

data class SignalRegisterKeysRequest(
    @SerializedName("registrationId") val registrationId: Int,
    @SerializedName("identityKey") val identityKey: String,
    @SerializedName("signedPreKey") val signedPreKey: SignalSignedPreKeyDto,
    @SerializedName("kyberPreKey") val kyberPreKey: SignalSignedPreKeyDto,
    @SerializedName("oneTimePreKeys") val oneTimePreKeys: List<SignalPublicPreKeyDto>,
)

data class SignalReplenishPreKeysRequest(
    @SerializedName("oneTimePreKeys") val oneTimePreKeys: List<SignalPublicPreKeyDto>,
)

data class SignalKeyActionResponse(
    val ok: Boolean = false,
    @SerializedName("oneTimePreKeysStored") val oneTimePreKeysStored: Int = 0,
    val error: String? = null,
)

data class EncryptedSendMessageRequest(
    @SerializedName("recipientId") val recipientId: String,
    val content: String,
    @SerializedName("signalType") val signalType: Int,
    @SerializedName("registrationId") val registrationId: Int,
)

data class EncryptedEditMessageRequest(
    val content: String,
    @SerializedName("signalType") val signalType: Int,
    @SerializedName("registrationId") val registrationId: Int,
)
