package com.opencookie.admin.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormattersTest {

    @Test
    fun formatLamports_alwaysShowsSol() {
        assertEquals("0.00001 SOL", formatLamports(10_000))
        assertEquals("1.5 SOL", formatLamports(1_500_000_000))
        assertEquals("0.000000001 SOL", formatLamports(1))
    }

    @Test
    fun solFromLamports_returnsDecimalWithoutSuffix() {
        assertEquals("0.00001", solFromLamports(10_000))
        assertEquals("1.5", solFromLamports(1_500_000_000))
    }

    @Test
    fun lamportsFromSolInput_parsesSol() {
        assertEquals(10_000L, lamportsFromSolInput("0.00001"))
        assertEquals(1_500_000_000L, lamportsFromSolInput("1.5"))
        assertNull(lamportsFromSolInput(""))
    }
}
