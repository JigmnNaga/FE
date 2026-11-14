package com.jigmnnaga.presentation.appointment.detail

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.jigmnnaga.R

@Composable
fun rememberAppointmentDetailLabels(): AppointmentDetailLabels {
    val context = LocalContext.current
    return remember(context) { context.appointmentDetailLabels() }
}

private fun Context.appointmentDetailLabels() = AppointmentDetailLabels(
    scheduleBeforeConfirm = getString(R.string.appointment_status_before_confirm),
    scheduleConfirmed = getString(R.string.appointment_status_confirmed),
    scheduleCanceled = getString(R.string.appointment_status_canceled),
    scheduleSubmitted = getString(R.string.appointment_member_schedule_submitted),
    schedulePending = getString(R.string.appointment_member_schedule_pending),
    attending = getString(R.string.appointment_member_attending),
    notAttending = getString(R.string.appointment_member_not_attending),
    attendancePending = getString(R.string.appointment_member_attendance_pending),
    rescheduleRequested = getString(R.string.appointment_member_reschedule_requested),
    placeholder = getString(R.string.final_confirmation_placeholder),
    roomSubtitle = { getString(R.string.appointment_room_subtitle, it) },
    memberSummary = { getString(R.string.appointment_member_count, it) }
)
