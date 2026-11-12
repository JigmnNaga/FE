package com.jigmnnaga.presentation.appointment.schedule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppointmentDateTimeViewModelTest {
    private val nov14 = AppointmentDateValue(2026, 11, 14)
    private val nov15 = AppointmentDateValue(2026, 11, 15)

    private val members = listOf(
        MemberResponseUiState("1", "태양", MemberResponseStatus.COMPLETED),
        MemberResponseUiState("2", "나", MemberResponseStatus.PENDING, isMe = true)
    )

    private fun viewModel() = AppointmentDateTimeViewModel(
        appointmentTitle = "중간고사 카공 모임",
        initialMonth = AppointmentCalendarMonth(2026, 11),
        memberResponses = members
    )

    private fun AppointmentDateTimeViewModel.goToTimes(vararg dates: AppointmentDateValue) {
        dates.forEach { onDateToggle(it) }
        onNext()
    }

    private fun time(hour: Int, minute: Int = 0) = AppointmentTimeValue(hour, minute)

    @Test
    fun dateToggleSelectsAndDeselects() {
        val vm = viewModel()

        vm.onDateToggle(nov14)
        assertEquals(setOf(nov14), vm.uiState.value.selectedDates)
        assertTrue(vm.uiState.value.canAdvance)

        vm.onDateToggle(nov14)
        assertTrue(vm.uiState.value.selectedDates.isEmpty())
        assertFalse(vm.uiState.value.canAdvance)
    }

    @Test
    fun monthChangeKeepsSelectedDates() {
        val vm = viewModel()
        vm.onDateToggle(nov14)

        vm.onMonthChange(AppointmentCalendarMonth(2026, 12))

        assertEquals(AppointmentCalendarMonth(2026, 12), vm.uiState.value.displayedMonth)
        assertEquals(setOf(nov14), vm.uiState.value.selectedDates)
    }

    @Test
    fun nextWithoutDatesStaysOnDatesStep() {
        val vm = viewModel()

        vm.onNext()

        assertEquals(AppointmentDateTimeStep.DATES, vm.uiState.value.step)
    }

    @Test
    fun nextMovesToTimesAndActivatesEarliestDate() {
        val vm = viewModel()

        vm.goToTimes(nov15, nov14)

        assertEquals(AppointmentDateTimeStep.TIMES, vm.uiState.value.step)
        assertEquals(nov14, vm.uiState.value.activeDate)
        assertFalse(vm.uiState.value.canAdvance)
    }

    @Test
    fun quickOptionsSetRangeAndMergeOnlyWhenContiguous() {
        val vm = viewModel()
        vm.goToTimes(nov14)

        vm.onQuickTimeClick("morning")
        assertEquals(AppointmentTimeRange(time(9), time(12)), vm.uiState.value.selectedTimeRange)

        vm.onQuickTimeClick("afternoon")
        assertEquals(AppointmentTimeRange(time(9), time(17)), vm.uiState.value.selectedTimeRange)
        assertEquals(setOf("morning", "afternoon"), vm.uiState.value.selectedQuickTimeIds)
    }

    @Test
    fun nonContiguousQuickOptionReplacesSelection() {
        val vm = viewModel()
        vm.goToTimes(nov14)
        vm.onQuickTimeClick("morning")

        vm.onQuickTimeClick("evening")

        assertEquals(setOf("evening"), vm.uiState.value.selectedQuickTimeIds)
        assertEquals(AppointmentTimeRange(time(17), time(21)), vm.uiState.value.selectedTimeRange)
    }

    @Test
    fun tappingSelectedQuickOptionAgainClearsRange() {
        val vm = viewModel()
        vm.goToTimes(nov14)
        vm.onQuickTimeClick("morning")

        vm.onQuickTimeClick("morning")

        assertNull(vm.uiState.value.selectedTimeRange)
        assertTrue(vm.uiState.value.selectedQuickTimeIds.isEmpty())
        assertFalse(vm.uiState.value.canAdvance)
    }

    @Test
    fun startOnlyCreatesOneHourRange() {
        val vm = viewModel()
        vm.goToTimes(nov14)

        vm.onStartTimeClick()
        assertEquals(TimePickerTarget.START, vm.uiState.value.timePicker?.target)
        assertEquals(time(9), vm.uiState.value.timePicker?.selectedTime)
        vm.onTimePickerConfirm(time(14))

        assertEquals(AppointmentTimeRange(time(14), time(15)), vm.uiState.value.selectedTimeRange)
        assertNull(vm.uiState.value.timePicker)
    }

    @Test
    fun endOnlyCreatesOneHourRange() {
        val vm = viewModel()
        vm.goToTimes(nov14)

        vm.onEndTimeClick()
        vm.onTimePickerConfirm(time(10))

        assertEquals(AppointmentTimeRange(time(9), time(10)), vm.uiState.value.selectedTimeRange)
    }

    @Test
    fun endBeforeStartShowsErrorAndKeepsRange() {
        val vm = viewModel()
        vm.goToTimes(nov14)
        vm.onQuickTimeClick("afternoon")

        vm.onEndTimeClick()
        vm.onTimePickerConfirm(time(11))

        assertEquals(AppointmentDateTimeViewModel.ERROR_TIME_ORDER, vm.uiState.value.errorMessage)
        assertEquals(AppointmentTimeRange(time(12), time(17)), vm.uiState.value.selectedTimeRange)
        assertNull(vm.uiState.value.timePicker)
    }

    @Test
    fun startAtLastMinuteIsRejected() {
        val vm = viewModel()
        vm.goToTimes(nov14)

        vm.onStartTimeClick()
        vm.onTimePickerConfirm(time(23, 59))

        assertEquals(AppointmentDateTimeViewModel.ERROR_TIME_ORDER, vm.uiState.value.errorMessage)
        assertNull(vm.uiState.value.selectedTimeRange)
    }

    @Test
    fun manualTimeEditClearsQuickSelection() {
        val vm = viewModel()
        vm.goToTimes(nov14)
        vm.onQuickTimeClick("afternoon")

        vm.onStartTimeClick()
        vm.onTimePickerConfirm(time(13))

        assertEquals(AppointmentTimeRange(time(13), time(17)), vm.uiState.value.selectedTimeRange)
        assertTrue(vm.uiState.value.selectedQuickTimeIds.isEmpty())
    }

    @Test
    fun pickerDismissKeepsExistingRange() {
        val vm = viewModel()
        vm.goToTimes(nov14)
        vm.onQuickTimeClick("morning")

        vm.onStartTimeClick()
        vm.onTimePickerValueChange(time(10))
        assertEquals(time(10), vm.uiState.value.timePicker?.selectedTime)
        vm.onTimePickerDismiss()

        assertNull(vm.uiState.value.timePicker)
        assertEquals(AppointmentTimeRange(time(9), time(12)), vm.uiState.value.selectedTimeRange)
    }

    @Test
    fun timesAreKeptPerDateAndAllDatesNeedOne() {
        val vm = viewModel()
        vm.goToTimes(nov14, nov15)

        vm.onQuickTimeClick("morning")
        assertFalse(vm.uiState.value.canAdvance)

        vm.onActiveDateSelect(nov15)
        assertNull(vm.uiState.value.selectedTimeRange)
        vm.onQuickTimeClick("evening")
        assertTrue(vm.uiState.value.canAdvance)

        vm.onActiveDateSelect(nov14)
        assertEquals(AppointmentTimeRange(time(9), time(12)), vm.uiState.value.selectedTimeRange)
    }

    @Test
    fun goingBackKeepsDatesAndTimes() {
        val vm = viewModel()
        vm.goToTimes(nov14)
        vm.onQuickTimeClick("morning")

        assertTrue(vm.movePrevious())
        assertEquals(AppointmentDateTimeStep.DATES, vm.uiState.value.step)
        assertEquals(setOf(nov14), vm.uiState.value.selectedDates)

        vm.onNext()
        assertEquals(AppointmentTimeRange(time(9), time(12)), vm.uiState.value.selectedTimeRange)
        assertFalse(viewModel().movePrevious())
    }

    @Test
    fun removingDateDropsItsTime() {
        val vm = viewModel()
        vm.goToTimes(nov14)
        vm.onQuickTimeClick("morning")
        vm.movePrevious()

        vm.onDateToggle(nov14)
        vm.onDateToggle(nov14)
        vm.onNext()

        assertNull(vm.uiState.value.selectedTimeRange)
        assertFalse(vm.uiState.value.canAdvance)
    }

    @Test
    fun nextOnTimesStoresDraftWithoutCompleting() {
        val vm = viewModel()
        vm.goToTimes(nov15, nov14)
        vm.onQuickTimeClick("morning")
        vm.onActiveDateSelect(nov15)
        vm.onQuickTimeClick("evening")

        vm.onNext()

        assertEquals(AppointmentDateTimeStep.TIMES, vm.uiState.value.step)
        assertEquals(AppointmentDateTimeViewModel.ERROR_API_NOT_CONNECTED, vm.uiState.value.errorMessage)
        assertEquals(
            AppointmentScheduleResponseDraft(
                listOf(
                    AppointmentScheduleEntry(nov14, AppointmentTimeRange(time(9), time(12))),
                    AppointmentScheduleEntry(nov15, AppointmentTimeRange(time(17), time(21)))
                )
            ),
            vm.pendingResponse.value
        )
    }

    @Test
    fun editingAfterDraftClearsErrorAndDraft() {
        val vm = viewModel()
        vm.goToTimes(nov14)
        vm.onQuickTimeClick("morning")
        vm.onNext()
        assertNotNull(vm.pendingResponse.value)

        vm.onQuickTimeClick("afternoon")

        assertNull(vm.pendingResponse.value)
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun submittedResponseCompletesAndMarksMe() {
        val vm = viewModel()
        vm.goToTimes(nov14)
        vm.onQuickTimeClick("morning")

        vm.onResponseSubmitted()

        assertEquals(AppointmentDateTimeStep.COMPLETE, vm.uiState.value.step)
        assertTrue(vm.uiState.value.pendingMembers.isEmpty())
        assertEquals(listOf("태양", "나"), vm.uiState.value.completionMemberLabels)
        assertNull(vm.pendingResponse.value)
    }

    @Test
    fun submittedBeforeTimesStepIsIgnored() {
        val vm = viewModel()

        vm.onResponseSubmitted()

        assertEquals(AppointmentDateTimeStep.DATES, vm.uiState.value.step)
    }

    @Test
    fun statusToggleAndMemberUpdateAreReflected() {
        val vm = viewModel()

        vm.onStatusToggle()
        assertTrue(vm.uiState.value.isStatusExpanded)

        vm.updateMemberResponses(
            listOf(MemberResponseUiState("3", "하은", MemberResponseStatus.COMPLETED))
        )
        assertEquals(listOf("하은"), vm.uiState.value.completedMembers.map { it.name })
    }
}