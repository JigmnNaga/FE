package com.jigmnnaga.domain.model

/** 마이페이지에 저장한 기본 위치 한 곳(집, 회사, 학교 등). */
data class MemberLocation(
    val locationId: Long,
    val name: String,
    val address: String? = null,
    val latitude: Double,
    val longitude: Double
)

/** 기본 위치 API 호출 결과. */
sealed interface MemberLocationResult<out T> {
    data class Success<T>(val value: T) : MemberLocationResult<T>
    data object LoginRequired : MemberLocationResult<Nothing>
    data class Failure(val retryable: Boolean) : MemberLocationResult<Nothing>
}
