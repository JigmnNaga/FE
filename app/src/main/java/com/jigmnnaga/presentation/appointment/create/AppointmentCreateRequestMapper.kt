package com.jigmnnaga.presentation.appointment.create

import com.jigmnnaga.domain.model.NewAppointment
import com.jigmnnaga.domain.model.NewAppointmentDateType
import com.jigmnnaga.domain.model.NewCandidateLocation
import com.jigmnnaga.domain.model.PreviousMembers
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

private val SEOUL: TimeZone = TimeZone.getTimeZone("Asia/Seoul")
private const val MAX_RANGE_DAYS = 366

/**
 * 화면에서 만든 초안을 서버 요청으로 바꾼다. 다음 경우에는 요청을 만들 수 없어 null을 돌려준다.
 * - 후보 장소에 위치(위도·경도)가 없다.
 * - 기간이 너무 길거나 응답 기한을 정할 수 없다.
 *
 * @param nowMillis "24시간", "3일", "1주일" 같은 응답 기한을 계산할 기준 시각
 */
internal fun AppointmentCreateDraft.toNewAppointment(
    nowMillis: Long,
    previousMembers: PreviousMembers? = null
): NewAppointment? {
    val candidateLocations = locations.map { it.toNewCandidateLocation() ?: return null }
    val (dateType, dates) = candidateDates() ?: return null
    val deadline = responseDeadlineText(nowMillis) ?: return null
    return NewAppointment(
        title = name.trim(),
        dateType = dateType,
        candidateDates = dates.map { it.toRequestText() },
        candidateLocations = candidateLocations,
        responseDeadline = deadline,
        previousMembers = previousMembers
    )
}

private fun AppointmentLocation.toNewCandidateLocation(): NewCandidateLocation? {
    val lat = latitude ?: return null
    val lng = longitude ?: return null
    return NewCandidateLocation(
        placeName = name,
        address = roadAddress,
        lineNames = lineNames,
        latitude = lat,
        longitude = lng
    )
}

/**
 * 날짜 선택 방식에 맞는 후보 날짜 목록을 만든다. 서버는 "날짜별" 선택에 2개 이상을 요구하므로,
 * 하나만 고른 경우에는 "하루"로 보낸다.
 */
private fun AppointmentCreateDraft.candidateDates(): Pair<NewAppointmentDateType, List<AppointmentDate>>? =
    when (scheduleType) {
        AppointmentScheduleType.SINGLE_DAY -> startDate?.let { NewAppointmentDateType.SINGLE to listOf(it) }
        AppointmentScheduleType.DATE_RANGE -> {
            val start = startDate
            val end = endDate
            if (start == null || end == null) null
            else expandRange(start, end)?.let { NewAppointmentDateType.RANGE to it }
        }
        AppointmentScheduleType.SPECIFIC_DATES -> {
            val dates = specificDates.distinct().sorted()
            when {
                dates.isEmpty() -> null
                dates.size == 1 -> NewAppointmentDateType.SINGLE to dates
                else -> NewAppointmentDateType.MULTIPLE to dates
            }
        }
    }

private fun expandRange(start: AppointmentDate, end: AppointmentDate): List<AppointmentDate>? {
    if (end <= start) return null
    val dates = mutableListOf(start)
    var current = start
    while (current < end) {
        if (dates.size >= MAX_RANGE_DAYS) return null
        current = current.nextDay()
        dates += current
    }
    return dates
}

private fun AppointmentDate.nextDay(): AppointmentDate {
    val calendar = Calendar.getInstance(SEOUL).apply {
        clear()
        set(year, month - 1, day)
        add(Calendar.DAY_OF_MONTH, 1)
    }
    return AppointmentDate(
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH) + 1,
        calendar.get(Calendar.DAY_OF_MONTH)
    )
}

private fun AppointmentDate.toRequestText(): String =
    String.format(Locale.US, "%04d-%02d-%02d", year, month, day)

/** 직접 고른 날짜는 그날 23:59:59까지, 나머지는 지금부터 정해진 시간 뒤까지로 한다. 한국 시간 기준이다. */
private fun AppointmentCreateDraft.responseDeadlineText(nowMillis: Long): String? {
    val calendar = Calendar.getInstance(SEOUL).apply { timeInMillis = nowMillis }
    when (deadlineChoice) {
        AppointmentDeadlineChoice.ONE_DAY -> calendar.add(Calendar.HOUR_OF_DAY, 24)
        AppointmentDeadlineChoice.THREE_DAYS -> calendar.add(Calendar.DAY_OF_MONTH, 3)
        AppointmentDeadlineChoice.ONE_WEEK -> calendar.add(Calendar.DAY_OF_MONTH, 7)
        AppointmentDeadlineChoice.CUSTOM_DATE -> {
            val date = customDeadline ?: return null
            calendar.clear()
            calendar.set(date.year, date.month - 1, date.day, 23, 59, 59)
        }
    }
    return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        .apply { timeZone = SEOUL }
        .format(calendar.time)
}
