package com.jigmnnaga.domain.model

/** 후보 장소 변경 요청. 서버는 유지할 기존 장소 ID와 새로 추가할 장소를 따로 받고, 빠진 장소는 삭제한다. */
data class CandidateLocationChange(
    val keepLocationIds: List<Long>,
    val newLocations: List<NewCandidateLocation>
)

/** 미응답자 알림 유형별 발송 인원. [type]은 서버의 알림 유형 이름 그대로다. */
data class ReminderCount(
    val type: String,
    val sentCount: Int
)

/** 미응답자 알림 발송 결과. */
data class ReminderSendSummary(
    val sentCount: Int,
    val reminders: List<ReminderCount>
)

/** 서버가 관리 요청을 받아 주지 않은 이유. */
enum class AppointmentManageRejectReason {
    APPOINTMENT_CANCELED,

    /** 날짜가 중복·과거이거나 선택 방식과 맞지 않을 때. */
    INVALID_DATES,

    /** 응답 기한이 현재 이전이거나 첫 후보 날짜 이후일 때. */
    INVALID_DEADLINE,

    /** 후보 날짜가 응답 기한보다 빠를 때. 응답 기한을 먼저 바꿔야 한다. */
    DATES_BEFORE_DEADLINE,

    /** 후보 장소가 1~5개가 아니거나 약속의 후보 장소가 아닐 때. */
    INVALID_LOCATIONS,

    /** 존재하지 않는 회원일 때. */
    INVALID_MEMBER,
    INVALID_INPUT
}

/** 방장 약속방 관리 API 호출 결과. */
sealed interface AppointmentManageResult<out T> {
    data class Success<T>(val value: T) : AppointmentManageResult<T>
    data object LoginRequired : AppointmentManageResult<Nothing>

    /** 방장(또는 참여자)이 아니거나 약속이 없을 때. */
    data object NotHost : AppointmentManageResult<Nothing>
    data class Rejected(val reason: AppointmentManageRejectReason) : AppointmentManageResult<Nothing>
    data class Failure(val retryable: Boolean) : AppointmentManageResult<Nothing>
}
