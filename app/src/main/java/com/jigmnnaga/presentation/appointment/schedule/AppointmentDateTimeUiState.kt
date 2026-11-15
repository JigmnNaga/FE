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

    /** 서버에서 쓰는 `yyyy-MM-dd` 형식. */
    fun toIsoString(): String = String.format(Locale.US, "%04d-%02d-%02d", year, month, day)

    companion object {
        /** `yyyy-MM-dd` 문자열을 날짜로 바꾼다. 형식이 맞지 않거나 없는 날짜면 null. */
        fun parseIso(value: String): AppointmentDateValue? {
            val parts = value.trim().split("-")
            if (parts.size != 3) return null
            val year = parts[0].toIntOrNull() ?: return null
            val month = parts[1].toIntOrNull()?.takeIf { it in 1..12 } ?: return null
            val day = parts[2].toIntOrNull()?.takeIf { it in 1..31 } ?: return null
            return AppointmentDateValue(year, month, day)
        }
    }
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

    companion object {
        /** `HH:mm` 문자열을 시각으로 바꾼다. 형식이 맞지 않으면 null. */
        fun parse(value: String): AppointmentTimeValue? {
            val parts = value.trim().split(":")
            if (parts.size < 2) return null
            val hour = parts[0].toIntOrNull()?.takeIf { it in 0..23 } ?: return null
            val minute = parts[1].toIntOrNull()?.takeIf { it in 0..59 } ?: return null
            return AppointmentTimeValue(hour, minute)
        }
    }
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

enum class MemberResponseStatus {
    COMPLETED,
    PENDING
}

data class MemberResponseUiState(
    val id: String,
    val name: String,
    val status: MemberResponseStatus,
    val isMe: Boolean = false
) {
    val initial: String get() = name.take(1)
}

/** UI-only state. Availability and selection rules are supplied by the owning feature. */
data class AppointmentDateTimeUiState(
    val step: AppointmentDateTimeStep = AppointmentDateTimeStep.DATES,
    val appointmentTitle: String,
    val displayedMonth: AppointmentCalendarMonth,
    val selectedDates: Set<AppointmentDateValue> = emptySet(),
    val activeDate: AppointmentDateValue? = null,
    val selectedTimeRange: AppointmentTimeRange? = null,
    val quickTimeOptions: List<QuickTimeOptionUiState> = emptyList(),
    val selectedQuickTimeIds: Set<String> = emptySet(),
    val completionMemberLabels: List<String> = emptyList(),
    val memberResponses: List<MemberResponseUiState> = emptyList(),
    val isStatusExpanded: Boolean = false,
    val timePicker: TimePickerUiState? = null,
    val canAdvance: Boolean = false,
    val errorMessage: String? = null,
    /** 고를 수 있는 날짜. null이면 제한이 없고, 값이 있으면 그 날짜만 고를 수 있다(약속의 후보 날짜). */
    val availableDates: Set<AppointmentDateValue>? = null,
    /** 서버에 일정을 보내는 중. */
    val isSubmitting: Boolean = false
) {
    val completedMembers: List<MemberResponseUiState>
        get() = memberResponses.filter { it.status == MemberResponseStatus.COMPLETED }

    val pendingMembers: List<MemberResponseUiState>
        get() = memberResponses.filter { it.status == MemberResponseStatus.PENDING }
}