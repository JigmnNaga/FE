package com.jigmnnaga.presentation.appointment.manage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jigmnnaga.domain.model.AppointmentDetail
import com.jigmnnaga.domain.model.AppointmentManageRejectReason
import com.jigmnnaga.domain.model.AppointmentManageResult
import com.jigmnnaga.domain.model.PlaceSearchResult
import com.jigmnnaga.domain.model.PlaceSearchType
import com.jigmnnaga.domain.repository.AppointmentManageRepository
import com.jigmnnaga.domain.repository.PlaceSearchRepository
import com.jigmnnaga.presentation.appointment.AppointmentRefreshSignal
import com.jigmnnaga.presentation.appointment.create.AppointmentCreateStep
import com.jigmnnaga.presentation.appointment.create.AppointmentCreateUiState
import com.jigmnnaga.presentation.appointment.create.AppointmentLocation
import com.jigmnnaga.presentation.appointment.create.MAX_SELECTED_LOCATIONS
import com.jigmnnaga.presentation.appointment.create.candidateDateRequest
import com.jigmnnaga.presentation.appointment.create.responseDeadlineRequest
import com.jigmnnaga.presentation.appointment.create.toLocation
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 약속 관리 화면이 한 번 보여 주는 안내. */
enum class AppointmentManageNotice {
    /** 로그인이 필요하다. 화면이 로그인으로 보낸 뒤 [AppointmentManageViewModel.resumeAfterLogin]을 부른다. */
    LOGIN_REQUIRED,
    NOT_HOST,
    APPOINTMENT_CANCELED,
    INVALID_DATES,
    INVALID_DEADLINE,
    DATES_BEFORE_DEADLINE,
    INVALID_LOCATIONS,
    INVALID_INPUT,
    FAILED,
    FAILED_RETRY,
    LOCATION_SEARCH_FAILED,
    SAVED,
    CANCELED_DONE
}

/**
 * 방장이 약속 이름·날짜·장소·응답 기한을 바꾸고 약속을 취소·삭제하는 화면의 상태를 관리한다.
 * 서버가 받아 준 뒤에만 저장된 값을 바꾸고, 실패하면 편집 중인 값을 그대로 둬서 다시 시도할 수 있게 한다.
 *
 * @param onChanged 서버가 변경을 받아 줬을 때 부른다. 열려 있는 약속방이 약속 상세를 다시 불러오는 데 쓴다.
 */
class AppointmentManageViewModel(
    detail: AppointmentDetail,
    private val repository: AppointmentManageRepository,
    private val placeSearchRepository: PlaceSearchRepository? = null,
    private val searchDebounceMillis: Long = DEFAULT_SEARCH_DEBOUNCE_MILLIS,
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val onChanged: () -> Unit = AppointmentRefreshSignal::notifyChanged
) : ViewModel() {
    private val appointmentId = detail.appointmentId

    /** 서버가 아는 후보 장소 ID. 장소를 바꿀 때 "유지할 장소"를 가려내는 데 쓴다. */
    private var knownLocationIds: Map<AppointmentLocation, Long> = detail.existingLocationIds()

    private val _uiState = MutableStateFlow(
        detail.toManageForm().let { form ->
            AppointmentManageUiState(
                saved = form,
                form = form,
                savedDeadline = detail.responseDeadline,
                isCanceled = detail.canceled
            )
        }
    )
    val uiState: StateFlow<AppointmentManageUiState> = _uiState.asStateFlow()

    private val _notice = MutableStateFlow<AppointmentManageNotice?>(null)
    val notice: StateFlow<AppointmentManageNotice?> = _notice.asStateFlow()

    private var searchJob: Job? = null
    private var searchResults: List<AppointmentLocation> = emptyList()
    private var loginResume: (() -> Unit)? = null

    /** 관리 목록에서 항목을 눌렀을 때. 그 항목의 편집값을 저장된 값으로 새로 채운다. */
    fun openStep(step: AppointmentManageStep) {
        val state = _uiState.value
        if (state.isWorking || step == AppointmentManageStep.MENU) return
        searchJob?.cancel()
        searchResults = emptyList()
        _notice.value = null
        _uiState.value = state.copy(step = step, form = state.saved.copy(step = step.toCreateStep()))
    }

    /**
     * 뒤로 가기. 변경 화면이면 편집 중인 값을 버리고 관리 목록으로 돌아가며 true를 돌려준다.
     * 관리 목록이면 false이고, 화면을 나가는 일은 호출한 쪽이 한다.
     */
    fun back(): Boolean {
        val state = _uiState.value
        if (state.step == AppointmentManageStep.MENU) return false
        if (state.isWorking) return true
        searchJob?.cancel()
        searchResults = emptyList()
        _uiState.value = state.copy(step = AppointmentManageStep.MENU, form = state.saved)
        return true
    }

    /** 변경 화면의 입력이 바뀔 때마다 화면이 부른다. 장소 검색 결과는 이 ViewModel만 바꾼다. */
    fun onFormChange(newForm: AppointmentCreateUiState) {
        val state = _uiState.value
        if (state.step == AppointmentManageStep.MENU) return
        val current = state.form
        val normalized = newForm.copy(
            appointmentName = newForm.appointmentName.take(MAX_APPOINTMENT_TITLE_LENGTH),
            selectedLocations = newForm.selectedLocations.distinct().take(MAX_SELECTED_LOCATIONS),
            selectedDates = newForm.selectedDates.distinct().sorted()
        )
        _uiState.value = state.copy(form = normalized.withCandidates())
        if (placeSearchRepository != null && current.locationQuery != normalized.locationQuery) {
            searchLocations(normalized.locationQuery.trim())
        }
    }

    /** 저장 버튼. 지금 열린 변경 화면의 값을 서버에 보낸다. */
    fun save() {
        val state = _uiState.value
        if (!state.canSave()) return
        val form = state.form
        when (state.step) {
            AppointmentManageStep.MENU -> Unit
            AppointmentManageStep.TITLE -> {
                val title = form.appointmentName.trim()
                execute(retry = ::save, call = { repository.updateTitle(appointmentId, title) }) {
                    onSaved(saved = it.saved.copy(appointmentName = title))
                }
            }
            AppointmentManageStep.SCHEDULE -> {
                val request = form.scheduleType?.let {
                    candidateDateRequest(it, form.selectedStartDate, form.selectedEndDate, form.selectedDates)
                }
                if (request == null) {
                    _notice.value = AppointmentManageNotice.INVALID_DATES
                    return
                }
                execute(
                    retry = ::save,
                    call = { repository.updateCandidateDates(appointmentId, request.first, request.second) }
                ) {
                    onSaved(
                        saved = it.saved.copy(
                            scheduleType = form.scheduleType,
                            selectedStartDate = form.selectedStartDate,
                            selectedEndDate = form.selectedEndDate,
                            selectedDates = form.selectedDates,
                            displayedMonth = form.displayedMonth
                        )
                    )
                }
            }
            AppointmentManageStep.LOCATIONS -> {
                val selected = form.selectedLocations.distinct()
                val change = toCandidateLocationChange(selected, knownLocationIds)
                if (change == null) {
                    _notice.value = AppointmentManageNotice.INVALID_LOCATIONS
                    return
                }
                execute(retry = ::save, call = { repository.updateCandidateLocations(appointmentId, change) }) {
                    // 빠진 장소의 ID는 더 이상 쓸 수 없다. 다시 고르면 새 장소로 보낸다.
                    knownLocationIds = knownLocationIds.filterKeys { key -> key in selected }
                    onSaved(
                        saved = it.saved.copy(
                            selectedLocations = selected,
                            locationCandidates = selected
                        )
                    )
                }
            }
            AppointmentManageStep.DEADLINE -> {
                val deadline = responseDeadlineRequest(form.deadlineChoice, form.customDeadline, nowMillis())
                if (deadline == null) {
                    _notice.value = AppointmentManageNotice.INVALID_DEADLINE
                    return
                }
                execute(retry = ::save, call = { repository.updateResponseDeadline(appointmentId, deadline) }) {
                    onSaved(saved = it.saved, deadline = deadline)
                }
            }
        }
    }

    /** 취소·삭제 버튼. 바로 실행하지 않고 확인 팝업을 띄운다. */
    fun requestConfirm(confirm: AppointmentManageConfirm) {
        val state = _uiState.value
        if (state.isWorking || state.step != AppointmentManageStep.MENU) return
        if (confirm == AppointmentManageConfirm.CANCEL_APPOINTMENT && state.isCanceled) return
        _uiState.value = state.copy(confirm = confirm)
    }

    fun dismissConfirm() {
        _uiState.value = _uiState.value.copy(confirm = null)
    }

    /** 확인 팝업에서 확인을 눌렀을 때. */
    fun confirmAction() {
        val state = _uiState.value
        val confirm = state.confirm ?: return
        if (state.isWorking) return
        _uiState.value = state.copy(confirm = null)
        when (confirm) {
            AppointmentManageConfirm.CANCEL_APPOINTMENT -> execute(
                retry = { requestConfirm(confirm); confirmAction() },
                call = { repository.cancel(appointmentId) }
            ) {
                onChanged()
                _notice.value = AppointmentManageNotice.CANCELED_DONE
                _uiState.value = it.copy(isCanceled = true, isWorking = false)
            }
            AppointmentManageConfirm.DELETE_APPOINTMENT -> execute(
                retry = { requestConfirm(confirm); confirmAction() },
                call = { repository.delete(appointmentId) }
            ) {
                onChanged()
                _uiState.value = it.copy(isWorking = false, finished = AppointmentManageFinish.DELETED)
            }
        }
    }

    /** 로그인에 성공해 돌아오면 화면이 부른다. 로그인 전에 하던 일을 다시 한다. */
    fun resumeAfterLogin() {
        val resume = loginResume ?: return
        loginResume = null
        resume()
    }

    fun clearNotice() {
        _notice.value = null
    }

    private fun onSaved(saved: AppointmentCreateUiState, deadline: String? = null) {
        onChanged()
        val state = _uiState.value
        _notice.value = AppointmentManageNotice.SAVED
        _uiState.value = state.copy(
            step = AppointmentManageStep.MENU,
            saved = saved,
            form = saved,
            savedDeadline = deadline ?: state.savedDeadline,
            isWorking = false
        )
    }

    /** 서버 호출 하나를 실행하고 결과를 안내로 바꾼다. 성공하면 [onSuccess]가 상태를 정리한다. */
    private fun execute(
        retry: () -> Unit,
        call: suspend () -> AppointmentManageResult<Unit>,
        onSuccess: (AppointmentManageUiState) -> Unit
    ) {
        _uiState.value = _uiState.value.copy(isWorking = true)
        viewModelScope.launch {
            when (val result = call()) {
                is AppointmentManageResult.Success -> onSuccess(_uiState.value)
                AppointmentManageResult.LoginRequired -> {
                    finishWorking()
                    loginResume = retry
                    _notice.value = AppointmentManageNotice.LOGIN_REQUIRED
                }
                AppointmentManageResult.NotHost -> {
                    finishWorking()
                    _notice.value = AppointmentManageNotice.NOT_HOST
                }
                is AppointmentManageResult.Rejected -> {
                    finishWorking()
                    _notice.value = result.reason.toNotice()
                }
                is AppointmentManageResult.Failure -> {
                    finishWorking()
                    _notice.value = if (result.retryable) AppointmentManageNotice.FAILED_RETRY
                    else AppointmentManageNotice.FAILED
                }
            }
        }
    }

    private fun finishWorking() {
        _uiState.value = _uiState.value.copy(isWorking = false)
    }

    private fun searchLocations(keyword: String) {
        searchJob?.cancel()
        val repository = placeSearchRepository ?: return
        if (keyword.isEmpty()) {
            searchResults = emptyList()
            refreshCandidates()
            return
        }
        searchJob = viewModelScope.launch {
            delay(searchDebounceMillis)
            when (val result = repository.search(keyword, PlaceSearchType.STATION)) {
                is PlaceSearchResult.Success -> {
                    searchResults = result.value.places.mapNotNull { it.toLocation() }
                    refreshCandidates()
                }
                PlaceSearchResult.LoginRequired -> {
                    loginResume = { searchLocations(_uiState.value.form.locationQuery.trim()) }
                    _notice.value = AppointmentManageNotice.LOGIN_REQUIRED
                }
                is PlaceSearchResult.Failure -> {
                    searchResults = emptyList()
                    refreshCandidates()
                    _notice.value = AppointmentManageNotice.LOCATION_SEARCH_FAILED
                }
            }
        }
    }

    private fun refreshCandidates() {
        val state = _uiState.value
        _uiState.value = state.copy(form = state.form.withCandidates())
    }

    /**
     * 후보 목록은 고른 장소를 맨 위에 두고 그 아래에 검색 결과를 붙인다.
     * 그래야 이미 고른 장소를 검색어와 상관없이 목록에서 빼낼 수 있다.
     */
    private fun AppointmentCreateUiState.withCandidates(): AppointmentCreateUiState {
        val candidates = if (locationQuery.isBlank()) selectedLocations
        else (selectedLocations + searchResults).distinct()
        return copy(locationCandidates = candidates, locationCandidatesFromServer = true)
    }

    private fun AppointmentManageRejectReason.toNotice() = when (this) {
        AppointmentManageRejectReason.APPOINTMENT_CANCELED -> AppointmentManageNotice.APPOINTMENT_CANCELED
        AppointmentManageRejectReason.INVALID_DATES -> AppointmentManageNotice.INVALID_DATES
        AppointmentManageRejectReason.INVALID_DEADLINE -> AppointmentManageNotice.INVALID_DEADLINE
        AppointmentManageRejectReason.DATES_BEFORE_DEADLINE -> AppointmentManageNotice.DATES_BEFORE_DEADLINE
        AppointmentManageRejectReason.INVALID_LOCATIONS -> AppointmentManageNotice.INVALID_LOCATIONS
        AppointmentManageRejectReason.INVALID_MEMBER,
        AppointmentManageRejectReason.INVALID_INPUT -> AppointmentManageNotice.INVALID_INPUT
    }

    private fun AppointmentManageStep.toCreateStep() = when (this) {
        AppointmentManageStep.MENU,
        AppointmentManageStep.TITLE -> AppointmentCreateStep.APPOINTMENT_NAME
        AppointmentManageStep.SCHEDULE -> AppointmentCreateStep.SCHEDULE
        AppointmentManageStep.LOCATIONS -> AppointmentCreateStep.LOCATIONS
        AppointmentManageStep.DEADLINE -> AppointmentCreateStep.RESPONSE_DEADLINE
    }

    private companion object {
        const val DEFAULT_SEARCH_DEBOUNCE_MILLIS = 300L
    }
}
