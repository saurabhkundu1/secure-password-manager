package com.applify.securepass.data;

import org.junit.Test;
import static org.junit.Assert.*;
import java.io.File;
import java.nio.file.Files;
import java.util.List;

public class TestVault {

    @Test
    public void testVaultLifecycle() throws Exception {
        // Create a temporary folder for the test vault
        File tempDir = Files.createTempDirectory("securepass-test").toFile();

        VaultManager vault = new VaultManager(tempDir);

        // 1. Setup new vault with code "123456"
        vault.setupNewVault("123456");

        // 2. Add a dummy entry
        VaultItem item = new VaultItem("example.com", "user@mail.com", "MySecret123", "Q: Pet? A: Fluffy");
        List<VaultItem> entries = vault.loadEntries();
        entries.add(item);
        vault.saveEntries(entries);

        // 3. Simulate closing and reopening with a fresh manager
        VaultManager freshVault = new VaultManager(tempDir);
        freshVault.unlock("123456");

        List<VaultItem> loaded = freshVault.loadEntries();
        assertEquals(1, loaded.size());
        VaultItem loadedItem = loaded.get(0);
        assertEquals("example.com", loadedItem.website);
        assertEquals("user@mail.com", loadedItem.username);
        assertEquals("MySecret123", loadedItem.password);
        assertEquals("Q: Pet? A: Fluffy", loadedItem.notes);

        System.out.println("✅ Vault test passed! Entry: " + loadedItem.website);

        // Clean up temp files (optional)
        tempDir.deleteOnExit();
    }
}