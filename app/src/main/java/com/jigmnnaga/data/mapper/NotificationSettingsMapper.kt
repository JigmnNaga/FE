package com.jigmnnaga.data.mapper

import com.jigmnnaga.data.remote.dto.NotificationSettingsDto
import com.jigmnnaga.domain.model.NotificationSettings

/** 서버 값을 앱 설정으로 바꾼다. */
fun NotificationSettingsDto.toDomain(): NotificationSettings = NotificationSettings(
    allNotificationsEnabled = allNotificationsEnabled,
    appointmentReminderEnabled = appointmentReminderEnabled,
    departureTimeEnabled = departureTimeEnabled,
    friendArrivalEnabled = friendArrivalEnabled,
    marketingEnabled = marketingEnabled,
)

/** 앱 설정을 서버로 보낼 값으로 바꾼다. */
fun NotificationSettings.toDto(): NotificationSettingsDto = NotificationSettingsDto(
    allNotificationsEnabled = allNotificationsEnabled,
    appointmentReminderEnabled = appointmentReminderEnabled,
    departureTimeEnabled = departureTimeEnabled,
    friendArrivalEnabled = friendArrivalEnabled,
    marketingEnabled = marketingEnabled,
)
