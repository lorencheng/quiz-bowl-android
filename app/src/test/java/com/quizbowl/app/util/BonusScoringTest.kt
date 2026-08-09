package com.quizbowl.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Bonus scoring rules.
 *
 * A bonus has three parts worth 10 each, so a bonus scores 0-30 and converting
 * all three is a "thirty". PPB (points per bonus) is the standard efficiency
 * measure: total points divided by bonuses heard.
 */
class BonusScoringTest {

    private fun part(points: Int) =
        PartResult(correct = points > 0, points = points, userAnswer = "")

    // ── Per-bonus totals ─────────────────────────────────────────────────────

    @Test
    fun `a bonus with no parts answered scores 0`() {
        assertEquals(0, calcBonusTotal(emptyList()))
        assertEquals(0, calcBonusTotal(listOf(part(0), part(0), part(0))))
    }

    @Test
    fun `converting all three parts scores 30`() {
        assertEquals(30, calcBonusTotal(listOf(part(10), part(10), part(10))))
    }

    @Test
    fun `partial conversion sums only the parts earned`() {
        assertEquals(10, calcBonusTotal(listOf(part(10), part(0), part(0))))
        assertEquals(20, calcBonusTotal(listOf(part(10), part(0), part(10))))
    }

    @Test
    fun `part total is driven by points, not the correct flag`() {
        // The correct flag is display state; scoring reads points.
        val parts = listOf(
            PartResult(correct = false, points = 10, userAnswer = "x"),
            PartResult(correct = true, points = 0, userAnswer = "y"),
        )
        assertEquals(10, calcBonusTotal(parts))
    }

    // ── Running score ────────────────────────────────────────────────────────

    @Test
    fun `new score starts empty`() {
        val s = BonusScore()
        assertEquals(0, s.total)
        assertEquals(0, s.bonuses)
        assertEquals(0, s.thirties)
    }

    @Test
    fun `a thirty is counted as a thirty`() {
        val s = BonusScore().update(30)
        assertEquals(30, s.total)
        assertEquals(1, s.bonuses)
        assertEquals(1, s.thirties)
    }

    @Test
    fun `anything short of 30 is not a thirty`() {
        assertEquals(0, BonusScore().update(20).thirties)
        assertEquals(0, BonusScore().update(10).thirties)
        assertEquals(0, BonusScore().update(0).thirties)
    }

    @Test
    fun `a zeroed bonus still counts as heard`() {
        val s = BonusScore().update(0)
        assertEquals(0, s.total)
        assertEquals(1, s.bonuses)
        assertEquals(0, s.thirties)
    }

    @Test
    fun `scores accumulate across a run of bonuses`() {
        val s = BonusScore().update(30).update(20).update(0)
        assertEquals(50, s.total)
        assertEquals(3, s.bonuses)
        assertEquals(1, s.thirties)
    }

    // ── PPB ──────────────────────────────────────────────────────────────────

    @Test
    fun `ppb is zero before any bonus is heard`() {
        // Guards against dividing by zero on a fresh session.
        assertEquals(0f, BonusScore().ppb, 0.001f)
    }

    @Test
    fun `ppb is total over bonuses heard`() {
        assertEquals(30f, BonusScore().update(30).ppb, 0.001f)
        assertEquals(25f, BonusScore().update(30).update(20).ppb, 0.001f)
        assertEquals(20f, BonusScore().update(30).update(20).update(10).ppb, 0.001f)
    }

    @Test
    fun `ppb is fractional when it does not divide evenly`() {
        // 30 + 10 + 10 = 50 over 3 bonuses
        assertEquals(16.667f, BonusScore().update(30).update(10).update(10).ppb, 0.001f)
    }

    @Test
    fun `ppb stays zero when every bonus is zeroed`() {
        assertEquals(0f, BonusScore().update(0).update(0).ppb, 0.001f)
    }
}
