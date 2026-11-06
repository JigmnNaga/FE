
package com.jigmnnaga.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jigmnnaga.core.designsystem.theme.AppColors
import com.jigmnnaga.core.designsystem.theme.AppDimens
import java.util.Locale

@Composable
fun DesignSystemTestScreen() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {

        Text(
            text = "Design System",
            fontSize = 24.sp,
            color = AppColors.MainGreen.Green900
        )

        // Main Green
        ColorGroup(
            title = "Main / Green",
            colors = listOf(
                "Green900" to AppColors.MainGreen.Green900,
                "Green800" to AppColors.MainGreen.Green800,
                "Green700" to AppColors.MainGreen.Green700,
                "Green600" to AppColors.MainGreen.Green600,
                "Green500" to AppColors.MainGreen.Green500,
                "Green400" to AppColors.MainGreen.Green400,
                "Green300" to AppColors.MainGreen.Green300,
                "Green200" to AppColors.MainGreen.Green200
            )
        )

        // Sub Yellow
        ColorGroup(
            title = "Sub / Yellow",
            colors = listOf(
                "Yellow900" to AppColors.SubYellow.Yellow900,
                "Yellow800" to AppColors.SubYellow.Yellow800,
                "Yellow700" to AppColors.SubYellow.Yellow700,
                "Yellow600" to AppColors.SubYellow.Yellow600,
                "Yellow500" to AppColors.SubYellow.Yellow500,
                "Yellow400" to AppColors.SubYellow.Yellow400,
                "Yellow300" to AppColors.SubYellow.Yellow300,
                "Yellow200" to AppColors.SubYellow.Yellow200
            )
        )

        // Monotone
        ColorGroup(
            title = "Monotone",
            colors = listOf(
                "SoftBlack" to AppColors.Monotone.SoftBlack,
                "Gray900" to AppColors.Monotone.Gray900,
                "Gray800" to AppColors.Monotone.Gray800,
                "Gray700" to AppColors.Monotone.Gray700,
                "Gray600" to AppColors.Monotone.Gray600,
                "Gray500" to AppColors.Monotone.Gray500,
                "Gray400" to AppColors.Monotone.Gray400,
                "Gray300" to AppColors.Monotone.Gray300,
                "Gray200" to AppColors.Monotone.Gray200,
                "Gray100" to AppColors.Monotone.Gray100,
                "White" to AppColors.Monotone.White
            )
        )

        // Danger
        ColorGroup(
            title = "Danger / Red",
            colors = listOf(
                "Red600" to AppColors.Danger.Red600,
                "Red200" to AppColors.Danger.Red200
            )
        )

        // Icon
        ColorGroup(
            title = "Icon",
            colors = listOf(
                "Background" to AppColors.Icon.Background,
                "Yellow" to AppColors.Icon.Yellow
            )
        )

        // Dimensions
        Text(
            text = "Dimensions",
            fontSize = 20.sp,
            color = AppColors.MainGreen.Green900
        )

        DimensionGroup(
            title = "Common Spacing",
            dimensions = listOf(
                "Spacing4" to AppDimens.Spacing4,
                "Spacing8" to AppDimens.Spacing8,
                "Spacing10" to AppDimens.Spacing10,
                "Spacing12" to AppDimens.Spacing12,
                "Spacing14" to AppDimens.Spacing14,
                "Spacing16" to AppDimens.Spacing16,
                "Spacing20" to AppDimens.Spacing20,
                "Spacing21" to AppDimens.Spacing21,
                "Spacing24" to AppDimens.Spacing24,
                "Spacing32" to AppDimens.Spacing32,
                "Spacing40" to AppDimens.Spacing40
            )
        )

        DimensionGroup(
            title = "Component Sizes",
            dimensions = listOf(
                "CircleButtonSmall" to AppDimens.CircleButtonSmall,
                "CircleButtonMedium" to AppDimens.CircleButtonMedium,
                "CircleButtonLarge" to AppDimens.CircleButtonLarge,
                "IconSmall" to AppDimens.IconSmall,
                "IconMedium" to AppDimens.IconMedium,
                "IconLarge" to AppDimens.IconLarge
            )
        )

        DimensionGroup(
            title = "Bottom Navigation",
            dimensions = listOf(
                "BottomNavigationHeight" to AppDimens.BottomNavigationHeight
            )
        )

        DimensionGroup(
            title = "Input",
            dimensions = listOf(
                "InputHeight" to AppDimens.InputHeight,
                "SendButtonSize" to AppDimens.SendButtonSize
            )
        )

        DimensionGroup(
            title = "Main Buttons",
            dimensions = listOf(
                "ButtonHeight" to AppDimens.ButtonHeight,
                "StartButtonHeight" to AppDimens.StartButtonHeight
            )
        )

        DimensionGroup(
            title = "Header",
            dimensions = listOf(
                "MainHeaderHeight" to AppDimens.MainHeaderHeight,
                "MeetingHeaderHeight" to AppDimens.MeetingHeaderHeight,
                "BackHeaderHeight" to AppDimens.BackHeaderHeight
            )
        )
    }
}

@Composable
private fun ColorGroup(
    title: String,
    colors: List<Pair<String, Color>>
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            color = AppColors.Monotone.SoftBlack
        )

        colors.forEach { (name, color) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 52.dp, height = 38.dp)
                        .background(
                            color = color,
                            shape = RoundedCornerShape(6.dp)
                        )
                )

                Column {
                    Text(
                        text = name,
                        fontSize = 13.sp,
                        color = AppColors.Monotone.Gray900
                    )

                    Text(
                        text = String.format(
                            Locale.ROOT,
                            "#%08X",
                            color.toArgb()
                        ),
                        fontSize = 11.sp,
                        color = AppColors.Monotone.Gray500
                    )
                }
            }
        }
    }
}

@Composable
private fun DimensionGroup(
    title: String,
    dimensions: List<Pair<String, Dp>>
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            color = AppColors.Monotone.SoftBlack
        )

        dimensions.forEach { (name, value) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(value)
                        .height(24.dp)
                        .background(
                            color = AppColors.MainGreen.Green700,
                            shape = RoundedCornerShape(4.dp)
                        )
                )

                Text(
                    text = "$name (${value.value}dp)",
                    fontSize = 12.sp,
                    color = AppColors.Monotone.Gray900
                )
            }
        }
    }
}
