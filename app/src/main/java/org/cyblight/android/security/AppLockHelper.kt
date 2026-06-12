package org.cyblight.android.security

import java.security.MessageDigest
import java.security.SecureRandom

object AppLockHelper {
    private const val PIN_SALT_BYTES = 16

    fun generateSalt(): String {
        val bytes = ByteArray(PIN_SALT_BYTES)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hashPin(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("$salt:$pin".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verifyPin(pin: String, salt: String, expectedHash: String): Boolean {
        if (pin.isBlank() || salt.isBlank() || expectedHash.isBlank()) return false
        return hashPin(pin, salt) == expectedHash
    }
}
