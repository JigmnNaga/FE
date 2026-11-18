package com.jigmnnaga.presentation.appointment.route

import com.jigmnnaga.domain.model.MemberLocation
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RouteStartViewModelTest {
    private class FakeMemberLocations : MemberLocationRepository {
        var result: MemberLocationResult<List<MemberLocation>> = MemberLocationResult.Success(emptyList())
        var calls = 0
        override suspend fun getLocations(): MemberLocationResult<List<MemberLocation>> {
            calls++
            return result
        }
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
    private val locations = FakeMemberLocations()
    private val search = FakeSearch()
    private var coordinates: RouteCoordinates? = RouteCoordinates(37.4979, 127.0276)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun vm() = RouteViewModel(
        initialState = RouteSampleData.selecting,
        memberLocationRepository = locations,
        placeSearchRepository = search,
        currentLocationProvider = { coordinates },
        currentLocationName = "현재 위치",
        searchDebounceMillis = 300L
    )

    private fun TestScope.finishSearch() {
        advanceTimeBy(301)
        runCurrent()
    }

    @Test
    fun `선택 화면을 열면 내 기본 위치를 불러와 보여 준다`() = runTest(dispatcher) {
        locations.result = MemberLocationResult.Success(
            listOf(
                MemberLocation(1, "집", "서울 강남구 테헤란로 123", 37.5, 127.0),
                MemberLocation(2, "회사", null, 37.4, 127.1)
            )
        )
        val vm = vm()
        vm.onOpenStartPicker()
        runCurrent()

        val picker = vm.uiState.value.startPicker
        assertTrue(picker.isOpen)
        assertEquals(listOf("basic-1", "basic-2"), picker.basicLocations.map { it.id })
        assertEquals("서울 강남구 테헤란로 123", picker.basicLocations.first().detail)
        assertFalse(picker.basicLocationsFailed)
    }

    @Test
    fun `기본 위치를 불러오지 못하면 실패로 표시한다`() = runTest(dispatcher) {
        locations.result = MemberLocationResult.Failure(retryable = true)
        val vm = vm()
        vm.onOpenStartPicker()
        runCurrent()
        assertTrue(vm.uiState.value.startPicker.basicLocationsFailed)
    }

    @Test
    fun `검색어를 입력하면 잠시 뒤 장소를 검색해 좌표가 있는 결과만 보여 준다`() = runTest(dispatcher) {
        search.result = PlaceSearchResult.Success(
            PlaceSearchPage(
                places = listOf(
                    SearchedPlace("1", "강남역", lineNames = "2호선", roadAddress = "강남대로 396", latitude = 37.49, longitude = 127.02),
                    SearchedPlace("2", "좌표 없는 곳")
                ),
                isEnd = true
            )
        )
        val vm = vm()
        vm.onOpenStartPicker()
        vm.onStartQueryChange("강남역")
        assertEquals(RouteSearchStatus.LOADING, vm.uiState.value.startPicker.searchStatus)
        finishSearch()

        val picker = vm.uiState.value.startPicker
        assertEquals(listOf("search-1"), picker.searchResults.map { it.id })
        assertEquals("2호선·강남대로 396", picker.searchResults.first().detail)
        assertEquals(listOf("강남역" to PlaceSearchType.PLACE), search.queries)
    }

    @Test
    fun `검색 결과가 없거나 실패하면 상태를 알려 준다`() = runTest(dispatcher) {
        val vm = vm()
        vm.onOpenStartPicker()
        vm.onStartQueryChange("없는곳")
        finishSearch()
        assertEquals(RouteSearchStatus.EMPTY, vm.uiState.value.startPicker.searchStatus)

        search.result = PlaceSearchResult.Failure(retryable = true)
        vm.onStartQueryChange("강남역")
        finishSearch()
        assertEquals(RouteSearchStatus.FAILED, vm.uiState.value.startPicker.searchStatus)
    }

    @Test
    fun `검색어를 지우면 검색하지 않고 결과를 비운다`() = runTest(dispatcher) {
        val vm = vm()
        vm.onOpenStartPicker()
        vm.onStartQueryChange("강남")
        vm.onStartQueryChange("")
        finishSearch()
        assertTrue(search.queries.isEmpty())
        assertEquals(RouteSearchStatus.IDLE, vm.uiState.value.startPicker.searchStatus)
    }

    @Test
    fun `현재 위치를 가져오면 그 위치가 선택되고 출발하기를 누를 수 있다`() = runTest(dispatcher) {
        val vm = vm()
        vm.onOpenStartPicker()
        vm.requestCurrentLocation()
        assertEquals(RouteCurrentLocationStatus.LOADING, vm.uiState.value.startPicker.currentStatus)
        runCurrent()

        val picker = vm.uiState.value.startPicker
        assertEquals("current", picker.selected?.id)
        assertEquals(37.4979, picker.current?.latitude ?: 0.0, 0.0001)
        assertTrue(picker.canConfirm)
    }

    @Test
    fun `현재 위치를 가져오지 못하면 실패로 표시하고 고르지 않는다`() = runTest(dispatcher) {
        coordinates = null
        val vm = vm()
        vm.onOpenStartPicker()
        vm.requestCurrentLocation()
        runCurrent()

        val picker = vm.uiState.value.startPicker
        assertEquals(RouteCurrentLocationStatus.FAILED, picker.currentStatus)
        assertNull(picker.selected)
    }

    @Test
    fun `위치 권한을 거부하면 안내 상태가 된다`() = runTest(dispatcher) {
        val vm = vm()
        vm.onOpenStartPicker()
        vm.onCurrentLocationPermissionDenied()
        assertEquals(RouteCurrentLocationStatus.PERMISSION_DENIED, vm.uiState.value.startPicker.currentStatus)
    }

    @Test
    fun `위치를 골라 출발하면 도착지 카드가 쓸 출발 위치가 정해진다`() = runTest(dispatcher) {
        locations.result = MemberLocationResult.Success(listOf(MemberLocation(1, "집", null, 37.5, 127.0)))
        val vm = vm()
        vm.onOpenStartPicker()
        runCurrent()
        vm.onSelectStartCandidate(vm.uiState.value.startPicker.basicLocations.first())
        vm.onConfirmStartLocation()

        assertEquals("집", vm.uiState.value.startLocation?.name)
        assertFalse(vm.uiState.value.startPicker.isOpen)
    }

    @Test
    fun `뒤로 가기는 열려 있는 선택 화면도 함께 닫는다`() = runTest(dispatcher) {
        val vm = vm()
        vm.onOpenStartPicker()
        assertTrue(vm.onBack())
        assertFalse(vm.uiState.value.startPicker.isOpen)
        assertEquals(RouteStage.OVERVIEW, vm.uiState.value.stage)
    }
}
