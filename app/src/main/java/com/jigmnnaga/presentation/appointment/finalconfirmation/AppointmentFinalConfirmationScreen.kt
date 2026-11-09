package com.jigmnnaga.presentation.appointment.finalconfirmation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.jigmnnaga.R
import com.jigmnnaga.core.designsystem.component.AppButton
import com.jigmnnaga.core.designsystem.component.BackHeader
import com.jigmnnaga.core.designsystem.theme.AppColors
import com.jigmnnaga.core.designsystem.theme.AppDimens
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme

/**
 * Standalone participant confirmation UI for APT-005-05.
 *
 * The owner supplies the confirmed appointment and controls whether the confirmation button is
 * enabled. This composable only reports the selected attendance and confirm action. The parent
 * screen remains responsible for navigation, system-bar insets, and IME/window policy.
 */
@Composable
fun AppointmentFinalConfirmationScreen(
    state: AppointmentFinalConfirmationUiState,
    actions: AppointmentFinalConfirmationActions,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.MainGreen.Green200)
    ) {
        BackHeader(
            title = stringResource(R.string.final_confirmation_header),
            onBackClick = actions.onBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppDimens.Spacing20)
                .padding(top = AppDimens.Spacing16, bottom = AppDimens.Spacing24),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing20)
        ) {
            AppointmentSummary(state)
            AttendanceChoiceSection(
                choice = state.attendanceChoice,
                onChoiceChange = actions.onAttendanceChoiceChange
            )
        }

        Surface(color = AppColors.MainGreen.Green200) {
            AppButton(
                text = stringResource(R.string.final_confirmation_confirm),
                enabled = state.canConfirm && !state.isSubmitting,
                onClick = actions.onConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = AppDimens.Spacing20,
                        vertical = AppDimens.Spacing12
                    )
                    .testTag("final-confirmation-confirm")
            )
        }
    }
}

@Composable
private fun AppointmentSummary(state: AppointmentFinalConfirmationUiState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimens.ButtonCornerRadius),
        color = AppColors.Monotone.White
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.Spacing16),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing16)
        ) {
            Text(
                text = state.appointmentTitle,
                style = MaterialTheme.typography.titleMedium,
                color = AppColors.Monotone.SoftBlack,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            AppointmentInfoRow(
                label = stringResource(R.string.final_confirmation_date),
                value = state.dateLabel
            )
            AppointmentInfoRow(
                label = stringResource(R.string.final_confirmation_time),
                value = state.timeLabel
            )
            AppointmentInfoRow(
                label = stringResource(R.string.final_confirmation_place),
                value = state.placeName
            )
            state.detailLocation?.takeIf(String::isNotBlank)?.let {
                AppointmentInfoRow(
                    label = stringResource(R.string.final_confirmation_detail_location),
                    value = it
                )
            }
        }
    }
}

@Composable
private fun AppointmentInfoRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing4)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.Monotone.Gray600
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = AppColors.Monotone.SoftBlack
        )
    }
}

@Composable
private fun AttendanceChoiceSection(
    choice: AppointmentAttendanceChoice?,
    onChoiceChange: (AppointmentAttendanceChoice) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)) {
        Text(
            text = stringResource(R.string.final_confirmation_attendance_question),
            style = MaterialTheme.typography.titleMedium,
            color = AppColors.Monotone.SoftBlack
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
        ) {
            AttendanceChoiceCard(
                choice = AppointmentAttendanceChoice.ATTENDING,
                selected = choice == AppointmentAttendanceChoice.ATTENDING,
                modifier = Modifier.weight(1f),
                onClick = onChoiceChange
            )
            AttendanceChoiceCard(
                choice = AppointmentAttendanceChoice.NOT_ATTENDING,
                selected = choice == AppointmentAttendanceChoice.NOT_ATTENDING,
                modifier = Modifier.weight(1f),
                onClick = onChoiceChange
            )
        }
    }
}

@Composable
private fun AttendanceChoiceCard(
    choice: AppointmentAttendanceChoice,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: (AppointmentAttendanceChoice) -> Unit
) {
    val label = when (choice) {
        AppointmentAttendanceChoice.ATTENDING -> stringResource(R.string.final_confirmation_attending)
        AppointmentAttendanceChoice.NOT_ATTENDING -> stringResource(R.string.final_confirmation_not_attending)
    }
    Surface(
        modifier = modifier
            .heightIn(min = AppDimens.ButtonHeight)
            .testTag(
                if (choice == AppointmentAttendanceChoice.ATTENDING) {
                    "attendance-attending"
                } else {
                    "attendance-not-attending"
                }
            )
            .semantics(mergeDescendants = true) {
                contentDescription = label
                this.selected = selected
            }
            .clickable(role = Role.RadioButton) { onClick(choice) },
        shape = RoundedCornerShape(AppDimens.ButtonCornerRadius),
        color = if (selected) AppColors.MainGreen.Green900 else AppColors.Monotone.White
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimens.Spacing12, vertical = AppDimens.Spacing16),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) AppColors.MainGreen.Green200 else AppColors.Monotone.SoftBlack,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 320)
@Composable
private fun AppointmentFinalConfirmationPreview() {
    JigmnnagaTheme {
        AppointmentFinalConfirmationScreen(
            state = AppointmentFinalConfirmationUiState(
                appointmentTitle = "주말 저녁 모임",
                dateLabel = "9월 4일 (월)",
                timeLabel = "오후 5:00",
                placeName = "서울역 근처 약속 장소",
                detailLocation = "1번 출구 앞",
                attendanceChoice = AppointmentAttendanceChoice.ATTENDING,
                canConfirm = true
            ),
            actions = AppointmentFinalConfirmationActions(
                onBack = {},
                onAttendanceChoiceChange = {},
                onConfirm = {}
            )
        )
    }
}
