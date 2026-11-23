package com.jigmnnaga.data.mapper

import com.jigmnnaga.data.remote.dto.MyInfoResponseDto
import com.jigmnnaga.domain.model.MyProfile

internal fun MyInfoResponseDto.toDomain(): MyProfile = MyProfile(
    memberId = memberId,
    nickname = nickname,
    email = email,
    profileImageUrl = profileImageUrl
)
