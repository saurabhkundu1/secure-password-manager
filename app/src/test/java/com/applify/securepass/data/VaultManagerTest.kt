package com.applify.securepass.data

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class VaultManagerTest {
    @Test
    fun verifiesPinEvenWhenVaultIsAlreadyUnlockedAndChangesCodeSafely() {
        val directory = Files.createTempDirectory("securepass-vault").toFile()
        val manager = VaultManager(directory)
        try {
            manager.setupNewVault("123456")
            manager.saveEntries(listOf(VaultItem("example.com", "user", "secret", "note")))

            assertThrows(Exception::class.java) { manager.changeMasterCode("000000", "654321") }
            assertEquals("secret", manager.loadEntries().single().password)

            manager.changeMasterCode("123456", "654321")
            VaultManager.clearGlobalKey()
            val reopened = VaultManager(directory)
            reopened.unlock("654321")
            assertEquals("secret", reopened.loadEntries().single().password)
            assertThrows(Exception::class.java) { reopened.unlock("123456") }
        } finally {
            VaultManager.clearGlobalKey()
            directory.deleteRecursively()
        }
    }
}
