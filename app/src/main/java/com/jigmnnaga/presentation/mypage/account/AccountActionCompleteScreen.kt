package com.jigmnnaga.presentation.mypage.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.jigmnnaga.R
import com.jigmnnaga.core.designsystem.theme.AppColors
import com.jigmnnaga.core.designsystem.theme.AppDimens
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme

/** 계정 작업 완료 화면 타입. */
enum class AccountActionType { LOGOUT, WITHDRAWAL }

/**
 * MYP-002-03 로그아웃/회원탈퇴 완료 화면.
 * 중앙에 체크 아이콘, 제목, 설명을 표시하고 하단에 "지금 나가기" 버튼을 배치한다.
 */
@Composable
fun AccountActionCompleteScreen(
    actionType: AccountActionType,
    onLeaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val titleRes = when (actionType) {
        AccountActionType.LOGOUT -> R.string.logout_complete_title
        AccountActionType.WITHDRAWAL -> R.string.withdrawal_complete_title
    }
    val messageRes = when (actionType) {
        AccountActionType.LOGOUT -> R.string.logout_complete_message
        AccountActionType.WITHDRAWAL -> R.string.withdrawal_complete_message
    }
    val messageColor = when (actionType) {
        AccountActionType.LOGOUT -> AppColors.Monotone.Gray500
        AccountActionType.WITHDRAWAL -> AppColors.MainGreen.Green700
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.MainGreen.Green200)
            .systemBarsPadding()
    ) {
        // 빈 헤더 영역 (Figma: 68dp 높이, 타이틀/뒤로가기 없음)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = AppDimens.Spacing20)
        )

        // 중앙 콘텐츠
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(AppColors.MainGreen.Green700.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.check),
                        contentDescription = null,
                        modifier = Modifier.size(38.dp),
                        tint = Color.Unspecified
                    )
                }
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(titleRes),
                        style = TextStyle(
                            fontFamily = MaterialTheme.typography.titleMedium.fontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            lineHeight = 28.sp
                        ),
                        color = AppColors.Monotone.SoftBlack,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(messageRes),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            lineHeight = 22.sp
                        ),
                        color = messageColor,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // 하단 버튼
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
                    .clickable(onClick = onLeaveClick)
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.account_action_complete_button),
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
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892, name = "로그아웃 완료")
@Composable
private fun LogoutCompletePreview() {
    JigmnnagaTheme {
        AccountActionCompleteScreen(
            actionType = AccountActionType.LOGOUT,
            onLeaveClick = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892, name = "회원탈퇴 완료")
@Composable
private fun WithdrawalCompletePreview() {
    JigmnnagaTheme {
        AccountActionCompleteScreen(
            actionType = AccountActionType.WITHDRAWAL,
            onLeaveClick = {}
        )
    }
}
