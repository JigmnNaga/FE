package com.jigmnnaga.presentation.appointment.create

import com.jigmnnaga.domain.model.AppointmentCreateRejectReason
import com.jigmnnaga.domain.model.AppointmentCreateResult
import com.jigmnnaga.domain.model.CreatedAppointment
import com.jigmnnaga.domain.model.NewAppointment
import com.jigmnnaga.domain.model.NewAppointmentDateType
import com.jigmnnaga.domain.model.NewCandidateLocation
import com.jigmnnaga.domain.model.PlaceSearchPage
import com.jigmnnaga.domain.model.PlaceSearchResult
import com.jigmnnaga.domain.model.PlaceSearchType
import com.jigmnnaga.domain.model.PreviousMembers
import com.jigmnnaga.domain.model.SearchedPlace
import com.jigmnnaga.domain.repository.AppointmentCreateRepository
import com.jigmnnaga.domain.repository.PlaceSearchRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
class AppointmentCreateServerViewModelTest {

    private class FakeCreate : AppointmentCreateRepository {
        var result: AppointmentCreateResult<CreatedAppointment> = AppointmentCreateResult.Success(
            CreatedAppointment(9L, "abc", "https://example.com/invite/abc")
        )
        var gate: CompletableDeferred<Unit>? = null
        val requests = mutableListOf<NewAppointment>()

        override suspend fun create(appointment: NewAppointment): AppointmentCreateResult<CreatedAppointment> {
            requests += appointment
            gate?.await()
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

    /** 2026-11-14 12:00:00 (한국 시간) */
    private val now: Long = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul")).apply {
        clear()
        set(2026, 10, 14, 12, 0, 0)
    }.timeInMillis

    private val dispatcher = StandardTestDispatcher()
    private val create = FakeCreate()
    private val search = FakeSearch()

    private val gangnam = AppointmentLocation(
        name = "강남역",
        address = "2호선 · 서울특별시 강남구",
        lineNames = "2호선",
        roadAddress = "서울특별시 강남구",
        latitude = 37.49,
        longitude = 127.02
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        withSearch: Boolean = false,
        previousMembers: PreviousMembers? = null
    ) = AppointmentCreateViewModel(
        createRepository = create,
        placeSearchRepository = if (withSearch) search else null,
        previousMembers = previousMembers,
        searchDebounceMillis = 300L,
        nowMillis = { now }
    )

    private fun AppointmentCreateViewModel.fillValidForm(
        locations: List<AppointmentLocation> = listOf(gangnam)
    ) {
        updateUiState(
            uiState.value.copy(
                step = AppointmentCreateStep.RESPONSE_DEADLINE,
                appointmentName = "  저녁 모임  ",
                scheduleType = AppointmentScheduleType.SINGLE_DAY,
                selectedStartDate = AppointmentDate(2026, 12, 5),
                selectedLocations = locations,
                deadlineChoice = AppointmentDeadlineChoice.THREE_DAYS
            )
        )
    }

    private fun AppointmentCreateViewModel.submit() = requestCreate(buildDraft()!!)

    @Test
    fun `생성에 성공하면 요청 내용을 보내고 초대 링크를 마지막 단계에 보여 준다`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.fillValidForm()

        vm.submit()
        runCurrent()

        assertEquals(1, create.requests.size)
        val request = create.requests.single()
        assertEquals("저녁 모임", request.title)
        assertEquals(NewAppointmentDateType.SINGLE, request.dateType)
        assertEquals(listOf("2026-12-05"), request.candidateDates)
        assertEquals("2026-11-17T12:00:00", request.responseDeadline)
        assertEquals(
            listOf(NewCandidateLocation("강남역", "서울특별시 강남구", "2호선", 37.49, 127.02)),
            request.candidateLocations
        )
        val state = vm.uiState.value
        assertEquals(AppointmentCreateStep.INVITATION, state.step)
        assertEquals(AppointmentCreateSubmissionStatus.SUCCEEDED, state.submissionStatus)
        assertEquals("https://example.com/invite/abc", state.inviteLink)
        assertFalse(state.isCreating)
        assertNull(vm.notice.value)
        assertNull(vm.pendingDraft.value)
    }

    @Test
    fun `이전 약속 멤버가 있으면 요청에 함께 보낸다`() = runTest(dispatcher) {
        val members = PreviousMembers(4L, listOf(2L, 3L))
        val vm = viewModel(previousMembers = members)
        vm.fillValidForm()

        vm.submit()
        runCurrent()

        assertEquals(members, create.requests.single().previousMembers)
    }

    @Test
    fun `요청하는 동안에는 생성 중이고 다시 눌러도 한 번만 보낸다`() = runTest(dispatcher) {
        create.gate = CompletableDeferred()
        val vm = viewModel()
        vm.fillValidForm()
        val draft = vm.buildDraft()!!

        vm.requestCreate(draft)
        runCurrent()
        assertTrue(vm.uiState.value.isCreating)
        vm.requestCreate(draft)
        runCurrent()
        assertEquals(1, create.requests.size)

        create.gate!!.complete(Unit)
        runCurrent()

        assertFalse(vm.uiState.value.isCreating)
        assertEquals(AppointmentCreateStep.INVITATION, vm.uiState.value.step)
        vm.requestCreate(draft)
        runCurrent()
        assertEquals(1, create.requests.size)
    }

    @Test
    fun `초대 링크가 비어 있어도 약속은 만들어졌으므로 다시 생성하지 않고 안내한다`() = runTest(dispatcher) {
        create.result = AppointmentCreateResult.Success(CreatedAppointment(9L, null, ""))
        val vm = viewModel()
        vm.fillValidForm()
        val draft = vm.buildDraft()!!

        vm.requestCreate(draft)
        runCurrent()

        val state = vm.uiState.value
        assertEquals(AppointmentCreateStep.INVITATION, state.step)
        assertEquals(AppointmentCreateSubmissionStatus.SUCCEEDED, state.submissionStatus)
        assertNull(state.inviteLink)
        assertFalse(state.isCreating)
        assertEquals(AppointmentCreateNotice.INVITE_LINK_UNAVAILABLE, vm.notice.value)
        vm.requestCreate(draft)
        runCurrent()
        assertEquals(1, create.requests.size)
    }

    @Test
    fun `로그인이 필요하면 안내하고 로그인 뒤에 같은 내용으로 다시 요청한다`() = runTest(dispatcher) {
        create.result = AppointmentCreateResult.LoginRequired
        val vm = viewModel()
        vm.fillValidForm()

        vm.submit()
        runCurrent()

        assertEquals(AppointmentCreateNotice.LOGIN_REQUIRED, vm.notice.value)
        assertFalse(vm.uiState.value.isCreating)
        assertEquals(AppointmentCreateStep.RESPONSE_DEADLINE, vm.uiState.value.step)
        assertEquals("  저녁 모임  ", vm.uiState.value.appointmentName)

        create.result = AppointmentCreateResult.Success(CreatedAppointment(9L, "abc", "https://x/abc"))
        vm.clearNotice()
        vm.resumeAfterLogin()
        runCurrent()

        assertEquals(2, create.requests.size)
        assertEquals(create.requests[0], create.requests[1])
        assertEquals(AppointmentCreateStep.INVITATION, vm.uiState.value.step)
        assertEquals("https://x/abc", vm.uiState.value.inviteLink)
    }

    @Test
    fun `로그인 뒤 복귀 함수는 한 번만 실행된다`() = runTest(dispatcher) {
        create.result = AppointmentCreateResult.LoginRequired
        val vm = viewModel()
        vm.fillValidForm()
        vm.submit()
        runCurrent()

        create.result = AppointmentCreateResult.Success(CreatedAppointment(9L, "abc", "https://x/abc"))
        vm.resumeAfterLogin()
        runCurrent()
        vm.resumeAfterLogin()
        runCurrent()

        assertEquals(2, create.requests.size)
    }

    @Test
    fun `서버가 거절하면 이유별 안내를 보내고 입력은 그대로 두어 다시 시도할 수 있다`() = runTest(dispatcher) {
        val expected = mapOf(
            AppointmentCreateRejectReason.INVALID_DATES to AppointmentCreateNotice.INVALID_DATES,
            AppointmentCreateRejectReason.INVALID_DEADLINE to AppointmentCreateNotice.INVALID_DEADLINE,
            AppointmentCreateRejectReason.INVALID_LOCATIONS to AppointmentCreateNotice.INVALID_LOCATIONS,
            AppointmentCreateRejectReason.INVALID_PREVIOUS_MEMBERS to AppointmentCreateNotice.INVALID_PREVIOUS_MEMBERS,
            AppointmentCreateRejectReason.INVALID_INPUT to AppointmentCreateNotice.INVALID_INPUT
        )

        for ((reason, notice) in expected) {
            create.requests.clear()
            create.result = AppointmentCreateResult.Rejected(reason)
            val vm = viewModel()
            vm.fillValidForm()

            vm.submit()
            runCurrent()

            assertEquals(reason.name, notice, vm.notice.value)
            val state = vm.uiState.value
            assertEquals(AppointmentCreateStep.RESPONSE_DEADLINE, state.step)
            assertEquals(AppointmentCreateSubmissionStatus.READY, state.submissionStatus)
            assertFalse(state.isCreating)
            assertEquals("  저녁 모임  ", state.appointmentName)

            vm.submit()
            runCurrent()
            assertEquals(reason.name, 2, create.requests.size)
        }
    }

    @Test
    fun `네트워크 서버 오류는 다시 시도 안내를 보내고 그 밖의 실패는 일반 안내를 보낸다`() = runTest(dispatcher) {
        create.result = AppointmentCreateResult.Failure(retryable = true)
        val retryable = viewModel()
        retryable.fillValidForm()
        retryable.submit()
        runCurrent()
        assertEquals(AppointmentCreateNotice.CREATE_FAILED_RETRY, retryable.notice.value)
        assertFalse(retryable.uiState.value.isCreating)

        create.result = AppointmentCreateResult.Failure(retryable = false)
        val permanent = viewModel()
        permanent.fillValidForm()
        permanent.submit()
        runCurrent()
        assertEquals(AppointmentCreateNotice.CREATE_FAILED, permanent.notice.value)
        assertEquals(AppointmentCreateSubmissionStatus.READY, permanent.uiState.value.submissionStatus)
    }

    @Test
    fun `위치가 없는 장소가 있으면 서버를 부르지 않고 안내한다`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.fillValidForm(locations = listOf(AppointmentLocation("성수역", "2호선 · 서울")))

        vm.submit()
        runCurrent()

        assertTrue(create.requests.isEmpty())
        assertEquals(AppointmentCreateNotice.INVALID_LOCATIONS, vm.notice.value)
        assertFalse(vm.uiState.value.isCreating)
        assertEquals(AppointmentCreateStep.RESPONSE_DEADLINE, vm.uiState.value.step)
    }

    @Test
    fun `입력을 고치면 거절 안내가 사라지고 다시 요청할 수 있다`() = runTest(dispatcher) {
        create.result = AppointmentCreateResult.Rejected(AppointmentCreateRejectReason.INVALID_DEADLINE)
        val vm = viewModel()
        vm.fillValidForm()
        vm.submit()
        runCurrent()
        assertEquals(AppointmentCreateNotice.INVALID_DEADLINE, vm.notice.value)

        vm.updateUiState(vm.uiState.value.copy(deadlineChoice = AppointmentDeadlineChoice.ONE_WEEK))
        assertNull(vm.notice.value)
        create.result = AppointmentCreateResult.Success(CreatedAppointment(9L, "abc", "https://x/abc"))
        vm.submit()
        runCurrent()

        assertEquals("2026-11-21T12:00:00", create.requests.last().responseDeadline)
        assertEquals(AppointmentCreateStep.INVITATION, vm.uiState.value.step)
    }

    @Test
    fun `저장소가 없으면 서버 호출 없이 미연결 안내만 보낸다`() = runTest(dispatcher) {
        val vm = AppointmentCreateViewModel()
        vm.fillValidForm()

        vm.submit()
        runCurrent()

        assertEquals(AppointmentCreateSubmissionStatus.API_UNAVAILABLE, vm.uiState.value.submissionStatus)
        assertEquals(AppointmentCreateNotice.CREATE_API_NOT_CONNECTED, vm.notice.value)
        assertTrue(create.requests.isEmpty())
    }

    // --- 장소 검색 ---

    private fun place(
        id: String,
        name: String,
        lat: Double? = 37.5,
        lng: Double? = 127.0,
        lines: String? = "2호선",
        road: String? = "서울 강남구"
    ) = SearchedPlace(id, name, lines, road, null, lat, lng)

    @Test
    fun `검색어를 입력하면 잠시 기다린 뒤 마지막 검색어로 지하철역을 한 번만 검색한다`() = runTest(dispatcher) {
        search.result = PlaceSearchResult.Success(
            PlaceSearchPage(listOf(place("1", "강남역")), isEnd = true)
        )
        val vm = viewModel(withSearch = true)

        vm.updateUiState(vm.uiState.value.copy(locationQuery = "강"))
        advanceTimeBy(100)
        vm.updateUiState(vm.uiState.value.copy(locationQuery = "강남"))
        advanceTimeBy(299)
        runCurrent()
        assertTrue(search.queries.isEmpty())

        advanceTimeBy(1)
        runCurrent()

        assertEquals(listOf("강남" to PlaceSearchType.STATION), search.queries)
        val state = vm.uiState.value
        assertTrue(state.locationCandidatesFromServer)
        assertEquals(
            listOf(
                AppointmentLocation(
                    name = "강남역",
                    address = "2호선 · 서울 강남구",
                    lineNames = "2호선",
                    roadAddress = "서울 강남구",
                    latitude = 37.5,
                    longitude = 127.0
                )
            ),
            state.locationCandidates
        )
    }

    @Test
    fun `위치가 없는 검색 결과는 후보로 보여 주지 않는다`() = runTest(dispatcher) {
        search.result = PlaceSearchResult.Success(
            PlaceSearchPage(
                listOf(place("1", "강남역"), place("2", "위치 없음", lat = null), place("3", "경도 없음", lng = null)),
                isEnd = true
            )
        )
        val vm = viewModel(withSearch = true)

        vm.updateUiState(vm.uiState.value.copy(locationQuery = "역"))
        advanceTimeBy(300)
        runCurrent()

        assertEquals(listOf("강남역"), vm.uiState.value.locationCandidates.map { it.name })
    }

    @Test
    fun `노선이나 주소가 없으면 있는 것만 보여 준다`() = runTest(dispatcher) {
        search.result = PlaceSearchResult.Success(
            PlaceSearchPage(
                listOf(place("1", "성수역", lines = null), place("2", "신촌역", road = null)),
                isEnd = true
            )
        )
        val vm = viewModel(withSearch = true)

        vm.updateUiState(vm.uiState.value.copy(locationQuery = "역"))
        advanceTimeBy(300)
        runCurrent()

        assertEquals(listOf("서울 강남구", "2호선"), vm.uiState.value.locationCandidates.map { it.address })
    }

    @Test
    fun `검색어를 지우면 서버를 부르지 않고 후보를 비운다`() = runTest(dispatcher) {
        search.result = PlaceSearchResult.Success(PlaceSearchPage(listOf(place("1", "강남역")), isEnd = true))
        val vm = viewModel(withSearch = true)
        vm.updateUiState(vm.uiState.value.copy(locationQuery = "강남"))
        advanceTimeBy(300)
        runCurrent()
        assertEquals(1, vm.uiState.value.locationCandidates.size)

        vm.updateUiState(vm.uiState.value.copy(locationQuery = "  "))
        advanceTimeBy(300)
        runCurrent()

        assertEquals(1, search.queries.size)
        assertTrue(vm.uiState.value.locationCandidates.isEmpty())
        assertTrue(vm.uiState.value.locationCandidatesFromServer)
    }

    @Test
    fun `검색에 실패하면 후보를 비우고 안내한다`() = runTest(dispatcher) {
        search.result = PlaceSearchResult.Failure(retryable = true)
        val vm = viewModel(withSearch = true)

        vm.updateUiState(vm.uiState.value.copy(locationQuery = "강남"))
        advanceTimeBy(300)
        runCurrent()

        assertEquals(AppointmentCreateNotice.LOCATION_SEARCH_FAILED, vm.notice.value)
        assertTrue(vm.uiState.value.locationCandidates.isEmpty())
    }

    @Test
    fun `검색에 로그인이 필요하면 안내하고 로그인 뒤 현재 검색어로 다시 검색한다`() = runTest(dispatcher) {
        search.result = PlaceSearchResult.LoginRequired
        val vm = viewModel(withSearch = true)
        vm.updateUiState(vm.uiState.value.copy(locationQuery = "강남"))
        advanceTimeBy(300)
        runCurrent()
        assertEquals(AppointmentCreateNotice.LOGIN_REQUIRED, vm.notice.value)

        search.result = PlaceSearchResult.Success(PlaceSearchPage(listOf(place("1", "강남역")), isEnd = true))
        vm.clearNotice()
        vm.resumeAfterLogin()
        advanceTimeBy(300)
        runCurrent()

        assertEquals(listOf("강남", "강남"), search.queries.map { it.first })
        assertEquals(listOf("강남역"), vm.uiState.value.locationCandidates.map { it.name })
    }

    @Test
    fun `화면이 오래된 상태를 보내도 검색 결과 후보는 유지한다`() = runTest(dispatcher) {
        search.result = PlaceSearchResult.Success(PlaceSearchPage(listOf(place("1", "강남역")), isEnd = true))
        val vm = viewModel(withSearch = true)
        val stale = vm.uiState.value.copy(locationQuery = "강남")
        vm.updateUiState(stale)
        advanceTimeBy(300)
        runCurrent()

        vm.updateUiState(stale.copy(selectedLocations = listOf(gangnam)))

        assertEquals(listOf("강남역"), vm.uiState.value.locationCandidates.map { it.name })
        assertEquals(listOf(gangnam), vm.uiState.value.selectedLocations)
        assertEquals(1, search.queries.size)
    }

    @Test
    fun `검색 연결이 없으면 기존처럼 화면이 준 후보를 그대로 쓴다`() = runTest(dispatcher) {
        val vm = viewModel(withSearch = false)

        vm.updateUiState(
            vm.uiState.value.copy(locationQuery = "강", locationCandidates = listOf(gangnam))
        )
        advanceTimeBy(300)
        runCurrent()

        assertEquals(listOf(gangnam), vm.uiState.value.locationCandidates)
        assertFalse(vm.uiState.value.locationCandidatesFromServer)
        assertTrue(search.queries.isEmpty())
    }
}
