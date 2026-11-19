package com.jigmnnaga.presentation.mypage.sharing

import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
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

// Figma MYP-004의 아이콘 배지·안내 배너 배경. 디자인 시스템 색상에 없는 값이라 이 화면에서만 쓴다.
private val BadgeColor = Color(0xFFE1E9D9)
private val NoticeColor = Color(0xFFEEF3E8)
private val CardShape = RoundedCornerShape(16.dp)

private fun TextStyle.sized(size: Int, line: Int, weight: FontWeight): TextStyle =
    copy(fontSize = size.sp, lineHeight = line.sp, fontWeight = weight)

/**
 * 위치 공유 설정 화면(MYP-004). 위치 권한 상태, 위치 공유 사용, 공유 범위, 공유 시작 시간, 안내를 보여 주고
 * 바꾼 사실만 콜백으로 알린다.
 */
@Composable
fun LocationSharingSettingsScreen(
    state: LocationSharingSettingsUiState,
    onBackClick: () -> Unit,
    onSharingEnabledChange: (Boolean) -> Unit,
    onScopeSelect: (LocationSharingScope) -> Unit,
    onStartTimeSelect: (LocationSharingStartTime) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.MainGreen.Green200)
            .systemBarsPadding()
            .testTag("location-sharing-screen")
    ) {
        BackHeader(title = stringResource(R.string.location_sharing_title), onBackClick = onBackClick)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(AppDimens.Spacing20),
            verticalArrangement = Arrangement.spacedBy(30.dp)
        ) {
            PermissionCard()
            SharingEnableSection(enabled = state.sharingEnabled, onChange = onSharingEnabledChange)
            ScopeSection(selected = state.scope, onSelect = onScopeSelect)
            StartTimeSection(selected = state.startTime, onSelect = onStartTimeSelect)
            NoticeBanner()
        }
    }
}

@Composable
private fun SettingsCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = CardShape, clip = false)
            .background(Color.White, CardShape)
    ) { content() }
}

@Composable
private fun IconBadge(@DrawableRes iconRes: Int) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .background(BadgeColor, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = Color.Unspecified
        )
    }
}

@Composable
private fun SectionTitle(title: String, description: String? = null) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.sized(13, 18, FontWeight.Bold),
            color = AppColors.MainGreen.Green900
        )
        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall.sized(11, 16, FontWeight.Normal),
                color = AppColors.Monotone.Gray500
            )
        }
    }
}

@Composable
private fun PermissionCard() {
    SettingsCard(modifier = Modifier.testTag("location-sharing-permission")) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconBadge(R.drawable.location_permission)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = stringResource(R.string.location_sharing_permission_title),
                    style = MaterialTheme.typography.bodyMedium.sized(14, 20, FontWeight.Bold),
                    color = AppColors.Monotone.SoftBlack
                )
                Text(
                    text = stringResource(R.string.location_sharing_permission_granted_description),
                    style = MaterialTheme.typography.labelSmall.sized(11, 16, FontWeight.Normal),
                    color = AppColors.Monotone.Gray500
                )
            }
            Box(
                modifier = Modifier
                    .background(BadgeColor, CircleShape)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = stringResource(R.string.location_sharing_permission_granted_badge),
                    style = MaterialTheme.typography.labelSmall.sized(11, 14, FontWeight.Bold),
                    color = AppColors.MainGreen.Green700
                )
            }
        }
    }
}

@Composable
private fun SharingEnableSection(enabled: Boolean, onChange: (Boolean) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle(
            title = stringResource(R.string.location_sharing_friends_title),
            description = stringResource(R.string.location_sharing_friends_description)
        )
        SettingsCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                IconBadge(R.drawable.location_sharing)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(R.string.location_sharing_enable_title),
                        style = MaterialTheme.typography.bodyMedium.sized(14, 20, FontWeight.Normal),
                        color = AppColors.Monotone.SoftBlack
                    )
                    Text(
                        text = stringResource(R.string.location_sharing_enable_description),
                        style = MaterialTheme.typography.labelSmall.sized(11, 16, FontWeight.Normal),
                        color = AppColors.Monotone.Gray500
                    )
                }
                SharingSwitch(checked = enabled, onCheckedChange = onChange)
            }
        }
    }
}

/**
 * 44x24 토글. Figma에는 켜짐 모양만 있어서 켜짐은 Figma대로(초록 트랙, 오른쪽 흰 손잡이) 그리고,
 * 꺼짐은 같은 크기에서 회색 트랙과 왼쪽 손잡이로 그린다.
 */
@Composable
private fun SharingSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Box(
        modifier = Modifier
            .width(44.dp)
            .height(24.dp)
            .background(
                if (checked) AppColors.MainGreen.Green700 else AppColors.Monotone.Gray300,
                CircleShape
            )
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(2.dp)
            .testTag("location-sharing-switch"),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(Color.White, CircleShape)
        )
    }
}

@Composable
private fun ScopeSection(selected: LocationSharingScope, onSelect: (LocationSharingScope) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle(title = stringResource(R.string.location_sharing_scope_title))
        SettingsCard {
            ScopeRow(
                scope = LocationSharingScope.FRIENDS_ONLY,
                title = stringResource(R.string.location_sharing_scope_friends_title),
                description = stringResource(R.string.location_sharing_scope_friends_description),
                selected = selected == LocationSharingScope.FRIENDS_ONLY,
                showDivider = true,
                onSelect = onSelect
            )
            ScopeRow(
                scope = LocationSharingScope.NONE,
                title = stringResource(R.string.location_sharing_scope_none_title),
                description = stringResource(R.string.location_sharing_scope_none_description),
                selected = selected == LocationSharingScope.NONE,
                showDivider = false,
                onSelect = onSelect
            )
        }
    }
}

@Composable
private fun ScopeRow(
    scope: LocationSharingScope,
    title: String,
    description: String,
    selected: Boolean,
    showDivider: Boolean,
    onSelect: (LocationSharingScope) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(selected = selected, role = Role.RadioButton, onClick = { onSelect(scope) })
                .padding(horizontal = 15.dp, vertical = 11.dp)
                .testTag("location-sharing-scope-${scope.name}"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.sized(14, 20, FontWeight.Normal),
                    color = AppColors.Monotone.SoftBlack
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall.sized(11, 16, FontWeight.Normal),
                    color = AppColors.Monotone.Gray500
                )
            }
            RadioMark(selected)
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(AppColors.Monotone.Gray100)
            )
        }
    }
}

@Composable
private fun RadioMark(selected: Boolean) {
    if (selected) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(AppColors.MainGreen.Green600, CircleShape)
                .border(BorderStroke(1.5.dp, AppColors.MainGreen.Green600), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.check),
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = Color.White
            )
        }
    } else {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(Color.White, CircleShape)
                .border(BorderStroke(1.5.dp, AppColors.Monotone.Gray400), CircleShape)
        )
    }
}

@Composable
private fun StartTimeSection(selected: LocationSharingStartTime, onSelect: (LocationSharingStartTime) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle(
            title = stringResource(R.string.location_sharing_start_title),
            description = stringResource(R.string.location_sharing_start_description)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LocationSharingStartTime.entries.forEach { time ->
                StartTimeChip(
                    time = time,
                    label = stringResource(time.labelRes()),
                    selected = time == selected,
                    onSelect = onSelect
                )
            }
        }
    }
}

@Composable
private fun StartTimeChip(
    time: LocationSharingStartTime,
    label: String,
    selected: Boolean,
    onSelect: (LocationSharingStartTime) -> Unit
) {
    val background = if (selected) AppColors.MainGreen.Green600 else Color.White
    val border = if (selected) AppColors.MainGreen.Green600 else AppColors.Monotone.Gray100
    Box(
        modifier = Modifier
            .height(34.dp)
            .background(background, CircleShape)
            .border(BorderStroke(1.dp, border), CircleShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = { onSelect(time) })
            .padding(horizontal = 13.dp)
            .testTag("location-sharing-time-${time.name}"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.sized(
                12, 16, if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (selected) Color.White else AppColors.Monotone.Gray700
        )
    }
}

private fun LocationSharingStartTime.labelRes(): Int = when (this) {
    LocationSharingStartTime.MINUTES_10 -> R.string.location_sharing_start_10_minutes
    LocationSharingStartTime.MINUTES_30 -> R.string.location_sharing_start_30_minutes
    LocationSharingStartTime.HOUR_1 -> R.string.location_sharing_start_1_hour
}

@Composable
private fun NoticeBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NoticeColor, CardShape)
            .padding(14.dp)
            .testTag("location-sharing-notice"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        IconBadge(R.drawable.privacy)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = stringResource(R.string.location_sharing_notice_title),
                style = MaterialTheme.typography.labelMedium.sized(13, 18, FontWeight.Bold),
                color = AppColors.MainGreen.Green900
            )
            Text(
                text = stringResource(R.string.location_sharing_notice_description),
                style = MaterialTheme.typography.labelSmall.sized(11, 16, FontWeight.Normal),
                color = AppColors.Monotone.Gray700
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun LocationSharingSettingsScreenPreview() {
    JigmnnagaTheme {
        LocationSharingSettingsScreen(
            state = LocationSharingSettingsUiState(),
            onBackClick = {},
            onSharingEnabledChange = {},
            onScopeSelect = {},
            onStartTimeSelect = {}
        )
    }
}
