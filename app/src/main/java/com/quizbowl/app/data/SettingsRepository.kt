package com.quizbowl.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "quizbowl_settings")

data class TossupSettings(
    val rate: Float = 1.0f,
    val voiceName: String? = null,
    val categories: List<String> = emptyList(),
    val difficulties: List<Int> = emptyList(),
    val buzzTimer: Float = 5f,
    val answerTimer: Float = 3f,
)

data class BonusSettings(
    val rate: Float = 1.0f,
    val voiceName: String? = null,
    val categories: List<String> = emptyList(),
    val difficulties: List<Int> = emptyList(),
)

class SettingsRepository(private val context: Context) {

    companion object {
        private val KEY_USERNAME = stringPreferencesKey("username")
        private val KEY_TOSSUP_RATE = floatPreferencesKey("tossup_rate")
        private val KEY_TOSSUP_VOICE = stringPreferencesKey("tossup_voice")
        private val KEY_TOSSUP_CATEGORIES = stringPreferencesKey("tossup_categories")
        private val KEY_TOSSUP_DIFFICULTIES = stringPreferencesKey("tossup_difficulties")
        private val KEY_TOSSUP_BUZZ_TIMER = floatPreferencesKey("tossup_buzz_timer")
        private val KEY_TOSSUP_ANSWER_TIMER = floatPreferencesKey("tossup_answer_timer")
        private val KEY_BONUS_RATE = floatPreferencesKey("bonus_rate")
        private val KEY_BONUS_VOICE = stringPreferencesKey("bonus_voice")
        private val KEY_BONUS_CATEGORIES = stringPreferencesKey("bonus_categories")
        private val KEY_BONUS_DIFFICULTIES = stringPreferencesKey("bonus_difficulties")
    }

    val username: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_USERNAME] ?: ""
    }

    val tossupSettings: Flow<TossupSettings> = context.dataStore.data.map { prefs ->
        TossupSettings(
            rate = prefs[KEY_TOSSUP_RATE] ?: 1.0f,
            voiceName = prefs[KEY_TOSSUP_VOICE],
            categories = prefs[KEY_TOSSUP_CATEGORIES]?.split(",")?.filter { it.isNotEmpty() } ?: emptyList(),
            difficulties = prefs[KEY_TOSSUP_DIFFICULTIES]?.split(",")?.mapNotNull { it.toIntOrNull() } ?: emptyList(),
            buzzTimer = prefs[KEY_TOSSUP_BUZZ_TIMER] ?: 5f,
            answerTimer = prefs[KEY_TOSSUP_ANSWER_TIMER] ?: 3f,
        )
    }

    val bonusSettings: Flow<BonusSettings> = context.dataStore.data.map { prefs ->
        BonusSettings(
            rate = prefs[KEY_BONUS_RATE] ?: 1.0f,
            voiceName = prefs[KEY_BONUS_VOICE],
            categories = prefs[KEY_BONUS_CATEGORIES]?.split(",")?.filter { it.isNotEmpty() } ?: emptyList(),
            difficulties = prefs[KEY_BONUS_DIFFICULTIES]?.split(",")?.mapNotNull { it.toIntOrNull() } ?: emptyList(),
        )
    }

    suspend fun saveUsername(name: String) {
        context.dataStore.edit { prefs -> prefs[KEY_USERNAME] = name }
    }

    suspend fun saveTossupSettings(s: TossupSettings) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TOSSUP_RATE] = s.rate
            if (s.voiceName != null) prefs[KEY_TOSSUP_VOICE] = s.voiceName else prefs.remove(KEY_TOSSUP_VOICE)
            prefs[KEY_TOSSUP_CATEGORIES] = s.categories.joinToString(",")
            prefs[KEY_TOSSUP_DIFFICULTIES] = s.difficulties.joinToString(",")
            prefs[KEY_TOSSUP_BUZZ_TIMER] = s.buzzTimer
            prefs[KEY_TOSSUP_ANSWER_TIMER] = s.answerTimer
        }
    }

    suspend fun saveBonusSettings(s: BonusSettings) {
        context.dataStore.edit { prefs ->
            prefs[KEY_BONUS_RATE] = s.rate
            if (s.voiceName != null) prefs[KEY_BONUS_VOICE] = s.voiceName else prefs.remove(KEY_BONUS_VOICE)
            prefs[KEY_BONUS_CATEGORIES] = s.categories.joinToString(",")
            prefs[KEY_BONUS_DIFFICULTIES] = s.difficulties.joinToString(",")
        }
    }
}
