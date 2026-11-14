package com.jigmnnaga.presentation.notification

/** 알림 화면 상단 탭. */
enum class NotificationTab { INVITE, ACTIVITY }

/** 초대 알림의 처리 상태. 처리 전에는 "확인하기" 버튼, 처리 후에는 안내 문구를 보여 준다. */
enum class InviteNotificationStatus { PENDING, HANDLED }

/** 활동 알림 유형. 유형마다 앞에 붙는 이모지가 다르다. */
enum class ActivityNotificationType { DEPARTED, ARRIVED, JOINED, MESSAGE, REMINDER }

/**
 * 초대 알림 한 건. 화면에 보이는 문구는 호출하는 쪽이 만들어서 넘긴다.
 *
 * @param inviterName 초대한 사람 이름. 아바타에는 첫 글자만 표시한다.
 * @param metaLabel "방금 · 4명"처럼 시간과 인원을 합친 문구
 */
data class InviteNotificationItem(
    val id: Long,
    val inviterName: String,
    val metaLabel: String,
    val appointmentTitle: String,
    val status: InviteNotificationStatus
)

/**
 * 활동 알림 한 건.
 *
 * @param message "지우가 도착했어요"처럼 알림 본문
 * @param subtitle "금요일 저녁 모임 · 32분 전"처럼 약속명과 시간을 합친 문구
 */
data class ActivityNotificationItem(
    val id: Long,
    val type: ActivityNotificationType,
    val message: String,
    val subtitle: String
)

/**
 * 알림 화면 UI 상태. 서버 연동 전에는 목록이 비어 있고, 연동 후 ViewModel이 채운다.
 */
data class NotificationUiState(
    val selectedTab: NotificationTab = NotificationTab.INVITE,
    val invites: List<InviteNotificationItem> = emptyList(),
    val activities: List<ActivityNotificationItem> = emptyList()
) {
    /** 지금 선택된 탭에 보여 줄 알림이 없으면 true. */
    val isCurrentTabEmpty: Boolean
        get() = when (selectedTab) {
            NotificationTab.INVITE -> invites.isEmpty()
            NotificationTab.ACTIVITY -> activities.isEmpty()
        }
}
