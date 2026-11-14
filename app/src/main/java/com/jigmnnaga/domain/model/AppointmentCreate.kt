package com.jigmnnaga.domain.model

/** 날짜 선택 방식. 서버의 `dateSelectionType` 값과 같다. */
enum class NewAppointmentDateType {
    /** 하루만 후보로 정한다. */
    SINGLE,

    /** 연속된 기간을 후보로 정한다. */
    RANGE,

    /** 떨어진 여러 날짜를 후보로 정한다. */
    MULTIPLE
}

/** 새 약속의 후보 장소. 위치가 있어야 서버가 받아 준다. */
data class NewCandidateLocation(
    val placeName: String,
    val address: String?,
    /** 지하철역 검색 결과의 노선 문구. */
    val lineNames: String?,
    val latitude: Double,
    val longitude: Double
)

/** 이전 약속의 참여자를 바로 초대할 때 쓴다. (다시 약속 잡기) */
data class PreviousMembers(
    val appointmentId: Long,
    val memberIds: List<Long>
)

/**
 * 약속 생성 요청 내용.
 *
 * 날짜는 `yyyy-MM-dd`, 응답 기한은 `yyyy-MM-dd'T'HH:mm:ss`(한국 시간) 문자열이다.
 */
data class NewAppointment(
    val title: String,
    val dateType: NewAppointmentDateType,
    val candidateDates: List<String>,
    val candidateLocations: List<NewCandidateLocation>,
    val responseDeadline: String,
    val previousMembers: PreviousMembers? = null
)

/** 약속 생성에 성공했을 때 서버가 돌려준 값. */
data class CreatedAppointment(
    val appointmentId: Long,
    val inviteCode: String?,
    val inviteUrl: String?
)

/** 서버가 요청 내용을 받아 주지 않은 이유. */
enum class AppointmentCreateRejectReason {
    /** 지난 날짜, 중복 날짜, 선택 방식에 맞지 않는 날짜. */
    INVALID_DATES,

    /** 응답 기한이 지금 이전이거나 첫 후보 날짜 이후. */
    INVALID_DEADLINE,

    /** 후보 장소 개수나 내용이 맞지 않음. */
    INVALID_LOCATIONS,

    /** 이전 약속에 함께하지 않은 멤버를 초대함. */
    INVALID_PREVIOUS_MEMBERS,

    /** 그 밖의 잘못된 입력. */
    INVALID_INPUT
}

/** 약속 생성 API 호출 결과. */
sealed interface AppointmentCreateResult<out T> {
    data class Success<T>(val value: T) : AppointmentCreateResult<T>
    data object LoginRequired : AppointmentCreateResult<Nothing>
    data class Rejected(val reason: AppointmentCreateRejectReason) : AppointmentCreateResult<Nothing>
    data class Failure(val retryable: Boolean) : AppointmentCreateResult<Nothing>
}
