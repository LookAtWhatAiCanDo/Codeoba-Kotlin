package com.whataicando.codeoba.core.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SemVerTest {

    @Test
    fun testSemVerParsing() {
        assertEquals(SemVer(1, 2, 3), SemVer.parse("1.2.3"))
        assertEquals(SemVer(1, 2, 3), SemVer.parse("v1.2.3"))
        assertEquals(SemVer(1, 10, 0), SemVer.parse("v1.10.0-beta1"))
        assertEquals(SemVer(2, 0, 15), SemVer.parse("2.0.15+build.123"))
        assertEquals(SemVer(0, 0, 0), SemVer.parse("invalid"))
        assertEquals(SemVer(1, 0, 0), SemVer.parse("1"))
        assertEquals(SemVer(1, 2, 0), SemVer.parse("1.2"))
        assertEquals(SemVer(1, 2, 3), SemVer.parse("  V1.2.3  "))
        assertEquals(SemVer(1, 2, 3), SemVer.parse("V1.2.3"))
        assertEquals(SemVer(1, 2, 3), SemVer.parse("  1.2.3  "))
    }

    @Test
    fun testSemVerComparison() {
        assertTrue(SemVer.parse("1.10.0") > SemVer.parse("1.9.0"))
        assertTrue(SemVer.parse("2.0.0") > SemVer.parse("1.99.99"))
        assertTrue(SemVer.parse("1.0.1") > SemVer.parse("1.0.0"))
        assertTrue(SemVer.parse("1.0.0") == SemVer.parse("1.0.0"))
        assertTrue(SemVer.parse("1.0.0") < SemVer.parse("1.0.1"))
        assertTrue(SemVer.parse("0.9.3") < SemVer.parse("1.0.0"))
    }
}
