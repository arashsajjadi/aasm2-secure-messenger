package com.arash.aasm2.crypto

import android.util.Base64
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object Aasm2Crypto {
    private val MAGIC = "AASM2".toByteArray(Charsets.US_ASCII)
    private const val FORMAT_VERSION: Byte = 1
    private const val KDF_PBKDF2_SHA256: Byte = 1
    private const val CIPHER_AES_256_GCM: Byte = 1
    private const val DEFAULT_ITERATIONS = 350_000
    private const val SALT_LENGTH = 16
    private const val NONCE_LENGTH = 12
    private const val GCM_TAG_LENGTH_BITS = 128

    data class DecryptionResult(
        val plaintext: ByteArray,
        val metadata: Map<String, String>
    )

    fun encryptTextToBase64(text: String, password: String): String {
        val blob = encryptBytes(text.toByteArray(Charsets.UTF_8), password, mapOf("type" to "text"))
        return Base64.encodeToString(blob, Base64.NO_WRAP)
    }

    fun decryptTextFromBase64(raw: String, password: String): String {
        if (raw.isBlank()) {
            throw IllegalArgumentException("Input is empty.")
        }
        val blob = try {
            Base64.decode(raw, Base64.DEFAULT)
        } catch (ex: Exception) {
            throw IllegalArgumentException("Text input is not valid Base64.", ex)
        }
        return decryptBytes(blob, password).plaintext.toString(Charsets.UTF_8)
    }

    fun encryptBytes(
        data: ByteArray,
        password: String,
        metadata: Map<String, String> = emptyMap(),
        iterations: Int = DEFAULT_ITERATIONS
    ): ByteArray {
        require(password.isNotBlank()) { "Password cannot be empty." }
        val salt = randomBytes(SALT_LENGTH)
        val nonce = randomBytes(NONCE_LENGTH)
        val metadataBytes = encodeMetadata(metadata)
        val header = buildHeader(salt, nonce, metadataBytes, iterations)
        val key = deriveKey(password, salt, iterations)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonce))
        cipher.updateAAD(header)
        val ciphertext = cipher.doFinal(data)
        return header + ciphertext
    }

    fun decryptBytes(blob: ByteArray, password: String): DecryptionResult {
        require(password.isNotBlank()) { "Password cannot be empty." }
        if (blob.size < MAGIC.size + 18 + SALT_LENGTH + NONCE_LENGTH + 16) {
            throw IllegalArgumentException("Encrypted data is too short.")
        }
        if (!blob.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) {
            throw IllegalArgumentException("Invalid AASM2 magic header.")
        }

        var pos = MAGIC.size
        val fixed = ByteBuffer.wrap(blob, pos, 13).order(ByteOrder.BIG_ENDIAN)
        val version = fixed.get()
        val kdfId = fixed.get()
        val cipherId = fixed.get()
        val iterations = fixed.int
        val saltLength = fixed.get().toInt() and 0xFF
        val nonceLength = fixed.get().toInt() and 0xFF
        val metadataLength = fixed.int
        pos += 13

        if (version != FORMAT_VERSION) {
            throw IllegalArgumentException("Unsupported AASM2 format version.")
        }
        if (kdfId != KDF_PBKDF2_SHA256 || cipherId != CIPHER_AES_256_GCM) {
            throw IllegalArgumentException("Unsupported AASM2 cryptographic identifiers.")
        }
        if (saltLength <= 0 || nonceLength <= 0) {
            throw IllegalArgumentException("Invalid salt or nonce length.")
        }
        if (pos + saltLength + nonceLength + metadataLength >= blob.size) {
            throw IllegalArgumentException("Encrypted payload is corrupted.")
        }

        val salt = blob.copyOfRange(pos, pos + saltLength)
        pos += saltLength
        val nonce = blob.copyOfRange(pos, pos + nonceLength)
        pos += nonceLength
        val metadataBytes = blob.copyOfRange(pos, pos + metadataLength)
        pos += metadataLength
        val header = blob.copyOfRange(0, pos)
        val ciphertext = blob.copyOfRange(pos, blob.size)
        val metadata = decodeMetadata(metadataBytes)
        val key = deriveKey(password, salt, iterations)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        return try {
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonce))
            cipher.updateAAD(header)
            val plaintext = cipher.doFinal(ciphertext)
            DecryptionResult(plaintext, metadata)
        } catch (ex: Exception) {
            throw IllegalArgumentException("Wrong password or tampered data.", ex)
        }
    }

    private fun deriveKey(password: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    private fun buildHeader(salt: ByteArray, nonce: ByteArray, metadata: ByteArray, iterations: Int): ByteArray {
        val fixed = ByteBuffer.allocate(13).order(ByteOrder.BIG_ENDIAN)
            .put(FORMAT_VERSION)
            .put(KDF_PBKDF2_SHA256)
            .put(CIPHER_AES_256_GCM)
            .putInt(iterations)
            .put(salt.size.toByte())
            .put(nonce.size.toByte())
            .putInt(metadata.size)
            .array()
        return MAGIC + fixed + salt + nonce + metadata
    }

    private fun encodeMetadata(metadata: Map<String, String>): ByteArray {
        val sortedKeys = metadata.keys.sorted()
        val json = JSONObject()
        for (key in sortedKeys) {
            json.put(key, metadata[key])
        }
        return json.toString().toByteArray(Charsets.UTF_8)
    }

    private fun decodeMetadata(bytes: ByteArray): Map<String, String> {
        if (bytes.isEmpty()) {
            return emptyMap()
        }
        val json = JSONObject(bytes.toString(Charsets.UTF_8))
        val result = linkedMapOf<String, String>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            result[key] = json.optString(key)
        }
        return result.toSortedMap()
    }

    private fun randomBytes(length: Int): ByteArray {
        return ByteArray(length).also { SecureRandom().nextBytes(it) }
    }
}
