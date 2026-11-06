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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jigmnnaga.core.designsystem.theme.AppColors

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
        "CircleButton requires exactly one of icon or text."
    }
    require(icon == null || iconSize != null) {
        "iconSize is required when CircleButton displays an icon."
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
                    color = if (enabled) contentColor else disabledContentColor,
                    fontSize = textSize,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Image(
                    painter = icon!!,
                    contentDescription = null,
                    modifier = Modifier.size(iconSize!!),
                    colorFilter = if (iconTint == null) null else
                        androidx.compose.ui.graphics.ColorFilter.tint(
                            if (enabled) iconTint else disabledContentColor
                        )
                )
            }
        }
    }
}
