package com.jigmnnaga.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class KakaoLoginRequestDto(
    val kakaoAccessToken: String
)

@Serializable
data class LoginResponseDto(
    val memberId: Long,
    val accessToken: String,
    val refreshToken: String,
    val isNewMember: Boolean
)
