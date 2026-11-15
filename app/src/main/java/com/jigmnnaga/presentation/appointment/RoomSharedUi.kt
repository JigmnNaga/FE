package com.jigmnnaga.presentation.appointment

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth()) {
        RoomSummaryDetailRow(R.drawable.calendar_gray, dateLabel, dateValue, showUndecidedBadge, tintIcon = false)
        HorizontalDivider(
            color = AppColors.Monotone.Gray100,
            modifier = Modifier.padding(vertical = AppDimens.Spacing12)
        )
        RoomSummaryDetailRow(R.drawable.place, placeLabel, placeValue, showUndecidedBadge, tintIcon = true)
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
