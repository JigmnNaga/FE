package com.jigmnnaga.domain.repository

import com.jigmnnaga.domain.model.MemberLocation
import com.jigmnnaga.domain.model.MemberLocationResult

interface MemberLocationRepository {
    /** 내가 저장한 기본 위치를 등록한 순서대로 가져온다. */
    suspend fun getLocations(): MemberLocationResult<List<MemberLocation>>
}
