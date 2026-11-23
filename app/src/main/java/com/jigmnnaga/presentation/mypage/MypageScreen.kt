package com.jigmnnaga.presentation.mypage

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jigmnnaga.R
import com.jigmnnaga.core.designsystem.component.BackHeader
import com.jigmnnaga.core.designsystem.theme.AppColors
import com.jigmnnaga.core.designsystem.theme.AppDimens
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme

// Figma: 통계 숫자 색상 → Green700, 도착률 퍼센트 색상 → SubYellow Yellow600

/**
 * MYP-001 마이페이지 메인 화면 순수 UI Composable.
 * ViewModel 의존 없이 상태와 이벤트 콜백을 파라미터로 주고받는다.
 */
@Composable
fun MypageScreen(
    state: MypageUiState,
    onBackClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onNotificationSettingsClick: () -> Unit,
    onLocationSharingSettingsClick: () -> Unit,
    onHelpClick: () -> Unit,
    onServiceInfoClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDefaultLocationClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onWithdrawClick: () -> Unit = {}
) {
    // 하단 여백을 포함한 전체 배경을 Green300으로 설정 (상단은 각 섹션에서 명시적으로 Green200 처리)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.MainGreen.Green300)
            .verticalScroll(rememberScrollState())
    ) {
        // 상태바 영역: Green200 배경 명시
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(AppColors.MainGreen.Green200)
        )

        // 헤더: "프로필" + 뒤로가기 버튼 (BackHeader 내부에서 Green200 배경 처리)
        BackHeader(
            title = "프로필",
            onBackClick = onBackClick
        )

        // 상단 body: 프로필 + 통계 (Green200 배경)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.MainGreen.Green200)
                .padding(
                    horizontal = AppDimens.Spacing20,
                    vertical = AppDimens.Spacing20
                ),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing20)
        ) {
            ProfileRow(
                nickname = state.nickname,
                email = state.email,
                profileInitial = state.profileInitial,
                onEditClick = onEditProfileClick
            )
            StatsRow(
                totalAppointments = state.totalAppointments,
                onTimeCount = state.onTimeCount,
                lateCount = state.lateCount,
                onTimeRateText = state.onTimeRateText,
                onTimeRatePercent = state.onTimeRatePercent
            )
        }

        // 경계 곡선: Figma imgIcon6 — Green300 위에 Green200 돔 아치
        WaveDivider()

        // 하단 body: 설정 메뉴 + 로그아웃/회원탈퇴 + Footer (Green300 배경)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.MainGreen.Green300)
                .padding(
                    horizontal = AppDimens.Spacing20,
                    vertical = AppDimens.Spacing20
                ),
            verticalArrangement = Arrangement.spacedBy(30.dp)
        ) {
            SettingsCard(
                onNotificationSettingsClick = onNotificationSettingsClick,
                onDefaultLocationClick = onDefaultLocationClick,
                onLocationSharingSettingsClick = onLocationSharingSettingsClick,
                onHelpClick = onHelpClick,
                onServiceInfoClick = onServiceInfoClick
            )
            LogoutWithdrawRow(
                onLogoutClick = onLogoutClick,
                onWithdrawClick = onWithdrawClick
            )
            AppFooter()
        }

        // 하단 내비게이션 바 여백 (Green300 배경이 외부 Column에서 채워짐)
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

// ─────────────────────────────────────────
// 경계 곡선 (Figma imgIcon6)
// ─────────────────────────────────────────

/**
 * Green200 → Green300 경계 곡선.
 * Figma: 412×30dp, Green300 배경 위에 Green200 돔 아치가 중앙에서 아래로 휘어 내려감.
 *
 * Green200 아치 하단 경계:
 *  - 좌우 가장자리: 높이의 약 30% 지점에서 시작
 *  - 중앙: 전체 높이(100%)까지 내려옴 (2차 베지어, 제어점 h*1.7)
 */
@Composable
private fun WaveDivider(modifier: Modifier = Modifier) {
    val green200 = AppColors.MainGreen.Green200
    val green300 = AppColors.MainGreen.Green300
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(30.dp)
    ) {
        val w = size.width
        val h = size.height

        // Green300 배경
        drawRect(color = green300)

        // Green200 돔 아치
        // 상단은 전체 너비, 하단 경계는 좌우 h*0.3에서 시작해 중앙 h*1.0까지 내려오는 곡선
        val path = Path().apply {
            moveTo(0f, 0f)              // 좌상단
            lineTo(w, 0f)              // 우상단
            lineTo(w, h * 0.3f)        // 우측 가장자리 (30% 지점)
            quadraticTo(
                w / 2f, h * 1.7f,      // 제어점: 중앙 하단 (캔버스 아래, 깊은 아치)
                0f, h * 0.3f           // 좌측 가장자리 (30% 지점)
            )
            close()
        }
        drawPath(path = path, color = green200)
    }
}

// ─────────────────────────────────────────
// 프로필 행
// ─────────────────────────────────────────

@Composable
private fun ProfileRow(
    nickname: String,
    email: String,
    profileInitial: String?,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(76.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ProfileAvatar(initial = profileInitial)

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            // 이름: Figma 22sp / font-black → Bold 대체 (Noto Sans KR Black 미등록)
            Text(
                text = nickname,
                style = TextStyle(
                    fontFamily = MaterialTheme.typography.titleMedium.fontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    lineHeight = 33.sp
                ),
                color = AppColors.Monotone.SoftBlack,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            // 이메일: 14sp / Regular / Gray700
            Text(
                text = email,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.Monotone.Gray700,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 프로필 수정 버튼: 42dp 원형, White 배경, Green500 테두리
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(AppColors.Monotone.White, CircleShape)
                .border(1.dp, AppColors.MainGreen.Green500, CircleShape)
                .clip(CircleShape)
                .clickable(onClick = onEditClick),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.edit),
                contentDescription = "프로필 수정",
                modifier = Modifier.size(18.dp),
                colorFilter = ColorFilter.tint(AppColors.Monotone.SoftBlack)
            )
        }
    }
}

@Composable
private fun ProfileAvatar(
    initial: String?,
    modifier: Modifier = Modifier
) {
    // Figma: 76dp 원형, Green700 배경, Green200 테두리
    Box(
        modifier = modifier
            .size(76.dp)
            .background(AppColors.MainGreen.Green700, CircleShape)
            .border(1.dp, AppColors.MainGreen.Green200, CircleShape)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (initial.isNullOrBlank()) {
            Image(
                painter = painterResource(R.drawable.profile),
                contentDescription = null,
                modifier = Modifier.size(38.dp),
                colorFilter = ColorFilter.tint(AppColors.MainGreen.Green200)
            )
        } else {
            Text(
                text = initial.trim().take(1),
                style = TextStyle(
                    fontFamily = MaterialTheme.typography.titleMedium.fontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    lineHeight = 20.sp
                ),
                color = AppColors.MainGreen.Green200
            )
        }
    }
}

// ─────────────────────────────────────────
// 통계 행
// ─────────────────────────────────────────

@Composable
private fun StatsRow(
    totalAppointments: Int,
    onTimeCount: Int,
    lateCount: Int,
    onTimeRateText: String,
    onTimeRatePercent: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(142.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 제때 도착률 원형 차트 (142×142dp)
        ArrivalRateChart(
            ratePercent = onTimeRatePercent,
            rateText = onTimeRateText,
            modifier = Modifier.size(142.dp)
        )

        // 중앙 구분선: 1dp × 108dp, Gray200
        Spacer(modifier = Modifier.width(16.dp))
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(108.dp)
                .background(AppColors.Monotone.Gray200)
        )
        Spacer(modifier = Modifier.width(16.dp))

        // 오른쪽 통계 목록
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 총 약속: calendar_gray (라이트 배경용, 18dp)
            StatItem(
                icon = {
                    Image(
                        painter = painterResource(R.drawable.calendar_gray),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        colorFilter = ColorFilter.tint(AppColors.Monotone.SoftBlack)
                    )
                },
                label = "총 약속",
                count = totalAppointments
            )
            // 제때 도착: participation_confirm (원형 체크, 22dp)
            StatItem(
                icon = {
                    Image(
                        painter = painterResource(R.drawable.participation_confirm),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        colorFilter = ColorFilter.tint(AppColors.Monotone.SoftBlack)
                    )
                },
                label = "제때 도착",
                count = onTimeCount
            )
            // 지각: Canvas 원형 X (22dp)
            StatItem(
                icon = { CircleXIcon() },
                label = "지각",
                count = lateCount
            )
        }
    }
}

/** 제때 도착률 원형 차트. Canvas로 아크를 그리고 중앙에 텍스트를 배치한다. */
@Composable
private fun ArrivalRateChart(
    ratePercent: Float,
    rateText: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 13.dp.toPx()
            val padding = strokeWidth / 2f + 4.dp.toPx()
            val diameter = size.minDimension - padding * 2f
            val topLeft = Offset(padding, padding)
            val arcSize = Size(diameter, diameter)

            // 배경 원호 (빈 부분): Green300, 각진 끝단 (StrokeCap.Butt)
            drawArc(
                color = AppColors.MainGreen.Green300,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                topLeft = topLeft,
                size = arcSize
            )

            // 채워진 원호 (도착률 비율): Green700, 각진 끝단 (StrokeCap.Butt)
            if (ratePercent > 0f) {
                drawArc(
                    color = AppColors.MainGreen.Green700,
                    startAngle = -90f,
                    sweepAngle = 360f * (ratePercent / 100f).coerceIn(0f, 1f),
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                    topLeft = topLeft,
                    size = arcSize
                )
            }
        }

        // 중앙 텍스트
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "제때 도착률",
                style = TextStyle(
                    fontFamily = MaterialTheme.typography.titleSmall.fontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                ),
                color = AppColors.Monotone.SoftBlack
            )
            Spacer(modifier = Modifier.height(4.dp))
            // Figma: 23sp / font-black → Bold 대체, 색상 SubYellow Yellow600 (#DDC55B)
            Text(
                text = rateText,
                style = TextStyle(
                    fontFamily = MaterialTheme.typography.titleSmall.fontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 23.sp,
                    lineHeight = 34.5.sp
                ),
                color = AppColors.SubYellow.Yellow600
            )
        }
    }
}

/** 통계 항목 행. icon 슬롯에 원하는 아이콘 Composable을 넣는다. */
@Composable
private fun StatItem(
    icon: @Composable () -> Unit,
    label: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 아이콘 컨테이너: 28dp 너비 고정 (Figma 기준)
        Box(
            modifier = Modifier.width(28.dp),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        // 레이블: Bold 14sp
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = AppColors.Monotone.SoftBlack,
            modifier = Modifier.weight(1f)
        )
        // 숫자: 22sp / font-black → Bold 대체, 색상 Green700 (#566149)
        Text(
            text = count.toString(),
            style = TextStyle(
                fontFamily = MaterialTheme.typography.titleSmall.fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                lineHeight = 33.sp
            ),
            color = AppColors.MainGreen.Green700
        )
    }
}

/** 지각 아이콘: Canvas로 그린 원형 + X 마크 (22dp, Figma late_arrival 대응). */
@Composable
private fun CircleXIcon(
    modifier: Modifier = Modifier,
    color: Color = AppColors.Monotone.SoftBlack
) {
    Canvas(modifier = modifier.size(22.dp)) {
        val strokeWidthPx = 1.5.dp.toPx()
        val radius = size.minDimension / 2f - strokeWidthPx / 2f
        val cx = size.width / 2f
        val cy = size.height / 2f

        // 원형 테두리
        drawCircle(
            color = color,
            radius = radius,
            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
        )

        // X 마크 (두 대각선)
        val xOffset = radius * 0.33f
        drawLine(
            color = color,
            start = Offset(cx - xOffset, cy - xOffset),
            end = Offset(cx + xOffset, cy + xOffset),
            strokeWidth = strokeWidthPx,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(cx + xOffset, cy - xOffset),
            end = Offset(cx - xOffset, cy + xOffset),
            strokeWidth = strokeWidthPx,
            cap = StrokeCap.Round
        )
    }
}

// ─────────────────────────────────────────
// 설정 카드
// ─────────────────────────────────────────

@Composable
private fun SettingsCard(
    onNotificationSettingsClick: () -> Unit,
    onDefaultLocationClick: () -> Unit,
    onLocationSharingSettingsClick: () -> Unit,
    onHelpClick: () -> Unit,
    onServiceInfoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Figma: rounded-[24px], bg rgba(255,255,255,0.86), shadow 0px 8px 24px rgba(65,74,56,0.08)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(24.dp), clip = false)
            .background(Color.White.copy(alpha = 0.86f), RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
    ) {
        SettingsMenuItem(
            iconRes = R.drawable.bell,
            title = "알림 설정",
            subtitle = "약속·멤버 도착 알림",
            onClick = onNotificationSettingsClick,
            isLast = false
        )
        SettingsMenuItem(
            iconRes = R.drawable.place,
            title = "기본 위치 설정",
            subtitle = "집·회사 등 자주 출발하는 위치 등록",
            onClick = onDefaultLocationClick,
            isLast = false
        )
        SettingsMenuItem(
            iconRes = R.drawable.location_sharing_settings,
            title = "위치 공유 설정",
            subtitle = "자동 공유 시작 시간",
            onClick = onLocationSharingSettingsClick,
            isLast = false
        )
        SettingsMenuItem(
            iconRes = R.drawable.contact_help,
            title = "도움말 및 문의",
            subtitle = null,
            onClick = onHelpClick,
            isLast = false
        )
        SettingsMenuItem(
            iconRes = R.drawable.help,
            title = "서비스 정보",
            subtitle = "이용약관 · 개인정보 처리방침 · 앱 정보",
            onClick = onServiceInfoClick,
            isLast = true
        )
    }
}

@Composable
private fun SettingsMenuItem(
    iconRes: Int,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    isLast: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                // Figma: px-[20px] py-[14px]
                .padding(horizontal = AppDimens.Spacing20, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 아이콘 컨테이너: 32dp 너비 고정 (Figma 기준)
            Box(
                modifier = Modifier.width(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    colorFilter = ColorFilter.tint(AppColors.Monotone.SoftBlack)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                // 메뉴 타이틀: 15sp Bold
                Text(
                    text = title,
                    style = TextStyle(
                        fontFamily = MaterialTheme.typography.titleSmall.fontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        lineHeight = 22.5.sp
                    ),
                    color = AppColors.Monotone.SoftBlack
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    // 부제목: 12sp Regular, Gray600
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.Monotone.Gray600,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 오른쪽 화살표: 10dp × 17dp (Figma: h-[17px] w-[10px])
            Image(
                painter = painterResource(R.drawable.arrow_right),
                contentDescription = null,
                modifier = Modifier.size(width = 10.dp, height = 17.dp),
                colorFilter = ColorFilter.tint(AppColors.Monotone.Gray500)
            )
        }

        if (!isLast) {
            // Figma: border-b-[0.667px] border-[var(--monotone\/100,#eef0ed)]
            HorizontalDivider(
                color = AppColors.Monotone.Gray100,
                thickness = (0.667).dp
            )
        }
    }
}

// ─────────────────────────────────────────
// 로그아웃 / 회원탈퇴 행
// ─────────────────────────────────────────

/**
 * Figma MYP-001: "로그아웃 · 회원탈퇴" 텍스트 버튼 행.
 * Gray700, 14sp Bold, 중앙 정렬. 가운데 점 구분자.
 */
@Composable
private fun LogoutWithdrawRow(
    onLogoutClick: () -> Unit,
    onWithdrawClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "로그아웃",
            style = TextStyle(
                fontFamily = MaterialTheme.typography.titleSmall.fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                lineHeight = 20.sp
            ),
            color = AppColors.Monotone.Gray700,
            modifier = Modifier
                .clickable(onClick = onLogoutClick)
                .padding(vertical = 8.dp)
        )
        Spacer(modifier = Modifier.width(20.dp))
        Text(
            text = "·",
            style = TextStyle(
                fontFamily = MaterialTheme.typography.titleSmall.fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                lineHeight = 20.sp
            ),
            color = AppColors.Monotone.Gray700
        )
        Spacer(modifier = Modifier.width(20.dp))
        Text(
            text = "회원탈퇴",
            style = TextStyle(
                fontFamily = MaterialTheme.typography.titleSmall.fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                lineHeight = 20.sp
            ),
            color = AppColors.Monotone.Gray700,
            modifier = Modifier
                .clickable(onClick = onWithdrawClick)
                .padding(vertical = 8.dp)
        )
    }
}

// ─────────────────────────────────────────
// Footer
// ─────────────────────────────────────────

@Composable
private fun AppFooter(modifier: Modifier = Modifier) {
    // Figma: 12sp Regular, White, 중앙 정렬, gap 10dp
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "지금 나가 v1.0.0",
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.Monotone.White
        )
        Text(
            text = "made by JigmnNaga.team",
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.Monotone.White
        )
    }
}

// ─────────────────────────────────────────
// Preview
// ─────────────────────────────────────────

@Preview(showSystemUi = true, name = "기본 마이페이지 화면")
@Composable
private fun MypageScreenDefaultPreview() {
    JigmnnagaTheme {
        MypageScreen(
            state = MypageUiState.PreviewDefault,
            onBackClick = {},
            onEditProfileClick = {},
            onNotificationSettingsClick = {},
            onLocationSharingSettingsClick = {},
            onHelpClick = {},
            onServiceInfoClick = {}
        )
    }
}

@Preview(showSystemUi = true, name = "프로필 이미지 없는 화면")
@Composable
private fun MypageScreenNoProfileImagePreview() {
    JigmnnagaTheme {
        MypageScreen(
            state = MypageUiState.PreviewNoProfileImage,
            onBackClick = {},
            onEditProfileClick = {},
            onNotificationSettingsClick = {},
            onLocationSharingSettingsClick = {},
            onHelpClick = {},
            onServiceInfoClick = {}
        )
    }
}

@Preview(showSystemUi = true, name = "긴 닉네임 및 이메일")
@Composable
private fun MypageScreenLongNamePreview() {
    JigmnnagaTheme {
        MypageScreen(
            state = MypageUiState.PreviewLongName,
            onBackClick = {},
            onEditProfileClick = {},
            onNotificationSettingsClick = {},
            onLocationSharingSettingsClick = {},
            onHelpClick = {},
            onServiceInfoClick = {}
        )
    }
}

@Preview(showSystemUi = true, name = "통계 데이터 0인 화면")
@Composable
private fun MypageScreenZeroStatsPreview() {
    JigmnnagaTheme {
        MypageScreen(
            state = MypageUiState.PreviewZeroStats,
            onBackClick = {},
            onEditProfileClick = {},
            onNotificationSettingsClick = {},
            onLocationSharingSettingsClick = {},
            onHelpClick = {},
            onServiceInfoClick = {}
        )
    }
}

@Preview(
    showBackground = true,
    widthDp = 412,
    heightDp = 892,
    name = "Figma 원본 프레임 크기 (412×892)"
)
@Composable
private fun MypageScreenFigmaSizePreview() {
    JigmnnagaTheme {
        MypageScreen(
            state = MypageUiState.PreviewDefault,
            onBackClick = {},
            onEditProfileClick = {},
            onNotificationSettingsClick = {},
            onLocationSharingSettingsClick = {},
            onHelpClick = {},
            onServiceInfoClick = {}
        )
    }
}
