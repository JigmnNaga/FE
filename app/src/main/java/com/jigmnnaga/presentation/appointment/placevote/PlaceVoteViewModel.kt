package com.jigmnnaga.presentation.appointment.placevote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jigmnnaga.domain.model.AppointmentLocation
import com.jigmnnaga.domain.model.AppointmentManageRejectReason
import com.jigmnnaga.domain.model.AppointmentManageResult
import com.jigmnnaga.domain.model.CandidateLocationChange
import com.jigmnnaga.domain.model.LocationVote
import com.jigmnnaga.domain.model.NewCandidateLocation
import com.jigmnnaga.domain.model.PlaceSearchResult
import com.jigmnnaga.domain.model.PlaceSearchType
import com.jigmnnaga.domain.model.ScheduleRejectReason
import com.jigmnnaga.domain.model.ScheduleResult
import com.jigmnnaga.domain.model.ScheduleStatus
import com.jigmnnaga.domain.model.ScheduleSubmission
import com.jigmnnaga.domain.model.SearchedPlace
import com.jigmnnaga.domain.repository.AppointmentManageRepository
import com.jigmnnaga.domain.repository.MyMemberIdProvider
import com.jigmnnaga.domain.repository.PlaceSearchRepository
import com.jigmnnaga.domain.repository.ScheduleRepository
import com.jigmnnaga.presentation.appointment.AppointmentRefreshSignal
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
    SEARCH_FAILED,

    /** 방장이 고른 후보 장소를 서버가 받아 줬다. 화면은 약속방으로 돌아간다. */
    SAVED,

    /** 참여자가 고른 장소를 서버가 받아 줬다. */
    SUBMITTED,

    /** 저장·제출·현황 조회에 로그인이 필요함. 로그인하면 하던 일을 다시 한다. */
    ACTION_LOGIN_REQUIRED,

    /** 취소됐거나 응답 기한이 지난 약속. */
    ACTION_CLOSED,

    /** 서버가 받지 않는 장소(후보가 아니거나 위치가 없음). */
    ACTION_INVALID_PLACE,

    /** 방장이 아니거나 약속에 참여한 멤버가 아님. */
    ACTION_NOT_ALLOWED,
    ACTION_FAILED_RETRYABLE,
    ACTION_FAILED,
    VOTES_LOAD_FAILED
}

/** 서버가 아는 후보 장소의 화면용 id 앞에 붙는 말. 검색 결과(카카오 장소 id)와 겹치지 않게 한다. */
internal const val PLACE_LOCATION_ID_PREFIX = "location-"

/** 서버가 아는 후보 장소를 화면 후보로 바꾼다. 장소 ID가 없는 장소는 고를 수 없어 null이다. */
internal fun AppointmentLocation.toPlaceCandidate(): PlaceCandidateUiModel? {
    val id = locationId ?: return null
    return PlaceCandidateUiModel(
        id = "$PLACE_LOCATION_ID_PREFIX$id",
        name = if (lineNames.isNullOrBlank()) placeName else "$placeName · $lineNames",
        address = address
    )
}

/** 화면용 후보 id에서 서버의 후보 장소 ID를 꺼낸다. 서버가 모르는 장소(검색 결과)면 null이다. */
internal fun serverLocationId(candidateId: String): Long? =
    if (candidateId.startsWith(PLACE_LOCATION_ID_PREFIX)) {
        candidateId.removePrefix(PLACE_LOCATION_ID_PREFIX).toLongOrNull()
    } else {
        null
    }

/**
 * 장소 검색·투표 화면의 검색어 필터, 후보 추가·삭제, 선택, 저장 버튼 상태를 관리한다.
 * 화면에 쓰는 문구와 처음 후보는 호출하는 쪽이 [template]으로 넘긴다.
 *
 * [searchRepository]가 있고 방장 화면이면, 검색어를 입력할 때 서버에서 지하철역을 검색해 결과를 후보로 보여 준다.
 * 검색 결과에서 고른 장소는 후보 목록에 남고, 검색어를 지우면 고른 후보 목록으로 돌아간다.
 * 그 밖의 경우(참여자 화면 등)에는 넘겨 받은 후보 안에서만 걸러 보여 준다.
 *
 * [appointmentId]와 역할에 맞는 저장소가 있으면 저장 버튼이 서버로 이어진다.
 * - 방장: 선택한 장소를 약속의 후보 장소로 저장한다. 이미 있던 장소는 ID만 보내 유지하고, 검색에서 고른 장소는 새로 보낸다.
 * - 참여자: 서버가 희망 장소를 1개만 받으므로 하나만 고를 수 있고, 장소를 누르면 선택이 바뀐다.
 *   화면을 열면 내가 제출한 장소와 장소별 투표 현황을 불러온다.
 * 저장소가 없으면 서버로 보내지 않고 안내만 보낸다.
 *
 * @param maxSelection 한 번에 고를 수 있는 장소 수
 * @param searchDebounceMillis 검색어 입력이 멈춘 뒤 검색을 시작하기까지 기다리는 시간
 * @param texts 투표 수와 선택 현황 카드 문구. 없으면 서버 투표 현황을 화면에 보여 주지 않는다.
 * @param onChanged 서버가 저장·제출을 받아 줬을 때 부른다. 열려 있는 약속방이 약속 상세를 다시 불러오는 데 쓴다.
 */
class PlaceVoteViewModel(
    private val template: PlaceVoteUiState,
    private val maxSelection: Int = DEFAULT_MAX_SELECTION,
    private val searchRepository: PlaceSearchRepository? = null,
    private val searchDebounceMillis: Long = DEFAULT_SEARCH_DEBOUNCE_MILLIS,
    private val appointmentId: Long? = null,
    private val manageRepository: AppointmentManageRepository? = null,
    private val scheduleRepository: ScheduleRepository? = null,
    private val myMemberIdProvider: MyMemberIdProvider? = null,
    private val texts: PlaceVoteTexts? = null,
    private val onChanged: () -> Unit = AppointmentRefreshSignal::notifyChanged
) : ViewModel() {

    private val isHost = template.role == PlaceVoteRole.HOST
    private val usesServerSearch = searchRepository != null && isHost
    private val savesToServer = appointmentId != null &&
        (if (isHost) manageRepository != null else scheduleRepository != null)

    /** 서버가 희망 장소를 1개만 받는 참여자 화면에서는 하나만 고른다. */
    private val singleSelect = !isHost && savesToServer

    private var searchResults: List<PlaceCandidateUiModel> = emptyList()
    private var searchedById: Map<String, SearchedPlace> = emptyMap()
    private val pickedPlaces = mutableMapOf<String, SearchedPlace>()
    private var searchJob: Job? = null

    private var allCandidates: List<PlaceCandidateUiModel> = template.candidates
    private var selectedIds: Set<String> = template.selectedCandidateIds
        .filter { id -> allCandidates.any { it.id == id } }
        .take(maxSelection)
        .toSet()
    private var query: String = template.searchQuery

    private var status: ScheduleStatus? = null
    private var votes: Map<Long, LocationVote> = emptyMap()
    private var myMemberId: Long? = null
    private var isSubmitting = false
    private var pendingRetry: (() -> Unit)? = null

    private val _uiState = MutableStateFlow(buildState())
    val uiState: StateFlow<PlaceVoteUiState> = _uiState.asStateFlow()

    private val _notice = MutableStateFlow<PlaceVoteNotice?>(null)
    val notice: StateFlow<PlaceVoteNotice?> = _notice.asStateFlow()

    /** 선택한 장소 요약 카드를 보여 줄 화면인지. */
    val showsSelectedSummary: Boolean = template.selectedSummaryTitle != null

    init {
        if (singleSelect) loadVotes()
    }

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
        searchedById = emptyMap()
        if (keyword.isEmpty()) return
        val repository = searchRepository ?: return
        searchJob = viewModelScope.launch {
            delay(searchDebounceMillis)
            when (val result = repository.search(keyword, PlaceSearchType.STATION)) {
                is PlaceSearchResult.Success -> {
                    searchedById = result.value.places.associateBy { it.kakaoPlaceId }
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
     * 하나만 고르는 화면에서는 새로 누른 장소로 선택을 바꾼다. 후보에 없는 id는 무시한다.
     */
    fun onSelectionChange(newSelection: Set<String>) {
        val known = allCandidates + searchResults
        var valid = newSelection.filter { id -> known.any { it.id == id } }.toSet()
        if (singleSelect && valid.size > 1) {
            val pressed = (valid - selectedIds).take(1).toSet()
            valid = pressed.ifEmpty { valid.take(1).toSet() }
        }
        if (valid.size > maxSelection) {
            _notice.value = PlaceVoteNotice.MAX_SELECTION_REACHED
            return
        }
        // 검색 결과에서 고른 장소는 검색어를 지워도 남도록 후보 목록에 넣는다.
        val added = searchResults.filter { it.id in valid && allCandidates.none { c -> c.id == it.id } }
        if (added.isNotEmpty()) {
            allCandidates = allCandidates + added
            added.forEach { candidate ->
                searchedById[candidate.id]?.let { pickedPlaces[candidate.id] = it }
            }
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

    /**
     * 저장 버튼. 서버 저장소가 있으면 방장은 후보 장소를 저장하고 참여자는 고른 장소를 제출한다.
     * 저장소가 없으면 서버로 보내지 않고 안내만 보낸다. 선택값은 어느 경우에도 유지한다.
     */
    fun onSubmit() {
        if (selectedIds.isEmpty() || isSubmitting) return
        if (!savesToServer) {
            _notice.value = PlaceVoteNotice.SUBMIT_API_NOT_CONNECTED
            return
        }
        if (isHost) saveCandidates() else submitChoice()
    }

    private fun saveCandidates() {
        val repository = manageRepository ?: return
        val id = appointmentId ?: return
        val keep = mutableListOf<Long>()
        val added = mutableListOf<NewCandidateLocation>()
        for (candidate in allCandidates.filter { it.id in selectedIds }) {
            val serverId = serverLocationId(candidate.id)
            if (serverId != null) {
                keep += serverId
                continue
            }
            val place = pickedPlaces[candidate.id]
            val latitude = place?.latitude
            val longitude = place?.longitude
            if (place == null || latitude == null || longitude == null) {
                _notice.value = PlaceVoteNotice.ACTION_INVALID_PLACE
                return
            }
            added += NewCandidateLocation(
                placeName = place.name,
                address = place.roadAddress ?: place.jibunAddress,
                lineNames = place.lineNames,
                latitude = latitude,
                longitude = longitude
            )
        }
        val change = CandidateLocationChange(keepLocationIds = keep, newLocations = added)
        isSubmitting = true
        publish()
        viewModelScope.launch {
            val result = repository.updateCandidateLocations(id, change)
            isSubmitting = false
            when (result) {
                is AppointmentManageResult.Success -> {
                    onChanged()
                    _notice.value = PlaceVoteNotice.SAVED
                }
                AppointmentManageResult.LoginRequired -> requestLogin { saveCandidates() }
                AppointmentManageResult.NotHost -> _notice.value = PlaceVoteNotice.ACTION_NOT_ALLOWED
                is AppointmentManageResult.Rejected -> _notice.value = when (result.reason) {
                    AppointmentManageRejectReason.APPOINTMENT_CANCELED -> PlaceVoteNotice.ACTION_CLOSED
                    AppointmentManageRejectReason.INVALID_LOCATIONS -> PlaceVoteNotice.ACTION_INVALID_PLACE
                    else -> PlaceVoteNotice.ACTION_FAILED
                }
                is AppointmentManageResult.Failure -> _notice.value = failureNotice(result.retryable)
            }
            publish()
        }
    }

    private fun submitChoice() {
        val repository = scheduleRepository ?: return
        val id = appointmentId ?: return
        val locationId = selectedIds.singleOrNull()?.let { serverLocationId(it) }
        if (locationId == null) {
            _notice.value = PlaceVoteNotice.ACTION_INVALID_PLACE
            return
        }
        isSubmitting = true
        publish()
        viewModelScope.launch {
            val result = repository.submitSchedule(id, ScheduleSubmission(preferredLocationId = locationId))
            isSubmitting = false
            when (result) {
                is ScheduleResult.Success -> {
                    onChanged()
                    _notice.value = PlaceVoteNotice.SUBMITTED
                    loadVotes()
                }
                ScheduleResult.LoginRequired -> requestLogin { submitChoice() }
                ScheduleResult.NotParticipant -> _notice.value = PlaceVoteNotice.ACTION_NOT_ALLOWED
                is ScheduleResult.Rejected -> _notice.value = when (result.reason) {
                    ScheduleRejectReason.APPOINTMENT_CANCELED,
                    ScheduleRejectReason.DEADLINE_PASSED -> PlaceVoteNotice.ACTION_CLOSED
                    ScheduleRejectReason.INVALID_LOCATION -> PlaceVoteNotice.ACTION_INVALID_PLACE
                    else -> PlaceVoteNotice.ACTION_FAILED
                }
                is ScheduleResult.Failure -> _notice.value = failureNotice(result.retryable)
            }
            publish()
        }
    }

    /** 내가 제출한 장소와 장소별 투표 현황을 불러온다. 후보는 먼저 보여 주고, 값이 오면 채운다. */
    private fun loadVotes() {
        val repository = scheduleRepository ?: return
        val id = appointmentId ?: return
        viewModelScope.launch {
            myMemberId = myMemberId ?: myMemberIdProvider?.myMemberId()
            when (val mine = repository.getMySchedule(id)) {
                is ScheduleResult.Success -> mine.value.preferredLocationId?.let { applyMyChoice(it) }
                ScheduleResult.LoginRequired -> {
                    requestLogin { loadVotes() }
                    return@launch
                }
                else -> Unit
            }
            when (val result = repository.getScheduleStatus(id)) {
                is ScheduleResult.Success -> applyStatus(result.value)
                ScheduleResult.LoginRequired -> requestLogin { loadVotes() }
                else -> _notice.value = PlaceVoteNotice.VOTES_LOAD_FAILED
            }
        }
    }

    /** 사용자가 아직 고르지 않았을 때만 내가 제출했던 장소를 선택으로 채운다. */
    private fun applyMyChoice(locationId: Long) {
        val candidateId = "$PLACE_LOCATION_ID_PREFIX$locationId"
        if (selectedIds.isEmpty() && allCandidates.any { it.id == candidateId }) {
            selectedIds = setOf(candidateId)
            publish()
        }
    }

    private fun applyStatus(value: ScheduleStatus) {
        status = value
        votes = value.locationVotes.associateBy { it.locationId }
        publish()
    }

    private fun requestLogin(retry: () -> Unit) {
        pendingRetry = retry
        _notice.value = PlaceVoteNotice.ACTION_LOGIN_REQUIRED
    }

    /** 로그인에 성공하면 하던 일(저장·제출·현황 조회)을 다시 한다. */
    fun retryAfterLogin() {
        val retry = pendingRetry ?: return
        pendingRetry = null
        retry()
    }

    private fun failureNotice(retryable: Boolean) =
        if (retryable) PlaceVoteNotice.ACTION_FAILED_RETRYABLE else PlaceVoteNotice.ACTION_FAILED

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
            candidates = visible.map { it.withVotes() },
            selectedCandidateIds = selectedIds,
            submitEnabled = selectedIds.isNotEmpty() && !isSubmitting,
            selectedPlaceNames = allCandidates.filter { it.id in selectedIds }.map { it.name },
            progress = progressState() ?: template.progress
        )
    }

    /** 서버 투표 현황이 있으면 장소 카드에 "3/3명" 같은 투표 수를 붙인다. */
    private fun PlaceCandidateUiModel.withVotes(): PlaceCandidateUiModel {
        val formatter = texts ?: return this
        val total = status?.totalCount ?: return this
        val vote = serverLocationId(id)?.let { votes[it] } ?: return this
        return copy(
            voteLabel = formatter.voteLabel(vote.voteCount, total),
            isVoteComplete = total > 0 && vote.voteCount == total
        )
    }

    /** 장소를 고른 멤버만 아바타로 보여 주고, 아직 고르지 않은 인원을 안내한다. */
    private fun progressState(): PlaceVoteProgressUiState? {
        val formatter = texts ?: return null
        val current = status ?: return null
        val chosen = current.members.filter { it.preferredLocationId != null }
        val remaining = (current.totalCount - chosen.size).coerceAtLeast(0)
        return PlaceVoteProgressUiState(
            members = chosen.map {
                PlaceVoteProgressMemberUiModel(initial = it.nickname.take(1), isMe = it.memberId == myMemberId)
            },
            title = formatter.progressTitle(chosen.size),
            subtitle = if (remaining > 0) formatter.progressSubtitle(remaining) else null,
            statusLabel = if (remaining > 0) formatter.progressSelecting else formatter.progressDone
        )
    }

    companion object {
        const val DEFAULT_MAX_SELECTION = 5
        const val DEFAULT_SEARCH_DEBOUNCE_MILLIS = 300L
    }
}
