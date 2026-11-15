package com.jigmnnaga.presentation.appointment.host

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jigmnnaga.R
import com.jigmnnaga.presentation.appointment.RoomMemberStatusUi
import com.jigmnnaga.presentation.appointment.RoomPendingMapStatus
import com.jigmnnaga.presentation.appointment.RoomSummaryDetails
import com.jigmnnaga.presentation.appointment.RoomTabRow
import com.jigmnnaga.core.designsystem.component.ChatInputBar
import com.jigmnnaga.core.designsystem.component.MeetingHeader
import com.jigmnnaga.core.designsystem.theme.AppColors
import com.jigmnnaga.core.designsystem.theme.AppDimens
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme
import com.jigmnnaga.presentation.chat.ChatListEffects
import com.jigmnnaga.presentation.chat.ChatMessageUiState

/**
 * Host-room UI only. Data and navigation actions are owned by the caller; no API, map SDK,
 * WebSocket, or navigation dependency is introduced here.
 */
@Composable
fun HostAppointmentRoomScreen(
    state: HostAppointmentRoomUiState,
    onBackClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onPlaceClick: () -> Unit,
    onMoreClick: () -> Unit,
    onTabSelected: (HostAppointmentRoomTab) -> Unit,
    onSummaryExpandedChange: (Boolean) -> Unit,
    onMessageChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    modifier: Modifier = Modifier,
    mapContent: (@Composable (Modifier) -> Unit)? = null,
    canLoadMoreMessages: Boolean = false,
    onLoadMoreMessages: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.MainGreen.Green200)
            .safeDrawingPadding()
            .imePadding()
            .testTag("host-appointment-room")
    ) {
        MeetingHeader(
            onBackClick = onBackClick,
            onCalendarClick = onCalendarClick,
            onPlaceClick = onPlaceClick,
            onMoreClick = onMoreClick,
            title = state.title,
            subtitle = state.subtitle
        )
        HostAppointmentRoomTabs(
            selectedTab = state.selectedTab,
            onTabSelected = onTabSelected
        )

        when (state.selectedTab) {
            HostAppointmentRoomTab.CHAT -> {
                AppointmentSummaryCard(
                    state = state,
                    onExpandedChange = onSummaryExpandedChange
                )
                HostChatContent(
                    state = state,
                    canLoadMore = canLoadMoreMessages,
                    onLoadMore = onLoadMoreMessages,
                    modifier = Modifier.weight(1f)
                )
                ChatInputBar(
                    value = state.draftMessage,
                    onValueChange = onMessageChange,
                    onSendClick = onSendMessage,
                    modifier = Modifier.testTag("host-room-chat-input")
                )
            }

            HostAppointmentRoomTab.MAP_STATUS -> {
                if (!state.isConfirmed) {
                    RoomPendingMapStatus(
                        members = state.participants.map {
                            RoomMemberStatusUi(it.name, it.isHost, it.hasResponded)
                        },
                        modifier = Modifier.weight(1f).testTag("host-room-map-status")
                    )
                } else {
                    HostMapStatusContent(
                        state = state,
                        mapContent = mapContent,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HostAppointmentRoomTabs(
    selectedTab: HostAppointmentRoomTab,
    onTabSelected: (HostAppointmentRoomTab) -> Unit
) {
    RoomTabRow(
        chatLabel = stringResource(R.string.host_room_tab_chat),
        mapLabel = stringResource(R.string.host_room_tab_map_status),
        isMapSelected = selectedTab == HostAppointmentRoomTab.MAP_STATUS,
        onChatClick = { onTabSelected(HostAppointmentRoomTab.CHAT) },
        onMapClick = { onTabSelected(HostAppointmentRoomTab.MAP_STATUS) },
        chatTestTag = "host-room-tab-chat",
        mapTestTag = "host-room-tab-map"
    )
}

@Composable
private fun AppointmentSummaryCard(
    state: HostAppointmentRoomUiState,
    onExpandedChange: (Boolean) -> Unit
) {
    val toggleDescription = stringResource(
        if (state.isSummaryExpanded) {
            R.string.host_room_summary_collapse
        } else {
            R.string.host_room_summary_expand
        }
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.Spacing20, vertical = AppDimens.Spacing8)
            .testTag("host-room-summary"),
        color = AppColors.Monotone.White,
        shape = RoundedCornerShape(AppDimens.Spacing16)
    ) {
        Column(modifier = Modifier.padding(AppDimens.Spacing16)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AppDimens.Spacing4))
                    .clickable { onExpandedChange(!state.isSummaryExpanded) }
                    .semantics(mergeDescendants = true) {},
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
            ) {
                Text(
                    text = stringResource(R.string.host_room_summary_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = AppColors.Monotone.SoftBlack,
                    modifier = Modifier.weight(1f)
                )
                if (!state.scheduleStatusLabel.isNullOrBlank()) {
                    Text(
                        text = state.scheduleStatusLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = AppColors.Monotone.Gray600,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(AppColors.Monotone.Gray100)
                            .padding(horizontal = AppDimens.Spacing8, vertical = AppDimens.Spacing4)
                    )
                }
                androidx.compose.foundation.Image(
                    painter = painterResource(
                        if (state.isSummaryExpanded) R.drawable.chevron_up else R.drawable.chevron_down
                    ),
                    contentDescription = toggleDescription,
                    modifier = Modifier
                        .size(AppDimens.HeaderIconSize)
                        .testTag("host-room-summary-toggle")
                )
            }

            if (state.isSummaryExpanded) {
                Spacer(Modifier.height(AppDimens.Spacing12))
                val empty = stringResource(R.string.room_summary_candidate_empty)
                if (state.isConfirmed) {
                    RoomSummaryDetails(
                        dateLabel = stringResource(R.string.host_room_date_label),
                        dateValue = state.dateSummary ?: empty,
                        placeLabel = stringResource(R.string.host_room_place_label),
                        placeValue = state.placeSummary ?: empty,
                        showUndecidedBadge = false
                    )
                } else {
                    RoomSummaryDetails(
                        dateLabel = stringResource(R.string.room_summary_candidate_date),
                        dateValue = state.candidateDateSummary ?: empty,
                        placeLabel = stringResource(R.string.room_summary_candidate_place),
                        placeValue = state.candidatePlaceSummary ?: empty,
                        showUndecidedBadge = true
                    )
                }
            }
        }
    }
}

@Composable
private fun HostChatContent(
    state: HostAppointmentRoomUiState,
    canLoadMore: Boolean,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    ChatListEffects(
        listState = listState,
        messages = state.messages.map { ChatMessageUiState(id = it.id, body = it.body, isMine = it.isMine) },
        hasMore = canLoadMore,
        onLoadMore = onLoadMore,
        leadingItemCount = if (state.systemNotice != null) 1 else 0
    )
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .testTag("host-room-chat-list"),
        contentPadding = PaddingValues(
            horizontal = AppDimens.Spacing20,
            vertical = AppDimens.Spacing12
        ),
        verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
    ) {
        state.systemNotice?.let { notice ->
            item(key = "system-notice") {
                Text(
                    text = notice,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape)
                        .background(AppColors.Monotone.Gray100)
                        .padding(horizontal = AppDimens.Spacing12, vertical = AppDimens.Spacing8),
                    color = AppColors.Monotone.Gray600,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        items(state.messages, key = { it.id }) { message ->
            ChatMessageBubble(message)
        }
    }
}

@Composable
private fun ChatMessageBubble(message: HostRoomMessageUiState) {
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
        return
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isMine) Alignment.End else Alignment.Start
    ) {
        if (!message.isMine && !message.senderName.isNullOrBlank()) {
            Text(
                text = message.senderName,
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.Monotone.Gray600,
                modifier = Modifier.padding(start = AppDimens.Spacing8, bottom = AppDimens.Spacing4)
            )
        }
        Text(
            text = message.body,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(AppDimens.Spacing16))
                .background(if (message.isMine) AppColors.MainGreen.Green700 else AppColors.Monotone.White)
                .padding(horizontal = AppDimens.Spacing12, vertical = AppDimens.Spacing8),
            color = if (message.isMine) AppColors.MainGreen.Green200 else AppColors.Monotone.SoftBlack,
            style = MaterialTheme.typography.bodyMedium
        )
        if (message.unreadCount > 0) {
            Text(
                text = message.unreadCount.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.MainGreen.Green700,
                modifier = Modifier
                    .padding(horizontal = AppDimens.Spacing8)
                    .testTag("chat-unread-${message.id}")
            )
        }
        message.timeLabel?.let { time ->
            Text(
                text = time,
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.Monotone.Gray500,
                modifier = Modifier.padding(horizontal = AppDimens.Spacing8, vertical = AppDimens.Spacing4)
            )
        }
    }
}

@Composable
private fun HostMapStatusContent(
    state: HostAppointmentRoomUiState,
    mapContent: (@Composable (Modifier) -> Unit)?,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .testTag("host-room-map-status"),
        contentPadding = PaddingValues(AppDimens.Spacing20),
        verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing16)
    ) {
        item {
            if (mapContent != null) {
                mapContent(Modifier.fillMaxWidth())
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(AppDimens.Spacing16))
                        .background(AppColors.Monotone.Gray100),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.host_room_map_not_connected),
                        color = AppColors.Monotone.Gray600,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        item {
            ResponseOverview(state)
        }
        if (state.participants.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.host_room_participants_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = AppColors.Monotone.SoftBlack
                )
            }
            items(state.participants, key = { "participant-${it.id}" }) { participant ->
                ParticipantRow(participant)
            }
        }
    }
}

@Composable
private fun ResponseOverview(state: HostAppointmentRoomUiState) {
    val total = state.participantCount.coerceAtLeast(0)
    val answered = state.responseCount.coerceIn(0, total.coerceAtLeast(0))
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AppColors.Monotone.White,
        shape = RoundedCornerShape(AppDimens.Spacing16)
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.Spacing16),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.host_room_response_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = AppColors.Monotone.SoftBlack,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    stringResource(R.string.host_room_response_count, answered, total),
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.MainGreen.Green700
                )
            }
            LinearProgressIndicator(
                progress = { if (total == 0) 0f else answered.toFloat() / total },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                color = AppColors.MainGreen.Green700,
                trackColor = AppColors.Monotone.Gray100
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
            ) {
                Text(
                    stringResource(R.string.host_room_response_done_count, answered),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.Monotone.Gray600,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    stringResource(
                        R.string.host_room_response_pending_count,
                        (total - answered).coerceAtLeast(0)
                    ),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.Monotone.Gray600,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ParticipantRow(participant: HostRoomParticipantUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.Spacing12))
            .background(AppColors.Monotone.White)
            .padding(horizontal = AppDimens.Spacing16, vertical = AppDimens.Spacing12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(AppColors.MainGreen.Green400),
            contentAlignment = Alignment.Center
        ) {
            Text(
                participant.name.take(1),
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.MainGreen.Green900
            )
        }
        Text(
            participant.name,
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.Monotone.SoftBlack,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (participant.isHost) {
            Text(
                stringResource(R.string.host_room_host_badge),
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.MainGreen.Green700
            )
        }
        participant.statusLabel?.let { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.Monotone.Gray600,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun HostAppointmentRoomChatPreview() {
    JigmnnagaTheme {
        HostAppointmentRoomScreen(
            state = previewHostRoomState(),
            onBackClick = {},
            onCalendarClick = {},
            onPlaceClick = {},
            onMoreClick = {},
            onTabSelected = {},
            onSummaryExpandedChange = {},
            onMessageChange = {},
            onSendMessage = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 800, fontScale = 1.5f)
@Composable
private fun HostAppointmentRoomNarrowPreview() {
    JigmnnagaTheme {
        HostAppointmentRoomScreen(
            state = previewHostRoomState().copy(isSummaryExpanded = true),
            onBackClick = {},
            onCalendarClick = {},
            onPlaceClick = {},
            onMoreClick = {},
            onTabSelected = {},
            onSummaryExpandedChange = {},
            onMessageChange = {},
            onSendMessage = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun HostAppointmentRoomMapStatusPreview() {
    JigmnnagaTheme {
        HostAppointmentRoomScreen(
            state = previewHostRoomState().copy(
                selectedTab = HostAppointmentRoomTab.MAP_STATUS,
                isSummaryExpanded = true
            ),
            onBackClick = {},
            onCalendarClick = {},
            onPlaceClick = {},
            onMoreClick = {},
            onTabSelected = {},
            onSummaryExpandedChange = {},
            onMessageChange = {},
            onSendMessage = {}
        )
    }
}

private fun previewHostRoomState() = HostAppointmentRoomUiState(
    title = "중간고사 카공 모임",
    subtitle = "3명 · 약속방",
    scheduleStatusLabel = "확정 전",
    dateSummary = "9월 4일–9월 11일",
    placeSummary = "강남역, 잠실역",
    responseCount = 2,
    participantCount = 3,
    participants = listOf(
        HostRoomParticipantUiState("host", "나", "응답 완료", isHost = true),
        HostRoomParticipantUiState("member-1", "태희", "응답 완료"),
        HostRoomParticipantUiState("member-2", "하은", "미응답")
    ),
    messages = listOf(
        HostRoomMessageUiState("m1", "태희", "저는 오후 6시 이후면 괜찮아요!", "오후 1:24"),
        HostRoomMessageUiState("m2", "하은", "저도 토요일 저녁 좋아요", "오후 1:25"),
        HostRoomMessageUiState("m3", null, "다들 이번 주 토요일 어때요?", "오후 1:22", isMine = true)
    ),
    systemNotice = "채팅방이 열렸어요"
)
