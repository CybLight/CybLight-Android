package org.cyblight.android.data.realtime

import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicReference
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
        .pingInterval(25, TimeUnit.SECONDS)
        .build()

    private val webSocketRef = AtomicReference<WebSocket?>(null)

    private val running = AtomicBoolean(false)
    private var reconnectJob: Job? = null
    private var pingJob: Job? = null
    private var connectAttempts = 0

    val isConnected: Boolean
        get() = webSocketRef.get() != null

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
        pingJob?.cancel()
        pingJob = null
        closeGracefully("stop")
        onConnectionChanged(false)
    }

    private fun closeGracefully(reason: String) {
        val socket = webSocketRef.getAndSet(null) ?: return
        runCatching { socket.close(1000, reason) }
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

        closeGracefully("reconnect")
        webSocketRef.set(
            httpClient.newWebSocket(
                request,
                object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        connectAttempts = 0
                        onConnectionChanged(true)
                        startApplicationPing(scope, webSocket)
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        if (text == "pong") return
                        runCatching {
                            val event = gson.fromJson(text, ChatWsEvent::class.java) ?: return@runCatching
                            when (event.type) {
                                "message.new", "message.deleted", "message.edited" -> onEvent(event)
                            }
                        }
                    }

                    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                        if (isTransmittableCloseCode(code)) {
                            runCatching { webSocket.close(code, reason) }
                        }
                    }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        handleDisconnect(webSocket, scope)
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        handleDisconnect(webSocket, scope)
                    }
                },
            ),
        )
    }

    private fun startApplicationPing(scope: CoroutineScope, webSocket: WebSocket) {
        pingJob?.cancel()
        pingJob = scope.launch {
            while (isActive && running.get() && webSocketRef.get() === webSocket) {
                delay(25_000)
                if (!isActive || !running.get() || webSocketRef.get() !== webSocket) break
                runCatching { webSocket.send("ping") }
            }
        }
    }

    private fun handleDisconnect(closedSocket: WebSocket, scope: CoroutineScope) {
        if (webSocketRef.compareAndSet(closedSocket, null)) {
            pingJob?.cancel()
            pingJob = null
            onConnectionChanged(false)
            scheduleReconnect(scope)
        }
    }

    private fun isTransmittableCloseCode(code: Int): Boolean =
        code in 1000..4999 && code !in setOf(1004, 1005, 1006, 1015)

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
