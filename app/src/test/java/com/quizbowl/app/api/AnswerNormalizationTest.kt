package com.quizbowl.app.api

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Answer normalisation applied before an answer is sent to the check-answer API.
 *
 * Speech recognition renders spoken negative numbers as "-5", but answer lines
 * spell them out, so a leading minus sign in front of a digit is expanded to the
 * word "negative". This mirrors the behaviour of the iOS client.
 */
class AnswerNormalizationTest {

    private fun normalize(s: String) = QbReaderService.normalizeAnswer(s)

    @Test
    fun `a leading negative number is spelled out`() {
        assertEquals("negative 5", normalize("-5"))
        assertEquals("negative 0", normalize("-0"))
    }

    @Test
    fun `multi-digit negative numbers keep their remaining digits`() {
        assertEquals("negative 12", normalize("-12"))
        assertEquals("negative 273", normalize("-273"))
    }

    @Test
    fun `trailing words are preserved`() {
        assertEquals("negative 5 degrees", normalize("-5 degrees"))
        assertEquals("negative 40 degrees Celsius", normalize("-40 degrees Celsius"))
    }

    @Test
    fun `positive numbers are left alone`() {
        assertEquals("5", normalize("5"))
        assertEquals("42 degrees", normalize("42 degrees"))
    }

    @Test
    fun `ordinary text answers are left alone`() {
        assertEquals("Hamlet", normalize("Hamlet"))
        assertEquals("Battle of Kulikovo", normalize("Battle of Kulikovo"))
    }

    @Test
    fun `an answer already spelled out is left alone`() {
        assertEquals("negative 5", normalize("negative 5"))
    }

    @Test
    fun `a hyphen not followed by a digit is left alone`() {
        // Hyphenated words and dashes must survive untouched.
        assertEquals("-abc", normalize("-abc"))
        assertEquals("-", normalize("-"))
    }

    @Test
    fun `a hyphen inside a word is left alone`() {
        assertEquals("Austria-Hungary", normalize("Austria-Hungary"))
        assertEquals("x-5", normalize("x-5"))
    }

    @Test
    fun `only the leading sign is expanded`() {
        // A second negative later in the answer is not touched.
        assertEquals("negative 5 to -10", normalize("-5 to -10"))
    }

    @Test
    fun `an empty answer stays empty`() {
        assertEquals("", normalize(""))
    }

    /**
     * NOTE: documents current behaviour. The pattern is anchored to the very
     * start of the string, so a leading space prevents the expansion. Worth
     * revisiting if speech results can arrive with leading whitespace.
     */
    @Test
    fun `a leading space prevents expansion`() {
        assertEquals(" -5", normalize(" -5"))
    }
}
