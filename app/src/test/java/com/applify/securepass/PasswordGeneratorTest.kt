package com.applify.securepass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PasswordGeneratorTest {
    @Test
    fun omitsConfusableCharactersFromEachSelectedSet() {
        val upper = PasswordGenerator.generate(256, true, false, false, false, true)
        val lower = PasswordGenerator.generate(256, false, true, false, false, true)
        val digits = PasswordGenerator.generate(256, false, false, true, false, true)
        val symbols = PasswordGenerator.generate(256, false, false, false, true, true)

        assertTrue(upper.all { it in "ABCDEFGHJKLMNPQRSTUVWXYZ" })
        assertTrue(lower.all { it in "abcdefghijkmnpqrstuvwxyz" })
        assertTrue(digits.all { it in "23456789" })
        assertTrue(symbols.all { it in "!@#$%^&*()-_=+[]{};:,.<>?" })
    }

    @Test
    fun fallbackPoolAlsoOmitsConfusableCharacters() {
        val password = PasswordGenerator.generate(256, false, false, false, false, true)

        assertTrue(password.all { it in "abcdefghijkmnpqrstuvwxyz23456789" })
    }

    @Test
    fun generatePassphrase_defaultSixWords_joinedWithDoubleHyphens() {
        val words = (1..7776).map { "token$it" }
        val passphrase = PasswordGenerator.generatePassphrase(words)
        val tokens = passphrase.split("--")

        assertEquals(6, tokens.size)
        assertTrue(tokens.all { it.startsWith("token") })
    }

    @Test
    fun generatePassphrase_customWordCount_withinBounds() {
        val words = (1..7776).map { "item$it" }
        val passphrase = PasswordGenerator.generatePassphrase(words, wordCount = 10)
        val tokens = passphrase.split("--")

        assertEquals(10, tokens.size)
        assertTrue(tokens.all { it.startsWith("item") })
    }

    @Test
    fun generatePassphrase_rejectsInvalidWordCount() {
        val words = (1..7776).map { "w$it" }
        try {
            PasswordGenerator.generatePassphrase(words, wordCount = 5)
            fail("Expected IllegalArgumentException for count < 6")
        } catch (_: IllegalArgumentException) { }

        try {
            PasswordGenerator.generatePassphrase(words, wordCount = 13)
            fail("Expected IllegalArgumentException for count > 12")
        } catch (_: IllegalArgumentException) { }
    }

    @Test
    fun generatePassphrase_rejectsInvalidWordList() {
        val duplicateWords = (1..7775).map { "w$it" } + listOf("w1")
        try {
            PasswordGenerator.generatePassphrase(duplicateWords)
            fail("Expected IllegalArgumentException for duplicates")
        } catch (_: IllegalArgumentException) { }

        try {
            PasswordGenerator.generatePassphrase(listOf("w1", "w2"))
            fail("Expected IllegalArgumentException for size != 7776")
        } catch (_: IllegalArgumentException) { }
    }
}
