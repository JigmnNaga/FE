package com.jigmnnaga.presentation.mypage.help

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
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

// Figma MYP-006 고유 색상
private val NoticeBannerColor = Color(0xFFEEF3E8)   // 답변 안내 배너 배경
private val FaqAnswerColor = Color(0xFFEEF3E8)      // FAQ 답변 배경 (배너와 동일)
private val BadgeGreenColor = Color(0xFFE1E9D9)     // 아이콘 배지 배경
private val CardShape = RoundedCornerShape(16.dp)
private val BadgeShape = RoundedCornerShape(12.dp)
private val AnswerShape = RoundedCornerShape(12.dp)

private fun TextStyle.sized(size: Int, line: Int, weight: FontWeight): TextStyle =
    copy(fontSize = size.sp, lineHeight = line.sp, fontWeight = weight)

/**
 * MYP-006 도움말 및 문의 화면 순수 UI Composable.
 * FAQ 항목 접기/펼치기 상태는 [expandedStates]로 받고, 변경은 [onItemToggle]로 발행한다.
 * 하단 "문의하기" 버튼은 [onInquiryClick]으로 연결된다.
 *
 * ※ Figma(336-2481, 1041-6533) 헤더에 "위치 공유 설정"으로 표기되어 있으나
 *    이는 Figma 컴포넌트 재사용 시 텍스트 미교체 오류로 판단하여 "도움말 및 문의"로 구현.
 */
@Composable
fun HelpSupportScreen(
    expandedStates: List<Boolean>,
    onBackClick: () -> Unit,
    onItemToggle: (Int) -> Unit,
    onInquiryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.MainGreen.Green200)
            .systemBarsPadding()
    ) {
        BackHeader(
            title = stringResource(R.string.help_support_title),
            onBackClick = onBackClick
        )

        // 스크롤 가능한 본문
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppDimens.Spacing20)
        ) {
            // 답변 안내 배너 (상단 여백 없음)
            NoticeBanner()

            // FAQ 섹션 (배너 아래 16dp 여백)
            Column(
                modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SectionHeader()
                FaqCard(expandedStates = expandedStates, onItemToggle = onItemToggle)
            }
        }

        // 하단 고정 "문의하기" 버튼
        InquiryButton(onClick = onInquiryClick)
    }
}

// ─────────────────────────────────────────
// 답변 안내 배너
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
        FaqIconBadge(iconRes = R.drawable.help, iconSize = 18.dp, badgeSize = 34.dp)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = stringResource(R.string.help_support_notice_title),
                style = MaterialTheme.typography.labelMedium.sized(13, 18, FontWeight.Bold),
                color = AppColors.MainGreen.Green900
            )
            Text(
                text = stringResource(R.string.help_support_notice_description),
                style = MaterialTheme.typography.labelSmall.sized(11, 16, FontWeight.Normal),
                color = AppColors.Monotone.Gray700
            )
        }
    }
}

// ─────────────────────────────────────────
// 섹션 제목
// ─────────────────────────────────────────

@Composable
private fun SectionHeader() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = stringResource(R.string.help_support_faq_section_title),
            style = MaterialTheme.typography.labelMedium.sized(13, 18, FontWeight.Bold),
            color = AppColors.MainGreen.Green900
        )
        Text(
            text = stringResource(R.string.help_support_faq_section_description),
            style = MaterialTheme.typography.labelSmall.sized(11, 16, FontWeight.Normal),
            color = AppColors.Monotone.Gray500
        )
    }
}

// ─────────────────────────────────────────
// FAQ 카드 (전체)
// ─────────────────────────────────────────

@Composable
private fun FaqCard(
    expandedStates: List<Boolean>,
    onItemToggle: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = CardShape, clip = false)
            .background(Color.White, CardShape)
    ) {
        FaqItem.entries.forEachIndexed { index, item ->
            FaqRow(
                item = item,
                expanded = expandedStates.getOrElse(index) { false },
                showDivider = index < FaqItem.entries.size - 1,
                onToggle = { onItemToggle(index) }
            )
        }
    }
}

// ─────────────────────────────────────────
// FAQ 단일 항목
// ─────────────────────────────────────────

@Composable
private fun FaqRow(
    item: FaqItem,
    expanded: Boolean,
    showDivider: Boolean,
    onToggle: () -> Unit
) {
    Column {
        // 질문 행
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 아이콘 배지 (FAQ는 30dp — Figma 기준)
            FaqIconBadge(
                iconRes = item.iconRes,
                iconSize = item.iconSize,
                badgeSize = 30.dp
            )
            Text(
                text = stringResource(item.questionRes),
                style = MaterialTheme.typography.labelMedium.sized(13, 19, FontWeight.Normal),
                color = AppColors.Monotone.SoftBlack,
                modifier = Modifier.weight(1f)
            )
            // 펼침/접힘 화살표
            Icon(
                painter = painterResource(
                    if (expanded) R.drawable.chevron_up else R.drawable.chevron_down
                ),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = AppColors.Monotone.Gray700
            )
        }

        // 답변 (접기/펼치기 애니메이션)
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, bottom = 12.dp)
                    .background(FaqAnswerColor, AnswerShape)
                    .padding(horizontal = 12.dp, vertical = 9.dp)
            ) {
                Text(
                    text = stringResource(item.answerRes),
                    style = MaterialTheme.typography.labelSmall.sized(11, 17, FontWeight.Normal),
                    color = AppColors.Monotone.Gray700
                )
            }
        }

        // 구분선
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

// ─────────────────────────────────────────
// 아이콘 배지
// ─────────────────────────────────────────

@Composable
private fun FaqIconBadge(
    @DrawableRes iconRes: Int,
    iconSize: androidx.compose.ui.unit.Dp,
    badgeSize: androidx.compose.ui.unit.Dp
) {
    Box(
        modifier = Modifier
            .size(badgeSize)
            .background(BadgeGreenColor, BadgeShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = Color.Unspecified
        )
    }
}

// ─────────────────────────────────────────
// 하단 "문의하기" 버튼
// ─────────────────────────────────────────

@Composable
private fun InquiryButton(onClick: () -> Unit) {
    // Figma: Green900 배경, Green200 텍스트, py-20dp, rounded-16, 하단 여백 15dp
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.MainGreen.Green200)
            .padding(horizontal = AppDimens.Spacing20)
            .padding(top = 1.dp, bottom = 15.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.MainGreen.Green900, RoundedCornerShape(16.dp))
                .clickable(onClick = onClick)
                .padding(vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.help_support_inquiry_button),
                style = TextStyle(
                    fontFamily = MaterialTheme.typography.titleMedium.fontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                ),
                color = AppColors.MainGreen.Green200
            )
        }
    }
}

// ─────────────────────────────────────────
// FAQ 데이터
// ─────────────────────────────────────────

/**
 * FAQ 항목 목록. Figma MYP-006의 4개 항목을 순서대로 정의한다.
 * [iconSize]는 배지 내 아이콘 크기 (항목별로 Figma 원본 크기에 맞춤).
 */
private enum class FaqItem(
    @param:DrawableRes @field:DrawableRes val iconRes: Int,
    val iconSize: androidx.compose.ui.unit.Dp,
    val questionRes: Int,
    val answerRes: Int
) {
    ACCOUNT(
        iconRes = R.drawable.profile,
        iconSize = 22.dp,       // Figma: 21.996dp ≈ 22dp
        questionRes = R.string.help_support_faq_q1,
        answerRes = R.string.help_support_faq_a1
    ),
    APPOINTMENT(
        iconRes = R.drawable.appointment_reminder,
        iconSize = 18.dp,
        questionRes = R.string.help_support_faq_q2,
        answerRes = R.string.help_support_faq_a2
    ),
    LOCATION(
        iconRes = R.drawable.location_sharing_settings,
        iconSize = 18.dp,
        questionRes = R.string.help_support_faq_q3,
        answerRes = R.string.help_support_faq_a3
    ),
    NOTIFICATION(
        iconRes = R.drawable.bell,
        iconSize = 18.dp,
        questionRes = R.string.help_support_faq_q4,
        answerRes = R.string.help_support_faq_a4
    )
}

// ─────────────────────────────────────────
// Preview
// ─────────────────────────────────────────

@Preview(showBackground = true, widthDp = 412, heightDp = 892, name = "도움말 - 모두 접힘")
@Composable
private fun HelpSupportScreenCollapsedPreview() {
    JigmnnagaTheme {
        HelpSupportScreen(
            expandedStates = listOf(false, false, false, false),
            onBackClick = {},
            onItemToggle = {},
            onInquiryClick = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892, name = "도움말 - 첫 항목 펼침")
@Composable
private fun HelpSupportScreenFirstExpandedPreview() {
    JigmnnagaTheme {
        HelpSupportScreen(
            expandedStates = listOf(true, false, false, false),
            onBackClick = {},
            onItemToggle = {},
            onInquiryClick = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892, name = "도움말 - 모두 펼침")
@Composable
private fun HelpSupportScreenAllExpandedPreview() {
    JigmnnagaTheme {
        HelpSupportScreen(
            expandedStates = listOf(true, true, true, true),
            onBackClick = {},
            onItemToggle = {},
            onInquiryClick = {}
        )
    }
}
