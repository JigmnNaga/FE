package com.jigmnnaga.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jigmnnaga.domain.model.CalendarDay
import com.jigmnnaga.domain.model.CalendarResult
import com.jigmnnaga.domain.model.HomeAppointment
import com.jigmnnaga.domain.repository.CalendarRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * 캘린더 화면 ViewModel.
 *
 * - 초기화 시 현재 월의 약속을 조회한다.
 * - [goToPrevMonth]/[goToNextMonth]는 진행 중인 요청을 취소하고 새 월을 조회한다.
 * - [selectDate]는 날짜를 선택/해제하며 API를 재호출하지 않는다.
 * - [retry]는 현재 월을 다시 조회한다.
 * - [consumeLoginRequired]는 로그인 화면 이동 후 loginRequired 플래그를 초기화한다.
 */
class CalendarViewModel(
    private val repository: CalendarRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    today: CalendarDate = currentDate()
) : ViewModel() {

    val today: CalendarDate = today

    private val _uiState = MutableStateFlow(
        CalendarUiState(
            displayMonth = CalendarMonth(today.year, today.month),
            today = today,
            appointments = emptyList(),
            isLoading = true
        )
    )
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        doLoad(_uiState.value.displayMonth)
    }

    // ── 공개 메서드 ──────────────────────────────────────────────────────────

    /** 이전 달로 이동하고 해당 월의 약속을 조회한다. */
    fun goToPrevMonth() {
        val prevMonth = _uiState.value.displayMonth.prev()
        _uiState.update {
            it.copy(
                displayMonth = prevMonth,
                selectedDate = null,
                isLoading = true,
                error = null
            )
        }
        doLoad(prevMonth)
    }

    /** 다음 달로 이동하고 해당 월의 약속을 조회한다. */
    fun goToNextMonth() {
        val nextMonth = _uiState.value.displayMonth.next()
        _uiState.update {
            it.copy(
                displayMonth = nextMonth,
                selectedDate = null,
                isLoading = true,
                error = null
            )
        }
        doLoad(nextMonth)
    }

    /**
     * 날짜를 선택하거나 이미 선택된 날짜를 탭하면 선택을 해제한다.
     * API를 재호출하지 않는다.
     */
    fun selectDate(day: Int) {
        _uiState.update { state ->
            val current = state.displayMonth
            val newDate = CalendarDate(current.year, current.month, day)
            val next = if (state.selectedDate == newDate) null else newDate
            state.copy(selectedDate = next)
        }
    }

    /** 현재 월을 다시 조회한다. */
    fun retry() {
        val month = _uiState.value.displayMonth
        _uiState.update { it.copy(isLoading = true, error = null) }
        doLoad(month)
    }

    /**
     * 로그인 화면 이동 후 호출. [CalendarUiState.loginRequired]를 false로 초기화한다.
     * CalendarRoute가 loginRequired 상태를 처리한 뒤 반드시 호출해야 한다.
     */
    fun consumeLoginRequired() {
        _uiState.update { it.copy(loginRequired = false) }
    }

    // ── 내부 로직 ────────────────────────────────────────────────────────────

    private fun doLoad(month: CalendarMonth) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch(dispatcher) {
            val result = repository.getCalendar(formatMonthParam(month))

            _uiState.update { state ->
                // 응답이 도착했을 때 표시 중인 월이 이미 바뀌었으면 무시한다 (경쟁 조건 방지)
                if (state.displayMonth != month) return@update state

                when (result) {
                    is CalendarResult.Success -> {
                        val appointments = result.days
                            .flatMap { it.toUiModels(today) }
                        state.copy(
                            isLoading = false,
                            appointments = appointments,
                            error = null
                        )
                    }
                    CalendarResult.LoginRequired -> {
                        state.copy(
                            isLoading = false,
                            loginRequired = true
                        )
                    }
                    is CalendarResult.Failure -> {
                        state.copy(
                            isLoading = false,
                            error = CalendarError(retryable = result.retryable)
                        )
                    }
                }
            }
        }
    }

    // ── CalendarDay → MockCalendarAppointment 변환 ───────────────────────────

    /**
     * 도메인 날짜 그룹을 화면 약속 목록으로 변환한다.
     *
     * - [today]: 오늘 날짜 (TODAY 레이블 판별용)
     * - confirmedDate 파싱에 실패한 약속은 건너뛴다.
     */
    private fun CalendarDay.toUiModels(today: CalendarDate): List<MockCalendarAppointment> {
        val parsed = parseDate(date) ?: return emptyList()
        val (year, month, day) = parsed
        val isToday = year == today.year && month == today.month && day == today.day
        val dayLabel = if (isToday) "TODAY" else dayOfWeekLabel(year, month, day)
        return appointments.map { it.toUiModel(year, month, day, dayLabel, isToday) }
    }

    private fun HomeAppointment.toUiModel(
        year: Int,
        month: Int,
        day: Int,
        dayLabel: String,
        isToday: Boolean
    ): MockCalendarAppointment = MockCalendarAppointment(
        id = appointmentId.toString(),
        title = title,
        year = year,
        month = month,
        day = day,
        dayLabel = dayLabel,
        time = confirmedTime?.let { formatTime(it) } ?: "",
        location = confirmedLocationName ?: detailPlaceName ?: "장소 미정",
        participants = emptyList(),
        isToday = isToday
    )

    // ── 날짜·시간 포맷 ────────────────────────────────────────────────────────

    /**
     * "yyyy-MM-dd" 문자열을 (year, month, day) Triple로 파싱한다.
     * 파싱 실패 시 null을 반환한다.
     */
    private fun parseDate(yyyyMmDd: String): Triple<Int, Int, Int>? = try {
        val cal = Calendar.getInstance().also { cal ->
            SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).parse(yyyyMmDd)?.let { cal.time = it }
                ?: return null
        }
        Triple(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    } catch (e: Exception) {
        null
    }

    /** 날짜를 요일 레이블로 변환한다. 0=일, 1=월 … 6=토. */
    private fun dayOfWeekLabel(year: Int, month: Int, day: Int): String {
        val dow = Calendar.getInstance().apply {
            set(year, month - 1, day)
        }.get(Calendar.DAY_OF_WEEK) - 1  // Calendar: 1=일 → index 0=일
        return DAY_OF_WEEK_LABELS[dow]
    }

    /** "HH:mm" → "오후 3:00". 파싱 실패 시 원본 반환. */
    private fun formatTime(hhMm: String): String = try {
        val date = SimpleDateFormat("HH:mm", Locale.KOREA).parse(hhMm)!!
        SimpleDateFormat("a h:mm", Locale.KOREA).format(date)
    } catch (e: Exception) {
        hhMm
    }

    /** [CalendarMonth] → API 쿼리 파라미터 형식 "yyyy-MM". */
    private fun formatMonthParam(month: CalendarMonth): String =
        String.format(Locale.KOREA, "%04d-%02d", month.year, month.month)

    // ── 동반 객체 / 상수 ─────────────────────────────────────────────────────

    companion object {
        /** 요일 레이블 배열. Calendar.DAY_OF_WEEK - 1 을 인덱스로 사용한다. */
        internal val DAY_OF_WEEK_LABELS = arrayOf("일", "월", "화", "수", "목", "금", "토")

        /** 현재 날짜를 [CalendarDate]로 반환한다. */
        internal fun currentDate(): CalendarDate = Calendar.getInstance().let { cal ->
            CalendarDate(
                year = cal.get(Calendar.YEAR),
                month = cal.get(Calendar.MONTH) + 1,
                day = cal.get(Calendar.DAY_OF_MONTH)
            )
        }
    }
}
