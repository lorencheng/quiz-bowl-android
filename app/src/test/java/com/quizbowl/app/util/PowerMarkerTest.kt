package com.quizbowl.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Power marker parsing.
 *
 * Questions mark the end of the power region with a literal "(*)". The marker
 * must be located for scoring, then removed before the text is handed to TTS so
 * it is never read aloud.
 */
class PowerMarkerTest {

    // ── Locating the marker ──────────────────────────────────────────────────

    @Test
    fun `finds a standalone marker token`() {
        val words = listOf("This", "novel", "(*)", "recounts", "how")
        assertEquals(2, findPowerIndex(words))
    }

    @Test
    fun `finds a marker attached to a word`() {
        val words = listOf("This", "novel(*)", "recounts")
        assertEquals(1, findPowerIndex(words))
    }

    @Test
    fun `finds a marker attached with punctuation`() {
        // Trimming punctuation would eat the parens and asterisk, so matching is
        // done with contains() rather than by stripping punctuation.
        val words = listOf("banned,", "(*)", "following")
        assertEquals(1, findPowerIndex(words))
        assertEquals(1, findPowerIndex(listOf("banned,", "event(*),", "following")))
    }

    @Test
    fun `returns -1 when there is no marker`() {
        assertEquals(-1, findPowerIndex(listOf("This", "novel", "recounts")))
    }

    @Test
    fun `returns -1 for an empty question`() {
        assertEquals(-1, findPowerIndex(emptyList()))
    }

    @Test
    fun `returns the first marker when several are present`() {
        val words = listOf("a", "(*)", "b", "(*)", "c")
        assertEquals(1, findPowerIndex(words))
    }

    @Test
    fun `a lone asterisk is not a power marker`() {
        // Only the exact "(*)" sequence counts.
        assertEquals(-1, findPowerIndex(listOf("a", "*", "b")))
        assertEquals(-1, findPowerIndex(listOf("a", "()", "b")))
    }

    // ── Stripping the marker ─────────────────────────────────────────────────

    @Test
    fun `a standalone marker is removed entirely`() {
        val words = listOf("This", "novel", "(*)", "recounts")
        assertEquals(listOf("This", "novel", "recounts"), stripPowerMarker(words))
    }

    @Test
    fun `a marker attached to a word keeps the word`() {
        val words = listOf("This", "novel(*)", "recounts")
        assertEquals(listOf("This", "novel", "recounts"), stripPowerMarker(words))
    }

    @Test
    fun `attached markers keep surrounding punctuation`() {
        assertEquals(listOf("event,"), stripPowerMarker(listOf("event(*),")))
    }

    @Test
    fun `a question with no marker is unchanged`() {
        val words = listOf("This", "novel", "recounts")
        assertEquals(words, stripPowerMarker(words))
    }

    @Test
    fun `an empty question stays empty`() {
        assertEquals(emptyList<String>(), stripPowerMarker(emptyList()))
    }

    @Test
    fun `no spoken word is ever left blank`() {
        // Blank entries would desynchronise the TTS word-boundary callbacks
        // that drive the on-screen reveal.
        val stripped = stripPowerMarker(listOf("a", "(*)", "", "  ", "b"))
        assertEquals(listOf("a", "b"), stripped)
        assertEquals(0, stripped.count { it.isBlank() })
    }

    @Test
    fun `the marker is never spoken`() {
        val stripped = stripPowerMarker(listOf("a", "(*)", "b(*)", "c"))
        assertEquals(0, stripped.count { it.contains("(*)") })
    }

    // ── Relationship between the two index spaces ────────────────────────────

    /**
     * findPowerIndex runs on the raw words, but the buzz index reported back by
     * TTS refers to the *stripped* list. These tests document how the two line up.
     */
    @Test
    fun `words before a standalone marker keep the same index after stripping`() {
        val raw = listOf("This", "novel", "(*)", "recounts", "how")
        val stripped = stripPowerMarker(raw)
        val powerIndex = findPowerIndex(raw)

        // Everything ahead of the marker is positionally identical, which is what
        // makes comparing a stripped-space buzz index against a raw-space power
        // index valid for the in-power region.
        for (i in 0 until powerIndex) {
            assertEquals(raw[i], stripped[i])
        }
    }

    @Test
    fun `words after a standalone marker shift down by one after stripping`() {
        val raw = listOf("This", "novel", "(*)", "recounts", "how")
        val stripped = stripPowerMarker(raw)
        val powerIndex = findPowerIndex(raw)

        assertEquals("recounts", raw[powerIndex + 1])
        assertEquals("recounts", stripped[powerIndex])
    }

    @Test
    fun `an attached marker causes no shift at all`() {
        val raw = listOf("This", "novel(*)", "recounts")
        val stripped = stripPowerMarker(raw)

        assertEquals(raw.size, stripped.size)
        assertEquals(1, findPowerIndex(raw))
        // The word carrying the marker stays at its original index, so here the
        // raw and stripped index spaces coincide throughout.
        assertEquals("recounts", stripped[2])
    }
}
