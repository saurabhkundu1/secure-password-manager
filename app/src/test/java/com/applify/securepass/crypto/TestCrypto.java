package com.applify.securepass.crypto;

import org.junit.Test;
import java.security.SecureRandom;
import java.util.Base64;
import static org.junit.Assert.*;

public class TestCrypto {
    @Test
    public void testKeyDerivation() {
        try {
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            System.out.println("Salt (Base64): " + Base64.getEncoder().encodeToString(salt));

            String testPin = "123456";
            var key = CryptoManager.deriveKey(testPin, salt);

            System.out.println("Key algorithm: " + key.getAlgorithm());
            System.out.println("Key length: " + key.getEncoded().length * 8 + " bits");

            var key2 = CryptoManager.deriveKey(testPin, salt);
            boolean same = java.util.Arrays.equals(key.getEncoded(), key2.getEncoded());
            System.out.println("Same key? " + same);
            
            assertTrue("Derived keys should be the same for the same input", same);

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
}