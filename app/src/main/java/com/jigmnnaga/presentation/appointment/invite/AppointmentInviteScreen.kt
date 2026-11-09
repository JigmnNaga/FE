package com.jigmnnaga.presentation.appointment.invite

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.jigmnnaga.R
import com.jigmnnaga.core.designsystem.component.AppButton
import com.jigmnnaga.core.designsystem.component.BackHeader
import com.jigmnnaga.core.designsystem.theme.AppColors
import com.jigmnnaga.core.designsystem.theme.AppDimens
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme

/**
 * APT-002 화면. 내비게이션과 서버 응답은 소유하지 않으며, 결과 화면은 상위에서 전달합니다.
 * 상태 바와 시스템 내비게이션 영역은 이 독립 화면이 안전 영역으로 처리합니다.
 */
@Composable
fun AppointmentInviteScreen(
    state: AppointmentInviteUiState,
    onBack: () -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onOpenChat: () -> Unit,
    onReturnHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.MainGreen.Green200)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        if (state.result == AppointmentInviteResult.PENDING) {
            BackHeader(title = state.appointmentName, onBackClick = onBack)
            PendingInviteContent(
                state = state,
                onAccept = onAccept,
                onDecline = onDecline,
                modifier = Modifier.weight(1f)
            )
        } else {
            ResultInviteContent(
                result = state.result,
                onOpenChat = onOpenChat,
                onReturnHome = onReturnHome,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PendingInviteContent(
    state: AppointmentInviteUiState,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppDimens.Spacing20),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(AppDimens.Spacing16))
            Text(
                text = stringResource(
                    R.string.invite_headline,
                    state.inviterName,
                    state.inviterName.subjectParticle()
                ),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 24.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = AppColors.MainGreen.Green800,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(Modifier.height(AppDimens.Spacing12))
            Text(
                text = stringResource(R.string.invite_participant_count, state.participatingMemberCount),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = AppColors.MainGreen.Green800
            )
            Spacer(Modifier.height(AppDimens.Spacing40))
            Column(
                modifier = Modifier
                    .widthIn(max = 342.dp)
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = stringResource(R.string.invite_members_heading, state.participatingMemberCount),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = AppColors.Monotone.SoftBlack,
                    modifier = Modifier.semantics { heading() }
                )
                Spacer(Modifier.height(AppDimens.Spacing8))
                state.members.forEachIndexed { index, member ->
                    InviteMemberRow(
                        member = member,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("invite-member-$index")
                    )
                    if (index != state.members.lastIndex) Spacer(Modifier.height(AppDimens.Spacing8))
                }
            }
            Spacer(
                Modifier.height(
                    AppDimens.ButtonHeight + 48.dp + AppDimens.Spacing8 * 2 + AppDimens.Spacing16
                )
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(AppColors.MainGreen.Green200)
                .padding(horizontal = AppDimens.Spacing20),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TextButton(
                onClick = onDecline,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("invite-decline")
            ) {
                Text(
                    text = stringResource(R.string.invite_decline),
                    style = MaterialTheme.typography.labelLarge,
                    color = AppColors.Monotone.Gray700
                )
            }
            Spacer(Modifier.height(AppDimens.Spacing8))
            AppButton(
                text = stringResource(R.string.invite_accept),
                modifier = Modifier.fillMaxWidth().testTag("invite-accept"),
                onClick = onAccept
            )
            Spacer(Modifier.height(AppDimens.Spacing16))
        }
    }
}

@Composable
private fun InviteMemberRow(member: AppointmentInviteMember, modifier: Modifier = Modifier) {
    val avatarColor = when (member.status) {
        AppointmentInviteMemberStatus.HOST -> AppColors.MainGreen.Green400
        AppointmentInviteMemberStatus.ACCEPTED -> AppColors.MainGreen.Green300
        AppointmentInviteMemberStatus.DECLINED -> AppColors.Monotone.Gray200
        AppointmentInviteMemberStatus.WAITING -> AppColors.MainGreen.Green900
    }
    val badgeBackground = when (member.status) {
        AppointmentInviteMemberStatus.HOST -> AppColors.Monotone.Gray100
        AppointmentInviteMemberStatus.ACCEPTED -> AppColors.MainGreen.Green200
        AppointmentInviteMemberStatus.DECLINED -> AppColors.Danger.Red200
        AppointmentInviteMemberStatus.WAITING -> AppColors.Monotone.Gray100
    }
    val badgeForeground = when (member.status) {
        AppointmentInviteMemberStatus.HOST -> AppColors.MainGreen.Green700
        AppointmentInviteMemberStatus.ACCEPTED -> AppColors.MainGreen.Green700
        AppointmentInviteMemberStatus.DECLINED -> AppColors.Danger.Red600
        AppointmentInviteMemberStatus.WAITING -> AppColors.Monotone.Gray700
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.Monotone.White)
            .padding(horizontal = AppDimens.Spacing16, vertical = AppDimens.Spacing12),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(avatarColor)
                .clearAndSetSemantics { },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = member.name.firstOrNull()?.toString().orEmpty(),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = if (member.status == AppointmentInviteMemberStatus.WAITING) {
                    AppColors.MainGreen.Green200
                } else {
                    AppColors.Monotone.White
                }
            )
        }
        Text(
            text = member.name,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = AppColors.Monotone.SoftBlack,
            modifier = Modifier.weight(1f).padding(start = AppDimens.Spacing12)
        )
        Text(
            text = stringResource(member.status.labelRes),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = badgeForeground,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(badgeBackground)
                .padding(horizontal = AppDimens.Spacing8, vertical = AppDimens.Spacing4)
        )
    }
}

private val AppointmentInviteMemberStatus.labelRes: Int
    get() = when (this) {
        AppointmentInviteMemberStatus.HOST -> R.string.invite_member_host
        AppointmentInviteMemberStatus.ACCEPTED -> R.string.invite_member_accepted
        AppointmentInviteMemberStatus.DECLINED -> R.string.invite_member_declined
        AppointmentInviteMemberStatus.WAITING -> R.string.invite_member_waiting
    }

private fun String.subjectParticle(): String {
    val finalChar = lastOrNull() ?: return "이"
    val hangulOffset = finalChar.code - 0xAC00
    return if (hangulOffset in 0..11171 && hangulOffset % 28 == 0) "가" else "이"
}

@Composable
private fun ResultInviteContent(
    result: AppointmentInviteResult,
    onOpenChat: () -> Unit,
    onReturnHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accepted = result == AppointmentInviteResult.ACCEPTED
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.Spacing20),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ResultMark(accepted = accepted)
            Spacer(Modifier.height(AppDimens.Spacing24))
            Text(
                text = stringResource(if (accepted) R.string.invite_accepted_title else R.string.invite_declined_title),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp, lineHeight = 28.sp),
                color = AppColors.Monotone.SoftBlack,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(Modifier.height(AppDimens.Spacing8))
            Text(
                text = stringResource(if (accepted) R.string.invite_accepted_message else R.string.invite_declined_message),
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.Monotone.Gray500
            )
        }
        AppButton(
            text = stringResource(if (accepted) R.string.invite_open_chat else R.string.invite_return_home),
            modifier = Modifier.fillMaxWidth().testTag("invite-result-action"),
            onClick = if (accepted) onOpenChat else onReturnHome
        )
        Spacer(Modifier.height(AppDimens.Spacing16))
    }
}

@Composable
private fun ResultMark(accepted: Boolean) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(if (accepted) AppColors.MainGreen.Green300 else AppColors.Danger.Red200)
            .testTag(if (accepted) "invite-accepted-mark" else "invite-declined-mark"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(30.dp)) {
            val path = Path()
            if (accepted) {
                path.moveTo(size.width * 0.16f, size.height * 0.52f)
                path.lineTo(size.width * 0.41f, size.height * 0.76f)
                path.lineTo(size.width * 0.84f, size.height * 0.25f)
            } else {
                path.moveTo(size.width * 0.24f, size.height * 0.24f)
                path.lineTo(size.width * 0.76f, size.height * 0.76f)
                path.moveTo(size.width * 0.76f, size.height * 0.24f)
                path.lineTo(size.width * 0.24f, size.height * 0.76f)
            }
            drawPath(
                path = path,
                color = if (accepted) AppColors.MainGreen.Green900 else AppColors.Danger.Red600,
                style = Stroke(width = 2.5.dp.toPx())
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun AppointmentInvitePendingPreview() {
    JigmnnagaTheme {
        AppointmentInviteScreen(previewInviteState(), {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun AppointmentInviteAcceptedPreview() {
    JigmnnagaTheme {
        AppointmentInviteScreen(
            previewInviteState().copy(result = AppointmentInviteResult.ACCEPTED), {}, {}, {}, {}, {}
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun AppointmentInviteDeclinedPreview() {
    JigmnnagaTheme {
        AppointmentInviteScreen(
            previewInviteState().copy(result = AppointmentInviteResult.DECLINED), {}, {}, {}, {}, {}
        )
    }
}

private fun previewInviteState() = AppointmentInviteUiState(
    appointmentName = "중간고사 카공 모임",
    inviterName = "태양",
    members = listOf(
        AppointmentInviteMember("태양", AppointmentInviteMemberStatus.HOST),
        AppointmentInviteMember("하은", AppointmentInviteMemberStatus.ACCEPTED),
        AppointmentInviteMember("채림", AppointmentInviteMemberStatus.DECLINED),
        AppointmentInviteMember("나", AppointmentInviteMemberStatus.WAITING)
    )
)
