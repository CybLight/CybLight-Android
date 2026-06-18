package org.cyblight.android.crypto

import android.content.Context
import android.util.Log
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val persistence = SignalStorePersistence(context)
    private val decryptCache = DecryptCache(context)
    private val plaintextSync = MessagePlaintextSyncService(context, api)
    private val lock = ReentrantLock()
    private val ensureMutex = Mutex()
    private var nextPreKeyId = (System.currentTimeMillis() / 1000L).toInt()
    private var prekeysAlignedForUser: String? = null
    private var lastEnsureAtMs: Long = 0L
    private var lastEnsureUserId: String? = null

    suspend fun ensureRegistered(userId: String) {
        ensureMutex.withLock {
            ensureRegisteredInner(userId)
        }
    }

    suspend fun cacheSentPlaintext(userId: String, messageId: String, plaintext: String) {
        decryptCache.write(userId, messageId, plaintext)
        plaintextSync.push(userId, messageId, plaintext)
    }

    suspend fun encryptMessage(userId: String, recipientId: String, plaintext: String): EncryptedPayload {
        ensureRegistered(userId)
        val ctx = requireContext(userId)
        val address = SignalProtocolAddress(recipientId, DEVICE_ID)

        var ciphertext = encryptForPeer(ctx, address, recipientId, plaintext)

        if (
            ciphertext.type == CiphertextMessage.WHISPER_TYPE &&
            !ctx.manifest.preKeyHandshakePeers.contains(recipientId)
        ) {
            resetPeerSession(ctx, address, recipientId)
            establishSession(ctx, address, recipientId)
            ciphertext = encryptForPeer(ctx, address, recipientId, plaintext)
        }

        persistence.trackSession(ctx, address)
        persistence.persistSessionUpdate(ctx, address)

        return EncryptedPayload(
            content = SignalStorePersistence.base64Encode(ciphertext.serialize()),
            signalType = ciphertext.type,
            registrationId = ctx.manifest.registrationId,
        )
    }

    private suspend fun encryptForPeer(
        ctx: SignalStoreContext,
        address: SignalProtocolAddress,
        recipientId: String,
        plaintext: String,
    ): CiphertextMessage {
        if (!ctx.store.containsSession(address)) {
            establishSession(ctx, address, recipientId)
        }
        return SessionCipher(ctx.store, address).encrypt(plaintext.toByteArray(Charsets.UTF_8))
    }

    private suspend fun establishSession(
        ctx: SignalStoreContext,
        address: SignalProtocolAddress,
        recipientId: String,
    ) {
        val bundleResponse = api.signalKeyBundle(recipientId)
        if (!bundleResponse.ok || bundleResponse.bundle == null) {
            throw IllegalStateException(bundleResponse.error ?: "key_bundle_failed")
        }
        SessionBuilder(ctx.store, address).process(bundleResponse.bundle.toPreKeyBundle())
        persistence.trackSession(ctx, address)
    }

    private fun resetPeerSession(
        ctx: SignalStoreContext,
        address: SignalProtocolAddress,
        recipientId: String,
    ) {
        if (ctx.store.containsSession(address)) {
            ctx.store.deleteSession(address)
        }
        ctx.manifest.sessionKeys.remove(SignalStorePersistence.sessionAddressKey(address))
        ctx.manifest.preKeyHandshakePeers.remove(recipientId)
    }

    suspend fun decryptMessage(userId: String, message: MessageDto): String {
        ensureRegistered(userId)
        val ctx = requireContext(userId)
        if (message.id.isNotBlank()) {
            prefetchPlaintextSync(userId, listOf(message.id))
        }
        return decryptIncomingMessage(userId, message, ctx)
    }

    suspend fun decryptMessages(userId: String, messages: List<MessageDto>): List<MessageDto> {
        ensureRegistered(userId)
        val ctx = requireContext(userId)

        val decryptOrder = messages.sortedBy { messageSortKey(it) }
        val decryptedByKey = LinkedHashMap<String, String>()

        val syncCandidateIds = decryptOrder.mapNotNull { message ->
            message.id.takeIf {
                it.isNotBlank() &&
                    message.encryption == "signal_v1" &&
                    decryptCache.read(userId, it) == null
            }
        }
        prefetchPlaintextSync(userId, syncCandidateIds)

        for (message in decryptOrder) {
            val key = messageKey(message)
            if (decryptedByKey.containsKey(key)) continue
            val content = runCatching { decryptIncomingMessage(userId, message, ctx) }
                .getOrElse { failed ->
                    if (message.id.isNotBlank()) {
                        prefetchPlaintextSync(userId, listOf(message.id))
                        decryptCache.read(userId, message.id)
                    } else {
                        null
                    } ?: "🔒 Не удалось расшифровать сообщение"
                }
            decryptedByKey[key] = content
        }

        return messages.map { message ->
            message.copy(content = decryptedByKey[messageKey(message)] ?: message.content)
        }
    }

    private suspend fun decryptIncomingMessage(
        userId: String,
        message: MessageDto,
        ctx: SignalStoreContext,
    ): String {
        if (message.encryption != "signal_v1") {
            return message.content
        }

        if (message.senderId == userId) {
            if (message.id.isNotBlank()) {
                decryptCache.read(userId, message.id)?.let { return it }
            }
            return "🔒 Сообщение отправлено"
        }

        if (message.id.isNotBlank()) {
            decryptCache.read(userId, message.id)?.let { return it }
        }

        val address = SignalProtocolAddress(message.senderId, DEVICE_ID)
        val cipher = SessionCipher(ctx.store, address)
        val raw = SignalStorePersistence.base64Decode(message.content)
        val signalType = message.signalType ?: throw IllegalStateException("unsupported_signal_type")

        val plaintext = try {
            when (signalType) {
                CiphertextMessage.PREKEY_TYPE -> cipher.decrypt(PreKeySignalMessage(raw))
                CiphertextMessage.WHISPER_TYPE -> cipher.decrypt(SignalMessage(raw))
                else -> throw IllegalStateException("unsupported_signal_type")
            }
        } catch (error: Exception) {
            if (
                signalType == CiphertextMessage.WHISPER_TYPE &&
                ctx.store.containsSession(address)
            ) {
                resetPeerSession(ctx, address, message.senderId)
                persistence.persistSessionUpdate(ctx, address)
            }
            throw error
        }

        persistence.trackSession(ctx, address)
        ctx.manifest.preKeyHandshakePeers.add(message.senderId)
        persistence.persistSessionUpdate(ctx, address)

        val text = String(plaintext, Charsets.UTF_8)
        if (message.id.isNotBlank()) {
            decryptCache.write(userId, message.id, text)
            plaintextSync.push(userId, message.id, text)
        }
        return text
    }

    private suspend fun prefetchPlaintextSync(userId: String, messageIds: List<String>) {
        val missing = messageIds.filter { decryptCache.read(userId, it) == null }
        if (missing.isEmpty()) return
        val remote = plaintextSync.fetchBatch(userId, missing)
        remote.forEach { (messageId, plaintext) ->
            decryptCache.write(userId, messageId, plaintext)
        }
    }

    private suspend fun ensureRegisteredInner(userId: String) {
        val now = System.currentTimeMillis()
        if (prekeysAlignedForUser == userId &&
            lastEnsureUserId == userId &&
            now - lastEnsureAtMs < ENSURE_STATUS_TTL_MS &&
            lock.withLock { persistence.loadContext(userId) } != null
        ) {
            return
        }

        val status = api.signalKeyStatus()
        lastEnsureAtMs = now
        lastEnsureUserId = userId
        var ctx = lock.withLock { persistence.loadContext(userId) }

        if (ctx == null) {
            ctx = lock.withLock { persistence.loadLegacyIdentityContext(userId) }
        }

        if (ctx == null) {
            val registrationId = KeyHelper.generateRegistrationId(false)
            val identityKeyPair = IdentityKeyPair.generate()
            val manifest = MutableSignalStoreManifest(
                registrationId = registrationId,
                identitySerialized = SignalStorePersistence.base64Encode(identityKeyPair.serialize()),
            )
            val store = InMemorySignalProtocolStore(identityKeyPair, registrationId)
            ctx = SignalStoreContext(userId, store, manifest)
            publishRegistrationKeys(ctx)
            prekeysAlignedForUser = userId
            return
        }

        val synced = isServerLocalKeySync(status, ctx)
        val signedPresent = ctx.manifest.latestSignedPreKeyId?.let { ctx.store.containsSignedPreKey(it) } == true
        val kyberPresent = ctx.manifest.latestKyberPreKeyId?.let { ctx.store.containsKyberPreKey(it) } == true

        if (!synced || !signedPresent || !kyberPresent) {
            publishRegistrationKeys(ctx)
            prekeysAlignedForUser = userId
            return
        }

        val unused = status.unusedOneTimePreKeys
        val oldestMissingLocally = status.oldestUnusedPreKeyId?.let { keyId ->
            !persistence.hasLocalPreKey(ctx, keyId)
        } == true
        val serverHasUnknownPrekeys = serverHasPrekeysOutsideLocal(ctx, status)

        if (oldestMissingLocally || serverHasUnknownPrekeys) {
            Log.w(TAG, "Server prekeys not all present locally; skipping replenish")
        } else if (unused < REPLENISH_THRESHOLD) {
            replenishOneTimePreKeys(ctx)
        }
        prekeysAlignedForUser = userId
    }

    private suspend fun publishRegistrationKeys(ctx: SignalStoreContext) {
        ctx.manifest.sessionKeys.forEach { sessionKey ->
            SignalStorePersistence.parseSessionAddress(sessionKey)?.let { address ->
                if (ctx.store.containsSession(address)) {
                    ctx.store.deleteSession(address)
                }
            }
        }
        ctx.manifest.sessionKeys.clear()
        ctx.manifest.preKeyHandshakePeers.clear()

        val signedPreKeyId = nextKeyId()
        val signedPreKey = generateSignedPreKey(ctx.store.identityKeyPair, signedPreKeyId)
        val kyberPreKeyId = nextKeyId()
        val kyberPreKey = generateKyberPreKey(ctx.store.identityKeyPair, kyberPreKeyId)
        val oneTimePreKeys = generateOneTimePreKeys(ctx, ONE_TIME_PREKEY_BATCH)

        ctx.store.storeSignedPreKey(signedPreKeyId, signedPreKey)
        persistence.trackSignedPreKey(ctx, signedPreKeyId)
        ctx.store.storeKyberPreKey(kyberPreKeyId, kyberPreKey)
        persistence.trackKyberPreKey(ctx, kyberPreKeyId)

        api.registerSignalKeys(
            SignalRegisterKeysRequest(
                registrationId = ctx.manifest.registrationId,
                identityKey = SignalStorePersistence.base64Encode(ctx.store.identityKeyPair.publicKey.serialize()),
                signedPreKey = SignalSignedPreKeyDto(
                    keyId = signedPreKeyId,
                    publicKey = SignalStorePersistence.base64Encode(signedPreKey.keyPair.publicKey.serialize()),
                    signature = SignalStorePersistence.base64Encode(signedPreKey.signature),
                ),
                kyberPreKey = SignalSignedPreKeyDto(
                    keyId = kyberPreKeyId,
                    publicKey = SignalStorePersistence.base64Encode(kyberPreKey.keyPair.publicKey.serialize()),
                    signature = SignalStorePersistence.base64Encode(kyberPreKey.signature),
                ),
                oneTimePreKeys = oneTimePreKeys,
            ),
        )
        persistence.persistContext(ctx)
        prekeysAlignedForUser = ctx.userId
    }

    private suspend fun replenishOneTimePreKeys(ctx: SignalStoreContext) {
        val batch = generateOneTimePreKeys(ctx, ONE_TIME_PREKEY_BATCH)
        api.replenishSignalPreKeys(SignalReplenishPreKeysRequest(batch))
        persistence.persistContext(ctx)
    }

    private fun requireContext(userId: String): SignalStoreContext =
        lock.withLock {
            persistence.loadContext(userId)
                ?: throw IllegalStateException("signal_store_missing")
        }

    private fun generateOneTimePreKeys(
        ctx: SignalStoreContext,
        count: Int,
    ): List<SignalPublicPreKeyDto> {
        val startId = nextKeyId()
        val records = generatePreKeys(startId, count)
        val out = ArrayList<SignalPublicPreKeyDto>(records.size)
        records.forEach { record ->
            ctx.store.storePreKey(record.id, record)
            persistence.trackPreKey(ctx, record.id)
            out.add(
                SignalPublicPreKeyDto(
                    keyId = record.id,
                    publicKey = SignalStorePersistence.base64Encode(record.keyPair.publicKey.serialize()),
                ),
            )
        }
        nextPreKeyId = startId + count
        return out
    }

    private fun isServerLocalKeySync(
        status: org.cyblight.android.data.api.SignalKeyStatusResponse,
        ctx: SignalStoreContext,
    ): Boolean {
        if (!status.ok || !status.registered) return false
        if (status.registrationId == null || status.registrationId != ctx.manifest.registrationId) return false
        if (!status.identityKeyPublic.isNullOrBlank() &&
            status.identityKeyPublic != persistence.localIdentityPublic(ctx)
        ) {
            return false
        }
        if (status.signedPreKeyId != null && status.signedPreKeyId != ctx.manifest.latestSignedPreKeyId) return false
        if (status.kyberPreKeyId != null && status.kyberPreKeyId != ctx.manifest.latestKyberPreKeyId) return false
        return true
    }

    private fun serverHasPrekeysOutsideLocal(
        ctx: SignalStoreContext,
        status: org.cyblight.android.data.api.SignalKeyStatusResponse,
    ): Boolean {
        if (status.unusedOneTimePreKeys <= 0) return false
        val oldest = status.oldestUnusedPreKeyId ?: return false
        val newest = status.newestUnusedPreKeyId ?: return false
        return !persistence.hasLocalPreKey(ctx, oldest) || !persistence.hasLocalPreKey(ctx, newest)
    }

    private fun SignalKeyBundleDto.toPreKeyBundle(): PreKeyBundle {
        val preKeyId = oneTimePreKey?.keyId ?: PreKeyBundle.NULL_PRE_KEY_ID
        val preKeyPublic = oneTimePreKey?.publicKey?.let { ECPublicKey(SignalStorePersistence.base64Decode(it)) }
        val kyber = kyberPreKey ?: throw IllegalStateException("kyber_prekey_missing")
        return PreKeyBundle(
            registrationId,
            deviceId,
            preKeyId,
            preKeyPublic,
            signedPreKey.keyId,
            ECPublicKey(SignalStorePersistence.base64Decode(signedPreKey.publicKey)),
            SignalStorePersistence.base64Decode(signedPreKey.signature),
            IdentityKey(ECPublicKey(SignalStorePersistence.base64Decode(identityKey))),
            kyber.keyId,
            KEMPublicKey(SignalStorePersistence.base64Decode(kyber.publicKey)),
            SignalStorePersistence.base64Decode(kyber.signature),
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

    private fun generatePreKeys(startId: Int, count: Int): List<PreKeyRecord> =
        (0 until count).map { offset ->
            PreKeyRecord(startId + offset, ECKeyPair.generate())
        }

    private fun nextKeyId(): Int {
        nextPreKeyId += 1
        return nextPreKeyId
    }

    private fun messageSortKey(message: MessageDto): Long {
        val raw = message.createdAt
        if (raw <= 0L) return 0L
        return if (raw > 10_000_000_000L) raw else raw * 1000L
    }

    private fun messageKey(message: MessageDto): String =
        if (message.id.isNotBlank()) message.id else "${message.senderId}:${message.content}"

    companion object {
        private const val TAG = "SignalCryptoManager"
        private const val DEVICE_ID = 1
        private const val ONE_TIME_PREKEY_BATCH = 100
        private const val REPLENISH_THRESHOLD = 20
        private const val ENSURE_STATUS_TTL_MS = 60_000L
    }
}
