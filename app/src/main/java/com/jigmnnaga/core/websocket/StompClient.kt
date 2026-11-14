package com.jigmnnaga.core.websocket

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

enum class StompConnectionState {
    /** 연결하지 않았거나 끊어졌다. [StompClient.connect] 뒤에 끊어지면 곧 다시 연결을 시도한다. */
    DISCONNECTED,
    CONNECTING,
    CONNECTED,

    /** 저장된 로그인 토큰이 없어 연결하지 못했다. 로그인한 뒤 다시 [StompClient.connect]를 부른다. */
    LOGIN_REQUIRED
}

/** 구독한 목적지로 온 메시지. [body]는 서버가 보낸 JSON 문자열 그대로다. */
data class StompMessage(
    val destination: String,
    val body: String,
    val headers: Map<String, String> = emptyMap()
)

/**
 * 서버 STOMP 엔드포인트에 연결하는 공통 클라이언트. 채팅과 실시간 위치 공유가 함께 쓴다.
 *
 * - [connect]로 연결하고 [disconnect]로 끊는다. [connect]는 여러 번 불러도 연결은 하나다.
 * - [subscribe]는 모으는 동안만 구독한다. 연결 전에 구독해도 연결되면 구독하고, 끊겼다 다시 연결되면 다시 구독한다.
 * - 연결이 끊기면 [retryDelayMillis] 간격으로 다시 연결한다. 다시 연결할 때마다 토큰을 새로 읽는다.
 * - [accessTokens]가 비면(로그아웃) 연결을 끊는다.
 *
 * 연결 구현 스레드에서 호출이 들어오므로 상태 변경은 모두 [lock] 안에서 한다.
 */
class StompClient(
    private val serverUrl: String,
    private val accessToken: suspend () -> String?,
    private val transportFactory: StompTransportFactory,
    private val scope: CoroutineScope,
    private val retryDelayMillis: (attempt: Int) -> Long = ::defaultRetryDelayMillis,
    accessTokens: Flow<String?>? = null
) {
    private class Subscription(val destination: String, val onMessage: (StompMessage) -> Unit)

    private val lock = Any()
    private val _connectionState = MutableStateFlow(StompConnectionState.DISCONNECTED)
    val connectionState: StateFlow<StompConnectionState> = _connectionState.asStateFlow()

    private var wanted = false
    private var generation = 0
    private var attempt = 0
    private var transport: StompTransport? = null
    private var pendingConnect: Job? = null
    private var nextSubscriptionId = 0
    private val subscriptions = LinkedHashMap<String, Subscription>()

    init {
        accessTokens?.let { tokens ->
            scope.launch {
                tokens.distinctUntilChanged().collect { token ->
                    if (token.isNullOrBlank()) disconnect()
                }
            }
        }
    }

    fun connect() {
        synchronized(lock) {
            if (wanted) return
            wanted = true
            attempt = 0
        }
        scheduleConnect(delayMillis = 0)
    }

    fun disconnect() {
        synchronized(lock) {
            wanted = false
            generation++
            pendingConnect?.cancel()
            pendingConnect = null
            transport?.let {
                it.send(StompFrame(StompFrame.DISCONNECT).encode())
                it.close()
            }
            transport = null
            _connectionState.value = StompConnectionState.DISCONNECTED
        }
    }

    /** [destination]을 구독한다. 이 Flow를 모으는 동안만 구독이 유지된다. */
    fun subscribe(destination: String): Flow<StompMessage> = callbackFlow {
        val id = addSubscription(destination) { trySend(it) }
        awaitClose { removeSubscription(id) }
    }

    /** [destination]으로 [body]를 보낸다. 연결되어 있지 않거나 보내지 못하면 false. */
    fun send(destination: String, body: String): Boolean = synchronized(lock) {
        if (_connectionState.value != StompConnectionState.CONNECTED) return false
        val frame = StompFrame(
            command = StompFrame.SEND,
            headers = mapOf("destination" to destination, "content-type" to "application/json"),
            body = body
        )
        transport?.send(frame.encode()) ?: false
    }

    private fun addSubscription(destination: String, onMessage: (StompMessage) -> Unit): String =
        synchronized(lock) {
            val id = "sub-${nextSubscriptionId++}"
            subscriptions[id] = Subscription(destination, onMessage)
            if (_connectionState.value == StompConnectionState.CONNECTED) sendSubscribe(id, destination)
            id
        }

    private fun removeSubscription(id: String) {
        synchronized(lock) {
            if (subscriptions.remove(id) == null) return
            if (_connectionState.value == StompConnectionState.CONNECTED) {
                transport?.send(StompFrame(StompFrame.UNSUBSCRIBE, mapOf("id" to id)).encode())
            }
        }
    }

    private fun sendSubscribe(id: String, destination: String) {
        val frame = StompFrame(
            StompFrame.SUBSCRIBE,
            mapOf("id" to id, "destination" to destination, "ack" to "auto")
        )
        transport?.send(frame.encode())
    }

    private fun scheduleConnect(delayMillis: Long) {
        val job = scope.launch {
            if (delayMillis > 0) delay(delayMillis)
            open()
        }
        synchronized(lock) { pendingConnect = job }
    }

    private suspend fun open() {
        val token = accessToken()
        val openedGeneration: Int
        synchronized(lock) {
            if (!wanted) return
            if (token.isNullOrBlank()) {
                wanted = false
                _connectionState.value = StompConnectionState.LOGIN_REQUIRED
                return
            }
            openedGeneration = ++generation
            _connectionState.value = StompConnectionState.CONNECTING
        }

        val validToken = token ?: return
        val opened = transportFactory.open(serverUrl, ConnectionListener(openedGeneration, validToken))
        synchronized(lock) {
            if (generation == openedGeneration) transport = opened else opened.close()
        }
    }

    /** 연결이 끊기거나 서버가 오류를 보냈을 때. 아직 연결을 원하면 잠시 뒤 다시 연결한다. */
    private fun onConnectionLost(lostGeneration: Int, closeTransport: Boolean) {
        val retryAfter: Long
        synchronized(lock) {
            if (lostGeneration != generation) return
            generation++
            if (closeTransport) transport?.close()
            transport = null
            _connectionState.value = StompConnectionState.DISCONNECTED
            if (!wanted) return
            retryAfter = retryDelayMillis(attempt)
            attempt++
        }
        scheduleConnect(retryAfter)
    }

    private inner class ConnectionListener(
        private val connectionGeneration: Int,
        private val token: String
    ) : StompTransportListener {

        override fun onOpen(transport: StompTransport) {
            synchronized(lock) {
                if (connectionGeneration != generation) {
                    transport.close()
                    return
                }
                this@StompClient.transport = transport
                val frame = StompFrame(
                    command = StompFrame.CONNECT,
                    headers = mapOf(
                        "accept-version" to "1.2",
                        "heart-beat" to "0,0",
                        "Authorization" to "Bearer $token"
                    )
                )
                transport.send(frame.encode())
            }
        }

        override fun onText(text: String) {
            val frame = StompFrame.parse(text) ?: return
            when (frame.command) {
                StompFrame.CONNECTED -> handleConnected()
                StompFrame.MESSAGE -> handleMessage(frame)
                StompFrame.ERROR -> onConnectionLost(connectionGeneration, closeTransport = true)
            }
        }

        override fun onClosed() = onConnectionLost(connectionGeneration, closeTransport = false)

        override fun onFailure(error: Throwable) = onConnectionLost(connectionGeneration, closeTransport = false)

        private fun handleConnected() {
            synchronized(lock) {
                if (connectionGeneration != generation) return
                attempt = 0
                _connectionState.value = StompConnectionState.CONNECTED
                subscriptions.forEach { (id, subscription) -> sendSubscribe(id, subscription.destination) }
            }
        }

        private fun handleMessage(frame: StompFrame) {
            val subscription = synchronized(lock) {
                if (connectionGeneration != generation) return
                subscriptions[frame.headers["subscription"]]
            } ?: return
            subscription.onMessage(
                StompMessage(
                    destination = frame.headers["destination"] ?: subscription.destination,
                    body = frame.body,
                    headers = frame.headers
                )
            )
        }
    }
}

/** 다시 연결하는 간격. 1초부터 두 배씩 늘려 30초를 넘기지 않는다. */
fun defaultRetryDelayMillis(attempt: Int): Long =
    (1_000L shl attempt.coerceIn(0, 5)).coerceAtMost(30_000L)

/** HTTP 서버 주소에서 STOMP WebSocket 주소를 만든다. 예: `https://host/` → `wss://host/ws`. */
fun stompServerUrl(baseUrl: String): String {
    val trimmed = baseUrl.trim().trimEnd('/')
    val base = when {
        trimmed.startsWith("https://") -> "wss://" + trimmed.removePrefix("https://")
        trimmed.startsWith("http://") -> "ws://" + trimmed.removePrefix("http://")
        else -> trimmed
    }
    return "$base/ws"
}
