package com.jigmnnaga.presentation.appointment.hostconfirmation

import com.jigmnnaga.domain.model.AppointmentLocation
import com.jigmnnaga.domain.model.ConfirmDetailPlace
import com.jigmnnaga.domain.model.ConfirmationRejectReason
import com.jigmnnaga.domain.model.ConfirmationResult
import com.jigmnnaga.domain.model.PlaceSearchPage
import com.jigmnnaga.domain.model.PlaceSearchResult
import com.jigmnnaga.domain.model.PlaceSearchType
import com.jigmnnaga.domain.model.SearchedPlace
import com.jigmnnaga.domain.model.TimeCandidate
import com.jigmnnaga.domain.model.TimeCandidates
import com.jigmnnaga.domain.repository.ConfirmationRepository
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
class HostConfirmationServerViewModelTest {

    private class FakeConfirmation : ConfirmationRepository {
        var timeResult: ConfirmationResult<TimeCandidates> = ConfirmationResult.Success(
            TimeCandidates(
                totalMemberCount = 4,
                candidates = listOf(
                    TimeCandidate("2026-11-20", "19:00", "21:00", 3),
                    TimeCandidate("2026-11-21", "18:00", "20:00", 2)
                )
            )
        )
        var dateResult: ConfirmationResult<Unit> = ConfirmationResult.Success(Unit)
        var locationResult: ConfirmationResult<Unit> = ConfirmationResult.Success(Unit)
        var timeCalls = 0
        val dateCalls = mutableListOf<Triple<Long, String, String>>()
        val locationCalls = mutableListOf<Triple<Long, Long, ConfirmDetailPlace?>>()

        override suspend fun getTimeCandidates(appointmentId: Long): ConfirmationResult<TimeCandidates> {
            timeCalls++
            return timeResult
        }

        override suspend fun confirmDateTime(appointmentId: Long, date: String, time: String):
            ConfirmationResult<Unit> {
            dateCalls += Triple(appointmentId, date, time)
            return dateResult
        }

        override suspend fun confirmLocation(
            appointmentId: Long,
            locationId: Long,
            detailPlace: ConfirmDetailPlace?
        ): ConfirmationResult<Unit> {
            locationCalls += Triple(appointmentId, locationId, detailPlace)
            return locationResult
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
    private val confirmation = FakeConfirmation()
    private val search = FakeSearch()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val locations = listOf(
        AppointmentLocation(10L, "강남역", "서울 강남구", "2호선, 신분당선", 37.49, 127.02),
        AppointmentLocation(11L, "잠실역", "서울 송파구", "2호선", 37.51, 127.10)
    )

    private fun vm(isHost: Boolean = true) = HostConfirmationViewModel(
        initialState = HostConfirmationUiState(
            appointmentTitle = "저녁 약속",
            selectedDateTime = null,
            selectedPlace = null
        ),
        isHost = isHost,
        appointmentId = 7L,
        candidateLocations = locations,
        confirmationRepository = confirmation,
        placeSearchRepository = search,
        searchDebounceMillis = 300L
    )

    private fun TestScope.settle() {
        advanceTimeBy(301)
        runCurrent()
    }

    private fun HostConfirmationViewModel.chooseDateTimeAndPlace() {
        onSelectDateTime("2026-11-20|19:00")
        onSelectPlace("10")
    }

    @Test
    fun `후보 장소는 처음부터 선택지로 보이고 시간 후보는 불러오면 채워진다`() = runTest(dispatcher) {
        val vm = vm()
        assertEquals(listOf("10", "11"), vm.uiState.value.placeChoices.map { it.id })
        assertEquals("2호선, 신분당선 · 서울 강남구", vm.uiState.value.placeChoices[0].supportingText)

        vm.loadTimeCandidates()
        runCurrent()

        val choices = vm.uiState.value.dateTimeChoices
        assertEquals(listOf("2026-11-20|19:00", "2026-11-21|18:00"), choices.map { it.id })
        assertEquals("11월 20일 19:00~21:00", choices[0].title)
        assertEquals("3/4명 가능", choices[0].supportingText)
    }

    @Test
    fun `방장이 아니면 시간 후보를 불러오지 않는다`() = runTest(dispatcher) {
        vm(isHost = false).loadTimeCandidates()
        runCurrent()
        assertEquals(0, confirmation.timeCalls)
    }

    @Test
    fun `시간 후보를 불러올 때 로그인이 필요하면 알림을 보내고 로그인 뒤 다시 불러온다`() = runTest(dispatcher) {
        confirmation.timeResult = ConfirmationResult.LoginRequired
        val vm = vm()
        vm.loadTimeCandidates()
        runCurrent()
        assertEquals(HostConfirmationNotice.LOGIN_REQUIRED, vm.notice.value)

        confirmation.timeResult = ConfirmationResult.Success(
            TimeCandidates(2, listOf(TimeCandidate("2026-11-20", "19:00", "21:00", 2)))
        )
        vm.clearNotice()
        vm.loadTimeCandidates()
        runCurrent()
        assertEquals(1, vm.uiState.value.dateTimeChoices.size)
    }

    @Test
    fun `확정하면 날짜·시간과 장소를 차례로 보내고 완료 단계로 간다`() = runTest(dispatcher) {
        val vm = vm()
        vm.loadTimeCandidates()
        runCurrent()
        vm.chooseDateTimeAndPlace()

        vm.onConfirm()
        assertTrue(vm.uiState.value.isBusy)
        runCurrent()

        assertEquals(listOf(Triple(7L, "2026-11-20", "19:00")), confirmation.dateCalls)
        assertEquals(listOf(Triple(7L, 10L, null)), confirmation.locationCalls)
        assertEquals(HostConfirmationStep.COMPLETE, vm.uiState.value.step)
        assertFalse(vm.uiState.value.isBusy)
    }

    @Test
    fun `세부 위치를 고르면 장소 확정에 함께 보낸다`() = runTest(dispatcher) {
        search.result = PlaceSearchResult.Success(
            PlaceSearchPage(
                listOf(
                    SearchedPlace("k1", "스터디카페", null, "강남대로 390", null, 37.4979, 127.0276),
                    SearchedPlace("k2", "좌표 없는 곳", null, null, null, null, null)
                ),
                isEnd = true
            )
        )
        val vm = vm()
        vm.loadTimeCandidates()
        runCurrent()
        vm.chooseDateTimeAndPlace()

        vm.onOpenDetailLocation()
        vm.onDetailSearchQueryChange("스터디")
        settle()

        assertEquals(listOf("스터디" to PlaceSearchType.PLACE), search.queries)
        assertEquals(listOf("k1"), vm.uiState.value.detailLocationChoices.map { it.id })

        vm.onSelectDetailLocation("k1")
        vm.onConfirm()
        runCurrent()

        assertEquals(
            ConfirmDetailPlace("스터디카페", "강남대로 390", 37.4979, 127.0276),
            confirmation.locationCalls.single().third
        )
    }

    @Test
    fun `세부 위치를 나중에 정하면 세부 위치 없이 보낸다`() = runTest(dispatcher) {
        val vm = vm()
        vm.loadTimeCandidates()
        runCurrent()
        vm.chooseDateTimeAndPlace()
        vm.onChooseDetailLater()

        vm.onConfirm()
        runCurrent()

        assertNull(confirmation.locationCalls.single().third)
    }

    @Test
    fun `세부 위치 검색어를 지우면 검색하지 않고 결과를 비운다`() = runTest(dispatcher) {
        val vm = vm()
        vm.onDetailSearchQueryChange("스터디")
        vm.onDetailSearchQueryChange("")
        settle()
        assertTrue(search.queries.isEmpty())
        assertTrue(vm.uiState.value.detailLocationChoices.isEmpty())
        assertEquals("", vm.uiState.value.detailSearchQuery)
    }

    @Test
    fun `날짜·시간 확정이 실패하면 장소는 보내지 않고 안내하며 다시 누를 수 있다`() = runTest(dispatcher) {
        confirmation.dateResult = ConfirmationResult.Failure(retryable = true)
        val vm = vm()
        vm.loadTimeCandidates()
        runCurrent()
        vm.chooseDateTimeAndPlace()

        vm.onConfirm()
        runCurrent()

        assertTrue(confirmation.locationCalls.isEmpty())
        assertEquals(HostConfirmationNotice.FAILURE_RETRYABLE, vm.notice.value)
        assertEquals(HostConfirmationStep.REVIEW, vm.uiState.value.step)
        assertTrue(vm.uiState.value.canConfirm)
    }

    @Test
    fun `서버 결과별로 안내를 나눈다`() = runTest(dispatcher) {
        val expected = listOf(
            ConfirmationResult.LoginRequired to HostConfirmationNotice.LOGIN_REQUIRED,
            ConfirmationResult.NotHost to HostConfirmationNotice.NOT_HOST,
            ConfirmationResult.Rejected(ConfirmationRejectReason.APPOINTMENT_CANCELED) to
                HostConfirmationNotice.APPOINTMENT_CANCELED,
            ConfirmationResult.Rejected(ConfirmationRejectReason.INVALID_DATE) to
                HostConfirmationNotice.INVALID_SELECTION,
            ConfirmationResult.Rejected(ConfirmationRejectReason.INVALID_LOCATION) to
                HostConfirmationNotice.INVALID_SELECTION,
            ConfirmationResult.Failure(retryable = false) to HostConfirmationNotice.FAILURE
        )
        for ((result, notice) in expected) {
            confirmation.locationResult = result
            val vm = vm()
            vm.loadTimeCandidates()
            runCurrent()
            vm.chooseDateTimeAndPlace()
            vm.onConfirm()
            runCurrent()
            assertEquals(notice, vm.notice.value)
        }
    }

    @Test
    fun `처리 중에는 확정을 다시 보내지 않는다`() = runTest(dispatcher) {
        val vm = vm()
        vm.loadTimeCandidates()
        runCurrent()
        vm.chooseDateTimeAndPlace()

        vm.onConfirm()
        vm.onConfirm()
        runCurrent()

        assertEquals(1, confirmation.dateCalls.size)
    }
}
