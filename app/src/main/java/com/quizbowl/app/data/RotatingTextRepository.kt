package com.quizbowl.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.rotatingStore: DataStore<Preferences>
    by preferencesDataStore(name = "quizbowl_rotating")

object RotatingTextRepository {

    val TOSSUP_NAV = listOf(
        "Solo tossup drills. Buzz like the real thing.",
        "Hear every clue. Race the clock.",
        "Train your buzz. One question at a time.",
        "Questions read aloud, just like tournament day.",
        "Drill tossups. Build your buzzer instincts.",
        "Power through tossups. Track your score.",
        "Every question read aloud. Your buzz, your score.",
        "Competitive tossup training, anywhere you go.",
    )

    val BONUS_NAV = listOf(
        "Chase the thirty. One part at a time.",
        "Three parts. Thirty points. Can you convert?",
        "Master bonus conversion. Go for 30/30.",
        "Work through bonus questions. Build your conversion rate.",
        "Thirty points is the goal. Start earning it.",
        "Get better at bonuses before your team needs you.",
        "Hear every part. Answer all three.",
        "Three chances per bonus. Make them count.",
    )

    // Index 0 = null (show nothing). 6 remaining text options.
    val IDLE_HINTS: List<String?> = listOf(
        null,
        "Ready when you are.",
        "Your next question is waiting.",
        "Warm up your buzzer.",
        "Let's see what you know.",
        "Hit Start to pull your first question.",
        "Questions loaded and ready.",
    )

    private val KEY_TOSSUP_NAV = stringSetPreferencesKey("shown_tossup_nav")
    private val KEY_BONUS_NAV = stringSetPreferencesKey("shown_bonus_nav")
    private val KEY_IDLE_HINT = stringSetPreferencesKey("shown_idle_hint")

    suspend fun nextTossupSubtitle(context: Context): String =
        TOSSUP_NAV[pickNext(context, KEY_TOSSUP_NAV, TOSSUP_NAV.size)]

    suspend fun nextBonusSubtitle(context: Context): String =
        BONUS_NAV[pickNext(context, KEY_BONUS_NAV, BONUS_NAV.size)]

    suspend fun nextIdleHint(context: Context): String? =
        IDLE_HINTS[pickNext(context, KEY_IDLE_HINT, IDLE_HINTS.size)]

    private suspend fun pickNext(
        context: Context,
        key: Preferences.Key<Set<String>>,
        poolSize: Int,
    ): Int {
        val prefs = context.rotatingStore.data.first()
        val shown = prefs[key]?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
        val remaining = (0 until poolSize).filter { it !in shown }
        val pick = if (remaining.isNotEmpty()) {
            remaining.random()
        } else {
            context.rotatingStore.edit { it[key] = emptySet() }
            (0 until poolSize).random()
        }
        context.rotatingStore.edit { it[key] = (prefs[key] ?: emptySet()) + pick.toString() }
        return pick
    }
}
