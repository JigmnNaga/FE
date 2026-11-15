package com.jigmnnaga.presentation.appointment.manage

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jigmnnaga.R
import com.jigmnnaga.core.designsystem.component.AppButton
import com.jigmnnaga.core.designsystem.component.BackHeader
import com.jigmnnaga.core.designsystem.component.ButtonVariant
import com.jigmnnaga.core.designsystem.theme.AppColors
import com.jigmnnaga.core.designsystem.theme.AppDimens
import com.jigmnnaga.presentation.appointment.create.AppointmentCreateUiState
import com.jigmnnaga.presentation.appointment.create.AppointmentDeadlineChoices
import com.jigmnnaga.presentation.appointment.create.AppointmentLocationsStep
import com.jigmnnaga.presentation.appointment.create.AppointmentScheduleStep
import com.jigmnnaga.presentation.appointment.create.AppointmentScheduleType
import com.jigmnnaga.presentation.appointment.create.AppointmentTextField

/**
 * 방장 약속방 관리 화면(APT-006). 관리 목록과 항목별 변경 화면을 [AppointmentManageUiState.step]으로 나눠 그린다.
 * 상태와 서버 호출, 화면 이동은 호출하는 쪽이 맡고, 이 화면은 안전 영역과 키보드 여백을 직접 처리한다.
 */
@Composable
fun AppointmentManageScreen(
    state: AppointmentManageUiState,
    onBack: () -> Unit,
    onOpenStep: (AppointmentManageStep) -> Unit,
    onFormChange: (AppointmentCreateUiState) -> Unit,
    onSave: () -> Unit,
    onRequestConfirm: (AppointmentManageConfirm) -> Unit,
    onDismissConfirm: () -> Unit,
    onConfirm: () -> Unit,
    onSendReminders: () -> Unit,
    onRequestKick: (Long) -> Unit,
    onDismissKick: () -> Unit,
    onConfirmKick: () -> Unit,
    modifier: Modifier = Modifier,
    onSelectNewHost: (Long) -> Unit = {},
    onRequestLeave: () -> Unit = {},
    onDismissLeave: () -> Unit = {},
    onConfirmLeave: () -> Unit = {},
    onGoHome: () -> Unit = {},
    onOpenConfirmation: () -> Unit = {},
    onCopyInviteLink: () -> Unit = {},
    onShareInviteLink: () -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    state.completion?.let { completion ->
        LeaveCompletionScreen(completion = completion, onGoHome = onGoHome, modifier = modifier)
        return
    }
    val headerTitle = when (state.step) {
        AppointmentManageStep.MENU -> stringResource(R.string.appointment_manage_title)
        AppointmentManageStep.TITLE -> stringResource(R.string.appointment_manage_title_edit)
        AppointmentManageStep.MEMBERS -> stringResource(R.string.appointment_manage_members_title)
        AppointmentManageStep.LEAVE -> leaveHeaderTitle(state.leaveMode)
        else -> state.saved.appointmentName.ifBlank { stringResource(R.string.appointment_manage_title) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.MainGreen.Green200)
            .safeDrawingPadding()
            .imePadding()
    ) {
        BackHeader(title = headerTitle, onBackClick = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppDimens.Spacing20, vertical = AppDimens.Spacing16),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing16)
        ) {
            when (state.step) {
                AppointmentManageStep.MENU -> ManageMenu(
                    state = state,
                    onOpenStep = onOpenStep,
                    onRequestConfirm = onRequestConfirm,
                    onSendReminders = onSendReminders,
                    onOpenConfirmation = onOpenConfirmation,
                    onCopyInviteLink = onCopyInviteLink,
                    onShareInviteLink = onShareInviteLink
                )

                AppointmentManageStep.MEMBERS -> MemberManager(state = state, onRequestKick = onRequestKick)

                AppointmentManageStep.LEAVE -> LeaveContent(state = state, onSelectNewHost = onSelectNewHost)

                AppointmentManageStep.TITLE -> TitleEditor(form = state.form, onFormChange = onFormChange)

                AppointmentManageStep.SCHEDULE -> AppointmentScheduleStep(
                    state = state.form,
                    onStateChange = onFormChange
                )

                AppointmentManageStep.LOCATIONS -> AppointmentLocationsStep(
                    state = state.form,
                    onStateChange = onFormChange
                )

                AppointmentManageStep.DEADLINE -> AppointmentDeadlineChoices(
                    state = state.form,
                    onStateChange = onFormChange
                )
            }
        }

        if (state.step == AppointmentManageStep.LEAVE) {
            AppButton(
                text = leaveButtonText(state),
                variant = ButtonVariant.DEFAULT,
                enabled = state.canLeave(),
                onClick = onRequestLeave,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = AppDimens.Spacing20,
                        end = AppDimens.Spacing20,
                        bottom = AppDimens.Spacing16
                    )
                    .testTag("appointment-manage-leave-button")
            )
        } else if (state.step != AppointmentManageStep.MENU && state.step != AppointmentManageStep.MEMBERS) {
            AppButton(
                text = stringResource(R.string.appointment_manage_save),
                variant = ButtonVariant.DEFAULT,
                enabled = state.canSave(),
                onClick = {
                    focusManager.clearFocus()
                    onSave()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = AppDimens.Spacing20,
                        end = AppDimens.Spacing20,
                        bottom = AppDimens.Spacing16
                    )
                    .testTag("appointment-manage-save")
            )
        }
    }

    state.kickTarget?.let { target ->
        AlertDialog(
            onDismissRequest = onDismissKick,
            title = { Text(stringResource(R.string.appointment_manage_kick_dialog_title, target.nickname)) },
            text = { Text(stringResource(R.string.appointment_manage_kick_dialog_message, target.nickname)) },
            confirmButton = {
                TextButton(onClick = onConfirmKick, modifier = Modifier.testTag("appointment-manage-kick-confirm")) {
                    Text(
                        stringResource(R.string.appointment_manage_members_kick),
                        color = AppColors.Danger.Red600
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissKick) {
                    Text(stringResource(R.string.appointment_manage_dialog_cancel))
                }
            }
        )
    }

    if (state.isLeaveDialogVisible) {
        LeaveConfirmDialog(state = state, onDismiss = onDismissLeave, onConfirm = onConfirmLeave)
    }

    state.confirm?.let { confirm ->
        val soloRoom = state.members.size <= 1
        val (titleResource, messageResource) = when (confirm) {
            AppointmentManageConfirm.CANCEL_APPOINTMENT ->
                R.string.appointment_manage_cancel_dialog_title to R.string.appointment_manage_cancel_dialog_message
            AppointmentManageConfirm.DELETE_APPOINTMENT ->
                R.string.appointment_manage_delete_dialog_title to if (soloRoom) {
                    R.string.appointment_leave_dialog_delete_message
                } else {
                    R.string.appointment_manage_delete_dialog_message
                }
        }
        ManageConfirmDialog(
            title = stringResource(titleResource),
            message = stringResource(messageResource),
            confirmText = stringResource(
                if (confirm == AppointmentManageConfirm.DELETE_APPOINTMENT) {
                    R.string.appointment_leave_dialog_confirm_delete
                } else {
                    R.string.appointment_confirm
                }
            ),
            onDismiss = onDismissConfirm,
            onConfirm = onConfirm,
            dialogTag = "appointment-manage-confirm-dialog",
            cancelTag = "appointment-manage-confirm-cancel",
            confirmTag = "appointment-manage-confirm"
        )
    }
}

@Composable
private fun ManageMenu(
    state: AppointmentManageUiState,
    onOpenStep: (AppointmentManageStep) -> Unit,
    onRequestConfirm: (AppointmentManageConfirm) -> Unit,
    onSendReminders: () -> Unit,
    onOpenConfirmation: () -> Unit,
    onCopyInviteLink: () -> Unit,
    onShareInviteLink: () -> Unit
) {
    val undecided = stringResource(R.string.appointment_manage_undecided)
    val editable = !state.isCanceled && !state.isWorking

    if (state.isCanceled) {
        Text(
            text = stringResource(R.string.appointment_manage_canceled_banner),
            style = MaterialTheme.typography.labelMedium,
            color = AppColors.Danger.Red600
        )
    }

    SectionTitle(
        text = stringResource(R.string.appointment_manage_section_info),
        badge = stringResource(R.string.appointment_manage_badge_host_only)
    )
    ManageCard {
        ManageRow(
            title = stringResource(R.string.appointment_manage_item_title),
            subtitle = state.saved.appointmentName.ifBlank { undecided },
            enabled = editable,
            tag = "appointment-manage-title",
            icon = R.drawable.edit,
            onClick = { onOpenStep(AppointmentManageStep.TITLE) }
        )
        HorizontalDivider(color = AppColors.Monotone.Gray100)
        ManageRow(
            title = stringResource(R.string.appointment_manage_item_dates),
            subtitle = state.saved.dateSummary(undecided),
            enabled = editable,
            tag = "appointment-manage-dates",
            icon = R.drawable.calendar,
            tintIcon = true,
            onClick = { onOpenStep(AppointmentManageStep.SCHEDULE) }
        )
        HorizontalDivider(color = AppColors.Monotone.Gray100)
        ManageRow(
            title = stringResource(R.string.appointment_manage_item_locations),
            subtitle = state.saved.locationSummary(undecided),
            enabled = editable,
            tag = "appointment-manage-locations",
            icon = R.drawable.place,
            tintIcon = true,
            onClick = { onOpenStep(AppointmentManageStep.LOCATIONS) }
        )
        HorizontalDivider(color = AppColors.Monotone.Gray100)
        ManageRow(
            title = stringResource(R.string.appointment_manage_item_deadline),
            subtitle = deadlineSummary(state.savedDeadline, undecided),
            enabled = editable,
            tag = "appointment-manage-deadline",
            icon = R.drawable.deadline,
            onClick = { onOpenStep(AppointmentManageStep.DEADLINE) }
        )
    }
    Text(
        text = stringResource(R.string.appointment_manage_info_note),
        style = MaterialTheme.typography.labelSmall,
        color = AppColors.Monotone.Gray600
    )

    SectionTitle(
        text = stringResource(R.string.appointment_manage_section_schedule),
        badge = stringResource(R.string.appointment_manage_badge_host_only)
    )
    ManageCard {
        ManageRow(
            title = stringResource(R.string.appointment_manage_item_confirmed_date),
            subtitle = state.confirmedDateTime ?: undecided,
            enabled = editable,
            tag = "appointment-manage-confirm-date",
            icon = R.drawable.calendar,
            tintIcon = true,
            onClick = onOpenConfirmation
        )
        HorizontalDivider(color = AppColors.Monotone.Gray100)
        ManageRow(
            title = stringResource(R.string.appointment_manage_item_confirmed_place),
            subtitle = state.confirmedPlace ?: undecided,
            enabled = editable,
            tag = "appointment-manage-confirm-place",
            icon = R.drawable.place,
            tintIcon = true,
            onClick = onOpenConfirmation
        )
    }
    Text(
        text = stringResource(R.string.appointment_manage_info_note),
        style = MaterialTheme.typography.labelSmall,
        color = AppColors.Monotone.Gray600
    )

    SectionTitle(stringResource(R.string.appointment_manage_section_responses))
    ResponseStatusCard(
        state = state,
        onCopyInviteLink = onCopyInviteLink,
        onShareInviteLink = onShareInviteLink
    )

    ManageCard {
        ManageRow(
            title = stringResource(R.string.appointment_manage_item_reminder),
            subtitle = reminderSubtitle(state),
            enabled = editable,
            tag = "appointment-manage-reminder",
            icon = R.drawable.deadline_notification,
            trailing = if (state.isConfirmed || state.pendingCount == 0) {
                null
            } else {
                stringResource(R.string.appointment_manage_reminder_pending_count, state.pendingCount)
            },
            onClick = onSendReminders
        )
        HorizontalDivider(color = AppColors.Monotone.Gray100)
        ManageRow(
            title = stringResource(R.string.appointment_manage_item_members),
            subtitle = stringResource(R.string.appointment_manage_item_members_desc),
            enabled = !state.isWorking,
            tag = "appointment-manage-members",
            icon = R.drawable.members,
            onClick = { onOpenStep(AppointmentManageStep.MEMBERS) }
        )
    }

    Text(
        text = stringResource(R.string.appointment_manage_section_cleanup),
        style = MaterialTheme.typography.labelMedium,
        color = AppColors.Monotone.Gray600
    )
    ManageCard {
        ManageRow(
            title = stringResource(R.string.appointment_manage_cancel),
            subtitle = stringResource(R.string.appointment_manage_cancel_desc),
            enabled = editable,
            danger = true,
            tag = "appointment-manage-cancel",
            icon = R.drawable.appointment_cancel,
            onClick = { onRequestConfirm(AppointmentManageConfirm.CANCEL_APPOINTMENT) }
        )
        HorizontalDivider(color = AppColors.Monotone.Gray100)
        ManageRow(
            title = stringResource(R.string.appointment_manage_delete),
            subtitle = stringResource(R.string.appointment_manage_delete_desc),
            enabled = !state.isWorking,
            danger = true,
            tag = "appointment-manage-delete",
            icon = R.drawable.appointment_delete,
            onClick = { onRequestConfirm(AppointmentManageConfirm.DELETE_APPOINTMENT) }
        )
        HorizontalDivider(color = AppColors.Monotone.Gray100)
        ManageRow(
            title = stringResource(R.string.appointment_manage_leave),
            subtitle = leaveMenuDescription(state.leaveMode),
            enabled = !state.isWorking,
            danger = true,
            tag = "appointment-manage-leave",
            icon = R.drawable.leave_meeting,
            onClick = { onOpenStep(AppointmentManageStep.LEAVE) }
        )
    }
}

@Composable
private fun TitleEditor(form: AppointmentCreateUiState, onFormChange: (AppointmentCreateUiState) -> Unit) {
    Text(
        text = stringResource(R.string.appointment_create_name_question),
        style = MaterialTheme.typography.titleSmall,
        color = AppColors.Monotone.SoftBlack
    )
    AppointmentTextField(
        value = form.appointmentName,
        onValueChange = { onFormChange(form.copy(appointmentName = it)) },
        placeholder = stringResource(R.string.appointment_create_name_hint),
        modifier = Modifier.testTag("appointment-manage-title-field")
    )
    Text(
        text = stringResource(
            R.string.appointment_manage_title_count,
            form.appointmentName.length,
            MAX_APPOINTMENT_TITLE_LENGTH
        ),
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.labelSmall,
        color = AppColors.Monotone.Gray600
    )
}

@Composable
private fun MemberManager(state: AppointmentManageUiState, onRequestKick: (Long) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing4)) {
        Text(
            text = stringResource(R.string.appointment_manage_members_title_label),
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.Monotone.Gray600
        )
        Text(
            text = state.saved.appointmentName,
            style = MaterialTheme.typography.titleMedium,
            color = AppColors.Monotone.SoftBlack
        )
    }
    SectionTitle(stringResource(R.string.appointment_manage_members_count, state.members.size))
    ManageCard {
        state.members.forEachIndexed { index, member ->
            if (index > 0) HorizontalDivider(color = AppColors.Monotone.Gray100)
            MemberRow(
                member = member,
                canKick = !state.isCanceled && !state.isWorking,
                onKick = { onRequestKick(member.memberId) }
            )
        }
    }
    Text(
        text = stringResource(R.string.appointment_manage_members_note),
        style = MaterialTheme.typography.labelSmall,
        color = AppColors.Monotone.Gray600
    )
}

@Composable
private fun MemberRow(member: ManageMember, canKick: Boolean, onKick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(horizontal = AppDimens.Spacing16, vertical = AppDimens.Spacing12)
            .testTag("appointment-manage-member-${member.memberId}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = if (member.isHost) AppColors.MainGreen.Green900 else AppColors.MainGreen.Green700
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = member.nickname.take(1),
                    style = MaterialTheme.typography.titleSmall,
                    color = AppColors.Monotone.White
                )
            }
        }
        Text(
            text = member.nickname,
            modifier = Modifier.weight(1f, fill = false),
            style = MaterialTheme.typography.titleSmall,
            color = AppColors.Monotone.SoftBlack,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (member.isHost) {
            Surface(shape = CircleShape, color = AppColors.MainGreen.Green200) {
                Text(
                    text = stringResource(R.string.appointment_manage_members_host_badge),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.MainGreen.Green900
                )
            }
        }
        Spacer(Modifier.weight(1f))
        if (!member.isHost) {
            Surface(
                onClick = onKick,
                enabled = canKick,
                modifier = Modifier.testTag("appointment-manage-kick-${member.memberId}"),
                shape = RoundedCornerShape(8.dp),
                color = AppColors.Monotone.White,
                border = BorderStroke(1.dp, AppColors.Danger.Red200)
            ) {
                Text(
                    text = stringResource(R.string.appointment_manage_members_kick),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (canKick) AppColors.Danger.Red600 else AppColors.Monotone.Gray500
                )
            }
        }
    }
}

/**
 * 참여 멤버의 날짜·시간 선택 현황(Figma APT-004-02). 몇 명이 골랐는지와 선택 완료·미완료 멤버,
 * 초대 링크 복사·공유를 보여 준다. 머리줄을 누르면 접고 펼친다.
 */
@Composable
private fun ResponseStatusCard(
    state: AppointmentManageUiState,
    onCopyInviteLink: () -> Unit,
    onShareInviteLink: () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    val total = state.members.size
    val responded = state.members.filter { it.scheduleSubmitted }
    val pending = state.members.filterNot { it.scheduleSubmitted }
    ManageCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.Spacing20)
                .testTag("appointment-manage-response-card"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .testTag("appointment-manage-response-toggle"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
            ) {
                Text(
                    text = stringResource(R.string.appointment_manage_response_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = AppColors.Monotone.SoftBlack,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(R.string.appointment_manage_response_count, responded.size, total),
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.MainGreen.Green700,
                    modifier = Modifier
                        .background(Color(0xFFE9EFE3), CircleShape)
                        .padding(horizontal = 10.dp, vertical = AppDimens.Spacing4)
                        .testTag("appointment-manage-response-count")
                )
                Image(
                    painter = painterResource(if (expanded) R.drawable.chevron_up else R.drawable.chevron_down),
                    contentDescription = null,
                    modifier = Modifier.size(AppDimens.HeaderIconSize)
                )
            }
            if (expanded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(AppColors.Monotone.Gray100)
                ) {
                    if (total > 0 && responded.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(responded.size.toFloat() / total)
                                .fillMaxHeight()
                                .background(AppColors.MainGreen.Green400, CircleShape)
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing16)) {
                    ResponseGroup(
                        label = stringResource(R.string.appointment_manage_response_done, responded.size),
                        members = responded,
                        responded = true,
                        modifier = Modifier.weight(1f).testTag("appointment-manage-response-done")
                    )
                    ResponseGroup(
                        label = stringResource(R.string.appointment_manage_response_pending, pending.size),
                        members = pending,
                        responded = false,
                        modifier = Modifier.weight(1f).testTag("appointment-manage-response-pending")
                    )
                }
                if (!state.isConfirmed) {
                    Text(
                        text = stringResource(R.string.appointment_manage_response_note),
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.Monotone.Gray600
                    )
                }
                HorizontalDivider(color = AppColors.Monotone.Gray100)
                Text(
                    text = stringResource(R.string.appointment_invite_link_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.Monotone.Gray600
                )
                Text(
                    text = state.inviteUrl?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.appointment_invite_link_pending),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.Monotone.Gray700,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.Monotone.Gray100, RoundedCornerShape(8.dp))
                        .padding(AppDimens.Spacing12)
                        .testTag("appointment-manage-invite-link")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)) {
                    InviteActionButton(
                        text = stringResource(R.string.appointment_copy_link),
                        filled = false,
                        tag = "appointment-manage-copy-link",
                        onClick = onCopyInviteLink,
                        modifier = Modifier.weight(1f)
                    )
                    InviteActionButton(
                        text = stringResource(R.string.appointment_manage_share_link),
                        filled = true,
                        tag = "appointment-manage-share-link",
                        onClick = onShareInviteLink,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = stringResource(R.string.appointment_manage_response_footnote),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.Monotone.Gray600
                )
            }
        }
    }
}

@Composable
private fun InviteActionButton(
    text: String,
    filled: Boolean,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 56.dp).testTag(tag),
        shape = RoundedCornerShape(12.dp),
        color = if (filled) AppColors.MainGreen.Green600 else AppColors.Monotone.White,
        border = if (filled) null else BorderStroke(1.dp, AppColors.Monotone.Gray200)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall,
                color = if (filled) AppColors.Monotone.White else AppColors.MainGreen.Green700
            )
        }
    }
}

@Composable
private fun ResponseGroup(
    label: String,
    members: List<ManageMember>,
    responded: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (responded) AppColors.MainGreen.Green700 else AppColors.Monotone.Gray600
        )
        members.forEach { member ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            if (responded) AppColors.MainGreen.Green400 else AppColors.Monotone.Gray300,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = member.nickname.take(1),
                        style = MaterialTheme.typography.titleSmall,
                        color = if (responded) AppColors.Monotone.White else AppColors.Monotone.Gray700
                    )
                }
                Text(
                    text = member.nickname,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (responded) AppColors.Monotone.SoftBlack else AppColors.Monotone.Gray600,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, badge: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = AppColors.Monotone.SoftBlack,
            modifier = Modifier.weight(1f)
        )
        if (badge != null) {
            Text(
                text = badge,
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.MainGreen.Green700,
                modifier = Modifier
                    .background(Color(0xFFE9EFE3), CircleShape)
                    .padding(horizontal = 10.dp, vertical = AppDimens.Spacing4)
            )
        }
    }
}

/** 미응답자 알림 줄의 설명. 아직 응답하지 않은 멤버 이름을 보여 주고, 없으면 기본 설명을 보여 준다. */
@Composable
private fun reminderSubtitle(state: AppointmentManageUiState): String {
    val pending = state.members.filterNot { it.scheduleSubmitted }
    if (state.isConfirmed || pending.isEmpty()) {
        return stringResource(R.string.appointment_manage_item_reminder_desc)
    }
    return if (pending.size == 1) {
        stringResource(R.string.appointment_manage_reminder_target_one, pending.first().nickname)
    } else {
        stringResource(
            R.string.appointment_manage_reminder_target_many,
            pending.first().nickname,
            pending.size - 1
        )
    }
}

@Composable
private fun ManageCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = AppColors.Monotone.White
    ) {
        Column { content() }
    }
}

@Composable
private fun ManageRow(
    title: String,
    subtitle: String,
    enabled: Boolean,
    tag: String,
    onClick: () -> Unit,
    icon: Int,
    danger: Boolean = false,
    tintIcon: Boolean = false,
    trailing: String? = null
) {
    val titleColor = when {
        !enabled -> AppColors.Monotone.Gray500
        danger -> AppColors.Danger.Red600
        else -> AppColors.Monotone.SoftBlack
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).testTag(tag),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AppDimens.Spacing20, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
        ) {
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                colorFilter = if (tintIcon) ColorFilter.tint(AppColors.MainGreen.Green700) else null
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall, color = titleColor)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.Monotone.Gray500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (trailing != null) {
                Text(
                    text = trailing,
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.MainGreen.Green700
                )
            }
            Box { Text(text = "›", style = MaterialTheme.typography.titleMedium, color = AppColors.Monotone.Gray500) }
        }
    }
}

@Composable
private fun AppointmentCreateUiState.dateSummary(undecided: String): String {
    val dates = when (scheduleType) {
        AppointmentScheduleType.SINGLE_DAY -> listOfNotNull(selectedStartDate)
        AppointmentScheduleType.DATE_RANGE -> listOfNotNull(selectedStartDate, selectedEndDate)
        AppointmentScheduleType.SPECIFIC_DATES -> selectedDates
        null -> emptyList()
    }
    return when {
        dates.isEmpty() -> undecided
        scheduleType == AppointmentScheduleType.DATE_RANGE && dates.size == 2 ->
            stringResource(
                R.string.appointment_manage_date_range,
                dates[0].month, dates[0].day, dates[1].month, dates[1].day
            )
        dates.size == 1 -> stringResource(R.string.appointment_manage_date_single, dates[0].month, dates[0].day)
        else -> stringResource(
            R.string.appointment_manage_date_multiple,
            dates[0].month, dates[0].day, dates.size - 1
        )
    }
}

@Composable
private fun AppointmentCreateUiState.locationSummary(undecided: String): String = when (selectedLocations.size) {
    0 -> undecided
    1 -> selectedLocations[0].name
    else -> stringResource(
        R.string.appointment_manage_locations_more,
        selectedLocations[0].name,
        selectedLocations.size - 1
    )
}

@Composable
private fun deadlineSummary(deadline: String?, undecided: String): String {
    val date = parseDeadlineDate(deadline) ?: return undecided
    return stringResource(R.string.appointment_manage_deadline_value, date.month, date.day)
}
