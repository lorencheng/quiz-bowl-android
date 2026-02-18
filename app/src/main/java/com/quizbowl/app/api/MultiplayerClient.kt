package com.quizbowl.app.api

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.TimeUnit

/**
 * WebSocket client for qbreader.org multiplayer.
 * Messages are JSON objects with a "type" field.
 * Port of multiplayer.js MultiplayerClient.
 */
class MultiplayerClient {

    companion object {
        private const val WS_BASE = "wss://www.qbreader.org"
        private const val PING_INTERVAL_MS = 30_000L

        fun generateUserId(): String =
            "user_" + (1..8).map { ('a'..'z').random() }.joinToString("")
    }

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS) // No timeout for WebSocket
        .build()

    private var webSocket: WebSocket? = null
    private var pingTimer: Timer? = null

    var userId: String? = null
    var username: String? = null
    var roomName: String? = null

    // Event flow — emits Pair<type, JsonObject>
    private val _events = MutableSharedFlow<Pair<String, JsonObject>>(extraBufferCapacity = 64)
    val events: SharedFlow<Pair<String, JsonObject>> = _events.asSharedFlow()

    fun connect(roomName: String, username: String, userId: String = generateUserId()) {
        this.roomName = roomName
        this.username = username
        this.userId = userId

        val url = "$WS_BASE/play/mp/room/${roomName.encodeUrlComponent()}" +
            "?roomName=${roomName.encodeUrlComponent()}" +
            "&userId=${userId.encodeUrlComponent()}" +
            "&username=${username.encodeUrlComponent()}"

        val request = Request.Builder().url(url).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                emit("_connected", JsonObject())
                startPing()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val obj = gson.fromJson(text, JsonObject::class.java)
                    val type = obj.get("type")?.asString ?: return
                    emit(type, obj)
                } catch (e: Exception) {
                    // Ignore malformed messages
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                cleanup()
                val data = JsonObject().apply { addProperty("reason", reason) }
                emit("_disconnected", data)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                cleanup()
                val data = JsonObject().apply { addProperty("error", t.message) }
                emit("_error", data)
                emit("_disconnected", data)
            }
        })
    }

    fun send(type: String, data: Map<String, Any> = emptyMap()) {
        val obj = JsonObject().apply {
            addProperty("type", type)
            data.forEach { (k, v) ->
                when (v) {
                    is String -> addProperty(k, v)
                    is Number -> addProperty(k, v)
                    is Boolean -> addProperty(k, v)
                    else -> addProperty(k, v.toString())
                }
            }
        }
        webSocket?.send(gson.toJson(obj))
    }

    fun disconnect() {
        cleanup()
        webSocket?.close(1000, "User left")
        webSocket = null
    }

    // Convenience actions (mirror web app API)
    fun buzz() = send("buzz")
    fun next() = send("next")
    fun chat(message: String) = send("chat", mapOf("message" to message))
    fun giveAnswer(givenAnswer: String) = send("give-answer", mapOf("givenAnswer" to givenAnswer))
    fun giveAnswerLiveUpdate(givenAnswer: String) = send("give-answer-live-update", mapOf("givenAnswer" to givenAnswer))

    private fun emit(type: String, data: JsonObject) {
        _events.tryEmit(type to data)
    }

    private fun startPing() {
        pingTimer = Timer().apply {
            scheduleAtFixedRate(object : TimerTask() {
                override fun run() { send("ping") }
            }, PING_INTERVAL_MS, PING_INTERVAL_MS)
        }
    }

    private fun cleanup() {
        pingTimer?.cancel()
        pingTimer = null
    }

    private fun String.encodeUrlComponent(): String =
        java.net.URLEncoder.encode(this, "UTF-8")
}
