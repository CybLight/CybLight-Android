package org.cyblight.android.data.realtime

import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.cyblight.android.BuildConfig
import org.cyblight.android.data.session.SessionManager
import org.cyblight.android.util.AppUserAgent
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class ChatWebSocketClient(
    private val sessionManager: SessionManager,
    private val onEvent: (ChatWsEvent) -> Unit,
    private val onConnectionChanged: (Boolean) -> Unit = {},
) {
    private val gson = Gson()
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(45, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var webSocket: WebSocket? = null

    private val running = AtomicBoolean(false)
    private var reconnectJob: Job? = null
    private var connectAttempts = 0

    val isConnected: Boolean
        get() = webSocket != null

    fun start(scope: CoroutineScope) {
        val wasRunning = !running.compareAndSet(false, true)
        reconnectJob?.cancel()
        if (wasRunning) {
            connect(scope)
        } else {
            connectAttempts = 0
            connect(scope)
        }
    }

    fun stop() {
        running.set(false)
        reconnectJob?.cancel()
        reconnectJob = null
        webSocket?.close(1000, "stop")
        webSocket = null
        onConnectionChanged(false)
    }

    private fun connect(scope: CoroutineScope) {
        if (!running.get()) return

        val token = runCatching {
            runBlocking { sessionManager.getToken() }
        }.getOrNull()
        if (token.isNullOrBlank()) {
            scheduleReconnect(scope)
            return
        }

        val wsUrl = buildWebSocketUrl()
        val request = Request.Builder()
            .url(wsUrl)
            .header("Authorization", "Bearer $token")
            .header("X-CybLight-Client", "android")
            .header("User-Agent", AppUserAgent.build())
            .header("Origin", BuildConfig.WEBSITE_URL)
            .build()

        webSocket?.cancel()
        webSocket = httpClient.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    connectAttempts = 0
                    onConnectionChanged(true)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    if (text == "pong") return
                    runCatching {
                        val event = gson.fromJson(text, ChatWsEvent::class.java)
                        if (event?.type == "message.new") {
                            onEvent(event)
                        }
                    }
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    webSocket.close(code, reason)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    handleDisconnect(webSocket, scope)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    handleDisconnect(webSocket, scope)
                }
            },
        )
    }

    private fun handleDisconnect(closedSocket: WebSocket, scope: CoroutineScope) {
        if (webSocket === closedSocket) {
            webSocket = null
        }
        onConnectionChanged(false)
        scheduleReconnect(scope)
    }

    private fun scheduleReconnect(scope: CoroutineScope) {
        if (!running.get()) return
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            connectAttempts += 1
            val delayMs = minOf(30_000L, 1_000L shl minOf(connectAttempts, 5))
            delay(delayMs)
            if (isActive && running.get()) {
                connect(scope)
            }
        }
    }

    private fun buildWebSocketUrl(): String {
        val base = BuildConfig.API_BASE_URL.trim().trimEnd('/')
        val wsBase = when {
            base.startsWith("https://") -> "wss://" + base.removePrefix("https://")
            base.startsWith("http://") -> "ws://" + base.removePrefix("http://")
            else -> "wss://$base"
        }
        return "$wsBase/ws/messages"
    }
}
