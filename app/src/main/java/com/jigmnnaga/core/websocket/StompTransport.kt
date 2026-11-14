package com.jigmnnaga.core.websocket

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

/** WebSocket 연결 하나. STOMP 클라이언트가 실제 연결 방식에 묶이지 않도록 나눠 둔다. */
interface StompTransport {
    /** 텍스트를 보낸다. 연결이 닫혔거나 큐에 넣지 못하면 false. */
    fun send(text: String): Boolean
    fun close()
}

/** [StompTransport]가 보내는 이벤트. 호출 스레드는 연결 구현에 따라 다르다. */
interface StompTransportListener {
    fun onOpen(transport: StompTransport)
    fun onText(text: String)
    fun onClosed()
    fun onFailure(error: Throwable)
}

fun interface StompTransportFactory {
    fun open(url: String, listener: StompTransportListener): StompTransport
}

/** OkHttp WebSocket으로 [StompTransport]를 만든다. */
class OkHttpStompTransportFactory(private val client: OkHttpClient) : StompTransportFactory {

    override fun open(url: String, listener: StompTransportListener): StompTransport {
        val request = Request.Builder().url(url).build()
        val webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                listener.onOpen(OkHttpStompTransport(webSocket))
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                listener.onText(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(code, reason)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                listener.onClosed()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                listener.onFailure(t)
            }
        })
        return OkHttpStompTransport(webSocket)
    }
}

private class OkHttpStompTransport(private val webSocket: WebSocket) : StompTransport {
    override fun send(text: String): Boolean = webSocket.send(text)
    override fun close() {
        webSocket.close(NORMAL_CLOSURE, null)
    }

    private companion object {
        const val NORMAL_CLOSURE = 1000
    }
}
