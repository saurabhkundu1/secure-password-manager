package com.applify.securepass.crypto

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.spec.InvalidKeySpecException
import java.security.spec.KeySpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoManager {

    /**
     * Derives a 256-bit AES key from the user's 6-digit code and a random salt.
     */
    @JvmStatic
    @Throws(NoSuchAlgorithmException::class, InvalidKeySpecException::class)
    fun deriveKey(userCode: String?, salt: ByteArray?): SecretKey {
        requireNotNull(userCode) { "User code cannot be null for key derivation" }

        val iterations = 100_000
        val keyLength = 256

        val spec: KeySpec = PBEKeySpec(userCode.toCharArray(), salt, iterations, keyLength)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    /**
     * Encrypts plaintext with AES-256-GCM.
     * Returns a Base64 string containing IV + ciphertext.
     */
    @JvmStatic
    @Throws(Exception::class)
    fun encrypt(plaintext: String, key: SecretKey?): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")

        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)
        val spec = GCMParameterSpec(128, iv)

        cipher.init(Cipher.ENCRYPT_MODE, key, spec)
        val ciphertext = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))

        val byteBuffer = ByteBuffer.allocate(iv.size + ciphertext.size)
        byteBuffer.put(iv)
        byteBuffer.put(ciphertext)

        return Base64.getEncoder().encodeToString(byteBuffer.array())
    }

    /**
     * Decrypts a Base64 string produced by encrypt().
     * Returns the original plaintext.
     */
    @JvmStatic
    @Throws(Exception::class)
    fun decrypt(base64Ciphertext: String?, key: SecretKey?): String {
        val data = Base64.getDecoder().decode(base64Ciphertext)
        val byteBuffer = ByteBuffer.wrap(data)

        val iv = ByteArray(12)
        byteBuffer.get(iv)

        val ciphertext = ByteArray(byteBuffer.remaining())
        byteBuffer.get(ciphertext)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        val plaintext = cipher.doFinal(ciphertext)

        return String(plaintext, StandardCharsets.UTF_8)
    }
}