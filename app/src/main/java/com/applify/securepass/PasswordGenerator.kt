package com.applify.securepass

import java.security.SecureRandom

object PasswordGenerator {
    private const val UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val LOWER = "abcdefghijklmnopqrstuvwxyz"
    private const val DIGITS = "0123456789"
    private const val SYMBOLS = "!@#$%^&*()-_=+[]{}|;:,.<>?"

    @JvmStatic
    fun generate(
        length: Int,
        useUpper: Boolean,
        useLower: Boolean,
        useDigits: Boolean,
        useSymbols: Boolean
    ): String {
        val pool = StringBuilder()
        if (useUpper) pool.append(UPPER)
        if (useLower) pool.append(LOWER)
        if (useDigits) pool.append(DIGITS)
        if (useSymbols) pool.append(SYMBOLS)
        if (pool.isEmpty()) {
            pool.append(LOWER).append(DIGITS)
        }

        val random = SecureRandom()
        val password = StringBuilder(length)
        for (i in 0 until length) {
            val index = random.nextInt(pool.length)
            password.append(pool[index])
        }
        return password.toString()
    }
}