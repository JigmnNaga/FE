
package com.jigmnnaga.catalog.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.jigmnnaga.core.designsystem.theme.AppColors
import com.jigmnnaga.core.designsystem.theme.AppDimens
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme
import com.jigmnnaga.R
import java.util.Locale

@Composable
fun DesignSystemTestScreen() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(AppDimens.Spacing20),
        verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing24)
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

        ColorGroup(
            title = "Brown",
            colors = listOf("Brown950" to AppColors.Brown.Brown950)
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
                "Spacing12" to AppDimens.Spacing12,
                "Spacing16" to AppDimens.Spacing16,
                "Spacing20" to AppDimens.Spacing20,
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
                "StartButtonWidth" to AppDimens.StartButtonWidth,
                "StartButtonHeight" to AppDimens.StartButtonHeight,
                "ButtonWidth" to AppDimens.ButtonWidth,
                "ButtonHeight" to AppDimens.ButtonHeight
            )
        )

        DimensionGroup(
            title = "Header",
            dimensions = listOf(
                "MainHeaderHeight" to AppDimens.MainHeaderHeight,
                "MeetingHeaderHeight" to AppDimens.MeetingHeaderHeight,
                "BackHeaderHeight" to AppDimens.BackHeaderHeight,
                "HeaderHorizontalPadding" to AppDimens.HeaderHorizontalPadding,
                "HeaderActionSize" to AppDimens.HeaderActionSize,
                "HeaderIconSize" to AppDimens.HeaderIconSize
            )
        )

        Text(
            text = "Typography",
            fontSize = 20.sp,
            color = AppColors.MainGreen.Green900
        )
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)) {
            Text("버튼 / Bold · 16/24 (sans fallback)", style = MaterialTheme.typography.labelLarge)
            Text("헤더 / Bold · 16/20", style = MaterialTheme.typography.titleMedium)
            Text("부제 / Regular · 12/16", style = MaterialTheme.typography.labelSmall)
        }

        val drawableResources = remember {
            R.drawable::class.java.fields.mapNotNull { field ->
                runCatching { field.name to field.getInt(null) }.getOrNull()
            }.filterNot { (name, _) -> name.startsWith("ic_launcher") }
                .sortedBy { it.first }
        }
        DrawableCatalogGroup("Icons", drawableResources.filterNot { it.first.startsWith("logo") || it.first.startsWith("error_") })
        DrawableCatalogGroup("Logos", drawableResources.filter { it.first.startsWith("logo") })
        DrawableCatalogGroup("Error illustrations", drawableResources.filter { it.first.startsWith("error_") })
    }
}

@Preview(showBackground = true, widthDp = 412)
@Composable
private fun DesignSystemTestScreenPreview() {
    JigmnnagaTheme { DesignSystemTestScreen() }
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
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
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
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
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

@Composable
private fun DrawableCatalogGroup(
    title: String,
    resources: List<Pair<String, Int>>
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)) {
        Text(title, fontSize = 16.sp, color = AppColors.Monotone.SoftBlack)
        resources.chunked(4).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
            ) {
                rowItems.forEach { (name, resourceId) ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing4)
                    ) {
                        Image(
                            painter = painterResource(resourceId),
                            contentDescription = name,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = name,
                            fontSize = 9.sp,
                            color = AppColors.Monotone.Gray700,
                            maxLines = 2
                        )
                    }
                }
                repeat(4 - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}
