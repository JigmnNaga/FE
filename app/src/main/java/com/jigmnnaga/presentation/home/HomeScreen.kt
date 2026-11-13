package com.jigmnnaga.presentation.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import com.jigmnnaga.R
import com.jigmnnaga.core.designsystem.component.BottomNavigationBar
import com.jigmnnaga.core.designsystem.component.BottomNavigationTab
import com.jigmnnaga.core.designsystem.component.MainHeader
import com.jigmnnaga.core.designsystem.theme.AppColors
import com.jigmnnaga.core.designsystem.theme.AppDimens
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme

/**
 * 홈 화면 순수 UI Composable.
 * ViewModel 의존 없이 상태와 이벤트를 파라미터로 주고받는다.
 * API 연동 시 HomeRoute(또는 HomeViewModel)에서 state를 공급하면 된다.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onTabSelect: (HomeTab) -> Unit,
    onAddClick: () -> Unit,
    onScheduleClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onProfileClick: () -> Unit,
    onAppointmentClick: (String) -> Unit,
    onInvitationClick: () -> Unit,
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

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(
                horizontal = AppDimens.HeaderHorizontalPadding,
                vertical = 15.dp
            )
        ) {
            val hasBanners =
                state.todayAppointment != null || state.pendingInviteCount > 0

            if (hasBanners) {
                item(key = "banners") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        state.todayAppointment?.let { today ->
                            TodayAppointmentBanner(appointment = today)
                        }
                        if (state.pendingInviteCount > 0) {
                            InvitationBanner(
                                count = state.pendingInviteCount,
                                onClick = onInvitationClick
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(15.dp))
                }
            }

            item(key = "tab_row") {
                HomeTabRow(
                    selectedTab = state.selectedTab,
                    onTabSelect = onTabSelect
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            val appointments = state.currentAppointments
            if (appointments.isEmpty()) {
                item(key = "empty_state") {
                    HomeEmptyState(tab = state.selectedTab)
                }
            } else {
                items(items = appointments, key = { it.id }) { appointment ->
                    AppointmentCard(
                        appointment = appointment,
                        isPast = state.selectedTab == HomeTab.PAST,
                        onClick = { onAppointmentClick(appointment.id) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        BottomNavigationBar(
            selectedTab = BottomNavigationTab.HOME,
            onHomeClick = {},
            onAddClick = onAddClick,
            onScheduleClick = onScheduleClick,
            modifier = Modifier.navigationBarsPadding()
        )
    }
}

// ─────────────────────────────────────────
// 배너 영역
// ─────────────────────────────────────────

/** 오늘의 약속 배너 (진한 녹색) */
@Composable
private fun TodayAppointmentBanner(
    appointment: MockAppointment,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.MainGreen.Green900, RoundedCornerShape(20.dp))
            .padding(horizontal = 15.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 좌측 장식선 (그라디언트 수직 라인)
        Canvas(
            modifier = Modifier
                .width(2.dp)
                .height(74.dp)
        ) {
            drawLine(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        AppColors.SubYellow.Yellow400,
                        AppColors.SubYellow.Yellow400,
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = size.height
                ),
                start = Offset(size.width / 2f, 0f),
                end = Offset(size.width / 2f, size.height),
                strokeWidth = size.width,
                cap = StrokeCap.Round
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // 레이블
            Text(
                text = stringResource(R.string.home_today_appointment_label),
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.MainGreen.Green300
            )
            // 약속 제목
            Text(
                text = appointment.title,
                // Figma: 15sp Bold — Typography에 없는 크기이므로 inline TextStyle 사용
                style = TextStyle(
                    fontFamily = MaterialTheme.typography.titleMedium.fontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    lineHeight = 22.5.sp
                ),
                color = AppColors.MainGreen.Green200,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // 시간 · 장소
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(R.drawable.appointment_time),
                        contentDescription = null,
                        modifier = Modifier.size(10.dp),
                        colorFilter = ColorFilter.tint(AppColors.MainGreen.Green200)
                    )
                    Text(
                        text = appointment.dateTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.MainGreen.Green200
                    )
                }
                Text(
                    text = " ｜ ",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.MainGreen.Green200
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(R.drawable.appointment_location),
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        colorFilter = ColorFilter.tint(AppColors.MainGreen.Green200)
                    )
                    Text(
                        text = appointment.location,
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.MainGreen.Green200,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/** 새 약속 초대 배너 (노란 테두리) */
@Composable
private fun InvitationBanner(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.SubYellow.Yellow200, RoundedCornerShape(15.dp))
            .border(2.dp, AppColors.SubYellow.Yellow600, RoundedCornerShape(15.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.new_invitation),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            colorFilter = ColorFilter.tint(AppColors.Icon.Yellow)
        )
        Text(
            text = stringResource(R.string.home_new_invitation_format, count),
            style = MaterialTheme.typography.labelMedium,
            color = AppColors.Icon.Yellow,
            modifier = Modifier.weight(1f)
        )
        Image(
            painter = painterResource(R.drawable.more_home),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            colorFilter = ColorFilter.tint(AppColors.Icon.Yellow)
        )
    }
}

// ─────────────────────────────────────────
// 탭 영역
// ─────────────────────────────────────────

@Composable
private fun HomeTabRow(
    selectedTab: HomeTab,
    onTabSelect: (HomeTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing20)
    ) {
        listOf(
            HomeTab.UPCOMING to stringResource(R.string.home_tab_upcoming),
            HomeTab.PAST to stringResource(R.string.home_tab_past)
        ).forEach { (tab, label) ->
            HomeTabItem(
                label = label,
                selected = tab == selectedTab,
                onClick = { onTabSelect(tab) }
            )
        }
    }
}

@Composable
private fun HomeTabItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(IntrinsicSize.Max)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = if (selected) AppColors.MainGreen.Green900 else AppColors.Monotone.Gray400,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.27.dp)
                .background(
                    if (selected) AppColors.MainGreen.Green900 else Color.Transparent
                )
        )
    }
}

// ─────────────────────────────────────────
// 빈 상태
// ─────────────────────────────────────────

@Composable
private fun HomeEmptyState(
    tab: HomeTab,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(500.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    AppColors.MainGreen.Green700.copy(alpha = 0.12f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.close),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                colorFilter = ColorFilter.tint(AppColors.Monotone.Gray400)
            )
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Figma: 20sp Bold — Typography에 없는 크기이므로 inline TextStyle 사용
        Text(
            text = stringResource(
                when (tab) {
                    HomeTab.UPCOMING -> R.string.home_empty_upcoming_title
                    HomeTab.PAST -> R.string.home_empty_past_title
                }
            ),
            style = TextStyle(
                fontFamily = MaterialTheme.typography.bodyMedium.fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                lineHeight = 28.sp
            ),
            color = AppColors.Monotone.SoftBlack,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(
                when (tab) {
                    HomeTab.UPCOMING -> R.string.home_empty_upcoming_subtitle
                    HomeTab.PAST -> R.string.home_empty_past_subtitle
                }
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.Monotone.Gray500,
            textAlign = TextAlign.Center
        )
    }
}

// ─────────────────────────────────────────
// 약속 카드
// ─────────────────────────────────────────

@Composable
private fun AppointmentCard(
    appointment: MockAppointment,
    isPast: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(24.dp), clip = false)
            .background(AppColors.Monotone.White, RoundedCornerShape(24.dp))
            .border(0.567.dp, AppColors.Monotone.Gray100, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // 제목 행
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isPast) {
                    Text(
                        text = appointment.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = AppColors.Monotone.SoftBlack,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        appointment.arrivalStatus?.let { status ->
                            ArrivalStatusBadge(
                                status = status,
                                lateMinutes = appointment.lateMinutes
                            )
                        }
                        Image(
                            painter = painterResource(R.drawable.more_home),
                            contentDescription = stringResource(R.string.home_appointment_detail),
                            modifier = Modifier.size(18.dp),
                            colorFilter = ColorFilter.tint(AppColors.Monotone.Gray700)
                        )
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (appointment.isToday) {
                            TodayBadge()
                        }
                        Text(
                            text = appointment.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = AppColors.Monotone.SoftBlack,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Image(
                        painter = painterResource(R.drawable.more_home),
                        contentDescription = stringResource(R.string.home_appointment_detail),
                        modifier = Modifier.size(18.dp),
                        colorFilter = ColorFilter.tint(AppColors.Monotone.Gray700)
                    )
                }
            }

            // 날짜·시간
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.appointment_time),
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    colorFilter = ColorFilter.tint(AppColors.Monotone.Gray500)
                )
                Text(
                    text = appointment.dateTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.Monotone.Gray500
                )
            }

            // 장소
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.appointment_location),
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    colorFilter = ColorFilter.tint(AppColors.Monotone.Gray600)
                )
                Text(
                    text = appointment.location,
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.Monotone.Gray600,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 참여자
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ParticipantAvatarGroup(participants = appointment.participants)
                Text(
                    text = stringResource(R.string.home_participant_count, appointment.participantCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.Monotone.Gray500
                )
            }
        }
    }
}

@Composable
private fun TodayBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(AppColors.MainGreen.Green900, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = stringResource(R.string.home_today_badge),
            style = MaterialTheme.typography.labelMedium,
            color = AppColors.MainGreen.Green200
        )
    }
}

@Composable
private fun ArrivalStatusBadge(
    status: ArrivalStatus,
    lateMinutes: Int = 0,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label) = when (status) {
        ArrivalStatus.ON_TIME -> Triple(
            AppColors.SubYellow.Yellow200,
            AppColors.SubYellow.Yellow600,
            stringResource(R.string.home_on_time)
        )
        ArrivalStatus.LATE -> Triple(
            AppColors.Danger.Red200,
            AppColors.Danger.Red600,
            stringResource(R.string.home_late_minutes, lateMinutes)
        )
    }
    Box(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = textColor
        )
    }
}

// ─────────────────────────────────────────
// 참여자 아바타 그룹
// ─────────────────────────────────────────

@Composable
private fun ParticipantAvatarGroup(
    participants: List<MockParticipant>,
    maxVisible: Int = 4,
    modifier: Modifier = Modifier
) {
    val visible = participants.take(maxVisible)
    val avatarSize = 28.dp
    val overlap = 8.dp

    Layout(
        modifier = modifier,
        content = {
            visible.forEach { participant ->
                ParticipantAvatar(participant)
            }
        }
    ) { measurables, _ ->
        val sizePx = avatarSize.roundToPx()
        val overlapPx = overlap.roundToPx()
        val placeables = measurables.map {
            it.measure(Constraints.fixed(sizePx, sizePx))
        }
        val totalWidth = if (placeables.isEmpty()) 0
        else sizePx + (placeables.size - 1) * (sizePx - overlapPx)

        layout(width = totalWidth, height = sizePx) {
            placeables.forEachIndexed { index, placeable ->
                placeable.placeRelative(
                    x = index * (sizePx - overlapPx),
                    y = 0,
                    zIndex = index.toFloat()
                )
            }
        }
    }
}

@Composable
private fun ParticipantAvatar(
    participant: MockParticipant,
    modifier: Modifier = Modifier
) {
    val avatarBg = if (participant.isMe) AppColors.MainGreen.Green700 else AppColors.MainGreen.Green400
    Box(
        modifier = modifier
            .size(28.dp)
            // Green200 테두리 (border = background + inner padding 방식)
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
                fontSize = 9.24.sp,
                lineHeight = 13.86.sp
            ),
            color = AppColors.MainGreen.Green200
        )
    }
}

// ─────────────────────────────────────────
// Compose Preview
// ─────────────────────────────────────────

@Preview(showSystemUi = true, name = "다가오는 약속 — 목록 있음")
@Composable
private fun HomeScreenUpcomingListPreview() {
    JigmnnagaTheme {
        HomeScreen(
            state = HomeUiState.PreviewWithList,
            onTabSelect = {},
            onAddClick = {},
            onScheduleClick = {},
            onNotificationClick = {},
            onProfileClick = {},
            onAppointmentClick = {},
            onInvitationClick = {}
        )
    }
}

@Preview(showSystemUi = true, name = "지난 약속 — 목록 있음")
@Composable
private fun HomeScreenPastListPreview() {
    JigmnnagaTheme {
        HomeScreen(
            state = HomeUiState.PreviewPastTab,
            onTabSelect = {},
            onAddClick = {},
            onScheduleClick = {},
            onNotificationClick = {},
            onProfileClick = {},
            onAppointmentClick = {},
            onInvitationClick = {}
        )
    }
}

@Preview(showSystemUi = true, name = "다가오는 약속 — 빈 상태")
@Composable
private fun HomeScreenEmptyUpcomingPreview() {
    JigmnnagaTheme {
        HomeScreen(
            state = HomeUiState.PreviewEmpty,
            onTabSelect = {},
            onAddClick = {},
            onScheduleClick = {},
            onNotificationClick = {},
            onProfileClick = {},
            onAppointmentClick = {},
            onInvitationClick = {}
        )
    }
}

@Preview(showSystemUi = true, name = "지난 약속 — 빈 상태")
@Composable
private fun HomeScreenEmptyPastPreview() {
    JigmnnagaTheme {
        HomeScreen(
            state = HomeUiState.PreviewEmptyPast,
            onTabSelect = {},
            onAddClick = {},
            onScheduleClick = {},
            onNotificationClick = {},
            onProfileClick = {},
            onAppointmentClick = {},
            onInvitationClick = {}
        )
    }
}
