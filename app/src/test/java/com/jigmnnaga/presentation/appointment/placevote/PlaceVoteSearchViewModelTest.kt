package com.jigmnnaga.presentation.appointment.placevote

import com.jigmnnaga.domain.model.PlaceSearchPage
import com.jigmnnaga.domain.model.PlaceSearchResult
import com.jigmnnaga.domain.model.PlaceSearchType
import com.jigmnnaga.domain.model.SearchedPlace
import com.jigmnnaga.domain.repository.PlaceSearchRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaceVoteSearchViewModelTest {

    private class FakeSearchRepository : PlaceSearchRepository {
        var result: PlaceSearchResult<PlaceSearchPage> = PlaceSearchResult.Success(PlaceSearchPage(emptyList(), true))
        val queries = mutableListOf<Pair<String, PlaceSearchType>>()

        override suspend fun search(
            query: String,
            type: PlaceSearchType,
            page: Int,
            size: Int
        ): PlaceSearchResult<PlaceSearchPage> {
            queries += query to type
            return result
        }
    }

    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeSearchRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun place(id: String, name: String, lines: String? = null, address: String? = null) =
        SearchedPlace(kakaoPlaceId = id, name = name, lineNames = lines, roadAddress = address)

    private fun state(role: PlaceVoteRole = PlaceVoteRole.HOST) = PlaceVoteUiState(
        role = role,
        searchQuery = "",
        candidates = emptyList(),
        selectedCandidateIds = emptySet(),
        title = "일정 조율",
        question = "어디서 만날까요?",
        submitLabel = "저장",
        showSearchInput = true
    )

    private fun vm(role: PlaceVoteRole = PlaceVoteRole.HOST, maxSelection: Int = 5) =
        PlaceVoteViewModel(state(role), maxSelection, repository, searchDebounceMillis = 300L)

    private fun TestScope.finishSearch() {
        advanceTimeBy(301)
        runCurrent()
    }

    @Test
    fun `방장이 검색어를 입력하면 잠시 뒤 지하철역을 검색해 결과를 후보로 보여 준다`() = kotlinx.coroutines.test.runTest(dispatcher) {
        repository.result = PlaceSearchResult.Success(
            PlaceSearchPage(listOf(place("1", "강남역", "2호선, 신분당선", "강남대로 396")), isEnd = true)
        )
        val vm = vm()

        vm.onSearchQueryChange("강남")
        assertTrue(repository.queries.isEmpty())
        finishSearch()

        assertEquals(listOf("강남" to PlaceSearchType.STATION), repository.queries)
        val candidate = vm.uiState.value.candidates.single()
        assertEquals("1", candidate.id)
        assertEquals("강남역 · 2호선, 신분당선", candidate.name)
        assertEquals("강남대로 396", candidate.address)
    }

    @Test
    fun `입력이 이어지면 마지막 검색어로만 검색한다`() = kotlinx.coroutines.test.runTest(dispatcher) {
        val vm = vm()
        vm.onSearchQueryChange("강")
        advanceTimeBy(100)
        vm.onSearchQueryChange("강남")
        finishSearch()
        assertEquals(listOf("강남" to PlaceSearchType.STATION), repository.queries)
    }

    @Test
    fun `검색 결과에서 고른 장소는 검색어를 지워도 후보에 남는다`() = kotlinx.coroutines.test.runTest(dispatcher) {
        repository.result = PlaceSearchResult.Success(
            PlaceSearchPage(listOf(place("1", "강남역"), place("2", "교대역")), isEnd = true)
        )
        val vm = vm()
        vm.onSearchQueryChange("역")
        finishSearch()

        vm.onSelectionChange(setOf("2"))
        assertEquals(setOf("2"), vm.uiState.value.selectedCandidateIds)

        vm.onSearchQueryChange("")
        assertEquals(listOf("2"), vm.uiState.value.candidates.map { it.id })
        assertEquals(setOf("2"), vm.uiState.value.selectedCandidateIds)
    }

    @Test
    fun `검색 결과 선택도 최대 개수를 넘기지 못한다`() = kotlinx.coroutines.test.runTest(dispatcher) {
        repository.result = PlaceSearchResult.Success(
            PlaceSearchPage(listOf(place("1", "가"), place("2", "나"), place("3", "다")), isEnd = true)
        )
        val vm = vm(maxSelection = 2)
        vm.onSearchQueryChange("역")
        finishSearch()

        vm.onSelectionChange(setOf("1", "2", "3"))

        assertEquals(PlaceVoteNotice.MAX_SELECTION_REACHED, vm.notice.value)
        assertTrue(vm.uiState.value.selectedCandidateIds.isEmpty())
    }

    @Test
    fun `검색어를 지우면 검색하지 않고 고른 후보 목록으로 돌아간다`() = kotlinx.coroutines.test.runTest(dispatcher) {
        val vm = vm()
        vm.onSearchQueryChange("강남")
        vm.onSearchQueryChange("  ")
        finishSearch()
        assertTrue(repository.queries.isEmpty())
        assertTrue(vm.uiState.value.candidates.isEmpty())
    }

    @Test
    fun `참여자 화면에서는 서버를 검색하지 않는다`() = kotlinx.coroutines.test.runTest(dispatcher) {
        val vm = vm(role = PlaceVoteRole.PARTICIPANT)
        vm.onSearchQueryChange("강남")
        finishSearch()
        assertTrue(repository.queries.isEmpty())
    }

    @Test
    fun `로그인이 필요하면 알림을 보내고 로그인 뒤 같은 검색어로 다시 검색한다`() = kotlinx.coroutines.test.runTest(dispatcher) {
        repository.result = PlaceSearchResult.LoginRequired
        val vm = vm()
        vm.onSearchQueryChange("강남")
        finishSearch()
        assertEquals(PlaceVoteNotice.SEARCH_LOGIN_REQUIRED, vm.notice.value)

        repository.result = PlaceSearchResult.Success(PlaceSearchPage(listOf(place("1", "강남역")), isEnd = true))
        vm.clearNotice()
        vm.retrySearch()
        finishSearch()

        assertEquals(2, repository.queries.size)
        assertEquals(listOf("1"), vm.uiState.value.candidates.map { it.id })
    }

    @Test
    fun `검색에 실패하면 다시 시도할 수 있는지에 따라 알림을 나눈다`() = kotlinx.coroutines.test.runTest(dispatcher) {
        val vm = vm()
        repository.result = PlaceSearchResult.Failure(retryable = true)
        vm.onSearchQueryChange("강남")
        finishSearch()
        assertEquals(PlaceVoteNotice.SEARCH_FAILED_RETRYABLE, vm.notice.value)

        vm.clearNotice()
        repository.result = PlaceSearchResult.Failure(retryable = false)
        vm.onSearchQueryChange("강남역")
        finishSearch()
        assertEquals(PlaceVoteNotice.SEARCH_FAILED, vm.notice.value)
    }
}
