package com.jigmnnaga.presentation.appointment.schedule

import java.util.Calendar
import java.util.Locale

enum class AppointmentDateTimeStep {
    DATES,
    TIMES,
    COMPLETE
}

enum class TimePickerTarget {
    START,
    END
}

data class AppointmentCalendarMonth(
    val year: Int,
    val month: Int
) {
    init {
        require(month in 1..12)
    }

    fun dayCount(): Int = Calendar.getInstance().apply {
        set(year, month - 1, 1)
    }.getActualMaximum(Calendar.DAY_OF_MONTH)

    fun firstWeekdayOffset(): Int = Calendar.getInstance().apply {
        set(year, month - 1, 1)
    }.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY

    fun label(): String = String.format(Locale.KOREA, "%d년 %d월", year, month)
}

data class AppointmentDateValue(
    val year: Int,
    val month: Int,
    val day: Int
) {
    init {
        require(month in 1..12)
        require(day in 1..31)
    }

    fun shortLabel(): String = "$month/$day"

    fun accessibilityLabel(): String = "${year}년 ${month}월 ${day}일"
}

data class AppointmentTimeValue(
    val hour: Int,
    val minute: Int
) {
    init {
        require(hour in 0..23)
        require(minute in 0..59)
    }

    fun label(): String = String.format(Locale.KOREA, "%02d:%02d", hour, minute)
}

data class AppointmentTimeRange(
    val start: AppointmentTimeValue,
    val end: AppointmentTimeValue
)

data class QuickTimeOptionUiState(
    val id: String,
    val label: String,
    val rangeLabel: String
)

data class TimePickerUiState(
    val target: TimePickerTarget,
    val selectedTime: AppointmentTimeValue
)

/** UI-only state. Availability and selection rules are supplied by the owning feature. */
data class AppointmentDateTimeUiState(
    val step: AppointmentDateTimeStep = AppointmentDateTimeStep.DATES,
    val appointmentTitle: String,
    val completionStatusLabel: String,
    val displayedMonth: AppointmentCalendarMonth,
    val selectedDates: Set<AppointmentDateValue> = emptySet(),
    val activeDate: AppointmentDateValue? = null,
    val selectedTimeRange: AppointmentTimeRange? = null,
    val quickTimeOptions: List<QuickTimeOptionUiState> = emptyList(),
    val selectedQuickTimeIds: Set<String> = emptySet(),
    val completionMemberLabels: List<String> = emptyList(),
    val timePicker: TimePickerUiState? = null,
    val canAdvance: Boolean = false,
    val errorMessage: String? = null
)
