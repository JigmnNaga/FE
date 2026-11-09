package com.jigmnnaga.presentation.appointment.finalconfirmation

/** Attendance choice displayed by the final appointment confirmation screen. */
enum class AppointmentAttendanceChoice {
    ATTENDING,
    NOT_ATTENDING
}

/**
 * Display data supplied by the owning feature. This model does not fetch or confirm an appointment.
 */
data class AppointmentFinalConfirmationUiState(
    val appointmentTitle: String,
    val dateLabel: String,
    val timeLabel: String,
    val placeName: String,
    val detailLocation: String? = null,
    val attendanceChoice: AppointmentAttendanceChoice? = null,
    val canConfirm: Boolean = false,
    val isSubmitting: Boolean = false
)

/** All user actions are forwarded to the owner; successful confirmation is not assumed here. */
data class AppointmentFinalConfirmationActions(
    val onBack: () -> Unit,
    val onAttendanceChoiceChange: (AppointmentAttendanceChoice) -> Unit,
    val onConfirm: () -> Unit
)
