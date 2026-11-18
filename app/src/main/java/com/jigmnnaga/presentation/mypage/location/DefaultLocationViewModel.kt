package com.jigmnnaga.presentation.mypage.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jigmnnaga.domain.model.MemberLocation
import com.jigmnnaga.domain.model.MemberLocationInput
import com.jigmnnaga.domain.model.MemberLocationRejectReason
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
 * 마이페이지 "기본 위치 설정" 화면의 목록 조회, 추가·수정·삭제를 맡는다.
 * 위치를 고르는 검색은 출발 위치 선택 화면과 같은 장소 검색을 쓴다.
 *
 * @param searchDebounceMillis 검색어 입력이 멈춘 뒤 검색을 시작하기까지 기다리는 시간
 */
class DefaultLocationViewModel(
    private val memberLocationRepository: MemberLocationRepository,
    private val placeSearchRepository: PlaceSearchRepository,
    private val searchDebounceMillis: Long = DEFAULT_SEARCH_DEBOUNCE_MILLIS
) : ViewModel() {
    private val _uiState = MutableStateFlow(DefaultLocationUiState())
    val uiState: StateFlow<DefaultLocationUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var searchJob: Job? = null

    init {
        load()
    }

    /** 목록을 서버에서 다시 가져온다. 실패 화면의 "다시 시도"도 이걸 쓴다. */
    fun load() {
        loadJob?.cancel()
        _uiState.update { it.copy(loadStatus = DefaultLocationLoadStatus.LOADING) }
        loadJob = viewModelScope.launch {
            when (val result = memberLocationRepository.getLocations()) {
                is MemberLocationResult.Success -> _uiState.update {
                    it.copy(
                        loadStatus = DefaultLocationLoadStatus.LOADED,
                        locations = result.value.map { location -> location.toItem() }
                    )
                }
                MemberLocationResult.LoginRequired -> _uiState.update {
                    it.copy(loadStatus = DefaultLocationLoadStatus.FAILED, loginRequired = true)
                }
                else -> _uiState.update { it.copy(loadStatus = DefaultLocationLoadStatus.FAILED) }
            }
        }
    }

    fun consumeLoginRequired() = _uiState.update { it.copy(loginRequired = false) }

    /** "기본 위치 추가하기". 이미 3개거나 목록을 못 불러왔으면 열지 않는다. */
    fun onAddClick() = _uiState.update {
        if (it.canAdd) it.copy(editor = DefaultLocationEditorUiState()) else it
    }

    /** 카드의 "수정". 지금 저장된 위치를 처음 선택한 상태로 열어서 이름만 바꿔도 저장할 수 있다. */
    fun onEditClick(locationId: Long) = _uiState.update { state ->
        val item = state.locations.firstOrNull { it.id == locationId } ?: return@update state
        state.copy(
            editor = DefaultLocationEditorUiState(
                editingId = item.id,
                name = item.name,
                selected = item.toPlace()
            )
        )
    }

    fun onDismissEditor() {
        searchJob?.cancel()
        _uiState.update { it.copy(editor = null) }
    }

    fun onNameChange(name: String) = updateEditor {
        it.copy(name = name.take(MAX_DEFAULT_LOCATION_NAME_LENGTH), saveStatus = DefaultLocationActionStatus.IDLE)
    }

    fun onSelectPlace(place: DefaultLocationPlace) = updateEditor {
        it.copy(selected = place, saveStatus = DefaultLocationActionStatus.IDLE)
    }

    /** 검색어를 바꾸면 입력이 멈춘 뒤에 검색한다. 빈 검색어는 결과를 비운다. */
    fun onQueryChange(query: String) {
        searchJob?.cancel()
        val keyword = query.trim()
        updateEditor {
            it.copy(
                query = query,
                searchResults = emptyList(),
                searchStatus = if (keyword.isEmpty()) DefaultLocationSearchStatus.IDLE else DefaultLocationSearchStatus.LOADING
            )
        }
        if (keyword.isEmpty()) return
        searchJob = viewModelScope.launch {
            delay(searchDebounceMillis)
            val result = placeSearchRepository.search(keyword, PlaceSearchType.PLACE)
            updateEditor { it.withSearchResult(result) }
        }
    }

    /** "저장하기". 추가 중이면 등록하고, 수정 중이면 같은 위치를 바꾼다. */
    fun onSave() {
        val editor = _uiState.value.editor?.takeIf { it.canSave } ?: return
        val place = editor.selected ?: return
        if (editor.editingId == null && !_uiState.value.canAdd) return
        val input = MemberLocationInput(
            name = editor.name.trim(),
            address = place.address,
            latitude = place.latitude,
            longitude = place.longitude
        )
        updateEditor { it.copy(saveStatus = DefaultLocationActionStatus.LOADING) }
        viewModelScope.launch {
            val result = if (editor.editingId == null) {
                memberLocationRepository.createLocation(input)
            } else {
                memberLocationRepository.updateLocation(editor.editingId, input)
            }
            when (result) {
                is MemberLocationResult.Success -> _uiState.update { state ->
                    state.copy(
                        locations = state.locations.upsert(result.value.toItem()),
                        editor = null
                    )
                }
                MemberLocationResult.LoginRequired -> {
                    updateEditor { it.copy(saveStatus = DefaultLocationActionStatus.IDLE) }
                    _uiState.update { it.copy(loginRequired = true) }
                }
                is MemberLocationResult.Rejected -> if (result.reason == MemberLocationRejectReason.NOT_FOUND) {
                    // 수정하려던 위치가 이미 지워졌다. 편집을 닫고 목록을 새로 가져온다.
                    _uiState.update { it.copy(editor = null) }
                    load()
                } else {
                    updateEditor { it.copy(saveStatus = DefaultLocationActionStatus.FAILED) }
                }
                is MemberLocationResult.Failure ->
                    updateEditor { it.copy(saveStatus = DefaultLocationActionStatus.FAILED) }
            }
        }
    }

    /** 카드의 "삭제". 바로 지우지 않고 확인 팝업을 띄운다. */
    fun onDeleteClick(locationId: Long) = _uiState.update { state ->
        val item = state.locations.firstOrNull { it.id == locationId } ?: return@update state
        state.copy(deleteTarget = item, deleteStatus = DefaultLocationActionStatus.IDLE)
    }

    fun onDismissDelete() = _uiState.update { it.copy(deleteTarget = null) }

    /** 확인 팝업의 "삭제". 서버가 이미 없다고 답하면 지워진 것으로 본다. */
    fun onConfirmDelete() {
        val target = _uiState.value.deleteTarget ?: return
        if (_uiState.value.deleteStatus == DefaultLocationActionStatus.LOADING) return
        _uiState.update { it.copy(deleteStatus = DefaultLocationActionStatus.LOADING) }
        viewModelScope.launch {
            val result = memberLocationRepository.deleteLocation(target.id)
            val deleted = result is MemberLocationResult.Success ||
                (result is MemberLocationResult.Rejected && result.reason == MemberLocationRejectReason.NOT_FOUND)
            when {
                deleted -> _uiState.update { state ->
                    state.copy(
                        locations = state.locations.filterNot { it.id == target.id },
                        deleteTarget = null,
                        deleteStatus = DefaultLocationActionStatus.IDLE
                    )
                }
                result is MemberLocationResult.LoginRequired -> _uiState.update {
                    it.copy(
                        deleteTarget = null,
                        deleteStatus = DefaultLocationActionStatus.IDLE,
                        loginRequired = true
                    )
                }
                else -> _uiState.update {
                    it.copy(deleteTarget = null, deleteStatus = DefaultLocationActionStatus.FAILED)
                }
            }
        }
    }

    private fun updateEditor(transform: (DefaultLocationEditorUiState) -> DefaultLocationEditorUiState) =
        _uiState.update { state -> state.editor?.let { state.copy(editor = transform(it)) } ?: state }

    companion object {
        const val DEFAULT_SEARCH_DEBOUNCE_MILLIS = 300L
    }
}

private fun MemberLocation.toItem() = DefaultLocationItem(
    id = locationId,
    name = name,
    address = address,
    latitude = latitude,
    longitude = longitude
)

/** 수정을 시작할 때 처음 선택돼 있는 장소. 주소가 없으면 이름으로 보여준다. */
private fun DefaultLocationItem.toPlace() = DefaultLocationPlace(
    id = "saved-$id",
    title = address ?: name,
    detail = null,
    address = address,
    latitude = latitude,
    longitude = longitude
)

/** 같은 ID가 있으면 그 자리에서 바꾸고, 없으면 맨 뒤에 붙인다. */
private fun List<DefaultLocationItem>.upsert(item: DefaultLocationItem): List<DefaultLocationItem> =
    if (any { it.id == item.id }) map { if (it.id == item.id) item else it } else this + item

private fun DefaultLocationEditorUiState.withSearchResult(
    result: PlaceSearchResult<PlaceSearchPage>
): DefaultLocationEditorUiState = when (result) {
    is PlaceSearchResult.Success -> {
        val places = result.value.places.mapNotNull { it.toPlace() }
        copy(
            searchResults = places,
            searchStatus = if (places.isEmpty()) DefaultLocationSearchStatus.EMPTY else DefaultLocationSearchStatus.IDLE
        )
    }
    else -> copy(searchResults = emptyList(), searchStatus = DefaultLocationSearchStatus.FAILED)
}

/** 좌표가 없는 검색 결과는 출발 위치로 쓸 수 없어서 목록에서 뺀다. */
private fun SearchedPlace.toPlace(): DefaultLocationPlace? {
    val lat = latitude ?: return null
    val lng = longitude ?: return null
    val address = roadAddress ?: jibunAddress
    return DefaultLocationPlace(
        id = "search-$kakaoPlaceId",
        title = name,
        detail = listOfNotNull(lineNames, address).joinToString("·").takeIf { it.isNotBlank() },
        address = address,
        latitude = lat,
        longitude = lng
    )
}
