package com.jigmnnaga.presentation.appointment.finalconfirmation

import com.jigmnnaga.domain.model.AttendanceAnswer
import com.jigmnnaga.domain.model.AttendanceRejectReason
import com.jigmnnaga.domain.model.AttendanceResult
import com.jigmnnaga.domain.repository.AttendanceRepository
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
class AppointmentFinalConfirmationViewModelTest {

    private class FakeAttendanceRepository(
        var result: AttendanceResult<Unit> = AttendanceResult.Success(Unit)
    ) : AttendanceRepository {
        val calls = mutableListOf<Pair<Long, AttendanceAnswer>>()

        override suspend fun respond(
            appointmentId: Long,
            answer: AttendanceAnswer
        ): AttendanceResult<Unit> {
            calls += appointmentId to answer
            return result
        }
    }

    private val repository = FakeAttendanceRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun state(
        choice: AppointmentAttendanceChoice? = null,
        isSubmitting: Boolean = false
    ) = AppointmentFinalConfirmationUiState(
        appointmentTitle = "중간고사 카공 모임",
        dateLabel = "11월 20일 (금)",
        timeLabel = "오후 7:00",
        placeName = "강남역",
        attendanceChoice = choice,
        isSubmitting = isSubmitting
    )

    private fun viewModel(initial: AppointmentFinalConfirmationUiState = state()) =
        AppointmentFinalConfirmationViewModel(7L, initial, repository)

    @Test
    fun `참석 여부를 고르기 전에는 확인할 수 없다`() {
        assertFalse(viewModel().uiState.value.canConfirm)
    }

    @Test
    fun `참석 여부를 고르면 확인할 수 있다`() {
        val vm = viewModel()
        vm.onAttendanceChoiceChange(AppointmentAttendanceChoice.ATTENDING)
        assertEquals(AppointmentAttendanceChoice.ATTENDING, vm.uiState.value.attendanceChoice)
        assertTrue(vm.uiState.value.canConfirm)
    }

    @Test
    fun `선택을 바꾸면 마지막 선택이 남는다`() {
        val vm = viewModel()
        vm.onAttendanceChoiceChange(AppointmentAttendanceChoice.ATTENDING)
        vm.onAttendanceChoiceChange(AppointmentAttendanceChoice.NOT_ATTENDING)
        assertEquals(AppointmentAttendanceChoice.NOT_ATTENDING, vm.uiState.value.attendanceChoice)
    }

    @Test
    fun `처음부터 선택돼 있으면 바로 확인할 수 있다`() {
        val vm = viewModel(state(choice = AppointmentAttendanceChoice.ATTENDING))
        assertTrue(vm.uiState.value.canConfirm)
    }

    @Test
    fun `처리 중에는 선택을 바꾸거나 확인할 수 없다`() {
        val vm = viewModel(state(choice = AppointmentAttendanceChoice.ATTENDING, isSubmitting = true))
        assertFalse(vm.uiState.value.canConfirm)
        vm.onAttendanceChoiceChange(AppointmentAttendanceChoice.NOT_ATTENDING)
        assertEquals(AppointmentAttendanceChoice.ATTENDING, vm.uiState.value.attendanceChoice)
        vm.onConfirm()
        assertTrue(repository.calls.isEmpty())
        assertNull(vm.notice.value)
    }

    @Test
    fun `선택하지 않고 확인을 눌러도 서버로 보내지 않는다`() {
        val vm = viewModel()
        vm.onConfirm()
        assertTrue(repository.calls.isEmpty())
        assertNull(vm.notice.value)
    }

    @Test
    fun `참석을 확인하면 참석 응답을 보내고 성공 알림을 보낸다`() {
        val vm = viewModel(state(choice = AppointmentAttendanceChoice.ATTENDING))
        vm.onConfirm()
        assertEquals(listOf(7L to AttendanceAnswer.ATTENDING), repository.calls)
        assertEquals(AppointmentFinalConfirmationNotice.SUBMITTED, vm.notice.value)
        assertFalse(vm.uiState.value.isSubmitting)
    }

    @Test
    fun `불참을 확인하면 불참 응답을 보낸다`() {
        val vm = viewModel(state(choice = AppointmentAttendanceChoice.NOT_ATTENDING))
        vm.onConfirm()
        assertEquals(listOf(7L to AttendanceAnswer.NOT_ATTENDING), repository.calls)
    }

    @Test
    fun `로그인이 필요하면 로그인 알림을 보내고 선택값을 유지한다`() {
        repository.result = AttendanceResult.LoginRequired
        val vm = viewModel(state(choice = AppointmentAttendanceChoice.ATTENDING))
        vm.onConfirm()
        assertEquals(AppointmentFinalConfirmationNotice.LOGIN_REQUIRED, vm.notice.value)
        assertEquals(AppointmentAttendanceChoice.ATTENDING, vm.uiState.value.attendanceChoice)
        assertTrue(vm.uiState.value.canConfirm)
    }

    @Test
    fun `참여자가 아니면 해당 알림을 보낸다`() {
        repository.result = AttendanceResult.NotParticipant
        val vm = viewModel(state(choice = AppointmentAttendanceChoice.ATTENDING))
        vm.onConfirm()
        assertEquals(AppointmentFinalConfirmationNotice.NOT_PARTICIPANT, vm.notice.value)
    }

    @Test
    fun `서버가 거절한 이유별로 알림을 나눈다`() {
        val expected = mapOf(
            AttendanceRejectReason.APPOINTMENT_CANCELED to AppointmentFinalConfirmationNotice.APPOINTMENT_CANCELED,
            AttendanceRejectReason.NOT_CONFIRMED to AppointmentFinalConfirmationNotice.NOT_CONFIRMED,
            AttendanceRejectReason.TIME_PASSED to AppointmentFinalConfirmationNotice.TIME_PASSED,
            AttendanceRejectReason.INVALID_ANSWER to AppointmentFinalConfirmationNotice.INVALID_ANSWER
        )
        expected.forEach { (reason, notice) ->
            repository.result = AttendanceResult.Rejected(reason)
            val vm = viewModel(state(choice = AppointmentAttendanceChoice.ATTENDING))
            vm.onConfirm()
            assertEquals(notice, vm.notice.value)
        }
    }

    @Test
    fun `실패하면 다시 시도할 수 있는지에 따라 알림을 나누고 다시 누를 수 있다`() {
        repository.result = AttendanceResult.Failure(retryable = true)
        val vm = viewModel(state(choice = AppointmentAttendanceChoice.ATTENDING))
        vm.onConfirm()
        assertEquals(AppointmentFinalConfirmationNotice.FAILURE_RETRYABLE, vm.notice.value)
        assertTrue(vm.uiState.value.canConfirm)

        repository.result = AttendanceResult.Failure(retryable = false)
        vm.clearNotice()
        vm.onConfirm()
        assertEquals(AppointmentFinalConfirmationNotice.FAILURE, vm.notice.value)
        assertEquals(2, repository.calls.size)
    }

    @Test
    fun `알림을 지우면 비워진다`() {
        val vm = viewModel(state(choice = AppointmentAttendanceChoice.ATTENDING))
        vm.onConfirm()
        vm.clearNotice()
        assertNull(vm.notice.value)
    }
}
