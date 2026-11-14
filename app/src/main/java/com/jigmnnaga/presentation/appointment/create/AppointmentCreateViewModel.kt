package com.jigmnnaga.presentation.appointment.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jigmnnaga.domain.model.AppointmentCreateRejectReason
import com.jigmnnaga.domain.model.AppointmentCreateResult
import com.jigmnnaga.domain.model.CreatedAppointment
import com.jigmnnaga.domain.model.PlaceSearchResult
import com.jigmnnaga.domain.model.PlaceSearchType
import com.jigmnnaga.domain.model.PreviousMembers
import com.jigmnnaga.domain.model.SearchedPlace
import com.jigmnnaga.domain.repository.AppointmentCreateRepository
import com.jigmnnaga.domain.repository.PlaceSearchRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** User-visible one-shot feedback emitted by the appointment creation flow. */
enum class AppointmentCreateNotice {
    CREATE_API_NOT_CONNECTED,
    INVITE_LINK_UNAVAILABLE,
    SHARE_UNAVAILABLE,

    /** 서버 호출에 로그인이 필요하다. 화면이 로그인으로 보낸 뒤 [AppointmentCreateViewModel.resumeAfterLogin]을 부른다. */
    LOGIN_REQUIRED,
    INVALID_DATES,
    INVALID_DEADLINE,
    INVALID_LOCATIONS,
    INVALID_PREVIOUS_MEMBERS,
    INVALID_INPUT,
    CREATE_FAILED,
    CREATE_FAILED_RETRY,
    LOCATION_SEARCH_FAILED
}

/**
 * Owns the APT-001 form state and step transitions.
 *
 * [createRepository]가 있으면 생성 버튼이 서버에 약속 생성을 요청하고, 응답으로 받은 초대 링크를
 * 마지막 단계에 보여 준다. 없으면 입력만 검증해 두고 서버 호출 없이 "미연결" 안내를 보낸다.
 * [placeSearchRepository]가 있으면 장소 단계에서 입력한 검색어로 서버에서 지하철역을 검색한다.
 * [previousMembers]는 다시 약속 잡기처럼 이전 약속의 참여자를 바로 초대할 때 넘긴다.
 *
 * 서버 응답 없이 약속 생성 성공이나 초대 링크 발급을 임의로 표시하지 않는다.
 */
class AppointmentCreateViewModel(
    private val createRepository: AppointmentCreateRepository? = null,
    private val placeSearchRepository: PlaceSearchRepository? = null,
    private val previousMembers: PreviousMembers? = null,
    private val searchDebounceMillis: Long = DEFAULT_SEARCH_DEBOUNCE_MILLIS,
    private val nowMillis: () -> Long = System::currentTimeMillis
) : ViewModel() {
    private val _uiState = MutableStateFlow(AppointmentCreateUiState())
    val uiState: StateFlow<AppointmentCreateUiState> = _uiState.asStateFlow()

    private val _pendingDraft = MutableStateFlow<AppointmentCreateDraft?>(null)
    val pendingDraft: StateFlow<AppointmentCreateDraft?> = _pendingDraft.asStateFlow()

    private val _notice = MutableStateFlow<AppointmentCreateNotice?>(null)
    val notice: StateFlow<AppointmentCreateNotice?> = _notice.asStateFlow()

    private var searchJob: Job? = null
    private var loginResume: (() -> Unit)? = null

    /** Receives immutable form updates from the existing UI composable. */
    fun updateUiState(state: AppointmentCreateUiState) {
        val current = _uiState.value
        val serverSearch = placeSearchRepository != null
        val normalized = state.copy(
            selectedLocations = state.selectedLocations.distinct().take(MAX_SELECTED_LOCATIONS),
            selectedDates = state.selectedDates.distinct().sorted(),
            // 검색 결과는 이 ViewModel만 바꾼다. 화면이 보낸 오래된 값으로 되돌리지 않는다.
            locationCandidates = if (serverSearch) current.locationCandidates else state.locationCandidates,
            locationCandidatesFromServer = if (serverSearch) current.locationCandidatesFromServer
            else state.locationCandidatesFromServer
        )
        val formChanged = current.appointmentName != normalized.appointmentName ||
            current.scheduleType != normalized.scheduleType ||
            current.selectedStartDate != normalized.selectedStartDate ||
            current.selectedEndDate != normalized.selectedEndDate ||
            current.selectedDates != normalized.selectedDates ||
            current.selectedLocations != normalized.selectedLocations ||
            current.deadlineChoice != normalized.deadlineChoice ||
            current.customDeadline != normalized.customDeadline

        _uiState.value = normalized.copy(
            submissionStatus = if (formChanged) AppointmentCreateSubmissionStatus.READY
            else current.submissionStatus
        )
        if (formChanged) {
            _pendingDraft.value = null
            _notice.value = null
        }
        if (serverSearch && current.locationQuery != normalized.locationQuery) {
            searchLocations(normalized.locationQuery.trim())
        }
    }

    /** Advances only when the current step is valid; all entered values remain in UiState. */
    fun moveNext(): Boolean {
        val state = _uiState.value
        if (!state.isCurrentStepValid()) return false
        val nextStep = when (state.step) {
            AppointmentCreateStep.APPOINTMENT_NAME -> AppointmentCreateStep.SCHEDULE
            AppointmentCreateStep.SCHEDULE -> AppointmentCreateStep.LOCATIONS
            AppointmentCreateStep.LOCATIONS -> AppointmentCreateStep.RESPONSE_DEADLINE
            AppointmentCreateStep.RESPONSE_DEADLINE,
            AppointmentCreateStep.INVITATION -> return false
        }
        _uiState.value = state.copy(step = nextStep)
        return true
    }

    /** Returns to the previous input step without clearing any input. */
    fun movePrevious(): Boolean {
        val state = _uiState.value
        val previousStep = when (state.step) {
            AppointmentCreateStep.APPOINTMENT_NAME -> return false
            AppointmentCreateStep.SCHEDULE -> AppointmentCreateStep.APPOINTMENT_NAME
            AppointmentCreateStep.LOCATIONS -> AppointmentCreateStep.SCHEDULE
            AppointmentCreateStep.RESPONSE_DEADLINE -> AppointmentCreateStep.LOCATIONS
            AppointmentCreateStep.INVITATION -> AppointmentCreateStep.RESPONSE_DEADLINE
        }
        _uiState.value = state.copy(step = previousStep)
        return true
    }

    /** Builds the validated draft without assigning an ID or URL. */
    fun buildDraft(): AppointmentCreateDraft? = _uiState.value.toDraftOrNull()

    /**
     * 검증된 초안으로 약속 생성을 요청한다. 이미 요청 중이거나 성공한 뒤에는 다시 보내지 않는다.
     * 저장소가 없으면 서버 호출 없이 초안만 보관하고 "미연결" 상태로 둔다.
     */
    fun requestCreate(draftFromUi: AppointmentCreateDraft) {
        val current = _uiState.value
        if (current.submissionStatus != AppointmentCreateSubmissionStatus.READY || current.isCreating) return
        val validatedDraft = current.toDraftOrNull()
        val normalizedUiDraft = draftFromUi.copy(
            name = draftFromUi.name.trim(),
            specificDates = draftFromUi.specificDates.distinct().sorted(),
            locations = draftFromUi.locations.distinct().take(MAX_SELECTED_LOCATIONS)
        )
        if (validatedDraft == null || validatedDraft != normalizedUiDraft) return
        _pendingDraft.value = validatedDraft

        val repository = createRepository
        if (repository == null) {
            // No API call is made here, so this is a terminal unavailable state rather than a spinner.
            _uiState.value = current.copy(
                isCreating = false,
                submissionStatus = AppointmentCreateSubmissionStatus.API_UNAVAILABLE
            )
            _notice.value = AppointmentCreateNotice.CREATE_API_NOT_CONNECTED
            return
        }

        val request = validatedDraft.toNewAppointment(nowMillis(), previousMembers)
        if (request == null) {
            // 위치가 없는 장소처럼 서버가 받을 수 없는 내용이다. 서버를 부르지 않는다.
            _notice.value = AppointmentCreateNotice.INVALID_LOCATIONS
            return
        }

        _uiState.value = current.copy(isCreating = true)
        viewModelScope.launch {
            when (val result = repository.create(request)) {
                is AppointmentCreateResult.Success -> onCreated(result.value)
                AppointmentCreateResult.LoginRequired -> {
                    finishCreating()
                    loginResume = ::retryCreate
                    _notice.value = AppointmentCreateNotice.LOGIN_REQUIRED
                }
                is AppointmentCreateResult.Rejected -> {
                    finishCreating()
                    _notice.value = result.reason.toNotice()
                }
                is AppointmentCreateResult.Failure -> {
                    finishCreating()
                    _notice.value = if (result.retryable) AppointmentCreateNotice.CREATE_FAILED_RETRY
                    else AppointmentCreateNotice.CREATE_FAILED
                }
            }
        }
    }

    /** 로그인에 성공해 돌아오면 화면이 부른다. 로그인 전에 하던 요청(생성 또는 검색)을 다시 한다. */
    fun resumeAfterLogin() {
        val resume = loginResume ?: return
        loginResume = null
        resume()
    }

    private fun retryCreate() {
        val draft = _pendingDraft.value ?: return
        requestCreate(draft)
    }

    private fun finishCreating() {
        _uiState.value = _uiState.value.copy(isCreating = false)
    }

    private fun onCreated(created: CreatedAppointment) {
        val link = created.inviteUrl
        if (link.isNullOrBlank()) {
            // 약속은 이미 만들어졌다. 다시 생성하면 중복되므로 성공 상태로 두고 안내만 보낸다.
            _uiState.value = _uiState.value.copy(
                step = AppointmentCreateStep.INVITATION,
                isCreating = false,
                submissionStatus = AppointmentCreateSubmissionStatus.SUCCEEDED,
                inviteLink = null,
                copyConfirmationVisible = false
            )
            _pendingDraft.value = null
            _notice.value = AppointmentCreateNotice.INVITE_LINK_UNAVAILABLE
        } else {
            onInviteLinkIssued(link)
        }
    }

    private fun searchLocations(keyword: String) {
        searchJob?.cancel()
        val repository = placeSearchRepository ?: return
        if (keyword.isEmpty()) {
            setCandidates(emptyList())
            return
        }
        searchJob = viewModelScope.launch {
            delay(searchDebounceMillis)
            when (val result = repository.search(keyword, PlaceSearchType.STATION)) {
                is PlaceSearchResult.Success -> setCandidates(result.value.places.mapNotNull { it.toLocation() })
                PlaceSearchResult.LoginRequired -> {
                    loginResume = { searchLocations(_uiState.value.locationQuery.trim()) }
                    _notice.value = AppointmentCreateNotice.LOGIN_REQUIRED
                }
                is PlaceSearchResult.Failure -> {
                    setCandidates(emptyList())
                    _notice.value = AppointmentCreateNotice.LOCATION_SEARCH_FAILED
                }
            }
        }
    }

    private fun setCandidates(candidates: List<AppointmentLocation>) {
        _uiState.value = _uiState.value.copy(
            locationCandidates = candidates,
            locationCandidatesFromServer = true
        )
    }

    private fun AppointmentCreateRejectReason.toNotice() = when (this) {
        AppointmentCreateRejectReason.INVALID_DATES -> AppointmentCreateNotice.INVALID_DATES
        AppointmentCreateRejectReason.INVALID_DEADLINE -> AppointmentCreateNotice.INVALID_DEADLINE
        AppointmentCreateRejectReason.INVALID_LOCATIONS -> AppointmentCreateNotice.INVALID_LOCATIONS
        AppointmentCreateRejectReason.INVALID_PREVIOUS_MEMBERS -> AppointmentCreateNotice.INVALID_PREVIOUS_MEMBERS
        AppointmentCreateRejectReason.INVALID_INPUT -> AppointmentCreateNotice.INVALID_INPUT
    }

    /** Integration point for a real successful backend response. Blank links never show success. */
    fun onInviteLinkIssued(inviteLink: String) {
        if (inviteLink.isBlank()) {
            _notice.value = AppointmentCreateNotice.INVITE_LINK_UNAVAILABLE
            return
        }
        _uiState.value = _uiState.value.copy(
            step = AppointmentCreateStep.INVITATION,
            isCreating = false,
            submissionStatus = AppointmentCreateSubmissionStatus.SUCCEEDED,
            inviteLink = inviteLink,
            copyConfirmationVisible = false
        )
        _pendingDraft.value = null
    }

    fun onInviteCopied() {
        if (_uiState.value.inviteLink.isNullOrBlank()) {
            _notice.value = AppointmentCreateNotice.INVITE_LINK_UNAVAILABLE
            return
        }
        _uiState.value = _uiState.value.copy(copyConfirmationVisible = true)
    }

    fun dismissCopyConfirmation() {
        _uiState.value = _uiState.value.copy(copyConfirmationVisible = false)
    }

    fun notifyShareUnavailable() {
        _notice.value = AppointmentCreateNotice.SHARE_UNAVAILABLE
    }

    fun clearNotice() {
        _notice.value = null
    }

    private companion object {
        const val DEFAULT_SEARCH_DEBOUNCE_MILLIS = 300L
    }
}

/** 위치가 없는 장소는 서버가 받지 않으므로 후보로 보여 주지 않는다. 약속 생성과 약속 관리가 함께 쓴다. */
internal fun SearchedPlace.toLocation(): AppointmentLocation? {
    val lat = latitude ?: return null
    val lng = longitude ?: return null
    val address = roadAddress ?: jibunAddress
    return AppointmentLocation(
        name = name,
        address = listOfNotNull(lineNames?.takeIf { it.isNotBlank() }, address?.takeIf { it.isNotBlank() })
            .joinToString(" · "),
        lineNames = lineNames,
        roadAddress = address,
        latitude = lat,
        longitude = lng
    )
}
