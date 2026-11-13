package com.jigmnnaga.presentation.home

enum class HomeTab { UPCOMING, PAST }

enum class ArrivalStatus { ON_TIME, LATE }

data class MockParticipant(
    val initial: String,
    val isMe: Boolean = false
)

/**
 * 홈 화면에서 표시할 약속 목업 모델.
 * API 연동 시 domain 레이어의 Appointment 모델로 교체한다.
 */
data class MockAppointment(
    val id: String,
    val title: String,
    val dateTime: String,
    val location: String,
    val participants: List<MockParticipant>,
    val participantCount: Int,
    val isToday: Boolean = false,
    // 지난 약속 전용
    val arrivalStatus: ArrivalStatus? = null,
    val lateMinutes: Int = 0
)

/**
 * 홈 화면 UI 상태.
 * API 연동 시 HomeViewModel이 이 상태를 StateFlow로 노출한다.
 */
data class HomeUiState(
    val selectedTab: HomeTab = HomeTab.UPCOMING,
    val upcomingAppointments: List<MockAppointment> = emptyList(),
    val pastAppointments: List<MockAppointment> = emptyList(),
    /** 오늘의 약속 배너에 표시할 가장 가까운 약속 */
    val todayAppointment: MockAppointment? = null,
    /** 대기 중인 약속 초대 수 */
    val pendingInviteCount: Int = 0
) {
    val currentAppointments: List<MockAppointment>
        get() = when (selectedTab) {
            HomeTab.UPCOMING -> upcomingAppointments
            HomeTab.PAST -> pastAppointments
        }

    companion object {
        private val mockParticipants = listOf(
            MockParticipant("나", isMe = true),
            MockParticipant("채"),
            MockParticipant("도"),
            MockParticipant("수")
        )

        private val mockTodayAppointment = MockAppointment(
            id = "today",
            title = "스터디 모임",
            dateTime = "오후 3:00",
            location = "강남역 카공카페",
            participants = mockParticipants,
            participantCount = 4,
            isToday = true
        )

        private val mockUpcoming = listOf(
            MockAppointment(
                id = "u1",
                title = "스터디 모임",
                dateTime = "10월 24일 토요일 · 오후 3:00",
                location = "강남역 카공카페",
                participants = mockParticipants,
                participantCount = 4,
                isToday = true
            ),
            MockAppointment(
                id = "u2",
                title = "스터디 모임",
                dateTime = "10월 24일 토요일 · 오후 3:00",
                location = "강남역 카공카페",
                participants = mockParticipants,
                participantCount = 4
            ),
            MockAppointment(
                id = "u3",
                title = "스터디 모임",
                dateTime = "10월 24일 토요일 · 오후 3:00",
                location = "강남역 카공카페",
                participants = mockParticipants,
                participantCount = 4
            )
        )

        private val mockPast = listOf(
            MockAppointment(
                id = "p1",
                title = "스터디 모임",
                dateTime = "10월 24일 토요일 · 오후 3:00",
                location = "강남역 카공카페",
                participants = mockParticipants,
                participantCount = 4,
                arrivalStatus = ArrivalStatus.LATE,
                lateMinutes = 12
            ),
            MockAppointment(
                id = "p2",
                title = "스터디 모임",
                dateTime = "10월 24일 토요일 · 오후 3:00",
                location = "강남역 카공카페",
                participants = mockParticipants,
                participantCount = 4,
                arrivalStatus = ArrivalStatus.ON_TIME
            ),
            MockAppointment(
                id = "p3",
                title = "스터디 모임",
                dateTime = "10월 24일 토요일 · 오후 3:00",
                location = "강남역 카공카페",
                participants = mockParticipants,
                participantCount = 4,
                arrivalStatus = ArrivalStatus.ON_TIME
            )
        )

        /** Preview: 목록 있는 상태 — 다가오는 약속 탭 */
        val PreviewWithList = HomeUiState(
            selectedTab = HomeTab.UPCOMING,
            upcomingAppointments = mockUpcoming,
            pastAppointments = mockPast,
            todayAppointment = mockTodayAppointment,
            pendingInviteCount = 1
        )

        /** Preview: 목록 있는 상태 — 지난 약속 탭 */
        val PreviewPastTab = PreviewWithList.copy(selectedTab = HomeTab.PAST)

        /** Preview: 빈 상태 — 다가오는 약속 탭 */
        val PreviewEmpty = HomeUiState()

        /** Preview: 빈 상태 — 지난 약속 탭 */
        val PreviewEmptyPast = HomeUiState(selectedTab = HomeTab.PAST)
    }
}
