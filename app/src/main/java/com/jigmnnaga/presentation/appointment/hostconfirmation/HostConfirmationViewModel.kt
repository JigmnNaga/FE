package com.jigmnnaga.presentation.appointment.hostconfirmation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jigmnnaga.domain.model.AppointmentLocation
import com.jigmnnaga.domain.model.ConfirmDetailPlace
import com.jigmnnaga.domain.model.ConfirmationRejectReason
import com.jigmnnaga.domain.model.ConfirmationResult
import com.jigmnnaga.domain.model.PlaceSearchResult
import com.jigmnnaga.domain.model.PlaceSearchType
import com.jigmnnaga.domain.model.SearchedPlace
import com.jigmnnaga.domain.model.TimeCandidate
import com.jigmnnaga.domain.repository.ConfirmationRepository
import com.jigmnnaga.domain.repository.PlaceSearchRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 방장 최종 확정 화면에서 사용자에게 한 번 알려야 하는 안내. */
enum class HostConfirmationNotice {
    /** 서버 저장소가 연결되지 않은 화면(미리보기 등)에서 확정을 눌렀을 때. */
    CONFIRM_API_NOT_CONNECTED,

    /** 로그인이 필요함. 로그인 화면으로 보낸다. */
    LOGIN_REQUIRED,
    NOT_HOST,
    APPOINTMENT_CANCELED,

    /** 후보 날짜나 후보 장소에 없는 값을 보냄. */
    INVALID_SELECTION,
    FAILURE_RETRYABLE,
    FAILURE
}

/**
 * 방장 최종 확정 화면의 단계 이동, 날짜·시간·장소·세부 위치 선택, 확정 버튼 활성 조건을 관리한다.
 *
 * [confirmationRepository]가 있으면 가능 인원이 많은 시간대를 후보로 불러오고, 확정을 누르면 날짜·시간과
 * 장소를 차례로 서버에 확정한다. 세부 위치는 [placeSearchRepository]로 상호를 검색해 고르며, 고르지 않으면
 * 나중에 정한다. 서버 저장소가 없으면 서버로 보내지 않고 선택값을 유지한 채 안내만 보낸다.
 * 약속 정보는 호출하는 쪽이 [initialState]로, 후보 장소는 [candidateLocations]로 넘긴다.
 *
 * @param isHost 방장 여부. 방장이 아니면 확정할 수 없다.
 */
class HostConfirmationViewModel(
    initialState: HostConfirmationUiState,
    private val isHost: Boolean = true,
    private val appointmentId: Long = 0L,
    candidateLocations: List<AppointmentLocation> = emptyList(),
    private val confirmationRepository: ConfirmationRepository? = null,
    private val placeSearchRepository: PlaceSearchRepository? = null,
    private val searchDebounceMillis: Long = DEFAULT_SEARCH_DEBOUNCE_MILLIS
) : ViewModel() {

    private var timeCandidates: Map<String, TimeCandidate> = emptyMap()
    private var detailResults: Map<String, SearchedPlace> = emptyMap()
    private var selectedDetailPlace: SearchedPlace? = null
    private var searchJob: Job? = null
    private var loadJob: Job? = null
    private val locationIds: Map<String, Long> = candidateLocations
        .mapNotNull { location -> location.locationId?.let { it.toString() to it } }
        .toMap()

    private val _uiState = MutableStateFlow(
        initialState.withPlaceChoices(candidateLocations).withConfirmEligibility()
    )
    val uiState: StateFlow<HostConfirmationUiState> = _uiState.asStateFlow()

    private val _notice = MutableStateFlow<HostConfirmationNotice?>(null)
    val notice: StateFlow<HostConfirmationNotice?> = _notice.asStateFlow()

    fun onEditDateTime() = moveTo(HostConfirmationStep.DATE_TIME)

    fun onEditPlace() = moveTo(HostConfirmationStep.PLACE)

    fun onOpenDetailLocation() = moveTo(HostConfirmationStep.DETAIL_LOCATION)

    /** 후보 중 하나를 고르면 선택값을 바꾸고 확인 단계로 돌아간다. 없는 후보는 무시한다. */
    fun onSelectDateTime(id: String) {
        edit {
            val choice = dateTimeChoices.firstOrNull { it.id == id } ?: return@edit this
            copy(
                dateTimeChoices = dateTimeChoices.map { it.copy(selected = it.id == id) },
                selectedDateTime = choice.title,
                step = HostConfirmationStep.REVIEW
            )
        }
    }

    fun onSelectPlace(id: String) {
        edit {
            val choice = placeChoices.firstOrNull { it.id == id } ?: return@edit this
            copy(
                placeChoices = placeChoices.map { it.copy(selected = it.id == id) },
                selectedPlace = choice.title,
                selectedPlaceDescription = choice.supportingText,
                step = HostConfirmationStep.REVIEW
            )
        }
    }

    fun onSelectDetailLocation(id: String) {
        detailResults[id]?.let { selectedDetailPlace = it }
        edit {
            val choice = detailLocationChoices.firstOrNull { it.id == id } ?: return@edit this
            copy(
                detailLocationChoices = detailLocationChoices.map { it.copy(selected = it.id == id) },
                selectedDetailLocation = choice.title,
                step = HostConfirmationStep.REVIEW
            )
        }
    }

    /** 세부 위치는 나중에 정하기로 하고 선택을 비운 채 확인 단계로 돌아간다. */
    fun onChooseDetailLater() {
        selectedDetailPlace = null
        edit {
            copy(
                detailLocationChoices = detailLocationChoices.map { it.copy(selected = false) },
                selectedDetailLocation = null,
                step = HostConfirmationStep.REVIEW
            )
        }
    }

    /** 선택 단계에서는 확인 단계로 돌아가고 true를 돌려준다. 확인·완료 단계면 false라 화면을 닫아야 한다. */
    fun onBack(): Boolean {
        val step = _uiState.value.step
        return if (step == HostConfirmationStep.REVIEW || step == HostConfirmationStep.COMPLETE) {
            false
        } else {
            edit { copy(step = HostConfirmationStep.REVIEW) }
            true
        }
    }

    /** 확정 조건이 맞으면 서버에 날짜·시간, 장소 순서로 확정한다. 서버 저장소가 없으면 안내만 띄운다. */
    fun onConfirm() {
        val state = _uiState.value
        if (state.step != HostConfirmationStep.REVIEW || !state.canConfirm || state.isBusy) return
        val repository = confirmationRepository
        if (repository == null) {
            _notice.value = HostConfirmationNotice.CONFIRM_API_NOT_CONNECTED
            return
        }
        val timeId = state.dateTimeChoices.firstOrNull { it.selected }?.id
        val time = timeId?.let { timeCandidates[it] }
        val locationId = state.placeChoices.firstOrNull { it.selected }?.id?.let { locationIds[it] }
        if (time == null || locationId == null) {
            _notice.value = HostConfirmationNotice.INVALID_SELECTION
            return
        }
        val detail = selectedDetailPlace?.toConfirmDetailPlace()

        edit { copy(isBusy = true) }
        viewModelScope.launch {
            val dateResult = repository.confirmDateTime(appointmentId, time.date, time.startTime)
            if (dateResult !is ConfirmationResult.Success) {
                finishWithFailure(dateResult)
                return@launch
            }
            val locationResult = repository.confirmLocation(appointmentId, locationId, detail)
            if (locationResult !is ConfirmationResult.Success) {
                finishWithFailure(locationResult)
                return@launch
            }
            edit { copy(isBusy = false, step = HostConfirmationStep.COMPLETE) }
        }
    }

    private fun finishWithFailure(result: ConfirmationResult<*>) {
        edit { copy(isBusy = false) }
        _notice.value = result.toNotice()
    }

    /** 가능 인원이 많은 시간대를 불러와 날짜·시간 후보로 보여 준다. 로그인 뒤에도 다시 부른다. */
    fun loadTimeCandidates() {
        val repository = confirmationRepository ?: return
        if (!isHost || loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            when (val result = repository.getTimeCandidates(appointmentId)) {
                is ConfirmationResult.Success -> {
                    val total = result.value.totalMemberCount
                    timeCandidates = result.value.candidates.associateBy { it.choiceId() }
                    edit {
                        val previous = dateTimeChoices.firstOrNull { it.selected }?.id
                        copy(
                            dateTimeChoices = result.value.candidates.map {
                                it.toChoice(total).copy(selected = it.choiceId() == previous)
                            }
                        )
                    }
                }
                else -> _notice.value = result.toNotice()
            }
        }
    }

    /** 세부 위치 검색어를 바꾼다. 입력이 멈춘 뒤 상호·장소를 서버에서 검색해 결과를 후보로 보여 준다. */
    fun onDetailSearchQueryChange(value: String) {
        edit { copy(detailSearchQuery = value) }
        val repository = placeSearchRepository ?: return
        searchJob?.cancel()
        val keyword = value.trim()
        if (keyword.isEmpty()) {
            detailResults = emptyMap()
            edit { copy(detailLocationChoices = emptyList()) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(searchDebounceMillis)
            when (val result = repository.search(keyword, PlaceSearchType.PLACE)) {
                is PlaceSearchResult.Success -> {
                    // 서버가 확정에 좌표를 요구하므로 좌표가 없는 장소는 고를 수 없게 뺀다.
                    val places = result.value.places.filter { it.latitude != null && it.longitude != null }
                    detailResults = places.associateBy { it.kakaoPlaceId }
                    edit {
                        copy(
                            detailLocationChoices = places.map {
                                HostConfirmationChoice(
                                    id = it.kakaoPlaceId,
                                    title = it.name,
                                    supportingText = it.roadAddress ?: it.jibunAddress,
                                    selected = it.kakaoPlaceId == selectedDetailPlace?.kakaoPlaceId
                                )
                            }
                        )
                    }
                }
                PlaceSearchResult.LoginRequired -> _notice.value = HostConfirmationNotice.LOGIN_REQUIRED
                is PlaceSearchResult.Failure -> _notice.value =
                    if (result.retryable) HostConfirmationNotice.FAILURE_RETRYABLE else HostConfirmationNotice.FAILURE
            }
        }
    }

    fun clearNotice() {
        _notice.value = null
    }

    private fun ConfirmationResult<*>.toNotice(): HostConfirmationNotice = when (this) {
        is ConfirmationResult.Success -> HostConfirmationNotice.FAILURE
        ConfirmationResult.LoginRequired -> HostConfirmationNotice.LOGIN_REQUIRED
        ConfirmationResult.NotHost -> HostConfirmationNotice.NOT_HOST
        is ConfirmationResult.Rejected -> when (reason) {
            ConfirmationRejectReason.APPOINTMENT_CANCELED -> HostConfirmationNotice.APPOINTMENT_CANCELED
            ConfirmationRejectReason.INVALID_DATE,
            ConfirmationRejectReason.INVALID_LOCATION -> HostConfirmationNotice.INVALID_SELECTION
        }
        is ConfirmationResult.Failure ->
            if (retryable) HostConfirmationNotice.FAILURE_RETRYABLE else HostConfirmationNotice.FAILURE
    }

    private fun HostConfirmationUiState.withPlaceChoices(
        locations: List<AppointmentLocation>
    ): HostConfirmationUiState {
        if (locations.isEmpty()) return this
        return copy(
            placeChoices = locations.mapNotNull { location ->
                val id = location.locationId?.toString() ?: return@mapNotNull null
                HostConfirmationChoice(
                    id = id,
                    title = location.placeName,
                    supportingText = listOfNotNull(location.lineNames, location.address)
                        .filter { it.isNotBlank() }
                        .joinToString(" · ")
                        .ifBlank { null },
                    selected = placeChoices.firstOrNull { it.id == id }?.selected == true
                )
            }
        )
    }

    private fun moveTo(step: HostConfirmationStep) {
        if (_uiState.value.step == HostConfirmationStep.COMPLETE) return
        edit { copy(step = step) }
    }

    private fun edit(transform: HostConfirmationUiState.() -> HostConfirmationUiState) {
        _uiState.value = _uiState.value.transform().withConfirmEligibility()
    }

    /** 방장이 날짜·시간과 장소를 모두 골랐고 처리 중이 아닐 때만 확정할 수 있다. */
    private fun HostConfirmationUiState.withConfirmEligibility(): HostConfirmationUiState = copy(
        canConfirm = isHost &&
            !selectedDateTime.isNullOrBlank() &&
            !selectedPlace.isNullOrBlank() &&
            !isBusy
    )

    companion object {
        const val DEFAULT_SEARCH_DEBOUNCE_MILLIS = 300L
    }
}

private fun TimeCandidate.choiceId(): String = "$date|$startTime"

private fun SearchedPlace.toConfirmDetailPlace(): ConfirmDetailPlace? {
    val lat = latitude ?: return null
    val lng = longitude ?: return null
    return ConfirmDetailPlace(
        placeName = name,
        address = roadAddress ?: jibunAddress,
        latitude = lat,
        longitude = lng
    )
}

/** 시간대 후보를 화면 선택지로 바꾼다. minSdk 24라 java.time 없이 `yyyy-MM-dd`를 직접 나눈다. */
private fun TimeCandidate.toChoice(totalMemberCount: Int): HostConfirmationChoice {
    val parts = date.split("-")
    val day = if (parts.size == 3) "${parts[1].toIntOrNull() ?: parts[1]}월 ${parts[2].toIntOrNull() ?: parts[2]}일" else date
    return HostConfirmationChoice(
        id = choiceId(),
        title = "$day $startTime~$endTime",
        supportingText = "${availableCount}/${totalMemberCount}명 가능"
    )
}
