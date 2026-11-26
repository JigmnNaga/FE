package com.jigmnnaga.presentation.appointment.create

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.safeDrawingPadding
import com.jigmnnaga.R
import com.jigmnnaga.core.designsystem.component.AppButton
import com.jigmnnaga.core.designsystem.component.AppButtonPair
import com.jigmnnaga.core.designsystem.component.BackHeader
import com.jigmnnaga.presentation.appointment.manage.ManageConfirmDialog
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
    val canGoPrevious = state.step != AppointmentCreateStep.APPOINTMENT_NAME &&
        state.step != AppointmentCreateStep.INVITATION
    var showExitDialog by remember { mutableStateOf(false) }
    val requestExit: () -> Unit = {
        if (state.hasInput()) {
            showExitDialog = true
        } else {
            onBack()
        }
    }
    BackHandler(enabled = !state.isCreating) { requestExit() }
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
        BackHeader(title = headerTitle, onBackClick = requestExit)

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
                    onStateChange = onStateChange,
                    blockPastDates = true
                )

                AppointmentCreateStep.LOCATIONS -> AppointmentLocationsStep(
                    state = state,
                    onStateChange = onStateChange,
                    showSelectedSummary = false
                )

                AppointmentCreateStep.RESPONSE_DEADLINE -> AppointmentDeadlineStep(
                    state = state,
                    onStateChange = onStateChange,
                    onCreate = onCreate
                )

                AppointmentCreateStep.INVITATION -> AppointmentInvitationStep(
                    state = state,
                    onShare = onShare
                )
            }
        }

        if (state.step == AppointmentCreateStep.LOCATIONS && state.selectedLocations.isNotEmpty()) {
            SelectedLocationsCard(
                locations = state.selectedLocations,
                modifier = Modifier
                    .padding(horizontal = AppDimens.Spacing20)
                    .padding(bottom = AppDimens.Spacing12)
            )
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
            AppButtonPair(
                leftText = stringResource(R.string.appointment_previous_button),
                rightText = stringResource(R.string.appointment_next_button),
                onLeftClick = onPrevious,
                onRightClick = continueAction,
                leftEnabled = canGoPrevious && !state.isCreating,
                rightEnabled = ready && !state.isCreating,
                modifier = footerPadding,
                leftTestTag = "appointment-previous",
                rightTestTag = "appointment-next"
            )
        }
    }

    if (showExitDialog) {
        ManageConfirmDialog(
            title = stringResource(R.string.appointment_exit_dialog_title),
            message = stringResource(R.string.appointment_exit_dialog_message),
            confirmText = stringResource(R.string.appointment_exit_dialog_confirm),
            cancelText = stringResource(R.string.appointment_exit_dialog_cancel),
            onDismiss = { showExitDialog = false },
            onConfirm = {
                showExitDialog = false
                onBack()
            },
            dialogTag = "appointment-exit-dialog",
            cancelTag = "appointment-exit-dialog-cancel",
            confirmTag = "appointment-exit-dialog-confirm"
        )
    }

    if (state.copyConfirmationVisible) {
        LinkCopiedDialog(onDismiss = onDismissCopyConfirmation)
    }
}

@Composable
private fun LinkCopiedDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().testTag("appointment-copy-popup"),
            shape = RoundedCornerShape(20.dp),
            color = AppColors.Monotone.White
        ) {
            Column(
                modifier = Modifier.padding(AppDimens.Spacing20),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
            ) {
                Spacer(Modifier.height(AppDimens.Spacing8))
                SuccessMark(
                    markSize = 48.dp,
                    background = AppColors.MainGreen.Green200,
                    checkColor = AppColors.MainGreen.Green900
                )
                Spacer(Modifier.height(AppDimens.Spacing4))
                Text(
                    text = stringResource(R.string.appointment_copy_confirmation_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = AppColors.Monotone.SoftBlack,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.appointment_copy_confirmation_message),
                    style = MaterialTheme.typography.titleSmall,
                    color = AppColors.Monotone.SoftBlack,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(AppDimens.Spacing12))
                AppButton(
                    text = stringResource(R.string.appointment_confirm),
                    variant = ButtonVariant.DEFAULT,
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().testTag("appointment-copy-popup-confirm")
                )
            }
        }
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
            .padding(top = AppDimens.Spacing16, bottom = AppDimens.Spacing8),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
    ) {
        labels.forEachIndexed { index, label ->
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(
                            if (index <= currentIndex) AppColors.MainGreen.Green900
                            else AppColors.Monotone.Gray300
                        )
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (index == currentIndex) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = if (index == currentIndex) AppColors.MainGreen.Green900
                    else AppColors.Monotone.Gray400,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppointmentNameStep(
    state: AppointmentCreateUiState,
    onNameChange: (String) -> Unit,
    onQuickSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)) {
            Text(
                text = stringResource(R.string.appointment_create_name_question),
                style = MaterialTheme.typography.titleSmall,
                color = AppColors.Monotone.SoftBlack
            )
            AppointmentTextField(
                value = state.appointmentName,
                onValueChange = onNameChange,
                placeholder = stringResource(R.string.appointment_create_name_hint),
                modifier = Modifier.testTag("appointment-name-field"),
                outlined = true
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.appointment_create_quick_select),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = AppColors.Monotone.Gray500
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

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8),
                verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
            ) {
                suggestions.forEach { (labelResource, testTag) ->
                    val label = stringResource(labelResource)
                    val selected = state.appointmentName == label
                    Surface(
                        onClick = { onQuickSelect(label) },
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("appointment-quick-choice-$testTag"),
                        shape = CircleShape,
                        color = if (selected) AppColors.MainGreen.Green600
                        else AppColors.Monotone.Gray100,
                        contentColor = if (selected) AppColors.MainGreen.Green200
                        else AppColors.Monotone.Gray700
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun AppointmentScheduleStep(
    state: AppointmentCreateUiState,
    onStateChange: (AppointmentCreateUiState) -> Unit,
    blockPastDates: Boolean = false
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
            val rangeStart = state.selectedStartDate
            val rangeEnd = state.selectedEndDate
            Text(
                text = if (state.scheduleType == AppointmentScheduleType.DATE_RANGE &&
                    rangeStart != null && rangeEnd != null
                ) {
                    "${rangeStart.dayLabel()} ~ ${rangeEnd.dayLabel()}"
                } else {
                    stringResource(hintResource)
                },
                style = MaterialTheme.typography.bodySmall,
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
                isDateEnabled = { date -> !blockPastDates || date >= todayDate() },
                minMonth = if (blockPastDates) todayDate().monthStart() else null,
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
            color = AppColors.Monotone.Gray500,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SelectedDateSummary(state: AppointmentCreateUiState) {
    val dates = when (state.scheduleType) {
        AppointmentScheduleType.SINGLE_DAY -> listOfNotNull(state.selectedStartDate)
        AppointmentScheduleType.DATE_RANGE -> {
            val start = state.selectedStartDate
            val end = state.selectedEndDate
            if (start != null && end != null) datesBetween(start, end) else listOfNotNull(start)
        }
        AppointmentScheduleType.SPECIFIC_DATES -> state.selectedDates
        null -> emptyList()
    }
    if (dates.isEmpty()) return

    SelectionChipCard(
        title = stringResource(R.string.appointment_selected_dates, dates.size),
        chips = dates.map { it.shortLabel() },
        modifier = Modifier.testTag("appointment-selected-dates")
    )
}

/** 선택된 날짜·장소를 흰 카드 안의 칩으로 보여 준다. 칩이 많으면 줄을 바꿔 카드가 아래로 길어진다. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectionChipCard(title: String, chips: List<String>, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = AppointmentCardShape,
        color = AppColors.Monotone.White
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.Spacing16),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.Monotone.Gray600
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8),
                verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
            ) {
                chips.forEach { label ->
                    Surface(shape = CircleShape, color = AppColors.Monotone.Gray100) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = AppColors.Monotone.SoftBlack,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedLocationsCard(locations: List<AppointmentLocation>, modifier: Modifier = Modifier) {
    SelectionChipCard(
        title = stringResource(R.string.appointment_selected_location_count, locations.size),
        chips = locations.map { it.name },
        modifier = modifier.testTag("appointment-selected-locations")
    )
}

@Composable
internal fun AppointmentLocationsStep(
    state: AppointmentCreateUiState,
    onStateChange: (AppointmentCreateUiState) -> Unit,
    showSelectedSummary: Boolean = true
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
        modifier = Modifier.testTag("appointment-location-search"),
        showSearchIcon = true
    )

    Text(
        text = stringResource(R.string.appointment_location_limit),
        style = MaterialTheme.typography.labelSmall,
        color = AppColors.Monotone.Gray600
    )

    val candidates = remember(state.locationCandidates, state.locationQuery, state.locationCandidatesFromServer) {
        if (state.locationCandidatesFromServer) {
            state.locationCandidates
        } else {
            state.locationCandidates.filter {
                state.locationQuery.isBlank() || it.name.contains(state.locationQuery, ignoreCase = true)
            }
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

    if (showSelectedSummary && state.selectedLocations.isNotEmpty()) {
        SelectedLocationsCard(locations = state.selectedLocations)
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
        color = AppColors.Monotone.White,
        border = if (selected) BorderStroke(2.dp, AppColors.MainGreen.Green500)
        else BorderStroke(1.dp, AppColors.Monotone.Gray100)
    ) {
        Row(
            modifier = Modifier.padding(AppDimens.Spacing12),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AppColors.Monotone.Gray100),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.place),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = AppColors.MainGreen.Green900
                )
            }
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
    onStateChange: (AppointmentCreateUiState) -> Unit,
    onCreate: (AppointmentCreateDraft) -> Unit
) {
    AppointmentSummaryCard(state, showDeadline = false)
    AppointmentDeadlineChoices(
        state = state,
        onStateChange = onStateChange,
        confirmLabelRes = R.string.appointment_create_button,
        onCustomDeadlineConfirmed = { updated -> updated.toDraftOrNull()?.let(onCreate) }
    )
}

/** 응답 기한 선택지. 직접 선택은 바텀시트의 달력으로 고른다. 약속 생성과 약속 관리 화면이 함께 쓴다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppointmentDeadlineChoices(
    state: AppointmentCreateUiState,
    onStateChange: (AppointmentCreateUiState) -> Unit,
    confirmLabelRes: Int = R.string.appointment_confirm,
    onCustomDeadlineConfirmed: ((AppointmentCreateUiState) -> Unit)? = null
) {
    val today = remember { todayDate() }
    val firstDate = state.firstCandidateDate()
    var sheetVisible by remember { mutableStateOf(false) }

    Text(
        text = stringResource(R.string.appointment_deadline_question),
        style = MaterialTheme.typography.titleMedium,
        color = AppColors.Monotone.SoftBlack
    )

    val options = listOf(
        AppointmentDeadlineChoice.ONE_DAY to R.string.appointment_deadline_one_day,
        AppointmentDeadlineChoice.THREE_DAYS to R.string.appointment_deadline_three_days,
        AppointmentDeadlineChoice.ONE_WEEK to R.string.appointment_deadline_one_week,
        AppointmentDeadlineChoice.CUSTOM_DATE to R.string.appointment_deadline_custom
    )
    Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)) {
        options.forEach { (choice, labelResource) ->
            val optionDate = if (choice == AppointmentDeadlineChoice.CUSTOM_DATE) {
                state.customDeadline
            } else {
                deadlineDateFor(choice, today)
            }
            val enabled = choice == AppointmentDeadlineChoice.CUSTOM_DATE ||
                firstDate == null || optionDate == null || optionDate <= firstDate
            DeadlineChoiceCard(
                title = stringResource(labelResource),
                subtitle = optionDate?.dayLabel() ?: stringResource(R.string.appointment_deadline_custom_hint),
                selected = state.deadlineChoice == choice,
                enabled = enabled,
                modifier = Modifier
                    .weight(1f)
                    .testTag("appointment-deadline-${choice.name.lowercase()}"),
                onClick = {
                    if (choice == AppointmentDeadlineChoice.CUSTOM_DATE) {
                        sheetVisible = true
                    } else {
                        onStateChange(state.copy(deadlineChoice = choice, customDeadline = null))
                    }
                }
            )
        }
    }
    if (firstDate != null) {
        Text(
            text = stringResource(
                R.string.appointment_deadline_helper,
                firstDate.dayLabel(),
                firstDate.plusDays(-1).dayLabel()
            ),
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.Monotone.Gray700
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = AppointmentCardShape,
        color = AppColors.Monotone.Gray100
    ) {
        Row(
            modifier = Modifier.padding(AppDimens.Spacing12),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .border(1.5.dp, AppColors.Monotone.Gray700, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "?",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.Monotone.Gray700
                )
            }
            Text(
                text = stringResource(R.string.appointment_deadline_note),
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.Monotone.Gray700
            )
        }
    }

    if (sheetVisible) {
        // 응답 기한은 오늘부터 첫 후보일 전날까지만 고를 수 있다.
        val lastSelectable = firstDate?.plusDays(-1)
        var pending by remember { mutableStateOf(state.customDeadline) }
        var month by remember { mutableStateOf((state.customDeadline ?: today).monthStart()) }
        val isSelectable = { date: AppointmentDate ->
            date >= today && (lastSelectable == null || date <= lastSelectable)
        }
        ModalBottomSheet(
            onDismissRequest = { sheetVisible = false },
            containerColor = AppColors.Monotone.White
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = AppDimens.Spacing20)
                    .padding(bottom = AppDimens.Spacing16),
                verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.appointment_deadline_sheet_title),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        color = AppColors.Monotone.SoftBlack
                    )
                    Text(
                        text = stringResource(R.string.appointment_schedule_cancel),
                        modifier = Modifier
                            .clickable { sheetVisible = false }
                            .padding(vertical = 4.dp, horizontal = 4.dp)
                            .testTag("appointment-deadline-sheet-cancel"),
                        style = MaterialTheme.typography.labelLarge,
                        color = AppColors.Monotone.Gray600
                    )
                }
                AppointmentCalendar(
                    displayedMonth = month,
                    selection = CalendarSelection(
                        type = null,
                        startDate = pending,
                        endDate = null,
                        dates = emptyList()
                    ),
                    onMonthChange = { month = it },
                    onDateClick = { pending = it },
                    isDateEnabled = isSelectable,
                    markedDate = firstDate
                )
                if (firstDate != null && lastSelectable != null) {
                    if (lastSelectable >= today) {
                        Text(
                            text = stringResource(
                                R.string.appointment_deadline_sheet_range,
                                today.dayLabel(),
                                lastSelectable.dayLabel()
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.MainGreen.Green700
                        )
                    }
                    Text(
                        text = stringResource(
                            R.string.appointment_deadline_sheet_first_date,
                            firstDate.dayLabel()
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.Monotone.Gray600
                    )
                }
                val chosen = pending
                AppButton(
                    text = stringResource(confirmLabelRes),
                    variant = ButtonVariant.DEFAULT,
                    enabled = chosen != null && isSelectable(chosen),
                    onClick = {
                        val updated = state.copy(
                            deadlineChoice = AppointmentDeadlineChoice.CUSTOM_DATE,
                            customDeadline = chosen
                        )
                        onStateChange(updated)
                        sheetVisible = false
                        onCustomDeadlineConfirmed?.invoke(updated)
                    },
                    modifier = Modifier.fillMaxWidth().testTag("appointment-deadline-sheet-confirm")
                )
            }
        }
    }
}

@Composable
private fun DeadlineChoiceCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val container = when {
        !enabled -> AppColors.Monotone.Gray100
        selected -> AppColors.MainGreen.Green500
        else -> AppColors.Monotone.White
    }
    val titleColor = when {
        !enabled -> AppColors.Monotone.Gray400
        selected -> AppColors.Monotone.White
        else -> AppColors.Monotone.SoftBlack
    }
    val subtitleColor = when {
        !enabled -> AppColors.Monotone.Gray400
        selected -> AppColors.Monotone.White
        else -> AppColors.Monotone.Gray600
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 64.dp),
        shape = AppointmentCardShape,
        color = container
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = AppDimens.Spacing12, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = titleColor,
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = subtitleColor,
                maxLines = 1
            )
        }
    }
}

private fun todayDate(): AppointmentDate {
    val calendar = Calendar.getInstance()
    return AppointmentDate(
        year = calendar.get(Calendar.YEAR),
        month = calendar.get(Calendar.MONTH) + 1,
        day = calendar.get(Calendar.DAY_OF_MONTH)
    )
}

/** 기한 선택지가 가리키는 날짜. 직접 선택은 사용자가 고른 날짜라 null이다. */
private fun deadlineDateFor(choice: AppointmentDeadlineChoice, today: AppointmentDate): AppointmentDate? {
    val days = when (choice) {
        AppointmentDeadlineChoice.ONE_DAY -> 1
        AppointmentDeadlineChoice.THREE_DAYS -> 3
        AppointmentDeadlineChoice.ONE_WEEK -> 7
        AppointmentDeadlineChoice.CUSTOM_DATE -> return null
    }
    val calendar = today.toCalendar().apply { add(Calendar.DAY_OF_MONTH, days) }
    return AppointmentDate(
        year = calendar.get(Calendar.YEAR),
        month = calendar.get(Calendar.MONTH) + 1,
        day = calendar.get(Calendar.DAY_OF_MONTH)
    )
}

/** 후보 날짜 중 가장 이른 날. 응답 기한은 이 날을 넘길 수 없다. */
private fun AppointmentCreateUiState.firstCandidateDate(): AppointmentDate? = when (scheduleType) {
    AppointmentScheduleType.SINGLE_DAY,
    AppointmentScheduleType.DATE_RANGE -> selectedStartDate
    AppointmentScheduleType.SPECIFIC_DATES -> selectedDates.minOrNull()
    null -> null
}

/** [start]부터 [end]까지 모든 날짜. 서버 한도(366일)를 넘는 기간은 그 안에서 자른다. */
private fun datesBetween(start: AppointmentDate, end: AppointmentDate): List<AppointmentDate> {
    if (end < start) return listOf(start)
    val calendar = start.toCalendar()
    val result = mutableListOf<AppointmentDate>()
    while (result.size < 366) {
        val date = AppointmentDate(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        result += date
        if (date >= end) break
        calendar.add(Calendar.DAY_OF_MONTH, 1)
    }
    return result
}

private fun AppointmentDate.plusDays(days: Int): AppointmentDate {
    val calendar = toCalendar().apply { add(Calendar.DAY_OF_MONTH, days) }
    return AppointmentDate(
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH) + 1,
        calendar.get(Calendar.DAY_OF_MONTH)
    )
}

private fun AppointmentDate.dayLabel(): String = "${month}월 ${day}일"

private fun AppointmentCreateUiState.deadlineSummary(): String = when (deadlineChoice) {
    null -> ""
    AppointmentDeadlineChoice.CUSTOM_DATE -> customDeadline?.dayLabel().orEmpty()
    else -> deadlineDateFor(deadlineChoice, todayDate())?.dayLabel().orEmpty()
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
        Text(
            text = if (state.inviteLink == null) stringResource(R.string.appointment_link_unavailable)
            else stringResource(R.string.appointment_invite_link_helper),
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.Monotone.Gray500
        )
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
private fun SuccessMark(
    markSize: androidx.compose.ui.unit.Dp = 52.dp,
    background: Color = AppColors.MainGreen.Green500,
    checkColor: Color = AppColors.Monotone.White
) {
    Box(
        modifier = Modifier
            .size(markSize)
            .clip(CircleShape)
            .background(background),
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
                color = checkColor,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

@Composable
private fun AppointmentSummaryCard(state: AppointmentCreateUiState, showDeadline: Boolean = true) {
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
                style = MaterialTheme.typography.titleMedium,
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
            if (showDeadline) {
                SummaryLine(
                    iconResource = R.drawable.departure_time,
                    label = stringResource(R.string.appointment_summary_deadline),
                    value = state.deadlineSummary().let {
                        if (it.isBlank()) it else stringResource(R.string.appointment_deadline_until, it)
                    }
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
        Icon(
            painter = painterResource(iconResource),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = AppColors.MainGreen.Green900
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
internal fun AppointmentTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    showSearchIcon: Boolean = false,
    outlined: Boolean = false
) {
    val focusManager = LocalFocusManager.current
    Surface(
        modifier = modifier.fillMaxWidth().heightIn(min = if (outlined) 54.dp else 48.dp),
        shape = RoundedCornerShape(if (outlined) 16.dp else 12.dp),
        color = AppColors.Monotone.White,
        border = if (outlined) BorderStroke(1.dp, AppColors.Monotone.Gray100) else null
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (outlined) AppDimens.Spacing16 else AppDimens.Spacing12,
                vertical = if (outlined) AppDimens.Spacing16 else AppDimens.Spacing12
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
        ) {
            if (showSearchIcon) {
                Icon(
                    painter = painterResource(R.drawable.search),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = AppColors.Monotone.Gray400
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
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
    onDateClick: (AppointmentDate) -> Unit,
    isDateEnabled: (AppointmentDate) -> Boolean = { true },
    markedDate: AppointmentDate? = null,
    minMonth: AppointmentDate? = null
) {
    val canGoPrevious = minMonth == null || displayedMonth.monthStart() > minMonth.monthStart()
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
                    onClick = { if (canGoPrevious) onMonthChange(displayedMonth.shiftMonth(-1)) },
                    enabled = canGoPrevious,
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
                                rangeMode = selection.type == AppointmentScheduleType.DATE_RANGE,
                                enabled = isDateEnabled(date),
                                marked = date == markedDate,
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
    rangeMode: Boolean,
    enabled: Boolean,
    marked: Boolean,
    weekday: Int,
    onClick: () -> Unit
) {
    // 기간 선택은 시작일부터 종료일까지 같은 색으로 채운다.
    val background = when {
        marked -> AppColors.Monotone.Gray100
        rangeMode && (selected || inRange) -> AppColors.MainGreen.Green500
        selected -> AppColors.MainGreen.Green900
        inRange -> AppColors.MainGreen.Green300
        else -> Color.Transparent
    }
    val textColor = when {
        marked -> AppColors.Monotone.Gray600
        !enabled -> AppColors.Monotone.Gray300
        selected || (rangeMode && inRange) -> AppColors.Monotone.White
        weekday == 0 -> AppColors.Danger.Red600
        weekday == 6 -> AppColors.MainGreen.Green700
        else -> AppColors.Monotone.SoftBlack
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        border = if (marked) BorderStroke(1.dp, AppColors.Monotone.Gray400) else null,
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
        if (start == null || end == null) "" else "${start.month}월 ${start.day}일~${end.month}월 ${end.day}일"
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

@Preview(showBackground = true, widthDp = 412, heightDp = 892, name = "APT-001-02 초기화면")
@Composable
private fun AppointmentScheduleEmptyPreview() {
    JigmnnagaTheme {
        AppointmentCreateScreen(
            state = AppointmentCreateUiState(
                step = AppointmentCreateStep.SCHEDULE,
                appointmentName = "저녁 식사 모임"
            ),
            onStateChange = {},
            onBack = {},
            onNext = {},
            onCreate = {},
            onShare = { _, _ -> },
            onDismissCopyConfirmation = {}
        )
    }
}

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
