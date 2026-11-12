package com.jigmnnaga.presentation.appointment.schedule

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 한 날짜에 대해 참여자가 고른 시간대. */
data class AppointmentScheduleEntry(
    val date: AppointmentDateValue,
    val timeRange: AppointmentTimeRange
)

/** 서버로 보낼 응답의 모양. 서버 계약이 정해지기 전까지는 화면 안에서만 만든다. */
data class AppointmentScheduleResponseDraft(
    val entries: List<AppointmentScheduleEntry>
)

/**
 * 희망 날짜·시간 선택 화면의 상태와 선택 규칙을 관리한다.
 * 응답 저장 API가 아직 없으므로 응답 데이터만 만들고, 서버 저장이 성공한 것처럼 처리하지 않는다.
 */
class AppointmentDateTimeViewModel(
    appointmentTitle: String,
    initialMonth: AppointmentCalendarMonth,
    memberResponses: List<MemberResponseUiState> = emptyList()
) : ViewModel() {

    private data class DateSelection(
        val range: AppointmentTimeRange,
        val quickIds: Set<String>
    )

    /** 시간대를 고른 날짜만 들어 있다. */
    private var selections: Map<AppointmentDateValue, DateSelection> = emptyMap()

    private val _uiState = MutableStateFlow(
        AppointmentDateTimeUiState(
            appointmentTitle = appointmentTitle,
            displayedMonth = initialMonth,
            quickTimeOptions = QUICK_TIME_OPTIONS.map {
                QuickTimeOptionUiState(it.id, it.label, it.rangeLabel)
            },
            memberResponses = memberResponses
        )
    )
    val uiState: StateFlow<AppointmentDateTimeUiState> = _uiState.asStateFlow()

    private val _pendingResponse = MutableStateFlow<AppointmentScheduleResponseDraft?>(null)
    val pendingResponse: StateFlow<AppointmentScheduleResponseDraft?> = _pendingResponse.asStateFlow()

    fun onStatusToggle() {
        _uiState.value = _uiState.value.copy(isStatusExpanded = !_uiState.value.isStatusExpanded)
    }

    fun onMonthChange(month: AppointmentCalendarMonth) {
        edit { copy(displayedMonth = month) }
    }

    fun onDateToggle(date: AppointmentDateValue) {
        if (_uiState.value.step != AppointmentDateTimeStep.DATES) return
        val removing = date in _uiState.value.selectedDates
        if (removing) selections = selections - date
        edit {
            val dates = if (removing) selectedDates - date else selectedDates + date
            copy(selectedDates = dates, activeDate = activeDate?.takeIf { it in dates })
        }
    }

    fun onActiveDateSelect(date: AppointmentDateValue) {
        val state = _uiState.value
        if (state.step != AppointmentDateTimeStep.TIMES || date !in state.selectedDates) return
        edit { copy(activeDate = date, timePicker = null) }
    }

    fun onStartTimeClick() = openTimePicker(TimePickerTarget.START)

    fun onEndTimeClick() = openTimePicker(TimePickerTarget.END)

    private fun openTimePicker(target: TimePickerTarget) {
        val state = _uiState.value
        val date = state.activeDate ?: return
        if (state.step != AppointmentDateTimeStep.TIMES) return
        val range = selections[date]?.range
        val initial = when (target) {
            TimePickerTarget.START -> range?.start ?: DEFAULT_PICKER_START
            TimePickerTarget.END -> range?.end ?: DEFAULT_PICKER_END
        }
        edit { copy(timePicker = TimePickerUiState(target, initial)) }
    }

    fun onTimePickerValueChange(time: AppointmentTimeValue) {
        val picker = _uiState.value.timePicker ?: return
        _uiState.value = _uiState.value.copy(timePicker = picker.copy(selectedTime = time))
    }

    fun onTimePickerDismiss() {
        _uiState.value = _uiState.value.copy(timePicker = null)
    }

    fun onTimePickerConfirm(time: AppointmentTimeValue) {
        val state = _uiState.value
        val picker = state.timePicker ?: return
        val date = state.activeDate ?: return
        val current = selections[date]?.range
        val newRange = when (picker.target) {
            TimePickerTarget.START ->
                if (current == null) rangeFromStart(time) else AppointmentTimeRange(time, current.end)
            TimePickerTarget.END ->
                if (current == null) rangeFromEnd(time) else AppointmentTimeRange(current.start, time)
        }
        if (newRange == null || newRange.start.totalMinutes() >= newRange.end.totalMinutes()) {
            edit { copy(timePicker = null) }
            _uiState.value = _uiState.value.copy(errorMessage = ERROR_TIME_ORDER)
            return
        }
        // 직접 고른 시간은 빠른 선택과 무관하므로 빠른 선택 표시를 해제한다.
        selections = selections + (date to DateSelection(newRange, emptySet()))
        edit { copy(timePicker = null) }
    }

    fun onQuickTimeClick(id: String) {
        val state = _uiState.value
        if (state.step != AppointmentDateTimeStep.TIMES) return
        val date = state.activeDate ?: return
        if (QUICK_TIME_OPTIONS.none { it.id == id }) return

        val currentIds = selections[date]?.quickIds ?: emptySet()
        var ids = if (id in currentIds) currentIds - id else currentIds + id
        // 이어지지 않는 시간대를 함께 고르면 사이 시간까지 포함되므로, 방금 누른 것만 남긴다.
        if (!ids.isContiguousQuickSelection()) ids = setOf(id)

        val range = ids.toQuickRange()
        selections = if (range == null) selections - date
        else selections + (date to DateSelection(range, ids))
        edit { copy(timePicker = null) }
    }

    /** 시간 단계에서는 이전 날짜 단계로 돌아가고, 선택값은 그대로 유지한다. 돌아갈 곳이 없으면 false. */
    fun movePrevious(): Boolean {
        if (_uiState.value.step != AppointmentDateTimeStep.TIMES) return false
        edit { copy(step = AppointmentDateTimeStep.DATES, timePicker = null) }
        return true
    }

    fun onNext() {
        val state = _uiState.value
        when (state.step) {
            AppointmentDateTimeStep.DATES -> {
                if (state.selectedDates.isEmpty()) return
                edit {
                    copy(
                        step = AppointmentDateTimeStep.TIMES,
                        activeDate = activeDate?.takeIf { it in selectedDates }
                            ?: selectedDates.minWithOrNull(DATE_ORDER)
                    )
                }
            }
            AppointmentDateTimeStep.TIMES -> {
                val draft = buildResponseDraft() ?: return
                _pendingResponse.value = draft
                // 저장 API가 없으므로 완료 단계로 넘어가지 않고 안내만 표시한다.
                _uiState.value = state.copy(errorMessage = ERROR_API_NOT_CONNECTED)
            }
            AppointmentDateTimeStep.COMPLETE -> Unit
        }
    }

    /** 선택한 모든 날짜에 시간대가 있을 때만 응답 데이터를 만든다. */
    fun buildResponseDraft(): AppointmentScheduleResponseDraft? {
        val dates = _uiState.value.selectedDates
        if (dates.isEmpty()) return null
        val entries = dates.sortedWith(DATE_ORDER).map { date ->
            val range = selections[date]?.range ?: return null
            AppointmentScheduleEntry(date, range)
        }
        return AppointmentScheduleResponseDraft(entries)
    }

    /** 실제 저장이 성공했을 때 API 연동 쪽에서 호출할 지점. 내 응답을 완료로 바꾸고 완료 화면으로 이동한다. */
    fun onResponseSubmitted() {
        val state = _uiState.value
        if (state.step != AppointmentDateTimeStep.TIMES) return
        val members = state.memberResponses.map {
            if (it.isMe) it.copy(status = MemberResponseStatus.COMPLETED) else it
        }
        _pendingResponse.value = null
        _uiState.value = state.copy(
            step = AppointmentDateTimeStep.COMPLETE,
            memberResponses = members,
            completionMemberLabels = members.completionLabels(),
            isStatusExpanded = false,
            timePicker = null,
            errorMessage = null,
            canAdvance = true
        )
    }

    /** 서버에서 참여자 응답 현황을 받았을 때 반영하는 지점. */
    fun updateMemberResponses(responses: List<MemberResponseUiState>) {
        val state = _uiState.value
        _uiState.value = state.copy(
            memberResponses = responses,
            completionMemberLabels = if (state.step == AppointmentDateTimeStep.COMPLETE) {
                responses.completionLabels()
            } else {
                state.completionMemberLabels
            }
        )
    }

    /** 사용자 입력이 바뀌면 에러와 만들어 둔 응답 데이터를 지우고, 선택값에서 파생되는 값을 다시 계산한다. */
    private fun edit(block: AppointmentDateTimeUiState.() -> AppointmentDateTimeUiState) {
        _pendingResponse.value = null
        _uiState.value = _uiState.value.copy(errorMessage = null).block().synced()
    }

    private fun AppointmentDateTimeUiState.synced(): AppointmentDateTimeUiState {
        val selection = activeDate?.let { selections[it] }
        return copy(
            selectedTimeRange = selection?.range,
            selectedQuickTimeIds = selection?.quickIds ?: emptySet(),
            canAdvance = when (step) {
                AppointmentDateTimeStep.DATES -> selectedDates.isNotEmpty()
                AppointmentDateTimeStep.TIMES ->
                    selectedDates.isNotEmpty() && selectedDates.all { it in selections }
                AppointmentDateTimeStep.COMPLETE -> true
            }
        )
    }

    private fun List<MemberResponseUiState>.completionLabels(): List<String> =
        filter { it.status == MemberResponseStatus.COMPLETED }
            .map { if (it.isMe) "나" else it.name }

    companion object {
        internal const val ERROR_TIME_ORDER = "종료 시간은 시작 시간보다 늦어야 해요."
        internal const val ERROR_API_NOT_CONNECTED = "아직 서버와 연결되지 않아 응답을 저장할 수 없어요."
    }
}

private data class QuickTimeOption(
    val id: String,
    val label: String,
    val startHour: Int,
    val endHour: Int
) {
    val rangeLabel: String get() = "$startHour:00–$endHour:00"
    val startMinutes: Int get() = startHour * 60
    val endMinutes: Int get() = endHour * 60
}

private val QUICK_TIME_OPTIONS = listOf(
    QuickTimeOption("morning", "오전", 9, 12),
    QuickTimeOption("afternoon", "오후", 12, 17),
    QuickTimeOption("evening", "저녁", 17, 21)
)

private val DATE_ORDER = compareBy<AppointmentDateValue>({ it.year }, { it.month }, { it.day })

private val DEFAULT_PICKER_START = AppointmentTimeValue(9, 0)
private val DEFAULT_PICKER_END = AppointmentTimeValue(18, 0)

private const val LAST_MINUTE_OF_DAY = 23 * 60 + 59
private const val DEFAULT_DURATION_MINUTES = 60

private fun AppointmentTimeValue.totalMinutes(): Int = hour * 60 + minute

private fun minutesToTime(minutes: Int) = AppointmentTimeValue(minutes / 60, minutes % 60)

/** 시작만 고른 경우 기본 1시간 범위를 만든다. 하루의 마지막 시각이면 종료를 정할 수 없어 null. */
private fun rangeFromStart(start: AppointmentTimeValue): AppointmentTimeRange? {
    val startMinutes = start.totalMinutes()
    if (startMinutes >= LAST_MINUTE_OF_DAY) return null
    val end = minOf(startMinutes + DEFAULT_DURATION_MINUTES, LAST_MINUTE_OF_DAY)
    return AppointmentTimeRange(start, minutesToTime(end))
}

/** 종료만 고른 경우 기본 1시간 범위를 만든다. 00:00이면 시작을 정할 수 없어 null. */
private fun rangeFromEnd(end: AppointmentTimeValue): AppointmentTimeRange? {
    val endMinutes = end.totalMinutes()
    if (endMinutes <= 0) return null
    val start = maxOf(endMinutes - DEFAULT_DURATION_MINUTES, 0)
    return AppointmentTimeRange(minutesToTime(start), end)
}

private fun Set<String>.chosenQuickOptions(): List<QuickTimeOption> =
    QUICK_TIME_OPTIONS.filter { it.id in this }.sortedBy { it.startMinutes }

private fun Set<String>.isContiguousQuickSelection(): Boolean =
    chosenQuickOptions().zipWithNext().all { (a, b) -> a.endMinutes == b.startMinutes }

private fun Set<String>.toQuickRange(): AppointmentTimeRange? {
    val chosen = chosenQuickOptions()
    if (chosen.isEmpty()) return null
    return AppointmentTimeRange(
        minutesToTime(chosen.first().startMinutes),
        minutesToTime(chosen.last().endMinutes)
    )
}