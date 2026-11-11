package com.jigmnnaga.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = AppColors.MainGreen.Green900,
    onPrimary = AppColors.MainGreen.Green200,
    primaryContainer = AppColors.MainGreen.Green200,
    onPrimaryContainer = AppColors.MainGreen.Green900,
    secondary = AppColors.MainGreen.Green700,
    onSecondary = AppColors.MainGreen.Green200,
    secondaryContainer = AppColors.MainGreen.Green300,
    onSecondaryContainer = AppColors.Monotone.SoftBlack,
    tertiary = AppColors.SubYellow.Yellow600,
    onTertiary = AppColors.Monotone.SoftBlack,
    background = AppColors.MainGreen.Green200,
    onBackground = AppColors.Monotone.SoftBlack,
    surface = AppColors.Monotone.White,
    onSurface = AppColors.Monotone.SoftBlack,
    surfaceVariant = AppColors.Monotone.Gray100,
    onSurfaceVariant = AppColors.Monotone.Gray800,
    error = AppColors.Danger.Red600,
    onError = AppColors.Monotone.White
)

/**
 * 앱 테마. 현재 디자인은 라이트 모드만 지원하며, 시스템 다크 모드나 동적 색상(Material You)과
 * 관계없이 항상 라이트 팔레트를 적용한다.
 *
 * 다크 모드를 지원하게 되면 공통 컴포넌트에 고정된 색상을 [MaterialTheme.colorScheme]의
 * 역할별 색상으로 먼저 바꾼 뒤 다크 팔레트를 추가한다.
 */
@Composable
fun JigmnnagaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}