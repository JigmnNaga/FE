package com.jigmnnaga.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * 회원 위치 공유 설정. 조회 응답과 저장 요청이 같은 모양이다.
 * [scope]는 PARTICIPANTS(약속에 참여한 친구만) 또는 NONE(공유 안 함), [startMinutesBefore]는 10·30·60 중 하나다.
 */
@Serializable
data class LocationSharingSettingsDto(
    val enabled: Boolean,
    val scope: String,
    val startMinutesBefore: Int
)
