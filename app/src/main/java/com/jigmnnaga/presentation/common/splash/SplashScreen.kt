
package com.jigmnnaga.presentation.common.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jigmnnaga.core.designsystem.theme.AppColors
import com.jigmnnaga.core.designsystem.theme.AppDimens
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme
import com.jigmnnaga.presentation.common.LogoSection
import kotlinx.coroutines.delay

// 로고 등장 애니메이션 시간 (ms)
private const val LOGO_DURATION_MS = 400
// 로고 등장 완료 후 기능 태그 등장까지 대기 시간 (ms)
private const val LOGO_TO_FEATURES_DELAY_MS = 500L
// 기능 태그 각각의 페이드인 시간 (ms)
private const val FEATURE_DURATION_MS = 300
// 기능 태그 간 등장 간격 (ms)
private const val FEATURE_STAGGER_MS = 120

@Composable
fun SplashScreen(
    modifier: Modifier = Modifier
) {
    // 0 = 초기, 1 = 로고 등장, 2 = 기능 태그 등장
    var phase by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        phase = 1
        delay(LOGO_TO_FEATURES_DELAY_MS)
        phase = 2
    }

    // 로고: 위에서 슬라이드 다운 + 페이드인
    val logoAlpha by animateFloatAsState(
        targetValue = if (phase >= 1) 1f else 0f,
        animationSpec = tween(durationMillis = LOGO_DURATION_MS),
        label = "logoAlpha"
    )
    val logoTranslationY by animateFloatAsState(
        targetValue = if (phase >= 1) 0f else -30f,
        animationSpec = tween(durationMillis = LOGO_DURATION_MS),
        label = "logoTranslationY"
    )

    // 기능 태그 3개: 순서대로 페이드인 (stagger)
    val tag1Alpha by animateFloatAsState(
        targetValue = if (phase >= 2) 1f else 0f,
        animationSpec = tween(durationMillis = FEATURE_DURATION_MS, delayMillis = 0),
        label = "tag1Alpha"
    )
    val tag2Alpha by animateFloatAsState(
        targetValue = if (phase >= 2) 1f else 0f,
        animationSpec = tween(durationMillis = FEATURE_DURATION_MS, delayMillis = FEATURE_STAGGER_MS),
        label = "tag2Alpha"
    )
    val tag3Alpha by animateFloatAsState(
        targetValue = if (phase >= 2) 1f else 0f,
        animationSpec = tween(durationMillis = FEATURE_DURATION_MS, delayMillis = FEATURE_STAGGER_MS * 2),
        label = "tag3Alpha"
    )

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
            val featureSpacing = if (isSmallScreen) 28.dp else 50.dp
            val tagSpacing = if (isSmallScreen) 8.dp else AppDimens.Spacing12

            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = AppDimens.Spacing20),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LogoSection(
                    isSmallScreen = isSmallScreen,
                    modifier = Modifier.graphicsLayer {
                        alpha = logoAlpha
                        translationY = logoTranslationY
                    }
                )

                Spacer(Modifier.height(featureSpacing))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(tagSpacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SplashFeatureTag(
                        text = "실시간 위치 공유",
                        isSmallScreen = isSmallScreen,
                        modifier = Modifier.graphicsLayer { alpha = tag1Alpha }
                    )
                    SplashFeatureTag(
                        text = "간편한 일정 조율",
                        isSmallScreen = isSmallScreen,
                        modifier = Modifier.graphicsLayer { alpha = tag2Alpha }
                    )
                }

                Spacer(Modifier.height(tagSpacing))

                SplashFeatureTag(
                    text = "도착 현황 확인",
                    isSmallScreen = isSmallScreen,
                    modifier = Modifier.graphicsLayer { alpha = tag3Alpha }
                )
            }
        }
    }
}

@Composable
private fun SplashFeatureTag(
    text: String,
    isSmallScreen: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
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
        SplashScreen()
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
        SplashScreen()
    }
}
