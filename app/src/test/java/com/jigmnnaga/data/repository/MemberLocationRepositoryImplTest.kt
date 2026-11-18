package com.jigmnnaga.data.repository

import com.jigmnnaga.data.mapper.toDomain
import com.jigmnnaga.data.remote.dto.MemberLocationDto
import com.jigmnnaga.domain.model.MemberLocationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MemberLocationRepositoryImplTest {
    @Test
    fun `서버 응답을 기본 위치로 바꾼다`() {
        val location = MemberLocationDto(7, "집", "  ", 37.5, 127.0).toDomain()
        assertEquals(7L, location.locationId)
        assertNull(location.address)
    }

    @Test
    fun `응답 코드에 따라 실패 종류를 나눈다`() {
        assertEquals(MemberLocationResult.LoginRequired, mapMemberLocationError(401))
        assertEquals(MemberLocationResult.Failure(retryable = true), mapMemberLocationError(500))
        assertEquals(MemberLocationResult.Failure(retryable = false), mapMemberLocationError(404))
    }
}
