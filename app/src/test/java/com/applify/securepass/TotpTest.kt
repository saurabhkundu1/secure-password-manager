package com.applify.securepass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TotpTest {
    @Test
    fun matchesRfc6238Sha1Vector() {
        assertEquals("287082", Totp.code("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ", 59_000))
    }

    @Test
    fun rejectsMalformedBase32Secrets() {
        assertThrows(IllegalArgumentException::class.java) { Totp.code("AB=CD") }
        assertThrows(IllegalArgumentException::class.java) { Totp.code("A") }
    }
}
