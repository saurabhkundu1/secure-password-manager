package com.applify.securepass.data

import com.applify.securepass.PasswordGenerator

/**
 * Data model for popular day-to-day sites with automated password rules.
 */
data class SitePreset(
    val name: String,
    val domain: String,
    val length: Int,
    val useUpper: Boolean = true,
    val useLower: Boolean = true,
    val useDigits: Boolean = true,
    val useSymbols: Boolean = true
) {
    fun generatePassword(): String {
        return PasswordGenerator.generate(length, useUpper, useLower, useDigits, useSymbols)
    }

    companion object {
        val PRESETS = listOf(
            SitePreset("Google / Gmail", "google.com", 16),
            SitePreset("GitHub", "github.com", 20),
            SitePreset("Amazon", "amazon.com", 16),
            SitePreset("Netflix", "netflix.com", 16),
            SitePreset("Banking / Finance", "bank.com", 16),
            SitePreset("PayPal", "paypal.com", 16),
            SitePreset("Microsoft / Outlook", "microsoft.com", 16),
            SitePreset("Apple / iCloud", "apple.com", 16),
            SitePreset("Facebook", "facebook.com", 16),
            SitePreset("X / Twitter", "x.com", 16),
            SitePreset("Wi-Fi Router", "wifi-router", 20, useSymbols = false)
        )
    }
}
