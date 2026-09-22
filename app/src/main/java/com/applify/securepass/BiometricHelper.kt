package com.applify.securepass

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import java.nio.ByteBuffer
import java.security.KeyStore
import java.util.concurrent.Executors
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object BiometricHelper {

    private const val KEY_NAME = "biometric_vault_key"
    private const val ANDROID_KEY_STORE = "AndroidKeyStore"

    /**
     * Encrypts the raw vault key using a Keystore key.
     * This Keystore key is generated if it doesn't exist.
     */
    @JvmStatic
    @Throws(Exception::class)
    fun encryptKeyWithBiometric(rawKey: ByteArray): String {
        val secretKey = getOrCreateKey(false)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val iv = cipher.iv
        val encryptedKey = cipher.doFinal(rawKey)

        val byteBuffer = ByteBuffer.allocate(iv.size + encryptedKey.size)
        byteBuffer.put(iv)
        byteBuffer.put(encryptedKey)
        return Base64.encodeToString(byteBuffer.array(), Base64.DEFAULT)
    }

    /**
     * Decrypts the stored key using BiometricPrompt.
     */
    @JvmStatic
    fun decryptKeyWithBiometric(
        activity: FragmentActivity,
        base64EncryptedKey: String,
        callback: BiometricAuthenticationCallback
    ) {
        try {
            val secretKey = getOrCreateKey(true)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")

            val data = Base64.decode(base64EncryptedKey, Base64.DEFAULT)
            val byteBuffer = ByteBuffer.wrap(data)
            val iv = ByteArray(12)
            byteBuffer.get(iv)
            val ciphertext = ByteArray(byteBuffer.remaining())
            byteBuffer.get(ciphertext)

            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))

            val executor = Executors.newSingleThreadExecutor()
            val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    activity.runOnUiThread { callback.onError(errString.toString()) }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    try {
                        val authCipher = result.cryptoObject?.cipher
                        if (authCipher != null) {
                            val decryptedKey = authCipher.doFinal(ciphertext)
                            activity.runOnUiThread { callback.onSuccess(decryptedKey) }
                        } else {
                            activity.runOnUiThread { callback.onError("Cipher was null") }
                        }
                    } catch (e: Exception) {
                        activity.runOnUiThread { callback.onError(e.message ?: "Decryption error") }
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // Keep waiting or let user know
                }
            })

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Vault")
                .setSubtitle("Authenticate to access your passwords")
                .setNegativeButtonText("Use PIN")
                .build()

            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))

        } catch (e: Exception) {
            callback.onError(e.message ?: "Authentication error")
        }
    }

    @Throws(Exception::class)
    private fun getOrCreateKey(requiredAuth: Boolean): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE)
        keyStore.load(null)

        if (!keyStore.containsAlias(KEY_NAME)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
            val builder = KeyGenParameterSpec.Builder(
                KEY_NAME,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)

            if (requiredAuth) {
                builder.setUserAuthenticationRequired(true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    builder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
                }
            }

            keyGenerator.init(builder.build())
            return keyGenerator.generateKey()
        }

        return keyStore.getKey(KEY_NAME, null) as SecretKey
    }

    interface BiometricAuthenticationCallback {
        fun onSuccess(decryptedKey: ByteArray)
        fun onError(error: String?)
    }
}