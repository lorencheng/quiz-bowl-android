package com.quizbowl.app.data

import android.graphics.Bitmap
import android.os.Build
import android.util.Base64
import com.quizbowl.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.concurrent.TimeUnit

object FeedbackRepository {

    // Deploy scripts/feedback_webhook.gs as a Google Apps Script Web App and paste
    // the deployment URL here before releasing.
    private const val ENDPOINT = "https://script.google.com/macros/s/AKfycbzubvI6-05flN05WGvoynaX6OEi87wuc1m5rujCDWQpmhdVcakk5KY5KaGs_EZ1uxQS/exec"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun submit(feedbackText: String, screenshot: Bitmap?): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (ENDPOINT.isEmpty()) {
                return@withContext Result.failure(Exception("Feedback endpoint not configured — see FeedbackRepository.ENDPOINT"))
            }
            runCatching {
                val body = JSONObject().apply {
                    put("timestamp", Instant.now().toString())
                    put("feedback", feedbackText)
                    put("appVersion", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                    put("androidVersion", "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                    put("deviceModel", "${Build.MANUFACTURER} ${Build.MODEL}".trim())
                    if (screenshot != null) put("screenshot", encodeBitmap(screenshot))
                }.toString()

                val request = Request.Builder()
                    .url(ENDPOINT)
                    .post(body.toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) error("HTTP ${response.code}")
                }
            }
        }

    private fun encodeBitmap(bitmap: Bitmap): String {
        val scaled = if (bitmap.width > 720) {
            val ratio = 720f / bitmap.width
            Bitmap.createScaledBitmap(bitmap, 720, (bitmap.height * ratio).toInt(), true)
        } else bitmap
        val stream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 60, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }
}
