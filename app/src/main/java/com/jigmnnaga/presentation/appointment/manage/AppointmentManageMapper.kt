package com.jigmnnaga.presentation.appointment.manage

import com.jigmnnaga.domain.model.AppointmentDateSelectionType
import com.jigmnnaga.domain.model.AppointmentDetail
import com.jigmnnaga.domain.model.CandidateLocationChange
import com.jigmnnaga.domain.model.NewCandidateLocation
import com.jigmnnaga.presentation.appointment.create.AppointmentCreateStep
import com.jigmnnaga.presentation.appointment.create.AppointmentCreateUiState
import com.jigmnnaga.presentation.appointment.create.AppointmentDate
import com.jigmnnaga.presentation.appointment.create.AppointmentLocation
import com.jigmnnaga.presentation.appointment.create.AppointmentScheduleType
import com.jigmnnaga.domain.model.AppointmentLocation as DetailLocation

/** 약속 상세를 관리 화면에서 편집할 수 있는 입력값으로 바꾼다. */
internal fun AppointmentDetail.toManageForm(): AppointmentCreateUiState {
    val dates = candidateDates.mapNotNull(::parseAppointmentDate).distinct().sorted()
    val type = when (dateSelectionType) {
        AppointmentDateSelectionType.SINGLE -> AppointmentScheduleType.SINGLE_DAY
        AppointmentDateSelectionType.RANGE -> AppointmentScheduleType.DATE_RANGE
        AppointmentDateSelectionType.MULTIPLE -> AppointmentScheduleType.SPECIFIC_DATES
        AppointmentDateSelectionType.OTHER -> null
    }
    val locations = candidateLocations.map { it.toFormLocation() }.distinct()
    return AppointmentCreateUiState(
        step = AppointmentCreateStep.APPOINTMENT_NAME,
        appointmentName = title,
        scheduleType = type,
        displayedMonth = (dates.firstOrNull() ?: AppointmentDate.currentMonth()).monthStart(),
        selectedStartDate = when (type) {
            AppointmentScheduleType.SINGLE_DAY, AppointmentScheduleType.DATE_RANGE -> dates.firstOrNull()
            else -> null
        },
        selectedEndDate = if (type == AppointmentScheduleType.DATE_RANGE && dates.size > 1) dates.last() else null,
        selectedDates = if (type == AppointmentScheduleType.SPECIFIC_DATES) dates else emptyList(),
        locationCandidates = locations,
        locationCandidatesFromServer = true,
        selectedLocations = locations
    )
}

/** 서버가 알려 준 후보 장소 ID. 화면의 장소 값과 짝지어 "유지할 장소"를 가려내는 데 쓴다. */
internal fun AppointmentDetail.existingLocationIds(): Map<AppointmentLocation, Long> =
    candidateLocations.mapNotNull { location ->
        location.locationId?.let { id -> location.toFormLocation() to id }
    }.toMap()

internal fun DetailLocation.toFormLocation() = AppointmentLocation(
    name = placeName,
    address = listOfNotNull(lineNames?.takeIf { it.isNotBlank() }, address?.takeIf { it.isNotBlank() })
        .joinToString(" · "),
    lineNames = lineNames,
    roadAddress = address,
    latitude = latitude,
    longitude = longitude
)

/**
 * 고른 장소를 서버 요청으로 바꾼다. 이미 있던 장소는 ID만 보내 유지하고, 새로 고른 장소는 위치와 함께 보낸다.
 * 새 장소에 위치(위도·경도)가 없으면 서버가 받지 않으므로 null이다.
 */
internal fun toCandidateLocationChange(
    selected: List<AppointmentLocation>,
    existingIds: Map<AppointmentLocation, Long>
): CandidateLocationChange? {
    val keep = mutableListOf<Long>()
    val added = mutableListOf<NewCandidateLocation>()
    for (location in selected.distinct()) {
        val id = existingIds[location]
        if (id != null) {
            keep += id
        } else {
            val lat = location.latitude ?: return null
            val lng = location.longitude ?: return null
            added += NewCandidateLocation(
                placeName = location.name,
                address = location.roadAddress,
                lineNames = location.lineNames,
                latitude = lat,
                longitude = lng
            )
        }
    }
    return CandidateLocationChange(keepLocationIds = keep, newLocations = added)
}

/** `yyyy-MM-dd`를 날짜로 바꾼다. 형식이 다르거나 없는 날짜면 null이다. */
internal fun parseAppointmentDate(text: String): AppointmentDate? {
    val parts = text.trim().split("-")
    if (parts.size != 3) return null
    val year = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull()?.takeIf { it in 1..12 } ?: return null
    val day = parts[2].toIntOrNull()?.takeIf { it in 1..31 } ?: return null
    return AppointmentDate(year, month, day)
}

/** 서버 응답 기한(`yyyy-MM-dd'T'HH:mm:ss`)에서 날짜만 꺼낸다. */
internal fun parseDeadlineDate(text: String?): AppointmentDate? =
    text?.substringBefore('T')?.let(::parseAppointmentDate)
