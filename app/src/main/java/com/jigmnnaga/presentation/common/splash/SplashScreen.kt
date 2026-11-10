
package com.jigmnnaga.presentation.common.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jigmnnaga.R
import com.jigmnnaga.core.designsystem.component.AppButton
import com.jigmnnaga.core.designsystem.component.ButtonVariant
import com.jigmnnaga.core.designsystem.theme.AppColors
import com.jigmnnaga.core.designsystem.theme.AppDimens
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme

@Composable
fun SplashScreen(
    onStartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.MainGreen.Green200)
    ) {
        val isSmallScreen = maxHeight < 700.dp

        val logoSize = if (isSmallScreen) 72.dp else 100.dp
        val logoOffsetX = if (isSmallScreen) 6.dp else 9.dp
        val logoTextWidth = if (isSmallScreen) 112.dp else 148.dp
        val logoTextHeight = if (isSmallScreen) 33.dp else 44.dp

        val logoSpacing = if (isSmallScreen) 10.dp else AppDimens.Spacing16
        val featureSpacing = if (isSmallScreen) 28.dp else 50.dp
        val tagSpacing = if (isSmallScreen) 8.dp else AppDimens.Spacing12
        val buttonBottomPadding = if (isSmallScreen) 55.dp else 100.dp

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = AppDimens.Spacing20),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.logo_mark),
                contentDescription = null,
                modifier = Modifier
                    .size(logoSize)
                    .offset(x = logoOffsetX)
            )

            Spacer(Modifier.height(logoSpacing))

            Image(
                painter = painterResource(R.drawable.logo_text),
                contentDescription = "지금 나가",
                modifier = Modifier.size(
                    width = logoTextWidth,
                    height = logoTextHeight
                )
            )

            Spacer(Modifier.height(if (isSmallScreen) 4.dp else 5.dp))

            Text(
                text = "약속, 이제 묻지 않아도 돼요",
                style = if (isSmallScreen) {
                    MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                } else {
                    MaterialTheme.typography.bodySmall
                },
                color = AppColors.MainGreen.Green900
            )

            Spacer(Modifier.height(featureSpacing))

            Row(
                horizontalArrangement = Arrangement.spacedBy(tagSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SplashFeatureTag(
                    text = "실시간 위치 공유",
                    isSmallScreen = isSmallScreen
                )
                SplashFeatureTag(
                    text = "간편한 일정 조율",
                    isSmallScreen = isSmallScreen
                )
            }

            Spacer(Modifier.height(tagSpacing))

            SplashFeatureTag(
                text = "도착 현황 확인",
                isSmallScreen = isSmallScreen
            )
        }

        AppButton(
            text = "시작하기",
            variant = ButtonVariant.START,
            onClick = onStartClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = buttonBottomPadding)
        )
    }
}

@Composable
private fun SplashFeatureTag(
    text: String,
    isSmallScreen: Boolean
) {
    Box(
        modifier = Modifier
            .background(
                color = AppColors.MainGreen.Green900.copy(alpha = 0.07f),
                shape = CircleShape
            )
            .border(
                width = 1.dp,
                color = AppColors.MainGreen.Green900.copy(alpha = 0.12f),
                shape = CircleShape
            )
            .padding(
                horizontal = if (isSmallScreen) 10.dp else 14.dp,
                vertical = if (isSmallScreen) 4.dp else 5.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = if (isSmallScreen) {
                MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
            } else {
                MaterialTheme.typography.labelSmall
            },
            color = AppColors.MainGreen.Green700
        )
    }
}

@Preview(
    name = "Default Screen",
    showBackground = true,
    widthDp = 412,
    heightDp = 892
)
@Composable
private fun SplashScreenPreview() {
    JigmnnagaTheme {
        SplashScreen(onStartClick = {})
    }
}

@Preview(
    name = "Small Screen",
    showBackground = true,
    widthDp = 360,
    heightDp = 640
)
@Composable
private fun SplashScreenSmallPreview() {
    JigmnnagaTheme {
        SplashScreen(onStartClick = {})
    }
}
