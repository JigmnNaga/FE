package com.jigmnnaga.presentation.mypage.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jigmnnaga.R
import com.jigmnnaga.core.designsystem.component.BackHeader
import com.jigmnnaga.core.designsystem.theme.AppColors
import com.jigmnnaga.core.designsystem.theme.AppDimens
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme

/** 약속방 상태. 방장 역할에 따라 뱃지와 설명이 달라진다. */
enum class AppointmentRoomStatus {
    /** 3인 이상 → 방장 자동 위임 */
    LEADER_TRANSFER,
    /** 1~2인 → 탈퇴 시 자동 삭제 */
    LEADER_DELETE,
    /** 방장이 아닌 참여자 → 나가기 처리 */
    MEMBER
}

data class AppointmentRoomInfo(
    val name: String,
    val memberCount: Int,
    val isLeader: Boolean,
    val status: AppointmentRoomStatus
)

private fun TextStyle.sized(size: Int, line: Int, weight: FontWeight): TextStyle =
    copy(fontSize = size.sp, lineHeight = line.sp, fontWeight = weight)

/**
 * MYP-002-02 방장 회원탈퇴 안내 화면.
 * 방장으로 있는 약속방 목록과 처리 방식을 안내한다.
 * [showWithdrawalDialog]가 true이면 최종 확인 팝업을 표시한다.
 */
@Composable
fun WithdrawalGuideScreen(
    rooms: List<AppointmentRoomInfo>,
    activeAppointmentCount: Int,
    showWithdrawalDialog: Boolean,
    onBackClick: () -> Unit,
    onWithdrawClick: () -> Unit,
    onWithdrawalDialogDismiss: () -> Unit,
    onWithdrawalDialogConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.MainGreen.Green200)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            BackHeader(
                title = stringResource(R.string.withdrawal_guide_header),
                onBackClick = onBackClick
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = AppDimens.Spacing20)
                    .padding(top = AppDimens.Spacing20, bottom = AppDimens.Spacing16),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.withdrawal_guide_title, rooms.count { it.isLeader }),
                        style = MaterialTheme.typography.titleLarge.sized(22, 33, FontWeight.Bold),
                        color = AppColors.Monotone.SoftBlack
                    )
                    Text(
                        text = stringResource(R.string.withdrawal_guide_description),
                        style = MaterialTheme.typography.bodyMedium.sized(14, 21, FontWeight.Normal),
                        color = AppColors.Monotone.Gray600
                    )
                }
                rooms.forEach { room ->
                    AppointmentRoomCard(room = room)
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.MainGreen.Green200)
                    .padding(horizontal = AppDimens.Spacing20)
                    .padding(bottom = AppDimens.Spacing16)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.MainGreen.Green900, RoundedCornerShape(12.dp))
                        .clickable(onClick = onWithdrawClick)
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.withdrawal_guide_action),
                        style = TextStyle(
                            fontFamily = MaterialTheme.typography.titleMedium.fontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            lineHeight = 24.sp
                        ),
                        color = AppColors.Monotone.White
                    )
                }
            }
        }

        if (showWithdrawalDialog) {
            WithdrawalConfirmDialog(
                activeAppointmentCount = activeAppointmentCount,
                onDismiss = onWithdrawalDialogDismiss,
                onConfirm = onWithdrawalDialogConfirm
            )
        }
    }
}

@Composable
private fun AppointmentRoomCard(
    room: AppointmentRoomInfo,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.Monotone.White, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = room.name,
                style = MaterialTheme.typography.titleSmall.sized(15, 23, FontWeight.Bold),
                color = AppColors.Monotone.SoftBlack,
                modifier = Modifier.weight(1f)
            )
            when (room.status) {
                AppointmentRoomStatus.LEADER_TRANSFER -> RoomBadge(
                    text = stringResource(R.string.withdrawal_guide_badge_transfer),
                    bgColor = AppColors.Danger.Red200,
                    textColor = AppColors.Danger.Red600
                )
                AppointmentRoomStatus.LEADER_DELETE -> RoomBadge(
                    text = stringResource(R.string.withdrawal_guide_badge_delete),
                    bgColor = AppColors.Monotone.Gray100,
                    textColor = AppColors.MainGreen.Green700
                )
                AppointmentRoomStatus.MEMBER -> Unit
            }
        }
        Text(
            text = if (room.isLeader) {
                stringResource(R.string.withdrawal_guide_member_leader, room.memberCount)
            } else {
                stringResource(R.string.withdrawal_guide_member_participant, room.memberCount)
            },
            style = MaterialTheme.typography.labelSmall.sized(12, 18, FontWeight.Normal),
            color = AppColors.Monotone.Gray600
        )
        Text(
            text = when (room.status) {
                AppointmentRoomStatus.LEADER_TRANSFER -> stringResource(R.string.withdrawal_guide_transfer_info)
                AppointmentRoomStatus.LEADER_DELETE -> stringResource(R.string.withdrawal_guide_delete_info)
                AppointmentRoomStatus.MEMBER -> stringResource(R.string.withdrawal_guide_member_info)
            },
            style = MaterialTheme.typography.bodyMedium.sized(13, 20, FontWeight.Normal),
            color = AppColors.Monotone.Gray600
        )
    }
}

@Composable
private fun RoomBadge(
    text: String,
    bgColor: androidx.compose.ui.graphics.Color,
    textColor: androidx.compose.ui.graphics.Color
) {
    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.sized(11, 17, FontWeight.Bold),
            color = textColor
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892, name = "방장 회원탈퇴 안내")
@Composable
private fun WithdrawalGuidePreview() {
    JigmnnagaTheme {
        WithdrawalGuideScreen(
            rooms = listOf(
                AppointmentRoomInfo("중간고사 카공 모임", 3, true, AppointmentRoomStatus.LEADER_TRANSFER),
                AppointmentRoomInfo("팀플 점심 약속", 1, true, AppointmentRoomStatus.LEADER_DELETE),
                AppointmentRoomInfo("주말 영화", 2, false, AppointmentRoomStatus.MEMBER)
            ),
            activeAppointmentCount = 1,
            showWithdrawalDialog = false,
            onBackClick = {},
            onWithdrawClick = {},
            onWithdrawalDialogDismiss = {},
            onWithdrawalDialogConfirm = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892, name = "방장 회원탈퇴 - 팝업")
@Composable
private fun WithdrawalGuideWithDialogPreview() {
    JigmnnagaTheme {
        WithdrawalGuideScreen(
            rooms = listOf(
                AppointmentRoomInfo("중간고사 카공 모임", 3, true, AppointmentRoomStatus.LEADER_TRANSFER),
                AppointmentRoomInfo("팀플 점심 약속", 1, true, AppointmentRoomStatus.LEADER_DELETE),
                AppointmentRoomInfo("주말 영화", 2, false, AppointmentRoomStatus.MEMBER)
            ),
            activeAppointmentCount = 1,
            showWithdrawalDialog = true,
            onBackClick = {},
            onWithdrawClick = {},
            onWithdrawalDialogDismiss = {},
            onWithdrawalDialogConfirm = {}
        )
    }
}
