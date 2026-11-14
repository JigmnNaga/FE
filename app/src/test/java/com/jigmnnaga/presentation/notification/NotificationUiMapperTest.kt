package com.jigmnnaga.presentation.notification

import com.jigmnnaga.data.mapper.parseServerDateTime
import com.jigmnnaga.domain.model.AppNotification
import com.jigmnnaga.domain.model.NotificationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationUiMapperTest {

    private val now = 1_000_000_000_000L

    private fun notification(
        id: Long = 1,
        type: NotificationType,
        title: String = "",
        body: String = "",
        read: Boolean = false,
        createdAt: Long? = now - 5 * 60_000L,
        appointmentId: Long? = 9L
    ) = AppNotification(id, type, title, body, appointmentId, read, createdAt)

    @Test
    fun `상대 시간 표기`() {
        assertEquals("방금", formatRelativeTime(now - 10_000L, now))
        assertEquals("5분 전", formatRelativeTime(now - 5 * 60_000L, now))
        assertEquals("3시간 전", formatRelativeTime(now - 3 * 3_600_000L, now))
        assertEquals("어제", formatRelativeTime(now - 30 * 3_600_000L, now))
        assertEquals("", formatRelativeTime(null, now))
    }

    @Test
    fun `초대 알림은 초대한 사람과 약속명을 문구에서 뽑고 읽음 여부로 상태를 정한다`() {
        val item = notification(
            type = NotificationType.INVITED,
            title = "태양님이 '금요일 저녁 모임' 약속에 초대했어요"
        ).toInviteItem(now)
        assertEquals("태양", item.inviterName)
        assertEquals("금요일 저녁 모임", item.appointmentTitle)
        assertEquals("5분 전", item.metaLabel)
        assertEquals(InviteNotificationStatus.PENDING, item.status)
        assertEquals(9L, item.appointmentId)

        val read = notification(type = NotificationType.INVITED, title = "x", read = true).toInviteItem(now)
        assertEquals(InviteNotificationStatus.HANDLED, read.status)
    }

    @Test
    fun `문구 형식이 달라도 제목을 약속명으로 쓰고 이름은 비운다`() {
        val item = notification(type = NotificationType.INVITED, title = "초대가 왔어요").toInviteItem(now)
        assertEquals("", item.inviterName)
        assertEquals("초대가 왔어요", item.appointmentTitle)
    }

    @Test
    fun `활동 알림 유형과 부제`() {
        val item = notification(
            type = NotificationType.ATTENDANCE_REQUESTED,
            title = "도착했나요?",
            body = "'스터디 모임' 약속 장소에 도착했는지 알려주세요"
        ).toActivityItem(now)
        assertEquals(ActivityNotificationType.ARRIVED, item.type)
        assertEquals("도착했나요?", item.message)
        assertEquals("스터디 모임 · 5분 전", item.subtitle)

        assertEquals(
            ActivityNotificationType.REMINDER,
            notification(type = NotificationType.KICKED).toActivityItem(now).type
        )
    }

    @Test
    fun `초대는 초대 탭, 나머지는 활동 탭으로 나뉜다`() {
        val (invites, activities) = listOf(
            notification(id = 1, type = NotificationType.INVITED, title = "a"),
            notification(id = 2, type = NotificationType.APPOINTMENT_CONFIRMED, title = "b")
        ).toTabItems(now)
        assertEquals(listOf(1L), invites.map { it.id })
        assertEquals(listOf(2L), activities.map { it.id })
    }

    @Test
    fun `서버 시각 문자열을 읽고 잘못된 값은 null`() {
        assertNotNull(parseServerDateTime("2026-11-14T13:20:15.123456"))
        assertNull(parseServerDateTime("not a date"))
        assertNull(parseServerDateTime("2026-13-45T99:00:00"))
    }
}
