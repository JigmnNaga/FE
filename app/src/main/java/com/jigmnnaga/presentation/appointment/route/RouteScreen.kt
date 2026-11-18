package com.jigmnnaga.presentation.appointment.route

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jigmnnaga.R
import com.jigmnnaga.core.designsystem.component.AppButton
import com.jigmnnaga.core.designsystem.component.ButtonVariant
import com.jigmnnaga.core.designsystem.component.MeetingHeader
import com.jigmnnaga.core.designsystem.theme.AppColors
import com.jigmnnaga.core.designsystem.theme.AppDimens
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme

private val CardShape = RoundedCornerShape(16.dp)

private fun TextStyle.sized(size: Int, line: Int, weight: FontWeight? = null): TextStyle =
    copy(fontSize = size.sp, lineHeight = line.sp, fontWeight = weight ?: fontWeight)

/**
 * 이동 경로 화면(ROU-001) 표시 전용. 단계 전환과 모든 문구는 호출한 쪽이 [state]로 넘기고,
 * 이 화면은 누른 사실만 콜백으로 알린다. 상태바·내비게이션 바·IME inset은 부모 화면이 맡는다.
 */
@Composable
fun RouteScreen(
    state: RouteUiState,
    onBackClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onPlaceClick: () -> Unit,
    onMoreClick: () -> Unit,
    onChatTabClick: () -> Unit,
    onMyCardClick: () -> Unit,
    onSelectTransport: (RouteTransport) -> Unit,
    onToggleOption: (RouteTransport) -> Unit,
    onStartMoving: () -> Unit,
    onToggleMyRoute: () -> Unit,
    onArrive: () -> Unit,
    onMyLocationClick: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenStartPicker: () -> Unit = {},
    onRetryOptions: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.MainGreen.Green200)
    ) {
        MeetingHeader(
            onBackClick = onBackClick,
            onCalendarClick = onCalendarClick,
            onPlaceClick = onPlaceClick,
            onMoreClick = onMoreClick,
            title = state.appointmentTitle,
            subtitle = state.memberSummary
        )
        RouteTabs(onChatClick = onChatTabClick)
        RouteBody(
            state = state,
            onMyCardClick = onMyCardClick,
            onSelectTransport = onSelectTransport,
            onToggleOption = onToggleOption,
            onStartMoving = onStartMoving,
            onToggleMyRoute = onToggleMyRoute,
            onArrive = onArrive,
            onMyLocationClick = onMyLocationClick,
            modifier = Modifier.weight(1f),
            onOpenStartPicker = onOpenStartPicker,
            onRetryOptions = onRetryOptions
        )
    }
}

/**
 * 헤더와 탭을 뺀 이동 경로 본문(스크롤 영역 + 하단 버튼). 약속방의 "지도·현황" 탭 안에 그대로 끼워 넣는다.
 */
@Composable
fun RouteBody(
    state: RouteUiState,
    onMyCardClick: () -> Unit,
    onSelectTransport: (RouteTransport) -> Unit,
    onToggleOption: (RouteTransport) -> Unit,
    onStartMoving: () -> Unit,
    onToggleMyRoute: () -> Unit,
    onArrive: () -> Unit,
    onMyLocationClick: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenStartPicker: () -> Unit = {},
    onRetryOptions: () -> Unit = {}
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            when (state.stage) {
                RouteStage.OVERVIEW -> OverviewBody(state, onMyCardClick)
                RouteStage.SELECT -> SelectBody(
                    state = state,
                    onSelectTransport = onSelectTransport,
                    onToggleOption = onToggleOption,
                    onMyLocationClick = onMyLocationClick,
                    onOpenStartPicker = onOpenStartPicker,
                    onRetryOptions = onRetryOptions
                )
                RouteStage.MOVING -> MovingBody(
                    state = state,
                    onToggleMyRoute = onToggleMyRoute,
                    onMyLocationClick = onMyLocationClick
                )
            }
        }

        ActionBar(state = state, onStartMoving = onStartMoving, onArrive = onArrive)
    }
}

@Composable
private fun RouteTabs(onChatClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.Spacing20)
            .padding(top = 24.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally)
    ) {
        RouteTab(
            label = stringResource(R.string.route_tab_chat),
            iconRes = R.drawable.chat,
            iconSize = 22,
            selected = false,
            onClick = onChatClick
        )
        RouteTab(
            label = stringResource(R.string.route_tab_map_status),
            iconRes = R.drawable.place,
            iconSize = 20,
            selected = true,
            onClick = {}
        )
    }
}

@Composable
private fun RouteTab(
    label: String,
    @DrawableRes iconRes: Int,
    iconSize: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val color = if (selected) AppColors.MainGreen.Green900 else AppColors.Monotone.Gray500
    Column(
        modifier = Modifier
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics { this.selected = selected }
            .testTag(if (selected) "route-tab-map" else "route-tab-chat")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(iconSize.dp),
                tint = color
            )
            Text(text = label, style = MaterialTheme.typography.titleSmall, color = color, maxLines = 1)
        }
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.5.dp)
                .background(if (selected) AppColors.MainGreen.Green700 else Color.Transparent)
        )
    }
}

// ---------------------------------------------------------------- 초기화면 (ROU-001-01)

@Composable
private fun OverviewBody(state: RouteUiState, onMyCardClick: () -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = AppDimens.Spacing20),
        verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
    ) {
        RouteOverviewMap()
        MeetingTimeCard(state)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.route_member_status_title),
                style = MaterialTheme.typography.titleSmall,
                color = AppColors.Monotone.SoftBlack
            )
            LiveChip()
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            state.members.forEach { member ->
                OverviewMemberCard(member = member, onClick = if (member.isMe) onMyCardClick else null)
            }
        }
        Spacer(Modifier.height(AppDimens.Spacing8))
    }
}

@Composable
private fun MeetingTimeCard(state: RouteUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.MainGreen.Green600, CardShape)
            .padding(horizontal = AppDimens.Spacing16, vertical = AppDimens.Spacing12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.route_expected_meeting_time),
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.MainGreen.Green400
            )
            Row(
                modifier = Modifier.padding(top = 2.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = state.meetingTimeLabel,
                    style = MaterialTheme.typography.titleMedium.sized(18, 28),
                    color = AppColors.MainGreen.Green200
                )
                Text(
                    text = state.meetingTimeNote,
                    modifier = Modifier.padding(bottom = 4.dp),
                    style = MaterialTheme.typography.labelSmall.sized(10, 15),
                    color = AppColors.MainGreen.Green400
                )
            }
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.route_friend_location_hint),
                style = MaterialTheme.typography.labelSmall.sized(9, 15),
                color = AppColors.MainGreen.Green300,
                textAlign = TextAlign.End
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.place),
                    contentDescription = null,
                    modifier = Modifier.size(10.dp),
                    tint = AppColors.MainGreen.Green200
                )
                Text(
                    text = state.destinationName,
                    style = MaterialTheme.typography.labelSmall.sized(11, 15, FontWeight.SemiBold),
                    color = AppColors.MainGreen.Green200,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun LiveChip() {
    Row(
        modifier = Modifier
            .background(AppColors.Monotone.Gray100, CircleShape)
            .padding(horizontal = AppDimens.Spacing8, vertical = AppDimens.Spacing4),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.route_live_label),
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.Monotone.Gray600
        )
        Spacer(Modifier.width(AppDimens.Spacing4))
        Box(
            Modifier
                .size(6.dp)
                .background(AppColors.SubYellow.Yellow600, CircleShape)
        )
    }
}

@Composable
private fun OverviewMemberCard(member: RouteMemberUiState, onClick: (() -> Unit)?) {
    val clickModifier = if (onClick != null) {
        Modifier
            .clip(CardShape)
            .clickable(role = Role.Button, onClick = onClick)
            .testTag("route-my-card")
    } else {
        Modifier.testTag("route-member-${member.id}")
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(66.dp)
            .then(clickModifier)
            .background(AppColors.Monotone.Gray300.copy(alpha = 0.12f), CardShape)
            .border(1.2.dp, AppColors.Monotone.Gray300.copy(alpha = 0.4f), CardShape)
            .padding(horizontal = AppDimens.Spacing16, vertical = AppDimens.Spacing12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Avatar(
            letter = member.name.take(1),
            size = 40,
            background = if (member.isMe) AppColors.MainGreen.Green700 else AppColors.MainGreen.Green400,
            textColor = AppColors.MainGreen.Green200,
            ring = true
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = member.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = AppColors.Monotone.SoftBlack
                )
                member.statusLabel?.let { StatusChip(it) }
            }
            Text(
                text = member.description,
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.Monotone.Gray500,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (onClick != null) {
            Icon(
                painter = painterResource(R.drawable.chevron_left),
                contentDescription = null,
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer { rotationZ = 180f },
                tint = AppColors.Monotone.Gray700
            )
        }
    }
}

@Composable
private fun StatusChip(label: String) {
    Row(
        modifier = Modifier
            .background(AppColors.Monotone.Gray300.copy(alpha = 0.2f), CircleShape)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            Modifier
                .size(5.dp)
                .background(AppColors.Monotone.Gray300, CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = AppColors.MainGreen.Green700
        )
    }
}

@Composable
private fun Avatar(
    letter: String,
    size: Int,
    background: Color,
    textColor: Color,
    ring: Boolean = false
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(background, CircleShape)
            .then(
                if (ring) Modifier.border(2.4.dp, AppColors.MainGreen.Green200, CircleShape) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter,
            style = MaterialTheme.typography.labelSmall.sized(
                if (ring) 13 else 14,
                if (ring) 20 else 14,
                FontWeight.Bold
            ),
            color = textColor
        )
    }
}

// ---------------------------------------------------------------- 내 이동 경로 설정 (ROU-001-02)

@Composable
private fun SelectBody(
    state: RouteUiState,
    onSelectTransport: (RouteTransport) -> Unit,
    onToggleOption: (RouteTransport) -> Unit,
    onMyLocationClick: () -> Unit,
    onOpenStartPicker: () -> Unit,
    onRetryOptions: () -> Unit
) {
    RouteMap(state = state, onMyLocationClick = onMyLocationClick)
    Column(
        modifier = Modifier.padding(horizontal = AppDimens.Spacing20, vertical = AppDimens.Spacing12),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        DestinationCard(state, onOpenStartPicker)
        when (state.optionsStatus) {
            RouteRequestStatus.LOADING -> RouteStatusText(
                text = stringResource(R.string.route_options_loading),
                tag = "route-options-loading"
            )
            RouteRequestStatus.FAILED -> RouteStatusText(
                text = stringResource(R.string.route_options_failed),
                tag = "route-options-failed",
                isError = true,
                onClick = onRetryOptions
            )
            RouteRequestStatus.IDLE -> Unit
        }
        state.options.forEach { option ->
            OptionCard(
                option = option,
                selected = option.transport == state.selectedTransport,
                expanded = option.transport == state.expandedTransport,
                onSelect = { onSelectTransport(option.transport) },
                onToggle = { onToggleOption(option.transport) }
            )
            if (option.transport == state.expandedTransport) {
                StepDetailCard(
                    title = stringResource(R.string.route_option_detail_title, option.title),
                    totalLabel = null,
                    steps = option.steps,
                    footnote = stringResource(R.string.route_estimate_note),
                    modifier = Modifier.testTag("route-option-detail-${option.transport.name}")
                )
            }
        }
        state.selectedOption?.timeSummary?.let { TimeSummaryCard(it) }
    }
}

/** 경로를 불러오는 중이거나 실패했다는 안내 한 줄. [onClick]이 있으면 눌러서 다시 시도한다. */
@Composable
private fun RouteStatusText(
    text: String,
    tag: String,
    isError: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = AppDimens.Spacing8)
            .testTag(tag),
        style = MaterialTheme.typography.labelSmall.sized(12, 18),
        color = if (isError) AppColors.Danger.Red600 else AppColors.Monotone.Gray600,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun DestinationCard(state: RouteUiState, onOpenStartPicker: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, CardShape)
            .padding(horizontal = AppDimens.Spacing16, vertical = AppDimens.Spacing12)
            .testTag("route-destination-card"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.place),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = AppColors.Monotone.SoftBlack
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = state.destinationName,
                style = MaterialTheme.typography.titleSmall.sized(15, 20),
                color = AppColors.Monotone.SoftBlack,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // 출발 위치를 고르기 전에는 "출발 위치 선택", 고른 뒤에는 "○○에서 출발"을 보여 주고 누르면 선택 화면이 열린다.
            // 선택 화면을 열 수 없는 단계(약속 이동)에서는 고른 위치만 글자로 보여 주고, 없으면 "약속 장소"를 쓴다.
            val startText = state.startLocation?.let { stringResource(R.string.route_start_from, it.name) }
                ?: if (onOpenStartPicker != null) {
                    stringResource(R.string.route_start_pick_prompt)
                } else {
                    stringResource(R.string.route_destination_caption)
                }
            Text(
                text = startText,
                style = MaterialTheme.typography.labelSmall.sized(11, 16),
                color = AppColors.Monotone.Gray600,
                textDecoration = if (onOpenStartPicker != null) TextDecoration.Underline else null,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = if (onOpenStartPicker != null) {
                    Modifier
                        .clickable(onClick = onOpenStartPicker)
                        .testTag("route-start-location-link")
                } else {
                    Modifier
                }
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = state.destinationEstimateTitle,
                style = MaterialTheme.typography.titleSmall.sized(13, 18),
                color = AppColors.MainGreen.Green700
            )
            Text(
                text = state.destinationEstimateNote,
                style = MaterialTheme.typography.labelSmall.sized(11, 16),
                color = AppColors.Monotone.Gray600
            )
        }
    }
}

@Composable
private fun OptionCard(
    option: RouteOptionUiState,
    selected: Boolean,
    expanded: Boolean,
    onSelect: () -> Unit,
    onToggle: () -> Unit
) {
    val borderModifier = if (selected) {
        Modifier.border(1.5.dp, AppColors.MainGreen.Green700, CardShape)
    } else {
        Modifier.border(1.dp, AppColors.Monotone.Gray100, CardShape)
    }
    val toggleDescription = stringResource(
        if (expanded) R.string.route_collapse_description else R.string.route_expand_description,
        option.title
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(Color.White)
            .then(borderModifier)
            .clickable(role = Role.RadioButton, onClick = onSelect)
            .semantics { this.selected = selected }
            .padding(horizontal = 14.dp, vertical = AppDimens.Spacing12)
            .testTag("route-option-${option.transport.name}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    if (selected) AppColors.MainGreen.Green900 else AppColors.Monotone.Gray100,
                    CircleShape
                )
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(option.transport.iconRes()),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = if (selected) Color.White else AppColors.Monotone.Gray700
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = option.title,
                    style = MaterialTheme.typography.titleSmall.sized(15, 20),
                    color = AppColors.Monotone.SoftBlack
                )
                Text(
                    text = option.durationLabel,
                    style = MaterialTheme.typography.titleSmall.sized(13, 18),
                    color = AppColors.Monotone.SoftBlack
                )
            }
            Text(
                text = option.summary,
                style = MaterialTheme.typography.labelSmall.sized(11, 16),
                color = AppColors.Monotone.Gray600,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onToggle)
                .semantics { contentDescription = toggleDescription }
                .testTag("route-option-toggle-${option.transport.name}"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(if (expanded) R.drawable.chevron_up else R.drawable.chevron_down),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = AppColors.Monotone.Gray700
            )
        }
    }
}

@Composable
private fun StepDetailCard(
    title: String,
    totalLabel: String?,
    steps: List<RouteStepUiState>,
    footnote: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White, CardShape)
            .padding(AppDimens.Spacing16),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall.sized(14, 21),
                color = AppColors.Monotone.SoftBlack
            )
            totalLabel?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleSmall.sized(12, 18),
                    color = AppColors.MainGreen.Green700
                )
            }
        }
        RouteStepList(steps)
        Text(
            text = footnote,
            style = MaterialTheme.typography.labelSmall.sized(11, 17),
            color = AppColors.Monotone.Gray600
        )
    }
}

@Composable
private fun RouteStepList(steps: List<RouteStepUiState>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        steps.forEachIndexed { index, step ->
            val transit = step.kind == RouteStepKind.BUS ||
                step.kind == RouteStepKind.SUBWAY ||
                step.kind == RouteStepKind.TAXI
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .background(AppColors.Monotone.Gray100, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(step.kind.iconRes()),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (transit) AppColors.MainGreen.Green700 else AppColors.Monotone.Gray700
                        )
                    }
                    if (index != steps.lastIndex) {
                        Spacer(Modifier.height(2.dp))
                        Box(
                            Modifier
                                .width(2.dp)
                                .height(30.dp)
                                .background(AppColors.Monotone.Gray300)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = step.title,
                        style = MaterialTheme.typography.titleSmall.sized(13, 20),
                        color = if (transit) AppColors.MainGreen.Green700 else AppColors.Monotone.SoftBlack
                    )
                    Text(
                        text = step.description,
                        style = MaterialTheme.typography.labelSmall.sized(12, 18),
                        color = AppColors.Monotone.Gray700
                    )
                    Text(
                        text = step.timeLabel,
                        style = MaterialTheme.typography.labelSmall.sized(11, 17),
                        color = AppColors.Monotone.Gray600
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeSummaryCard(summary: RouteTimeSummaryUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.MainGreen.Green700, CardShape)
            .padding(horizontal = 18.dp, vertical = 14.dp)
            .testTag("route-time-summary"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SummaryColumn(
            caption = summary.startCaption,
            time = summary.startTime,
            note = summary.startNote,
            modifier = Modifier.weight(1f)
        )
        Box(
            Modifier
                .width(1.dp)
                .height(44.dp)
                .background(Color.White.copy(alpha = 0.3f))
        )
        SummaryColumn(
            caption = summary.endCaption,
            time = summary.endTime,
            note = summary.endNote,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SummaryColumn(caption: String, time: String, note: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = caption,
            style = MaterialTheme.typography.labelSmall.sized(11, 16),
            color = AppColors.MainGreen.Green200
        )
        Text(
            text = time,
            style = MaterialTheme.typography.titleMedium.sized(22, 30),
            color = Color.White,
            maxLines = 1
        )
        Text(
            text = note,
            style = MaterialTheme.typography.labelSmall.sized(10, 15),
            color = AppColors.MainGreen.Green200
        )
    }
}

// ---------------------------------------------------------------- 약속 이동 (ROU-001-03)

@Composable
private fun MovingBody(
    state: RouteUiState,
    onToggleMyRoute: () -> Unit,
    onMyLocationClick: () -> Unit
) {
    RouteMap(state = state, onMyLocationClick = onMyLocationClick)
    Column(
        modifier = Modifier.padding(horizontal = AppDimens.Spacing20, vertical = AppDimens.Spacing12),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        DestinationCard(state)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.route_member_movement_title),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall.sized(14, 21),
                color = AppColors.Monotone.SoftBlack
            )
            Text(
                text = stringResource(R.string.route_my_route_only_hint),
                style = MaterialTheme.typography.labelSmall.sized(11, 17),
                color = AppColors.Monotone.Gray600
            )
        }
        val moving = state.moving
        state.members.forEach { member ->
            if (member.isMe && moving != null) {
                MyRouteCard(
                    member = member,
                    moving = moving,
                    expanded = state.isMyRouteExpanded,
                    onToggle = onToggleMyRoute
                )
            } else {
                FriendCard(member)
            }
        }
        moving?.let { TimeSummaryCard(it.timeSummary) }
    }
}

@Composable
private fun MyRouteCard(
    member: RouteMemberUiState,
    moving: RouteMovingUiState,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    val toggleDescription = stringResource(
        if (expanded) R.string.route_my_route_collapse_description else R.string.route_my_route_expand_description
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(Color.White)
            .border(1.5.dp, AppColors.MainGreen.Green700, CardShape)
            .padding(14.dp)
            .testTag("route-my-route-card"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(role = Role.Button, onClick = onToggle)
                .semantics { contentDescription = toggleDescription }
                .testTag("route-my-route-toggle"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Avatar(
                letter = member.name.take(1),
                size = 38,
                background = AppColors.MainGreen.Green900,
                textColor = Color.White
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = moving.myTitle,
                    style = MaterialTheme.typography.titleSmall.sized(14, 21),
                    color = AppColors.Monotone.SoftBlack
                )
                Text(
                    text = moving.mySubtitle,
                    style = MaterialTheme.typography.labelSmall.sized(12, 18),
                    color = AppColors.Monotone.Gray600
                )
            }
            Icon(
                painter = painterResource(if (expanded) R.drawable.chevron_up else R.drawable.chevron_down),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = AppColors.Monotone.Gray700
            )
        }
        if (expanded) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(AppColors.Monotone.Gray100)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.route_my_route_title),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall.sized(14, 21),
                    color = AppColors.Monotone.SoftBlack
                )
                Text(
                    text = moving.routeTotalLabel,
                    style = MaterialTheme.typography.titleSmall.sized(12, 18),
                    color = AppColors.MainGreen.Green700
                )
            }
            RouteStepList(moving.steps)
            Text(
                text = moving.footnote,
                style = MaterialTheme.typography.labelSmall.sized(11, 17),
                color = AppColors.Monotone.Gray600,
                modifier = Modifier.testTag("route-my-route-detail")
            )
        }
    }
}

@Composable
private fun FriendCard(member: RouteMemberUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, CardShape)
            .padding(AppDimens.Spacing12)
            .testTag("route-member-${member.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Avatar(
            letter = member.name.take(1),
            size = 38,
            background = AppColors.MainGreen.Green400,
            textColor = AppColors.MainGreen.Green900
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = member.name,
                style = MaterialTheme.typography.titleSmall.sized(14, 21),
                color = AppColors.Monotone.SoftBlack
            )
            Text(
                text = member.description,
                style = MaterialTheme.typography.labelSmall.sized(12, 18),
                color = AppColors.Monotone.Gray600
            )
        }
        member.transport?.let {
            Icon(
                painter = painterResource(it.iconRes()),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = AppColors.Monotone.Gray700
            )
        }
    }
}

// ---------------------------------------------------------------- 하단 버튼

@Composable
private fun ActionBar(state: RouteUiState, onStartMoving: () -> Unit, onArrive: () -> Unit) {
    val moving = state.moving
    val label = when (state.stage) {
        RouteStage.OVERVIEW -> stringResource(R.string.route_action_leave_now)
        RouteStage.SELECT -> stringResource(R.string.route_action_depart_now)
        RouteStage.MOVING -> stringResource(
            if (state.canArrive() || moving?.hasArrived == true) {
                R.string.route_action_arrive
            } else {
                R.string.route_action_moving
            }
        )
    }
    val enabled = when (state.stage) {
        RouteStage.OVERVIEW -> false
        RouteStage.SELECT -> state.canStart()
        RouteStage.MOVING -> state.canArrive()
    }
    if (state.actionStatus == RouteRequestStatus.FAILED) {
        Text(
            text = stringResource(R.string.route_action_failed),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimens.Spacing20)
                .padding(bottom = AppDimens.Spacing8)
                .testTag("route-action-failed"),
            style = MaterialTheme.typography.labelSmall.sized(12, 18),
            color = AppColors.Danger.Red600,
            textAlign = TextAlign.Center
        )
    }
    AppButton(
        text = label,
        variant = ButtonVariant.DEFAULT,
        enabled = enabled,
        onClick = if (state.stage == RouteStage.MOVING) onArrive else onStartMoving,
        modifier = Modifier
            .padding(horizontal = AppDimens.Spacing20)
            .padding(top = 1.dp, bottom = 15.dp)
            .testTag("route-action-button")
    )
}

// ---------------------------------------------------------------- 미리보기

@Composable
private fun RoutePreview(state: RouteUiState) {
    JigmnnagaTheme {
        RouteScreen(
            state = state,
            onBackClick = {},
            onCalendarClick = {},
            onPlaceClick = {},
            onMoreClick = {},
            onChatTabClick = {},
            onMyCardClick = {},
            onSelectTransport = {},
            onToggleOption = {},
            onStartMoving = {},
            onToggleMyRoute = {},
            onArrive = {},
            onMyLocationClick = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun RouteOverviewPreview() = RoutePreview(RouteSampleData.overview)

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun RouteSelectPreview() = RoutePreview(RouteSampleData.selecting)

@Preview(showBackground = true, widthDp = 412, heightDp = 1140)
@Composable
private fun RouteSelectBusExpandedPreview() =
    RoutePreview(RouteSampleData.selecting.toggleOptionExpanded(RouteTransport.BUS))

@Preview(showBackground = true, widthDp = 412, heightDp = 1140)
@Composable
private fun RouteSelectSubwayExpandedPreview() =
    RoutePreview(RouteSampleData.selecting.toggleOptionExpanded(RouteTransport.SUBWAY))

@Preview(showBackground = true, widthDp = 412, heightDp = 1140)
@Composable
private fun RouteSelectTaxiExpandedPreview() =
    RoutePreview(RouteSampleData.selecting.toggleOptionExpanded(RouteTransport.TAXI))

@Preview(showBackground = true, widthDp = 412, heightDp = 960)
@Composable
private fun RouteSelectBusSelectedPreview() =
    RoutePreview(RouteSampleData.selecting.selectTransport(RouteTransport.BUS))

@Preview(showBackground = true, widthDp = 412, heightDp = 960)
@Composable
private fun RouteMovingPreview() = RoutePreview(RouteSampleData.moving)

@Preview(showBackground = true, widthDp = 412, heightDp = 1250)
@Composable
private fun RouteMovingExpandedPreview() =
    RoutePreview(RouteSampleData.moving.toggleMyRouteExpanded())

@Preview(showBackground = true, widthDp = 412, heightDp = 960)
@Composable
private fun RouteNearArrivalPreview() = RoutePreview(RouteSampleData.nearArrival)

@Preview(showBackground = true, widthDp = 412, heightDp = 960)
@Composable
private fun RouteArrivedPreview() = RoutePreview(RouteSampleData.arrived)
