package com.applify.securepass.crypto

import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.security.SecureRandom
import java.util.Arrays
import java.util.Base64

class TestCrypto {
    @Test
    fun testKeyDerivation() {
        try {
            val salt = ByteArray(16)
            SecureRandom().nextBytes(salt)
            println("Salt (Base64): " + Base64.getEncoder().encodeToString(salt))

            val testPin = "123456"
            val key = CryptoManager.deriveKey(testPin, salt)

            println("Key algorithm: " + key.algorithm)
            println("Key length: " + key.encoded.size * 8 + " bits")

            val key2 = CryptoManager.deriveKey(testPin, salt)
            val same = Arrays.equals(key.encoded, key2.encoded)
            println("Same key? $same")

            assertTrue("Derived keys should be the same for the same input", same)

        } catch (e: Exception) {
            e.printStackTrace()
            fail(e.message)
        }
    }
}