package com.jigmnnaga.presentation.appointment.hostconfirmation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jigmnnaga.R
import com.jigmnnaga.core.designsystem.component.AppButton
import com.jigmnnaga.core.designsystem.component.BackHeader
import com.jigmnnaga.core.designsystem.component.ButtonVariant
import com.jigmnnaga.core.designsystem.theme.AppColors
import com.jigmnnaga.core.designsystem.theme.AppDimens
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme

/**
 * Standalone host confirmation UI. The parent owns navigation, confirmation policy,
 * persisted values, and transition to [HostConfirmationStep.COMPLETE].
 */
@Composable
fun HostAppointmentConfirmationScreen(
    state: HostConfirmationUiState,
    actions: HostConfirmationActions,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.MainGreen.Green200)
            .testTag("host-confirmation-screen")
    ) {
        if (state.step != HostConfirmationStep.COMPLETE) {
            BackHeader(
                title = stringResource(R.string.host_confirmation_header),
                onBackClick = actions.onBack
            )
        }

        if (state.step == HostConfirmationStep.COMPLETE) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = AppDimens.Spacing20),
                contentAlignment = Alignment.Center
            ) {
                CompleteContent(modifier = Modifier.fillMaxWidth())
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppDimens.Spacing20)
                    .padding(top = AppDimens.Spacing12, bottom = AppDimens.Spacing16)
            ) {
                AppButton(
                    text = stringResource(R.string.host_confirmation_done),
                    onClick = actions.onComplete,
                    modifier = Modifier.fillMaxWidth().testTag("confirmation-done")
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = AppDimens.Spacing20)
                    .padding(top = AppDimens.Spacing16, bottom = AppDimens.Spacing24),
                verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing16)
            ) {
                when (state.step) {
                    HostConfirmationStep.REVIEW -> ReviewContent(state, actions)
                    HostConfirmationStep.DATE_TIME -> ChoiceContent(
                        title = stringResource(R.string.host_confirmation_date_time_title),
                        choices = state.dateTimeChoices,
                        onSelect = actions.onSelectDateTime,
                        tag = "date-time"
                    )
                    HostConfirmationStep.PLACE -> ChoiceContent(
                        title = stringResource(R.string.host_confirmation_place_title),
                        choices = state.placeChoices,
                        onSelect = actions.onSelectPlace,
                        tag = "place"
                    )
                    HostConfirmationStep.DETAIL_LOCATION -> DetailLocationContent(state, actions)
                    HostConfirmationStep.COMPLETE -> Unit
                }
            }

            val confirmEnabled = state.step == HostConfirmationStep.REVIEW && state.canConfirm && !state.isBusy
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppDimens.Spacing20)
                    .padding(top = AppDimens.Spacing12, bottom = AppDimens.Spacing16)
            ) {
                AppButton(
                    text = stringResource(R.string.host_confirmation_confirm),
                    variant = ButtonVariant.DEFAULT,
                    enabled = confirmEnabled,
                    onClick = actions.onConfirm,
                    modifier = Modifier.fillMaxWidth().testTag("host-confirm")
                )
            }
        }
    }
}

@Composable
private fun ReviewContent(state: HostConfirmationUiState, actions: HostConfirmationActions) {
    Text(
        text = stringResource(R.string.host_confirmation_review_title),
        style = MaterialTheme.typography.titleMedium,
        color = AppColors.Monotone.SoftBlack
    )
    Text(
        text = state.appointmentTitle,
        style = MaterialTheme.typography.bodyLarge,
        color = AppColors.Monotone.Gray700,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis
    )
    SummaryRow(
        label = stringResource(R.string.host_confirmation_date_time_title),
        value = state.selectedDateTime,
        actionLabel = stringResource(R.string.host_confirmation_change),
        onAction = actions.onEditDateTime,
        tag = "edit-date-time"
    )
    SummaryRow(
        label = stringResource(R.string.host_confirmation_place_title),
        value = state.selectedPlace,
        actionLabel = stringResource(R.string.host_confirmation_change),
        onAction = actions.onEditPlace,
        tag = "edit-place"
    )
    state.selectedPlaceDescription?.let {
        Text(it, style = MaterialTheme.typography.bodyMedium, color = AppColors.Monotone.Gray700)
    }
    state.selectedDetailLocation?.let {
        SummaryRow(
            label = stringResource(R.string.host_confirmation_detail_location),
            value = it,
            actionLabel = stringResource(R.string.host_confirmation_change),
            onAction = actions.onOpenDetailLocation,
            tag = "edit-detail-location"
        )
    } ?: Text(
        text = stringResource(R.string.host_confirmation_detail_location_pending),
        style = MaterialTheme.typography.bodySmall,
        color = AppColors.Monotone.Gray700
    )
    AppButton(
        text = stringResource(R.string.host_confirmation_detail_location),
        variant = ButtonVariant.SECONDARY,
        onClick = actions.onOpenDetailLocation,
        modifier = Modifier.fillMaxWidth().testTag("open-detail-location")
    )
}

@Composable
private fun SummaryRow(
    label: String,
    value: String?,
    actionLabel: String,
    onAction: () -> Unit,
    tag: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimens.ButtonCornerRadius),
        color = AppColors.Monotone.White
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.Spacing16),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = AppColors.Monotone.Gray600)
            Text(
                text = value?.takeIf(String::isNotBlank) ?: stringResource(R.string.host_confirmation_value_missing),
                style = MaterialTheme.typography.bodyLarge,
                color = AppColors.Monotone.SoftBlack
            )
            Text(
                text = actionLabel,
                modifier = Modifier
                    .align(Alignment.End)
                    .heightIn(min = 48.dp)
                    .clickable(role = Role.Button, onClick = onAction)
                    .semantics { contentDescription = "$label $actionLabel" }
                    .testTag(tag)
                    .padding(horizontal = AppDimens.Spacing12, vertical = AppDimens.Spacing12),
                style = MaterialTheme.typography.labelLarge,
                color = AppColors.MainGreen.Green900
            )
        }
    }
}

@Composable
private fun ChoiceContent(
    title: String,
    choices: List<HostConfirmationChoice>,
    onSelect: (String) -> Unit,
    tag: String
) {
    Text(title, style = MaterialTheme.typography.titleMedium, color = AppColors.Monotone.SoftBlack)
    choices.forEach { choice ->
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .clickable(role = Role.RadioButton) { onSelect(choice.id) }
                .semantics { selected = choice.selected }
                .testTag("$tag-choice-${choice.id}"),
            shape = RoundedCornerShape(AppDimens.ButtonCornerRadius),
            color = if (choice.selected) AppColors.MainGreen.Green300 else AppColors.Monotone.White
        ) {
            Column(
                modifier = Modifier.padding(AppDimens.Spacing16),
                verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing4)
            ) {
                Text(choice.title, style = MaterialTheme.typography.bodyLarge, color = AppColors.Monotone.SoftBlack)
                choice.supportingText?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = AppColors.Monotone.Gray700)
                }
            }
        }
    }
}

@Composable
private fun DetailLocationContent(state: HostConfirmationUiState, actions: HostConfirmationActions) {
    Text(
        stringResource(R.string.host_confirmation_detail_location_title),
        style = MaterialTheme.typography.titleMedium,
        color = AppColors.Monotone.SoftBlack
    )
    Text(state.selectedPlace.orEmpty(), style = MaterialTheme.typography.bodyLarge, color = AppColors.Monotone.Gray700)
    state.detailLocationChoices.forEach { choice ->
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .clickable(role = Role.RadioButton) { actions.onSelectDetailLocation(choice.id) }
                .semantics { selected = choice.selected }
                .testTag("detail-choice-${choice.id}"),
            shape = RoundedCornerShape(AppDimens.ButtonCornerRadius),
            color = if (choice.selected) AppColors.MainGreen.Green300 else AppColors.Monotone.White
        ) {
            Column(Modifier.padding(AppDimens.Spacing16)) {
                Text(choice.title, style = MaterialTheme.typography.bodyLarge, color = AppColors.Monotone.SoftBlack)
                choice.supportingText?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = AppColors.Monotone.Gray700)
                }
            }
        }
    }
    AppButton(
        text = stringResource(R.string.host_confirmation_choose_later),
        variant = ButtonVariant.SECONDARY,
        onClick = actions.onChooseDetailLater,
        modifier = Modifier.fillMaxWidth().testTag("choose-detail-later")
    )
}

@Composable
private fun CompleteContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
    ) {
        Text(
            text = stringResource(R.string.host_confirmation_complete_title),
            style = MaterialTheme.typography.titleMedium,
            color = AppColors.Monotone.SoftBlack,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.host_confirmation_complete_message),
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.Monotone.Gray700,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true, widthDp = 320)
@Composable
private fun HostConfirmationPreview() {
    JigmnnagaTheme {
        HostAppointmentConfirmationScreen(
            state = HostConfirmationUiState(
                appointmentTitle = "주말 약속",
                selectedDateTime = "6월 15일 오후 2시",
                selectedPlace = "서울역 근처 장소",
                selectedPlaceDescription = "주소는 외부 상태에서 전달됩니다.",
                canConfirm = true
            ),
            actions = PreviewActions
        )
    }
}

private val PreviewActions = HostConfirmationActions(
    onBack = {}, onEditDateTime = {}, onEditPlace = {}, onSelectDateTime = {},
    onSelectPlace = {}, onOpenDetailLocation = {}, onSelectDetailLocation = {},
    onChooseDetailLater = {}, onConfirm = {}, onComplete = {}
)
