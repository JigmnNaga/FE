package com.jigmnnaga.data.mapper

import com.jigmnnaga.data.remote.dto.NotificationDto
import com.jigmnnaga.data.remote.dto.NotificationPageDto
import com.jigmnnaga.domain.model.AppNotification
import com.jigmnnaga.domain.model.NotificationPage
import com.jigmnnaga.domain.model.NotificationType
import java.text.SimpleDateFormat
import java.util.Locale

internal fun NotificationPageDto.toDomain(): NotificationPage = NotificationPage(
    items = notifications.map { it.toDomain() },
    nextCursor = nextCursor,
    hasNext = hasNext
)

internal fun NotificationDto.toDomain(): AppNotification = AppNotification(
    id = notificationId,
    type = type.toNotificationType(),
    title = title,
    body = body,
    appointmentId = appointmentId,
    read = read,
    createdAtMillis = createdAt?.let(::parseServerDateTime)
)

private fun String?.toNotificationType(): NotificationType {
    val name = this?.uppercase() ?: return NotificationType.OTHER
    return NotificationType.values().firstOrNull { it.name == name } ?: NotificationType.OTHER
}

/**
 * 서버의 `2026-11-14T13:20:15.123456` 형식을 기기 시간대 기준 epoch 밀리초로 바꾼다.
 * minSdk 24에서는 java.time을 쓸 수 없어서 SimpleDateFormat으로 초 단위까지만 읽는다.
 * 읽을 수 없는 값이면 null.
 */
internal fun parseServerDateTime(value: String): Long? {
    if (value.length < 19) return null
    return try {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            .apply { isLenient = false }
            .parse(value.substring(0, 19))
            ?.time
    } catch (e: Exception) {
        null
    }
}
