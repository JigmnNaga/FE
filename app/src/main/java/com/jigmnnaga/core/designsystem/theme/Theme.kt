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
 * Applies the light app palette regardless of the system appearance.
 * [darkTheme] and [dynamicColor] remain for source compatibility and are intentionally ignored.
 */
@Composable
fun JigmnnagaTheme(
    darkTheme: Boolean = false,
    @Suppress("UNUSED_PARAMETER") dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
