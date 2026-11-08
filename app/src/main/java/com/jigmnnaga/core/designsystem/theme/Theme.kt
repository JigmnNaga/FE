package com.jigmnnaga.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
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

// 브랜드 팔레트 매핑. Figma 다크 모드 토큰 없음.
private val DarkColorScheme = darkColorScheme(
    primary = AppColors.MainGreen.Green300,
    onPrimary = AppColors.MainGreen.Green900,
    primaryContainer = AppColors.MainGreen.Green800,
    onPrimaryContainer = AppColors.MainGreen.Green200,
    secondary = AppColors.MainGreen.Green400,
    onSecondary = AppColors.MainGreen.Green900,
    secondaryContainer = AppColors.MainGreen.Green800,
    onSecondaryContainer = AppColors.MainGreen.Green200,
    tertiary = AppColors.SubYellow.Yellow500,
    onTertiary = AppColors.Monotone.SoftBlack,
    background = AppColors.Monotone.SoftBlack,
    onBackground = AppColors.MainGreen.Green200,
    surface = AppColors.Monotone.Gray900,
    onSurface = AppColors.MainGreen.Green200,
    surfaceVariant = AppColors.Monotone.Gray800,
    onSurfaceVariant = AppColors.Monotone.Gray200,
    error = AppColors.Danger.Red200,
    onError = AppColors.Danger.Red600
)

/**
 * Uses the app palette consistently; system dynamic colors are intentionally disabled.
 * [dynamicColor] remains for source compatibility but is intentionally ignored.
 */
@Composable
fun JigmnnagaTheme(
    darkTheme: Boolean = false,
    @Suppress("UNUSED_PARAMETER") dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
