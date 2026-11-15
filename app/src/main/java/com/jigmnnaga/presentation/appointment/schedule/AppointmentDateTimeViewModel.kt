package com.jigmnnaga.presentation.appointment.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jigmnnaga.domain.model.MySchedule
import com.jigmnnaga.domain.model.ScheduleAvailability
import com.jigmnnaga.domain.model.ScheduleRejectReason
import com.jigmnnaga.domain.model.ScheduleResult
import com.jigmnnaga.domain.model.ScheduleStatus
import com.jigmnnaga.domain.model.ScheduleSubmission
import com.jigmnnaga.domain.repository.ScheduleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 일정 선택 화면에서 사용자에게 한 번 알려야 하는 안내. */
enum class AppointmentDateTimeNotice {
    /** 로그인이 필요함. 로그인 화면으로 보낸다. */
    LOGIN_REQUIRED
}

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
 *
 * [scheduleRepository]와 [appointmentId]가 있으면 화면을 열 때 내가 제출한 일정과 참여자 응답 현황을 불러오고,
 * 다음 버튼으로 날짜별 시간대를 서버에 제출한다. 서버가 저장을 받아 준 뒤에만 완료 화면으로 넘어간다.
 * 둘 중 하나라도 없으면 서버에 보내지 않고 안내만 보여 준다.
 * 희망 장소는 이 화면에서 보내지 않는다. 서버는 보낸 항목만 저장하므로 장소 선택은 그대로 남는다.
 *
 * [candidateDates]가 있으면 그 날짜만 고를 수 있다.
 */
class AppointmentDateTimeViewModel(
    appointmentTitle: String,
    initialMonth: AppointmentCalendarMonth,
    memberResponses: List<MemberResponseUiState> = emptyList(),
    private val appointmentId: Long? = null,
    private val scheduleRepository: ScheduleRepository? = null,
    candidateDates: Set<AppointmentDateValue>? = null
) : ViewModel() {

    private val availableDates: Set<AppointmentDateValue>? = candidateDates?.takeIf { it.isNotEmpty() }

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
            memberResponses = memberResponses,
            availableDates = availableDates
        )
    )
    val uiState: StateFlow<AppointmentDateTimeUiState> = _uiState.asStateFlow()

    private val _notice = MutableStateFlow<AppointmentDateTimeNotice?>(null)
    val notice: StateFlow<AppointmentDateTimeNotice?> = _notice.asStateFlow()

    /** 제출 중 로그인이 필요해 로그인 화면으로 보냈는지. 로그인 뒤 다시 제출한다. */
    private var submitAfterLogin = false

    private val _pendingResponse = MutableStateFlow<AppointmentScheduleResponseDraft?>(null)
    val pendingResponse: StateFlow<AppointmentScheduleResponseDraft?> = _pendingResponse.asStateFlow()

    // 모든 상태 필드를 만든 뒤에 불러온다. 코루틴이 바로 실행될 수 있어 위쪽에 두면 아직 없는 필드를 건드린다.
    init {
        load()
    }

    fun onStatusToggle() {
        _uiState.value = _uiState.value.copy(isStatusExpanded = !_uiState.value.isStatusExpanded)
    }

    fun onMonthChange(month: AppointmentCalendarMonth) {
        edit { copy(displayedMonth = month) }
    }

    fun onDateToggle(date: AppointmentDateValue) {
        if (_uiState.value.step != AppointmentDateTimeStep.DATES) return
        val removing = date in _uiState.value.selectedDates
        if (!removing && availableDates != null && date !in availableDates) return
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
                if (state.isSubmitting) return
                val draft = buildResponseDraft() ?: return
                _pendingResponse.value = draft
                val repository = scheduleRepository
                val id = appointmentId
                if (repository == null || id == null) {
                    // 서버 연결 정보가 없으면 완료 단계로 넘어가지 않고 안내만 표시한다.
                    _uiState.value = state.copy(errorMessage = ERROR_API_NOT_CONNECTED)
                } else {
                    submit(repository, id, draft)
                }
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

    private fun submit(repository: ScheduleRepository, id: Long, draft: AppointmentScheduleResponseDraft) {
        _uiState.value = _uiState.value.copy(isSubmitting = true, errorMessage = null)
        viewModelScope.launch {
            val submission = ScheduleSubmission(
                availabilities = draft.entries.map {
                    ScheduleAvailability(
                        date = it.date.toIsoString(),
                        startTime = it.timeRange.start.label(),
                        endTime = it.timeRange.end.label()
                    )
                }
            )
            when (val result = repository.submitSchedule(id, submission)) {
                is ScheduleResult.Success -> onResponseSubmitted()
                ScheduleResult.LoginRequired -> {
                    submitAfterLogin = true
                    _uiState.value = _uiState.value.copy(isSubmitting = false)
                    _notice.value = AppointmentDateTimeNotice.LOGIN_REQUIRED
                }
                ScheduleResult.NotParticipant -> submitFailed(ERROR_NOT_PARTICIPANT)
                is ScheduleResult.Rejected -> submitFailed(result.reason.toMessage())
                is ScheduleResult.Failure ->
                    submitFailed(if (result.retryable) ERROR_NETWORK else ERROR_SUBMIT_FAILED)
            }
        }
    }

    private fun submitFailed(message: String) {
        _uiState.value = _uiState.value.copy(isSubmitting = false, errorMessage = message)
    }

    private fun ScheduleRejectReason.toMessage(): String = when (this) {
        ScheduleRejectReason.APPOINTMENT_CANCELED -> ERROR_APPOINTMENT_CANCELED
        ScheduleRejectReason.DEADLINE_PASSED -> ERROR_DEADLINE_PASSED
        ScheduleRejectReason.INVALID_DATE -> ERROR_INVALID_DATE
        ScheduleRejectReason.DUPLICATE_DATE -> ERROR_DUPLICATE_DATE
        ScheduleRejectReason.INVALID_TIME -> ERROR_INVALID_TIME
        ScheduleRejectReason.INVALID_LOCATION,
        ScheduleRejectReason.EMPTY_UPDATE -> ERROR_SUBMIT_FAILED
    }

    /** 내가 제출한 일정과 참여자 응답 현황을 불러온다. 실패해도 화면은 그대로 쓸 수 있다. */
    private fun load() {
        val repository = scheduleRepository ?: return
        val id = appointmentId ?: return
        viewModelScope.launch {
            when (val mine = repository.getMySchedule(id)) {
                is ScheduleResult.Success -> applyMySchedule(mine.value)
                ScheduleResult.LoginRequired -> _notice.value = AppointmentDateTimeNotice.LOGIN_REQUIRED
                else -> Unit
            }
            when (val status = repository.getScheduleStatus(id)) {
                is ScheduleResult.Success -> applyStatus(status.value)
                else -> Unit
            }
        }
    }

    /** 로그인에 성공해 돌아왔을 때 호출한다. 제출하던 중이었으면 다시 제출하고, 아니면 조회를 다시 한다. */
    fun resumeAfterLogin() {
        if (submitAfterLogin) {
            submitAfterLogin = false
            onNext()
        } else {
            load()
        }
    }

    fun clearNotice() {
        _notice.value = null
    }

    /** 내가 이미 낸 일정이 있고 사용자가 아직 아무것도 고르지 않았을 때만 선택값으로 채운다. */
    private fun applyMySchedule(schedule: MySchedule) {
        if (selections.isNotEmpty() || _uiState.value.selectedDates.isNotEmpty()) return
        val restored = linkedMapOf<AppointmentDateValue, DateSelection>()
        schedule.availabilities.forEach {
            val date = AppointmentDateValue.parseIso(it.date) ?: return@forEach
            if (availableDates != null && date !in availableDates) return@forEach
            val start = AppointmentTimeValue.parse(it.startTime) ?: return@forEach
            val end = AppointmentTimeValue.parse(it.endTime) ?: return@forEach
            if (start.totalMinutes() >= end.totalMinutes()) return@forEach
            restored[date] = DateSelection(AppointmentTimeRange(start, end), emptySet())
        }
        if (restored.isEmpty()) return
        selections = restored
        edit { copy(selectedDates = restored.keys.toSet(), activeDate = null) }
    }

    /** 일정을 하나라도 낸 참여자를 완료로 보여 준다. 내 표시는 기존 값을 따른다. */
    private fun applyStatus(status: ScheduleStatus) {
        val current = _uiState.value.memberResponses
        val responses = status.members.map { member ->
            val id = member.memberId.toString()
            MemberResponseUiState(
                id = id,
                name = member.nickname,
                status = if (member.submitted || member.availabilities.isNotEmpty()) {
                    MemberResponseStatus.COMPLETED
                } else {
                    MemberResponseStatus.PENDING
                },
                isMe = current.firstOrNull { it.id == id }?.isMe ?: false
            )
        }
        if (responses.isNotEmpty()) updateMemberResponses(responses)
    }

    /** 서버 저장이 성공했을 때 호출한다. 내 응답을 완료로 바꾸고 완료 화면으로 이동한다. */
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
            canAdvance = true,
            isSubmitting = false
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
        internal const val ERROR_NOT_PARTICIPANT = "이 약속의 참여자만 응답할 수 있어요."
        internal const val ERROR_APPOINTMENT_CANCELED = "취소된 약속이라 응답할 수 없어요."
        internal const val ERROR_DEADLINE_PASSED = "응답 기한이 지나 응답할 수 없어요."
        internal const val ERROR_INVALID_DATE = "후보 날짜가 아닌 날짜가 포함돼 있어요."
        internal const val ERROR_DUPLICATE_DATE = "같은 날짜가 두 번 선택됐어요."
        internal const val ERROR_INVALID_TIME = "시간 선택이 올바르지 않아요."
        internal const val ERROR_NETWORK = "네트워크가 불안정해요. 잠시 후 다시 시도해 주세요."
        internal const val ERROR_SUBMIT_FAILED = "일정을 저장하지 못했어요. 잠시 후 다시 시도해 주세요."
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