package com.quizbowl.app.api

import com.google.gson.GsonBuilder
import com.quizbowl.app.api.models.BonusResponse
import com.quizbowl.app.api.models.CheckAnswerResult
import com.quizbowl.app.api.models.RoomInfo
import com.quizbowl.app.api.models.TossupResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object QbReaderService {

    private const val BASE_URL = "https://www.qbreader.org/api/"
    private const val RATE_LIMIT_MS = 50L

    private var lastRequestTime = 0L

    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .addInterceptor { chain ->
            // Rate limiting: 20 req/sec
            val now = System.currentTimeMillis()
            val elapsed = now - lastRequestTime
            if (elapsed < RATE_LIMIT_MS) {
                Thread.sleep(RATE_LIMIT_MS - elapsed)
            }
            lastRequestTime = System.currentTimeMillis()
            chain.proceed(chain.request())
        }
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val gson = GsonBuilder().setLenient().create()

    private val api: QbReaderApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
        .create(QbReaderApi::class.java)

    /** Normalize answer: leading "-N" → "negative N" */
    fun normalizeAnswer(answer: String): String =
        answer.replace(Regex("^-(\\d)"), "negative $1")

    val categories = listOf(
        "Literature", "History", "Science", "Fine Arts",
        "Religion", "Mythology", "Philosophy", "Social Science",
        "Current Events", "Geography", "Other Academic", "Trash",
    )

    val difficulties = listOf(
        0 to "Unrated",
        1 to "1 - Middle School",
        2 to "2 - Easy High School",
        3 to "3 - Regular High School",
        4 to "4 - Hard High School",
        5 to "5 - Easy College",
        6 to "6 - Regular College",
        7 to "7 - Hard College",
        8 to "8 - Easy Open",
        9 to "9 - Regular Open",
        10 to "10 - National",
    )

    suspend fun getRandomTossup(
        categories: List<String> = emptyList(),
        difficulties: List<Int> = emptyList(),
    ): TossupResponse = api.randomTossup(
        categories = categories.takeIf { it.isNotEmpty() }?.joinToString(","),
        difficulties = difficulties.takeIf { it.isNotEmpty() }?.joinToString(","),
    )

    suspend fun getRandomBonus(
        categories: List<String> = emptyList(),
        difficulties: List<Int> = emptyList(),
    ): BonusResponse = api.randomBonus(
        categories = categories.takeIf { it.isNotEmpty() }?.joinToString(","),
        difficulties = difficulties.takeIf { it.isNotEmpty() }?.joinToString(","),
    )

    suspend fun checkAnswer(givenAnswer: String, answerline: String): CheckAnswerResult =
        api.checkAnswer(
            answerline = answerline,
            givenAnswer = normalizeAnswer(givenAnswer),
        )

    suspend fun getRoomList(): List<RoomInfo> = api.roomList().roomList
}
