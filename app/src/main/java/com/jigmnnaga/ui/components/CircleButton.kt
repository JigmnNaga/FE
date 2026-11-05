
package com.jigmnnaga.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(size),
        shape = CircleShape,
        color = backgroundColor
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            if (text != null) {
                Text(
                    text = text,
                    color = contentColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            } else if (icon != null) {
                Image(
                    painter = icon,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(iconSize),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                        contentColor
                    )
                )
            }
        }
    }
}
