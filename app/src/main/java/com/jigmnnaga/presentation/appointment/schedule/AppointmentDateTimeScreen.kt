package com.jigmnnaga.presentation.appointment.schedule

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.util.Locale
import com.jigmnnaga.R
import com.jigmnnaga.core.designsystem.theme.AppColors
import com.jigmnnaga.core.designsystem.theme.AppDimens
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme
import com.jigmnnaga.core.designsystem.component.AppButton
import com.jigmnnaga.core.designsystem.component.BackHeader

/**
 * Schedule selection content for the participant appointment room.
 * The owner screen provides dates, availability and validation; this UI does not save anything.
 * The parent owns system-bar and IME insets.
 */
@Composable
fun AppointmentDateTimeScreen(
    state: AppointmentDateTimeUiState,
    onBack: () -> Unit,
    onMonthChange: (AppointmentCalendarMonth) -> Unit,
    onDateToggle: (AppointmentDateValue) -> Unit,
    onActiveDateSelect: (AppointmentDateValue) -> Unit,
    onStartTimeClick: () -> Unit,
    onEndTimeClick: () -> Unit,
    onQuickTimeClick: (String) -> Unit,
    onTimePickerValueChange: (AppointmentTimeValue) -> Unit,
    onTimePickerDismiss: () -> Unit,
    onTimePickerConfirm: (AppointmentTimeValue) -> Unit,
    onNext: () -> Unit,
    onGoToChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.MainGreen.Green200)
    ) {
        BackHeader(title = state.appointmentTitle, onBackClick = onBack)

        if (state.step != AppointmentDateTimeStep.COMPLETE) {
            SelectionStatus(state.completionStatusLabel)
        }

        when (state.step) {
            AppointmentDateTimeStep.DATES -> DateSelectionContent(
                state = state,
                onMonthChange = onMonthChange,
                onDateToggle = onDateToggle,
                modifier = Modifier.weight(1f)
            )
            AppointmentDateTimeStep.TIMES -> TimeSelectionContent(
                state = state,
                onActiveDateSelect = onActiveDateSelect,
                onStartTimeClick = onStartTimeClick,
                onEndTimeClick = onEndTimeClick,
                onQuickTimeClick = onQuickTimeClick,
                modifier = Modifier.weight(1f)
            )
            AppointmentDateTimeStep.COMPLETE -> CompletionContent(state, modifier = Modifier.weight(1f))
        }

        if (state.errorMessage != null) {
            Text(
                text = state.errorMessage,
                color = AppColors.Danger.Red600,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppDimens.Spacing20, vertical = AppDimens.Spacing8)
                    .semantics { contentDescription = state.errorMessage }
            )
        }

        val buttonText = when (state.step) {
            AppointmentDateTimeStep.DATES, AppointmentDateTimeStep.TIMES ->
                stringResource(R.string.appointment_schedule_next)
            AppointmentDateTimeStep.COMPLETE ->
                stringResource(R.string.appointment_schedule_go_to_chat)
        }
        AppButton(
            text = buttonText,
            enabled = state.step == AppointmentDateTimeStep.COMPLETE || state.canAdvance,
            onClick = if (state.step == AppointmentDateTimeStep.COMPLETE) onGoToChat else onNext,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimens.Spacing20)
                .padding(bottom = AppDimens.Spacing12)
                .semantics { contentDescription = buttonText }
        )
    }

    state.timePicker?.let { picker ->
        TimePickerSheet(
            picker = picker,
            onValueChange = onTimePickerValueChange,
            onDismiss = onTimePickerDismiss,
            onConfirm = onTimePickerConfirm
        )
    }
}

@Composable
private fun SelectionStatus(label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.Spacing20, vertical = AppDimens.Spacing8),
        horizontalArrangement = Arrangement.End
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = AppColors.MainGreen.Green200,
            border = BorderStroke(1.dp, AppColors.Monotone.Gray200)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.Monotone.Gray600,
                modifier = Modifier.padding(horizontal = AppDimens.Spacing8, vertical = AppDimens.Spacing4)
            )
        }
    }
}

@Composable
private fun DateSelectionContent(
    state: AppointmentDateTimeUiState,
    onMonthChange: (AppointmentCalendarMonth) -> Unit,
    onDateToggle: (AppointmentDateValue) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AppDimens.Spacing20),
        verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing4)) {
            Text(
                text = stringResource(R.string.appointment_schedule_select_dates),
                style = MaterialTheme.typography.titleMedium,
                color = AppColors.Monotone.SoftBlack
            )
            Text(
                text = stringResource(R.string.appointment_schedule_dates_helper),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.Monotone.Gray500
            )
        }

        CalendarCard(
            month = state.displayedMonth,
            selectedDates = state.selectedDates,
            onMonthChange = onMonthChange,
            onDateToggle = onDateToggle
        )

        if (state.selectedDates.isNotEmpty()) {
            SelectedDatesCard(dates = state.selectedDates.inCalendarOrder())
        }
    }
}

@Composable
private fun CalendarCard(
    month: AppointmentCalendarMonth,
    selectedDates: Set<AppointmentDateValue>,
    onMonthChange: (AppointmentCalendarMonth) -> Unit,
    onDateToggle: (AppointmentDateValue) -> Unit
) {
    Surface(
        color = AppColors.Monotone.White,
        shape = RoundedCornerShape(AppDimens.Spacing16),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(AppDimens.Spacing12)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MonthArrow(
                    description = stringResource(R.string.appointment_schedule_previous_month),
                    flipped = false,
                    onClick = { onMonthChange(month.shiftMonth(-1)) }
                )
                Text(
                    text = month.label(),
                    style = MaterialTheme.typography.titleSmall,
                    color = AppColors.Monotone.SoftBlack
                )
                MonthArrow(
                    description = stringResource(R.string.appointment_schedule_next_month),
                    flipped = true,
                    onClick = { onMonthChange(month.shiftMonth(1)) }
                )
            }
            Spacer(Modifier.height(AppDimens.Spacing8))
            Row(modifier = Modifier.fillMaxWidth()) {
                stringArrayResource(R.array.appointment_schedule_weekdays).forEachIndexed { index, day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = when (index) {
                            0 -> AppColors.Danger.Red600
                            6 -> AppColors.MainGreen.Green700
                            else -> AppColors.Monotone.Gray600
                        }
                    )
                }
            }
            val cellCount = ((month.firstWeekdayOffset() + month.dayCount() + 6) / 7) * 7
            val firstDay = month.firstWeekdayOffset()
            val daysInMonth = month.dayCount()
            repeat(cellCount / 7) { rowIndex ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    repeat(7) { columnIndex ->
                        val cell = rowIndex * 7 + columnIndex
                        val day = cell - firstDay + 1
                        if (day in 1..daysInMonth) {
                            val date = AppointmentDateValue(month.year, month.month, day)
                            CalendarDateCell(
                                date = date,
                                selected = date in selectedDates,
                                onClick = { onDateToggle(date) },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(Modifier.weight(1f).height(48.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthArrow(description: String, flipped: Boolean, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .semantics { contentDescription = description }
    ) {
        Surface(
            shape = CircleShape,
            color = AppColors.Monotone.White,
            border = BorderStroke(1.dp, AppColors.Monotone.Gray200),
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                androidx.compose.foundation.Image(
                    painter = painterResource(R.drawable.chevron_left),
                    contentDescription = null,
                    modifier = Modifier
                        .size(AppDimens.IconSmall)
                        .graphicsLayer { if (flipped) scaleX = -1f }
                )
            }
        }
    }
}

@Composable
private fun CalendarDateCell(
    date: AppointmentDateValue,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = date.accessibilityLabel()
                this.selected = selected
                this.role = Role.Button
            }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (selected) AppColors.MainGreen.Green700 else AppColors.Monotone.White),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = date.day.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = if (selected) AppColors.Monotone.White else when {
                    date.day == 1 -> AppColors.Danger.Red600
                    else -> AppColors.Monotone.SoftBlack
                }
            )
        }
    }
}

@Composable
private fun SelectedDatesCard(dates: List<AppointmentDateValue>) {
    Surface(
        color = AppColors.Monotone.White,
        shape = RoundedCornerShape(AppDimens.Spacing16),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.Spacing12),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
        ) {
            Text(
                text = pluralStringResource(
                    R.plurals.appointment_schedule_selected_dates_count,
                    dates.size,
                    dates.size
                ),
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.Monotone.SoftBlack
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
            ) {
                dates.forEach { date ->
                    Surface(
                        shape = CircleShape,
                        color = AppColors.Monotone.Gray100
                    ) {
                        Text(
                            text = date.shortLabel(),
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.Monotone.SoftBlack,
                            modifier = Modifier.padding(horizontal = AppDimens.Spacing8, vertical = AppDimens.Spacing4)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeSelectionContent(
    state: AppointmentDateTimeUiState,
    onActiveDateSelect: (AppointmentDateValue) -> Unit,
    onStartTimeClick: () -> Unit,
    onEndTimeClick: () -> Unit,
    onQuickTimeClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AppDimens.Spacing20),
        verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
    ) {
        Text(
            text = stringResource(R.string.appointment_schedule_time_helper),
            style = MaterialTheme.typography.titleSmall,
            color = AppColors.Monotone.SoftBlack
        )

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
        ) {
            state.selectedDates.inCalendarOrder().forEach { date ->
                val active = date == state.activeDate
                Surface(
                    onClick = { onActiveDateSelect(date) },
                    shape = RoundedCornerShape(AppDimens.Spacing8),
                    color = if (active) AppColors.MainGreen.Green700 else AppColors.Monotone.White,
                    modifier = Modifier.semantics { selected = active }
                ) {
                    Text(
                        text = date.shortLabel(),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (active) AppColors.Monotone.White else AppColors.Monotone.SoftBlack,
                        modifier = Modifier.padding(horizontal = AppDimens.Spacing16, vertical = AppDimens.Spacing8)
                    )
                }
            }
        }

        Surface(
            color = AppColors.Monotone.White,
            shape = RoundedCornerShape(AppDimens.Spacing16),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(AppDimens.Spacing12),
                verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)) {
                    TimeField(
                        label = stringResource(R.string.appointment_schedule_start_time),
                        value = state.selectedTimeRange?.start?.label(),
                        onClick = onStartTimeClick,
                        modifier = Modifier.weight(1f)
                    )
                    TimeField(
                        label = stringResource(R.string.appointment_schedule_end_time),
                        value = state.selectedTimeRange?.end?.label(),
                        onClick = onEndTimeClick,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = stringResource(R.string.appointment_schedule_selected_time),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.Monotone.Gray600
                )
                state.selectedTimeRange?.let { range ->
                    Text(
                        text = stringResource(
                            R.string.appointment_schedule_time_range,
                            range.start.label(),
                            range.end.label()
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.Monotone.SoftBlack
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)) {
            Text(
                text = stringResource(R.string.appointment_schedule_quick_times),
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.Monotone.SoftBlack
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
            ) {
                state.quickTimeOptions.forEach { option ->
                    val selected = option.id in state.selectedQuickTimeIds
                    Surface(
                        onClick = { onQuickTimeClick(option.id) },
                        shape = RoundedCornerShape(AppDimens.Spacing8),
                        color = if (selected) AppColors.MainGreen.Green700 else AppColors.Monotone.White,
                        border = if (selected) null else BorderStroke(1.dp, AppColors.Monotone.Gray200),
                        modifier = Modifier
                            .defaultMinSize(minWidth = 82.dp)
                            .semantics { this.selected = selected }
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = AppDimens.Spacing8, vertical = AppDimens.Spacing8),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) AppColors.Monotone.White else AppColors.Monotone.Gray600
                            )
                            Text(
                                text = option.rangeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) AppColors.Monotone.White else AppColors.Monotone.SoftBlack,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeField(
    label: String,
    value: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.Monotone.Gray600
        )
        Spacer(Modifier.height(AppDimens.Spacing4))
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(AppDimens.Spacing8),
            color = AppColors.MainGreen.Green200,
            border = BorderStroke(1.dp, AppColors.Monotone.Gray300),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .semantics { contentDescription = listOfNotNull(label, value).joinToString(" ") }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = AppDimens.Spacing8, vertical = AppDimens.Spacing8),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value ?: stringResource(R.string.appointment_schedule_select_time),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.Monotone.SoftBlack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                androidx.compose.foundation.Image(
                    painter = painterResource(R.drawable.chevron_left),
                    contentDescription = null,
                    modifier = Modifier.size(AppDimens.IconSmall).graphicsLayer { rotationZ = -90f }
                )
            }
        }
    }
}

@Composable
private fun CompletionContent(state: AppointmentDateTimeUiState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AppDimens.Spacing20),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        Surface(
            shape = CircleShape,
            color = AppColors.Monotone.Gray100,
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(24.dp)) {
                    val check = Path().apply {
                        moveTo(size.width * 0.2f, size.height * 0.52f)
                        lineTo(size.width * 0.43f, size.height * 0.74f)
                        lineTo(size.width * 0.82f, size.height * 0.28f)
                    }
                    drawPath(
                        path = check,
                        color = AppColors.MainGreen.Green700,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }
        Spacer(Modifier.height(AppDimens.Spacing16))
        Text(
            text = stringResource(R.string.appointment_schedule_complete_title),
            style = MaterialTheme.typography.titleMedium,
            color = AppColors.Monotone.SoftBlack,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.appointment_schedule_complete_description),
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.Monotone.Gray600,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = AppDimens.Spacing8)
        )
        Spacer(Modifier.height(AppDimens.Spacing32))
        if (state.completionMemberLabels.isNotEmpty()) {
            Surface(
                color = AppColors.Monotone.White,
                shape = RoundedCornerShape(AppDimens.Spacing16),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(AppDimens.Spacing12),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
                ) {
                    Text(
                        text = stringResource(R.string.appointment_schedule_completion_status),
                        style = MaterialTheme.typography.labelMedium,
                        color = AppColors.Monotone.SoftBlack
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)) {
                        state.completionMemberLabels.forEach { label ->
                            Surface(shape = CircleShape, color = AppColors.Monotone.Gray100) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AppColors.Monotone.Gray600,
                                    modifier = Modifier.padding(horizontal = AppDimens.Spacing8, vertical = AppDimens.Spacing4)
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TimePickerSheet(
    picker: TimePickerUiState,
    onValueChange: (AppointmentTimeValue) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (AppointmentTimeValue) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var hour by remember(picker.target, picker.selectedTime) { mutableIntStateOf(picker.selectedTime.hour) }
    var minute by remember(picker.target, picker.selectedTime) { mutableIntStateOf(picker.selectedTime.minute) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppColors.Monotone.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimens.Spacing20)
                .padding(bottom = AppDimens.Spacing24),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
        ) {
            Text(
                text = stringResource(
                    if (picker.target == TimePickerTarget.START) R.string.appointment_schedule_start_time
                    else R.string.appointment_schedule_end_time
                ),
                style = MaterialTheme.typography.titleSmall,
                color = AppColors.Monotone.SoftBlack
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TimeWheelColumn(
                    values = (0..23).toList(),
                    selected = hour,
                    format = { String.format(Locale.KOREA, "%02d", it) },
                    onSelect = {
                        hour = it
                        onValueChange(AppointmentTimeValue(hour, minute))
                    },
                    modifier = Modifier.weight(1f)
                )
                Text(":", style = MaterialTheme.typography.titleMedium, color = AppColors.Monotone.SoftBlack)
                TimeWheelColumn(
                    values = (0..55 step 5).toList(),
                    selected = minute,
                    format = { String.format(Locale.KOREA, "%02d", it) },
                    onSelect = {
                        minute = it
                        onValueChange(AppointmentTimeValue(hour, minute))
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)) {
                SheetAction(
                    text = stringResource(R.string.appointment_schedule_cancel),
                    selected = false,
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                SheetAction(
                    text = stringResource(R.string.appointment_schedule_apply),
                    selected = true,
                    onClick = { onConfirm(AppointmentTimeValue(hour, minute)) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TimeWheelColumn(
    values: List<Int>,
    selected: Int,
    format: (Int) -> String,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val initialIndex = values.indexOf(selected).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (initialIndex - 2).coerceAtLeast(0))
    LaunchedEffect(selected) {
        val index = values.indexOf(selected)
        if (index >= 0) listState.animateScrollToItem((index - 2).coerceAtLeast(0))
    }
    LazyColumn(
        state = listState,
        modifier = modifier.height(160.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(values) { value ->
            val isSelected = value == selected
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .clip(RoundedCornerShape(AppDimens.Spacing8))
                    .background(if (isSelected) AppColors.MainGreen.Green200 else AppColors.Monotone.White)
                    .clickable { onSelect(value) }
                    .semantics {
                        this.selected = isSelected
                        contentDescription = format(value)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = format(value),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) AppColors.MainGreen.Green900 else AppColors.Monotone.Gray400
                )
            }
        }
    }
}

@Composable
private fun SheetAction(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(AppDimens.Spacing8),
        color = if (selected) AppColors.MainGreen.Green900 else AppColors.Monotone.Gray100,
        modifier = modifier.heightIn(min = 48.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) AppColors.Monotone.White else AppColors.Monotone.Gray600
            )
        }
    }
}

private fun AppointmentCalendarMonth.shiftMonth(delta: Int): AppointmentCalendarMonth {
    val calendar = java.util.Calendar.getInstance().apply {
        set(year, month - 1, 1)
        add(java.util.Calendar.MONTH, delta)
    }
    return AppointmentCalendarMonth(
        year = calendar.get(java.util.Calendar.YEAR),
        month = calendar.get(java.util.Calendar.MONTH) + 1
    )
}

private fun Set<AppointmentDateValue>.inCalendarOrder(): List<AppointmentDateValue> =
    sortedWith(compareBy({ it.year }, { it.month }, { it.day }))

@Preview(showBackground = true, widthDp = 320, heightDp = 720)
@Composable
private fun AppointmentDateTimeDatePreview() {
    JigmnnagaTheme {
        AppointmentDateTimeScreen(
            state = previewDateTimeState(),
            onBack = {}, onMonthChange = {}, onDateToggle = {}, onActiveDateSelect = {},
            onStartTimeClick = {}, onEndTimeClick = {}, onQuickTimeClick = {},
            onTimePickerValueChange = {}, onTimePickerDismiss = {}, onTimePickerConfirm = {},
            onNext = {}, onGoToChat = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 720)
@Composable
private fun AppointmentDateTimeTimePreview() {
    JigmnnagaTheme {
        AppointmentDateTimeScreen(
            state = previewDateTimeState().copy(
                step = AppointmentDateTimeStep.TIMES,
                selectedDates = setOf(AppointmentDateValue(2026, 9, 5), AppointmentDateValue(2026, 9, 6)),
                activeDate = AppointmentDateValue(2026, 9, 5),
                canAdvance = true,
                quickTimeOptions = previewQuickTimes(),
                selectedTimeRange = AppointmentTimeRange(AppointmentTimeValue(12, 0), AppointmentTimeValue(17, 0)),
                selectedQuickTimeIds = setOf("afternoon")
            ),
            onBack = {}, onMonthChange = {}, onDateToggle = {}, onActiveDateSelect = {},
            onStartTimeClick = {}, onEndTimeClick = {}, onQuickTimeClick = {},
            onTimePickerValueChange = {}, onTimePickerDismiss = {}, onTimePickerConfirm = {},
            onNext = {}, onGoToChat = {}
        )
    }
}

private fun previewDateTimeState() = AppointmentDateTimeUiState(
    appointmentTitle = "중간고사 카공 모임",
    completionStatusLabel = "1명 선택 완료",
    displayedMonth = AppointmentCalendarMonth(2026, 9),
    selectedDates = setOf(AppointmentDateValue(2026, 9, 5)),
    canAdvance = true,
    quickTimeOptions = previewQuickTimes(),
    completionMemberLabels = listOf("나", "태양", "하루 대기")
)

private fun previewQuickTimes() = listOf(
    QuickTimeOptionUiState("morning", "오전", "9:00–12:00"),
    QuickTimeOptionUiState("afternoon", "오후", "12:00–17:00"),
    QuickTimeOptionUiState("evening", "저녁", "17:00–21:00")
)
