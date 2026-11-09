package com.jigmnnaga.presentation.appointment.create

import java.util.Calendar

/** The five visible states in the APT-001 creation flow. */
enum class AppointmentCreateStep {
    APPOINTMENT_NAME,
    SCHEDULE,
    LOCATIONS,
    RESPONSE_DEADLINE,
    INVITATION
}

enum class AppointmentScheduleType {
    SINGLE_DAY,
    DATE_RANGE,
    SPECIFIC_DATES
}

enum class AppointmentDeadlineChoice {
    ONE_DAY,
    THREE_DAYS,
    ONE_WEEK,
    CUSTOM_DATE
}

enum class AppointmentShareAction {
    KAKAO_TALK,
    COPY_LINK
}

/**
 * Local submission boundary. This flow deliberately stops at API_UNAVAILABLE until the #35 API
 * contract is connected; SUCCEEDED is only entered after an externally issued invite link arrives.
 */
enum class AppointmentCreateSubmissionStatus {
    READY,
    API_UNAVAILABLE,
    SUCCEEDED
}

/** A calendar date value that remains compatible with the app's minSdk 24. */
data class AppointmentDate(
    val year: Int,
    val month: Int,
    val day: Int
) : Comparable<AppointmentDate> {
    init {
        require(month in 1..12)
        require(day in 1..31)
    }

    override fun compareTo(other: AppointmentDate): Int =
        compareValuesBy(this, other, AppointmentDate::year, AppointmentDate::month, AppointmentDate::day)

    fun toCalendar(): Calendar = Calendar.getInstance().apply {
        clear()
        set(year, month - 1, day)
    }

    fun monthStart(): AppointmentDate = copy(day = 1)

    companion object {
        fun currentMonth(): AppointmentDate {
            val calendar = Calendar.getInstance()
            return AppointmentDate(
                year = calendar.get(Calendar.YEAR),
                month = calendar.get(Calendar.MONTH) + 1,
                day = 1
            )
        }
    }
}

data class AppointmentLocation(
    val name: String,
    val address: String
)

/** UI-only draft. It intentionally has no server ID or invite URI contract. */
data class AppointmentCreateDraft(
    val name: String,
    val scheduleType: AppointmentScheduleType,
    val startDate: AppointmentDate?,
    val endDate: AppointmentDate?,
    val specificDates: List<AppointmentDate>,
    val locations: List<AppointmentLocation>,
    val deadlineChoice: AppointmentDeadlineChoice,
    val customDeadline: AppointmentDate?
)

data class AppointmentCreateUiState(
    val step: AppointmentCreateStep = AppointmentCreateStep.APPOINTMENT_NAME,
    val appointmentName: String = "",
    val scheduleType: AppointmentScheduleType? = null,
    val displayedMonth: AppointmentDate = AppointmentDate.currentMonth(),
    val selectedStartDate: AppointmentDate? = null,
    val selectedEndDate: AppointmentDate? = null,
    val selectedDates: List<AppointmentDate> = emptyList(),
    val locationQuery: String = "",
    val locationCandidates: List<AppointmentLocation> = emptyList(),
    val selectedLocations: List<AppointmentLocation> = emptyList(),
    val deadlineChoice: AppointmentDeadlineChoice? = null,
    val customDeadline: AppointmentDate? = null,
    val isCreating: Boolean = false,
    val submissionStatus: AppointmentCreateSubmissionStatus = AppointmentCreateSubmissionStatus.READY,
    val inviteLink: String? = null,
    val copyConfirmationVisible: Boolean = false
) {
    /** Shared validation used by both the screen's enabled state and the ViewModel's guard. */
    fun isCurrentStepValid(): Boolean = when (step) {
        AppointmentCreateStep.APPOINTMENT_NAME -> appointmentName.isNotBlank()
        AppointmentCreateStep.SCHEDULE -> isScheduleValid()
        AppointmentCreateStep.LOCATIONS -> selectedLocations.distinct().size in 1..MAX_SELECTED_LOCATIONS
        AppointmentCreateStep.RESPONSE_DEADLINE -> isFormValid()
        AppointmentCreateStep.INVITATION -> false
    }

    /** Rechecks every required input before creating a draft, even if state was injected directly. */
    fun isFormValid(): Boolean = appointmentName.isNotBlank() &&
        isScheduleValid() &&
        selectedLocations.distinct().size in 1..MAX_SELECTED_LOCATIONS &&
        when (deadlineChoice) {
            AppointmentDeadlineChoice.CUSTOM_DATE -> customDeadline.isValid()
            null -> false
            else -> true
        }

    fun canContinue(): Boolean = isCurrentStepValid() &&
        !isCreating && submissionStatus == AppointmentCreateSubmissionStatus.READY

    fun toDraftOrNull(): AppointmentCreateDraft? {
        if (step != AppointmentCreateStep.RESPONSE_DEADLINE || !isFormValid()) return null
        return AppointmentCreateDraft(
            name = appointmentName.trim(),
            scheduleType = requireNotNull(scheduleType),
            startDate = selectedStartDate,
            endDate = selectedEndDate,
            specificDates = selectedDates.distinct().sorted(),
            locations = selectedLocations.distinct().take(MAX_SELECTED_LOCATIONS),
            deadlineChoice = requireNotNull(deadlineChoice),
            customDeadline = customDeadline
        )
    }

    private fun isScheduleValid(): Boolean = when (scheduleType) {
        AppointmentScheduleType.SINGLE_DAY -> selectedStartDate.isValid()
        AppointmentScheduleType.DATE_RANGE -> selectedStartDate.isValid() &&
            selectedEndDate.isValid() && selectedEndDate!! > selectedStartDate!!
        AppointmentScheduleType.SPECIFIC_DATES -> selectedDates.distinct().isNotEmpty() &&
            selectedDates.all { it.isValid() }
        null -> false
    }
}

internal const val MAX_SELECTED_LOCATIONS = 5

private fun AppointmentDate?.isValid(): Boolean {
    if (this == null) return false
    val maximumDay = when (month) {
        4, 6, 9, 11 -> 30
        2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
        else -> 31
    }
    return day <= maximumDay
}
