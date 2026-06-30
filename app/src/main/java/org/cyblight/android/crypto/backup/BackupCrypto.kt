package org.cyblight.android.crypto.backup

import android.util.Base64
import com.google.gson.Gson
import org.cyblight.android.data.api.createApiGson
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object BackupCrypto {
    private val gson = createApiGson()
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
        val normalized = normalizeFile(file)
        val salt = base64Decode(normalized.saltBase64)
        val iv = base64Decode(normalized.ivBase64)
        val key = deriveKey(password, salt, normalized.iterations)
        
        val ciphertext = file.ciphertext
        val decryptedBytes = decryptBytes(ciphertext, key, iv)
        
        return try {
            val bis = java.io.ByteArrayInputStream(decryptedBytes)
            val reader = java.io.InputStreamReader(bis, StandardCharsets.UTF_8)
            val jsonReader = com.google.gson.stream.JsonReader(reader)
            val payload = gson.fromJson<CyblightBackupPayload>(jsonReader, CyblightBackupPayload::class.java)
            jsonReader.close()
            payload
        } catch (e: Exception) {
            android.util.Log.e("BackupCrypto", "JSON parse error", e)
            throw IllegalArgumentException("backup_payload_invalid")
        } finally {
            // No easy way to null out locals in Kotlin, but we can call GC
            if (decryptedBytes.size > 256 * 1024) {
                System.gc()
            }
        }
    }

    private fun decryptBytes(ciphertextBase64: String, key: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        
        val encrypted = base64Decode(ciphertextBase64)
        return try {
            cipher.doFinal(encrypted)
        } catch (_: Exception) {
            throw IllegalArgumentException("backup_password_invalid")
        }
    }

    fun serializeFile(file: CyblightBackupFile): String = gson.toJson(file)

    fun parseFile(raw: String): CyblightBackupFile {
        val trimmed = raw.trim().removePrefix("\uFEFF")
        if (trimmed.isBlank()) {
            throw IllegalArgumentException("backup_file_invalid")
        }
        val file = try {
            gson.fromJson(trimmed, CyblightBackupFile::class.java)
        } catch (_: Exception) {
            throw IllegalArgumentException("backup_file_invalid")
        }
        if (file == null) {
            throw IllegalArgumentException("backup_file_invalid")
        }
        if (file.saltBase64.isNullOrBlank() || file.ivBase64.isNullOrBlank() || file.ciphertext.isNullOrBlank()) {
            throw IllegalArgumentException("backup_file_invalid")
        }
        return file
    }

    private fun normalizeFile(file: CyblightBackupFile): CyblightBackupFile {
        val version = if (file.version <= 0) BACKUP_VERSION else file.version
        val kdf = file.kdf.takeIf { it.isNotBlank() } ?: BACKUP_KDF
        val iterations = if (file.iterations <= 0) BACKUP_ITERATIONS else file.iterations
        if (kdf != BACKUP_KDF) {
            throw IllegalArgumentException("backup_kdf_unsupported")
        }
        return file.copy(version = version, kdf = kdf, iterations = iterations)
    }

    private fun deriveKey(password: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    private fun base64Encode(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.NO_WRAP)

    private fun base64Decode(value: String): ByteArray =
        try {
            Base64.decode(value, Base64.NO_WRAP)
        } catch (_: Exception) {
            throw IllegalArgumentException("backup_file_invalid")
        }
}
