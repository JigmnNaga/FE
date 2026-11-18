package com.jigmnnaga.domain.repository

import com.jigmnnaga.domain.model.MemberLocation
import com.jigmnnaga.domain.model.MemberLocationInput
import com.jigmnnaga.domain.model.MemberLocationResult

interface MemberLocationRepository {
    /** 내가 저장한 기본 위치를 등록한 순서대로 가져온다. */
    suspend fun getLocations(): MemberLocationResult<List<MemberLocation>>

    /** 기본 위치를 새로 등록한다. */
    suspend fun createLocation(input: MemberLocationInput): MemberLocationResult<MemberLocation>

    /** 등록한 기본 위치의 이름과 위치를 바꾼다. */
    suspend fun updateLocation(
        locationId: Long,
        input: MemberLocationInput
    ): MemberLocationResult<MemberLocation>

    /** 등록한 기본 위치를 지운다. */
    suspend fun deleteLocation(locationId: Long): MemberLocationResult<Unit>
}
