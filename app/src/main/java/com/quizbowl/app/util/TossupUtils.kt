package com.quizbowl.app.util

/** Find the word index of the (*) power marker in raw question words. Returns -1 if not found. */
fun findPowerIndex(rawWords: List<String>): Int =
    rawWords.indexOfFirst { it.contains("(*)") }

/** Remove (*) from words, filtering out any that become blank. */
fun stripPowerMarker(rawWords: List<String>): List<String> =
    rawWords.map { it.replace("(*)", "").trim() }.filter { it.isNotEmpty() }

/**
 * Calculate tossup points.
 * power (buzzed before *): 15 pts
 * correct: 10 pts
 * wrong: -5 pts
 * no answer / timeout after done: 0 pts
 */
fun calcTossupPoints(directive: String, powerIndex: Int, buzzIndex: Int): Int = when (directive) {
    "accept" -> if (powerIndex >= 0 && buzzIndex < powerIndex) 15 else 10
    "reject" -> -5
    else -> 0
}

data class TossupScore(
    val correct: Int = 0,
    val neg: Int = 0,
    val total: Int = 0,
    val questions: Int = 0,
)

fun TossupScore.update(points: Int): TossupScore = copy(
    correct = correct + if (points > 0) 1 else 0,
    neg = neg + if (points < 0) 1 else 0,
    total = total + points,
    questions = questions + 1,
)
