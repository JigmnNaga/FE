package com.jigmnnaga.data.mapper

import com.jigmnnaga.data.remote.dto.LocationSharingSettingsDto
import com.jigmnnaga.domain.model.LocationSharingScope
import com.jigmnnaga.domain.model.LocationSharingSettings
import com.jigmnnaga.domain.model.LocationSharingStartTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocationSharingSettingsMapperTest {
    @Test
    fun `서버의 참여한 친구만 범위는 앱의 친구만 범위가 된다`() {
        val settings = LocationSharingSettingsDto(true, "PARTICIPANTS", 30).toDomain()

        assertEquals(
            LocationSharingSettings(true, LocationSharingScope.FRIENDS_ONLY, LocationSharingStartTime.MINUTES_30),
            settings
        )
    }

    @Test
    fun `공유 안 함 범위와 꺼짐 상태를 그대로 옮긴다`() {
        val settings = LocationSharingSettingsDto(false, "NONE", 10).toDomain()

        assertEquals(
            LocationSharingSettings(false, LocationSharingScope.NONE, LocationSharingStartTime.MINUTES_10),
            settings
        )
    }

    @Test
    fun `시작 시간 10 30 60분을 앱의 시작 시간으로 바꾼다`() {
        assertEquals(LocationSharingStartTime.MINUTES_10, LocationSharingSettingsDto(true, "NONE", 10).toDomain()?.startTime)
        assertEquals(LocationSharingStartTime.MINUTES_30, LocationSharingSettingsDto(true, "NONE", 30).toDomain()?.startTime)
        assertEquals(LocationSharingStartTime.HOUR_1, LocationSharingSettingsDto(true, "NONE", 60).toDomain()?.startTime)
    }

    @Test
    fun `앱이 모르는 범위나 시작 시간이 오면 변환하지 않는다`() {
        assertNull(LocationSharingSettingsDto(true, "EVERYONE", 30).toDomain())
        assertNull(LocationSharingSettingsDto(true, "PARTICIPANTS", 15).toDomain())
    }

    @Test
    fun `앱 설정을 서버로 보낼 값으로 바꾼다`() {
        val dto = LocationSharingSettings(
            sharingEnabled = false,
            scope = LocationSharingScope.FRIENDS_ONLY,
            startTime = LocationSharingStartTime.HOUR_1
        ).toDto()

        assertEquals(LocationSharingSettingsDto(false, "PARTICIPANTS", 60), dto)
        assertEquals("NONE", LocationSharingSettings(scope = LocationSharingScope.NONE).toDto().scope)
    }
}
