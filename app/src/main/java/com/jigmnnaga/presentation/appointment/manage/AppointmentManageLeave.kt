package com.jigmnnaga.presentation.appointment.manage

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.jigmnnaga.R
import com.jigmnnaga.core.designsystem.component.AppButton
import com.jigmnnaga.core.designsystem.component.ButtonVariant
import com.jigmnnaga.core.designsystem.theme.AppColors
import com.jigmnnaga.core.designsystem.theme.AppDimens
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme
import com.jigmnnaga.presentation.appointment.create.AppointmentCreateUiState

/** 약속 관리 화면 상단 제목. 방 인원으로 정해지는 흐름마다 다르다. */
@Composable
internal fun leaveHeaderTitle(mode: AppointmentManageLeaveMode): String = when (mode) {
    AppointmentManageLeaveMode.DELEGATE -> stringResource(R.string.appointment_leave_title_delegate)
    AppointmentManageLeaveMode.AUTO_DELEGATE -> stringResource(R.string.appointment_leave_title_auto)
    AppointmentManageLeaveMode.DELETE_ONLY -> stringResource(R.string.appointment_leave_title_delete)
}

/** 나가기 화면 아래 버튼 문구. 3명 이상이면 고른 멤버 이름을 넣는다. */
@Composable
internal fun leaveButtonText(state: AppointmentManageUiState): String = when (state.leaveMode) {
    AppointmentManageLeaveMode.DELEGATE -> state.newHost?.let {
        stringResource(R.string.appointment_leave_button_delegate_named, it.nickname)
    } ?: stringResource(R.string.appointment_leave_button_delegate)
    AppointmentManageLeaveMode.AUTO_DELEGATE -> stringResource(R.string.appointment_leave_button_auto)
    AppointmentManageLeaveMode.DELETE_ONLY -> stringResource(R.string.appointment_leave_button_delete)
}

/** 관리 목록의 "약속방 나가기" 줄 설명. */
@Composable
internal fun leaveMenuDescription(mode: AppointmentManageLeaveMode): String = when (mode) {
    AppointmentManageLeaveMode.DELEGATE -> stringResource(R.string.appointment_manage_leave_desc_delegate)
    AppointmentManageLeaveMode.AUTO_DELEGATE -> stringResource(R.string.appointment_manage_leave_desc_auto)
    AppointmentManageLeaveMode.DELETE_ONLY -> stringResource(R.string.appointment_manage_leave_desc_delete)
}

/** 방장 나가기 화면 본문. 3명 이상은 새 방장을 고르고, 2명·1명은 영향 안내를 보여 준다. */
@Composable
internal fun LeaveContent(state: AppointmentManageUiState, onSelectNewHost: (Long) -> Unit) {
    val mode = state.leaveMode
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = AppDimens.Spacing4),
        verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing20)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)) {
            Text(
                text = if (mode == AppointmentManageLeaveMode.DELEGATE) {
                    stringResource(R.string.appointment_leave_caption_delegate)
                } else {
                    stringResource(R.string.appointment_leave_caption_count, state.members.size)
                },
                fontSize = 12.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.MainGreen.Green700
            )
            Text(
                text = when (mode) {
                    AppointmentManageLeaveMode.DELEGATE -> stringResource(R.string.appointment_leave_headline_delegate)
                    AppointmentManageLeaveMode.AUTO_DELEGATE -> stringResource(R.string.appointment_leave_headline_auto)
                    AppointmentManageLeaveMode.DELETE_ONLY -> stringResource(R.string.appointment_leave_headline_delete)
                },
                fontSize = 22.sp,
                lineHeight = 33.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.Monotone.SoftBlack
            )
            Text(
                text = when (mode) {
                    AppointmentManageLeaveMode.DELEGATE -> stringResource(R.string.appointment_leave_desc_delegate)
                    AppointmentManageLeaveMode.AUTO_DELEGATE -> stringResource(R.string.appointment_leave_desc_auto)
                    AppointmentManageLeaveMode.DELETE_ONLY -> stringResource(R.string.appointment_leave_desc_delete)
                },
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = AppColors.Monotone.Gray600
            )
        }

        if (mode == AppointmentManageLeaveMode.DELEGATE) {
            DelegateBody(state = state, onSelectNewHost = onSelectNewHost)
        } else {
            AutoBody(state = state)
        }
    }
}

@Composable
private fun LeaveCard(spacing: androidx.compose.ui.unit.Dp, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = AppColors.Monotone.White
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.Spacing16),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) { content() }
    }
}

@Composable
private fun DelegateBody(state: AppointmentManageUiState, onSelectNewHost: (Long) -> Unit) {
    LeaveCard(spacing = AppDimens.Spacing4) {
        Text(
            text = state.saved.appointmentName,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.Monotone.SoftBlack
        )
        Text(
            text = stringResource(R.string.appointment_leave_summary_count, state.members.size),
            fontSize = 12.sp,
            lineHeight = 18.sp,
            color = AppColors.Monotone.Gray600
        )
    }
    LeaveCard(spacing = AppDimens.Spacing12) {
        state.leaveCandidates.forEachIndexed { index, member ->
            if (index > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(AppColors.Monotone.Gray100))
            val selected = member.memberId == state.selectedNewHostId
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selected,
                        enabled = !state.isWorking,
                        role = Role.RadioButton,
                        onClick = { onSelectNewHost(member.memberId) }
                    )
                    .testTag("appointment-manage-leave-target-${member.memberId}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(AppColors.MainGreen.Green400),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = member.nickname.take(1),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.MainGreen.Green900
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = member.nickname,
                        fontSize = 15.sp,
                        lineHeight = 23.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Monotone.SoftBlack
                    )
                    Text(
                        text = stringResource(R.string.appointment_leave_member_role),
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = AppColors.Monotone.Gray600
                    )
                }
                SelectionMark(selected = selected)
            }
        }
    }
    InfoBox(
        background = AppColors.Monotone.Gray100,
        title = stringResource(R.string.appointment_leave_keep_title),
        titleColor = AppColors.MainGreen.Green700,
        message = stringResource(R.string.appointment_leave_keep_desc),
        messageColor = AppColors.Monotone.Gray600,
        spacing = AppDimens.Spacing4
    )
}

@Composable
private fun SelectionMark(selected: Boolean) {
    if (selected) {
        // Figma "선택됨": Green700 원 안에 흰 점
        Box(
            modifier = Modifier.size(22.dp).clip(CircleShape).background(AppColors.MainGreen.Green700),
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(AppColors.Monotone.White))
        }
    } else {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(AppColors.Monotone.White)
                .border(1.5.dp, AppColors.Monotone.Gray300, CircleShape)
        )
    }
}

@Composable
private fun AutoBody(state: AppointmentManageUiState) {
    val mode = state.leaveMode
    LeaveCard(spacing = AppDimens.Spacing12) {
        Text(
            text = state.saved.appointmentName,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.Monotone.SoftBlack
        )
        Text(
            text = stringResource(R.string.appointment_leave_participants, state.members.size),
            fontSize = 12.sp,
            lineHeight = 18.sp,
            color = AppColors.Monotone.Gray600
        )
        ParticipantLine(
            initial = stringResource(R.string.appointment_leave_me_initial),
            name = stringResource(R.string.appointment_leave_me_host)
        )
        state.leaveCandidates.forEach { member ->
            ParticipantLine(initial = member.nickname.take(1), name = member.nickname)
        }
    }
    val impactTitle = if (mode == AppointmentManageLeaveMode.AUTO_DELEGATE) {
        stringResource(R.string.appointment_leave_auto_impact_title, state.newHost?.nickname.orEmpty())
    } else {
        stringResource(R.string.appointment_leave_delete_impact_title)
    }
    val impactMessage = if (mode == AppointmentManageLeaveMode.AUTO_DELEGATE) {
        stringResource(R.string.appointment_leave_auto_impact_desc)
    } else {
        stringResource(R.string.appointment_leave_delete_impact_desc)
    }
    InfoBox(
        background = AppColors.Danger.Red200,
        title = impactTitle,
        titleColor = AppColors.Danger.Red600,
        message = impactMessage,
        messageColor = AppColors.Danger.Red600,
        spacing = 6.dp
    )
}

@Composable
private fun ParticipantLine(initial: String, name: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape).background(AppColors.MainGreen.Green900),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.Monotone.White
            )
        }
        Text(
            text = name,
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
            lineHeight = 21.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.Monotone.SoftBlack
        )
    }
}

@Composable
private fun InfoBox(
    background: Color,
    title: String,
    titleColor: Color,
    message: String,
    messageColor: Color,
    spacing: androidx.compose.ui.unit.Dp
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Bold,
            color = titleColor
        )
        Text(text = message, fontSize = 13.sp, lineHeight = 20.sp, color = messageColor)
    }
}

/** 나가기·삭제를 마친 뒤 보여 주는 완료 화면. 홈으로 이동 버튼만 있다. */
@Composable
internal fun LeaveCompletionScreen(
    completion: AppointmentManageCompletion,
    onGoHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.MainGreen.Green200)
            .safeDrawingPadding()
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier.offset(y = (-50).dp).testTag("appointment-manage-leave-done"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(AppColors.MainGreen.Green700.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.check),
                        contentDescription = null,
                        tint = AppColors.MainGreen.Green700,
                        modifier = Modifier.size(38.dp)
                    )
                }
                Text(
                    text = if (completion == AppointmentManageCompletion.LEFT) {
                        stringResource(R.string.appointment_leave_done_left_title)
                    } else {
                        stringResource(R.string.appointment_leave_done_deleted_title)
                    },
                    modifier = Modifier.padding(top = 17.dp),
                    fontSize = 20.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = AppColors.Monotone.SoftBlack
                )
                if (completion == AppointmentManageCompletion.LEFT) {
                    Text(
                        text = stringResource(R.string.appointment_leave_done_left_desc),
                        modifier = Modifier.padding(top = AppDimens.Spacing8),
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        textAlign = TextAlign.Center,
                        color = AppColors.Monotone.Gray600
                    )
                }
            }
        }
        AppButton(
            text = stringResource(R.string.appointment_leave_go_home),
            variant = ButtonVariant.DEFAULT,
            onClick = onGoHome,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = AppDimens.Spacing20,
                    end = AppDimens.Spacing20,
                    bottom = AppDimens.Spacing16
                )
                .testTag("appointment-manage-leave-home")
        )
    }
}

/** 나가기·삭제를 한 번 더 확인받는 팝업. */
@Composable
internal fun LeaveConfirmDialog(
    state: AppointmentManageUiState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val name = state.newHost?.nickname.orEmpty()
    val (title, message, confirmText) = when (state.leaveMode) {
        AppointmentManageLeaveMode.DELEGATE -> Triple(
            stringResource(R.string.appointment_leave_dialog_delegate_title, name),
            stringResource(R.string.appointment_leave_dialog_delegate_message, name, state.members.size - 1),
            stringResource(R.string.appointment_leave_dialog_confirm_delegate)
        )
        AppointmentManageLeaveMode.AUTO_DELEGATE -> Triple(
            stringResource(R.string.appointment_leave_dialog_auto_title, name),
            stringResource(R.string.appointment_leave_dialog_auto_message, name),
            stringResource(R.string.appointment_leave_dialog_confirm_delegate)
        )
        AppointmentManageLeaveMode.DELETE_ONLY -> Triple(
            stringResource(R.string.appointment_leave_dialog_delete_title),
            stringResource(R.string.appointment_leave_dialog_delete_message),
            stringResource(R.string.appointment_leave_dialog_confirm_delete)
        )
    }
    ManageConfirmDialog(
        title = title,
        message = message,
        confirmText = confirmText,
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        dialogTag = "appointment-manage-leave-dialog",
        cancelTag = "appointment-manage-leave-dialog-cancel",
        confirmTag = "appointment-manage-leave-dialog-confirm"
    )
}

/** 약속방 관리의 확인 팝업. Figma(APT-004-06)처럼 경고 아이콘, 취소/확인 버튼 두 개로 그린다. */
@Composable
internal fun ManageConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    dialogTag: String,
    cancelTag: String,
    confirmTag: String,
    cancelText: String = stringResource(R.string.appointment_manage_dialog_cancel)
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { window?.setDimAmount(0.44f) }
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 34.dp).testTag(dialogTag),
            shape = RoundedCornerShape(20.dp),
            color = AppColors.Monotone.White
        ) {
            Column(
                modifier = Modifier.padding(AppDimens.Spacing24),
                verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing16)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.warning),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = title,
                        modifier = Modifier.weight(1f),
                        fontSize = 18.sp,
                        lineHeight = 27.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Monotone.SoftBlack
                    )
                }
                Text(
                    text = message,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    color = AppColors.Monotone.Gray600
                )
                Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)) {
                    DialogButton(
                        text = cancelText,
                        filled = false,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).testTag(cancelTag)
                    )
                    DialogButton(
                        text = confirmText,
                        filled = true,
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f).testTag(confirmTag)
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogButton(text: String, filled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (filled) AppColors.MainGreen.Green900 else AppColors.Monotone.White,
        border = if (filled) null else BorderStroke(1.dp, AppColors.Monotone.Gray300)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Bold,
                color = if (filled) AppColors.Monotone.White else AppColors.MainGreen.Green700,
                maxLines = 1
            )
        }
    }
}

private fun previewState(
    memberCount: Int,
    selectedNewHostId: Long? = null
) = AppointmentCreateUiState(appointmentName = "중간고사 카공 모임").let { form ->
    AppointmentManageUiState(
        step = AppointmentManageStep.LEAVE,
        saved = form,
        form = form,
        members = listOf(
            ManageMember(1L, "나", true),
            ManageMember(2L, "태희", false),
            ManageMember(3L, "하은", false)
        ).take(memberCount),
        selectedNewHostId = selectedNewHostId
    )
}

@Composable
private fun LeavePreviewScreen(state: AppointmentManageUiState) {
    JigmnnagaTheme {
        AppointmentManageScreen(
            state = state,
            onBack = {},
            onOpenStep = {},
            onFormChange = {},
            onSave = {},
            onRequestConfirm = {},
            onDismissConfirm = {},
            onConfirm = {},
            onSendReminders = {},
            onRequestKick = {},
            onDismissKick = {},
            onConfirmKick = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun LeaveDelegatePreview() = LeavePreviewScreen(previewState(memberCount = 3))

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun LeaveDelegateSelectedPreview() =
    LeavePreviewScreen(previewState(memberCount = 3, selectedNewHostId = 2L))

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun LeaveAutoDelegatePreview() = LeavePreviewScreen(previewState(memberCount = 2))

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun LeaveDeleteOnlyPreview() = LeavePreviewScreen(previewState(memberCount = 1))

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun LeaveCompletionPreview() = JigmnnagaTheme {
    LeaveCompletionScreen(completion = AppointmentManageCompletion.LEFT, onGoHome = {})
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun LeaveDeletedCompletionPreview() = JigmnnagaTheme {
    LeaveCompletionScreen(completion = AppointmentManageCompletion.DELETED, onGoHome = {})
}
