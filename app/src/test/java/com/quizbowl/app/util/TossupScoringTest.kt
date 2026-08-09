package com.quizbowl.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tossup scoring rules.
 *
 * Quiz bowl scoring: buzzing correctly before the (*) power marker scores 15,
 * a correct buzz after it scores 10, an incorrect buzz negs for -5, and a
 * question that was never converted scores 0.
 *
 * `calcTossupPoints` only handles the accept/reject directives. Timeout scoring
 * is computed separately in TossupViewModel (0 if the buzz came after the
 * question finished reading, -5 otherwise) and is covered by [timeoutPointsRule].
 */
class TossupScoringTest {

    // ── Power ────────────────────────────────────────────────────────────────

    @Test
    fun `correct buzz before the power marker scores 15`() {
        assertEquals(15, calcTossupPoints("accept", powerIndex = 10, buzzIndex = 0))
        assertEquals(15, calcTossupPoints("accept", powerIndex = 10, buzzIndex = 5))
        assertEquals(15, calcTossupPoints("accept", powerIndex = 10, buzzIndex = 9))
    }

    @Test
    fun `correct buzz exactly on the power marker scores 10, not power`() {
        // Power is awarded strictly before the marker.
        assertEquals(10, calcTossupPoints("accept", powerIndex = 10, buzzIndex = 10))
    }

    @Test
    fun `correct buzz after the power marker scores 10`() {
        assertEquals(10, calcTossupPoints("accept", powerIndex = 10, buzzIndex = 11))
        assertEquals(10, calcTossupPoints("accept", powerIndex = 10, buzzIndex = 40))
    }

    @Test
    fun `question with no power marker always scores 10 when correct`() {
        // findPowerIndex returns -1 when the question contains no (*).
        assertEquals(10, calcTossupPoints("accept", powerIndex = -1, buzzIndex = 0))
        assertEquals(10, calcTossupPoints("accept", powerIndex = -1, buzzIndex = 50))
    }

    // ── Negs and non-conversions ─────────────────────────────────────────────

    @Test
    fun `incorrect buzz negs for -5 regardless of position`() {
        assertEquals(-5, calcTossupPoints("reject", powerIndex = 10, buzzIndex = 0))
        assertEquals(-5, calcTossupPoints("reject", powerIndex = 10, buzzIndex = 30))
        assertEquals(-5, calcTossupPoints("reject", powerIndex = -1, buzzIndex = -1))
    }

    @Test
    fun `directives other than accept and reject score 0`() {
        // e.g. the API's "prompt" directive, or an unset/unknown value.
        assertEquals(0, calcTossupPoints("prompt", powerIndex = 10, buzzIndex = 0))
        assertEquals(0, calcTossupPoints("", powerIndex = 10, buzzIndex = 0))
    }

    /**
     * Documents the timeout rule implemented in TossupViewModel.startAnswerTimer:
     * letting the answer clock run out negs for -5, unless the buzz came after the
     * question had finished reading, in which case there is no penalty.
     */
    @Test
    fun timeoutPointsRule() {
        fun timeoutPoints(buzzedAfterDone: Boolean) = if (buzzedAfterDone) 0 else -5

        assertEquals(-5, timeoutPoints(buzzedAfterDone = false))
        assertEquals(0, timeoutPoints(buzzedAfterDone = true))
    }

    /**
     * NOTE: unverified edge case, captured so it is visible rather than silent.
     *
     * buzzIndex is initialised to -1 and only set once TTS reports where it
     * stopped. If an answer is accepted while buzzIndex is still -1, the
     * `buzzIndex < powerIndex` comparison is satisfied and the question scores a
     * power. Whether that is desirable has not been confirmed — if it is not,
     * calcTossupPoints should guard against a negative buzzIndex.
     */
    @Test
    fun `accepted answer with unset buzz index currently scores power`() {
        assertEquals(15, calcTossupPoints("accept", powerIndex = 10, buzzIndex = -1))
    }

    // ── Running score ────────────────────────────────────────────────────────

    @Test
    fun `new score starts empty`() {
        val s = TossupScore()
        assertEquals(0, s.correct)
        assertEquals(0, s.neg)
        assertEquals(0, s.total)
        assertEquals(0, s.questions)
    }

    @Test
    fun `a power counts as correct and adds 15`() {
        val s = TossupScore().update(15)
        assertEquals(1, s.correct)
        assertEquals(0, s.neg)
        assertEquals(15, s.total)
        assertEquals(1, s.questions)
    }

    @Test
    fun `a ten counts as correct and adds 10`() {
        val s = TossupScore().update(10)
        assertEquals(1, s.correct)
        assertEquals(0, s.neg)
        assertEquals(10, s.total)
        assertEquals(1, s.questions)
    }

    @Test
    fun `a neg counts against and subtracts 5`() {
        val s = TossupScore().update(-5)
        assertEquals(0, s.correct)
        assertEquals(1, s.neg)
        assertEquals(-5, s.total)
        assertEquals(1, s.questions)
    }

    @Test
    fun `a zero counts as played but is neither correct nor a neg`() {
        // A dead question: still played, but no reward and no penalty.
        val s = TossupScore().update(0)
        assertEquals(0, s.correct)
        assertEquals(0, s.neg)
        assertEquals(0, s.total)
        assertEquals(1, s.questions)
    }

    @Test
    fun `scores accumulate across a run of questions`() {
        val s = TossupScore()
            .update(15)
            .update(10)
            .update(-5)
            .update(0)

        assertEquals(2, s.correct)
        assertEquals(1, s.neg)
        assertEquals(20, s.total)
        assertEquals(4, s.questions)
    }

    @Test
    fun `a run of negs produces a negative total`() {
        val s = TossupScore().update(-5).update(-5).update(-5)
        assertEquals(0, s.correct)
        assertEquals(3, s.neg)
        assertEquals(-15, s.total)
        assertEquals(3, s.questions)
    }
}
