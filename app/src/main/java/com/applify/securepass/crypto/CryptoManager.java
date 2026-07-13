package com.applify.securepass.crypto;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.nio.charset.StandardCharsets;

public class CryptoManager {

    /**
     * Derives a 256-bit AES key from the user's 6-digit code and a random salt.
     */
    public static SecretKey deriveKey(String userCode, byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {

        int iterations = 100_000;
        int keyLength = 256;

        KeySpec spec = new PBEKeySpec(userCode.toCharArray(), salt, iterations, keyLength);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Encrypts plaintext with AES-256-GCM.
     * Returns a Base64 string containing IV + ciphertext.
     */
    public static String encrypt(String plaintext, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);

        cipher.init(Cipher.ENCRYPT_MODE, key, spec);
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + ciphertext.length);
        byteBuffer.put(iv);
        byteBuffer.put(ciphertext);

        return Base64.getEncoder().encodeToString(byteBuffer.array());
    }

    /**
     * Decrypts a Base64 string produced by encrypt().
     * Returns the original plaintext.
     */
    public static String decrypt(String base64Ciphertext, SecretKey key) throws Exception {
        byte[] data = Base64.getDecoder().decode(base64Ciphertext);
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);

        byte[] iv = new byte[12];
        byteBuffer.get(iv);

        byte[] ciphertext = new byte[byteBuffer.remaining()];
        byteBuffer.get(ciphertext);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
        byte[] plaintext = cipher.doFinal(ciphertext);

        return new String(plaintext, StandardCharsets.UTF_8);
    }
}