package com.jigmnnaga.presentation.common.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jigmnnaga.R
import com.jigmnnaga.core.designsystem.theme.AppColors
import com.jigmnnaga.core.designsystem.theme.AppDimens
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme
import com.jigmnnaga.presentation.common.LogoSection

private val KakaoYellow = Color(0xFFFEE500)

@Composable
fun LoginScreen(
    onKakaoLoginClick: () -> Unit,
    onTermsClick: () -> Unit = {},
    onPrivacyClick: () -> Unit = {},
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.MainGreen.Green200)
    ) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val isSmallScreen = maxHeight < 700.dp

        val buttonBottomPadding = if (isSmallScreen) 40.dp else 50.dp

        // 로고 + 텍스트 (스플래시와 동일한 위치)
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = AppDimens.Spacing20),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LogoSection(isSmallScreen = isSmallScreen)

            // 스플래시의 핵심 기능 태그 3개가 차지하던 공간 (featureSpacing + 태그행 + tagSpacing + 태그)
            Spacer(Modifier.height(if (isSmallScreen) 84.dp else 114.dp))
        }

        // 카카오 버튼 + 약관
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = AppDimens.Spacing20)
                .padding(bottom = buttonBottomPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(50.dp)
        ) {
            Surface(
                onClick = onKakaoLoginClick,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppDimens.ButtonCornerRadius),
                color = KakaoYellow,
                shadowElevation = 0.dp
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 17.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.kakaotalk),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(AppDimens.Spacing12))
                    Text(
                        text = "카카오로 시작하기",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        ),
                        color = AppColors.Monotone.SoftBlack
                    )
                }
            }

            val termsText = buildAnnotatedString {
                append("로그인 시 ")
                pushStringAnnotation(tag = "TERMS", annotation = "terms")
                withStyle(
                    SpanStyle(
                        color = AppColors.MainGreen.Green700,
                        textDecoration = TextDecoration.Underline
                    )
                ) { append("이용약관") }
                pop()
                append("과 ")
                pushStringAnnotation(tag = "PRIVACY", annotation = "privacy")
                withStyle(
                    SpanStyle(
                        color = AppColors.MainGreen.Green700,
                        textDecoration = TextDecoration.Underline
                    )
                ) { append("개인정보처리방침") }
                pop()
                append("에 동의합니다")
            }
            ClickableText(
                text = termsText,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = AppColors.Monotone.Gray500
                ),
                onClick = { offset ->
                    termsText.getStringAnnotations("TERMS", offset, offset)
                        .firstOrNull()?.let { onTermsClick() }
                    termsText.getStringAnnotations("PRIVACY", offset, offset)
                        .firstOrNull()?.let { onPrivacyClick() }
                }
            )
        }
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = AppColors.MainGreen.Green700,
                    strokeWidth = 3.dp
                )
            }
        }
    }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun LoginScreenPreview() {
    JigmnnagaTheme {
        LoginScreen(onKakaoLoginClick = {})
    }
}
