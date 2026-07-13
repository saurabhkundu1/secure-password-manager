package com.applify.securepass.data;

import android.content.Context;
import com.applify.securepass.crypto.CryptoManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.lang.reflect.Type;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.SecretKey;

public class VaultManager {
    private static final String VAULT_FILE = "vault.enc";
    private static final String SALT_FILE = "vault.salt";
    private File baseDir;
    private SecretKey currentKey;
    private Gson gson = new Gson();

    // Constructor for normal use (app context)
    public VaultManager(Context context) {
        this.baseDir = context.getFilesDir();
    }

    // Constructor for testing (any directory)
    public VaultManager(File baseDir) {
        this.baseDir = baseDir;
    }

    // ---------- Setup & Unlock ----------
    public void setupNewVault(String userCode) throws Exception {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        saveSalt(salt);
        currentKey = CryptoManager.deriveKey(userCode, salt);
        saveEntries(new ArrayList<>());
    }

    public void unlock(String userCode) throws Exception {
        byte[] salt = loadSalt();
        currentKey = CryptoManager.deriveKey(userCode, salt);
    }

    // ---------- Vault Operations ----------
    public List<VaultItem> loadEntries() throws Exception {
        if (currentKey == null) throw new IllegalStateException("Vault not unlocked!");
        File file = new File(baseDir, VAULT_FILE);
        if (!file.exists()) return new ArrayList<>();
        String encryptedData = readFile(file);
        String json = CryptoManager.decrypt(encryptedData, currentKey);
        Type listType = new TypeToken<List<VaultItem>>() {}.getType();
        return gson.fromJson(json, listType);
    }

    public void saveEntries(List<VaultItem> entries) throws Exception {
        if (currentKey == null) throw new IllegalStateException("Vault not unlocked!");
        String json = gson.toJson(entries);
        String encrypted = CryptoManager.encrypt(json, currentKey);
        File file = new File(baseDir, VAULT_FILE);
        writeFile(file, encrypted);
    }

    // ---------- Salt & File Helpers ----------
    private void saveSalt(byte[] salt) throws IOException {
        File file = new File(baseDir, SALT_FILE);
        writeFile(file, java.util.Base64.getEncoder().encodeToString(salt));
    }

    private byte[] loadSalt() throws IOException {
        File file = new File(baseDir, SALT_FILE);
        String base64 = readFile(file);
        return java.util.Base64.getDecoder().decode(base64);
    }

    private String readFile(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    private void writeFile(File file, String content) throws IOException {
        try (FileWriter fw = new FileWriter(file)) {
            fw.write(content);
        }
    }
}