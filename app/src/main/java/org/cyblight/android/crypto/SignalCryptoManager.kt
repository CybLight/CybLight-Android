package org.cyblight.android.crypto

import android.content.Context
import android.util.Base64
import org.cyblight.android.data.api.CybLightApi
import org.cyblight.android.data.api.MessageDto
import org.cyblight.android.data.api.SignalKeyBundleDto
import org.cyblight.android.data.api.SignalPublicPreKeyDto
import org.cyblight.android.data.api.SignalRegisterKeysRequest
import org.cyblight.android.data.api.SignalReplenishPreKeysRequest
import org.cyblight.android.data.api.SignalSignedPreKeyDto
import org.signal.libsignal.protocol.IdentityKey
import org.signal.libsignal.protocol.IdentityKeyPair
import org.signal.libsignal.protocol.SessionBuilder
import org.signal.libsignal.protocol.SessionCipher
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.ecc.ECKeyPair
import org.signal.libsignal.protocol.ecc.ECPublicKey
import org.signal.libsignal.protocol.kem.KEMKeyPair
import org.signal.libsignal.protocol.kem.KEMKeyType
import org.signal.libsignal.protocol.kem.KEMPublicKey
import org.signal.libsignal.protocol.message.CiphertextMessage
import org.signal.libsignal.protocol.message.PreKeySignalMessage
import org.signal.libsignal.protocol.message.SignalMessage
import org.signal.libsignal.protocol.state.KyberPreKeyRecord
import org.signal.libsignal.protocol.state.PreKeyBundle
import org.signal.libsignal.protocol.state.PreKeyRecord
import org.signal.libsignal.protocol.state.SignedPreKeyRecord
import org.signal.libsignal.protocol.state.impl.InMemorySignalProtocolStore
import org.signal.libsignal.protocol.util.KeyHelper
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

data class EncryptedPayload(
    val content: String,
    val signalType: Int,
    val registrationId: Int,
)

class SignalCryptoManager(
    context: Context,
    private val api: CybLightApi,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val lock = ReentrantLock()
    private var nextPreKeyId = (System.currentTimeMillis() / 1000L).toInt()

    suspend fun ensureRegistered(userId: String) {
        val status = api.signalKeyStatus()
        val localRegistrationId = prefs.getInt(key(userId, "registration_id"), 0)
        val localIdentity = prefs.getString(key(userId, "identity_serialized"), null)

        if (status.ok && status.registered && localRegistrationId > 0 && !localIdentity.isNullOrBlank()) {
            if (status.unusedOneTimePreKeys < REPLENISH_THRESHOLD) {
                val store = loadStore(userId) ?: return
                val batch = generateOneTimePreKeys(store, ONE_TIME_PREKEY_BATCH)
                api.replenishSignalPreKeys(SignalReplenishPreKeysRequest(batch))
            }
            return
        }

        val registrationId = KeyHelper.generateRegistrationId(false)
        val identityKeyPair = IdentityKeyPair.generate()
        val signedPreKeyId = nextKeyId()
        val signedPreKey = generateSignedPreKey(identityKeyPair, signedPreKeyId)
        val kyberPreKeyId = nextKeyId()
        val kyberPreKey = generateKyberPreKey(identityKeyPair, kyberPreKeyId)
        val store = InMemorySignalProtocolStore(identityKeyPair, registrationId)
        val oneTimePreKeys = generateOneTimePreKeys(store, ONE_TIME_PREKEY_BATCH)

        saveIdentity(userId, registrationId, identityKeyPair)
        store.storeSignedPreKey(signedPreKeyId, signedPreKey)
        store.storeKyberPreKey(kyberPreKeyId, kyberPreKey)

        api.registerSignalKeys(
            SignalRegisterKeysRequest(
                registrationId = registrationId,
                identityKey = base64Encode(identityKeyPair.publicKey.serialize()),
                signedPreKey = SignalSignedPreKeyDto(
                    keyId = signedPreKeyId,
                    publicKey = base64Encode(signedPreKey.keyPair.publicKey.serialize()),
                    signature = base64Encode(signedPreKey.signature),
                ),
                kyberPreKey = SignalSignedPreKeyDto(
                    keyId = kyberPreKeyId,
                    publicKey = base64Encode(kyberPreKey.keyPair.publicKey.serialize()),
                    signature = base64Encode(kyberPreKey.signature),
                ),
                oneTimePreKeys = oneTimePreKeys,
            ),
        )
    }

    suspend fun encryptMessage(userId: String, recipientId: String, plaintext: String): EncryptedPayload {
        ensureRegistered(userId)
        val store = loadStore(userId) ?: throw IllegalStateException("signal_store_missing")
        val address = SignalProtocolAddress(recipientId, DEVICE_ID)

        if (!store.containsSession(address)) {
            val bundleResponse = api.signalKeyBundle(recipientId)
            if (!bundleResponse.ok || bundleResponse.bundle == null) {
                throw IllegalStateException(bundleResponse.error ?: "key_bundle_failed")
            }
            SessionBuilder(store, address).process(bundleResponse.bundle.toPreKeyBundle())
        }

        val cipher = SessionCipher(store, address)
        val ciphertext = cipher.encrypt(plaintext.toByteArray(Charsets.UTF_8))

        return EncryptedPayload(
            content = base64Encode(ciphertext.serialize()),
            signalType = ciphertext.type,
            registrationId = store.localRegistrationId,
        )
    }

    suspend fun decryptMessage(userId: String, message: MessageDto): String {
        if (message.encryption != "signal_v1") {
            return message.content
        }

        val store = loadStore(userId) ?: throw IllegalStateException("signal_store_missing")
        val address = SignalProtocolAddress(message.senderId, DEVICE_ID)
        val cipher = SessionCipher(store, address)
        val raw = base64Decode(message.content)
        val signalType = message.signalType ?: throw IllegalStateException("unsupported_signal_type")

        val plaintext = when (signalType) {
            CiphertextMessage.PREKEY_TYPE -> cipher.decrypt(PreKeySignalMessage(raw))
            CiphertextMessage.WHISPER_TYPE -> cipher.decrypt(SignalMessage(raw))
            else -> throw IllegalStateException("unsupported_signal_type")
        }

        return String(plaintext, Charsets.UTF_8)
    }

    suspend fun decryptMessages(userId: String, messages: List<MessageDto>): List<MessageDto> {
        return messages.map { message ->
            runCatching { decryptMessage(userId, message) }
                .fold(
                    onSuccess = { message.copy(content = it) },
                    onFailure = { message.copy(content = "🔒 Не удалось расшифровать сообщение") },
                )
        }
    }

    private fun loadStore(userId: String): InMemorySignalProtocolStore? {
        return lock.withLock {
            val registrationId = prefs.getInt(key(userId, "registration_id"), 0)
            val serialized = prefs.getString(key(userId, "identity_serialized"), null) ?: return null
            if (registrationId <= 0) return null
            val identityKeyPair = IdentityKeyPair(base64Decode(serialized))
            InMemorySignalProtocolStore(identityKeyPair, registrationId)
        }
    }

    private fun saveIdentity(userId: String, registrationId: Int, identityKeyPair: IdentityKeyPair) {
        prefs.edit()
            .putInt(key(userId, "registration_id"), registrationId)
            .putString(key(userId, "identity_serialized"), base64Encode(identityKeyPair.serialize()))
            .apply()
    }

    private fun generateOneTimePreKeys(
        store: InMemorySignalProtocolStore,
        count: Int,
    ): List<SignalPublicPreKeyDto> {
        val startId = nextKeyId()
        val records = generatePreKeys(startId, count)
        val out = ArrayList<SignalPublicPreKeyDto>(records.size)
        records.forEach { record ->
            store.storePreKey(record.id, record)
            out.add(
                SignalPublicPreKeyDto(
                    keyId = record.id,
                    publicKey = base64Encode(record.keyPair.publicKey.serialize()),
                ),
            )
        }
        nextPreKeyId = startId + count
        return out
    }

    private fun SignalKeyBundleDto.toPreKeyBundle(): PreKeyBundle {
        val preKeyId = oneTimePreKey?.keyId ?: PreKeyBundle.NULL_PRE_KEY_ID
        val preKeyPublic = oneTimePreKey?.publicKey?.let { ECPublicKey(base64Decode(it)) }
        val kyber = kyberPreKey ?: throw IllegalStateException("kyber_prekey_missing")
        return PreKeyBundle(
            registrationId,
            deviceId,
            preKeyId,
            preKeyPublic,
            signedPreKey.keyId,
            ECPublicKey(base64Decode(signedPreKey.publicKey)),
            base64Decode(signedPreKey.signature),
            IdentityKey(ECPublicKey(base64Decode(identityKey))),
            kyber.keyId,
            KEMPublicKey(base64Decode(kyber.publicKey)),
            base64Decode(kyber.signature),
        )
    }

    private fun generateSignedPreKey(identityKeyPair: IdentityKeyPair, signedPreKeyId: Int): SignedPreKeyRecord {
        val keyPair = ECKeyPair.generate()
        val signature = identityKeyPair.privateKey.calculateSignature(keyPair.publicKey.serialize())
        return SignedPreKeyRecord(signedPreKeyId, System.currentTimeMillis(), keyPair, signature)
    }

    private fun generateKyberPreKey(identityKeyPair: IdentityKeyPair, kyberPreKeyId: Int): KyberPreKeyRecord {
        val keyPair = KEMKeyPair.generate(KEMKeyType.KYBER_1024)
        val signature = identityKeyPair.privateKey.calculateSignature(keyPair.publicKey.serialize())
        return KyberPreKeyRecord(kyberPreKeyId, System.currentTimeMillis(), keyPair, signature)
    }

    private fun generatePreKeys(startId: Int, count: Int): List<PreKeyRecord> {
        return (0 until count).map { offset ->
            PreKeyRecord(startId + offset, ECKeyPair.generate())
        }
    }

    private fun nextKeyId(): Int {
        nextPreKeyId += 1
        return nextPreKeyId
    }

    private fun key(userId: String, suffix: String): String = "signal_${userId}_$suffix"

    private fun base64Encode(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.NO_WRAP)

    private fun base64Decode(value: String): ByteArray =
        Base64.decode(value, Base64.NO_WRAP)

    companion object {
        private const val PREFS_NAME = "cyblight_signal_crypto"
        private const val DEVICE_ID = 1
        private const val ONE_TIME_PREKEY_BATCH = 100
        private const val REPLENISH_THRESHOLD = 20
    }
}
