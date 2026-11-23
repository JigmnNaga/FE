package com.jigmnnaga.data.mapper

import com.jigmnnaga.data.remote.dto.AppointmentStatsResponseDto
import com.jigmnnaga.data.remote.dto.MyInfoResponseDto
import com.jigmnnaga.domain.model.MyProfile
import com.jigmnnaga.domain.model.MyStats

internal fun MyInfoResponseDto.toDomain(): MyProfile = MyProfile(
    memberId = memberId,
    nickname = nickname,
    email = email,
    profileImageUrl = profileImageUrl
)

internal fun AppointmentStatsResponseDto.toDomain(): MyStats = MyStats(
    totalCount = totalCount,
    onTimeCount = onTimeCount,
    lateCount = lateCount,
    onTimeRate = onTimeRate
)
