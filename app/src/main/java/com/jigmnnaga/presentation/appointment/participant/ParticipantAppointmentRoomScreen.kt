package com.jigmnnaga.presentation.appointment.participant

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
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
import com.jigmnnaga.core.designsystem.component.ChatInputBar
import com.jigmnnaga.core.designsystem.component.CircleButton
import com.jigmnnaga.core.designsystem.component.MeetingHeader
import com.jigmnnaga.core.designsystem.component.BackHeader
import com.jigmnnaga.core.designsystem.theme.AppColors
import com.jigmnnaga.core.designsystem.theme.AppDimens
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme
import com.jigmnnaga.presentation.chat.ChatListEffects
import com.jigmnnaga.presentation.chat.ChatMessageUiState

/**
 * Participant room presentation only. The caller owns state, navigation, system insets and service actions.
 * A map is rendered only when supplied by an integration; no fake map or server result is shown.
 */
@Composable
fun ParticipantAppointmentRoomScreen(
    state: ParticipantAppointmentRoomUiState,
    onBackClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onPlaceClick: () -> Unit,
    onManagementClick: () -> Unit,
    onTabSelected: (ParticipantAppointmentRoomTab) -> Unit,
    onSummaryExpandedChange: (Boolean) -> Unit,
    onMessageChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onDepartureClick: () -> Unit,
    modifier: Modifier = Modifier,
    mapContent: (@Composable (Modifier) -> Unit)? = null,
    canLoadMoreMessages: Boolean = false,
    onLoadMoreMessages: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.MainGreen.Green200)
            .testTag("participant-appointment-room")
    ) {
        MeetingHeader(
            onBackClick = onBackClick,
            onCalendarClick = onCalendarClick,
            onPlaceClick = onPlaceClick,
            onMoreClick = onManagementClick,
            title = state.title,
            subtitle = state.subtitle
        )
        ParticipantRoomTabs(state.selectedTab, onTabSelected)
        ParticipantAppointmentSummary(state, onSummaryExpandedChange)
        HorizontalDivider(color = AppColors.Monotone.Gray100)

        when (state.selectedTab) {
            ParticipantAppointmentRoomTab.CHAT -> {
                ParticipantChatList(state, canLoadMoreMessages, onLoadMoreMessages, Modifier.weight(1f))
                ChatInputBar(
                    value = state.draftMessage,
                    onValueChange = onMessageChange,
                    onSendClick = onSendMessage,
                    modifier = Modifier.testTag("participant-room-chat-input")
                )
            }
            ParticipantAppointmentRoomTab.MAP_STATUS -> {
                ParticipantMapStatus(
                    state = state,
                    mapContent = mapContent,
                    onDepartureClick = onDepartureClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** Participant-only management surface from APT-005-02 / APT-005-06. */
@Composable
fun ParticipantAppointmentManagementScreen(
    state: ParticipantAppointmentManagementUiState,
    onBackClick: () -> Unit,
    onEditAvailabilityClick: () -> Unit,
    onNotificationsEnabledChange: (Boolean) -> Unit,
    onLeaveAppointmentClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.MainGreen.Green200)
            .testTag("participant-appointment-management")
    ) {
        BackHeader(
            title = stringResource(R.string.participant_room_management_title),
            onBackClick = onBackClick
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = AppDimens.Spacing20,
                vertical = AppDimens.Spacing8
            ),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing16)
        ) {
            item {
                ManagementSectionTitle(
                    title = stringResource(R.string.participant_room_appointment_info),
                    trailing = stringResource(R.string.participant_room_badge)
                )
                ManagementCard {
                    InfoLine(stringResource(R.string.participant_room_name_label), state.appointmentName)
                    HorizontalDivider(color = AppColors.Monotone.Gray100)
                    InfoLine(stringResource(R.string.participant_room_date_label), state.dateSummary ?: stringResource(R.string.participant_room_unavailable_value))
                    HorizontalDivider(color = AppColors.Monotone.Gray100)
                    InfoLine(stringResource(R.string.participant_room_place_label), state.placeSummary ?: stringResource(R.string.participant_room_unavailable_value))
                }
                Text(
                    text = stringResource(R.string.participant_room_host_only_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.Monotone.Gray500,
                    modifier = Modifier.padding(top = AppDimens.Spacing8)
                )
            }
            item {
                ManagementSectionTitle(
                    title = stringResource(R.string.participant_room_members_title),
                    trailing = stringResource(R.string.participant_room_member_count, state.members.size)
                )
                ManagementCard {
                    state.members.forEachIndexed { index, member ->
                        MemberRow(member)
                        if (index != state.members.lastIndex) {
                            Spacer(Modifier.height(AppDimens.Spacing8))
                        }
                    }
                }
            }
            item {
                Text(
                    text = stringResource(R.string.participant_room_my_settings),
                    style = MaterialTheme.typography.titleSmall,
                    color = AppColors.Monotone.SoftBlack
                )
                ManagementCard {
                    ManagementActionRow(
                        title = stringResource(R.string.participant_room_edit_availability),
                        subtitle = state.myAvailabilitySummary.orEmpty(),
                        contentDescription = stringResource(R.string.participant_room_edit_availability)
                    ) { onEditAvailabilityClick() }
                    HorizontalDivider(color = AppColors.Monotone.Gray100)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = AppDimens.Spacing12),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.participant_room_notifications), style = MaterialTheme.typography.bodyMedium)
                            Text(stringResource(R.string.participant_room_notifications_description), style = MaterialTheme.typography.bodySmall, color = AppColors.Monotone.Gray500)
                        }
                        Switch(
                            checked = state.isNotificationsEnabled,
                            onCheckedChange = onNotificationsEnabledChange,
                            modifier = Modifier.testTag("participant-room-notifications")
                        )
                    }
                }
            }
            item {
                ManagementActionRow(
                    title = stringResource(R.string.participant_room_leave),
                    subtitle = stringResource(R.string.participant_room_leave_description),
                    contentDescription = stringResource(R.string.participant_room_leave)
                ) { onLeaveAppointmentClick() }
            }
        }
    }
}

@Composable
private fun ParticipantRoomTabs(
    selectedTab: ParticipantAppointmentRoomTab,
    onTabSelected: (ParticipantAppointmentRoomTab) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(48.dp).padding(horizontal = AppDimens.Spacing20),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing24),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RoomTab(
            label = stringResource(R.string.participant_room_tab_chat),
            selected = selectedTab == ParticipantAppointmentRoomTab.CHAT,
            onClick = { onTabSelected(ParticipantAppointmentRoomTab.CHAT) },
            icon = null
        )
        RoomTab(
            label = stringResource(R.string.participant_room_tab_map_status),
            selected = selectedTab == ParticipantAppointmentRoomTab.MAP_STATUS,
            onClick = { onTabSelected(ParticipantAppointmentRoomTab.MAP_STATUS) },
            icon = R.drawable.place
        )
    }
}

@Composable
private fun RoomTab(label: String, selected: Boolean, onClick: () -> Unit, icon: Int?) {
    val tint = if (selected) AppColors.MainGreen.Green900 else AppColors.Monotone.Gray400
    Row(
        modifier = Modifier
            .height(48.dp)
            .clip(RoundedCornerShape(AppDimens.Spacing8))
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics(mergeDescendants = true) { this.selected = selected }
            .testTag("participant-tab-$label"),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing4),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let {
            androidx.compose.foundation.Image(
                painter = painterResource(it),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(tint)
            )
        }
        Text(label, color = tint, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

@Composable
private fun ParticipantAppointmentSummary(
    state: ParticipantAppointmentRoomUiState,
    onExpandedChange: (Boolean) -> Unit
) {
    val summaryDescription = if (state.isSummaryExpanded) {
        "${stringResource(R.string.participant_room_summary_title)}, ${stringResource(R.string.participant_room_summary_collapse)}"
    } else {
        "${stringResource(R.string.participant_room_summary_title)}, ${stringResource(R.string.participant_room_summary_expand)}"
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.Spacing20, vertical = AppDimens.Spacing8)
            .testTag("participant-room-summary"),
        color = AppColors.Monotone.White,
        shape = RoundedCornerShape(AppDimens.Spacing16)
    ) {
        Column(Modifier.padding(AppDimens.Spacing16)) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(AppDimens.Spacing4))
                    .clickable { onExpandedChange(!state.isSummaryExpanded) }
                    .semantics(mergeDescendants = true) {
                        contentDescription = summaryDescription
                    }
                    .testTag("participant-room-summary-toggle"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.participant_room_summary_title),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    color = AppColors.Monotone.SoftBlack
                )
                state.scheduleStatusLabel?.takeIf(String::isNotBlank)?.let {
                    Text(
                        text = it,
                        modifier = Modifier.clip(CircleShape).background(AppColors.Monotone.Gray100)
                            .padding(horizontal = AppDimens.Spacing8, vertical = AppDimens.Spacing4),
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.Monotone.Gray600
                    )
                }
            }
            if (state.isSummaryExpanded) {
                Spacer(Modifier.height(AppDimens.Spacing12))
                InfoLine(stringResource(R.string.participant_room_date_label), state.dateSummary ?: stringResource(R.string.participant_room_unavailable_value))
                Spacer(Modifier.height(AppDimens.Spacing8))
                InfoLine(stringResource(R.string.participant_room_place_label), state.placeSummary ?: stringResource(R.string.participant_room_unavailable_value))
            }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = AppDimens.Spacing8),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
    ) {
        Text(label, color = AppColors.Monotone.Gray500, style = MaterialTheme.typography.labelMedium)
        Text(
            value,
            modifier = Modifier.weight(1f),
            color = AppColors.Monotone.SoftBlack,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ParticipantChatList(
    state: ParticipantAppointmentRoomUiState,
    canLoadMore: Boolean,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    ChatListEffects(
        listState = listState,
        messages = state.messages.map { ChatMessageUiState(id = it.id, body = it.body, isMine = it.isMine) },
        hasMore = canLoadMore,
        onLoadMore = onLoadMore
    )
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth().padding(horizontal = AppDimens.Spacing20),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = AppDimens.Spacing16),
        verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
    ) {
        items(state.messages, key = { it.id }) { message ->
            if (message.isSystem) {
                Text(
                    text = message.body,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape)
                        .background(AppColors.Monotone.Gray100)
                        .padding(horizontal = AppDimens.Spacing12, vertical = AppDimens.Spacing8),
                    color = AppColors.Monotone.Gray600,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center
                )
                return@items
            }
            Column(horizontalAlignment = if (message.isMine) Alignment.End else Alignment.Start) {
                message.senderName?.takeIf(String::isNotBlank)?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = AppColors.Monotone.Gray500)
                }
                Surface(
                    color = if (message.isMine) AppColors.MainGreen.Green700 else AppColors.Monotone.White,
                    shape = RoundedCornerShape(AppDimens.Spacing16)
                ) {
                    Text(
                        text = message.body,
                        modifier = Modifier.width(280.dp).padding(horizontal = AppDimens.Spacing12, vertical = AppDimens.Spacing8),
                        color = if (message.isMine) AppColors.Monotone.White else AppColors.Monotone.SoftBlack,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (message.unreadCount > 0) {
                    Text(
                        text = message.unreadCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.MainGreen.Green700,
                        modifier = Modifier.testTag("chat-unread-${message.id}")
                    )
                }
                message.timeLabel?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = AppColors.Monotone.Gray400)
                }
            }
        }
    }
}

@Composable
private fun ParticipantMapStatus(
    state: ParticipantAppointmentRoomUiState,
    mapContent: (@Composable (Modifier) -> Unit)?,
    onDepartureClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth()) {
        if (mapContent != null && state.appointmentStatus == ParticipantAppointmentStatus.CONFIRMED) {
            mapContent(Modifier.fillMaxWidth().height(220.dp))
        } else {
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = AppDimens.Spacing20, vertical = AppDimens.Spacing12)
                    .height(156.dp).clip(RoundedCornerShape(AppDimens.Spacing12))
                    .background(AppColors.Monotone.Gray100),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(
                        if (state.appointmentStatus == ParticipantAppointmentStatus.AWAITING_CONFIRMATION) {
                            R.string.participant_room_map_waiting
                        } else R.string.participant_room_map_not_connected
                    ),
                    color = AppColors.Monotone.Gray600,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        if (state.appointmentStatus == ParticipantAppointmentStatus.CONFIRMED) {
            ConfirmedPlaceSummary(state)
        } else {
            Text(
                text = stringResource(R.string.participant_room_members_title),
                modifier = Modifier.padding(horizontal = AppDimens.Spacing20, vertical = AppDimens.Spacing8),
                style = MaterialTheme.typography.titleSmall,
                color = AppColors.Monotone.SoftBlack
            )
        }
        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = AppDimens.Spacing20),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = AppDimens.Spacing4),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
        ) {
            items(state.members, key = { it.id }) { MemberRow(it) }
        }
        if (state.appointmentStatus == ParticipantAppointmentStatus.CONFIRMED) {
            CircleButton(
                contentDescription = stringResource(R.string.participant_room_departure_action),
                onClick = onDepartureClick,
                width = 165.dp,
                height = AppDimens.ButtonHeight,
                text = stringResource(R.string.participant_room_departure_action),
                backgroundColor = AppColors.MainGreen.Green700,
                contentColor = AppColors.Monotone.White,
                enabled = state.canReportDeparture,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = AppDimens.Spacing12)
            )
        }
    }
}

@Composable
private fun ConfirmedPlaceSummary(state: ParticipantAppointmentRoomUiState) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = AppDimens.Spacing20, vertical = AppDimens.Spacing8),
        shape = RoundedCornerShape(AppDimens.Spacing8),
        color = AppColors.MainGreen.Green700
    ) {
        Column(Modifier.padding(AppDimens.Spacing12)) {
            Text(stringResource(R.string.participant_room_confirmed_place), style = MaterialTheme.typography.labelSmall, color = AppColors.Monotone.White)
            Text(state.placeSummary.orEmpty(), style = MaterialTheme.typography.titleSmall, color = AppColors.Monotone.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(state.dateSummary.orEmpty(), style = MaterialTheme.typography.bodySmall, color = AppColors.Monotone.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun MemberRow(member: ParticipantRoomMemberUiState) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(AppDimens.Spacing12))
            .background(AppColors.Monotone.White).padding(AppDimens.Spacing8),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
    ) {
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape)
                .background(if (member.isCurrentUser) AppColors.MainGreen.Green700 else AppColors.MainGreen.Green400),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = member.name.firstOrNull()?.toString().orEmpty(),
                color = AppColors.Monotone.White,
                style = MaterialTheme.typography.labelMedium
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                member.name,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.Monotone.SoftBlack,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            member.statusLabel?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = AppColors.Monotone.Gray500, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun ManagementSectionTitle(title: String, trailing: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = AppDimens.Spacing8),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = AppColors.Monotone.SoftBlack)
        Text(trailing, style = MaterialTheme.typography.labelSmall, color = AppColors.MainGreen.Green700)
    }
}

@Composable
private fun ManagementCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimens.Spacing12),
        color = AppColors.Monotone.White
    ) {
        Column(Modifier.padding(horizontal = AppDimens.Spacing12, vertical = AppDimens.Spacing8), content = content)
    }
}

@Composable
private fun ManagementActionRow(
    title: String,
    subtitle: String,
    contentDescription: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(AppDimens.Spacing8))
            .clickable(onClick = onClick)
            .semantics { this.contentDescription = contentDescription }
            .padding(vertical = AppDimens.Spacing12)
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium, color = AppColors.Monotone.SoftBlack)
        if (subtitle.isNotBlank()) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = AppColors.Monotone.Gray500, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun ParticipantRoomPreview() {
    JigmnnagaTheme {
        ParticipantAppointmentRoomScreen(
            state = ParticipantAppointmentRoomUiState(
                title = "중간고사 카공 모임",
                subtitle = "3명 · 약속방",
                appointmentStatus = ParticipantAppointmentStatus.AWAITING_CONFIRMATION,
                scheduleStatusLabel = "확정 전",
                dateSummary = "확정 전",
                placeSummary = "확정 전",
                members = listOf(
                    ParticipantRoomMemberUiState("me", "나", "아직 일정 선택 전이에요", true),
                    ParticipantRoomMemberUiState("m1", "태양", "아직 일정 선택 전이에요"),
                    ParticipantRoomMemberUiState("m2", "하은", "아직 일정 선택 전이에요")
                ),
                messages = listOf(ParticipantRoomMessageUiState("m", "다들 이번 주 토요일 어때요?", "태양", "오후 2:22"))
            ),
            onBackClick = {}, onCalendarClick = {}, onPlaceClick = {}, onManagementClick = {},
            onTabSelected = {}, onSummaryExpandedChange = {}, onMessageChange = {},
            onSendMessage = {}, onDepartureClick = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun ParticipantRoomManagementPreview() {
    JigmnnagaTheme {
        ParticipantAppointmentManagementScreen(
            state = ParticipantAppointmentManagementUiState(
                appointmentName = "주말 약속",
                scheduleStatusLabel = "확정 전",
                members = listOf(
                    ParticipantRoomMemberUiState("me", "나", "일정 선택 완료", true),
                    ParticipantRoomMemberUiState("m1", "민지", "응답 대기")
                ),
                myAvailabilitySummary = "토요일 오후"
            ),
            onBackClick = {}, onEditAvailabilityClick = {},
            onNotificationsEnabledChange = {}, onLeaveAppointmentClick = {}
        )
    }
}
