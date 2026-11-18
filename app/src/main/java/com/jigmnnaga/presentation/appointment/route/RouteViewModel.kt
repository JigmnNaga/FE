package com.jigmnnaga.presentation.appointment.route

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jigmnnaga.domain.model.MemberLocationResult
import com.jigmnnaga.domain.model.PlaceSearchPage
import com.jigmnnaga.domain.model.PlaceSearchResult
import com.jigmnnaga.domain.model.PlaceSearchType
import com.jigmnnaga.domain.model.SearchedPlace
import com.jigmnnaga.domain.model.TravelRejectReason
import com.jigmnnaga.domain.model.TravelResult
import com.jigmnnaga.domain.repository.MemberLocationRepository
import com.jigmnnaga.domain.repository.PlaceSearchRepository
import com.jigmnnaga.domain.repository.TravelRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 이동 경로 화면(ROU-001)의 단계 전환, 출발 위치 선택, 이동 API 연동을 맡는다.
 * 고른 출발 위치를 약속에 등록하고 이동수단 후보를 불러오며, "지금 출발하기"와 "도착"을 서버에 알린다.
 * 위치 공유(WebSocket)와 지도 위 내 위치 갱신은 후속 작업이다.
 *
 * 저장소와 현재 위치 제공자는 없어도 동작한다(없으면 해당 목록이 비어 있고, 이동수단은 화면 안에서만 바뀐다).
 *
 * @param currentLocationName "현재 위치" 후보에 붙일 이름
 * @param searchDebounceMillis 검색어 입력이 멈춘 뒤 검색을 시작하기까지 기다리는 시간
 * @param appointmentId 이동 API를 부를 약속 ID
 * @param nowMillis 지금 시각. 이동 중 남은 시간을 계산할 때 쓴다.
 * @param movingTickMillis 이동 중 남은 시간을 다시 계산하는 간격
 */
class RouteViewModel(
    initialState: RouteUiState = RouteUiState(),
    private val memberLocationRepository: MemberLocationRepository? = null,
    private val placeSearchRepository: PlaceSearchRepository? = null,
    private val currentLocationProvider: CurrentLocationProvider? = null,
    private val currentLocationName: String = DEFAULT_CURRENT_LOCATION_NAME,
    private val searchDebounceMillis: Long = DEFAULT_SEARCH_DEBOUNCE_MILLIS,
    private val appointmentId: Long = 0L,
    private val travelRepository: TravelRepository? = null,
    private val travelLabels: RouteTravelLabels = RouteTravelLabels(),
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val movingTickMillis: Long = DEFAULT_MOVING_TICK_MILLIS
) : ViewModel() {
    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<RouteUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var currentLocationJob: Job? = null
    private var optionsJob: Job? = null
    private var movingTickerJob: Job? = null
    private var departedAtMillis: Long = 0L
    private var appointmentMillis: Long? = null

    /** 초기화면에서 내 카드를 눌렀을 때. */
    fun onMyCardClick() = _uiState.update { it.openRouteSelection() }

    fun onSelectTransport(transport: RouteTransport) = _uiState.update { it.selectTransport(transport) }

    fun onToggleOption(transport: RouteTransport) = _uiState.update { it.toggleOptionExpanded(transport) }

    /**
     * "지금 출발하기". 이동수단을 서버에 저장한 뒤 출발을 알리고, 둘 다 되면 약속 이동 단계로 넘어간다.
     * 이미 출발한 상태라는 응답은 출발한 것으로 본다. 저장소가 없으면 화면 안에서만 넘어간다.
     */
    fun onStartMoving() {
        val repository = travelRepository
        if (repository == null) {
            _uiState.update { it.startMoving() }
            return
        }
        val option = _uiState.value.takeIf { it.canStart() }?.selectedOption ?: return
        setActionStatus(RouteRequestStatus.LOADING)
        viewModelScope.launch {
            val selected = repository.selectTravelMode(
                appointmentId,
                option.transport.toTravelMode(),
                option.durationMinutes
            )
            val departed = if (selected is TravelResult.Success) repository.depart(appointmentId) else selected
            if (departed.isSuccessOr(TravelRejectReason.ALREADY_DEPARTED)) {
                val now = nowMillis()
                departedAtMillis = now
                _uiState.update { it.departed(option.toMoving(now, now, travelLabels)) }
                startMovingTicker(option)
            } else {
                setActionStatus(RouteRequestStatus.FAILED)
            }
        }
    }

    fun onToggleMyRoute() = _uiState.update { it.toggleMyRouteExpanded() }

    /**
     * "도착". 서버에 도착을 알린 뒤 도착 완료로 바꾼다. 이미 도착한 상태라는 응답은 도착한 것으로 본다.
     * 저장소가 없으면 화면 안에서만 바뀐다.
     */
    fun onArrive() {
        val repository = travelRepository
        if (repository == null) {
            _uiState.update { it.arrive() }
            return
        }
        if (!_uiState.value.canArrive()) return
        setActionStatus(RouteRequestStatus.LOADING)
        viewModelScope.launch {
            val result = repository.arrive(appointmentId)
            if (result.isSuccessOr(TravelRejectReason.ALREADY_ARRIVED)) {
                movingTickerJob?.cancel()
                val now = nowMillis()
                _uiState.update { state ->
                    val arrived = state.copy(actionStatus = RouteRequestStatus.IDLE).arrive()
                    val moving = arrived.moving?.toArrived(now, appointmentMillis, travelLabels)
                    arrived.copy(
                        moving = moving,
                        destinationEstimateTitle = travelLabels.arrivedEstimateTitle.format(RouteTime.formatClock(now)),
                        destinationEstimateNote = arrivedEstimateNote(now, appointmentMillis, travelLabels),
                        members = arrived.members.map { member ->
                            if (member.isMe && moving != null) member.copy(description = moving.mySubtitle) else member
                        }
                    )
                }
            } else {
                setActionStatus(RouteRequestStatus.FAILED)
            }
        }
    }

    private fun setActionStatus(status: RouteRequestStatus) =
        _uiState.update { it.copy(actionStatus = status) }

    /** 이동 중에는 일정 간격으로 남은 시간을 다시 계산한다. 도착 직전이면 "도착" 버튼이 켜진다. */
    private fun startMovingTicker(option: RouteOptionUiState) {
        movingTickerJob?.cancel()
        movingTickerJob = viewModelScope.launch {
            while (true) {
                delay(movingTickMillis)
                _uiState.update { state ->
                    val current = state.moving
                    if (current == null || current.hasArrived) {
                        state
                    } else {
                        state.withMoving(
                            option.toMoving(departedAtMillis, nowMillis(), travelLabels, current.canArrive)
                        )
                    }
                }
            }
        }
    }

    /** 도착지 카드의 출발 위치 줄을 눌렀을 때. 열 때마다 내 기본 위치를 새로 불러온다. */
    fun onOpenStartPicker() {
        _uiState.update { it.openStartPicker() }
        if (_uiState.value.startPicker.isOpen) loadBasicLocations()
    }

    fun onCloseStartPicker() = _uiState.update { it.closeStartPicker() }

    fun onSelectStartCandidate(candidate: RouteStartLocation) =
        _uiState.update { it.selectStartCandidate(candidate) }

    /** "여기에서 출발하기". 위치를 확정하고, 그 위치를 약속에 등록한 뒤 이동수단 후보를 불러온다. */
    fun onConfirmStartLocation() {
        if (!_uiState.value.startPicker.let { it.isOpen && it.canConfirm }) return
        _uiState.update { it.confirmStartLocation() }
        _uiState.value.startLocation?.let(::loadTravelOptions)
    }

    /** 이동수단 후보 불러오기에 실패했을 때 같은 출발 위치로 다시 시도한다. */
    fun onRetryTravelOptions() {
        val state = _uiState.value
        if (state.optionsStatus != RouteRequestStatus.FAILED) return
        state.startLocation?.let(::loadTravelOptions)
    }

    private fun loadTravelOptions(start: RouteStartLocation) {
        val repository = travelRepository ?: return
        val latitude = start.latitude ?: return
        val longitude = start.longitude ?: return
        optionsJob?.cancel()
        _uiState.update {
            it.copy(
                options = emptyList(),
                selectedTransport = null,
                expandedTransport = null,
                destinationEstimateTitle = "",
                destinationEstimateNote = "",
                optionsStatus = RouteRequestStatus.LOADING,
                actionStatus = RouteRequestStatus.IDLE
            )
        }
        optionsJob = viewModelScope.launch {
            val registered = repository.registerDepartureLocation(appointmentId, start.name, latitude, longitude)
            val found = if (registered is TravelResult.Success) {
                (repository.getTravelOptions(appointmentId, latitude, longitude) as? TravelResult.Success)?.value
            } else {
                null
            }
            val options = found?.toRouteOptions(start.name, travelLabels).orEmpty()
            appointmentMillis = RouteTime.parseServer(found?.appointmentDateTime)
            _uiState.update {
                it.copy(
                    options = options,
                    optionsStatus = if (options.isEmpty()) RouteRequestStatus.FAILED else RouteRequestStatus.IDLE
                )
            }
        }
    }

    /** 위치 검색어가 바뀌었을 때. 입력이 멈추면 검색한다. */
    fun onStartQueryChange(query: String) {
        searchJob?.cancel()
        val keyword = query.trim()
        _uiState.update {
            it.copy(
                startPicker = it.startPicker.copy(
                    query = query,
                    searchResults = emptyList(),
                    searchStatus = if (keyword.isEmpty() || placeSearchRepository == null) {
                        RouteSearchStatus.IDLE
                    } else {
                        RouteSearchStatus.LOADING
                    }
                )
            )
        }
        val repository = placeSearchRepository ?: return
        if (keyword.isEmpty()) return
        searchJob = viewModelScope.launch {
            delay(searchDebounceMillis)
            val result = repository.search(keyword, PlaceSearchType.PLACE)
            _uiState.update { state ->
                state.copy(startPicker = state.startPicker.withSearchResult(result))
            }
        }
    }

    /**
     * "현재 위치" 줄을 눌렀을 때 권한이 이미 있는 경우. 이미 가져온 위치가 있으면 그대로 고르고,
     * 없으면 현재 위치를 새로 가져온다.
     */
    fun onCurrentLocationClick() {
        val current = _uiState.value.startPicker.current
        if (current != null) {
            onSelectStartCandidate(current)
        } else {
            requestCurrentLocation()
        }
    }

    /** 위치 권한을 받은 뒤 현재 위치를 가져온다. */
    fun requestCurrentLocation() {
        val provider = currentLocationProvider
        if (provider == null) {
            setCurrentStatus(RouteCurrentLocationStatus.FAILED)
            return
        }
        currentLocationJob?.cancel()
        setCurrentStatus(RouteCurrentLocationStatus.LOADING)
        currentLocationJob = viewModelScope.launch {
            val coordinates = provider.current()
            if (coordinates == null) {
                setCurrentStatus(RouteCurrentLocationStatus.FAILED)
            } else {
                val current = RouteStartLocation(
                    id = CURRENT_LOCATION_ID,
                    kind = RouteStartKind.CURRENT,
                    name = currentLocationName,
                    latitude = coordinates.latitude,
                    longitude = coordinates.longitude
                )
                _uiState.update { state ->
                    state.copy(
                        startPicker = state.startPicker.copy(
                            current = current,
                            currentStatus = RouteCurrentLocationStatus.IDLE,
                            selected = if (state.startPicker.isOpen) current else state.startPicker.selected
                        )
                    )
                }
            }
        }
    }

    fun onCurrentLocationPermissionDenied() = setCurrentStatus(RouteCurrentLocationStatus.PERMISSION_DENIED)

    private fun setCurrentStatus(status: RouteCurrentLocationStatus) =
        _uiState.update { it.copy(startPicker = it.startPicker.copy(currentStatus = status)) }

    private fun loadBasicLocations() {
        val repository = memberLocationRepository ?: return
        viewModelScope.launch {
            val result = repository.getLocations()
            _uiState.update { state ->
                val picker = state.startPicker
                state.copy(
                    startPicker = when (result) {
                        is MemberLocationResult.Success -> picker.copy(
                            basicLocations = result.value.map { location ->
                                RouteStartLocation(
                                    id = "$BASIC_LOCATION_ID_PREFIX${location.locationId}",
                                    kind = RouteStartKind.BASIC,
                                    name = location.name,
                                    detail = location.address,
                                    latitude = location.latitude,
                                    longitude = location.longitude
                                )
                            },
                            basicLocationsFailed = false
                        )
                        else -> picker.copy(basicLocationsFailed = true)
                    }
                )
            }
        }
    }

    /**
     * 뒤로 가기. 경로 선택 단계에서는 초기화면으로 돌아가고 true를 돌려 준다.
     * 그 밖의 단계에서는 처리하지 않고 false를 돌려 주어 호출한 쪽이 화면을 닫게 한다.
     */
    fun onBack(): Boolean {
        val current = _uiState.value
        if (current.stage != RouteStage.SELECT) return false
        _uiState.value = current.closeStartPicker().closeRouteSelection()
        return true
    }

    private companion object {
        const val DEFAULT_CURRENT_LOCATION_NAME = "현재 위치"
        const val DEFAULT_SEARCH_DEBOUNCE_MILLIS = 300L
        const val DEFAULT_MOVING_TICK_MILLIS = 30_000L
        const val CURRENT_LOCATION_ID = "current"
        const val BASIC_LOCATION_ID_PREFIX = "basic-"
    }
}

private fun RouteStartPickerUiState.withSearchResult(
    result: PlaceSearchResult<PlaceSearchPage>
): RouteStartPickerUiState = when (result) {
    is PlaceSearchResult.Success -> {
        val places = result.value.places.mapNotNull { it.toStartLocation() }
        copy(
            searchResults = places,
            searchStatus = if (places.isEmpty()) RouteSearchStatus.EMPTY else RouteSearchStatus.IDLE
        )
    }
    else -> copy(searchResults = emptyList(), searchStatus = RouteSearchStatus.FAILED)
}

/** 좌표가 없는 검색 결과는 경로를 조회할 수 없어서 목록에서 뺀다. */
private fun SearchedPlace.toStartLocation(): RouteStartLocation? {
    val lat = latitude ?: return null
    val lng = longitude ?: return null
    return RouteStartLocation(
        id = "search-$kakaoPlaceId",
        kind = RouteStartKind.SEARCH,
        name = name,
        detail = listOfNotNull(lineNames, roadAddress ?: jibunAddress).joinToString("·").takeIf { it.isNotBlank() },
        latitude = lat,
        longitude = lng
    )
}

/** 성공했거나, 서버가 [alreadyDone]이라고 답한 경우(이미 같은 상태라는 뜻)면 true. */
private fun TravelResult<Unit>.isSuccessOr(alreadyDone: TravelRejectReason): Boolean =
    this is TravelResult.Success || (this is TravelResult.Rejected && reason == alreadyDone)
