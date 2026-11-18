package com.jigmnnaga.data.remote.dto

import kotlinx.serialization.Serializable

/** 기본 위치 목록(`GET api/v1/members/me/locations`)의 한 항목. */
@Serializable
data class MemberLocationDto(
    val locationId: Long,
    val name: String,
    val address: String? = null,
    val latitude: Double,
    val longitude: Double
)
