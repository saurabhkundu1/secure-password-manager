package com.applify.securepass.data

import android.content.Context
import com.applify.securepass.crypto.CryptoManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
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
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        saveSalt(salt)
        currentKey = CryptoManager.deriveKey(userCode, salt)
        setGlobalKey(currentKey) // share the key
        saveEntries(ArrayList())
    }

    @Throws(Exception::class)
    fun unlock(userCode: String) {
        if (isUnlocked()) return
        val salt = loadSalt()
        currentKey = CryptoManager.deriveKey(userCode, salt)
        setGlobalKey(currentKey) // share the key
    }

    /**
     * Changes the master code. Requires the old code to unlock, then re-encrypts
     * the vault with the new code.
     */
    @Throws(Exception::class)
    fun changeMasterCode(oldCode: String, newCode: String) {
        // Unlock with old code (this sets currentKey and globalKey)
        unlock(oldCode)

        // Load existing entries using old key
        val entries = loadEntries()

        // Derive a new key with the new code (reusing the same salt)
        val salt = loadSalt()
        val newKey = CryptoManager.deriveKey(newCode, salt)

        // Replace the key
        currentKey = newKey
        setGlobalKey(newKey)

        // Re-save entries with the new key
        saveEntries(entries)
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
        saveEntries(existing);
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
        val sb = StringBuilder()
        BufferedReader(FileReader(file)).use { br ->
            var line: String?
            while (br.readLine().also { line = it } != null) {
                sb.append(line)
            }
        }
        return sb.toString()
    }

    @Throws(IOException::class)
    private fun writeFile(file: File, content: String) {
        FileWriter(file).use { fw ->
            fw.write(content)
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