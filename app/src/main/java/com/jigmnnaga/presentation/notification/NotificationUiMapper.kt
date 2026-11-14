package com.jigmnnaga.presentation.notification

import com.jigmnnaga.domain.model.AppNotification
import com.jigmnnaga.domain.model.NotificationType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val INVITE_TITLE_REGEX = Regex("^(.+?)님이 '(.+)' 약속에 초대했어요")
private val QUOTED_TITLE_REGEX = Regex("'(.+?)'")

private const val MINUTE_MILLIS = 60_000L
private const val HOUR_MILLIS = 60 * MINUTE_MILLIS
private const val DAY_MILLIS = 24 * HOUR_MILLIS

internal fun AppNotification.toInviteItem(nowMillis: Long): InviteNotificationItem {
    val match = INVITE_TITLE_REGEX.find(title) ?: INVITE_TITLE_REGEX.find(body)
    return InviteNotificationItem(
        id = id,
        inviterName = match?.groupValues?.get(1).orEmpty(),
        metaLabel = formatRelativeTime(createdAtMillis, nowMillis),
        appointmentTitle = match?.groupValues?.get(2) ?: title,
        status = if (read) InviteNotificationStatus.HANDLED else InviteNotificationStatus.PENDING,
        appointmentId = appointmentId
    )
}

internal fun AppNotification.toActivityItem(nowMillis: Long): ActivityNotificationItem {
    val appointmentName = (QUOTED_TITLE_REGEX.find(body) ?: QUOTED_TITLE_REGEX.find(title))
        ?.groupValues?.get(1)
    val subtitle = listOfNotNull(
        appointmentName,
        formatRelativeTime(createdAtMillis, nowMillis).takeIf { it.isNotEmpty() }
    ).joinToString(" · ")
    return ActivityNotificationItem(
        id = id,
        type = type.toActivityType(),
        message = title,
        subtitle = subtitle,
        appointmentId = appointmentId
    )
}

private fun NotificationType.toActivityType(): ActivityNotificationType = when (this) {
    NotificationType.DEPARTURE_INPUT_REQUESTED -> ActivityNotificationType.DEPARTED
    NotificationType.ATTENDANCE_REQUESTED -> ActivityNotificationType.ARRIVED
    NotificationType.APPOINTMENT_CONFIRMED -> ActivityNotificationType.JOINED
    else -> ActivityNotificationType.REMINDER
}

/** 초대(INVITED)는 초대 탭, 나머지는 활동 탭에 보여 준다. */
internal fun List<AppNotification>.toTabItems(
    nowMillis: Long
): Pair<List<InviteNotificationItem>, List<ActivityNotificationItem>> {
    val (invites, others) = partition { it.type == NotificationType.INVITED }
    return invites.map { it.toInviteItem(nowMillis) } to others.map { it.toActivityItem(nowMillis) }
}

/** "방금", "N분 전", "N시간 전", "어제", "M월 d일". 시각을 모르면 빈 문자열. */
internal fun formatRelativeTime(createdAtMillis: Long?, nowMillis: Long): String {
    if (createdAtMillis == null) return ""
    val diff = nowMillis - createdAtMillis
    return when {
        diff < MINUTE_MILLIS -> "방금"
        diff < HOUR_MILLIS -> "${diff / MINUTE_MILLIS}분 전"
        diff < DAY_MILLIS -> "${diff / HOUR_MILLIS}시간 전"
        diff < 2 * DAY_MILLIS -> "어제"
        else -> SimpleDateFormat("M월 d일", Locale.KOREA).format(Date(createdAtMillis))
    }
}
