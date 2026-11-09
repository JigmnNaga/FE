package com.jigmnnaga.presentation.appointment.create

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.safeDrawingPadding
import com.jigmnnaga.R
import com.jigmnnaga.core.designsystem.component.AppButton
import com.jigmnnaga.core.designsystem.component.BackHeader
import com.jigmnnaga.core.designsystem.component.ButtonVariant
import com.jigmnnaga.core.designsystem.component.CircleButton
import com.jigmnnaga.core.designsystem.theme.AppColors
import com.jigmnnaga.core.designsystem.theme.AppDimens
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme
import java.util.Calendar

private val KakaoYellow = Color(0xFFFEE500)
private val KakaoInk = Color(0xFF191919)
private val AppointmentCardShape = RoundedCornerShape(12.dp)

/**
 * APT-001 full-screen UI. The caller owns state, flow navigation, API work, and share providers.
 * This screen consumes safe drawing and IME insets; callers should avoid applying them twice.
 */
@Composable
fun AppointmentCreateScreen(
    state: AppointmentCreateUiState,
    onStateChange: (AppointmentCreateUiState) -> Unit,
    onBack: () -> Unit,
    onPrevious: () -> Unit = onBack,
    onNext: () -> Unit,
    onCreate: (AppointmentCreateDraft) -> Unit,
    onShare: (AppointmentShareAction, String) -> Unit,
    onDismissCopyConfirmation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val headerTitle = if (
        state.step == AppointmentCreateStep.LOCATIONS && state.appointmentName.isNotBlank()
    ) {
        state.appointmentName
    } else {
        stringResource(R.string.appointment_create_title)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.MainGreen.Green200)
            .safeDrawingPadding()
            .imePadding()
    ) {
        BackHeader(title = headerTitle, onBackClick = onBack)

        if (state.step != AppointmentCreateStep.INVITATION) {
            AppointmentStepProgress(step = state.step)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppDimens.Spacing20, vertical = AppDimens.Spacing16),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing16)
        ) {
            when (state.step) {
                AppointmentCreateStep.APPOINTMENT_NAME -> AppointmentNameStep(
                    state = state,
                    onNameChange = { onStateChange(state.copy(appointmentName = it)) },
                    onQuickSelect = {
                        focusManager.clearFocus()
                        onStateChange(state.copy(appointmentName = it))
                    }
                )

                AppointmentCreateStep.SCHEDULE -> AppointmentScheduleStep(
                    state = state,
                    onStateChange = onStateChange
                )

                AppointmentCreateStep.LOCATIONS -> AppointmentLocationsStep(
                    state = state,
                    onStateChange = onStateChange
                )

                AppointmentCreateStep.RESPONSE_DEADLINE -> AppointmentDeadlineStep(
                    state = state,
                    onStateChange = onStateChange
                )

                AppointmentCreateStep.INVITATION -> AppointmentInvitationStep(
                    state = state,
                    onShare = onShare
                )
            }
        }

        if (state.step != AppointmentCreateStep.INVITATION) {
            val ready = state.canContinue()
            val footerPadding = Modifier.padding(
                start = AppDimens.Spacing20,
                end = AppDimens.Spacing20,
                bottom = AppDimens.Spacing16
            )
            val continueAction: () -> Unit = continueAction@{
                if (!state.canContinue()) return@continueAction
                focusManager.clearFocus()
                if (state.step == AppointmentCreateStep.RESPONSE_DEADLINE) {
                    state.toDraftOrNull()?.let(onCreate)
                } else {
                    onNext()
                }
            }
            if (state.step == AppointmentCreateStep.APPOINTMENT_NAME) {
                AppButton(
                    text = stringResource(R.string.appointment_next_button),
                    variant = ButtonVariant.DEFAULT,
                    enabled = ready && !state.isCreating,
                    onClick = continueAction,
                    modifier = Modifier.fillMaxWidth().then(footerPadding).testTag("appointment-next")
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().then(footerPadding),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing12),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onPrevious,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("appointment-previous"),
                        shape = RoundedCornerShape(AppDimens.ButtonCornerRadius),
                        color = AppColors.Monotone.White,
                        border = BorderStroke(1.dp, AppColors.Monotone.Gray300)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = stringResource(R.string.appointment_previous_button),
                                style = MaterialTheme.typography.labelLarge,
                                color = AppColors.Monotone.SoftBlack
                            )
                        }
                    }
                    AppButton(
                        text = stringResource(
                            if (state.step == AppointmentCreateStep.RESPONSE_DEADLINE) {
                                R.string.appointment_create_button
                            } else {
                                R.string.appointment_next_button
                            }
                        ),
                        variant = ButtonVariant.DEFAULT,
                        enabled = ready && !state.isCreating,
                        onClick = continueAction,
                        modifier = Modifier.weight(1f).testTag("appointment-next")
                    )
                }
            }
        }
    }

    if (state.copyConfirmationVisible) {
        AlertDialog(
            onDismissRequest = onDismissCopyConfirmation,
            title = { Text(stringResource(R.string.appointment_copy_confirmation_title)) },
            text = { Text(stringResource(R.string.appointment_copy_confirmation_message)) },
            confirmButton = {
                TextButton(onClick = onDismissCopyConfirmation) {
                    Text(stringResource(R.string.appointment_confirm))
                }
            }
        )
    }
}

@Composable
private fun AppointmentStepProgress(step: AppointmentCreateStep) {
    val currentIndex = step.ordinal
    val labels = listOf(
        stringResource(R.string.appointment_step_name),
        stringResource(R.string.appointment_step_schedule),
        stringResource(R.string.appointment_step_locations),
        stringResource(R.string.appointment_step_deadline)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.Spacing20)
            .padding(top = AppDimens.Spacing8),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
    ) {
        labels.forEachIndexed { index, label ->
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(
                            if (index <= currentIndex) AppColors.MainGreen.Green900
                            else AppColors.Monotone.Gray300
                        )
                )
                Spacer(Modifier.height(AppDimens.Spacing4))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (index == currentIndex) AppColors.Monotone.SoftBlack
                    else AppColors.Monotone.Gray500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun AppointmentNameStep(
    state: AppointmentCreateUiState,
    onNameChange: (String) -> Unit,
    onQuickSelect: (String) -> Unit
) {
    Text(
        text = stringResource(R.string.appointment_create_name_question),
        style = MaterialTheme.typography.titleSmall,
        color = AppColors.Monotone.SoftBlack
    )

    AppointmentTextField(
        value = state.appointmentName,
        onValueChange = onNameChange,
        placeholder = stringResource(R.string.appointment_create_name_hint),
        modifier = Modifier.testTag("appointment-name-field")
    )

    Text(
        text = stringResource(R.string.appointment_create_quick_select),
        style = MaterialTheme.typography.labelSmall,
        color = AppColors.Monotone.Gray600
    )

    val suggestions = listOf(
        R.string.appointment_suggestion_lunch to "lunch",
        R.string.appointment_suggestion_dinner to "dinner",
        R.string.appointment_suggestion_study to "study",
        R.string.appointment_suggestion_exercise to "exercise",
        R.string.appointment_suggestion_cafe to "cafe",
        R.string.appointment_suggestion_movie to "movie",
        R.string.appointment_suggestion_drinks to "drinks",
        R.string.appointment_suggestion_travel to "travel"
    )

    suggestions.chunked(4).forEach { row ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
        ) {
            row.forEach { (labelResource, testTag) ->
                val label = stringResource(labelResource)
                val selected = state.appointmentName == label
                Surface(
                    onClick = { onQuickSelect(label) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 36.dp)
                        .testTag("appointment-quick-choice-$testTag"),
                    shape = RoundedCornerShape(8.dp),
                    color = if (selected) AppColors.MainGreen.Green900
                    else AppColors.Monotone.Gray100,
                    contentColor = if (selected) AppColors.MainGreen.Green200
                    else AppColors.Monotone.SoftBlack
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(6.dp)) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppointmentScheduleStep(
    state: AppointmentCreateUiState,
    onStateChange: (AppointmentCreateUiState) -> Unit
) {
    Text(
        text = stringResource(R.string.appointment_schedule_question),
        style = MaterialTheme.typography.titleSmall,
        color = AppColors.Monotone.SoftBlack
    )

    val scheduleOptions = listOf(
        AppointmentScheduleType.SINGLE_DAY to R.string.appointment_schedule_day,
        AppointmentScheduleType.DATE_RANGE to R.string.appointment_schedule_range,
        AppointmentScheduleType.SPECIFIC_DATES to R.string.appointment_schedule_specific
    )
    Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)) {
        scheduleOptions.forEach { (type, labelResource) ->
            val selected = state.scheduleType == type
            Surface(
                onClick = {
                    onStateChange(
                        state.copy(
                            scheduleType = type,
                            selectedStartDate = null,
                            selectedEndDate = null,
                            selectedDates = emptyList()
                        )
                    )
                },
                modifier = Modifier.weight(1f).heightIn(min = 44.dp)
                    .testTag("appointment-schedule-${type.name.lowercase()}"),
                shape = RoundedCornerShape(8.dp),
                color = if (selected) AppColors.MainGreen.Green900 else AppColors.Monotone.White,
                contentColor = if (selected) AppColors.MainGreen.Green200
                else AppColors.Monotone.SoftBlack
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(4.dp)) {
                    Text(
                        text = stringResource(labelResource),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    when (state.scheduleType) {
        null -> EmptyCalendarHint()
        else -> {
            val hintResource = when (state.scheduleType) {
                AppointmentScheduleType.SINGLE_DAY -> R.string.appointment_schedule_day_hint
                AppointmentScheduleType.DATE_RANGE -> R.string.appointment_schedule_range_hint
                AppointmentScheduleType.SPECIFIC_DATES -> R.string.appointment_schedule_specific_hint
            }
            Text(
                text = stringResource(hintResource),
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.Monotone.Gray600
            )
            AppointmentCalendar(
                displayedMonth = state.displayedMonth,
                selection = CalendarSelection(
                    type = state.scheduleType,
                    startDate = state.selectedStartDate,
                    endDate = state.selectedEndDate,
                    dates = state.selectedDates
                ),
                onMonthChange = { onStateChange(state.copy(displayedMonth = it)) },
                onDateClick = { selectedDate ->
                    val updated = when (state.scheduleType) {
                        AppointmentScheduleType.SINGLE_DAY -> state.copy(
                            selectedStartDate = selectedDate,
                            selectedEndDate = null
                        )
                        AppointmentScheduleType.DATE_RANGE -> when {
                            state.selectedStartDate == null || state.selectedEndDate != null ||
                                selectedDate < state.selectedStartDate -> state.copy(
                                selectedStartDate = selectedDate,
                                selectedEndDate = null
                            )
                            else -> state.copy(selectedEndDate = selectedDate)
                        }
                        AppointmentScheduleType.SPECIFIC_DATES -> state.copy(
                            selectedDates = if (selectedDate in state.selectedDates) {
                                state.selectedDates - selectedDate
                            } else {
                                (state.selectedDates + selectedDate).sorted()
                            }
                        )
                    }
                    onStateChange(updated)
                }
            )
            SelectedDateSummary(state = state)
        }
    }
}

@Composable
private fun EmptyCalendarHint() {
    Column(
        modifier = Modifier.fillMaxWidth().height(220.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(R.drawable.calendar_gray),
            contentDescription = null,
            modifier = Modifier.size(32.dp)
        )
        Spacer(Modifier.height(AppDimens.Spacing12))
        Text(
            text = stringResource(R.string.appointment_schedule_empty_hint),
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.Monotone.Gray500
        )
    }
}

@Composable
private fun SelectedDateSummary(state: AppointmentCreateUiState) {
    val dates = when (state.scheduleType) {
        AppointmentScheduleType.SINGLE_DAY -> listOfNotNull(state.selectedStartDate)
        AppointmentScheduleType.DATE_RANGE -> listOfNotNull(
            state.selectedStartDate,
            state.selectedEndDate
        )
        AppointmentScheduleType.SPECIFIC_DATES -> state.selectedDates
        null -> emptyList()
    }
    if (dates.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)) {
        Text(
            text = stringResource(R.string.appointment_selected_dates, dates.size),
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.Monotone.Gray600
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
        ) {
            dates.forEach { date ->
                Surface(shape = CircleShape, color = AppColors.Monotone.Gray100) {
                    Text(
                        text = date.shortLabel(),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.Monotone.SoftBlack
                    )
                }
            }
        }
    }
}

@Composable
private fun AppointmentLocationsStep(
    state: AppointmentCreateUiState,
    onStateChange: (AppointmentCreateUiState) -> Unit
) {
    Text(
        text = stringResource(R.string.appointment_locations_question),
        style = MaterialTheme.typography.titleSmall,
        color = AppColors.Monotone.SoftBlack
    )
    AppointmentTextField(
        value = state.locationQuery,
        onValueChange = { onStateChange(state.copy(locationQuery = it)) },
        placeholder = stringResource(R.string.appointment_location_search_hint),
        modifier = Modifier.testTag("appointment-location-search")
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.appointment_location_limit),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.Monotone.Gray600
        )
        Text(
            text = stringResource(R.string.appointment_location_count, state.selectedLocations.size),
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.Monotone.Gray600
        )
    }

    val candidates = remember(state.locationCandidates, state.locationQuery) {
        state.locationCandidates.filter {
            state.locationQuery.isBlank() || it.name.contains(state.locationQuery, ignoreCase = true)
        }
    }
    if (candidates.isEmpty()) {
        Text(
            text = stringResource(R.string.appointment_location_empty),
            modifier = Modifier.fillMaxWidth().padding(vertical = AppDimens.Spacing32),
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.Monotone.Gray500
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)) {
            candidates.forEach { location ->
                val selected = location in state.selectedLocations
                val limitReached = state.selectedLocations.size >= MAX_SELECTED_LOCATIONS
                LocationCandidateCard(
                    location = location,
                    selected = selected,
                    enabled = selected || !limitReached,
                    onClick = {
                        val locations = if (selected) {
                            state.selectedLocations - location
                        } else {
                            state.selectedLocations + location
                        }
                        onStateChange(state.copy(selectedLocations = locations))
                    }
                )
            }
        }
    }

    if (state.selectedLocations.isNotEmpty()) {
        Text(
            text = stringResource(
                R.string.appointment_selected_location_count,
                state.selectedLocations.size
            ),
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.Monotone.Gray600
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
        ) {
            state.selectedLocations.forEach { location ->
                Surface(shape = CircleShape, color = AppColors.MainGreen.Green300) {
                    Text(
                        text = location.name,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.Monotone.SoftBlack
                    )
                }
            }
        }
    }
}

@Composable
private fun LocationCandidateCard(
    location: AppointmentLocation,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp),
        shape = AppointmentCardShape,
        color = if (selected) AppColors.MainGreen.Green200 else AppColors.Monotone.White,
        border = if (selected) BorderStroke(1.dp, AppColors.MainGreen.Green900) else null
    ) {
        Row(
            modifier = Modifier.padding(AppDimens.Spacing12),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
        ) {
            Image(
                painter = painterResource(R.drawable.place),
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = location.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = AppColors.Monotone.SoftBlack
                )
                if (location.address.isNotBlank()) {
                    Text(
                        text = location.address,
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.Monotone.Gray500,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun AppointmentDeadlineStep(
    state: AppointmentCreateUiState,
    onStateChange: (AppointmentCreateUiState) -> Unit
) {
    AppointmentSummaryCard(state)
    Text(
        text = stringResource(R.string.appointment_deadline_question),
        style = MaterialTheme.typography.titleSmall,
        color = AppColors.Monotone.SoftBlack
    )

    val options = listOf(
        AppointmentDeadlineChoice.ONE_DAY to R.string.appointment_deadline_one_day,
        AppointmentDeadlineChoice.THREE_DAYS to R.string.appointment_deadline_three_days,
        AppointmentDeadlineChoice.ONE_WEEK to R.string.appointment_deadline_one_week,
        AppointmentDeadlineChoice.CUSTOM_DATE to R.string.appointment_deadline_custom
    )
    options.chunked(2).forEachIndexed { rowIndex, row ->
        Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)) {
            row.forEachIndexed { columnIndex, (choice, labelResource) ->
                DeadlineChoiceCard(
                    title = stringResource(labelResource),
                    selected = state.deadlineChoice == choice,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("appointment-deadline-${choice.name.lowercase()}"),
                    onClick = {
                        onStateChange(
                            state.copy(
                                deadlineChoice = choice,
                                customDeadline = if (choice == AppointmentDeadlineChoice.CUSTOM_DATE) {
                                    state.customDeadline
                                } else {
                                    null
                                }
                            )
                        )
                    }
                )
            }
        }
    }

    if (state.deadlineChoice == AppointmentDeadlineChoice.CUSTOM_DATE) {
        AppointmentCalendar(
            displayedMonth = state.displayedMonth,
            selection = CalendarSelection(
                type = null,
                startDate = state.customDeadline,
                endDate = null,
                dates = emptyList()
            ),
            onMonthChange = { onStateChange(state.copy(displayedMonth = it)) },
            onDateClick = { onStateChange(state.copy(customDeadline = it)) }
        )
    }

    Text(
        text = stringResource(R.string.appointment_deadline_note),
        style = MaterialTheme.typography.labelSmall,
        color = AppColors.Monotone.Gray600
    )
}

@Composable
private fun DeadlineChoiceCard(
    title: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 58.dp),
        shape = AppointmentCardShape,
        color = if (selected) AppColors.MainGreen.Green900 else AppColors.Monotone.White,
        contentColor = if (selected) AppColors.MainGreen.Green200 else AppColors.Monotone.SoftBlack
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.Spacing12),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun AppointmentInvitationStep(
    state: AppointmentCreateUiState,
    onShare: (AppointmentShareAction, String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = AppDimens.Spacing16),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
    ) {
        SuccessMark()
        Text(
            text = stringResource(R.string.appointment_created_title),
            style = MaterialTheme.typography.titleMedium,
            color = AppColors.Monotone.SoftBlack
        )
        Text(
            text = stringResource(R.string.appointment_created_message),
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.Monotone.Gray600
        )
    }

    AppointmentSummaryCard(state)

    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)) {
        Text(
            text = stringResource(R.string.appointment_invite_link_label),
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.Monotone.Gray600
        )
        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            shape = RoundedCornerShape(12.dp),
            color = AppColors.Monotone.White,
            border = BorderStroke(1.dp, AppColors.Monotone.Gray300)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = AppDimens.Spacing12, vertical = AppDimens.Spacing8),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
            ) {
                Text("↗", color = AppColors.Monotone.Gray700, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = state.inviteLink ?: stringResource(R.string.appointment_invite_link_pending),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (state.inviteLink == null) AppColors.Monotone.Gray500
                    else AppColors.Monotone.SoftBlack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (state.inviteLink == null) {
            Text(
                text = stringResource(R.string.appointment_link_unavailable),
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.Monotone.Gray500
            )
        }
    }

    Surface(
        onClick = { state.inviteLink?.let { onShare(AppointmentShareAction.KAKAO_TALK, it) } },
        enabled = !state.inviteLink.isNullOrBlank(),
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
            .testTag("appointment-share-kakao"),
        shape = RoundedCornerShape(AppDimens.ButtonCornerRadius),
        color = KakaoYellow,
        contentColor = KakaoInk
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(AppDimens.Spacing12)) {
            Text(
                text = stringResource(R.string.appointment_share_kakao),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }

    AppButton(
        text = stringResource(R.string.appointment_copy_link),
        variant = ButtonVariant.DEFAULT,
        enabled = !state.inviteLink.isNullOrBlank(),
        onClick = {
            state.inviteLink?.let { onShare(AppointmentShareAction.COPY_LINK, it) }
        },
        modifier = Modifier.fillMaxWidth().testTag("appointment-copy-link")
    )
}

@Composable
private fun SuccessMark() {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(AppColors.MainGreen.Green700),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(22.dp)) {
            val path = Path().apply {
                moveTo(size.width * 0.18f, size.height * 0.52f)
                lineTo(size.width * 0.42f, size.height * 0.75f)
                lineTo(size.width * 0.84f, size.height * 0.25f)
            }
            drawPath(
                path = path,
                color = AppColors.Monotone.White,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

@Composable
private fun AppointmentSummaryCard(state: AppointmentCreateUiState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = AppointmentCardShape,
        color = AppColors.Monotone.White
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.Spacing16),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
        ) {
            Text(
                text = state.appointmentName.ifBlank { stringResource(R.string.appointment_create_title) },
                style = MaterialTheme.typography.titleSmall,
                color = AppColors.Monotone.SoftBlack,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            SummaryLine(
                iconResource = R.drawable.calendar_gray,
                label = stringResource(R.string.appointment_summary_schedule),
                value = state.scheduleSummary()
            )
            SummaryLine(
                iconResource = R.drawable.place,
                label = stringResource(R.string.appointment_summary_locations),
                value = state.selectedLocations.joinToString(", ") { it.name }
            )
            state.customDeadline?.let { date ->
                SummaryLine(
                    iconResource = R.drawable.departure_time,
                    label = stringResource(R.string.appointment_step_deadline),
                    value = date.fullLabel()
                )
            }
        }
    }
}

@Composable
private fun SummaryLine(iconResource: Int, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
    ) {
        Image(
            painter = painterResource(iconResource),
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.Monotone.Gray600
        )
        Text(
            text = value.ifBlank { "—" },
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.Monotone.SoftBlack,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AppointmentTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    Surface(
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp),
        shape = RoundedCornerShape(12.dp),
        color = AppColors.Monotone.White
    ) {
        Box(
            modifier = Modifier.padding(horizontal = AppDimens.Spacing12, vertical = AppDimens.Spacing12),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = AppColors.Monotone.SoftBlack),
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppColors.Monotone.Gray500,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}

private data class CalendarSelection(
    val type: AppointmentScheduleType?,
    val startDate: AppointmentDate?,
    val endDate: AppointmentDate?,
    val dates: List<AppointmentDate>
)

@Composable
private fun AppointmentCalendar(
    displayedMonth: AppointmentDate,
    selection: CalendarSelection,
    onMonthChange: (AppointmentDate) -> Unit,
    onDateClick: (AppointmentDate) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = AppointmentCardShape,
        color = AppColors.Monotone.White
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.Spacing12),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CircleButton(
                    contentDescription = stringResource(R.string.appointment_calendar_previous),
                    onClick = { onMonthChange(displayedMonth.shiftMonth(-1)) },
                    width = 32.dp,
                    height = 32.dp,
                    text = "‹",
                    textSize = 22.sp,
                    backgroundColor = AppColors.MainGreen.Green200,
                    contentColor = AppColors.MainGreen.Green900
                )
                Text(
                    text = displayedMonth.monthLabel(),
                    style = MaterialTheme.typography.titleSmall,
                    color = AppColors.Monotone.SoftBlack
                )
                CircleButton(
                    contentDescription = stringResource(R.string.appointment_calendar_next),
                    onClick = { onMonthChange(displayedMonth.shiftMonth(1)) },
                    width = 32.dp,
                    height = 32.dp,
                    text = "›",
                    textSize = 22.sp,
                    backgroundColor = AppColors.MainGreen.Green200,
                    contentColor = AppColors.MainGreen.Green900
                )
            }

            val weekdays = stringArrayResource(R.array.appointment_calendar_weekdays)
            Row(modifier = Modifier.fillMaxWidth()) {
                weekdays.forEachIndexed { index, label ->
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        color = when (index) {
                            0 -> AppColors.Danger.Red600
                            6 -> AppColors.MainGreen.Green700
                            else -> AppColors.Monotone.Gray600
                        },
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            val firstDayOffset = displayedMonth.toCalendar().get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
            val daysInMonth = displayedMonth.toCalendar().apply {
                set(Calendar.DAY_OF_MONTH, 1)
            }.getActualMaximum(Calendar.DAY_OF_MONTH)

            repeat(6) { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    repeat(7) { weekday ->
                        val cellIndex = week * 7 + weekday
                        val dayOfMonth = cellIndex - firstDayOffset + 1
                        if (dayOfMonth in 1..daysInMonth) {
                            val date = displayedMonth.copy(day = dayOfMonth)
                            CalendarDateCell(
                                date = date,
                                selected = selection.contains(date),
                                inRange = selection.inRange(date),
                                weekday = weekday,
                                onClick = { onDateClick(date) }
                            )
                        } else {
                            Spacer(Modifier.weight(1f).height(36.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun CalendarSelection.contains(date: AppointmentDate): Boolean = when (type) {
    AppointmentScheduleType.SINGLE_DAY -> startDate == date
    AppointmentScheduleType.DATE_RANGE -> startDate == date || endDate == date
    AppointmentScheduleType.SPECIFIC_DATES -> date in dates
    null -> startDate == date
}

private fun CalendarSelection.inRange(date: AppointmentDate): Boolean =
    type == AppointmentScheduleType.DATE_RANGE && startDate != null && endDate != null &&
        date > startDate && date < endDate

@Composable
private fun RowScope.CalendarDateCell(
    date: AppointmentDate,
    selected: Boolean,
    inRange: Boolean,
    weekday: Int,
    onClick: () -> Unit
) {
    val background = when {
        selected -> AppColors.MainGreen.Green900
        inRange -> AppColors.MainGreen.Green300
        else -> Color.Transparent
    }
    val textColor = when {
        selected -> AppColors.Monotone.White
        weekday == 0 -> AppColors.Danger.Red600
        weekday == 6 -> AppColors.MainGreen.Green700
        else -> AppColors.Monotone.SoftBlack
    }
    Surface(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .height(36.dp)
            .padding(1.dp)
            .semantics {
                contentDescription = "${date.year}년 ${date.month}월 ${date.day}일"
            }
            .testTag("appointment-date-${date.year}-${date.month}-${date.day}"),
        shape = CircleShape,
        color = background
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = date.day.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = textColor
            )
        }
    }
}

private fun AppointmentDate.shiftMonth(amount: Int): AppointmentDate {
    val calendar = toCalendar().apply { set(Calendar.DAY_OF_MONTH, 1); add(Calendar.MONTH, amount) }
    return AppointmentDate(
        year = calendar.get(Calendar.YEAR),
        month = calendar.get(Calendar.MONTH) + 1,
        day = 1
    )
}

private fun AppointmentDate.monthLabel(): String = "${year}년 ${month}월"

private fun AppointmentDate.shortLabel(): String = "${month}/${day}"

private fun AppointmentDate.fullLabel(): String = "${year}년 ${month}월 ${day}일"

private fun AppointmentCreateUiState.scheduleSummary(): String = when (scheduleType) {
    AppointmentScheduleType.SINGLE_DAY -> selectedStartDate?.shortLabel().orEmpty()
    AppointmentScheduleType.DATE_RANGE -> {
        val start = selectedStartDate
        val end = selectedEndDate
        if (start == null || end == null) "" else "${start.month}월 ${start.day}일–${end.month}월 ${end.day}일"
    }
    AppointmentScheduleType.SPECIFIC_DATES -> selectedDates.joinToString(", ") { it.shortLabel() }
    null -> ""
}

private fun previewState(step: AppointmentCreateStep): AppointmentCreateUiState =
    AppointmentCreateUiState(
        step = step,
        appointmentName = "저녁 식사 모임",
        scheduleType = AppointmentScheduleType.DATE_RANGE,
        displayedMonth = AppointmentDate(2026, 9, 1),
        selectedStartDate = AppointmentDate(2026, 9, 9),
        selectedEndDate = AppointmentDate(2026, 9, 11),
        locationQuery = if (step == AppointmentCreateStep.LOCATIONS) "강남역" else "",
        locationCandidates = previewLocations,
        selectedLocations = previewLocations.take(2),
        deadlineChoice = AppointmentDeadlineChoice.THREE_DAYS,
        customDeadline = AppointmentDate(2026, 9, 8),
        inviteLink = if (step == AppointmentCreateStep.INVITATION) {
            "https://jigeumnaga.app/i/dinner-0905"
        } else {
            null
        }
    )

private val previewLocations = listOf(
    AppointmentLocation("강남역", "2호선, 신분당선 · 서울특별시 강남구 강남대로"),
    AppointmentLocation("건대입구역", "2호선, 7호선 · 서울특별시 광진구 아차산로"),
    AppointmentLocation("고속터미널역", "3호선, 7호선, 9호선 · 서울특별시 서초구 신반포로"),
    AppointmentLocation("성수역", "2호선 · 서울특별시 성동구 아차산로"),
    AppointmentLocation("신촌역", "2호선 · 서울특별시 마포구 신촌로"),
    AppointmentLocation("잠실역", "2호선, 8호선 · 서울특별시 송파구 올림픽로")
)

@Preview(showBackground = true, widthDp = 412, heightDp = 892, name = "APT-001-01 이름")
@Composable
private fun AppointmentNamePreview() = AppointmentCreatePreview(AppointmentCreateStep.APPOINTMENT_NAME)

@Preview(showBackground = true, widthDp = 412, heightDp = 892, name = "APT-001-02 기간")
@Composable
private fun AppointmentSchedulePreview() = AppointmentCreatePreview(AppointmentCreateStep.SCHEDULE)

@Preview(showBackground = true, widthDp = 412, heightDp = 892, name = "APT-001-03 장소")
@Composable
private fun AppointmentLocationsPreview() = AppointmentCreatePreview(AppointmentCreateStep.LOCATIONS)

@Preview(showBackground = true, widthDp = 412, heightDp = 892, name = "APT-001-04 기한")
@Composable
private fun AppointmentDeadlinePreview() = AppointmentCreatePreview(AppointmentCreateStep.RESPONSE_DEADLINE)

@Preview(showBackground = true, widthDp = 412, heightDp = 892, name = "APT-001-05 초대")
@Composable
private fun AppointmentInvitationPreview() = AppointmentCreatePreview(AppointmentCreateStep.INVITATION)

@Composable
private fun AppointmentCreatePreview(step: AppointmentCreateStep) {
    JigmnnagaTheme {
        AppointmentCreateScreen(
            state = previewState(step),
            onStateChange = {},
            onBack = {},
            onNext = {},
            onCreate = {},
            onShare = { _, _ -> },
            onDismissCopyConfirmation = {}
        )
    }
}
