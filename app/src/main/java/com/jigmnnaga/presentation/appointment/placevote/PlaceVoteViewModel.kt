package com.jigmnnaga.presentation.appointment.placevote

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 장소 검색·투표 화면에서 사용자에게 한 번 알려야 하는 안내. */
enum class PlaceVoteNotice {
    SUBMIT_API_NOT_CONNECTED,
    MAX_SELECTION_REACHED
}

/**
 * 장소 검색·투표 화면의 검색어 필터, 후보 추가·삭제, 복수 선택, 저장 버튼 상태를 관리한다.
 * 장소 검색·투표 서버 연동 전이라 저장을 눌러도 서버로 보내지 않고, 선택값을 유지한 채 안내만 보낸다.
 * 화면에 쓰는 문구와 처음 후보는 호출하는 쪽이 [template]으로 넘긴다.
 *
 * @param maxSelection 한 번에 고를 수 있는 장소 수
 */
class PlaceVoteViewModel(
    private val template: PlaceVoteUiState,
    private val maxSelection: Int = DEFAULT_MAX_SELECTION
) : ViewModel() {

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
        publish()
    }

    /**
     * 화면이 넘겨 준 새 선택 집합을 반영한다. 최대 개수를 넘기면 선택을 바꾸지 않고 안내만 보낸다.
     * 후보에 없는 id는 무시한다.
     */
    fun onSelectionChange(newSelection: Set<String>) {
        val valid = newSelection.filter { id -> allCandidates.any { it.id == id } }.toSet()
        if (valid.size > maxSelection) {
            _notice.value = PlaceVoteNotice.MAX_SELECTION_REACHED
            return
        }
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
    }
}
