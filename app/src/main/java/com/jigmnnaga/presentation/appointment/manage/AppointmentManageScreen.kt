package com.jigmnnaga.presentation.appointment.manage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jigmnnaga.R
import com.jigmnnaga.core.designsystem.component.AppButton
import com.jigmnnaga.core.designsystem.component.BackHeader
import com.jigmnnaga.core.designsystem.component.ButtonVariant
import com.jigmnnaga.core.designsystem.theme.AppColors
import com.jigmnnaga.core.designsystem.theme.AppDimens
import com.jigmnnaga.presentation.appointment.create.AppointmentCreateUiState
import com.jigmnnaga.presentation.appointment.create.AppointmentDeadlineChoices
import com.jigmnnaga.presentation.appointment.create.AppointmentLocationsStep
import com.jigmnnaga.presentation.appointment.create.AppointmentScheduleStep
import com.jigmnnaga.presentation.appointment.create.AppointmentScheduleType
import com.jigmnnaga.presentation.appointment.create.AppointmentTextField

/**
 * 방장 약속방 관리 화면(APT-006). 관리 목록과 항목별 변경 화면을 [AppointmentManageUiState.step]으로 나눠 그린다.
 * 상태와 서버 호출, 화면 이동은 호출하는 쪽이 맡고, 이 화면은 안전 영역과 키보드 여백을 직접 처리한다.
 */
@Composable
fun AppointmentManageScreen(
    state: AppointmentManageUiState,
    onBack: () -> Unit,
    onOpenStep: (AppointmentManageStep) -> Unit,
    onFormChange: (AppointmentCreateUiState) -> Unit,
    onSave: () -> Unit,
    onRequestConfirm: (AppointmentManageConfirm) -> Unit,
    onDismissConfirm: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val headerTitle = when (state.step) {
        AppointmentManageStep.MENU -> stringResource(R.string.appointment_manage_title)
        AppointmentManageStep.TITLE -> stringResource(R.string.appointment_manage_title_edit)
        else -> state.saved.appointmentName.ifBlank { stringResource(R.string.appointment_manage_title) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.MainGreen.Green200)
            .safeDrawingPadding()
            .imePadding()
    ) {
        BackHeader(title = headerTitle, onBackClick = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppDimens.Spacing20, vertical = AppDimens.Spacing16),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing16)
        ) {
            when (state.step) {
                AppointmentManageStep.MENU -> ManageMenu(
                    state = state,
                    onOpenStep = onOpenStep,
                    onRequestConfirm = onRequestConfirm
                )

                AppointmentManageStep.TITLE -> TitleEditor(form = state.form, onFormChange = onFormChange)

                AppointmentManageStep.SCHEDULE -> AppointmentScheduleStep(
                    state = state.form,
                    onStateChange = onFormChange
                )

                AppointmentManageStep.LOCATIONS -> AppointmentLocationsStep(
                    state = state.form,
                    onStateChange = onFormChange
                )

                AppointmentManageStep.DEADLINE -> AppointmentDeadlineChoices(
                    state = state.form,
                    onStateChange = onFormChange
                )
            }
        }

        if (state.step != AppointmentManageStep.MENU) {
            AppButton(
                text = stringResource(R.string.appointment_manage_save),
                variant = ButtonVariant.DEFAULT,
                enabled = state.canSave(),
                onClick = {
                    focusManager.clearFocus()
                    onSave()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = AppDimens.Spacing20,
                        end = AppDimens.Spacing20,
                        bottom = AppDimens.Spacing16
                    )
                    .testTag("appointment-manage-save")
            )
        }
    }

    state.confirm?.let { confirm ->
        val (titleResource, messageResource) = when (confirm) {
            AppointmentManageConfirm.CANCEL_APPOINTMENT ->
                R.string.appointment_manage_cancel_dialog_title to R.string.appointment_manage_cancel_dialog_message
            AppointmentManageConfirm.DELETE_APPOINTMENT ->
                R.string.appointment_manage_delete_dialog_title to R.string.appointment_manage_delete_dialog_message
        }
        AlertDialog(
            onDismissRequest = onDismissConfirm,
            title = { Text(stringResource(titleResource)) },
            text = { Text(stringResource(messageResource)) },
            confirmButton = {
                TextButton(onClick = onConfirm, modifier = Modifier.testTag("appointment-manage-confirm")) {
                    Text(stringResource(R.string.appointment_confirm), color = AppColors.Danger.Red600)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissConfirm) {
                    Text(stringResource(R.string.appointment_manage_dialog_dismiss))
                }
            }
        )
    }
}

@Composable
private fun ManageMenu(
    state: AppointmentManageUiState,
    onOpenStep: (AppointmentManageStep) -> Unit,
    onRequestConfirm: (AppointmentManageConfirm) -> Unit
) {
    val undecided = stringResource(R.string.appointment_manage_undecided)
    val editable = !state.isCanceled && !state.isWorking

    if (state.isCanceled) {
        Text(
            text = stringResource(R.string.appointment_manage_canceled_banner),
            style = MaterialTheme.typography.labelMedium,
            color = AppColors.Danger.Red600
        )
    }

    SectionTitle(stringResource(R.string.appointment_manage_section_info))
    ManageCard {
        ManageRow(
            title = stringResource(R.string.appointment_manage_item_title),
            subtitle = state.saved.appointmentName.ifBlank { undecided },
            enabled = editable,
            tag = "appointment-manage-title",
            onClick = { onOpenStep(AppointmentManageStep.TITLE) }
        )
        HorizontalDivider(color = AppColors.Monotone.Gray100)
        ManageRow(
            title = stringResource(R.string.appointment_manage_item_dates),
            subtitle = state.saved.dateSummary(undecided),
            enabled = editable,
            tag = "appointment-manage-dates",
            onClick = { onOpenStep(AppointmentManageStep.SCHEDULE) }
        )
        HorizontalDivider(color = AppColors.Monotone.Gray100)
        ManageRow(
            title = stringResource(R.string.appointment_manage_item_locations),
            subtitle = state.saved.locationSummary(undecided),
            enabled = editable,
            tag = "appointment-manage-locations",
            onClick = { onOpenStep(AppointmentManageStep.LOCATIONS) }
        )
        HorizontalDivider(color = AppColors.Monotone.Gray100)
        ManageRow(
            title = stringResource(R.string.appointment_manage_item_deadline),
            subtitle = deadlineSummary(state.savedDeadline, undecided),
            enabled = editable,
            tag = "appointment-manage-deadline",
            onClick = { onOpenStep(AppointmentManageStep.DEADLINE) }
        )
    }
    Text(
        text = stringResource(R.string.appointment_manage_info_note),
        style = MaterialTheme.typography.labelSmall,
        color = AppColors.Monotone.Gray600
    )

    SectionTitle(stringResource(R.string.appointment_manage_section_cleanup))
    ManageCard {
        ManageRow(
            title = stringResource(R.string.appointment_manage_cancel),
            subtitle = stringResource(R.string.appointment_manage_cancel_desc),
            enabled = editable,
            danger = true,
            tag = "appointment-manage-cancel",
            onClick = { onRequestConfirm(AppointmentManageConfirm.CANCEL_APPOINTMENT) }
        )
        HorizontalDivider(color = AppColors.Monotone.Gray100)
        ManageRow(
            title = stringResource(R.string.appointment_manage_delete),
            subtitle = stringResource(R.string.appointment_manage_delete_desc),
            enabled = !state.isWorking,
            danger = true,
            tag = "appointment-manage-delete",
            onClick = { onRequestConfirm(AppointmentManageConfirm.DELETE_APPOINTMENT) }
        )
    }
}

@Composable
private fun TitleEditor(form: AppointmentCreateUiState, onFormChange: (AppointmentCreateUiState) -> Unit) {
    Text(
        text = stringResource(R.string.appointment_create_name_question),
        style = MaterialTheme.typography.titleSmall,
        color = AppColors.Monotone.SoftBlack
    )
    AppointmentTextField(
        value = form.appointmentName,
        onValueChange = { onFormChange(form.copy(appointmentName = it)) },
        placeholder = stringResource(R.string.appointment_create_name_hint),
        modifier = Modifier.testTag("appointment-manage-title-field")
    )
    Text(
        text = stringResource(
            R.string.appointment_manage_title_count,
            form.appointmentName.length,
            MAX_APPOINTMENT_TITLE_LENGTH
        ),
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.labelSmall,
        color = AppColors.Monotone.Gray600
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = AppColors.Monotone.SoftBlack
    )
}

@Composable
private fun ManageCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = AppColors.Monotone.White
    ) {
        Column { content() }
    }
}

@Composable
private fun ManageRow(
    title: String,
    subtitle: String,
    enabled: Boolean,
    tag: String,
    onClick: () -> Unit,
    danger: Boolean = false
) {
    val titleColor = when {
        !enabled -> AppColors.Monotone.Gray500
        danger -> AppColors.Danger.Red600
        else -> AppColors.Monotone.SoftBlack
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).testTag(tag),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AppDimens.Spacing16, vertical = AppDimens.Spacing12),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall, color = titleColor)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.Monotone.Gray500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box { Text(text = "›", style = MaterialTheme.typography.titleMedium, color = AppColors.Monotone.Gray500) }
        }
    }
}

@Composable
private fun AppointmentCreateUiState.dateSummary(undecided: String): String {
    val dates = when (scheduleType) {
        AppointmentScheduleType.SINGLE_DAY -> listOfNotNull(selectedStartDate)
        AppointmentScheduleType.DATE_RANGE -> listOfNotNull(selectedStartDate, selectedEndDate)
        AppointmentScheduleType.SPECIFIC_DATES -> selectedDates
        null -> emptyList()
    }
    return when {
        dates.isEmpty() -> undecided
        scheduleType == AppointmentScheduleType.DATE_RANGE && dates.size == 2 ->
            stringResource(
                R.string.appointment_manage_date_range,
                dates[0].month, dates[0].day, dates[1].month, dates[1].day
            )
        dates.size == 1 -> stringResource(R.string.appointment_manage_date_single, dates[0].month, dates[0].day)
        else -> stringResource(
            R.string.appointment_manage_date_multiple,
            dates[0].month, dates[0].day, dates.size - 1
        )
    }
}

@Composable
private fun AppointmentCreateUiState.locationSummary(undecided: String): String = when (selectedLocations.size) {
    0 -> undecided
    1 -> selectedLocations[0].name
    else -> stringResource(
        R.string.appointment_manage_locations_more,
        selectedLocations[0].name,
        selectedLocations.size - 1
    )
}

@Composable
private fun deadlineSummary(deadline: String?, undecided: String): String {
    val date = parseDeadlineDate(deadline) ?: return undecided
    return stringResource(R.string.appointment_manage_deadline_value, date.month, date.day)
}
