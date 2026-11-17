package com.jigmnnaga.presentation.home

import com.jigmnnaga.presentation.common.error.ErrorType

enum class HomeTab { UPCOMING, PAST }

enum class ArrivalStatus { ON_TIME, LATE }

data class MockParticipant(
    val initial: String,
    val isMe: Boolean = false
)

/**
 * 홈 화면에서 표시할 약속 뷰 모델.
 * API 연동 전에는 목업 데이터로, API 연동 후에는 HomeViewModel이 HomeAppointment를 변환해 채운다.
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

/** 약속 목록 로드 오류. */
data class HomeListError(val retryable: Boolean, val isNetworkError: Boolean = false)

fun HomeListError.toErrorType(): ErrorType =
    if (isNetworkError) ErrorType.Network else ErrorType.Service

/**
 * 홈 화면 UI 상태.
 *
 * - [isLoading]: 최초 로드 또는 새로 고침 진행 중
 * - [loginRequired]: 인증 필요 — Route에서 로그인 화면으로 이동 후 [consumeLoginRequired] 호출
 * - [upcomingError], [pastError]: 각 목록의 오류 (null이면 정상)
 */
data class HomeUiState(
    val selectedTab: HomeTab = HomeTab.UPCOMING,
    val upcomingAppointments: List<MockAppointment> = emptyList(),
    val pastAppointments: List<MockAppointment> = emptyList(),
    /** 오늘의 약속 배너에 표시할 가장 가까운 약속 (오늘 날짜가 아니면 null) */
    val todayAppointment: MockAppointment? = null,
    /** 대기 중인 약속 초대 수 */
    val pendingInviteCount: Int = 0,
    // ── 로딩/오류 상태 ──────────────────────────────────────────────────────
    val isLoading: Boolean = false,
    val loginRequired: Boolean = false,
    val upcomingError: HomeListError? = null,
    val pastError: HomeListError? = null
) {
    val currentAppointments: List<MockAppointment>
        get() = when (selectedTab) {
            HomeTab.UPCOMING -> upcomingAppointments
            HomeTab.PAST -> pastAppointments
        }

    val currentTabError: HomeListError?
        get() = when (selectedTab) {
            HomeTab.UPCOMING -> upcomingError
            HomeTab.PAST -> pastError
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

        /** Preview: 로딩 중 */
        val PreviewLoading = HomeUiState(isLoading = true)

        /** Preview: 오류 — 빈 목록 (재시도 가능) */
        val PreviewError = HomeUiState(upcomingError = HomeListError(retryable = true))

        /** Preview: 오류 — 목록 있음 (배너) */
        val PreviewErrorWithData = PreviewWithList.copy(upcomingError = HomeListError(retryable = true))
    }
}
