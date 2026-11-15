package com.jigmnnaga.presentation.appointment

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.jigmnnaga.core.designsystem.component.AppButton
import com.jigmnnaga.core.designsystem.component.ButtonVariant
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jigmnnaga.R
import com.jigmnnaga.core.designsystem.theme.AppColors
import com.jigmnnaga.core.designsystem.theme.AppDimens

/**
 * 약속방(방장·참여자) 공통 탭 줄. Figma처럼 채팅 / 지도·현황 두 탭을 가운데에 놓고,
 * 선택된 탭 아래에 밑줄을 그린다. 아이콘은 등록된 drawable(chat, place)을 쓴다.
 */
@Composable
fun RoomTabRow(
    chatLabel: String,
    mapLabel: String,
    isMapSelected: Boolean,
    onChatClick: () -> Unit,
    onMapClick: () -> Unit,
    chatTestTag: String,
    mapTestTag: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().height(48.dp),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing24, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RoomTabItem(chatLabel, R.drawable.chat, !isMapSelected, onChatClick, chatTestTag)
        RoomTabItem(mapLabel, R.drawable.place, isMapSelected, onMapClick, mapTestTag)
    }
}

@Composable
private fun RoomTabItem(label: String, icon: Int, selected: Boolean, onClick: () -> Unit, tag: String) {
    val tint = if (selected) AppColors.MainGreen.Green900 else AppColors.Monotone.Gray500
    Row(
        modifier = Modifier
            .height(48.dp)
            .clip(RoundedCornerShape(AppDimens.Spacing8))
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics(mergeDescendants = true) { this.selected = selected }
            .testTag(tag)
            .drawBehind {
                if (selected) {
                    val strokeWidth = 2.dp.toPx()
                    drawLine(
                        color = AppColors.MainGreen.Green900,
                        start = Offset(0f, size.height - strokeWidth / 2),
                        end = Offset(size.width, size.height - strokeWidth / 2),
                        strokeWidth = strokeWidth
                    )
                }
            }
            .padding(horizontal = AppDimens.Spacing8),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing4),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            colorFilter = ColorFilter.tint(tint)
        )
        Text(text = label, color = tint, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

/**
 * 약속 정보 카드 안의 날짜·장소 줄. 확정 전에는 "날짜 후보 / 약속 장소 후보"에 미정 배지를,
 * 확정 뒤에는 확정된 값을 보여 준다.
 */
@Composable
fun RoomSummaryDetails(
    dateLabel: String,
    dateValue: String,
    placeLabel: String,
    placeValue: String,
    showUndecidedBadge: Boolean,
    modifier: Modifier = Modifier,
    showPlaceUndecidedBadge: Boolean = showUndecidedBadge
) {
    Column(modifier.fillMaxWidth()) {
        RoomSummaryDetailRow(R.drawable.calendar_gray, dateLabel, dateValue, showUndecidedBadge, tintIcon = false)
        HorizontalDivider(
            color = AppColors.Monotone.Gray100,
            modifier = Modifier.padding(vertical = AppDimens.Spacing12)
        )
        RoomSummaryDetailRow(R.drawable.place, placeLabel, placeValue, showPlaceUndecidedBadge, tintIcon = true)
    }
}

@Composable
private fun RoomSummaryDetailRow(icon: Int, label: String, value: String, showBadge: Boolean, tintIcon: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing12),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            colorFilter = if (tintIcon) ColorFilter.tint(AppColors.Monotone.Gray500) else null
        )
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = AppColors.Monotone.Gray600)
            Text(
                value,
                style = MaterialTheme.typography.titleSmall,
                color = AppColors.Monotone.SoftBlack,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (showBadge) {
            Text(
                text = stringResource(R.string.room_summary_undecided),
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.Monotone.Gray600,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(AppColors.Monotone.Gray100)
                    .padding(horizontal = AppDimens.Spacing8, vertical = AppDimens.Spacing4)
            )
        }
    }
}


/** 확정 전 지도·현황 탭에 보여 줄 멤버 한 줄. */
data class RoomMemberStatusUi(
    val name: String,
    val isCurrentUser: Boolean,
    val hasResponded: Boolean
)

/**
 * 약속 확정 전 지도·현황 탭(Figma APT-005-01). 비활성 지도, 약속 준비 현황, 멤버 현황, 비활성 "지금 나가기" 버튼으로 이뤄진다.
 * 실제 지도는 확정 뒤에 연결하므로 지도 자리는 안내 문구가 있는 자리 표시로 둔다.
 */
@Composable
fun RoomPendingMapStatus(
    members: List<RoomMemberStatusUi>,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppDimens.Spacing20, vertical = AppDimens.Spacing8),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFFA3A8A0)),
                contentAlignment = Alignment.BottomCenter
            ) {
                Text(
                    text = stringResource(R.string.room_pending_map_notice),
                    modifier = Modifier.padding(bottom = AppDimens.Spacing24),
                    color = AppColors.Monotone.Gray100,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppDimens.Spacing16),
                color = AppColors.Monotone.White
            ) {
                Column(Modifier.padding(horizontal = AppDimens.Spacing16, vertical = AppDimens.Spacing12)) {
                    Text(
                        text = stringResource(R.string.room_pending_status_label),
                        fontSize = 10.sp,
                        color = AppColors.MainGreen.Green400
                    )
                    Text(
                        text = stringResource(R.string.room_pending_status_title),
                        modifier = Modifier.padding(top = 2.dp),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = AppColors.MainGreen.Green700
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = AppDimens.Spacing4),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.room_pending_members_title),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    color = AppColors.Monotone.SoftBlack
                )
                Text(
                    text = stringResource(R.string.participant_room_member_count, members.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.Monotone.Gray600,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(AppColors.Monotone.Gray100)
                        .padding(horizontal = 14.dp, vertical = AppDimens.Spacing4)
                )
            }
            members.forEach { RoomMemberStatusCard(it) }
        }
        AppButton(
            text = stringResource(R.string.participant_room_departure_action),
            variant = ButtonVariant.DEFAULT,
            enabled = false,
            onClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimens.Spacing20)
                .padding(bottom = 15.dp)
        )
    }
}

@Composable
private fun RoomMemberStatusCard(member: RoomMemberStatusUi) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.Spacing16))
            .background(AppColors.Monotone.Gray300.copy(alpha = 0.12f))
            .border(1.dp, AppColors.Monotone.Gray300.copy(alpha = 0.4f), RoundedCornerShape(AppDimens.Spacing16))
            .padding(horizontal = AppDimens.Spacing16, vertical = AppDimens.Spacing12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (member.isCurrentUser) AppColors.MainGreen.Green700 else AppColors.Monotone.Gray300),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = member.name.take(1),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (member.isCurrentUser) AppColors.MainGreen.Green200 else AppColors.Monotone.Gray700
            )
        }
        Column(Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
            ) {
                Text(
                    text = member.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = AppColors.Monotone.SoftBlack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(
                        if (member.hasResponded) R.string.room_member_badge_responded else R.string.room_member_badge_pending
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (member.hasResponded) AppColors.MainGreen.Green700 else AppColors.Monotone.Gray300,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (member.hasResponded) {
                                AppColors.MainGreen.Green400.copy(alpha = 0.2f)
                            } else {
                                AppColors.Monotone.Gray300.copy(alpha = 0.2f)
                            }
                        )
                        .padding(horizontal = AppDimens.Spacing8, vertical = 2.dp)
                )
            }
            Text(
                text = stringResource(
                    if (member.hasResponded) R.string.room_member_sub_responded else R.string.room_member_sub_pending
                ),
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.Monotone.Gray500
            )
        }
    }
}
