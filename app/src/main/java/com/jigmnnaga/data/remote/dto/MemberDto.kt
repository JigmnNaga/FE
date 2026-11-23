package com.jigmnnaga.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * GET api/v1/members/me 응답 DTO.
 * 직접 올린 프로필 이미지는 1시간 유효한 Presigned URL로 내려오므로 화면 진입 시마다 재조회한다.
 * locations 필드는 ignoreUnknownKeys=true로 무시된다.
 */
@Serializable
data class MyInfoResponseDto(
    val memberId: Long,
    val nickname: String,
    val profileImageUrl: String? = null,
    val email: String
)

/**
 * PATCH api/v1/members/me/profile 요청 DTO.
 * nickname과 profileImageKey 중 최소 하나는 반드시 포함해야 한다.
 */
@Serializable
data class ProfileUpdateRequestDto(
    val nickname: String? = null,
    val profileImageKey: String? = null
)

/**
 * POST api/v1/members/me/profile-image/upload-url 요청 DTO.
 */
@Serializable
data class ProfileImageUploadUrlRequestDto(
    val contentType: String,
    val contentLength: Long
)

/**
 * POST api/v1/members/me/profile-image/upload-url 응답 DTO.
 */
@Serializable
data class ProfileImageUploadUrlResponseDto(
    val uploadUrl: String,
    val profileImageKey: String,
    val expiresAt: String
)

/**
 * GET api/v1/members/me/stats 응답 DTO.
 * onTimeRate는 약속이 0개일 때 null로 내려온다.
 */
@Serializable
data class AppointmentStatsResponseDto(
    val totalCount: Int,
    val onTimeCount: Int,
    val lateCount: Int,
    val onTimeRate: Double? = null
)
