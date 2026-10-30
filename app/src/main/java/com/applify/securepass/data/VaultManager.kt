package com.applify.securepass.data

import android.content.Context
import com.applify.securepass.crypto.CryptoManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.lang.reflect.Type
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKey

class VaultManager {
    private val baseDir: File
    private var currentKey: SecretKey? = null
    private val gson = Gson()

    // Constructor for normal use (app context)
    constructor(context: Context) {
        this.baseDir = context.filesDir
    }

    // Constructor for testing (any directory)
    constructor(baseDir: File) {
        this.baseDir = baseDir
    }

    // ---------- Instance key management ----------
    fun getCurrentKey(): SecretKey? {
        if (currentKey == null) {
            currentKey = globalKey
        }
        return currentKey
    }

    fun isUnlocked(): Boolean {
        return getCurrentKey() != null
    }

    // Unlock using an already-derived key (for biometric flow)
    fun unlockWithKey(key: SecretKey) {
        this.currentKey = key
        setGlobalKey(key)
    }

    // ---------- Setup & Unlock ----------
    @Throws(Exception::class)
    fun setupNewVault(userCode: String) {
        require(userCode.matches(Regex("[0-9]{6}"))) { "Vault PIN must contain exactly six digits" }
        check(!File(baseDir, SALT_FILE).exists() && !File(baseDir, VAULT_FILE).exists()) { "Vault data already exists" }
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        saveSalt(salt)
        val key = CryptoManager.deriveKey(userCode, salt)
        try {
            writeEntries(emptyList(), key)
        } catch (e: Exception) {
            File(baseDir, SALT_FILE).delete()
            throw e
        }
        currentKey = key
        setGlobalKey(key)
    }

    @Throws(Exception::class)
    fun unlock(userCode: String) {
        require(userCode.matches(Regex("[0-9]{6}"))) { "Vault PIN must contain exactly six digits" }
        val salt = loadSalt()
        val candidateKey = CryptoManager.deriveKey(userCode, salt)
        val file = File(baseDir, VAULT_FILE)
        if (!file.exists()) throw IOException("Vault data is missing")
        CryptoManager.decrypt(readFile(file), candidateKey)
        currentKey = candidateKey
        setGlobalKey(candidateKey) // share the key
    }

    /**
     * Changes the master code. Requires the old code to unlock, then re-encrypts
     * the vault with the new code.
     */
    @Throws(Exception::class)
    fun changeMasterCode(oldCode: String, newCode: String) {
        require(newCode.matches(Regex("[0-9]{6}"))) { "Vault PIN must contain exactly six digits" }
        // Unlock with old code (this sets currentKey and globalKey)
        unlock(oldCode)

        // Load existing entries using old key
        val entries = loadEntries()

        // Derive a new key with the new code (reusing the same salt)
        val salt = loadSalt()
        val newKey = CryptoManager.deriveKey(newCode, salt)

        writeEntries(entries, newKey)
        currentKey = newKey
        setGlobalKey(newKey)
    }

    // ---------- Vault Operations ----------
    @Throws(Exception::class)
    fun loadEntries(): MutableList<VaultItem> {
        val key = getCurrentKey() ?: throw IllegalStateException("Vault not unlocked!")
        val file = File(baseDir, VAULT_FILE)
        if (!file.exists()) return ArrayList()
        val encryptedData = readFile(file)
        val json = CryptoManager.decrypt(encryptedData, key)
        val listType: Type = object : TypeToken<MutableList<VaultItem>>() {}.type
        return gson.fromJson(json, listType) ?: ArrayList()
    }

    @Throws(Exception::class)
    fun saveEntries(entries: List<VaultItem>) {
        val key = getCurrentKey() ?: throw IllegalStateException("Vault not unlocked!")
        writeEntries(entries, key)
    }

    private fun writeEntries(entries: List<VaultItem>, key: SecretKey) {
        val json = gson.toJson(entries)
        val encrypted = CryptoManager.encrypt(json, key)
        val file = File(baseDir, VAULT_FILE)
        writeFile(file, encrypted)
    }

    /**
     * Merges a list of items into the existing vault.
     * Skips items that already exist (same ID).
     */
    @Throws(Exception::class)
    fun mergeEntries(newItems: List<VaultItem>) {
        val existing = loadEntries()
        for (newItem in newItems) {
            var found = false
            for (ex in existing) {
                if (ex.id == newItem.id) {
                    found = true
                    break
                }
            }
            if (!found) {
                existing.add(newItem)
            }
        }
        saveEntries(existing)
    }

    // ---------- Salt & File Helpers ----------
    @Throws(IOException::class)
    private fun saveSalt(salt: ByteArray) {
        val file = File(baseDir, SALT_FILE)
        writeFile(file, Base64.getEncoder().encodeToString(salt))
    }

    @Throws(IOException::class)
    private fun loadSalt(): ByteArray {
        val file = File(baseDir, SALT_FILE)
        val base64 = readFile(file)
        return Base64.getDecoder().decode(base64)
    }

    @Throws(IOException::class)
    private fun readFile(file: File): String {
        return BufferedReader(FileInputStream(file).reader(StandardCharsets.UTF_8)).use { it.readText() }
    }

    @Throws(IOException::class)
    private fun writeFile(file: File, content: String) {
        val tempFile = File(file.parentFile, "${file.name}.tmp")
        try {
            FileOutputStream(tempFile).use { output ->
                output.write(content.toByteArray(StandardCharsets.UTF_8))
                output.fd.sync()
            }
            Files.move(
                tempFile.toPath(), file.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } finally {
            tempFile.delete()
        }
    }

    companion object {
        private const val VAULT_FILE = "vault.txt"
        private const val SALT_FILE = "vault.salt"

        @JvmStatic
        var globalKey: SecretKey? = null
            private set

        @JvmStatic
        fun setGlobalKey(key: SecretKey?) {
            globalKey = key
        }

        @JvmStatic
        fun clearGlobalKey() {
            globalKey = null
        }
    }
}
