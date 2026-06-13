package org.cyblight.android.crypto

import android.content.Context
import android.util.Base64
import com.google.gson.Gson
import org.signal.libsignal.protocol.IdentityKeyPair
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.state.KyberPreKeyRecord
import org.signal.libsignal.protocol.state.PreKeyRecord
import org.signal.libsignal.protocol.state.SessionRecord
import org.signal.libsignal.protocol.state.SignedPreKeyRecord
import org.signal.libsignal.protocol.state.impl.InMemorySignalProtocolStore

data class SignalStoreManifest(
    val registrationId: Int = 0,
    val identitySerialized: String = "",
    val preKeyIds: List<Int> = emptyList(),
    val signedPreKeyIds: List<Int> = emptyList(),
    val kyberPreKeyIds: List<Int> = emptyList(),
    val sessionKeys: List<String> = emptyList(),
    val latestSignedPreKeyId: Int? = null,
    val latestKyberPreKeyId: Int? = null,
)

class SignalStoreContext(
    val userId: String,
    val store: InMemorySignalProtocolStore,
    val manifest: MutableSignalStoreManifest,
)

class MutableSignalStoreManifest(
    var registrationId: Int,
    var identitySerialized: String,
    val preKeyIds: MutableSet<Int> = mutableSetOf(),
    val signedPreKeyIds: MutableSet<Int> = mutableSetOf(),
    val kyberPreKeyIds: MutableSet<Int> = mutableSetOf(),
    val sessionKeys: MutableSet<String> = mutableSetOf(),
    var latestSignedPreKeyId: Int? = null,
    var latestKyberPreKeyId: Int? = null,
) {
    fun toSnapshot(): SignalStoreManifest = SignalStoreManifest(
        registrationId = registrationId,
        identitySerialized = identitySerialized,
        preKeyIds = preKeyIds.sorted(),
        signedPreKeyIds = signedPreKeyIds.sorted(),
        kyberPreKeyIds = kyberPreKeyIds.sorted(),
        sessionKeys = sessionKeys.sorted(),
        latestSignedPreKeyId = latestSignedPreKeyId,
        latestKyberPreKeyId = latestKyberPreKeyId,
    )

    companion object {
        fun fromSnapshot(snapshot: SignalStoreManifest): MutableSignalStoreManifest =
            MutableSignalStoreManifest(
                registrationId = snapshot.registrationId,
                identitySerialized = snapshot.identitySerialized,
                preKeyIds = snapshot.preKeyIds.toMutableSet(),
                signedPreKeyIds = snapshot.signedPreKeyIds.toMutableSet(),
                kyberPreKeyIds = snapshot.kyberPreKeyIds.toMutableSet(),
                sessionKeys = snapshot.sessionKeys.toMutableSet(),
                latestSignedPreKeyId = snapshot.latestSignedPreKeyId,
                latestKyberPreKeyId = snapshot.latestKyberPreKeyId,
            )
    }
}

class SignalStorePersistence(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val legacyPrefs = context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun loadContext(userId: String): SignalStoreContext? {
        val manifestJson = prefs.getString(manifestKey(userId), null) ?: return null
        val snapshot = runCatching { gson.fromJson(manifestJson, SignalStoreManifest::class.java) }.getOrNull()
            ?: return null
        if (snapshot.registrationId <= 0 || snapshot.identitySerialized.isBlank()) return null

        val identityKeyPair = IdentityKeyPair(base64Decode(snapshot.identitySerialized))
        val store = InMemorySignalProtocolStore(identityKeyPair, snapshot.registrationId)
        val manifest = MutableSignalStoreManifest.fromSnapshot(snapshot)

        snapshot.preKeyIds.forEach { keyId ->
            readRecord(userId, "prekey", keyId)?.let { bytes ->
                store.storePreKey(keyId, PreKeyRecord(bytes))
            }
        }
        snapshot.signedPreKeyIds.forEach { keyId ->
            readRecord(userId, "signed", keyId)?.let { bytes ->
                store.storeSignedPreKey(keyId, SignedPreKeyRecord(bytes))
            }
        }
        snapshot.kyberPreKeyIds.forEach { keyId ->
            readRecord(userId, "kyber", keyId)?.let { bytes ->
                store.storeKyberPreKey(keyId, KyberPreKeyRecord(bytes))
            }
        }
        snapshot.sessionKeys.forEach { sessionKey ->
            val address = parseSessionAddress(sessionKey) ?: return@forEach
            readSession(userId, sessionKey)?.let { bytes ->
                store.storeSession(address, SessionRecord(bytes))
            }
        }

        return SignalStoreContext(userId, store, manifest)
    }

    fun loadLegacyIdentityContext(userId: String): SignalStoreContext? {
        val registrationId = legacyPrefs.getInt(legacyKey(userId, "registration_id"), 0)
        val identitySerialized = legacyPrefs.getString(legacyKey(userId, "identity_serialized"), null)
        if (registrationId <= 0 || identitySerialized.isNullOrBlank()) return null

        val identityKeyPair = IdentityKeyPair(base64Decode(identitySerialized))
        val manifest = MutableSignalStoreManifest(
            registrationId = registrationId,
            identitySerialized = identitySerialized,
        )
        val store = InMemorySignalProtocolStore(identityKeyPair, registrationId)
        return SignalStoreContext(userId, store, manifest)
    }

    fun persistContext(ctx: SignalStoreContext) {
        val editor = prefs.edit()
        val userId = ctx.userId
        val manifest = ctx.manifest

        manifest.preKeyIds.forEach { keyId ->
            if (ctx.store.containsPreKey(keyId)) {
                editor.putString(recordKey(userId, "prekey", keyId), base64Encode(ctx.store.loadPreKey(keyId).serialize()))
            }
        }
        manifest.signedPreKeyIds.forEach { keyId ->
            if (ctx.store.containsSignedPreKey(keyId)) {
                editor.putString(
                    recordKey(userId, "signed", keyId),
                    base64Encode(ctx.store.loadSignedPreKey(keyId).serialize()),
                )
            }
        }
        manifest.kyberPreKeyIds.forEach { keyId ->
            if (ctx.store.containsKyberPreKey(keyId)) {
                editor.putString(
                    recordKey(userId, "kyber", keyId),
                    base64Encode(ctx.store.loadKyberPreKey(keyId).serialize()),
                )
            }
        }
        manifest.sessionKeys.forEach { sessionKey ->
            val address = parseSessionAddress(sessionKey) ?: return@forEach
            if (ctx.store.containsSession(address)) {
                editor.putString(
                    sessionKey(userId, sessionKey),
                    base64Encode(ctx.store.loadSession(address).serialize()),
                )
            }
        }

        editor.putString(manifestKey(userId), gson.toJson(manifest.toSnapshot()))
        editor.apply()
    }

    fun trackPreKey(ctx: SignalStoreContext, keyId: Int) {
        ctx.manifest.preKeyIds.add(keyId)
    }

    fun trackSignedPreKey(ctx: SignalStoreContext, keyId: Int) {
        ctx.manifest.signedPreKeyIds.add(keyId)
        ctx.manifest.latestSignedPreKeyId = keyId
    }

    fun trackKyberPreKey(ctx: SignalStoreContext, keyId: Int) {
        ctx.manifest.kyberPreKeyIds.add(keyId)
        ctx.manifest.latestKyberPreKeyId = keyId
    }

    fun trackSession(ctx: SignalStoreContext, address: SignalProtocolAddress) {
        ctx.manifest.sessionKeys.add(sessionAddressKey(address))
    }

    fun localIdentityPublic(ctx: SignalStoreContext): String =
        base64Encode(ctx.store.identityKeyPair.publicKey.serialize())

    fun hasLocalPreKey(ctx: SignalStoreContext, keyId: Int): Boolean =
        ctx.store.containsPreKey(keyId)

    private fun readRecord(userId: String, kind: String, keyId: Int): ByteArray? =
        prefs.getString(recordKey(userId, kind, keyId), null)?.let(::base64Decode)

    private fun readSession(userId: String, sessionKey: String): ByteArray? =
        prefs.getString(sessionKey(userId, sessionKey), null)?.let(::base64Decode)

    private fun manifestKey(userId: String): String = "signal_${userId}_manifest"

    private fun recordKey(userId: String, kind: String, keyId: Int): String =
        "signal_${userId}_${kind}_$keyId"

    private fun sessionKey(userId: String, sessionKey: String): String =
        "signal_${userId}_session_$sessionKey"

    companion object {
        private const val PREFS_NAME = "cyblight_signal_store"
        private const val LEGACY_PREFS_NAME = "cyblight_signal_crypto"

        private fun legacyKey(userId: String, suffix: String): String = "signal_${userId}_$suffix"

        fun sessionAddressKey(address: SignalProtocolAddress): String =
            "${address.name}:${address.deviceId}"

        fun parseSessionAddress(sessionKey: String): SignalProtocolAddress? {
            val separator = sessionKey.lastIndexOf(':')
            if (separator <= 0 || separator >= sessionKey.lastIndex) return null
            val name = sessionKey.substring(0, separator)
            val deviceId = sessionKey.substring(separator + 1).toIntOrNull() ?: return null
            return SignalProtocolAddress(name, deviceId)
        }

        fun base64Encode(bytes: ByteArray): String =
            Base64.encodeToString(bytes, Base64.NO_WRAP)

        fun base64Decode(value: String): ByteArray =
            Base64.decode(value, Base64.NO_WRAP)
    }
}
