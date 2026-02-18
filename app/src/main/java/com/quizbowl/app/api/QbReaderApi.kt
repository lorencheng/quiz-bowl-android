package com.quizbowl.app.api

import com.quizbowl.app.api.models.BonusResponse
import com.quizbowl.app.api.models.CheckAnswerResult
import com.quizbowl.app.api.models.RoomInfo
import com.quizbowl.app.api.models.TossupResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface QbReaderApi {

    @GET("random-tossup")
    suspend fun randomTossup(
        @Query("number") number: Int = 1,
        @Query("categories") categories: String? = null,
        @Query("difficulties") difficulties: String? = null,
        @Query("minYear") minYear: Int? = null,
        @Query("maxYear") maxYear: Int? = null,
    ): TossupResponse

    @GET("random-bonus")
    suspend fun randomBonus(
        @Query("number") number: Int = 1,
        @Query("categories") categories: String? = null,
        @Query("difficulties") difficulties: String? = null,
        @Query("minYear") minYear: Int? = null,
        @Query("maxYear") maxYear: Int? = null,
    ): BonusResponse

    @GET("check-answer")
    suspend fun checkAnswer(
        @Query("answerline") answerline: String,
        @Query("givenAnswer") givenAnswer: String,
    ): CheckAnswerResult

    @GET("multiplayer/room-list")
    suspend fun roomList(): List<RoomInfo>
}
