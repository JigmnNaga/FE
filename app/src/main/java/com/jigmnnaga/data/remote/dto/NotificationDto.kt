package com.jigmnnaga.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class NotificationDto(
    val notificationId: Long,
    val type: String? = null,
    val title: String = "",
    val body: String = "",
    val appointmentId: Long? = null,
    val read: Boolean = false,
    val createdAt: String? = null
)

@Serializable
data class NotificationPageDto(
    val notifications: List<NotificationDto> = emptyList(),
    val nextCursor: Long? = null,
    val hasNext: Boolean = false
)

@Serializable
data class NotificationCountDto(
    val count: Long = 0
)
