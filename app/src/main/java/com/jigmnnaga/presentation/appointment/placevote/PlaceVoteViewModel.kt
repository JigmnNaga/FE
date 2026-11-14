package com.jigmnnaga.presentation.appointment.placevote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jigmnnaga.domain.model.PlaceSearchResult
import com.jigmnnaga.domain.model.PlaceSearchType
import com.jigmnnaga.domain.model.SearchedPlace
import com.jigmnnaga.domain.repository.PlaceSearchRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 장소 검색·투표 화면에서 사용자에게 한 번 알려야 하는 안내. */
enum class PlaceVoteNotice {
    SUBMIT_API_NOT_CONNECTED,
    MAX_SELECTION_REACHED,

    /** 장소 검색에 로그인이 필요함. 로그인 화면으로 보낸다. */
    SEARCH_LOGIN_REQUIRED,
    SEARCH_FAILED_RETRYABLE,
    SEARCH_FAILED
}

/**
 * 장소 검색·투표 화면의 검색어 필터, 후보 추가·삭제, 복수 선택, 저장 버튼 상태를 관리한다.
 * 장소 검색·투표 서버 연동 전이라 저장을 눌러도 서버로 보내지 않고, 선택값을 유지한 채 안내만 보낸다.
 * 화면에 쓰는 문구와 처음 후보는 호출하는 쪽이 [template]으로 넘긴다.
 *
 * [searchRepository]가 있고 방장 화면이면, 검색어를 입력할 때 서버에서 지하철역을 검색해 결과를 후보로 보여 준다.
 * 검색 결과에서 고른 장소는 후보 목록에 남고, 검색어를 지우면 고른 후보 목록으로 돌아간다.
 * 그 밖의 경우(참여자 화면 등)에는 넘겨 받은 후보 안에서만 걸러 보여 준다.
 *
 * @param maxSelection 한 번에 고를 수 있는 장소 수
 * @param searchDebounceMillis 검색어 입력이 멈춘 뒤 검색을 시작하기까지 기다리는 시간
 */
class PlaceVoteViewModel(
    private val template: PlaceVoteUiState,
    private val maxSelection: Int = DEFAULT_MAX_SELECTION,
    private val searchRepository: PlaceSearchRepository? = null,
    private val searchDebounceMillis: Long = DEFAULT_SEARCH_DEBOUNCE_MILLIS
) : ViewModel() {

    private val usesServerSearch = searchRepository != null && template.role == PlaceVoteRole.HOST
    private var searchResults: List<PlaceCandidateUiModel> = emptyList()
    private var searchJob: Job? = null

    private var allCandidates: List<PlaceCandidateUiModel> = template.candidates
    private var selectedIds: Set<String> = template.selectedCandidateIds
        .filter { id -> allCandidates.any { it.id == id } }
        .take(maxSelection)
        .toSet()
    private var query: String = template.searchQuery

    private val _uiState = MutableStateFlow(buildState())
    val uiState: StateFlow<PlaceVoteUiState> = _uiState.asStateFlow()

    private val _notice = MutableStateFlow<PlaceVoteNotice?>(null)
    val notice: StateFlow<PlaceVoteNotice?> = _notice.asStateFlow()

    /** 선택한 장소 요약 카드를 보여 줄 화면인지. */
    val showsSelectedSummary: Boolean = template.selectedSummaryTitle != null

    fun onSearchQueryChange(value: String) {
        query = value
        if (usesServerSearch) {
            searchServer(value.trim())
        }
        publish()
    }

    private fun searchServer(keyword: String) {
        searchJob?.cancel()
        searchResults = emptyList()
        if (keyword.isEmpty()) return
        val repository = searchRepository ?: return
        searchJob = viewModelScope.launch {
            delay(searchDebounceMillis)
            when (val result = repository.search(keyword, PlaceSearchType.STATION)) {
                is PlaceSearchResult.Success -> {
                    searchResults = result.value.places.map { it.toCandidate() }
                    publish()
                }
                PlaceSearchResult.LoginRequired -> _notice.value = PlaceVoteNotice.SEARCH_LOGIN_REQUIRED
                is PlaceSearchResult.Failure -> _notice.value =
                    if (result.retryable) PlaceVoteNotice.SEARCH_FAILED_RETRYABLE else PlaceVoteNotice.SEARCH_FAILED
            }
        }
    }

    /** 로그인 후 같은 검색어로 다시 검색한다. */
    fun retrySearch() {
        if (usesServerSearch) searchServer(query.trim())
    }

    private fun SearchedPlace.toCandidate() = PlaceCandidateUiModel(
        id = kakaoPlaceId,
        name = if (lineNames.isNullOrBlank()) name else "$name · $lineNames",
        address = roadAddress ?: jibunAddress
    )

    /**
     * 화면이 넘겨 준 새 선택 집합을 반영한다. 최대 개수를 넘기면 선택을 바꾸지 않고 안내만 보낸다.
     * 후보에 없는 id는 무시한다.
     */
    fun onSelectionChange(newSelection: Set<String>) {
        val known = allCandidates + searchResults
        val valid = newSelection.filter { id -> known.any { it.id == id } }.toSet()
        if (valid.size > maxSelection) {
            _notice.value = PlaceVoteNotice.MAX_SELECTION_REACHED
            return
        }
        // 검색 결과에서 고른 장소는 검색어를 지워도 남도록 후보 목록에 넣는다.
        val added = searchResults.filter { it.id in valid && allCandidates.none { c -> c.id == it.id } }
        if (added.isNotEmpty()) allCandidates = allCandidates + added
        selectedIds = valid
        publish()
    }

    /** 후보를 삭제하고, 선택돼 있었다면 선택에서도 뺀다. */
    fun onRemoveCandidate(id: String) {
        if (allCandidates.none { it.id == id }) return
        allCandidates = allCandidates.filterNot { it.id == id }
        selectedIds = selectedIds - id
        publish()
    }

    /** 새 후보를 목록 끝에 추가한다. 같은 id가 이미 있으면 무시한다. */
    fun addCandidate(candidate: PlaceCandidateUiModel) {
        if (allCandidates.any { it.id == candidate.id }) return
        allCandidates = allCandidates + candidate
        publish()
    }

    /** 저장을 누르면 서버로 보내지 않고 안내만 보낸다. 선택값은 유지한다. */
    fun onSubmit() {
        if (selectedIds.isEmpty()) return
        _notice.value = PlaceVoteNotice.SUBMIT_API_NOT_CONNECTED
    }

    fun clearNotice() {
        _notice.value = null
    }

    private fun publish() {
        _uiState.value = buildState()
    }

    private fun buildState(): PlaceVoteUiState {
        val keyword = query.trim()
        val visible = if (keyword.isEmpty()) {
            allCandidates
        } else if (usesServerSearch) {
            searchResults
        } else {
            allCandidates.filter {
                it.name.contains(keyword, ignoreCase = true) ||
                    it.address?.contains(keyword, ignoreCase = true) == true
            }
        }
        return template.copy(
            searchQuery = query,
            candidates = visible,
            selectedCandidateIds = selectedIds,
            submitEnabled = selectedIds.isNotEmpty(),
            selectedPlaceNames = allCandidates.filter { it.id in selectedIds }.map { it.name }
        )
    }

    companion object {
        const val DEFAULT_MAX_SELECTION = 5
        const val DEFAULT_SEARCH_DEBOUNCE_MILLIS = 300L
    }
}
