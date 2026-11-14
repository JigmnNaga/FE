package com.jigmnnaga.presentation.appointment.manage

import com.jigmnnaga.domain.model.AppointmentAttendanceStatus
import com.jigmnnaga.domain.model.AppointmentDateSelectionType
import com.jigmnnaga.domain.model.AppointmentDetail
import com.jigmnnaga.domain.model.AppointmentLocation as DetailLocation
import com.jigmnnaga.domain.model.AppointmentManageRejectReason
import com.jigmnnaga.domain.model.AppointmentManageResult
import com.jigmnnaga.domain.model.CandidateLocationChange
import com.jigmnnaga.domain.model.NewAppointmentDateType
import com.jigmnnaga.domain.model.NewCandidateLocation
import com.jigmnnaga.domain.model.PlaceSearchPage
import com.jigmnnaga.domain.model.PlaceSearchResult
import com.jigmnnaga.domain.model.PlaceSearchType
import com.jigmnnaga.domain.model.ReminderSendSummary
import com.jigmnnaga.domain.model.SearchedPlace
import com.jigmnnaga.domain.repository.AppointmentManageRepository
import com.jigmnnaga.domain.repository.PlaceSearchRepository
import com.jigmnnaga.presentation.appointment.create.AppointmentDate
import com.jigmnnaga.presentation.appointment.create.AppointmentDeadlineChoice
import com.jigmnnaga.presentation.appointment.create.AppointmentLocation
import com.jigmnnaga.presentation.appointment.create.AppointmentScheduleType
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
class AppointmentManageViewModelTest {

    private class FakeManage : AppointmentManageRepository {
        var result: AppointmentManageResult<Unit> = AppointmentManageResult.Success(Unit)
        var gate: CompletableDeferred<Unit>? = null
        val calls = mutableListOf<String>()
        var lastChange: CandidateLocationChange? = null
        var lastDates: Pair<NewAppointmentDateType, List<String>>? = null

        private suspend fun record(call: String): AppointmentManageResult<Unit> {
            calls += call
            gate?.await()
            return result
        }

        override suspend fun updateTitle(appointmentId: Long, title: String) =
            record("title:$appointmentId:$title")

        override suspend fun updateCandidateDates(
            appointmentId: Long,
            dateType: NewAppointmentDateType,
            dates: List<String>
        ): AppointmentManageResult<Unit> {
            lastDates = dateType to dates
            return record("dates:$appointmentId")
        }

        override suspend fun updateCandidateLocations(
            appointmentId: Long,
            change: CandidateLocationChange
        ): AppointmentManageResult<Unit> {
            lastChange = change
            return record("locations:$appointmentId")
        }

        override suspend fun updateResponseDeadline(appointmentId: Long, deadline: String) =
            record("deadline:$appointmentId:$deadline")

        override suspend fun cancel(appointmentId: Long) = record("cancel:$appointmentId")
        override suspend fun delete(appointmentId: Long) = record("delete:$appointmentId")
        override suspend fun delegateHost(appointmentId: Long, newHostMemberId: Long) =
            record("host:$appointmentId:$newHostMemberId")

        override suspend fun kickParticipant(appointmentId: Long, targetMemberId: Long) =
            record("kick:$appointmentId:$targetMemberId")

        override suspend fun sendReminders(appointmentId: Long): AppointmentManageResult<ReminderSendSummary> =
            AppointmentManageResult.Success(ReminderSendSummary(0, emptyList()))
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

    /** 2026-11-14 10:00:00 (한국 시간) */
    private val now: Long = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul")).apply {
        clear()
        set(2026, 10, 14, 10, 0, 0)
    }.timeInMillis

    private val dispatcher = StandardTestDispatcher()
    private val manage = FakeManage()
    private val search = FakeSearch()
    private var changedCount = 0

    private val gangnam = DetailLocation(1L, "강남역", "서울 강남구", "2호선", 37.49, 127.02)
    private val jamsil = DetailLocation(2L, "잠실역", "서울 송파구", "2호선", 37.51, 127.10)

    private val gangnamForm = AppointmentLocation(
        name = "강남역",
        address = "2호선 · 서울 강남구",
        lineNames = "2호선",
        roadAddress = "서울 강남구",
        latitude = 37.49,
        longitude = 127.02
    )
    private val jamsilForm = AppointmentLocation(
        name = "잠실역",
        address = "2호선 · 서울 송파구",
        lineNames = "2호선",
        roadAddress = "서울 송파구",
        latitude = 37.51,
        longitude = 127.10
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun detail(canceled: Boolean = false) = AppointmentDetail(
        appointmentId = 7L,
        title = "스터디",
        canceled = canceled,
        dateSelectionType = AppointmentDateSelectionType.RANGE,
        candidateDates = listOf("2026-11-20", "2026-11-21", "2026-11-22"),
        candidateLocations = listOf(gangnam, jamsil),
        responseDeadline = "2026-11-19T23:59:59",
        inviteUrl = null,
        confirmed = false,
        confirmedDate = null,
        confirmedTime = null,
        confirmedLocation = null,
        detailPlace = null,
        myRole = null,
        myAttendanceStatus = AppointmentAttendanceStatus.PENDING,
        myNotificationEnabled = false,
        myTravelMode = null,
        joinedCount = 2,
        scheduleSubmittedCount = 0,
        members = emptyList()
    )

    private fun viewModel(canceled: Boolean = false) = AppointmentManageViewModel(
        detail = detail(canceled),
        repository = manage,
        placeSearchRepository = search,
        searchDebounceMillis = 300L,
        nowMillis = { now },
        onChanged = { changedCount++ }
    )

    private fun place(id: String, name: String, lat: Double? = 37.0, lng: Double? = 127.0) = SearchedPlace(
        kakaoPlaceId = id,
        name = name,
        lineNames = "9호선",
        roadAddress = "서울 어딘가",
        jibunAddress = null,
        latitude = lat,
        longitude = lng
    )

    @Test
    fun `처음에는 약속 상세 값으로 관리 목록을 채운다`() {
        val state = viewModel().uiState.value

        assertEquals(AppointmentManageStep.MENU, state.step)
        assertEquals("스터디", state.saved.appointmentName)
        assertEquals(AppointmentScheduleType.DATE_RANGE, state.saved.scheduleType)
        assertEquals(AppointmentDate(2026, 11, 20), state.saved.selectedStartDate)
        assertEquals(AppointmentDate(2026, 11, 22), state.saved.selectedEndDate)
        assertEquals(listOf(gangnamForm, jamsilForm), state.saved.selectedLocations)
        assertEquals("2026-11-19T23:59:59", state.savedDeadline)
        assertFalse(state.isCanceled)
    }

    @Test
    fun `제목을 바꾸고 저장하면 서버에 보내고 저장된 값과 목록을 갱신한다`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.openStep(AppointmentManageStep.TITLE)
        vm.onFormChange(vm.uiState.value.form.copy(appointmentName = "  새 이름  "))
        assertTrue(vm.uiState.value.canSave())

        vm.save()
        runCurrent()

        assertEquals(listOf("title:7:새 이름"), manage.calls)
        val state = vm.uiState.value
        assertEquals(AppointmentManageStep.MENU, state.step)
        assertEquals("새 이름", state.saved.appointmentName)
        assertEquals("새 이름", state.form.appointmentName)
        assertFalse(state.isWorking)
        assertEquals(AppointmentManageNotice.SAVED, vm.notice.value)
        assertEquals(1, changedCount)
    }

    @Test
    fun `제목이 비었거나 그대로면 저장할 수 없고 30자를 넘으면 잘린다`() {
        val vm = viewModel()
        vm.openStep(AppointmentManageStep.TITLE)
        assertFalse(vm.uiState.value.canSave())

        vm.onFormChange(vm.uiState.value.form.copy(appointmentName = "   "))
        assertFalse(vm.uiState.value.canSave())

        vm.onFormChange(vm.uiState.value.form.copy(appointmentName = "가".repeat(40)))
        assertEquals(MAX_APPOINTMENT_TITLE_LENGTH, vm.uiState.value.form.appointmentName.length)
        assertTrue(vm.uiState.value.canSave())
    }

    @Test
    fun `날짜를 하루로 바꾸면 하루 선택으로 보낸다`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.openStep(AppointmentManageStep.SCHEDULE)
        vm.onFormChange(
            vm.uiState.value.form.copy(
                scheduleType = AppointmentScheduleType.SINGLE_DAY,
                selectedStartDate = AppointmentDate(2026, 11, 25),
                selectedEndDate = null,
                selectedDates = emptyList()
            )
        )
        assertTrue(vm.uiState.value.canSave())

        vm.save()
        runCurrent()

        assertEquals(NewAppointmentDateType.SINGLE to listOf("2026-11-25"), manage.lastDates)
        val saved = vm.uiState.value.saved
        assertEquals(AppointmentScheduleType.SINGLE_DAY, saved.scheduleType)
        assertEquals(AppointmentDate(2026, 11, 25), saved.selectedStartDate)
        assertEquals(1, changedCount)
    }

    @Test
    fun `기간을 바꾸면 사이 날짜를 모두 펼쳐 보내고 그대로면 저장할 수 없다`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.openStep(AppointmentManageStep.SCHEDULE)
        assertFalse(vm.uiState.value.canSave())

        vm.onFormChange(
            vm.uiState.value.form.copy(
                selectedStartDate = AppointmentDate(2026, 11, 28),
                selectedEndDate = AppointmentDate(2026, 11, 30)
            )
        )
        vm.save()
        runCurrent()

        assertEquals(
            NewAppointmentDateType.RANGE to listOf("2026-11-28", "2026-11-29", "2026-11-30"),
            manage.lastDates
        )
    }

    @Test
    fun `장소를 바꾸면 유지할 장소는 ID로 새 장소는 위치와 함께 보낸다`() = runTest(dispatcher) {
        search.result = PlaceSearchResult.Success(PlaceSearchPage(listOf(place("9", "서울역")), isEnd = true))
        val vm = viewModel()
        vm.openStep(AppointmentManageStep.LOCATIONS)
        // 처음에는 고른 장소가 후보 목록으로 보여서 목록에서 바로 뺄 수 있다.
        assertEquals(listOf(gangnamForm, jamsilForm), vm.uiState.value.form.locationCandidates)

        vm.onFormChange(vm.uiState.value.form.copy(locationQuery = "서울"))
        advanceTimeBy(300)
        runCurrent()
        assertEquals(listOf("서울" to PlaceSearchType.STATION), search.queries)
        val seoul = vm.uiState.value.form.locationCandidates.last()
        assertEquals("서울역", seoul.name)
        assertTrue(vm.uiState.value.form.locationCandidatesFromServer)

        vm.onFormChange(vm.uiState.value.form.copy(selectedLocations = listOf(gangnamForm, seoul)))
        assertTrue(vm.uiState.value.canSave())
        vm.save()
        runCurrent()

        assertEquals(
            CandidateLocationChange(
                keepLocationIds = listOf(1L),
                newLocations = listOf(NewCandidateLocation("서울역", "서울 어딘가", "9호선", 37.0, 127.0))
            ),
            manage.lastChange
        )
        assertEquals(listOf(gangnamForm, seoul), vm.uiState.value.saved.selectedLocations)
    }

    @Test
    fun `장소를 뺀 뒤 다시 고르면 옛 ID가 아니라 새 장소로 보낸다`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.openStep(AppointmentManageStep.LOCATIONS)
        vm.onFormChange(vm.uiState.value.form.copy(selectedLocations = listOf(gangnamForm)))
        vm.save()
        runCurrent()
        assertEquals(CandidateLocationChange(listOf(1L), emptyList()), manage.lastChange)

        vm.openStep(AppointmentManageStep.LOCATIONS)
        vm.onFormChange(vm.uiState.value.form.copy(selectedLocations = listOf(gangnamForm, jamsilForm)))
        vm.save()
        runCurrent()

        assertEquals(
            CandidateLocationChange(
                keepLocationIds = listOf(1L),
                newLocations = listOf(NewCandidateLocation("잠실역", "서울 송파구", "2호선", 37.51, 127.10))
            ),
            manage.lastChange
        )
    }

    @Test
    fun `위치가 없는 새 장소나 빈 선택이나 여섯 곳은 저장할 수 없다`() {
        val vm = viewModel()
        vm.openStep(AppointmentManageStep.LOCATIONS)

        val noPosition = AppointmentLocation(name = "어딘가", address = "")
        vm.onFormChange(vm.uiState.value.form.copy(selectedLocations = listOf(gangnamForm, noPosition)))
        assertFalse(vm.uiState.value.canSave())

        vm.onFormChange(vm.uiState.value.form.copy(selectedLocations = emptyList()))
        assertFalse(vm.uiState.value.canSave())

        val many = (1..6).map { gangnamForm.copy(name = "역$it") }
        vm.onFormChange(vm.uiState.value.form.copy(selectedLocations = many))
        assertEquals(5, vm.uiState.value.form.selectedLocations.size)
    }

    @Test
    fun `응답 기한을 고르면 지금부터 계산해 보내고 직접 고르면 그날 끝까지로 보낸다`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.openStep(AppointmentManageStep.DEADLINE)
        assertFalse(vm.uiState.value.canSave())

        vm.onFormChange(vm.uiState.value.form.copy(deadlineChoice = AppointmentDeadlineChoice.THREE_DAYS))
        vm.save()
        runCurrent()
        assertEquals("deadline:7:2026-11-17T10:00:00", manage.calls.last())
        assertEquals("2026-11-17T10:00:00", vm.uiState.value.savedDeadline)

        vm.openStep(AppointmentManageStep.DEADLINE)
        vm.onFormChange(vm.uiState.value.form.copy(deadlineChoice = AppointmentDeadlineChoice.CUSTOM_DATE))
        assertFalse(vm.uiState.value.canSave())
        vm.onFormChange(vm.uiState.value.form.copy(customDeadline = AppointmentDate(2026, 11, 19)))
        vm.save()
        runCurrent()
        assertEquals("deadline:7:2026-11-19T23:59:59", manage.calls.last())
    }

    @Test
    fun `서버가 거절하면 편집 화면과 입력을 그대로 두고 이유를 알린다`() = runTest(dispatcher) {
        manage.result = AppointmentManageResult.Rejected(AppointmentManageRejectReason.DATES_BEFORE_DEADLINE)
        val vm = viewModel()
        vm.openStep(AppointmentManageStep.SCHEDULE)
        vm.onFormChange(vm.uiState.value.form.copy(selectedEndDate = AppointmentDate(2026, 11, 30)))

        vm.save()
        runCurrent()

        val state = vm.uiState.value
        assertEquals(AppointmentManageStep.SCHEDULE, state.step)
        assertEquals(AppointmentDate(2026, 11, 30), state.form.selectedEndDate)
        assertEquals(AppointmentDate(2026, 11, 22), state.saved.selectedEndDate)
        assertFalse(state.isWorking)
        assertEquals(AppointmentManageNotice.DATES_BEFORE_DEADLINE, vm.notice.value)
        assertEquals(0, changedCount)
    }

    @Test
    fun `실패는 재시도 가능 여부에 따라 안내가 다르다`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.openStep(AppointmentManageStep.TITLE)
        vm.onFormChange(vm.uiState.value.form.copy(appointmentName = "새 이름"))

        manage.result = AppointmentManageResult.Failure(retryable = true)
        vm.save()
        runCurrent()
        assertEquals(AppointmentManageNotice.FAILED_RETRY, vm.notice.value)

        vm.clearNotice()
        manage.result = AppointmentManageResult.Failure(retryable = false)
        vm.save()
        runCurrent()
        assertEquals(AppointmentManageNotice.FAILED, vm.notice.value)

        vm.clearNotice()
        manage.result = AppointmentManageResult.NotHost
        vm.save()
        runCurrent()
        assertEquals(AppointmentManageNotice.NOT_HOST, vm.notice.value)
        assertEquals(0, changedCount)
    }

    @Test
    fun `로그인이 필요하면 안내하고 로그인 뒤 하던 저장을 다시 한다`() = runTest(dispatcher) {
        manage.result = AppointmentManageResult.LoginRequired
        val vm = viewModel()
        vm.openStep(AppointmentManageStep.TITLE)
        vm.onFormChange(vm.uiState.value.form.copy(appointmentName = "새 이름"))

        vm.save()
        runCurrent()
        assertEquals(AppointmentManageNotice.LOGIN_REQUIRED, vm.notice.value)
        assertEquals(1, manage.calls.size)

        manage.result = AppointmentManageResult.Success(Unit)
        vm.clearNotice()
        vm.resumeAfterLogin()
        runCurrent()

        assertEquals(2, manage.calls.size)
        assertEquals("새 이름", vm.uiState.value.saved.appointmentName)
        // 다시 불러도 한 번만 실행된다.
        vm.resumeAfterLogin()
        runCurrent()
        assertEquals(2, manage.calls.size)
    }

    @Test
    fun `저장하는 동안에는 다시 저장하지 않는다`() = runTest(dispatcher) {
        manage.gate = CompletableDeferred()
        val vm = viewModel()
        vm.openStep(AppointmentManageStep.TITLE)
        vm.onFormChange(vm.uiState.value.form.copy(appointmentName = "새 이름"))

        vm.save()
        runCurrent()
        vm.save()
        runCurrent()

        assertEquals(1, manage.calls.size)
        assertTrue(vm.uiState.value.isWorking)
        assertFalse(vm.uiState.value.canSave())

        manage.gate!!.complete(Unit)
        runCurrent()
        assertFalse(vm.uiState.value.isWorking)
    }

    @Test
    fun `뒤로 가면 편집 중인 값을 버리고 관리 목록으로 돌아간다`() {
        val vm = viewModel()
        vm.openStep(AppointmentManageStep.TITLE)
        vm.onFormChange(vm.uiState.value.form.copy(appointmentName = "바꾸는 중"))

        assertTrue(vm.back())

        val state = vm.uiState.value
        assertEquals(AppointmentManageStep.MENU, state.step)
        assertEquals("스터디", state.form.appointmentName)
        assertFalse(vm.back())
    }

    @Test
    fun `약속 취소는 확인한 뒤에만 실행하고 취소된 약속으로 바꾼다`() = runTest(dispatcher) {
        val vm = viewModel()

        vm.requestConfirm(AppointmentManageConfirm.CANCEL_APPOINTMENT)
        assertEquals(AppointmentManageConfirm.CANCEL_APPOINTMENT, vm.uiState.value.confirm)
        vm.dismissConfirm()
        vm.confirmAction()
        runCurrent()
        assertTrue(manage.calls.isEmpty())

        vm.requestConfirm(AppointmentManageConfirm.CANCEL_APPOINTMENT)
        vm.confirmAction()
        runCurrent()

        assertEquals(listOf("cancel:7"), manage.calls)
        val state = vm.uiState.value
        assertTrue(state.isCanceled)
        assertNull(state.confirm)
        assertFalse(state.canSave())
        assertEquals(AppointmentManageNotice.CANCELED_DONE, vm.notice.value)
        assertEquals(1, changedCount)

        vm.requestConfirm(AppointmentManageConfirm.CANCEL_APPOINTMENT)
        assertNull(vm.uiState.value.confirm)
    }

    @Test
    fun `약속방 삭제에 성공하면 화면을 닫도록 알린다`() = runTest(dispatcher) {
        val vm = viewModel()

        vm.requestConfirm(AppointmentManageConfirm.DELETE_APPOINTMENT)
        vm.confirmAction()
        runCurrent()

        assertEquals(listOf("delete:7"), manage.calls)
        assertEquals(AppointmentManageFinish.DELETED, vm.uiState.value.finished)
        assertEquals(1, changedCount)
    }

    @Test
    fun `삭제가 실패하면 닫지 않고 로그인 뒤 다시 확인 없이 이어서 실행한다`() = runTest(dispatcher) {
        manage.result = AppointmentManageResult.LoginRequired
        val vm = viewModel()
        vm.requestConfirm(AppointmentManageConfirm.DELETE_APPOINTMENT)
        vm.confirmAction()
        runCurrent()
        assertNull(vm.uiState.value.finished)
        assertEquals(AppointmentManageNotice.LOGIN_REQUIRED, vm.notice.value)

        manage.result = AppointmentManageResult.Success(Unit)
        vm.resumeAfterLogin()
        runCurrent()

        assertEquals(listOf("delete:7", "delete:7"), manage.calls)
        assertEquals(AppointmentManageFinish.DELETED, vm.uiState.value.finished)
    }

    @Test
    fun `이미 취소된 약속은 정보를 바꿀 수 없다`() {
        val vm = viewModel(canceled = true)
        vm.openStep(AppointmentManageStep.TITLE)
        vm.onFormChange(vm.uiState.value.form.copy(appointmentName = "새 이름"))

        assertTrue(vm.uiState.value.isCanceled)
        assertFalse(vm.uiState.value.canSave())
    }
}
