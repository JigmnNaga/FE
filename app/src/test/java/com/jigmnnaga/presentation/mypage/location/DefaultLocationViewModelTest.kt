package com.jigmnnaga.presentation.mypage.location

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultLocationViewModelTest {
    private class FakeLocations : MemberLocationRepository {
        var listResult: MemberLocationResult<List<MemberLocation>> = MemberLocationResult.Success(emptyList())
        var saveResult: MemberLocationResult<MemberLocation>? = null
        var deleteResult: MemberLocationResult<Unit> = MemberLocationResult.Success(Unit)
        var listCalls = 0
        val created = mutableListOf<MemberLocationInput>()
        val updated = mutableListOf<Pair<Long, MemberLocationInput>>()
        val deleted = mutableListOf<Long>()
        private var nextId = 100L

        override suspend fun getLocations(): MemberLocationResult<List<MemberLocation>> {
            listCalls++
            return listResult
        }

        override suspend fun createLocation(input: MemberLocationInput): MemberLocationResult<MemberLocation> {
            created += input
            return saveResult ?: MemberLocationResult.Success(input.toLocation(nextId++))
        }

        override suspend fun updateLocation(
            locationId: Long,
            input: MemberLocationInput
        ): MemberLocationResult<MemberLocation> {
            updated += locationId to input
            return saveResult ?: MemberLocationResult.Success(input.toLocation(locationId))
        }

        override suspend fun deleteLocation(locationId: Long): MemberLocationResult<Unit> {
            deleted += locationId
            return deleteResult
        }

        private fun MemberLocationInput.toLocation(id: Long) = MemberLocation(id, name, address, latitude, longitude)
    }

    private class FakeSearch : PlaceSearchRepository {
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
    private val locations = FakeLocations()
    private val search = FakeSearch()

    private val home = MemberLocation(1, "집", "서울 강남구 테헤란로 123", 37.5, 127.0)
    private val office = MemberLocation(2, "회사", "서울 서초구 서초대로 45", 37.4, 127.1)
    private val gangnam = SearchedPlace(
        "k1", "강남역", lineNames = "2호선", roadAddress = "서울 강남구 강남대로 396",
        latitude = 37.4979, longitude = 127.0276
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun vm() = DefaultLocationViewModel(locations, search, searchDebounceMillis = 300L)

    private fun TestScope.finishSearch() {
        advanceTimeBy(301)
        runCurrent()
    }

    @Test
    fun `화면을 열면 기본 위치를 불러와 목록에 보여 준다`() = runTest(dispatcher) {
        locations.listResult = MemberLocationResult.Success(listOf(home, office))
        val vm = vm()
        assertEquals(DefaultLocationLoadStatus.LOADING, vm.uiState.value.loadStatus)
        runCurrent()

        val state = vm.uiState.value
        assertEquals(DefaultLocationLoadStatus.LOADED, state.loadStatus)
        assertEquals(listOf("집", "회사"), state.locations.map { it.name })
        assertEquals(2, state.count)
        assertTrue(state.canAdd)
    }

    @Test
    fun `불러오지 못하면 실패로 표시하고 다시 시도하면 목록을 가져온다`() = runTest(dispatcher) {
        locations.listResult = MemberLocationResult.Failure(retryable = true)
        val vm = vm()
        runCurrent()
        assertEquals(DefaultLocationLoadStatus.FAILED, vm.uiState.value.loadStatus)
        assertFalse(vm.uiState.value.canAdd)

        locations.listResult = MemberLocationResult.Success(listOf(home))
        vm.load()
        runCurrent()
        assertEquals(DefaultLocationLoadStatus.LOADED, vm.uiState.value.loadStatus)
        assertEquals(2, locations.listCalls)
    }

    @Test
    fun `로그인이 필요하다는 응답이면 한 번만 알린다`() = runTest(dispatcher) {
        locations.listResult = MemberLocationResult.LoginRequired
        val vm = vm()
        runCurrent()
        assertTrue(vm.uiState.value.loginRequired)

        vm.consumeLoginRequired()
        assertFalse(vm.uiState.value.loginRequired)
    }

    @Test
    fun `3개를 모두 채우면 추가 화면을 열지 않는다`() = runTest(dispatcher) {
        locations.listResult = MemberLocationResult.Success(
            listOf(home, office, MemberLocation(3, "학교", null, 37.3, 127.2))
        )
        val vm = vm()
        runCurrent()
        assertFalse(vm.uiState.value.canAdd)

        vm.onAddClick()
        assertNull(vm.uiState.value.editor)
    }

    @Test
    fun `목록을 불러오기 전에는 추가 화면을 열지 않는다`() = runTest(dispatcher) {
        val vm = vm()
        vm.onAddClick()
        assertNull(vm.uiState.value.editor)
    }

    @Test
    fun `이름과 위치를 모두 고르기 전에는 저장할 수 없다`() = runTest(dispatcher) {
        val vm = vm()
        runCurrent()
        vm.onAddClick()
        assertFalse(vm.uiState.value.editor!!.canSave)

        vm.onNameChange("집")
        assertFalse(vm.uiState.value.editor!!.canSave)

        vm.onNameChange("   ")
        search.result = PlaceSearchResult.Success(PlaceSearchPage(listOf(gangnam), true))
        vm.onQueryChange("강남")
        finishSearch()
        vm.onSelectPlace(vm.uiState.value.editor!!.searchResults.single())
        assertFalse(vm.uiState.value.editor!!.canSave)

        vm.onNameChange("집")
        assertTrue(vm.uiState.value.editor!!.canSave)
    }

    @Test
    fun `이름은 최대 글자 수까지만 입력된다`() = runTest(dispatcher) {
        val vm = vm()
        runCurrent()
        vm.onAddClick()
        vm.onNameChange("가".repeat(MAX_DEFAULT_LOCATION_NAME_LENGTH + 5))
        assertEquals(MAX_DEFAULT_LOCATION_NAME_LENGTH, vm.uiState.value.editor!!.name.length)
    }

    @Test
    fun `검색어를 입력하면 잠시 뒤 장소를 검색해 좌표가 있는 결과만 보여 준다`() = runTest(dispatcher) {
        search.result = PlaceSearchResult.Success(
            PlaceSearchPage(listOf(gangnam, SearchedPlace("k2", "좌표 없는 곳")), true)
        )
        val vm = vm()
        runCurrent()
        vm.onAddClick()

        vm.onQueryChange("강남")
        assertEquals(DefaultLocationSearchStatus.LOADING, vm.uiState.value.editor!!.searchStatus)
        assertTrue(search.queries.isEmpty())
        finishSearch()

        val editor = vm.uiState.value.editor!!
        assertEquals(listOf("강남" to PlaceSearchType.PLACE), search.queries)
        assertEquals(listOf("search-k1"), editor.searchResults.map { it.id })
        assertEquals("2호선·서울 강남구 강남대로 396", editor.searchResults.single().detail)
        assertEquals(DefaultLocationSearchStatus.IDLE, editor.searchStatus)
    }

    @Test
    fun `검색 결과가 없거나 실패하면 상태로 알린다`() = runTest(dispatcher) {
        val vm = vm()
        runCurrent()
        vm.onAddClick()

        vm.onQueryChange("없는곳")
        finishSearch()
        assertEquals(DefaultLocationSearchStatus.EMPTY, vm.uiState.value.editor!!.searchStatus)

        search.result = PlaceSearchResult.Failure(retryable = true)
        vm.onQueryChange("강남")
        finishSearch()
        assertEquals(DefaultLocationSearchStatus.FAILED, vm.uiState.value.editor!!.searchStatus)
    }

    @Test
    fun `검색어를 지우면 검색하지 않고 결과를 비운다`() = runTest(dispatcher) {
        search.result = PlaceSearchResult.Success(PlaceSearchPage(listOf(gangnam), true))
        val vm = vm()
        runCurrent()
        vm.onAddClick()
        vm.onQueryChange("강남")
        finishSearch()

        vm.onQueryChange("  ")
        finishSearch()

        val editor = vm.uiState.value.editor!!
        assertTrue(editor.searchResults.isEmpty())
        assertEquals(DefaultLocationSearchStatus.IDLE, editor.searchStatus)
        assertEquals(1, search.queries.size)
    }

    @Test
    fun `새 위치를 저장하면 서버에 등록하고 목록 맨 뒤에 붙인다`() = runTest(dispatcher) {
        locations.listResult = MemberLocationResult.Success(listOf(home))
        search.result = PlaceSearchResult.Success(PlaceSearchPage(listOf(gangnam), true))
        val vm = vm()
        runCurrent()
        vm.onAddClick()
        vm.onNameChange(" 학교 ")
        vm.onQueryChange("강남")
        finishSearch()
        vm.onSelectPlace(vm.uiState.value.editor!!.searchResults.single())

        vm.onSave()
        runCurrent()

        assertEquals(
            listOf(MemberLocationInput("학교", "서울 강남구 강남대로 396", 37.4979, 127.0276)),
            locations.created
        )
        val state = vm.uiState.value
        assertNull(state.editor)
        assertEquals(listOf("집", "학교"), state.locations.map { it.name })
        assertEquals(2, state.count)
    }

    @Test
    fun `수정을 열면 지금 위치가 선택돼 있어서 이름만 바꿔도 저장할 수 있다`() = runTest(dispatcher) {
        locations.listResult = MemberLocationResult.Success(listOf(home, office))
        val vm = vm()
        runCurrent()

        vm.onEditClick(2)
        val editor = vm.uiState.value.editor!!
        assertEquals(2L, editor.editingId)
        assertEquals("회사", editor.name)
        assertEquals("서울 서초구 서초대로 45", editor.selected?.title)
        assertTrue(editor.canSave)

        vm.onNameChange("직장")
        vm.onSave()
        runCurrent()

        assertEquals(
            listOf(2L to MemberLocationInput("직장", "서울 서초구 서초대로 45", 37.4, 127.1)),
            locations.updated
        )
        assertTrue(locations.created.isEmpty())
        val state = vm.uiState.value
        assertNull(state.editor)
        assertEquals(listOf("집", "직장"), state.locations.map { it.name })
    }

    @Test
    fun `저장에 실패하면 화면을 닫지 않고 실패로 표시한다`() = runTest(dispatcher) {
        locations.saveResult = MemberLocationResult.Failure(retryable = true)
        search.result = PlaceSearchResult.Success(PlaceSearchPage(listOf(gangnam), true))
        val vm = vm()
        runCurrent()
        vm.onAddClick()
        vm.onNameChange("집")
        vm.onQueryChange("강남")
        finishSearch()
        vm.onSelectPlace(vm.uiState.value.editor!!.searchResults.single())

        vm.onSave()
        runCurrent()

        val editor = vm.uiState.value.editor!!
        assertEquals(DefaultLocationActionStatus.FAILED, editor.saveStatus)
        assertTrue(editor.canSave)
        assertEquals(0, vm.uiState.value.count)
    }

    @Test
    fun `수정하려던 위치가 이미 없으면 편집을 닫고 목록을 다시 불러온다`() = runTest(dispatcher) {
        locations.listResult = MemberLocationResult.Success(listOf(home))
        val vm = vm()
        runCurrent()
        vm.onEditClick(1)

        locations.saveResult = MemberLocationResult.Rejected(MemberLocationRejectReason.NOT_FOUND)
        locations.listResult = MemberLocationResult.Success(emptyList())
        vm.onSave()
        runCurrent()

        assertNull(vm.uiState.value.editor)
        assertEquals(2, locations.listCalls)
        assertEquals(0, vm.uiState.value.count)
    }

    @Test
    fun `저장하는 중에 로그인이 필요하다는 응답이면 로그인을 알린다`() = runTest(dispatcher) {
        locations.listResult = MemberLocationResult.Success(listOf(home))
        val vm = vm()
        runCurrent()
        vm.onEditClick(1)

        locations.saveResult = MemberLocationResult.LoginRequired
        vm.onSave()
        runCurrent()

        assertTrue(vm.uiState.value.loginRequired)
        assertNotNull(vm.uiState.value.editor)
    }

    @Test
    fun `삭제를 누르면 바로 지우지 않고 확인을 거친다`() = runTest(dispatcher) {
        locations.listResult = MemberLocationResult.Success(listOf(home, office))
        val vm = vm()
        runCurrent()

        vm.onDeleteClick(1)
        assertEquals("집", vm.uiState.value.deleteTarget?.name)
        assertTrue(locations.deleted.isEmpty())

        vm.onDismissDelete()
        assertNull(vm.uiState.value.deleteTarget)
        assertTrue(locations.deleted.isEmpty())
        assertEquals(2, vm.uiState.value.count)
    }

    @Test
    fun `삭제를 확인하면 서버에서 지우고 목록에서 뺀다`() = runTest(dispatcher) {
        locations.listResult = MemberLocationResult.Success(listOf(home, office))
        val vm = vm()
        runCurrent()
        vm.onDeleteClick(1)

        vm.onConfirmDelete()
        runCurrent()

        assertEquals(listOf(1L), locations.deleted)
        val state = vm.uiState.value
        assertNull(state.deleteTarget)
        assertEquals(listOf("회사"), state.locations.map { it.name })
        assertTrue(state.canAdd)
    }

    @Test
    fun `이미 지워진 위치를 삭제하면 지운 것으로 본다`() = runTest(dispatcher) {
        locations.listResult = MemberLocationResult.Success(listOf(home))
        locations.deleteResult = MemberLocationResult.Rejected(MemberLocationRejectReason.NOT_FOUND)
        val vm = vm()
        runCurrent()
        vm.onDeleteClick(1)

        vm.onConfirmDelete()
        runCurrent()

        assertEquals(0, vm.uiState.value.count)
        assertEquals(DefaultLocationActionStatus.IDLE, vm.uiState.value.deleteStatus)
    }

    @Test
    fun `삭제에 실패하면 목록을 그대로 두고 실패로 표시한다`() = runTest(dispatcher) {
        locations.listResult = MemberLocationResult.Success(listOf(home))
        locations.deleteResult = MemberLocationResult.Failure(retryable = true)
        val vm = vm()
        runCurrent()
        vm.onDeleteClick(1)

        vm.onConfirmDelete()
        runCurrent()

        val state = vm.uiState.value
        assertEquals(1, state.count)
        assertNull(state.deleteTarget)
        assertEquals(DefaultLocationActionStatus.FAILED, state.deleteStatus)

        vm.onDeleteClick(1)
        assertEquals(DefaultLocationActionStatus.IDLE, vm.uiState.value.deleteStatus)
    }
}
