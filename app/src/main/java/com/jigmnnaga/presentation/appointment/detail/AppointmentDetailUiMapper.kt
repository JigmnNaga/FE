package com.jigmnnaga.presentation.appointment.detail

import com.jigmnnaga.domain.model.AppointmentAttendanceStatus
import com.jigmnnaga.domain.model.AppointmentDetail
import com.jigmnnaga.domain.model.AppointmentMember
import com.jigmnnaga.domain.model.AppointmentMemberRole
import com.jigmnnaga.presentation.appointment.existingadd.ExistingAppointmentAddPage
import com.jigmnnaga.presentation.appointment.existingadd.ExistingAppointmentAddUiState
import com.jigmnnaga.presentation.appointment.existingadd.ExistingAppointmentMemberUiState
import com.jigmnnaga.presentation.appointment.finalconfirmation.AppointmentAttendanceChoice
import com.jigmnnaga.presentation.appointment.finalconfirmation.AppointmentFinalConfirmationUiState
import com.jigmnnaga.presentation.appointment.host.HostAppointmentRoomUiState
import com.jigmnnaga.presentation.appointment.host.HostRoomParticipantUiState
import com.jigmnnaga.presentation.appointment.participant.ParticipantAppointmentManagementUiState
import com.jigmnnaga.presentation.appointment.participant.ParticipantAppointmentRoomUiState
import com.jigmnnaga.presentation.appointment.participant.ParticipantAppointmentStatus
import com.jigmnnaga.presentation.appointment.participant.ParticipantRoomMemberUiState
import com.jigmnnaga.presentation.appointment.schedule.MemberResponseStatus
import com.jigmnnaga.presentation.appointment.schedule.MemberResponseUiState
import java.text.SimpleDateFormat
import java.util.Locale

/** 약속 상세를 화면에 보여 줄 문구로 바꿀 때 쓰는 문구 모음. 테스트에서는 간단한 값으로 만들어 쓴다. */
data class AppointmentDetailLabels(
    val scheduleBeforeConfirm: String,
    val scheduleConfirmed: String,
    val scheduleCanceled: String,
    val scheduleSubmitted: String,
    val schedulePending: String,
    val attending: String,
    val notAttending: String,
    val attendancePending: String,
    val rescheduleRequested: String,
    val placeholder: String,
    val roomSubtitle: (memberCount: Int) -> String,
    val memberSummary: (memberCount: Int) -> String
)

/**
 * `yyyy-MM-dd`를 "11월 20일 금요일"로 바꾼다. 읽을 수 없는 값이면 null.
 * minSdk 24에서는 java.time을 쓸 수 없어서 SimpleDateFormat을 쓴다.
 */
internal fun formatDateLabel(isoDate: String?): String? {
    if (isoDate.isNullOrBlank()) return null
    return try {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            .apply { isLenient = false }
            .parse(isoDate) ?: return null
        SimpleDateFormat("M월 d일 EEEE", Locale.KOREA).format(date)
    } catch (e: Exception) {
        null
    }
}

/** `HH:mm`을 "오후 3:00"으로 바꾼다. 읽을 수 없는 값이면 null. */
internal fun formatTimeLabel(time: String?): String? {
    if (time.isNullOrBlank()) return null
    return try {
        val parsed = SimpleDateFormat("HH:mm", Locale.US)
            .apply { isLenient = false }
            .parse(time) ?: return null
        SimpleDateFormat("a h:mm", Locale.KOREA).format(parsed)
    } catch (e: Exception) {
        null
    }
}

/** 확정된 약속의 날짜와 시간을 "11월 20일 금요일 오후 3:00"로 합친다. 확정 전이거나 날짜를 읽을 수 없으면 null. */
internal fun AppointmentDetail.dateSummary(): String? {
    if (!confirmed) return null
    val date = formatDateLabel(confirmedDate) ?: return null
    val time = formatTimeLabel(confirmedTime)
    return if (time != null) "$date $time" else date
}

/** 확정된 장소와 세부 장소를 "강남역 카공카페"처럼 합친다. 둘 다 없으면 null. */
internal fun AppointmentDetail.placeSummary(): String? =
    listOfNotNull(
        confirmedLocation?.placeName?.takeIf { it.isNotBlank() },
        detailPlace?.placeName?.takeIf { it.isNotBlank() }
    ).joinToString(" ").takeIf { it.isNotEmpty() }

private fun AppointmentDetail.scheduleStatusLabel(labels: AppointmentDetailLabels): String = when {
    canceled -> labels.scheduleCanceled
    confirmed -> labels.scheduleConfirmed
    else -> labels.scheduleBeforeConfirm
}

/**
 * 서버 응답에는 내 멤버 ID가 없어서, 내가 방장일 때만 방장 멤버를 "나"로 본다.
 * 참여자일 때는 어느 멤버가 나인지 알 수 없어 false를 돌려준다.
 */
private fun AppointmentDetail.isCurrentUser(member: AppointmentMember): Boolean =
    myRole == AppointmentMemberRole.HOST && member.role == AppointmentMemberRole.HOST

private fun attendanceLabel(status: AppointmentAttendanceStatus, labels: AppointmentDetailLabels): String =
    when (status) {
        AppointmentAttendanceStatus.ATTENDING -> labels.attending
        AppointmentAttendanceStatus.NOT_ATTENDING -> labels.notAttending
        AppointmentAttendanceStatus.RESCHEDULE_REQUESTED -> labels.rescheduleRequested
        AppointmentAttendanceStatus.PENDING,
        AppointmentAttendanceStatus.OTHER -> labels.attendancePending
    }

/** 확정 전에는 일정 제출 여부, 확정 뒤에는 참석 응답을 멤버 상태로 보여 준다. */
private fun AppointmentDetail.memberStatusLabel(
    member: AppointmentMember,
    labels: AppointmentDetailLabels
): String = if (confirmed) {
    attendanceLabel(member.attendanceStatus, labels)
} else if (member.scheduleSubmitted) {
    labels.scheduleSubmitted
} else {
    labels.schedulePending
}

internal fun AppointmentDetail.toHostRoomUiState(labels: AppointmentDetailLabels) = HostAppointmentRoomUiState(
    title = title,
    subtitle = labels.roomSubtitle(joinedCount),
    scheduleStatusLabel = scheduleStatusLabel(labels),
    dateSummary = dateSummary(),
    placeSummary = placeSummary(),
    responseCount = scheduleSubmittedCount,
    participantCount = joinedCount,
    participants = members.map {
        HostRoomParticipantUiState(
            id = it.memberId.toString(),
            name = it.nickname,
            statusLabel = memberStatusLabel(it, labels),
            isHost = it.role == AppointmentMemberRole.HOST
        )
    }
)

internal fun AppointmentDetail.toParticipantRoomUiState(labels: AppointmentDetailLabels) =
    ParticipantAppointmentRoomUiState(
        title = title,
        subtitle = labels.roomSubtitle(joinedCount),
        appointmentStatus = if (confirmed) {
            ParticipantAppointmentStatus.CONFIRMED
        } else {
            ParticipantAppointmentStatus.AWAITING_CONFIRMATION
        },
        scheduleStatusLabel = scheduleStatusLabel(labels),
        dateSummary = dateSummary(),
        placeSummary = placeSummary(),
        members = members.map {
            ParticipantRoomMemberUiState(
                id = it.memberId.toString(),
                name = it.nickname,
                statusLabel = memberStatusLabel(it, labels),
                isCurrentUser = isCurrentUser(it)
            )
        }
    )

internal fun AppointmentDetail.toParticipantManagementUiState(labels: AppointmentDetailLabels) =
    ParticipantAppointmentManagementUiState(
        appointmentName = title,
        scheduleStatusLabel = scheduleStatusLabel(labels),
        dateSummary = dateSummary(),
        placeSummary = placeSummary(),
        members = toParticipantRoomUiState(labels).members,
        isNotificationsEnabled = myNotificationEnabled
    )

internal fun AppointmentDetail.toFinalConfirmationUiState(labels: AppointmentDetailLabels) =
    AppointmentFinalConfirmationUiState(
        appointmentTitle = title,
        dateLabel = formatDateLabel(confirmedDate) ?: labels.placeholder,
        timeLabel = formatTimeLabel(confirmedTime) ?: labels.placeholder,
        placeName = confirmedLocation?.placeName?.takeIf { it.isNotBlank() } ?: labels.placeholder,
        detailLocation = detailPlace?.placeName?.takeIf { it.isNotBlank() },
        attendanceChoice = when (myAttendanceStatus) {
            AppointmentAttendanceStatus.ATTENDING -> AppointmentAttendanceChoice.ATTENDING
            AppointmentAttendanceStatus.NOT_ATTENDING -> AppointmentAttendanceChoice.NOT_ATTENDING
            else -> null
        }
    )

internal fun AppointmentDetail.toExistingAddUiState(labels: AppointmentDetailLabels) =
    ExistingAppointmentAddUiState(
        page = ExistingAppointmentAddPage.RESCHEDULE,
        appointmentTitle = title,
        memberSummary = labels.memberSummary(joinedCount),
        scheduledTime = dateSummary() ?: labels.placeholder,
        scheduledPlace = placeSummary() ?: labels.placeholder,
        members = members.map {
            val isMe = isCurrentUser(it)
            ExistingAppointmentMemberUiState(
                id = it.memberId.toString(),
                name = it.nickname,
                statusLabel = attendanceLabel(it.attendanceStatus, labels),
                isMe = isMe,
                isSelectable = !isMe
            )
        }
    )

/** 일정을 낸 멤버는 완료, 아직 안 낸 멤버는 대기로 보여 준다. */
internal fun AppointmentDetail.toMemberResponses(): List<MemberResponseUiState> = members.map {
    MemberResponseUiState(
        id = it.memberId.toString(),
        name = it.nickname,
        status = if (it.scheduleSubmitted) MemberResponseStatus.COMPLETED else MemberResponseStatus.PENDING,
        isMe = isCurrentUser(it)
    )
}
