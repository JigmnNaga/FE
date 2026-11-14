package com.jigmnnaga.presentation.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jigmnnaga.R
import com.jigmnnaga.core.designsystem.component.AppButton
import com.jigmnnaga.core.designsystem.component.BackHeader
import com.jigmnnaga.core.designsystem.theme.AppColors
import com.jigmnnaga.core.designsystem.theme.AppDimens
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/**
 * 알림 화면(HOM-003) 순수 UI. 상태와 이벤트는 파라미터로만 주고받는다.
 * 알림을 눌렀을 때 어디로 갈지는 호출하는 쪽이 정한다.
 */
@Composable
fun NotificationScreen(
    state: NotificationUiState,
    onBackClick: () -> Unit,
    onTabSelect: (NotificationTab) -> Unit,
    onInviteConfirmClick: (Long) -> Unit,
    onActivityClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onLoadMore: () -> Unit = {},
    onRetryClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.MainGreen.Green200)
            .testTag("notification-screen")
    ) {
        Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

        BackHeader(
            title = stringResource(R.string.notification_screen_title),
            onBackClick = onBackClick
        )

        NotificationTabRow(
            selectedTab = state.selectedTab,
            onTabSelect = onTabSelect,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimens.Spacing20, vertical = AppDimens.Spacing16)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            when {
                state.loadStatus == NotificationLoadStatus.LOADING && state.isCurrentTabEmpty -> {
                    CircularProgressIndicator(
                        color = AppColors.MainGreen.Green900,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .testTag("notification-loading")
                    )
                }
                state.loadStatus == NotificationLoadStatus.ERROR && state.isCurrentTabEmpty -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = AppDimens.Spacing40)
                            .testTag("notification-error"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing16)
                    ) {
                        Text(
                            text = stringResource(R.string.notification_screen_error),
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 22.5.sp),
                            color = AppColors.Monotone.Gray400,
                            textAlign = TextAlign.Center
                        )
                        AppButton(
                            text = stringResource(R.string.notification_screen_retry),
                            onClick = onRetryClick,
                            modifier = Modifier.testTag("notification-retry")
                        )
                    }
                }
                state.isCurrentTabEmpty -> {
                    Text(
                        text = stringResource(R.string.notification_screen_empty),
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 22.5.sp),
                        color = AppColors.Monotone.Gray400,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .testTag("notification-empty")
                    )
                }
                else -> when (state.selectedTab) {
                    NotificationTab.INVITE -> InviteList(
                        items = state.invites,
                        onConfirmClick = onInviteConfirmClick,
                        onLoadMore = onLoadMore
                    )
                    NotificationTab.ACTIVITY -> ActivityList(
                        items = state.activities,
                        onClick = onActivityClick,
                        onLoadMore = onLoadMore
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationTabRow(
    selectedTab: NotificationTab,
    onTabSelect: (NotificationTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(AppColors.Monotone.Gray100, RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        NotificationTabItem(
            label = stringResource(R.string.notification_screen_tab_invite),
            selected = selectedTab == NotificationTab.INVITE,
            onClick = { onTabSelect(NotificationTab.INVITE) },
            modifier = Modifier.weight(1f).testTag("notification-tab-invite")
        )
        NotificationTabItem(
            label = stringResource(R.string.notification_screen_tab_activity),
            selected = selectedTab == NotificationTab.ACTIVITY,
            onClick = { onTabSelect(NotificationTab.ACTIVITY) },
            modifier = Modifier.weight(1f).testTag("notification-tab-activity")
        )
    }
}

@Composable
private fun NotificationTabItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .then(if (selected) Modifier.shadow(1.dp, shape) else Modifier)
            .clip(shape)
            .background(if (selected) AppColors.Monotone.White else Color.Transparent)
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = if (selected) AppColors.Monotone.SoftBlack else AppColors.Monotone.Gray500
        )
    }
}

/** 마지막에서 세 번째 항목이 보이면 다음 페이지를 요청한다. */
@Composable
private fun LoadMoreEffect(listState: LazyListState, onLoadMore: () -> Unit) {
    val currentOnLoadMore by rememberUpdatedState(onLoadMore)
    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            info.totalItemsCount > 0 && lastVisible >= info.totalItemsCount - 3
        }
            .distinctUntilChanged()
            .filter { it }
            .collect { currentOnLoadMore() }
    }
}

@Composable
private fun InviteList(
    items: List<InviteNotificationItem>,
    onConfirmClick: (Long) -> Unit,
    onLoadMore: () -> Unit
) {
    val listState = rememberLazyListState()
    LoadMoreEffect(listState = listState, onLoadMore = onLoadMore)
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = AppDimens.Spacing20, vertical = AppDimens.Spacing8),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(items = items, key = { it.id }) { item ->
            InviteNotificationCard(item = item, onConfirmClick = { onConfirmClick(item.id) })
        }
    }
}

@Composable
private fun InviteNotificationCard(
    item: InviteNotificationItem,
    onConfirmClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val pending = item.status == InviteNotificationStatus.PENDING
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Monotone.White, shape)
            .border(
                width = 1.dp,
                color = if (pending) AppColors.SubYellow.Yellow600 else AppColors.Monotone.Gray100,
                shape = shape
            )
            .padding(16.dp)
            .testTag("notification-invite-${item.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(AppColors.MainGreen.Green400, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.inviterName.firstCharacter().ifEmpty { stringResource(R.string.notification_screen_invite_avatar_fallback) },
                        style = MaterialTheme.typography.titleSmall,
                        color = AppColors.MainGreen.Green200
                    )
                }
                Column {
                    Text(
                        text = if (item.inviterName.isBlank()) {
                            stringResource(R.string.notification_screen_invited_fallback)
                        } else {
                            stringResource(R.string.notification_screen_invited_by, item.inviterName)
                        },
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        ),
                        color = AppColors.MainGreen.Green700,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.metaLabel,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, lineHeight = 15.sp),
                        color = AppColors.Monotone.Gray400,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (pending) {
                Text(
                    text = stringResource(R.string.notification_screen_unconfirmed),
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.SubYellow.Yellow600,
                    modifier = Modifier
                        .background(AppColors.SubYellow.Yellow600.copy(alpha = 0.18f), CircleShape)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        Text(
            text = item.appointmentTitle,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp, lineHeight = 22.5.sp),
            color = AppColors.Monotone.SoftBlack,
            modifier = Modifier.padding(top = 8.dp)
        )

        if (pending) {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.MainGreen.Green900)
                    .clickable(role = Role.Button, onClick = onConfirmClick)
                    .padding(vertical = 10.dp)
                    .testTag("notification-invite-confirm-${item.id}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.notification_screen_confirm),
                    style = MaterialTheme.typography.titleSmall,
                    color = AppColors.MainGreen.Green200
                )
            }
        } else {
            Text(
                text = stringResource(R.string.notification_screen_invite_handled),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
                color = AppColors.Monotone.Gray400,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
private fun ActivityList(
    items: List<ActivityNotificationItem>,
    onClick: (Long) -> Unit,
    onLoadMore: () -> Unit
) {
    val listState = rememberLazyListState()
    LoadMoreEffect(listState = listState, onLoadMore = onLoadMore)
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = AppDimens.Spacing20, vertical = AppDimens.Spacing8),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(items = items, key = { it.id }) { item ->
            ActivityNotificationRow(item = item, onClick = { onClick(item.id) })
        }
    }
}

@Composable
private fun ActivityNotificationRow(
    item: ActivityNotificationItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("notification-activity-${item.id}"),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = item.type.emoji(),
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp, lineHeight = 28.sp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.message,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.Monotone.SoftBlack
            )
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 16.5.sp),
                color = AppColors.Monotone.Gray400,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

private fun ActivityNotificationType.emoji(): String = when (this) {
    ActivityNotificationType.DEPARTED -> "🚶"
    ActivityNotificationType.ARRIVED -> "📍"
    ActivityNotificationType.JOINED -> "✅"
    ActivityNotificationType.MESSAGE -> "💬"
    ActivityNotificationType.REMINDER -> "🔔"
}

/** 이름의 첫 글자. 이모지처럼 두 칸을 차지하는 글자도 깨지지 않게 코드포인트 기준으로 자른다. */
private fun String.firstCharacter(): String {
    val trimmed = trim()
    return if (trimmed.isEmpty()) "" else trimmed.substring(0, trimmed.offsetByCodePoints(0, 1))
}

private val previewInvites = listOf(
    InviteNotificationItem(1, "태양", "방금 · 4명", "금요일 저녁 모임", InviteNotificationStatus.PENDING),
    InviteNotificationItem(2, "지우", "어제 · 6명", "스터디 정기 모임", InviteNotificationStatus.HANDLED)
)

private val previewActivities = listOf(
    ActivityNotificationItem(1, ActivityNotificationType.DEPARTED, "민준이 스터디 출발했어요", "스터디 모임 · 14분 전"),
    ActivityNotificationItem(2, ActivityNotificationType.ARRIVED, "지우가 도착했어요", "금요일 저녁 모임 · 32분 전"),
    ActivityNotificationItem(3, ActivityNotificationType.JOINED, "하은이 약속에 참여했어요", "금요일 저녁 모임 · 1시간 전"),
    ActivityNotificationItem(4, ActivityNotificationType.MESSAGE, "새 메시지가 도착했어요", "주말 영화 모임 · 3시간 전"),
    ActivityNotificationItem(5, ActivityNotificationType.REMINDER, "약속이 1시간 후에 시작해요", "스터디 모임 · 어제")
)

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun NotificationInvitePreview() {
    JigmnnagaTheme {
        NotificationScreen(
            state = NotificationUiState(invites = previewInvites, activities = previewActivities),
            onBackClick = {}, onTabSelect = {}, onInviteConfirmClick = {}, onActivityClick = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun NotificationActivityPreview() {
    JigmnnagaTheme {
        NotificationScreen(
            state = NotificationUiState(
                selectedTab = NotificationTab.ACTIVITY,
                invites = previewInvites,
                activities = previewActivities
            ),
            onBackClick = {}, onTabSelect = {}, onInviteConfirmClick = {}, onActivityClick = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun NotificationEmptyPreview() {
    JigmnnagaTheme {
        NotificationScreen(
            state = NotificationUiState(),
            onBackClick = {}, onTabSelect = {}, onInviteConfirmClick = {}, onActivityClick = {}
        )
    }
}
