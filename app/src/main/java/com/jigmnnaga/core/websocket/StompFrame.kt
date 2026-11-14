package com.jigmnnaga.core.websocket

/**
 * STOMP 1.2 프레임. 서버(Spring)는 WebSocket 텍스트 메시지 하나에 프레임 하나를 담아 보낸다.
 * 프레임은 `명령\n헤더:값\n...\n\n본문\u0000` 형식이다.
 */
data class StompFrame(
    val command: String,
    val headers: Map<String, String> = emptyMap(),
    val body: String = ""
) {
    fun encode(): String {
        val escape = command.escapesHeaders()
        return buildString {
            append(command).append('\n')
            headers.forEach { (name, value) ->
                append(if (escape) escapeHeader(name) else name)
                append(':')
                append(if (escape) escapeHeader(value) else value)
                append('\n')
            }
            append('\n').append(body).append(NULL)
        }
    }

    companion object {
        private const val NULL = '\u0000'

        const val CONNECT = "CONNECT"
        const val CONNECTED = "CONNECTED"
        const val SEND = "SEND"
        const val SUBSCRIBE = "SUBSCRIBE"
        const val UNSUBSCRIBE = "UNSUBSCRIBE"
        const val DISCONNECT = "DISCONNECT"
        const val MESSAGE = "MESSAGE"
        const val ERROR = "ERROR"

        /** 서버가 보낸 텍스트를 프레임으로 바꾼다. 하트비트(빈 줄)나 읽을 수 없는 값이면 null. */
        fun parse(text: String): StompFrame? {
            val content = text.trimStart('\n', '\r').substringBefore(NULL)
            if (content.isBlank()) return null

            val separator = content.indexOf("\n\n")
            val head = if (separator >= 0) content.substring(0, separator) else content
            val body = if (separator >= 0) content.substring(separator + 2) else ""

            val lines = head.split('\n').map { it.trimEnd('\r') }
            val command = lines.first().trim()
            if (command.isEmpty()) return null
            val unescape = command.escapesHeaders()

            val headers = LinkedHashMap<String, String>()
            for (line in lines.drop(1)) {
                val colon = line.indexOf(':')
                if (colon <= 0) continue
                val name = line.substring(0, colon).let { if (unescape) unescapeHeader(it) else it }
                val value = line.substring(colon + 1).let { if (unescape) unescapeHeader(it) else it }
                // 같은 헤더가 여러 번 오면 첫 번째 값만 쓴다.
                headers.putIfAbsent(name, value)
            }
            return StompFrame(command, headers, body)
        }

        // CONNECT·CONNECTED 프레임의 헤더는 이스케이프하지 않는다.
        private fun String.escapesHeaders() = this != CONNECT && this != CONNECTED

        private fun escapeHeader(value: String): String = buildString {
            value.forEach { char ->
                when (char) {
                    '\\' -> append("\\\\")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    ':' -> append("\\c")
                    else -> append(char)
                }
            }
        }

        private fun unescapeHeader(value: String): String = buildString {
            var index = 0
            while (index < value.length) {
                val char = value[index]
                if (char == '\\' && index + 1 < value.length) {
                    when (value[index + 1]) {
                        'n' -> append('\n')
                        'r' -> append('\r')
                        'c' -> append(':')
                        '\\' -> append('\\')
                        else -> append(char).append(value[index + 1])
                    }
                    index += 2
                } else {
                    append(char)
                    index++
                }
            }
        }
    }
}
