package com.jigmnnaga.presentation.appointment.finalconfirmation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppointmentFinalConfirmationViewModelTest {

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

    @Test
    fun `참석 여부를 고르기 전에는 확인할 수 없다`() {
        val vm = AppointmentFinalConfirmationViewModel(state())
        assertFalse(vm.uiState.value.canConfirm)
    }

    @Test
    fun `참석 여부를 고르면 확인할 수 있다`() {
        val vm = AppointmentFinalConfirmationViewModel(state())
        vm.onAttendanceChoiceChange(AppointmentAttendanceChoice.ATTENDING)
        assertEquals(AppointmentAttendanceChoice.ATTENDING, vm.uiState.value.attendanceChoice)
        assertTrue(vm.uiState.value.canConfirm)
    }

    @Test
    fun `선택을 바꾸면 마지막 선택이 남는다`() {
        val vm = AppointmentFinalConfirmationViewModel(state())
        vm.onAttendanceChoiceChange(AppointmentAttendanceChoice.ATTENDING)
        vm.onAttendanceChoiceChange(AppointmentAttendanceChoice.NOT_ATTENDING)
        assertEquals(AppointmentAttendanceChoice.NOT_ATTENDING, vm.uiState.value.attendanceChoice)
    }

    @Test
    fun `처음부터 선택돼 있으면 바로 확인할 수 있다`() {
        val vm = AppointmentFinalConfirmationViewModel(state(choice = AppointmentAttendanceChoice.ATTENDING))
        assertTrue(vm.uiState.value.canConfirm)
    }

    @Test
    fun `처리 중에는 선택을 바꾸거나 확인할 수 없다`() {
        val vm = AppointmentFinalConfirmationViewModel(
            state(choice = AppointmentAttendanceChoice.ATTENDING, isSubmitting = true)
        )
        assertFalse(vm.uiState.value.canConfirm)
        vm.onAttendanceChoiceChange(AppointmentAttendanceChoice.NOT_ATTENDING)
        assertEquals(AppointmentAttendanceChoice.ATTENDING, vm.uiState.value.attendanceChoice)
        vm.onConfirm()
        assertNull(vm.notice.value)
    }

    @Test
    fun `확인을 누르면 서버로 보내지 않고 안내만 보내며 선택값은 유지한다`() {
        val vm = AppointmentFinalConfirmationViewModel(state())
        vm.onAttendanceChoiceChange(AppointmentAttendanceChoice.NOT_ATTENDING)

        vm.onConfirm()

        assertEquals(AppointmentFinalConfirmationNotice.CONFIRM_API_NOT_CONNECTED, vm.notice.value)
        assertEquals(AppointmentAttendanceChoice.NOT_ATTENDING, vm.uiState.value.attendanceChoice)
        vm.clearNotice()
        assertNull(vm.notice.value)
    }

    @Test
    fun `선택하지 않고 확인을 눌러도 안내를 보내지 않는다`() {
        val vm = AppointmentFinalConfirmationViewModel(state())
        vm.onConfirm()
        assertNull(vm.notice.value)
    }
}
