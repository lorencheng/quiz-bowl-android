package com.quizbowl.app.api.models

import com.google.gson.annotations.SerializedName

data class Tossup(
    @SerializedName("question") val question: String,
    @SerializedName("question_sanitized") val questionSanitized: String?,
    @SerializedName("answer") val answer: String,
    @SerializedName("answer_sanitized") val answerSanitized: String?,
    @SerializedName("category") val category: String?,
    @SerializedName("subcategory") val subcategory: String?,
    @SerializedName("difficulty") val difficulty: Int?,
    @SerializedName("set") val set: QuestionSet?,
)

data class Bonus(
    @SerializedName("leadin") val leadin: String,
    @SerializedName("leadin_sanitized") val leadinSanitized: String?,
    @SerializedName("parts") val parts: List<String>,
    @SerializedName("parts_sanitized") val partsSanitized: List<String>?,
    @SerializedName("answers") val answers: List<String>,
    @SerializedName("answers_sanitized") val answersSanitized: List<String>?,
    @SerializedName("category") val category: String?,
    @SerializedName("subcategory") val subcategory: String?,
    @SerializedName("difficulty") val difficulty: Int?,
    @SerializedName("set") val set: QuestionSet?,
)

data class QuestionSet(
    @SerializedName("name") val name: String?,
)

data class CheckAnswerResult(
    @SerializedName("directive") val directive: String, // "accept", "reject", "prompt"
    @SerializedName("directedPrompt") val directedPrompt: String?,
)

data class TossupResponse(
    @SerializedName("tossups") val tossups: List<Tossup>,
)

data class BonusResponse(
    @SerializedName("bonuses") val bonuses: List<Bonus>,
)

data class RoomListResponse(
    @SerializedName("roomList") val roomList: List<RoomInfo>,
)

data class RoomInfo(
    @SerializedName("name") val name: String?,
    @SerializedName("roomName") val roomName: String?,
    @SerializedName("playerCount") val playerCount: Int?,
    @SerializedName("players") val players: List<Any>?,
    @SerializedName("setName") val setName: String?,
    @SerializedName("mode") val mode: String?,
) {
    val displayName: String get() = name ?: roomName ?: ""
    val displayPlayerCount: Int get() = playerCount ?: players?.size ?: 0
}
