package com.jigmnnaga.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.jigmnnaga.R
import com.jigmnnaga.core.designsystem.theme.AppColors
import com.jigmnnaga.core.designsystem.theme.AppDimens
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme

@Composable
fun CircleButton(
    contentDescription: String,
    onClick: () -> Unit,
    width: Dp,
    height: Dp = width,
    icon: Painter? = null,
    iconSize: Dp? = null,
    text: String? = null,
    textSize: TextUnit = 16.sp,
    backgroundColor: Color = AppColors.MainGreen.Green900,
    contentColor: Color = AppColors.MainGreen.Green200,
    iconTint: Color? = contentColor,
    disabledBackgroundColor: Color = AppColors.Monotone.Gray100,
    disabledContentColor: Color = AppColors.Monotone.Gray400,
    enabled: Boolean = true,
    shape: Shape = CircleShape,
    modifier: Modifier = Modifier
) {
    require((icon != null) xor (text != null)) {
        "icon과 text 중 하나만 전달해야 합니다."
    }
    require(icon == null || iconSize != null) {
        "icon을 사용할 경우 iconSize도 함께 전달해야 합니다."
    }

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .size(width = width, height = height)
            .semantics(mergeDescendants = true) {
                this.contentDescription = contentDescription
            },
        shape = shape,
        color = if (enabled) backgroundColor else disabledBackgroundColor,
        contentColor = if (enabled) contentColor else disabledContentColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (text != null) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = textSize),
                    color = if (enabled) contentColor else disabledContentColor
                )
            } else {
                Image(
                    painter = icon!!,
                    contentDescription = null,
                    modifier = Modifier.size(iconSize!!),
                    colorFilter = if (iconTint == null) null else
                        ColorFilter.tint(if (enabled) iconTint else disabledContentColor)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CircleButtonPreview() {
    JigmnnagaTheme {
        Row(
            modifier = Modifier.padding(AppDimens.Spacing16),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
        ) {
            CircleButton(
                contentDescription = "알림",
                onClick = {},
                width = AppDimens.HeaderActionSize,
                icon = painterResource(R.drawable.bell),
                iconSize = AppDimens.HeaderIconSize,
                backgroundColor = AppColors.Monotone.Gray100,
                contentColor = AppColors.MainGreen.Green700
            )
            CircleButton(
                contentDescription = "텍스트",
                onClick = {},
                width = AppDimens.HeaderActionSize,
                text = "나"
            )
            CircleButton(
                contentDescription = "비활성화",
                onClick = {},
                width = AppDimens.HeaderActionSize,
                icon = painterResource(R.drawable.bell),
                iconSize = AppDimens.HeaderIconSize,
                enabled = false
            )
        }
    }
}
