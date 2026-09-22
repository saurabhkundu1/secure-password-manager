package com.applify.securepass.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Files

class TestVault {

    @Test
    @Throws(Exception::class)
    fun testVaultLifecycle() {
        // Create a temporary folder for the test vault
        val tempDir = Files.createTempDirectory("securepass-test").toFile()

        val vault = VaultManager(tempDir)

        // 1. Setup new vault with code "123456"
        vault.setupNewVault("123456")

        // 2. Add a dummy entry
        val item = VaultItem("example.com", "user@mail.com", "MySecret123", "Q: Pet? A: Fluffy")
        val entries = vault.loadEntries()
        entries.add(item)
        vault.saveEntries(entries)

        // 3. Simulate closing and reopening with a fresh manager
        val freshVault = VaultManager(tempDir)
        freshVault.unlock("123456")

        val loaded = freshVault.loadEntries()
        assertEquals(1, loaded.size)
        val loadedItem = loaded[0]
        assertEquals("example.com", loadedItem.website)
        assertEquals("user@mail.com", loadedItem.username)
        assertEquals("MySecret123", loadedItem.password)
        assertEquals("Q: Pet? A: Fluffy", loadedItem.notes)

        println("✅ Vault test passed! Entry: " + loadedItem.website)

        // Clean up temp files (optional)
        tempDir.deleteOnExit()
    }
}