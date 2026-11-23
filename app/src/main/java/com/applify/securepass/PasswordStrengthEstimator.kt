package com.applify.securepass

enum class StrengthLevel(val label: String, val colorHex: Int) {
    VERY_WEAK("Very Weak", 0xFFD32F2F.toInt()),
    WEAK("Weak", 0xFFE64A19.toInt()),
    FAIR("Fair", 0xFFF57C00.toInt()),
    STRONG("Strong", 0xFF388E3C.toInt()),
    VERY_STRONG("Very Strong", 0xFF1976D2.toInt())
}

data class StrengthResult(
    val score: Int,
    val level: StrengthLevel,
    val feedback: String
)

object PasswordStrengthEstimator {
    private val SEQUENCES = listOf(
        "1234567890",
        "abcdefghijklmnopqrstuvwxyz",
        "qwertyuiop",
        "asdfghjkl",
        "zxcvbnm"
    )

    fun estimate(password: String?): StrengthResult {
        if (password.isNullOrEmpty()) {
            return StrengthResult(0, StrengthLevel.VERY_WEAK, "Enter password")
        }

        var poolSize = 0
        var hasUpper = false
        var hasLower = false
        var hasDigit = false
        var hasSymbol = false

        for (ch in password) {
            when {
                ch in 'a'..'z' -> hasLower = true
                ch in 'A'..'Z' -> hasUpper = true
                ch in '0'..'9' -> hasDigit = true
                else -> hasSymbol = true
            }
        }

        if (hasLower) poolSize += 26
        if (hasUpper) poolSize += 26
        if (hasDigit) poolSize += 10
        if (hasSymbol) poolSize += 33

        // Base entropy calculation: length * log2(poolSize)
        val entropy = if (poolSize > 0) password.length * (Math.log(poolSize.toDouble()) / Math.log(2.0)) else 0.0

        var penalty = 0.0

        // 1. Repeated consecutive characters
        var repeatedCount = 0
        for (i in 1 until password.length) {
            if (password[i].lowercaseChar() == password[i - 1].lowercaseChar()) {
                repeatedCount++
            }
        }
        penalty += repeatedCount * 6.0

        // 2. Sequential patterns
        val lower = password.lowercase()
        var seqPenalty = 0.0
        for (seq in SEQUENCES) {
            for (len in seq.length downTo 3) {
                var found = false
                for (start in 0..seq.length - len) {
                    val sub = seq.substring(start, start + len)
                    if (lower.contains(sub) || lower.contains(sub.reversed())) {
                        seqPenalty += len * 3.0
                        found = true
                        break
                    }
                }
                if (found) break
            }
        }
        penalty += seqPenalty.coerceAtMost(45.0)

        // 3. Length penalty for short passwords
        if (password.length < 8) {
            penalty += (8 - password.length) * 10.0
        }

        val adjustedScore = ((entropy - penalty) * 1.25).coerceIn(0.0, 100.0).toInt()

        val level = when {
            adjustedScore < 25 -> StrengthLevel.VERY_WEAK
            adjustedScore < 50 -> StrengthLevel.WEAK
            adjustedScore < 70 -> StrengthLevel.FAIR
            adjustedScore < 88 -> StrengthLevel.STRONG
            else -> StrengthLevel.VERY_STRONG
        }

        val feedback = when {
            password.length < 8 -> "Too short — at least 8 chars recommended"
            !hasSymbol && !hasDigit -> "Add numbers and symbols"
            !hasSymbol -> "Add special characters"
            !hasUpper -> "Add uppercase letters"
            repeatedCount > 1 -> "Avoid repeated characters"
            adjustedScore >= 88 -> "Excellent password"
            adjustedScore >= 70 -> "Strong password"
            else -> "Mix letters, numbers & symbols"
        }

        return StrengthResult(adjustedScore, level, feedback)
    }
}
