package com.applify.securepass

import java.security.SecureRandom

object PasswordGenerator {
    const val DEFAULT_PASSPHRASE_WORD_COUNT = 6
    const val MIN_PASSPHRASE_WORD_COUNT = 6
    const val MAX_PASSPHRASE_WORD_COUNT = 12
    private const val PASSPHRASE_WORD_LIST_SIZE = 7_776

    private const val UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val LOWER = "abcdefghijklmnopqrstuvwxyz"
    private const val DIGITS = "0123456789"
    private const val SYMBOLS = "!@#$%^&*()-_=+[]{}|;:,.<>?"
    private const val CONFUSABLE = "Il1oO0|"

    @JvmStatic
    fun generate(
        length: Int,
        useUpper: Boolean,
        useLower: Boolean,
        useDigits: Boolean,
        useSymbols: Boolean,
        omitConfusable: Boolean = false
    ): String {
        var pool = buildString {
            if (useUpper) append(UPPER)
            if (useLower) append(LOWER)
            if (useDigits) append(DIGITS)
            if (useSymbols) append(SYMBOLS)
        }
        if (pool.isEmpty()) pool = LOWER + DIGITS
        if (omitConfusable) pool = pool.filterNot { it in CONFUSABLE }

        val random = SecureRandom()
        val password = StringBuilder(length)
        repeat(length) {
            val index = random.nextInt(pool.length)
            password.append(pool[index])
        }
        return password.toString()
    }

    fun generatePassphrase(words: List<String>, wordCount: Int = DEFAULT_PASSPHRASE_WORD_COUNT): String {
        require(wordCount in MIN_PASSPHRASE_WORD_COUNT..MAX_PASSPHRASE_WORD_COUNT)
        require(words.size == PASSPHRASE_WORD_LIST_SIZE && words.toSet().size == PASSPHRASE_WORD_LIST_SIZE)

        val random = SecureRandom()
        // Double hyphens keep boundaries clear because the list contains single-hyphenated words.
        return List(wordCount) { words[random.nextInt(words.size)] }.joinToString("--")
    }
}
