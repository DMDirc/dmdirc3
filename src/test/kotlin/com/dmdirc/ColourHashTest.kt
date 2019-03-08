package com.dmdirc

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ColourHashTest {

    @Test
    fun `can hash an empty string`() {
        assertEquals(0, "".colourHash())
    }

    @Test
    fun `can hash pre-defined examples`() {
        assertEquals(0, "acidBurn".colourHash())
        assertEquals(1, "crashOverride".colourHash())
        assertEquals(2, "phantomPhreak".colourHash())
        assertEquals(3, "/_ORD N1K0N".colourHash())
        assertEquals(4, "the-plague".colourHash())
        assertEquals(5, "zeroCool".colourHash())
        assertEquals(7, "cerealKiller".colourHash())
    }
}