package com.jigmnnaga.data.repository

import com.jigmnnaga.data.mapper.toDomain
import com.jigmnnaga.data.mapper.toRequestDto
import com.jigmnnaga.data.remote.dto.MemberLocationDto
import com.jigmnnaga.domain.model.MemberLocationInput
import com.jigmnnaga.domain.model.MemberLocationRejectReason
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

    @Test
    fun `서버 오류 코드로 거절 이유를 나눈다`() {
        assertEquals(
            MemberLocationResult.Rejected(MemberLocationRejectReason.LIMIT_EXCEEDED),
            mapMemberLocationError(400, "MEMBER_LOCATION_LIMIT_EXCEEDED")
        )
        assertEquals(
            MemberLocationResult.Rejected(MemberLocationRejectReason.NOT_FOUND),
            mapMemberLocationError(404, "MEMBER_LOCATION_NOT_FOUND")
        )
        assertEquals(
            MemberLocationResult.Rejected(MemberLocationRejectReason.INVALID_INPUT),
            mapMemberLocationError(400, "INVALID_INPUT_VALUE")
        )
        assertEquals(
            MemberLocationResult.Rejected(MemberLocationRejectReason.INVALID_INPUT),
            mapMemberLocationError(400, null)
        )
    }

    @Test
    fun `토큰 만료 코드는 로그인이 필요한 것으로 본다`() {
        assertEquals(MemberLocationResult.LoginRequired, mapMemberLocationError(403, "EXPIRED_ACCESS_TOKEN"))
        assertEquals(MemberLocationResult.LoginRequired, mapMemberLocationError(401, "UNAUTHORIZED"))
    }

    @Test
    fun `알 수 없는 오류 코드는 다시 시도할 수 없는 실패로 본다`() {
        assertEquals(MemberLocationResult.Failure(retryable = false), mapMemberLocationError(409, "SOMETHING_ELSE"))
    }

    @Test
    fun `등록 요청은 이름과 주소의 앞뒤 공백을 지우고 빈 주소는 보내지 않는다`() {
        val dto = MemberLocationInput("  집 ", "  ", 37.5, 127.0).toRequestDto()
        assertEquals("집", dto.name)
        assertNull(dto.address)

        val withAddress = MemberLocationInput("회사", " 서울 서초구 서초대로 45 ", 37.4, 127.0).toRequestDto()
        assertEquals("서울 서초구 서초대로 45", withAddress.address)
        assertEquals(37.4, withAddress.latitude, 0.0)
        assertEquals(127.0, withAddress.longitude, 0.0)
    }
}
