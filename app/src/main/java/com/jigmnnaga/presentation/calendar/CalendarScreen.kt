package com.jigmnnaga.presentation.calendar

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jigmnnaga.R
import com.jigmnnaga.core.designsystem.component.BottomNavigationBar
import com.jigmnnaga.core.designsystem.component.BottomNavigationTab
import com.jigmnnaga.core.designsystem.component.MainHeader
import com.jigmnnaga.core.designsystem.theme.AppColors
import com.jigmnnaga.core.designsystem.theme.AppDimens
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme

// Figma 캘린더 전용 색상 (AppColors에 없는 값)
private val CalendarSundayColor = Color(0xFFEA4335)
private val CalendarSaturdayColor = Color(0xFF4285F4)

/**
 * 캘린더 화면 순수 UI Composable.
 * ViewModel 의존 없이 상태와 이벤트를 파라미터로 주고받는다.
 * API 연동 시 CalendarRoute(또는 CalendarViewModel)에서 state를 공급하면 된다.
 */
@Composable
fun CalendarScreen(
    state: CalendarUiState,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onAddClick: () -> Unit,
    onHomeClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.MainGreen.Green200)
    ) {
        Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

        MainHeader(
            onNotificationClick = onNotificationClick,
            onProfileClick = onProfileClick,
            profileLabel = "나",
            modifier = Modifier.padding(horizontal = AppDimens.HeaderHorizontalPadding)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = AppDimens.Spacing20,
                    vertical = AppDimens.Spacing16
                )
        ) {
            Spacer(modifier = Modifier.height(AppDimens.Spacing20))

            MonthNavigationRow(
                year = state.displayMonth.year,
                month = state.displayMonth.month,
                onPrevClick = onPrevMonth,
                onNextClick = onNextMonth
            )

            Spacer(modifier = Modifier.height(AppDimens.Spacing8))

            CalendarCard(
                displayMonth = state.displayMonth,
                today = state.today,
                appointmentDays = state.appointmentDays
            )

            Spacer(modifier = Modifier.height(AppDimens.Spacing16))

            val monthAppointments = state.monthAppointments
            if (monthAppointments.isEmpty()) {
                CalendarEmptyState()
            } else {
                AppointmentSection(
                    appointments = monthAppointments,
                    today = state.today
                )
            }

            Spacer(modifier = Modifier.height(AppDimens.Spacing16))
        }

        BottomNavigationBar(
            selectedTab = BottomNavigationTab.SCHEDULE,
            onHomeClick = onHomeClick,
            onAddClick = onAddClick,
            onScheduleClick = {},  // 이미 캘린더 화면이므로 미처리
            modifier = Modifier.navigationBarsPadding()
        )
    }
}

// ─────────────────────────────────────────
// 월 이동 헤더
// ─────────────────────────────────────────

@Composable
private fun MonthNavigationRow(
    year: Int,
    month: Int,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        MonthNavButton(
            contentDescription = stringResource(R.string.appointment_calendar_previous),
            onClick = onPrevClick
        )

        Text(
            text = "${year}년 ${month}월",
            style = TextStyle(
                fontFamily = MaterialTheme.typography.titleMedium.fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                lineHeight = 24.sp
            ),
            color = AppColors.Monotone.SoftBlack
        )

        MonthNavButton(
            contentDescription = stringResource(R.string.appointment_calendar_next),
            onClick = onNextClick,
            mirrorHorizontal = true
        )
    }
}

@Composable
private fun MonthNavButton(
    contentDescription: String,
    onClick: () -> Unit,
    mirrorHorizontal: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .background(AppColors.Monotone.Gray100, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.chevron_left),
            contentDescription = contentDescription,
            modifier = Modifier
                .size(AppDimens.IconSmall)
                .then(if (mirrorHorizontal) Modifier.scale(scaleX = -1f, scaleY = 1f) else Modifier),
            colorFilter = ColorFilter.tint(AppColors.Monotone.SoftBlack)
        )
    }
}

// ─────────────────────────────────────────
// 캘린더 카드 (흰색 R16 카드)
// ─────────────────────────────────────────

@Composable
private fun CalendarCard(
    displayMonth: CalendarMonth,
    today: CalendarDate,
    appointmentDays: Set<Int>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 1.dp, shape = RoundedCornerShape(16.dp), clip = false)
            .background(AppColors.Monotone.White, RoundedCornerShape(16.dp))
            .border(0.567.dp, AppColors.Monotone.Gray100, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            // 카드 내부 패딩: 상하 대칭으로 통일
            .padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 16.dp)
    ) {
        CalendarDayHeader()
        CalendarDateGrid(
            displayMonth = displayMonth,
            today = today,
            appointmentDays = appointmentDays
        )
    }
}

/** 요일 헤더 (일~토). 날짜 그리드와 동일한 7열 구조 사용. */
@Composable
private fun CalendarDayHeader(modifier: Modifier = Modifier) {
    val weekdays = stringArrayResource(R.array.appointment_calendar_weekdays)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
    ) {
        weekdays.forEachIndexed { index, label ->
            val color = when (index) {
                0 -> CalendarSundayColor
                6 -> CalendarSaturdayColor
                else -> AppColors.Monotone.Gray500
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = color
                )
            }
        }
    }
}

@Composable
private fun CalendarDateGrid(
    displayMonth: CalendarMonth,
    today: CalendarDate,
    appointmentDays: Set<Int>,
    modifier: Modifier = Modifier
) {
    val offset = displayMonth.firstDayOfWeekOffset()
    val dayCount = displayMonth.dayCount()
    val totalCells = offset + dayCount
    val weekCount = (totalCells + 6) / 7

    Column(modifier = modifier.fillMaxWidth()) {
        for (week in 0 until weekCount) {
            // 각 주 행을 고정 높이로 설정 → 열 정렬이 항상 일정하게 유지됨
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                for (slot in 0 until 7) {
                    val day = week * 7 + slot - offset + 1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (day in 1..dayCount) {
                            val isToday = displayMonth.year == today.year &&
                                    displayMonth.month == today.month &&
                                    day == today.day
                            val hasAppointment = day in appointmentDays
                            DateCellContent(
                                day = day,
                                dayOfWeekSlot = slot,
                                isToday = isToday,
                                hasAppointment = hasAppointment
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 날짜 셀 콘텐츠.
 * - 오늘: Green600 원형 배경 + White 텍스트
 * - 약속 있는 날 (오늘 아님): Gray200 원형 배경 + 기본 텍스트 색상
 * - 클릭 불가 (UI 표시 전용)
 */
@Composable
private fun DateCellContent(
    day: Int,
    dayOfWeekSlot: Int,  // 0=일, 6=토
    isToday: Boolean,
    hasAppointment: Boolean,
    modifier: Modifier = Modifier
) {
    val isSunday = dayOfWeekSlot == 0
    val isSaturday = dayOfWeekSlot == 6

    // 원형 배경 색상: 오늘=Green600, 약속 있는 날=Gray200, 그 외=투명
    val circleBg = when {
        isToday -> AppColors.MainGreen.Green600
        hasAppointment -> AppColors.Monotone.Gray200
        else -> Color.Transparent
    }

    // 텍스트 색상: 오늘(원형 배경 위)=White, 일요일=Red, 토요일=Blue, 평일=SoftBlack
    val textColor = when {
        isToday -> AppColors.Monotone.White
        isSunday -> CalendarSundayColor
        isSaturday -> CalendarSaturdayColor
        else -> AppColors.Monotone.SoftBlack
    }

    Box(
        modifier = modifier
            .size(32.dp)
            .background(circleBg, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}

// ─────────────────────────────────────────
// 빈 상태
// ─────────────────────────────────────────

@Composable
private fun CalendarEmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.calendar_empty_title),
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.Monotone.Gray500,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.calendar_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.Monotone.Gray500,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─────────────────────────────────────────
// 약속 목록 섹션
// ─────────────────────────────────────────

@Composable
private fun AppointmentSection(
    appointments: List<MockCalendarAppointment>,
    today: CalendarDate,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.calendar_this_month_section_title),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = AppColors.Monotone.SoftBlack
        )
        Spacer(modifier = Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            appointments.forEach { appointment ->
                CalendarAppointmentCard(appointment = appointment, today = today)
            }
        }
    }
}

@Composable
private fun CalendarAppointmentCard(
    appointment: MockCalendarAppointment,
    today: CalendarDate,
    modifier: Modifier = Modifier
) {
    val isToday = appointment.year == today.year &&
            appointment.month == today.month &&
            appointment.day == today.day

    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 1.dp, shape = RoundedCornerShape(16.dp), clip = false)
            .background(AppColors.Monotone.White, RoundedCornerShape(16.dp))
            .border(0.567.dp, AppColors.Monotone.Gray100, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CalendarDateBox(
            day = appointment.day,
            dayLabel = appointment.dayLabel,
            isToday = isToday
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = appointment.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = AppColors.Monotone.SoftBlack,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${appointment.time} · ${appointment.location}",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.Monotone.Gray500,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        CalendarParticipantAvatarGroup(participants = appointment.participants)
    }
}

@Composable
private fun CalendarDateBox(
    day: Int,
    dayLabel: String,
    isToday: Boolean,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isToday) AppColors.MainGreen.Green900 else AppColors.Monotone.Gray100
    val labelColor = if (isToday) AppColors.SubYellow.Yellow600 else AppColors.Monotone.Gray500
    val dayColor = if (isToday) AppColors.MainGreen.Green200 else AppColors.MainGreen.Green900

    Box(
        modifier = modifier
            .size(44.dp)
            .background(bgColor, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = dayLabel,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = labelColor
            )
            Text(
                text = day.toString(),
                // Figma: 18sp Black — 프로젝트에 Black weight 없어 Bold 사용
                style = TextStyle(
                    fontFamily = MaterialTheme.typography.titleMedium.fontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    lineHeight = 18.sp
                ),
                color = dayColor
            )
        }
    }
}

// ─────────────────────────────────────────
// 참여자 아바타 (캘린더 카드용, 24dp)
// ─────────────────────────────────────────

@Composable
private fun CalendarParticipantAvatarGroup(
    participants: List<CalendarParticipant>,
    maxVisible: Int = 3,
    modifier: Modifier = Modifier
) {
    val visible = participants.take(maxVisible)
    val avatarSize = 24.dp
    val overlap = 8.dp

    Layout(
        modifier = modifier,
        content = { visible.forEach { CalendarParticipantAvatar(it) } }
    ) { measurables, _ ->
        val sizePx = avatarSize.roundToPx()
        val overlapPx = overlap.roundToPx()
        val placeables = measurables.map { it.measure(Constraints.fixed(sizePx, sizePx)) }
        val totalWidth = if (placeables.isEmpty()) 0
        else sizePx + (placeables.size - 1) * (sizePx - overlapPx)
        layout(width = totalWidth, height = sizePx) {
            placeables.forEachIndexed { index, placeable ->
                placeable.placeRelative(x = index * (sizePx - overlapPx), y = 0, zIndex = index.toFloat())
            }
        }
    }
}

@Composable
private fun CalendarParticipantAvatar(
    participant: CalendarParticipant,
    modifier: Modifier = Modifier
) {
    val avatarBg = if (participant.isMe) AppColors.MainGreen.Green700 else AppColors.MainGreen.Green400
    Box(
        modifier = modifier
            .size(24.dp)
            .background(AppColors.MainGreen.Green200, CircleShape)
            .padding(2.27.dp)
            .background(avatarBg, CircleShape)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = participant.initial,
            style = TextStyle(
                fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 7.92.sp,
                lineHeight = 11.88.sp
            ),
            color = AppColors.MainGreen.Green200
        )
    }
}

// ─────────────────────────────────────────
// Compose Preview
// ─────────────────────────────────────────

@Preview(showSystemUi = true, name = "캘린더 — 약속 없음")
@Composable
private fun CalendarScreenEmptyPreview() {
    JigmnnagaTheme {
        CalendarScreen(
            state = CalendarUiState.PreviewEmpty,
            onPrevMonth = {}, onNextMonth = {},
            onAddClick = {}, onHomeClick = {},
            onNotificationClick = {}, onProfileClick = {}
        )
    }
}

@Preview(showSystemUi = true, name = "캘린더 — 약속 있음 (오늘 6일)")
@Composable
private fun CalendarScreenWithAppointmentsPreview() {
    JigmnnagaTheme {
        CalendarScreen(
            state = CalendarUiState.PreviewWithAppointments,
            onPrevMonth = {}, onNextMonth = {},
            onAddClick = {}, onHomeClick = {},
            onNotificationClick = {}, onProfileClick = {}
        )
    }
}

@Preview(showSystemUi = true, name = "캘린더 — 오늘이 월말 (29일)")
@Composable
private fun CalendarScreenTodayAtEndPreview() {
    JigmnnagaTheme {
        CalendarScreen(
            state = CalendarUiState.PreviewTodayAtEnd,
            onPrevMonth = {}, onNextMonth = {},
            onAddClick = {}, onHomeClick = {},
            onNotificationClick = {}, onProfileClick = {}
        )
    }
}

@Preview(showSystemUi = true, name = "캘린더 — 약속 없는 달")
@Composable
private fun CalendarScreenEmptyMonthPreview() {
    JigmnnagaTheme {
        CalendarScreen(
            state = CalendarUiState.PreviewEmptyMonth,
            onPrevMonth = {}, onNextMonth = {},
            onAddClick = {}, onHomeClick = {},
            onNotificationClick = {}, onProfileClick = {}
        )
    }
}
