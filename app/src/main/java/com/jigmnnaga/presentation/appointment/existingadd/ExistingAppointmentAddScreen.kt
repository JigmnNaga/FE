package com.jigmnnaga.presentation.appointment.existingadd

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jigmnnaga.R
import com.jigmnnaga.core.designsystem.component.AppButton
import com.jigmnnaga.core.designsystem.component.ButtonVariant
import com.jigmnnaga.core.designsystem.component.MeetingHeader
import com.jigmnnaga.core.designsystem.theme.AppColors
import com.jigmnnaga.core.designsystem.theme.AppDimens
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme

/**
 * APT-006 presentation only. Data, selection rules, transitions, and all side effects are supplied
 * by the caller. The optional map slot is intentionally empty by default; it does not fabricate
 * map tiles or locations when a map provider is not connected.
 *
 * System bars and IME insets remain the responsibility of the parent screen/Scaffold.
 */
@Composable
fun ExistingAppointmentAddScreen(
    state: ExistingAppointmentAddUiState,
    onBackClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onPlaceClick: () -> Unit,
    onMoreClick: () -> Unit,
    onChatTabClick: () -> Unit,
    onMapAndStatusTabClick: () -> Unit,
    onMemberSelectionChange: (memberId: String, selected: Boolean) -> Unit,
    onRescheduleClick: () -> Unit,
    onRequestClick: () -> Unit,
    modifier: Modifier = Modifier,
    mapContent: @Composable BoxScope.() -> Unit = {}
) {
    val mapAreaDescription = stringResource(R.string.existing_add_map_area)
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

        RoomTabs(
            selectedTab = state.selectedRoomTab,
            onChatClick = onChatTabClick,
            onMapAndStatusClick = onMapAndStatusTabClick
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppDimens.Spacing16),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
                    .clip(RoundedCornerShape(AppDimens.ButtonCornerRadius))
                    .background(AppColors.Monotone.Gray100)
                    .semantics {
                        contentDescription = mapAreaDescription
                    },
                content = mapContent
            )

            AppointmentSummary(
                scheduledTime = state.scheduledTime,
                scheduledPlace = state.scheduledPlace
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.existing_add_member_status),
                    style = MaterialTheme.typography.titleSmall,
                    color = AppColors.Monotone.SoftBlack
                )
                state.liveStatusLabel?.let { label -> LiveStatusChip(label) }
            }

            state.members.forEach { member ->
                MemberRow(
                    member = member,
                    selectable = state.page == ExistingAppointmentAddPage.REQUEST_PARTICIPANTS && member.isSelectable,
                    selected = member.id in state.selectedMemberIds,
                    onSelectionChange = { selected -> onMemberSelectionChange(member.id, selected) }
                )
            }
        }

        AppButton(
            text = stringResource(
                if (state.page == ExistingAppointmentAddPage.RESCHEDULE) {
                    R.string.existing_add_reschedule
                } else {
                    R.string.existing_add_request
                }
            ),
            variant = ButtonVariant.DEFAULT,
            enabled = state.page == ExistingAppointmentAddPage.RESCHEDULE || state.canRequest,
            onClick = if (state.page == ExistingAppointmentAddPage.RESCHEDULE) onRescheduleClick else onRequestClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimens.Spacing16)
                .padding(top = AppDimens.Spacing8, bottom = AppDimens.Spacing16)
        )
    }
}

@Composable
private fun RoomTabs(
    selectedTab: ExistingAppointmentRoomTab,
    onChatClick: () -> Unit,
    onMapAndStatusClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.Spacing16),
        horizontalArrangement = Arrangement.Center
    ) {
        RoomTab(
            label = stringResource(R.string.existing_add_chat_tab),
            selected = selectedTab == ExistingAppointmentRoomTab.CHAT,
            onClick = onChatClick
        )
        Spacer(Modifier.width(AppDimens.Spacing24))
        RoomTab(
            label = stringResource(R.string.existing_add_map_status_tab),
            selected = selectedTab == ExistingAppointmentRoomTab.MAP_AND_STATUS,
            onClick = onMapAndStatusClick
        )
    }
}

@Composable
private fun RoomTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .selectable(selected = selected, onClick = onClick, role = Role.Tab)
            .padding(vertical = AppDimens.Spacing8),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) AppColors.Monotone.SoftBlack else AppColors.Monotone.Gray600
        )
        Spacer(Modifier.height(AppDimens.Spacing4))
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(2.dp)
                .background(
                    color = if (selected) AppColors.MainGreen.Green900 else AppColors.MainGreen.Green200,
                    shape = CircleShape
                )
        )
    }
}

@Composable
private fun LiveStatusChip(label: String) {
    Row(
        modifier = Modifier
            .background(AppColors.Monotone.Gray100, CircleShape)
            .padding(horizontal = AppDimens.Spacing8, vertical = AppDimens.Spacing4),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(AppDimens.Spacing4)
                .background(AppColors.SubYellow.Yellow500, CircleShape)
        )
        Spacer(Modifier.width(AppDimens.Spacing4))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.Monotone.Gray500
        )
    }
}

@Composable
private fun AppointmentSummary(scheduledTime: String, scheduledPlace: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimens.ButtonCornerRadius),
        color = AppColors.MainGreen.Green700
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = AppDimens.Spacing16,
                vertical = AppDimens.Spacing12
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = stringResource(R.string.existing_add_scheduled_time),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.MainGreen.Green200
                )
                Text(
                    text = scheduledTime,
                    style = MaterialTheme.typography.titleMedium,
                    color = AppColors.MainGreen.Green200,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = scheduledPlace,
                modifier = Modifier.weight(1f).padding(start = AppDimens.Spacing12),
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.MainGreen.Green200,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MemberRow(
    member: ExistingAppointmentMemberUiState,
    selectable: Boolean,
    selected: Boolean,
    onSelectionChange: (Boolean) -> Unit
) {
    val selectedBorder = if (selected) AppColors.SubYellow.Yellow600 else AppColors.Monotone.Gray100
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (selectable) {
                    Modifier
                        .clip(RoundedCornerShape(AppDimens.ButtonCornerRadius))
                        .toggleable(
                            value = selected,
                            onValueChange = onSelectionChange,
                            role = Role.Checkbox
                        )
                } else {
                    Modifier
                }
            )
            .border(
                border = BorderStroke(1.dp, selectedBorder),
                shape = RoundedCornerShape(AppDimens.ButtonCornerRadius)
            )
            .background(AppColors.Monotone.Gray100, RoundedCornerShape(AppDimens.ButtonCornerRadius))
            .padding(horizontal = AppDimens.Spacing12, vertical = AppDimens.Spacing8)
            .semantics(mergeDescendants = true) {
                if (selectable) {
                    contentDescription = "${member.name}, ${member.statusLabel}"
                }
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(AppDimens.HeaderActionSize)
                .background(AppColors.MainGreen.Green400, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = member.name.take(1),
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.Monotone.White
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = AppDimens.Spacing8),
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = member.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = AppColors.Monotone.SoftBlack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(AppDimens.Spacing8))
                Text(
                    text = member.statusLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.Monotone.Gray600,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            member.detailLabel?.let { detail ->
                Text(
                    text = detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.Monotone.Gray500,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        member.trailingLabel?.let { trailing ->
            Text(
                text = trailing,
                modifier = Modifier.padding(start = AppDimens.Spacing8),
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.Monotone.Gray500,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun ExistingAppointmentAddOverviewPreview() {
    JigmnnagaTheme {
        ExistingAppointmentAddScreen(
            state = previewState(ExistingAppointmentAddPage.RESCHEDULE),
            onBackClick = {}, onCalendarClick = {}, onPlaceClick = {}, onMoreClick = {},
            onChatTabClick = {}, onMapAndStatusTabClick = {}, onMemberSelectionChange = { _, _ -> },
            onRescheduleClick = {}, onRequestClick = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 720)
@Composable
private fun ExistingAppointmentAddRequestPreview() {
    JigmnnagaTheme {
        ExistingAppointmentAddScreen(
            state = previewState(ExistingAppointmentAddPage.REQUEST_PARTICIPANTS).copy(
                selectedMemberIds = setOf("member-1", "member-2"),
                canRequest = true
            ),
            onBackClick = {}, onCalendarClick = {}, onPlaceClick = {}, onMoreClick = {},
            onChatTabClick = {}, onMapAndStatusTabClick = {}, onMemberSelectionChange = { _, _ -> },
            onRescheduleClick = {}, onRequestClick = {}
        )
    }
}

private fun previewState(page: ExistingAppointmentAddPage) = ExistingAppointmentAddUiState(
    page = page,
    appointmentTitle = "중간고사 카공 모임",
    memberSummary = "3명 · 태양, 하은, 나",
    scheduledTime = "오후 3:05",
    scheduledPlace = "강남역 가까운 곳",
    liveStatusLabel = "실시간",
    members = listOf(
        ExistingAppointmentMemberUiState("member-1", "나", "도착", "장소에 도착했어요"),
        ExistingAppointmentMemberUiState("member-2", "태양", "도착", "장소에 도착했어요", "2분 지각"),
        ExistingAppointmentMemberUiState("member-3", "하은", "도착", "장소에 도착했어요", "5분 지각")
    )
)
