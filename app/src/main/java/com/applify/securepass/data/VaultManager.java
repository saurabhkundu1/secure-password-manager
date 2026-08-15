package com.applify.securepass.data;

import android.content.Context;
import com.applify.securepass.crypto.CryptoManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.lang.reflect.Type;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.crypto.SecretKey;

public class VaultManager {
    private static final String VAULT_FILE = "vault.txt";
    private static final String SALT_FILE = "vault.salt";
    private File baseDir;
    private SecretKey currentKey;
    private Gson gson = new Gson();
    private static SecretKey globalKey;   // session key shared across activities

    // Constructor for normal use (app context)
    public VaultManager(Context context) {
        this.baseDir = context.getFilesDir();
    }

    // Constructor for testing (any directory)
    public VaultManager(File baseDir) {
        this.baseDir = baseDir;
    }

    // ---------- Static key management ----------
    public static SecretKey getGlobalKey() {
        return globalKey;
    }

    public static void setGlobalKey(SecretKey key) {
        globalKey = key;
    }

    public static void clearGlobalKey() {
        globalKey = null;
    }

    // ---------- Instance key management ----------
    public SecretKey getCurrentKey() {
        if (currentKey == null) {
            currentKey = globalKey;
        }
        return currentKey;
    }

    public boolean isUnlocked() {
        return getCurrentKey() != null;
    }

    // Unlock using an already-derived key (for biometric flow)
    public void unlockWithKey(SecretKey key) {
        this.currentKey = key;
        setGlobalKey(key);
    }

    // ---------- Setup & Unlock ----------
    public void setupNewVault(String userCode) throws Exception {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        saveSalt(salt);
        currentKey = CryptoManager.deriveKey(userCode, salt);
        setGlobalKey(currentKey);   // share the key
        saveEntries(new ArrayList<>());
    }

    public void unlock(String userCode) throws Exception {
        if (isUnlocked()) return;
        byte[] salt = loadSalt();
        currentKey = CryptoManager.deriveKey(userCode, salt);
        setGlobalKey(currentKey);   // share the key
    }

    /**
     * Changes the master code. Requires the old code to unlock, then re-encrypts
     * the vault with the new code.
     */
    public void changeMasterCode(String oldCode, String newCode) throws Exception {
        // Unlock with old code (this sets currentKey and globalKey)
        unlock(oldCode);

        // Load existing entries using old key
        List<VaultItem> entries = loadEntries();

        // Derive a new key with the new code (reusing the same salt)
        byte[] salt = loadSalt();
        SecretKey newKey = CryptoManager.deriveKey(newCode, salt);

        // Replace the key
        currentKey = newKey;
        setGlobalKey(newKey);

        // Re-save entries with the new key
        saveEntries(entries);
    }

    // ---------- Vault Operations ----------
    public List<VaultItem> loadEntries() throws Exception {
        SecretKey key = getCurrentKey();
        if (key == null) throw new IllegalStateException("Vault not unlocked!");
        File file = new File(baseDir, VAULT_FILE);
        if (!file.exists()) return new ArrayList<>();
        String encryptedData = readFile(file);
        String json = CryptoManager.decrypt(encryptedData, key);
        Type listType = new TypeToken<List<VaultItem>>() {}.getType();
        return gson.fromJson(json, listType);
    }

    public void saveEntries(List<VaultItem> entries) throws Exception {
        SecretKey key = getCurrentKey();
        if (key == null) throw new IllegalStateException("Vault not unlocked!");
        String json = gson.toJson(entries);
        String encrypted = CryptoManager.encrypt(json, key);
        File file = new File(baseDir, VAULT_FILE);
        writeFile(file, encrypted);
    }

    /**
     * Merges a list of items into the existing vault.
     * Skips items that already exist (same ID).
     */
    public void mergeEntries(List<VaultItem> newItems) throws Exception {
        List<VaultItem> existing = loadEntries();
        for (VaultItem newItem : newItems) {
            boolean found = false;
            for (VaultItem ex : existing) {
                if (ex.id.equals(newItem.id)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                existing.add(newItem);
            }
        }
        saveEntries(existing);
    }

    // ---------- Salt & File Helpers ----------
    private void saveSalt(byte[] salt) throws IOException {
        File file = new File(baseDir, SALT_FILE);
        writeFile(file, Base64.getEncoder().encodeToString(salt));
    }

    private byte[] loadSalt() throws IOException {
        File file = new File(baseDir, SALT_FILE);
        String base64 = readFile(file);
        return Base64.getDecoder().decode(base64);
    }

    private String readFile(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    private void writeFile(File file, String content) throws IOException {
        try (FileWriter fw = new FileWriter(file)) {
            fw.write(content);
        }
    }
}