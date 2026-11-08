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
    val inviteLink: String? = null,
    val copyConfirmationVisible: Boolean = false
)
