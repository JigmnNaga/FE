package com.jigmnnaga.presentation.mypage.notification

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
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

// Figma MYP-003 고유 색상. 디자인 시스템 색상에 없는 값이라 이 화면에서만 쓴다.
private val BadgeGreenColor = Color(0xFFE1E9D9)       // 약속 알림·전체 알림·안내 배너 아이콘 배지
private val BadgeYellowColor = Color(0xFFF5EFCF)      // 소셜/서비스 알림 아이콘 배지
private val NoticeBannerColor = Color(0xFFEEF3E8)     // 안내 배너 배경
private val CardShape = RoundedCornerShape(16.dp)
private val BadgeShape = RoundedCornerShape(12.dp)

private fun TextStyle.sized(size: Int, line: Int, weight: FontWeight): TextStyle =
    copy(fontSize = size.sp, lineHeight = line.sp, fontWeight = weight)

/**
 * MYP-003 알림 설정 화면 순수 UI Composable.
 * 상태는 [NotificationSettingsUiState]로 받고, 변경 이벤트는 콜백으로 발행한다.
 */
@Composable
fun NotificationSettingsScreen(
    state: NotificationSettingsUiState,
    onBackClick: () -> Unit,
    onAllNotificationsChange: (Boolean) -> Unit,
    onAppointmentReminderChange: (Boolean) -> Unit,
    onDepartureTimeChange: (Boolean) -> Unit,
    onFriendArrivalChange: (Boolean) -> Unit,
    onMarketingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.MainGreen.Green200)
            .systemBarsPadding()
    ) {
        BackHeader(
            title = stringResource(R.string.notification_settings_title),
            onBackClick = onBackClick
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(AppDimens.Spacing20),
            verticalArrangement = Arrangement.spacedBy(30.dp)
        ) {
            // 전체 알림 카드
            AllNotificationsCard(
                enabled = state.allNotificationsEnabled,
                onChange = onAllNotificationsChange
            )

            // 약속 알림 섹션
            AppointmentSection(
                reminderEnabled = state.appointmentReminderEnabled,
                departureEnabled = state.departureTimeEnabled,
                arrivalEnabled = state.friendArrivalEnabled,
                onReminderChange = onAppointmentReminderChange,
                onDepartureChange = onDepartureTimeChange,
                onArrivalChange = onFriendArrivalChange
            )

            // 소셜 및 서비스 알림 섹션
            SocialSection(
                marketingEnabled = state.marketingEnabled,
                onMarketingChange = onMarketingChange
            )

            // 안내 배너
            NoticeBanner()
        }
    }
}

// ─────────────────────────────────────────
// 전체 알림 카드
// ─────────────────────────────────────────

@Composable
private fun AllNotificationsCard(enabled: Boolean, onChange: (Boolean) -> Unit) {
    SettingsCard {
        NotificationItem(
            iconRes = R.drawable.deadline_notification,
            badgeColor = BadgeGreenColor,
            title = stringResource(R.string.notification_settings_all_title),
            description = stringResource(R.string.notification_settings_all_description),
            checked = enabled,
            onCheckedChange = onChange,
            showDivider = false
        )
    }
}

// ─────────────────────────────────────────
// 약속 알림 섹션
// ─────────────────────────────────────────

@Composable
private fun AppointmentSection(
    reminderEnabled: Boolean,
    departureEnabled: Boolean,
    arrivalEnabled: Boolean,
    onReminderChange: (Boolean) -> Unit,
    onDepartureChange: (Boolean) -> Unit,
    onArrivalChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle(
            title = stringResource(R.string.notification_settings_appointment_section_title),
            description = stringResource(R.string.notification_settings_appointment_section_description)
        )
        SettingsCard {
            NotificationItem(
                iconRes = R.drawable.appointment_reminder,
                badgeColor = BadgeGreenColor,
                title = stringResource(R.string.notification_settings_reminder_title),
                description = stringResource(R.string.notification_settings_reminder_description),
                checked = reminderEnabled,
                onCheckedChange = onReminderChange,
                showDivider = true
            )
            NotificationItem(
                iconRes = R.drawable.departure_time,
                badgeColor = BadgeGreenColor,
                title = stringResource(R.string.notification_settings_departure_title),
                description = stringResource(R.string.notification_settings_departure_description),
                checked = departureEnabled,
                onCheckedChange = onDepartureChange,
                showDivider = true
            )
            NotificationItem(
                iconRes = R.drawable.participation_confirm,
                badgeColor = BadgeGreenColor,
                title = stringResource(R.string.notification_settings_arrival_title),
                description = stringResource(R.string.notification_settings_arrival_description),
                checked = arrivalEnabled,
                onCheckedChange = onArrivalChange,
                showDivider = false
            )
        }
    }
}

// ─────────────────────────────────────────
// 소셜 및 서비스 알림 섹션
// ─────────────────────────────────────────

@Composable
private fun SocialSection(
    marketingEnabled: Boolean,
    onMarketingChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle(
            title = stringResource(R.string.notification_settings_social_section_title),
            description = null
        )
        SettingsCard {
            NotificationItem(
                iconRes = R.drawable.marketing_notification,
                badgeColor = BadgeYellowColor,
                title = stringResource(R.string.notification_settings_marketing_title),
                description = stringResource(R.string.notification_settings_marketing_description),
                checked = marketingEnabled,
                onCheckedChange = onMarketingChange,
                showDivider = false
            )
        }
    }
}

// ─────────────────────────────────────────
// 안내 배너
// ─────────────────────────────────────────

@Composable
private fun NoticeBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NoticeBannerColor, CardShape)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        IconBadge(iconRes = R.drawable.help, badgeColor = BadgeGreenColor)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = stringResource(R.string.notification_settings_notice_title),
                style = MaterialTheme.typography.labelMedium.sized(13, 18, FontWeight.Bold),
                color = AppColors.MainGreen.Green900
            )
            Text(
                text = stringResource(R.string.notification_settings_notice_description),
                style = MaterialTheme.typography.labelSmall.sized(11, 16, FontWeight.Normal),
                color = AppColors.Monotone.Gray700
            )
        }
    }
}

// ─────────────────────────────────────────
// 공통 하위 Composable
// ─────────────────────────────────────────

/** 흰색 카드 컨테이너. shadow + rounded-16 */
@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = CardShape, clip = false)
            .background(Color.White, CardShape)
    ) { content() }
}

/** 섹션 제목 + 설명 영역 */
@Composable
private fun SectionTitle(title: String, description: String?) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
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

/**
 * 알림 설정 항목 행. 아이콘 배지 · 텍스트 · 토글로 구성된다.
 * [showDivider]가 true이면 하단에 Gray100 구분선을 그린다.
 */
@Composable
private fun NotificationItem(
    @DrawableRes iconRes: Int,
    badgeColor: Color,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    showDivider: Boolean
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            IconBadge(iconRes = iconRes, badgeColor = badgeColor)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
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
            NotificationToggle(checked = checked, onCheckedChange = onCheckedChange)
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

/** 아이콘 배지: 34dp, 12dp 라운드. 아이콘 색상은 Color.Unspecified(원본 그대로). */
@Composable
private fun IconBadge(@DrawableRes iconRes: Int, badgeColor: Color) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .background(badgeColor, BadgeShape),
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

/**
 * 44×24dp 토글. Figma: 켜짐 → Green700 트랙 + 오른쪽 흰 손잡이, 꺼짐 → Gray300 트랙 + 왼쪽.
 * LocationSharingSettingsScreen의 SharingSwitch와 동일한 패턴.
 */
@Composable
private fun NotificationToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Box(
        modifier = Modifier
            .width(44.dp)
            .height(24.dp)
            .background(
                if (checked) AppColors.MainGreen.Green700 else AppColors.Monotone.Gray300,
                CircleShape
            )
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(2.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(Color.White, CircleShape)
        )
    }
}

// ─────────────────────────────────────────
// Preview
// ─────────────────────────────────────────

@Preview(showBackground = true, widthDp = 412, heightDp = 892, name = "기본 알림 설정 화면")
@Composable
private fun NotificationSettingsScreenPreview() {
    JigmnnagaTheme {
        NotificationSettingsScreen(
            state = NotificationSettingsUiState(),
            onBackClick = {},
            onAllNotificationsChange = {},
            onAppointmentReminderChange = {},
            onDepartureTimeChange = {},
            onFriendArrivalChange = {},
            onMarketingChange = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892, name = "전체 알림 꺼짐")
@Composable
private fun NotificationSettingsScreenAllOffPreview() {
    JigmnnagaTheme {
        NotificationSettingsScreen(
            state = NotificationSettingsUiState(
                allNotificationsEnabled = false,
                appointmentReminderEnabled = false,
                departureTimeEnabled = false,
                friendArrivalEnabled = false,
                marketingEnabled = false
            ),
            onBackClick = {},
            onAllNotificationsChange = {},
            onAppointmentReminderChange = {},
            onDepartureTimeChange = {},
            onFriendArrivalChange = {},
            onMarketingChange = {}
        )
    }
}
