package org.cyblight.android.crypto.backup

import android.util.Base64
import com.google.gson.Gson
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object BackupCrypto {
    private val gson = Gson()
    private val secureRandom = SecureRandom()

    fun encryptPayload(payload: CyblightBackupPayload, password: String): CyblightBackupFile {
        val salt = ByteArray(16).also(secureRandom::nextBytes)
        val iv = ByteArray(12).also(secureRandom::nextBytes)
        val key = deriveKey(password, salt, BACKUP_ITERATIONS)
        val plaintext = gson.toJson(payload).toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(plaintext)

        return CyblightBackupFile(
            saltBase64 = base64Encode(salt),
            ivBase64 = base64Encode(iv),
            ciphertext = base64Encode(encrypted),
        )
    }

    fun decryptPayload(file: CyblightBackupFile, password: String): CyblightBackupPayload {
        if (file.format != BACKUP_FILE_FORMAT || file.version != BACKUP_VERSION) {
            throw IllegalArgumentException("backup_format_unsupported")
        }
        if (file.kdf != BACKUP_KDF) {
            throw IllegalArgumentException("backup_kdf_unsupported")
        }

        val salt = base64Decode(file.saltBase64)
        val iv = base64Decode(file.ivBase64)
        val key = deriveKey(password, salt, file.iterations)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))

        val decrypted = try {
            cipher.doFinal(base64Decode(file.ciphertext))
        } catch (_: Exception) {
            throw IllegalArgumentException("backup_password_invalid")
        }

        val payload = gson.fromJson(String(decrypted, Charsets.UTF_8), CyblightBackupPayload::class.java)
        if (payload.format != BACKUP_PAYLOAD_FORMAT || payload.version != BACKUP_VERSION) {
            throw IllegalArgumentException("backup_payload_invalid")
        }
        return payload
    }

    fun serializeFile(file: CyblightBackupFile): String = gson.toJson(file)

    fun parseFile(raw: String): CyblightBackupFile {
        val file = gson.fromJson(raw, CyblightBackupFile::class.java)
        if (file.format != BACKUP_FILE_FORMAT) {
            throw IllegalArgumentException("backup_file_invalid")
        }
        return file
    }

    private fun deriveKey(password: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    private fun base64Encode(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.NO_WRAP)

    private fun base64Decode(value: String): ByteArray =
        Base64.decode(value, Base64.NO_WRAP)
}
