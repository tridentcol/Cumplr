package com.cumplr.core.data.remote

import android.util.Log
import com.cumplr.core.data.remote.dto.TaskDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "Realtime"
private const val HEARTBEAT_MS = 25_000L

/**
 * Manages a persistent WebSocket connection to Supabase Realtime.
 * Subscribes to postgres_changes on the tasks table and delivers parsed
 * TaskDto objects via a callback whenever a row is inserted or updated.
 * Heartbeats keep the connection alive; polling in the ViewModel acts as
 * a fallback if the WebSocket is unavailable.
 */
@Singleton
class SupabaseRealtimeClient @Inject constructor() {

    private val wsClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val refSeq = AtomicInteger(0)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var ws: WebSocket? = null
    private var heartbeat: Job? = null
    private var onTask: ((TaskDto) -> Unit)? = null

    fun connect(accessToken: String, filter: String, onTask: (TaskDto) -> Unit) {
        disconnect()
        this.onTask = onTask

        val url = SupabaseConfig.url
            .replace("https://", "wss://")
            .replace("http://", "ws://") +
            "/realtime/v1/websocket?apikey=${SupabaseConfig.anonKey}&vsn=1.0.0"

        val request = Request.Builder()
            .url(url)
            .header("apikey", SupabaseConfig.anonKey)
            .header("Authorization", "Bearer $accessToken")
            .build()

        Log.d(TAG, "Connecting  filter=$filter")

        ws = wsClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(socket: WebSocket, response: Response) {
                Log.d(TAG, "Opened")
                val ref = refSeq.incrementAndGet().toString()
                socket.send(buildJoinMessage(filter, accessToken, ref))
                startHeartbeat(socket)
            }

            override fun onMessage(socket: WebSocket, text: String) = parseMessage(text)

            override fun onFailure(socket: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "Failure: ${t.message}")
                stopHeartbeat()
            }

            override fun onClosed(socket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "Closed $code")
                stopHeartbeat()
            }
        })
    }

    fun disconnect() {
        stopHeartbeat()
        ws?.close(1000, null)
        ws = null
        onTask = null
        Log.d(TAG, "Disconnected")
    }

    private fun buildJoinMessage(filter: String, token: String, ref: String): String =
        """{"topic":"realtime:cumplr-tasks","event":"phx_join","payload":{"config":{"broadcast":{"ack":false,"self":false},"presence":{"key":""},"postgres_changes":[{"event":"*","schema":"public","table":"tasks","filter":"$filter"}],"private":false},"access_token":"$token"},"ref":"$ref","join_ref":"$ref"}"""

    private fun startHeartbeat(socket: WebSocket) {
        stopHeartbeat()
        heartbeat = scope.launch {
            while (isActive) {
                delay(HEARTBEAT_MS)
                val ref = refSeq.incrementAndGet().toString()
                socket.send("""{"topic":"phoenix","event":"heartbeat","payload":{},"ref":"$ref"}""")
                Log.d(TAG, "Heartbeat $ref")
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeat?.cancel()
        heartbeat = null
    }

    private fun parseMessage(text: String) {
        try {
            val root = json.parseToJsonElement(text).jsonObject
            if (root["event"]?.jsonPrimitive?.contentOrNull != "postgres_changes") return
            val data = root["payload"]?.jsonObject?.get("data")?.jsonObject ?: return
            if (data["type"]?.jsonPrimitive?.contentOrNull == "DELETE") return
            val record = data["record"]?.jsonObject ?: return
            val dto = json.decodeFromJsonElement(TaskDto.serializer(), record)
            Log.d(TAG, "Event taskId=${dto.id} status=${dto.status}")
            onTask?.invoke(dto)
        } catch (e: Exception) {
            Log.w(TAG, "parseMessage error: ${e.message}")
        }
    }
}
