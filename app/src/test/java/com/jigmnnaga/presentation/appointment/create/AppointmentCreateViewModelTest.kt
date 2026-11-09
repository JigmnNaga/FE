package com.jigmnnaga.presentation.appointment.create

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppointmentCreateViewModelTest {
    @Test
    fun blankAndWhitespaceNamesCannotAdvance() {
        val viewModel = AppointmentCreateViewModel()

        viewModel.updateUiState(viewModel.uiState.value.copy(appointmentName = "   \n  "))

        assertFalse(viewModel.moveNext())
        assertEquals(AppointmentCreateStep.APPOINTMENT_NAME, viewModel.uiState.value.step)
    }

    @Test
    fun validNameAdvancesAndPreviousKeepsInput() {
        val viewModel = AppointmentCreateViewModel()
        viewModel.updateUiState(viewModel.uiState.value.copy(appointmentName = "  저녁 모임  "))

        assertTrue(viewModel.moveNext())
        assertEquals(AppointmentCreateStep.SCHEDULE, viewModel.uiState.value.step)
        viewModel.updateUiState(
            viewModel.uiState.value.copy(
                scheduleType = AppointmentScheduleType.SINGLE_DAY,
                selectedStartDate = AppointmentDate(2026, 11, 14)
            )
        )
        assertTrue(viewModel.moveNext())
        assertEquals(AppointmentCreateStep.LOCATIONS, viewModel.uiState.value.step)

        assertTrue(viewModel.movePrevious())
        assertEquals(AppointmentCreateStep.SCHEDULE, viewModel.uiState.value.step)
        assertEquals("  저녁 모임  ", viewModel.uiState.value.appointmentName)
        assertEquals(AppointmentDate(2026, 11, 14), viewModel.uiState.value.selectedStartDate)
    }

    @Test
    fun invalidDateRangeCannotAdvanceAndSpecificDatesAreNormalized() {
        val viewModel = AppointmentCreateViewModel()
        viewModel.updateUiState(
            viewModel.uiState.value.copy(
                step = AppointmentCreateStep.SCHEDULE,
                scheduleType = AppointmentScheduleType.DATE_RANGE,
                selectedStartDate = AppointmentDate(2026, 11, 14),
                selectedEndDate = AppointmentDate(2026, 11, 14)
            )
        )
        assertFalse(viewModel.moveNext())
        viewModel.updateUiState(
            viewModel.uiState.value.copy(selectedEndDate = AppointmentDate(2026, 11, 13))
        )
        assertFalse(viewModel.moveNext())

        viewModel.updateUiState(
            viewModel.uiState.value.copy(
                scheduleType = AppointmentScheduleType.SPECIFIC_DATES,
                selectedDates = listOf(
                    AppointmentDate(2026, 11, 20),
                    AppointmentDate(2026, 11, 5),
                    AppointmentDate(2026, 11, 20)
                )
            )
        )
        assertTrue(viewModel.moveNext())
        assertEquals(
            listOf(AppointmentDate(2026, 11, 5), AppointmentDate(2026, 11, 20)),
            viewModel.uiState.value.selectedDates
        )
    }

    @Test
    fun emptySpecificDatesInvalidCalendarDateAndMissingDeadlineCannotAdvance() {
        val viewModel = AppointmentCreateViewModel()
        viewModel.updateUiState(
            viewModel.uiState.value.copy(
                step = AppointmentCreateStep.SCHEDULE,
                scheduleType = AppointmentScheduleType.SPECIFIC_DATES,
                selectedDates = emptyList()
            )
        )
        assertFalse(viewModel.moveNext())

        viewModel.updateUiState(
            viewModel.uiState.value.copy(
                scheduleType = AppointmentScheduleType.SINGLE_DAY,
                selectedStartDate = AppointmentDate(2026, 2, 30)
            )
        )
        assertFalse(viewModel.moveNext())

        viewModel.updateUiState(
            viewModel.uiState.value.copy(
                step = AppointmentCreateStep.RESPONSE_DEADLINE,
                appointmentName = "모임",
                scheduleType = AppointmentScheduleType.SINGLE_DAY,
                selectedStartDate = AppointmentDate(2026, 2, 28),
                selectedLocations = listOf(AppointmentLocation("강남역", "주소")),
                deadlineChoice = null
            )
        )
        assertNull(viewModel.buildDraft())
        assertFalse(viewModel.uiState.value.canContinue())

        viewModel.updateUiState(
            viewModel.uiState.value.copy(
                deadlineChoice = AppointmentDeadlineChoice.CUSTOM_DATE,
                customDeadline = AppointmentDate(2026, 2, 30)
            )
        )
        assertNull(viewModel.buildDraft())
    }

    @Test
    fun leapYearFebruaryDateIsValidatedCorrectly() {
        val validLeapDay = AppointmentCreateUiState(
            step = AppointmentCreateStep.RESPONSE_DEADLINE,
            appointmentName = "모임",
            scheduleType = AppointmentScheduleType.SINGLE_DAY,
            selectedStartDate = AppointmentDate(2024, 2, 29),
            selectedLocations = listOf(AppointmentLocation("강남역", "주소")),
            deadlineChoice = AppointmentDeadlineChoice.ONE_DAY
        )

        assertTrue(validLeapDay.toDraftOrNull() != null)
        assertNull(validLeapDay.copy(selectedStartDate = AppointmentDate(2026, 2, 29)).toDraftOrNull())
    }

    @Test
    fun locationsAreCappedAndCanBeEditedAfterReturning() {
        val viewModel = AppointmentCreateViewModel()
        val locations = (1..6).map { AppointmentLocation("장소 $it", "주소 $it") }
        viewModel.updateUiState(
            viewModel.uiState.value.copy(
                step = AppointmentCreateStep.LOCATIONS,
                selectedLocations = locations
            )
        )

        assertEquals(5, viewModel.uiState.value.selectedLocations.size)
        assertTrue(viewModel.moveNext())
        assertEquals(AppointmentCreateStep.RESPONSE_DEADLINE, viewModel.uiState.value.step)
        assertTrue(viewModel.movePrevious())
        assertEquals(5, viewModel.uiState.value.selectedLocations.size)
    }

    @Test
    fun createPreparesTrimmedDraftButDoesNotPretendBackendSucceeded() {
        val viewModel = AppointmentCreateViewModel()
        viewModel.updateUiState(
            viewModel.uiState.value.copy(
                step = AppointmentCreateStep.RESPONSE_DEADLINE,
                appointmentName = "  저녁 모임 ",
                scheduleType = AppointmentScheduleType.DATE_RANGE,
                selectedStartDate = AppointmentDate(2026, 11, 10),
                selectedEndDate = AppointmentDate(2026, 11, 12),
                selectedLocations = listOf(AppointmentLocation("강남역", "서울")),
                deadlineChoice = AppointmentDeadlineChoice.THREE_DAYS
            )
        )

        val draft = viewModel.buildDraft()
        assertEquals("저녁 모임", draft?.name)
        assertEquals(AppointmentScheduleType.DATE_RANGE, draft?.scheduleType)
        assertNull(viewModel.uiState.value.inviteLink)
        viewModel.requestCreate(requireNotNull(draft).copy(name = "  저녁 모임 "))

        assertEquals(draft, viewModel.pendingDraft.value)
        assertEquals(AppointmentCreateNotice.CREATE_API_NOT_CONNECTED, viewModel.notice.value)
        assertEquals(AppointmentCreateStep.RESPONSE_DEADLINE, viewModel.uiState.value.step)
        assertNull(viewModel.uiState.value.inviteLink)
    }

    @Test
    fun createDraftIsPreparedOnlyOnceAndUnavailableStateDoesNotSpinOrClaimSuccess() {
        val viewModel = AppointmentCreateViewModel()
        viewModel.updateUiState(
            viewModel.uiState.value.copy(
                step = AppointmentCreateStep.RESPONSE_DEADLINE,
                appointmentName = "모임",
                scheduleType = AppointmentScheduleType.SINGLE_DAY,
                selectedStartDate = AppointmentDate(2026, 11, 14),
                selectedLocations = listOf(AppointmentLocation("강남역", "서울")),
                deadlineChoice = AppointmentDeadlineChoice.ONE_DAY
            )
        )
        val draft = requireNotNull(viewModel.buildDraft())

        viewModel.requestCreate(draft)
        viewModel.requestCreate(draft)

        assertEquals(draft, viewModel.pendingDraft.value)
        assertEquals(AppointmentCreateSubmissionStatus.API_UNAVAILABLE, viewModel.uiState.value.submissionStatus)
        assertFalse(viewModel.uiState.value.isCreating)
        assertFalse(viewModel.uiState.value.canContinue())
        assertNull(viewModel.uiState.value.inviteLink)
        assertEquals(AppointmentCreateNotice.CREATE_API_NOT_CONNECTED, viewModel.notice.value)

        viewModel.updateUiState(viewModel.uiState.value.copy(appointmentName = "수정한 모임"))
        assertNull(viewModel.pendingDraft.value)
        assertEquals(AppointmentCreateSubmissionStatus.READY, viewModel.uiState.value.submissionStatus)
    }

    @Test
    fun requestCreateRejectsInvalidStateEvenWhenCallerSuppliesAValidLookingDraft() {
        val viewModel = AppointmentCreateViewModel()
        viewModel.updateUiState(
            viewModel.uiState.value.copy(
                step = AppointmentCreateStep.RESPONSE_DEADLINE,
                appointmentName = "모임",
                scheduleType = AppointmentScheduleType.SINGLE_DAY,
                selectedStartDate = AppointmentDate(2026, 2, 30),
                selectedLocations = listOf(AppointmentLocation("강남역", "서울")),
                deadlineChoice = AppointmentDeadlineChoice.ONE_DAY
            )
        )
        assertFalse(viewModel.uiState.value.isFormValid())
        val callerDraft = AppointmentCreateDraft(
            name = "모임",
            scheduleType = AppointmentScheduleType.SINGLE_DAY,
            startDate = AppointmentDate(2026, 2, 30),
            endDate = null,
            specificDates = emptyList(),
            locations = listOf(AppointmentLocation("강남역", "서울")),
            deadlineChoice = AppointmentDeadlineChoice.ONE_DAY,
            customDeadline = null
        )

        viewModel.requestCreate(callerDraft)

        assertNull(viewModel.pendingDraft.value)
        assertEquals(AppointmentCreateSubmissionStatus.READY, viewModel.uiState.value.submissionStatus)
        assertNull(viewModel.notice.value)
    }

    @Test
    fun onlyBackendSuppliedNonblankLinkEntersInvitationState() {
        val viewModel = AppointmentCreateViewModel()
        viewModel.onInviteLinkIssued("  ")
        assertNull(viewModel.uiState.value.inviteLink)
        assertEquals(AppointmentCreateStep.APPOINTMENT_NAME, viewModel.uiState.value.step)
        assertEquals(AppointmentCreateNotice.INVITE_LINK_UNAVAILABLE, viewModel.notice.value)

        val backendLink = "https://example.invalid/issued-by-test-server"
        viewModel.onInviteLinkIssued(backendLink)
        assertEquals(AppointmentCreateStep.INVITATION, viewModel.uiState.value.step)
        assertEquals(backendLink, viewModel.uiState.value.inviteLink)
    }
}
