package com.jigmnnaga.domain.model

/** 마이페이지에 저장한 기본 위치 한 곳(집, 회사, 학교 등). */
data class MemberLocation(
    val locationId: Long,
    val name: String,
    val address: String? = null,
    val latitude: Double,
    val longitude: Double
)

/** 기본 위치를 등록하거나 수정할 때 서버에 보내는 값. */
data class MemberLocationInput(
    val name: String,
    val address: String? = null,
    val latitude: Double,
    val longitude: Double
)

/** 서버가 기본 위치 요청을 받아들이지 않은 이유. */
enum class MemberLocationRejectReason {
    /** 저장할 수 있는 개수를 넘었다. */
    LIMIT_EXCEEDED,

    /** 이미 지워졌거나 내 위치가 아니다. */
    NOT_FOUND,

    /** 이름이 비었거나 좌표가 올바르지 않다. */
    INVALID_INPUT
}

/** 기본 위치 API 호출 결과. */
sealed interface MemberLocationResult<out T> {
    data class Success<T>(val value: T) : MemberLocationResult<T>
    data object LoginRequired : MemberLocationResult<Nothing>
    data class Rejected(val reason: MemberLocationRejectReason) : MemberLocationResult<Nothing>
    data class Failure(val retryable: Boolean) : MemberLocationResult<Nothing>
}
