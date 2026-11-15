package com.jigmnnaga.presentation.appointment.schedule

import com.jigmnnaga.domain.model.MemberSchedule
import com.jigmnnaga.domain.model.MySchedule
import com.jigmnnaga.domain.model.ScheduleAvailability
import com.jigmnnaga.domain.model.ScheduleRejectReason
import com.jigmnnaga.domain.model.ScheduleResult
import com.jigmnnaga.domain.model.ScheduleStatus
import com.jigmnnaga.domain.model.ScheduleSubmission
import com.jigmnnaga.domain.repository.ScheduleRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppointmentDateTimeViewModelScheduleTest {

    private class FakeSchedule : ScheduleRepository {
        var mine: ScheduleResult<MySchedule> = ScheduleResult.Success(MySchedule(false, emptyList(), null))
        var status: ScheduleResult<ScheduleStatus> =
            ScheduleResult.Success(ScheduleStatus(0, 0, emptyList(), emptyList()))
        var submitResult: ScheduleResult<Unit> = ScheduleResult.Success(Unit)
        var gate: CompletableDeferred<Unit>? = null
        val submissions = mutableListOf<Pair<Long, ScheduleSubmission>>()
        var mineCalls = 0

        override suspend fun getMySchedule(appointmentId: Long): ScheduleResult<MySchedule> {
            mineCalls++
            return mine
        }

        override suspend fun getScheduleStatus(appointmentId: Long): ScheduleResult<ScheduleStatus> = status

        override suspend fun submitSchedule(
            appointmentId: Long,
            submission: ScheduleSubmission
        ): ScheduleResult<Unit> {
            submissions += appointmentId to submission
            gate?.await()
            return submitResult
        }
    }

    private val nov20 = AppointmentDateValue(2026, 11, 20)
    private val nov21 = AppointmentDateValue(2026, 11, 21)
    private val nov25 = AppointmentDateValue(2026, 11, 25)

    private val members = listOf(
        MemberResponseUiState("1", "태양", MemberResponseStatus.PENDING),
        MemberResponseUiState("2", "나", MemberResponseStatus.PENDING, isMe = true)
    )

    private val repository = FakeSchedule()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(candidates: Set<AppointmentDateValue>? = setOf(nov20, nov21)) =
        AppointmentDateTimeViewModel(
            appointmentTitle = "중간고사 카공 모임",
            initialMonth = AppointmentCalendarMonth(2026, 11),
            memberResponses = members,
            appointmentId = 7L,
            scheduleRepository = repository,
            candidateDates = candidates
        )

    private fun AppointmentDateTimeViewModel.pickAfternoonOn(date: AppointmentDateValue) {
        onDateToggle(date)
        onNext()
        onQuickTimeClick("afternoon")
    }

    @Test
    fun `후보 날짜가 아닌 날짜는 고를 수 없다`() {
        val vm = viewModel()

        vm.onDateToggle(nov25)
        assertTrue(vm.uiState.value.selectedDates.isEmpty())

        vm.onDateToggle(nov20)
        assertEquals(setOf(nov20), vm.uiState.value.selectedDates)
        assertEquals(setOf(nov20, nov21), vm.uiState.value.availableDates)
    }

    @Test
    fun `후보 날짜가 비어 있으면 날짜 제한을 두지 않는다`() {
        val vm = viewModel(candidates = emptySet())

        vm.onDateToggle(nov25)

        assertNull(vm.uiState.value.availableDates)
        assertEquals(setOf(nov25), vm.uiState.value.selectedDates)
    }

    @Test
    fun `화면을 열면 내가 제출한 일정을 선택값으로 채운다`() {
        repository.mine = ScheduleResult.Success(
            MySchedule(
                submitted = true,
                availabilities = listOf(
                    ScheduleAvailability("2026-11-20", "15:00", "18:00"),
                    ScheduleAvailability("2026-11-21", "09:00", "12:00"),
                    ScheduleAvailability("2026-11-25", "09:00", "12:00"),
                    ScheduleAvailability("잘못된 값", "09:00", "12:00")
                ),
                preferredLocationId = 3L
            )
        )

        val vm = viewModel()

        val state = vm.uiState.value
        assertEquals(setOf(nov20, nov21), state.selectedDates)
        assertTrue(state.canAdvance)

        vm.onNext()
        assertEquals(nov20, vm.uiState.value.activeDate)
        assertEquals(
            AppointmentTimeRange(AppointmentTimeValue(15, 0), AppointmentTimeValue(18, 0)),
            vm.uiState.value.selectedTimeRange
        )
        assertTrue(vm.uiState.value.canAdvance)
    }

    @Test
    fun `화면을 열면 참여자 응답 현황을 서버 값으로 바꾸고 내 표시는 유지한다`() {
        repository.status = ScheduleResult.Success(
            ScheduleStatus(
                totalCount = 2,
                submittedCount = 1,
                members = listOf(
                    MemberSchedule(
                        1L, "태양", null, false,
                        listOf(ScheduleAvailability("2026-11-20", "15:00", "18:00")), null
                    ),
                    MemberSchedule(2L, "나", null, false, emptyList(), null)
                ),
                locationVotes = emptyList()
            )
        )

        val vm = viewModel()

        val responses = vm.uiState.value.memberResponses
        assertEquals(MemberResponseStatus.COMPLETED, responses.first { it.id == "1" }.status)
        assertEquals(MemberResponseStatus.PENDING, responses.first { it.id == "2" }.status)
        assertTrue(responses.first { it.id == "2" }.isMe)
    }

    @Test
    fun `조회가 실패해도 화면은 그대로 쓸 수 있다`() {
        repository.mine = ScheduleResult.Failure(retryable = true)
        repository.status = ScheduleResult.Failure(retryable = true)

        val vm = viewModel()

        assertTrue(vm.uiState.value.selectedDates.isEmpty())
        assertEquals(members, vm.uiState.value.memberResponses)
        assertNull(vm.notice.value)
    }

    @Test
    fun `제출하면 날짜별 시간만 보내고 장소는 보내지 않은 뒤 완료 화면으로 넘어간다`() {
        val vm = viewModel()
        vm.pickAfternoonOn(nov20)

        vm.onNext()

        assertEquals(
            listOf(
                7L to ScheduleSubmission(
                    availabilities = listOf(ScheduleAvailability("2026-11-20", "12:00", "17:00"))
                )
            ),
            repository.submissions
        )
        assertNull(repository.submissions.single().second.preferredLocationId)
        val state = vm.uiState.value
        assertEquals(AppointmentDateTimeStep.COMPLETE, state.step)
        assertFalse(state.isSubmitting)
        assertNull(state.errorMessage)
        assertEquals(MemberResponseStatus.COMPLETED, state.memberResponses.first { it.isMe }.status)
        assertNull(vm.pendingResponse.value)
    }

    @Test
    fun `제출하는 동안에는 다시 보내지 않는다`() {
        repository.gate = CompletableDeferred()
        val vm = viewModel()
        vm.pickAfternoonOn(nov20)

        vm.onNext()
        assertTrue(vm.uiState.value.isSubmitting)
        vm.onNext()

        assertEquals(1, repository.submissions.size)
        repository.gate!!.complete(Unit)
        assertEquals(AppointmentDateTimeStep.COMPLETE, vm.uiState.value.step)
        assertFalse(vm.uiState.value.isSubmitting)
    }

    @Test
    fun `서버가 거절하면 시간 선택 단계에 남고 이유를 안내한다`() {
        val messages = mapOf(
            ScheduleRejectReason.APPOINTMENT_CANCELED to AppointmentDateTimeViewModel.ERROR_APPOINTMENT_CANCELED,
            ScheduleRejectReason.DEADLINE_PASSED to AppointmentDateTimeViewModel.ERROR_DEADLINE_PASSED,
            ScheduleRejectReason.INVALID_DATE to AppointmentDateTimeViewModel.ERROR_INVALID_DATE,
            ScheduleRejectReason.DUPLICATE_DATE to AppointmentDateTimeViewModel.ERROR_DUPLICATE_DATE,
            ScheduleRejectReason.INVALID_TIME to AppointmentDateTimeViewModel.ERROR_INVALID_TIME,
            ScheduleRejectReason.INVALID_LOCATION to AppointmentDateTimeViewModel.ERROR_SUBMIT_FAILED,
            ScheduleRejectReason.EMPTY_UPDATE to AppointmentDateTimeViewModel.ERROR_SUBMIT_FAILED
        )
        messages.forEach { (reason, message) ->
            repository.submitResult = ScheduleResult.Rejected(reason)
            val vm = viewModel()
            vm.pickAfternoonOn(nov20)

            vm.onNext()

            val state = vm.uiState.value
            assertEquals(reason.name, AppointmentDateTimeStep.TIMES, state.step)
            assertEquals(reason.name, message, state.errorMessage)
            assertFalse(state.isSubmitting)
            assertEquals(MemberResponseStatus.PENDING, state.memberResponses.first { it.isMe }.status)
        }
    }

    @Test
    fun `참여자가 아니거나 서버 오류이면 완료로 처리하지 않는다`() {
        val cases = mapOf(
            ScheduleResult.NotParticipant to AppointmentDateTimeViewModel.ERROR_NOT_PARTICIPANT,
            ScheduleResult.Failure(retryable = true) to AppointmentDateTimeViewModel.ERROR_NETWORK,
            ScheduleResult.Failure(retryable = false) to AppointmentDateTimeViewModel.ERROR_SUBMIT_FAILED
        )
        cases.forEach { (result, message) ->
            repository.submitResult = result
            val vm = viewModel()
            vm.pickAfternoonOn(nov20)

            vm.onNext()

            assertEquals(AppointmentDateTimeStep.TIMES, vm.uiState.value.step)
            assertEquals(message, vm.uiState.value.errorMessage)
        }
    }

    @Test
    fun `제출 중 로그인이 필요하면 안내를 보내고 로그인 뒤 다시 제출한다`() {
        repository.submitResult = ScheduleResult.LoginRequired
        val vm = viewModel()
        vm.pickAfternoonOn(nov20)

        vm.onNext()

        assertEquals(AppointmentDateTimeNotice.LOGIN_REQUIRED, vm.notice.value)
        assertEquals(AppointmentDateTimeStep.TIMES, vm.uiState.value.step)
        assertFalse(vm.uiState.value.isSubmitting)

        vm.clearNotice()
        repository.submitResult = ScheduleResult.Success(Unit)
        vm.resumeAfterLogin()

        assertEquals(2, repository.submissions.size)
        assertEquals(AppointmentDateTimeStep.COMPLETE, vm.uiState.value.step)
        assertEquals(1, repository.mineCalls)
    }

    @Test
    fun `조회 중 로그인이 필요하면 로그인 뒤 다시 조회한다`() {
        repository.mine = ScheduleResult.LoginRequired
        val vm = viewModel()
        assertEquals(AppointmentDateTimeNotice.LOGIN_REQUIRED, vm.notice.value)

        vm.clearNotice()
        repository.mine = ScheduleResult.Success(
            MySchedule(true, listOf(ScheduleAvailability("2026-11-20", "15:00", "18:00")), null)
        )
        vm.resumeAfterLogin()

        assertEquals(2, repository.mineCalls)
        assertEquals(setOf(nov20), vm.uiState.value.selectedDates)
    }
}
