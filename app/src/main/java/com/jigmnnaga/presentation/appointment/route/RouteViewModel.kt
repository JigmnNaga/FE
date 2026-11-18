package com.jigmnnaga.presentation.appointment.route

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jigmnnaga.domain.model.MemberLocationResult
import com.jigmnnaga.domain.model.PlaceSearchPage
import com.jigmnnaga.domain.model.PlaceSearchResult
import com.jigmnnaga.domain.model.PlaceSearchType
import com.jigmnnaga.domain.model.SearchedPlace
import com.jigmnnaga.domain.repository.MemberLocationRepository
import com.jigmnnaga.domain.repository.PlaceSearchRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 이동 경로 화면(ROU-001)의 단계 전환과 출발 위치 선택을 맡는다. 경로 조회·출발 기록·위치 공유 같은
 * 서버 연동은 후속 작업에서 붙이고, 지금은 화면 안의 선택과 펼침, 출발 위치 고르기만 다룬다.
 *
 * 저장소와 현재 위치 제공자는 없어도 동작한다(없으면 해당 목록이 비어 있다).
 *
 * @param currentLocationName "현재 위치" 후보에 붙일 이름
 * @param searchDebounceMillis 검색어 입력이 멈춘 뒤 검색을 시작하기까지 기다리는 시간
 */
class RouteViewModel(
    initialState: RouteUiState = RouteUiState(),
    private val memberLocationRepository: MemberLocationRepository? = null,
    private val placeSearchRepository: PlaceSearchRepository? = null,
    private val currentLocationProvider: CurrentLocationProvider? = null,
    private val currentLocationName: String = DEFAULT_CURRENT_LOCATION_NAME,
    private val searchDebounceMillis: Long = DEFAULT_SEARCH_DEBOUNCE_MILLIS
) : ViewModel() {
    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<RouteUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var currentLocationJob: Job? = null

    /** 초기화면에서 내 카드를 눌렀을 때. */
    fun onMyCardClick() = _uiState.update { it.openRouteSelection() }

    fun onSelectTransport(transport: RouteTransport) = _uiState.update { it.selectTransport(transport) }

    fun onToggleOption(transport: RouteTransport) = _uiState.update { it.toggleOptionExpanded(transport) }

    fun onStartMoving() = _uiState.update { it.startMoving() }

    fun onToggleMyRoute() = _uiState.update { it.toggleMyRouteExpanded() }

    fun onArrive() = _uiState.update { it.arrive() }

    /** 도착지 카드의 출발 위치 줄을 눌렀을 때. 열 때마다 내 기본 위치를 새로 불러온다. */
    fun onOpenStartPicker() {
        _uiState.update { it.openStartPicker() }
        if (_uiState.value.startPicker.isOpen) loadBasicLocations()
    }

    fun onCloseStartPicker() = _uiState.update { it.closeStartPicker() }

    fun onSelectStartCandidate(candidate: RouteStartLocation) =
        _uiState.update { it.selectStartCandidate(candidate) }

    fun onConfirmStartLocation() = _uiState.update { it.confirmStartLocation() }

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
