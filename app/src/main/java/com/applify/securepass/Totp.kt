package com.applify.securepass

import androidx.core.net.toUri
import java.nio.ByteBuffer
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object Totp {
    fun code(secret: String, timeMillis: Long = System.currentTimeMillis()): String {
        val rawSecret = secret.trim().let { value ->
            if (!value.startsWith("otpauth://", ignoreCase = true)) value else {
                val uri = value.toUri()
                require(uri.host?.equals("totp", ignoreCase = true) == true) { "Only TOTP authenticator URIs are supported" }
                require(uri.getQueryParameter("algorithm").orEmpty().let { it.isEmpty() || it.equals("SHA1", ignoreCase = true) }) { "Only SHA1 TOTP is supported" }
                require(uri.getQueryParameter("digits").orEmpty().let { it.isEmpty() || it == "6" }) { "Only six-digit TOTP is supported" }
                require(uri.getQueryParameter("period").orEmpty().let { it.isEmpty() || it == "30" }) { "Only 30-second TOTP is supported" }
                uri.getQueryParameter("secret").orEmpty()
            }
        }
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        val compact = rawSecret.uppercase(Locale.ROOT).filterNot { it == ' ' || it == '-' }
        val clean = compact.trimEnd('=')
        val padding = compact.length - clean.length
        val expectedPadding = when (clean.length % 8) {
            0 -> 0
            2 -> 6
            4 -> 4
            5 -> 3
            7 -> 1
            else -> throw IllegalArgumentException("Enter a valid Base32 secret")
        }
        require(padding == 0 || padding == expectedPadding) { "Enter a valid Base32 secret" }
        require(clean.isNotEmpty() && clean.all { it in alphabet }) { "Enter a valid Base32 secret" }
        val bits = clean.map { alphabet.indexOf(it).toString(2).padStart(5, '0') }.joinToString("")
        val remainder = bits.length % 8
        require(remainder == 0 || bits.takeLast(remainder).all { it == '0' }) { "Enter a valid Base32 secret" }
        val key = bits.take(bits.length / 8 * 8).chunked(8).map { it.toInt(2).toByte() }.toByteArray()
        require(key.size >= 10) { "Authenticator secret must contain at least 16 Base32 characters" }
        val counter = ByteBuffer.allocate(8).putLong(timeMillis / 30_000L).array()
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(key, "HmacSHA1"))
        val hash = mac.doFinal(counter)
        val offset = hash.last().toInt() and 0x0f
        val binary = ((hash[offset].toInt() and 0x7f) shl 24) or
            ((hash[offset + 1].toInt() and 0xff) shl 16) or
            ((hash[offset + 2].toInt() and 0xff) shl 8) or
            (hash[offset + 3].toInt() and 0xff)
        return (binary % 1_000_000).toString().padStart(6, '0')
    }
}
