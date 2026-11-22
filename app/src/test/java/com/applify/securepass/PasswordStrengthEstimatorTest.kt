package com.applify.securepass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordStrengthEstimatorTest {

    @Test
    fun nullOrEmptyPassword_isVeryWeak() {
        val nullResult = PasswordStrengthEstimator.estimate(null)
        assertEquals(0, nullResult.score)
        assertEquals(StrengthLevel.VERY_WEAK, nullResult.level)

        val emptyResult = PasswordStrengthEstimator.estimate("")
        assertEquals(0, emptyResult.score)
        assertEquals(StrengthLevel.VERY_WEAK, emptyResult.level)
    }

    @Test
    fun shortOrSimplePassword_isWeakOrVeryWeak() {
        val shortResult = PasswordStrengthEstimator.estimate("abc")
        assertTrue(shortResult.score < 25)
        assertEquals(StrengthLevel.VERY_WEAK, shortResult.level)
        assertTrue(shortResult.feedback.contains("Too short", ignoreCase = true))

        val simpleResult = PasswordStrengthEstimator.estimate("password")
        assertTrue(simpleResult.score < 50)
    }

    @Test
    fun sequentialOrRepeatedPatterns_receivePenalties() {
        val random8 = PasswordStrengthEstimator.estimate("k9#Xm2!w")
        val sequential8 = PasswordStrengthEstimator.estimate("12345678")

        assertTrue("Random password should score higher than sequential digits", random8.score > sequential8.score)
    }

    @Test
    fun complexLongPassword_isStrongOrVeryStrong() {
        val strongResult = PasswordStrengthEstimator.estimate("K9#mQ!v7xP$2zL@8")
        assertTrue(strongResult.score >= 70)
        assertTrue(strongResult.level == StrengthLevel.STRONG || strongResult.level == StrengthLevel.VERY_STRONG)
    }
}
