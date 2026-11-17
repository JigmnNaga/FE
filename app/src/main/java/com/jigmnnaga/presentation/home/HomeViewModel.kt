package com.jigmnnaga.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jigmnnaga.domain.model.HomeAppointment
import com.jigmnnaga.domain.model.HomeAppointmentListResult
import com.jigmnnaga.domain.model.NextAppointmentResult
import com.jigmnnaga.domain.model.PendingInviteCountResult
import com.jigmnnaga.domain.repository.HomeRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 홈 화면 ViewModel.
 *
 * - 초기화 시 약속 목록·다음 약속·초대 수를 병렬로 조회한다.
 * - 탭 전환([selectTab])은 API 재호출 없이 이미 불러온 데이터를 재사용한다.
 * - [refresh]는 진행 중인 요청을 취소하고 전체 데이터를 다시 조회한다.
 * - 초대 API 오류가 약속 목록 표시를 막지 않도록 각 오류를 독립적으로 처리한다.
 */
class HomeViewModel(
    private val repository: HomeRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        doLoad()
    }

    // ── 공개 메서드 ──────────────────────────────────────────────────────────

    /** 탭 전환. 이미 불러온 목록을 재사용하므로 API를 재호출하지 않는다. */
    fun selectTab(tab: HomeTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    /** 전체 데이터를 다시 조회한다. 진행 중인 요청은 취소된다. */
    fun refresh() {
        loadJob?.cancel()
        _uiState.update {
            it.copy(isLoading = true, upcomingError = null, pastError = null)
        }
        doLoad()
    }

    /**
     * 로그인 화면 이동 후 호출. [HomeUiState.loginRequired]를 false로 초기화한다.
     * HomeRoute가 loginRequired 상태를 처리한 뒤 반드시 호출해야 한다.
     */
    fun consumeLoginRequired() {
        _uiState.update { it.copy(loginRequired = false) }
    }

    // ── 내부 로직 ────────────────────────────────────────────────────────────

    private fun doLoad() {
        loadJob = viewModelScope.launch(dispatcher) {
            val upcomingDeferred = async { repository.getUpcomingAppointments() }
            val pastDeferred = async { repository.getPastAppointments() }
            val nextDeferred = async { repository.getNextAppointment() }
            val inviteDeferred = async { repository.getPendingInviteCount() }

            val upcomingResult = upcomingDeferred.await()
            val pastResult = pastDeferred.await()
            val nextResult = nextDeferred.await()
            val inviteResult = inviteDeferred.await()

            val today = todayDateString()
            var loginRequired = false

            // ── 예정 약속 목록 ────────────────────────────────────────────
            val upcomingAppointments: List<MockAppointment>
            val upcomingError: HomeListError?
            when (upcomingResult) {
                is HomeAppointmentListResult.Success -> {
                    upcomingAppointments = upcomingResult.appointments.map { it.toMockAppointment(today) }
                    upcomingError = null
                }
                HomeAppointmentListResult.LoginRequired -> {
                    loginRequired = true
                    upcomingAppointments = _uiState.value.upcomingAppointments
                    upcomingError = null
                }
                is HomeAppointmentListResult.Failure -> {
                    upcomingAppointments = _uiState.value.upcomingAppointments
                    upcomingError = HomeListError(upcomingResult.retryable, upcomingResult.isNetworkError)
                }
            }

            // ── 지난 약속 목록 ────────────────────────────────────────────
            val pastAppointments: List<MockAppointment>
            val pastError: HomeListError?
            when (pastResult) {
                is HomeAppointmentListResult.Success -> {
                    pastAppointments = pastResult.appointments.map { it.toMockAppointment(today) }
                    pastError = null
                }
                HomeAppointmentListResult.LoginRequired -> {
                    loginRequired = true
                    pastAppointments = _uiState.value.pastAppointments
                    pastError = null
                }
                is HomeAppointmentListResult.Failure -> {
                    pastAppointments = _uiState.value.pastAppointments
                    pastError = HomeListError(pastResult.retryable, pastResult.isNetworkError)
                }
            }

            // ── 오늘의 약속 (다음 확정 약속 중 오늘인 것만) ──────────────
            val todayAppointment: MockAppointment? = when (nextResult) {
                is NextAppointmentResult.Success ->
                    nextResult.appointment
                        ?.takeIf { it.confirmedDate == today }
                        ?.toMockAppointment(today)
                NextAppointmentResult.LoginRequired -> {
                    loginRequired = true
                    null
                }
                is NextAppointmentResult.Failure ->
                    _uiState.value.todayAppointment  // 오류 시 기존 값 유지
            }

            // ── 대기 중인 초대 수 ─────────────────────────────────────────
            // 초대 API 오류는 약속 목록 표시를 막지 않는다.
            val pendingInviteCount: Int = when (inviteResult) {
                is PendingInviteCountResult.Success -> inviteResult.count
                PendingInviteCountResult.LoginRequired -> {
                    loginRequired = true
                    0
                }
                is PendingInviteCountResult.Failure ->
                    _uiState.value.pendingInviteCount  // 오류 시 기존 값 유지
            }

            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    loginRequired = state.loginRequired || loginRequired,
                    upcomingAppointments = upcomingAppointments,
                    pastAppointments = pastAppointments,
                    todayAppointment = todayAppointment,
                    pendingInviteCount = pendingInviteCount,
                    upcomingError = upcomingError,
                    pastError = pastError
                )
            }
        }
    }

    // ── HomeAppointment → MockAppointment 변환 ───────────────────────────────

    /**
     * 도메인 모델을 홈 화면 뷰 모델로 변환한다.
     *
     * - [today]: "yyyy-MM-dd" 형식의 오늘 날짜 문자열
     * - 참여자 초대 이미지(participants)는 홈 목록 API에서 제공되지 않으므로 빈 목록을 사용한다.
     *   memberCount 텍스트는 그대로 표시된다.
     * - 도착 상태(arrivalStatus)는 REST API로 조회 불가 (WebSocket 전용)이므로 null.
     */
    private fun HomeAppointment.toMockAppointment(today: String): MockAppointment = MockAppointment(
        id = appointmentId.toString(),
        title = title,
        dateTime = formatAppointmentDateTime(
            confirmedDate = confirmedDate,
            confirmedTime = confirmedTime,
            candidateStart = candidateStartDate,
            candidateEnd = candidateEndDate
        ),
        location = confirmedLocationName ?: detailPlaceName ?: "",
        participants = emptyList(),
        participantCount = memberCount,
        isToday = confirmedDate == today,
        arrivalStatus = null,
        lateMinutes = 0
    )

    // ── 날짜·시간 포맷 ────────────────────────────────────────────────────────

    /**
     * 확정 날짜/시간 또는 후보 날짜 범위를 화면 표시용 문자열로 변환한다.
     *
     * - 확정: "10월 24일 토요일 · 오후 3:00" / 시간 없으면 "10월 24일 토요일"
     * - 미확정: "10월 20일 ~ 10월 24일" (candidateStart~End 사용)
     */
    private fun formatAppointmentDateTime(
        confirmedDate: String?,
        confirmedTime: String?,
        candidateStart: String?,
        candidateEnd: String?
    ): String {
        if (confirmedDate != null) {
            val datePart = formatDate(confirmedDate)
            val timePart = confirmedTime?.let { formatTime(it) }
            return if (timePart != null) "$datePart · $timePart" else datePart
        }
        // 미확정 — 후보 날짜 범위
        return when {
            candidateStart != null && candidateEnd != null ->
                "${formatDate(candidateStart)} ~ ${formatDate(candidateEnd)}"
            candidateStart != null -> formatDate(candidateStart)
            else -> ""
        }
    }

    /** "yyyy-MM-dd" → "M월 d일 EEEE" (e.g. "10월 24일 토요일"). 파싱 실패 시 원본 반환. */
    private fun formatDate(yyyyMmDd: String): String = try {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).parse(yyyyMmDd)!!
        SimpleDateFormat("M월 d일 EEEE", Locale.KOREA).format(date)
    } catch (e: Exception) {
        yyyyMmDd
    }

    /** "HH:mm" → "a h:mm" (e.g. "오후 3:00"). 파싱 실패 시 원본 반환. */
    private fun formatTime(hhMm: String): String = try {
        val time = SimpleDateFormat("HH:mm", Locale.KOREA).parse(hhMm)!!
        SimpleDateFormat("a h:mm", Locale.KOREA).format(time)
    } catch (e: Exception) {
        hhMm
    }

    /** 오늘 날짜를 "yyyy-MM-dd" 형식으로 반환한다. */
    private fun todayDateString(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(Date())
}
