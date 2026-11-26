package com.jigmnnaga.data.mapper

import com.jigmnnaga.data.remote.dto.NotificationSettingsDto
import com.jigmnnaga.domain.model.NotificationSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationSettingsMapperTest {

    @Test
    fun `서버 DTO를 도메인 모델로 바꾼다`() {
        val dto = NotificationSettingsDto(
            allNotificationsEnabled = true,
            appointmentReminderEnabled = true,
            departureTimeEnabled = false,
            friendArrivalEnabled = true,
            marketingEnabled = false,
        )

        assertEquals(
            NotificationSettings(
                allNotificationsEnabled = true,
                appointmentReminderEnabled = true,
                departureTimeEnabled = false,
                friendArrivalEnabled = true,
                marketingEnabled = false,
            ),
            dto.toDomain(),
        )
    }

    @Test
    fun `도메인 모델을 서버 DTO로 바꾼다`() {
        val settings = NotificationSettings(
            allNotificationsEnabled = false,
            appointmentReminderEnabled = true,
            departureTimeEnabled = true,
            friendArrivalEnabled = false,
            marketingEnabled = true,
        )

        assertEquals(
            NotificationSettingsDto(
                allNotificationsEnabled = false,
                appointmentReminderEnabled = true,
                departureTimeEnabled = true,
                friendArrivalEnabled = false,
                marketingEnabled = true,
            ),
            settings.toDto(),
        )
    }

    @Test
    fun `기본 설정을 DTO로 바꾸면 기본값이 그대로다`() {
        val dto = NotificationSettings().toDto()

        assertEquals(true, dto.allNotificationsEnabled)
        assertEquals(true, dto.appointmentReminderEnabled)
        assertEquals(true, dto.departureTimeEnabled)
        assertEquals(true, dto.friendArrivalEnabled)
        assertEquals(false, dto.marketingEnabled)
    }

    @Test
    fun `DTO를 도메인으로 바꾸고 다시 DTO로 바꾸면 원래 값과 같다`() {
        val original = NotificationSettingsDto(
            allNotificationsEnabled = true,
            appointmentReminderEnabled = false,
            departureTimeEnabled = true,
            friendArrivalEnabled = false,
            marketingEnabled = true,
        )

        assertEquals(original, original.toDomain().toDto())
    }
}
