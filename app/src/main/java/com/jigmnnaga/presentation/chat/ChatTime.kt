package com.jigmnnaga.presentation.chat

/**
 * 서버 시각 문자열(`yyyy-MM-dd'T'HH:mm:ss`, 뒤에 소수 초가 붙을 수 있다)을 `오후 1:22` 꼴로 바꾼다.
 * minSdk 24에서는 java.time을 쓸 수 없어 문자열에서 시·분만 읽는다. 읽을 수 없으면 null.
 */
fun formatChatTime(sentAt: String?): String? {
    val time = sentAt?.substringAfter('T', missingDelimiterValue = "")?.takeIf { it.length >= 5 } ?: return null
    val hour = time.substring(0, 2).toIntOrNull()?.takeIf { it in 0..23 } ?: return null
    if (time[2] != ':') return null
    val minute = time.substring(3, 5).toIntOrNull()?.takeIf { it in 0..59 } ?: return null
    val period = if (hour < 12) "오전" else "오후"
    val displayHour = when (val h = hour % 12) {
        0 -> 12
        else -> h
    }
    return "$period $displayHour:${minute.toString().padStart(2, '0')}"
}
