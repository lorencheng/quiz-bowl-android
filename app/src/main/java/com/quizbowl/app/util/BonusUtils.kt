package com.quizbowl.app.util

data class BonusScore(
    val total: Int = 0,
    val bonuses: Int = 0,
    val thirties: Int = 0,
) {
    val ppb: Float get() = if (bonuses == 0) 0f else total.toFloat() / bonuses
}

fun BonusScore.update(bonusTotal: Int): BonusScore = copy(
    total = total + bonusTotal,
    bonuses = bonuses + 1,
    thirties = thirties + if (bonusTotal == 30) 1 else 0,
)

data class PartResult(
    val correct: Boolean,
    val points: Int,
    val userAnswer: String,
)

fun calcBonusTotal(partResults: List<PartResult>): Int =
    partResults.sumOf { it.points }
