package com.jigmnnaga.presentation.appointment.route

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jigmnnaga.domain.model.LocationShareEnd
import com.jigmnnaga.domain.model.LocationShareError
import com.jigmnnaga.domain.model.MemberArrival
import com.jigmnnaga.domain.model.MemberDepartureStatus
import com.jigmnnaga.domain.model.MemberEtaUpdate
import com.jigmnnaga.domain.model.MemberLocationResult
import com.jigmnnaga.domain.model.MemberLocationUpdate
import com.jigmnnaga.domain.model.PlaceSearchPage
import com.jigmnnaga.domain.model.PlaceSearchResult
import com.jigmnnaga.domain.model.PlaceSearchType
import com.jigmnnaga.domain.model.SearchedPlace
import com.jigmnnaga.domain.model.TravelRejectReason
import com.jigmnnaga.domain.model.TravelResult
import com.jigmnnaga.domain.repository.LocationRealtime
import com.jigmnnaga.domain.repository.MemberLocationRepository
import com.jigmnnaga.domain.repository.PlaceSearchRepository
import com.jigmnnaga.domain.repository.TravelRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 이동 경로 화면(ROU-001)의 단계 전환, 출발 위치 선택, 이동 API 연동을 맡는다.
 * 고른 출발 위치를 약속에 등록하고 이동수단 후보를 불러오며, "지금 출발하기"와 "도착"을 서버에 알린다.
 * 실시간 위치 공유(STOMP)도 맡는다. 화면이 만들어지면 연결해 친구들의 이동·도착 소식과 내 도착 예정 시각을 받고,
 * 내가 출발하면 내 위치를 일정 간격으로 서버에 보낸다. 지도 위 표식 그리기는 후속 작업이다.
 *
 * 저장소, 현재 위치 제공자, 실시간 연결은 없어도 동작한다(없으면 해당 목록이 비어 있고, 이동수단은 화면 안에서만 바뀐다).
 *
 * @param currentLocationName "현재 위치" 후보에 붙일 이름
 * @param searchDebounceMillis 검색어 입력이 멈춘 뒤 검색을 시작하기까지 기다리는 시간
 * @param appointmentId 이동 API를 부를 약속 ID
 * @param nowMillis 지금 시각. 이동 중 남은 시간을 계산할 때 쓴다.
 * @param movingTickMillis 이동 중 남은 시간을 다시 계산하는 간격
 * @param locationRealtime 실시간 위치 공유 연결
 * @param locationUpdates 이동 중 내 위치 변화
 * @param locationSendIntervalMillis 내 위치를 서버에 보내는 최소 간격(서버는 1초 안의 반복 전송을 거절한다)
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
    private val movingTickMillis: Long = DEFAULT_MOVING_TICK_MILLIS,
    private val locationRealtime: LocationRealtime? = null,
    private val locationUpdates: CurrentLocationUpdates? = null,
    private val locationSendIntervalMillis: Long = DEFAULT_LOCATION_SEND_MILLIS
) : ViewModel() {
    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<RouteUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var currentLocationJob: Job? = null
    private var optionsJob: Job? = null
    private var movingTickerJob: Job? = null
    private var departedAtMillis: Long = 0L
    private var appointmentMillis: Long? = null
    private var movingOption: RouteOptionUiState? = null
    private var serverArrivalMillis: Long? = null
    private var shareJob: Job? = null
    private val realtimeJobs = mutableListOf<Job>()
    private var lastSentMillis: Long? = null
    private val myMemberId: String? = initialState.members.firstOrNull { it.isMe }?.id

    init {
        refreshDepartureOpen()
        startRealtime()
    }

    /** 오늘이 약속 당일인지 다시 확인해 "지금 출발하기"를 열거나 닫는다. 약속 날짜를 모르면 열어 둔다. */
    private fun refreshDepartureOpen() {
        val today = RouteTime.dayOf(nowMillis())
        _uiState.update { state ->
            val open = state.meetingDate.isNullOrBlank() || state.meetingDate == today
            if (state.departureOpen == open) state else state.copy(departureOpen = open)
        }
    }

    /** 초기화면에서 내 카드를 눌렀을 때. */
    fun onMyCardClick() {
        refreshDepartureOpen()
        _uiState.update { it.openRouteSelection() }
    }

    fun onSelectTransport(transport: RouteTransport) = _uiState.update { it.selectTransport(transport) }

    fun onToggleOption(transport: RouteTransport) = _uiState.update { it.toggleOptionExpanded(transport) }

    /**
     * "지금 출발하기". 이동수단을 서버에 저장한 뒤 출발을 알리고, 둘 다 되면 약속 이동 단계로 넘어간다.
     * 이미 출발한 상태라는 응답은 출발한 것으로 본다. 저장소가 없으면 화면 안에서만 넘어간다.
     */
    fun onStartMoving() {
        refreshDepartureOpen()
        if (!_uiState.value.departureOpen) return
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
                movingOption = option
                serverArrivalMillis = null
                _uiState.update { it.departed(option.toMoving(now, now, travelLabels)) }
                startMovingTicker()
                startLocationSharing()
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
                applyMyArrival(nowMillis())
            } else {
                setActionStatus(RouteRequestStatus.FAILED)
            }
        }
    }

    /** 내 도착을 화면에 반영한다. 서버 응답으로도, 서버가 위치로 도착을 알아챈 소식으로도 들어오며 한 번만 반영한다. */
    private fun applyMyArrival(at: Long) {
        if (_uiState.value.moving?.hasArrived != false) return
        movingTickerJob?.cancel()
        stopLocationSharing()
        _uiState.update { state ->
            val arrived = state.copy(actionStatus = RouteRequestStatus.IDLE).forceArrive()
            val moving = arrived.moving?.toArrived(at, appointmentMillis, travelLabels)
            arrived.copy(
                moving = moving,
                destinationEstimateTitle = travelLabels.arrivedEstimateTitle.format(RouteTime.formatClock(at)),
                destinationEstimateNote = arrivedEstimateNote(at, appointmentMillis, travelLabels),
                members = arrived.members.map { member ->
                    if (member.isMe && moving != null) member.copy(description = moving.mySubtitle) else member
                }
            )
        }
    }

    private fun setActionStatus(status: RouteRequestStatus) =
        _uiState.update { it.copy(actionStatus = status) }

    /** 이동 중에는 일정 간격으로 남은 시간을 다시 계산한다. 도착 직전이면 "도착" 버튼이 켜진다. */
    private fun startMovingTicker() {
        movingTickerJob?.cancel()
        movingTickerJob = viewModelScope.launch {
            while (true) {
                delay(movingTickMillis)
                refreshMoving()
            }
        }
    }

    /** 지금 시각과 서버가 알려 준 도착 예정 시각(있으면)으로 약속 이동 단계의 값을 다시 만든다. */
    private fun refreshMoving() {
        val option = movingOption ?: return
        _uiState.update { state ->
            val current = state.moving
            if (current == null || current.hasArrived) {
                state
            } else {
                state.withMoving(
                    option.toMoving(
                        departedAtMillis, nowMillis(), travelLabels, current.canArrive, serverArrivalMillis
                    )
                )
            }
        }
    }

    // ---- 실시간 위치 공유 ----

    /** 연결하고 약속의 위치·도착 예정·도착·공유 종료·오류 소식을 받기 시작한다. 내 위치 전송은 출발한 뒤에 시작한다. */
    private fun startRealtime() {
        val realtime = locationRealtime ?: return
        realtime.connect()
        collectRealtime(realtime.locations(appointmentId), ::onLocationUpdate)
        collectRealtime(realtime.etas(appointmentId), ::onEtaUpdate)
        collectRealtime(realtime.arrivals(appointmentId), ::onArrivalUpdate)
        collectRealtime(realtime.sharingEnds(appointmentId), ::onSharingEnd)
        collectRealtime(realtime.errors(), ::onShareError)
    }

    private fun <T> collectRealtime(flow: Flow<T>, handle: (T) -> Unit) {
        realtimeJobs += viewModelScope.launch { flow.collect(handle) }
    }

    /** 출발한 뒤 내 위치가 바뀔 때마다(최소 [locationSendIntervalMillis] 간격으로) 서버에 보낸다. */
    private fun startLocationSharing() {
        val realtime = locationRealtime ?: return
        val updates = locationUpdates ?: return
        shareJob?.cancel()
        lastSentMillis = null
        shareJob = viewModelScope.launch {
            updates.updates().collect { coordinates ->
                val now = nowMillis()
                val last = lastSentMillis
                if (last == null || now - last >= locationSendIntervalMillis) {
                    lastSentMillis = now
                    realtime.sendLocation(appointmentId, coordinates.latitude, coordinates.longitude)
                }
            }
        }
    }

    /**
     * 위치 권한 상태를 새로 확인한 결과. 권한이 없어서 위치 전송이 시작되지 못했다가 나중에 허용되면 전송을 다시 시작한다.
     * 도착했거나 공유가 끝나서 멈춘 경우([stopLocationSharing])에는 다시 시작하지 않는다.
     */
    fun onLocationPermissionChecked(granted: Boolean) {
        _uiState.update { if (it.locationPermissionGranted == granted) it else it.copy(locationPermissionGranted = granted) }
        val state = _uiState.value
        val ended = shareJob?.let { !it.isActive } ?: false
        if (granted && ended && state.stage == RouteStage.MOVING && state.moving?.hasArrived == false) {
            startLocationSharing()
        }
    }

    private fun stopLocationSharing() {
        shareJob?.cancel()
        shareJob = null
    }

    private fun isMe(memberId: Long): Boolean = myMemberId != null && myMemberId == memberId.toString()

    /** 친구의 위치 소식. 이동 중이면 이동수단과 함께 "이동 중"으로, 도착했으면 "도착 완료"로 바꾼다. */
    private fun onLocationUpdate(update: MemberLocationUpdate) {
        if (isMe(update.memberId)) return
        _uiState.update { state ->
            state.updateFriend(update.memberId.toString()) { member ->
                val located = member.copy(latitude = update.latitude, longitude = update.longitude)
                when {
                    member.status == RouteMemberStatus.ARRIVED -> located
                    update.departureStatus == MemberDepartureStatus.DEPARTED -> located.asMoving(travelLabels)
                    update.departureStatus == MemberDepartureStatus.ARRIVED -> located.asArrived(travelLabels)
                    else -> located
                }
            }
        }
    }

    /** 내 도착 예정 시각이 서버에서 다시 계산되어 오면 남은 시간과 도착지 카드를 그 시각으로 맞춘다. */
    private fun onEtaUpdate(update: MemberEtaUpdate) {
        if (!isMe(update.memberId)) return
        val arrival = RouteTime.parseServer(update.estimatedArrivalTime) ?: return
        val state = _uiState.value
        if (state.stage != RouteStage.MOVING || state.moving?.hasArrived != false) return
        serverArrivalMillis = arrival
        refreshMoving()
        val gap = appointmentMillis?.let { RouteTime.minutesBetween(arrival, it) }
        _uiState.update {
            it.copy(
                destinationEstimateTitle = travelLabels.estimateTitle.format(RouteTime.formatClock(arrival)),
                destinationEstimateNote = estimateNote(appointmentMillis, gap, travelLabels)
                    ?: it.destinationEstimateNote
            )
        }
    }

    private fun onArrivalUpdate(update: MemberArrival) {
        if (isMe(update.memberId)) {
            applyMyArrival(RouteTime.parseServer(update.arrivedAt) ?: nowMillis())
        } else {
            _uiState.update { state ->
                state.updateFriend(update.memberId.toString()) { it.asArrived(travelLabels) }
            }
        }
    }

    /** 내 위치 공유가 끝났으면(직접 껐거나 도착) 더 보내지 않는다. 친구의 공유 종료는 마지막 상태를 그대로 둔다. */
    private fun onSharingEnd(end: LocationShareEnd) {
        if (isMe(end.memberId)) stopLocationSharing()
    }

    /** 서버가 내 위치를 거절했을 때. 너무 자주 보낸 경우와 출발 전 오류는 다음 갱신에서 풀리므로 계속 보낸다. */
    private fun onShareError(error: LocationShareError) {
        when (error) {
            LocationShareError.SHARING_STOPPED, LocationShareError.OTHER -> stopLocationSharing()
            LocationShareError.NOT_DEPARTED, LocationShareError.TOO_FREQUENT -> Unit
        }
    }

    override fun onCleared() {
        realtimeJobs.forEach { it.cancel() }
        realtimeJobs.clear()
        stopLocationSharing()
        locationRealtime?.disconnect()
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
        const val DEFAULT_LOCATION_SEND_MILLIS = 5_000L
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

/** 친구가 이동을 시작했다. 약속 상세가 알려 준 이동수단이 있으면 함께 보여 준다. */
private fun RouteMemberUiState.asMoving(labels: RouteTravelLabels): RouteMemberUiState {
    val shown = plannedTransport ?: transport
    return copy(
        status = RouteMemberStatus.MOVING,
        statusLabel = null,
        description = labels.friendMovingText(shown),
        transport = shown
    )
}

private fun RouteMemberUiState.asArrived(labels: RouteTravelLabels): RouteMemberUiState = copy(
    status = RouteMemberStatus.ARRIVED,
    statusLabel = null,
    description = labels.friendArrived
)

/** 성공했거나, 서버가 [alreadyDone]이라고 답한 경우(이미 같은 상태라는 뜻)면 true. */
private fun TravelResult<Unit>.isSuccessOr(alreadyDone: TravelRejectReason): Boolean =
    this is TravelResult.Success || (this is TravelResult.Rejected && reason == alreadyDone)
