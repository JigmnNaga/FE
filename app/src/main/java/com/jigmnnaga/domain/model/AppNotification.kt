package com.jigmnnaga.domain.model

/** 서버가 보내는 알림 유형. 모르는 값은 [OTHER]로 받는다. */
enum class NotificationType {
    INVITED,
    SCHEDULE_INPUT_REQUESTED,
    APPOINTMENT_CONFIRMED,
    APPOINTMENT_CHANGED,
    APPOINTMENT_CANCELED,
    ATTENDANCE_REQUESTED,
    DEPARTURE_INPUT_REQUESTED,
    HOST_DELEGATED,
    KICKED,
    VOTE_DEADLINE_SOON,
    RESPONSE_DEADLINE_CLOSED,
    APPOINTMENT_SOON,
    RESCHEDULE_REQUESTED,
    OTHER
}

/**
 * 알림 한 건.
 *
 * @param createdAtMillis 서버가 준 생성 시각을 기기 시간대 기준 epoch 밀리초로 바꾼 값. 읽지 못하면 null
 */
data class AppNotification(
    val id: Long,
    val type: NotificationType,
    val title: String,
    val body: String,
    val appointmentId: Long?,
    val read: Boolean,
    val createdAtMillis: Long?
)

data class NotificationPage(
    val items: List<AppNotification>,
    val nextCursor: Long?,
    val hasNext: Boolean
)

/** 알림 API 호출 결과. */
sealed interface NotificationResult<out T> {
    data class Success<T>(val value: T) : NotificationResult<T>
    data object LoginRequired : NotificationResult<Nothing>
    data class Failure(val retryable: Boolean) : NotificationResult<Nothing>
}
