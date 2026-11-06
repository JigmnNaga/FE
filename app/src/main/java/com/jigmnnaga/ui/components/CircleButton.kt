
package com.jigmnnaga.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import com.jigmnnaga.core.designsystem.theme.AppColors

@Composable
fun CircleButton(
    icon: Painter? = null,
    contentDescription: String,
    onClick: () -> Unit = {},
    size: Dp,
    iconSize: Dp,
    backgroundColor: Color = AppColors.MainGreen.Green900,
    contentColor: Color = AppColors.MainGreen.Green200,
    enabled: Boolean = true,
    text: String? = null,
    modifier: Modifier = Modifier,
    width: Dp = size,
    height: Dp = size,
    shape: Shape = CircleShape,
    textSize: TextUnit = 16.sp
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .width(width)
            .height(height),
        shape = shape,
        color = backgroundColor
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            if (text != null) {
                Text(
                    text = text,
                    color = contentColor,
                    fontSize = textSize,
                    fontWeight = FontWeight.Bold
                )
            } else if (icon != null) {
                Image(
                    painter = icon,
                    contentDescription = contentDescription,
                    modifier = Modifier
                        .width(iconSize)
                        .height(iconSize),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                        contentColor
                    )
                )
            }
        }
    }
}
