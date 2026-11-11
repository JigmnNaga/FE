package com.jigmnnaga.presentation.appointment.existingadd

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jigmnnaga.R
import com.jigmnnaga.core.designsystem.component.AppButton
import com.jigmnnaga.core.designsystem.component.ButtonVariant
import com.jigmnnaga.core.designsystem.component.MeetingHeader
import com.jigmnnaga.core.designsystem.theme.AppColors
import com.jigmnnaga.core.designsystem.theme.AppDimens
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme

/**
 * APT-006 presentation only. Data, selection rules, transitions, and all side effects are supplied
 * by the caller. The map area draws a static illustration by default; a real map can be passed
 * through [mapContent] and is drawn on top of it.
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
                .padding(horizontal = AppDimens.Spacing20),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .semantics {
                        contentDescription = mapAreaDescription
                    }
            ) {
                MapIllustration(Modifier.matchParentSize())
                mapContent()
            }

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

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.members.forEach { member ->
                    MemberRow(
                        member = member,
                        selectable = state.page == ExistingAppointmentAddPage.REQUEST_PARTICIPANTS && member.isSelectable,
                        selected = member.id in state.selectedMemberIds,
                        onSelectionChange = { selected -> onMemberSelectionChange(member.id, selected) }
                    )
                }
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
                .padding(horizontal = AppDimens.Spacing20)
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
            .padding(horizontal = AppDimens.Spacing20)
            .padding(top = AppDimens.Spacing16, bottom = AppDimens.Spacing8),
        horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally)
    ) {
        RoomTab(
            label = stringResource(R.string.existing_add_chat_tab),
            iconRes = R.drawable.chat,
            iconSize = 22.dp,
            selected = selectedTab == ExistingAppointmentRoomTab.CHAT,
            onClick = onChatClick
        )
        RoomTab(
            label = stringResource(R.string.existing_add_map_status_tab),
            iconRes = R.drawable.place,
            iconSize = 20.dp,
            selected = selectedTab == ExistingAppointmentRoomTab.MAP_AND_STATUS,
            onClick = onMapAndStatusClick
        )
    }
}

@Composable
private fun RoomTab(
    label: String,
    @DrawableRes iconRes: Int,
    iconSize: Dp,
    selected: Boolean,
    onClick: () -> Unit
) {
    val contentColor = if (selected) AppColors.MainGreen.Green900 else AppColors.Monotone.Gray500
    Column(
        modifier = Modifier.selectable(selected = selected, onClick = onClick, role = Role.Tab)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                tint = contentColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = contentColor,
                maxLines = 1
            )
        }
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.5.dp)
                .background(if (selected) AppColors.MainGreen.Green700 else Color.Transparent)
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
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.Monotone.Gray600
        )
        Spacer(Modifier.width(AppDimens.Spacing4))
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(AppColors.SubYellow.Yellow600, CircleShape)
        )
    }
}

@Composable
private fun AppointmentSummary(scheduledTime: String, scheduledPlace: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimens.ButtonCornerRadius),
        color = AppColors.MainGreen.Green600
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = AppDimens.Spacing16,
                vertical = AppDimens.Spacing12
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.existing_add_scheduled_time),
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.MainGreen.Green400
                )
                Text(
                    text = scheduledTime,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 18.sp,
                        lineHeight = 28.sp
                    ),
                    color = AppColors.MainGreen.Green200,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.place),
                    contentDescription = null,
                    modifier = Modifier.size(10.dp),
                    tint = AppColors.MainGreen.Green200
                )
                Text(
                    text = scheduledPlace,
                    modifier = Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = AppColors.MainGreen.Green200,
                    textAlign = TextAlign.End,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
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
    val shape = RoundedCornerShape(AppDimens.ButtonCornerRadius)
    val containerColor = if (selected) AppColors.Monotone.White else AppColors.Monotone.Gray300.copy(alpha = 0.12f)
    val borderColor = if (selected) AppColors.SubYellow.Yellow600 else AppColors.Monotone.Gray300.copy(alpha = 0.4f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(containerColor)
            .then(
                if (selectable) {
                    Modifier.toggleable(
                        value = selected,
                        onValueChange = onSelectionChange,
                        role = Role.Checkbox
                    )
                } else {
                    Modifier
                }
            )
            .border(width = 1.2.dp, color = borderColor, shape = shape)
            .padding(horizontal = AppDimens.Spacing16, vertical = AppDimens.Spacing12)
            .semantics(mergeDescendants = true) {
                if (selectable) {
                    contentDescription = "${member.name}, ${member.statusLabel}"
                }
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (member.isMe) AppColors.MainGreen.Green700 else AppColors.Monotone.Gray300,
                    CircleShape
                )
                .border(2.4.dp, AppColors.MainGreen.Green200, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = member.name.take(1),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = if (member.isMe) AppColors.MainGreen.Green200 else AppColors.Monotone.Gray700
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = member.name,
                    modifier = Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = AppColors.Monotone.SoftBlack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(AppDimens.Spacing8))
                StatusBadge(member.statusLabel)
            }
            member.detailLabel?.let { detail ->
                Text(
                    text = detail,
                    modifier = Modifier.padding(top = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.Monotone.Gray500,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        member.trailingValue?.let { value ->
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 20.sp,
                        lineHeight = 20.sp
                    ),
                    color = AppColors.Monotone.Gray300,
                    maxLines = 1
                )
                member.trailingCaption?.let { caption ->
                    Text(
                        text = caption,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
                        color = AppColors.Monotone.Gray400,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(label: String) {
    Row(
        modifier = Modifier
            .background(AppColors.MainGreen.Green700.copy(alpha = 0.13f), CircleShape)
            .padding(horizontal = AppDimens.Spacing8, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .background(AppColors.MainGreen.Green700, CircleShape)
        )
        Spacer(Modifier.width(AppDimens.Spacing4))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.MainGreen.Green700,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ---- 지도 일러스트 (Figma 372 x 190 기준 좌표) ----

private val MapBase = Color(0xFFE7ECE1)
private val MapBlock = Color(0xFFDCE4D7)
private val MapRoad = Color(0xFFF4F7EE)

private data class MapRect(val left: Float, val top: Float, val right: Float, val bottom: Float)

private val MapBlocks = listOf(
    MapRect(12f, 17f, 138f, 82f),
    MapRect(170f, 17f, 262f, 82f),
    MapRect(290f, 17f, 355f, 82f),
    MapRect(12f, 105f, 138f, 141f),
    MapRect(170f, 105f, 262f, 141f)
)

@Composable
private fun MapIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sx = size.width / 372f
        val sy = size.height / 190f
        val u = minOf(sx, sy)

        drawRect(MapBase)

        drawRect(MapRoad, Offset(0f, 88f * sy), Size(size.width, 10f * sy))
        drawRect(MapRoad, Offset(0f, 148f * sy), Size(size.width, 8f * sy))
        drawRect(MapRoad, Offset(148f * sx, 0f), Size(12f * sx, size.height))
        drawRect(MapRoad, Offset(272f * sx, 0f), Size(8f * sx, size.height))

        MapBlocks.forEach { block ->
            drawRoundRect(
                color = MapBlock,
                topLeft = Offset(block.left * sx, block.top * sy),
                size = Size((block.right - block.left) * sx, (block.bottom - block.top) * sy),
                cornerRadius = CornerRadius(8f * u)
            )
        }

        val route = Path().apply {
            moveTo(92f * sx, 103f * sy)
            cubicTo(105f * sx, 82f * sy, 150f * sx, 100f * sy, 172f * sx, 88f * sy)
        }
        drawPath(
            path = route,
            color = AppColors.Monotone.Gray400,
            style = Stroke(
                width = 2f * u,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f * u, 4f * u))
            )
        )

        drawOval(
            color = Color(0x26000000),
            topLeft = Offset(169f * sx, 92f * sy),
            size = Size(24f * u, 8f * u)
        )
        drawPin(
            cx = 181f * sx,
            cy = 70f * sy,
            r = 13f * u,
            color = AppColors.MainGreen.Green900,
            ringColor = AppColors.Monotone.White,
            dotColor = AppColors.MainGreen.Green900
        )
        drawPin(
            cx = 91f * sx,
            cy = 112f * sy,
            r = 9f * u,
            color = AppColors.SubYellow.Yellow600,
            ringColor = AppColors.Monotone.White,
            dotColor = null
        )
    }
}

private fun DrawScope.drawPin(
    cx: Float,
    cy: Float,
    r: Float,
    color: Color,
    ringColor: Color,
    dotColor: Color?
) {
    val tipY = cy + r * 1.9f
    val body = Path().apply {
        moveTo(cx, tipY)
        cubicTo(cx - r * 1.2f, cy + r * 0.5f, cx - r * 1.35f, cy - r, cx, cy - r)
        cubicTo(cx + r * 1.35f, cy - r, cx + r * 1.2f, cy + r * 0.5f, cx, tipY)
        close()
    }
    drawPath(body, color)
    drawCircle(ringColor, radius = r * 0.5f, center = Offset(cx, cy))
    dotColor?.let { drawCircle(it, radius = r * 0.15f, center = Offset(cx, cy)) }
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
                selectedMemberIds = setOf("member-1", "member-2", "member-3"),
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
    scheduledPlace = "강남역 카공카페",
    liveStatusLabel = "실시간",
    members = listOf(
        ExistingAppointmentMemberUiState("member-1", "나", "도착", "장소에 도착했어요", isMe = true),
        ExistingAppointmentMemberUiState("member-2", "태양", "도착", "장소에 도착했어요", "2분", "지각"),
        ExistingAppointmentMemberUiState("member-3", "하은", "도착", "장소에 도착했어요", "5분", "지각")
    )
)