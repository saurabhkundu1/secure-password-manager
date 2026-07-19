package com.applify.securepass;

import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import androidx.annotation.RequiresApi;
import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.FragmentActivity;
import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class BiometricHelper {

    private static final String KEY_NAME = "biometric_vault_key";
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";

    /**
     * Encrypts the raw vault key using a Keystore key.
     * This Keystore key is generated if it doesn't exist.
     */
    public static String encryptKeyWithBiometric(byte[] rawKey) throws Exception {
        SecretKey secretKey = getOrCreateKey(false);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        
        byte[] iv = cipher.getIV();
        byte[] encryptedKey = cipher.doFinal(rawKey);

        ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encryptedKey.length);
        byteBuffer.put(iv);
        byteBuffer.put(encryptedKey);
        return Base64.encodeToString(byteBuffer.array(), Base64.DEFAULT);
    }

    /**
     * Decrypts the stored key using BiometricPrompt.
     */
    public static void decryptKeyWithBiometric(FragmentActivity activity, String base64EncryptedKey, BiometricAuthenticationCallback callback) {
        try {
            SecretKey secretKey = getOrCreateKey(true);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            
            byte[] data = Base64.decode(base64EncryptedKey, Base64.DEFAULT);
            ByteBuffer byteBuffer = ByteBuffer.wrap(data);
            byte[] iv = new byte[12];
            byteBuffer.get(iv);
            byte[] ciphertext = new byte[byteBuffer.remaining()];
            byteBuffer.get(ciphertext);

            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(128, iv));

            Executor executor = Executors.newSingleThreadExecutor();
            BiometricPrompt biometricPrompt = new BiometricPrompt(activity, executor, new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationError(int errorCode, CharSequence errString) {
                    super.onAuthenticationError(errorCode, errString);
                    activity.runOnUiThread(() -> callback.onError(errString.toString()));
                }

                @Override
                public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    try {
                        Cipher authCipher = result.getCryptoObject().getCipher();
                        byte[] decryptedKey = authCipher.doFinal(ciphertext);
                        activity.runOnUiThread(() -> callback.onSuccess(decryptedKey));
                    } catch (Exception e) {
                        activity.runOnUiThread(() -> callback.onError(e.getMessage()));
                    }
                }

                @Override
                public void onAuthenticationFailed() {
                    super.onAuthenticationFailed();
                    // Keep waiting or let user know
                }
            });

            BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Unlock Vault")
                    .setSubtitle("Authenticate to access your passwords")
                    .setNegativeButtonText("Use PIN")
                    .build();

            biometricPrompt.authenticate(promptInfo, new BiometricPrompt.CryptoObject(cipher));

        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    private static SecretKey getOrCreateKey(boolean requiredAuth) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
        keyStore.load(null);

        if (!keyStore.containsAlias(KEY_NAME)) {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE);
            KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(KEY_NAME,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256);
            
            if (requiredAuth) {
                builder.setUserAuthenticationRequired(true);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    builder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG);
                }
            }
            
            keyGenerator.init(builder.build());
            return keyGenerator.generateKey();
        }

        return (SecretKey) keyStore.getKey(KEY_NAME, null);
    }

    public interface BiometricAuthenticationCallback {
        void onSuccess(byte[] decryptedKey);
        void onError(String error);
    }
}