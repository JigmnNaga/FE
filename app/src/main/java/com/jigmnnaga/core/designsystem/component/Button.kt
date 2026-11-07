package com.jigmnnaga.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.jigmnnaga.core.designsystem.theme.AppColors
import com.jigmnnaga.core.designsystem.theme.AppDimens
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme

enum class ButtonVariant {
    START,
    DEFAULT,
    SECONDARY,
    PRIMARY
}

@Composable
fun AppButton(
    text: String,
    variant: ButtonVariant = ButtonVariant.DEFAULT,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = if (variant == ButtonVariant.START) {
        CircleShape
    } else {
        RoundedCornerShape(AppDimens.ButtonCornerRadius)
    }
    val backgroundColor = when {
        !enabled -> AppColors.Monotone.Gray400
        variant == ButtonVariant.SECONDARY -> AppColors.Monotone.Gray400
        else -> AppColors.MainGreen.Green900
    }
    val sizeModifier = when (variant) {
        ButtonVariant.START -> modifier
            .width(AppDimens.StartButtonWidth)
            .heightIn(min = AppDimens.StartButtonHeight)
        ButtonVariant.SECONDARY, ButtonVariant.PRIMARY -> modifier
            .width(AppDimens.ButtonWidth)
            .heightIn(min = AppDimens.ButtonHeight)
        ButtonVariant.DEFAULT -> modifier
            .fillMaxWidth()
            .heightIn(min = AppDimens.ButtonHeight)
    }

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = sizeModifier,
        shape = shape,
        color = backgroundColor,
        contentColor = AppColors.MainGreen.Green200
    ) {
        Box(
            modifier = Modifier
                .defaultMinSize(
                    minHeight = if (variant == ButtonVariant.START) {
                        AppDimens.StartButtonHeight
                    } else {
                        AppDimens.ButtonHeight
                    }
                )
                .padding(horizontal = AppDimens.Spacing16, vertical = AppDimens.Spacing12),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = if (variant == ButtonVariant.START) 1 else 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (variant == ButtonVariant.START) {
                    androidx.compose.foundation.layout.Spacer(
                        modifier = Modifier.width(AppDimens.StartButtonArrowGap)
                    )
                    StartArrowIcon()
                }
            }
        }
    }
}

@Composable
private fun StartArrowIcon() {
    Canvas(modifier = Modifier.size(AppDimens.StartButtonArrowSize)) {
        val strokeWidth = 1.7.dp.toPx()
        val path = Path().apply {
            moveTo(size.width * 0.15f, size.height * 0.5f)
            lineTo(size.width * 0.82f, size.height * 0.5f)
            moveTo(size.width * 0.55f, size.height * 0.22f)
            lineTo(size.width * 0.82f, size.height * 0.5f)
            lineTo(size.width * 0.55f, size.height * 0.78f)
        }
        drawPath(path, AppColors.MainGreen.Green200, style = Stroke(strokeWidth))
    }
}

@Preview(showBackground = true, widthDp = 412)
@Composable
private fun AppButtonPreview() {
    JigmnnagaTheme {
        Column(modifier = Modifier.padding(AppDimens.Spacing20)) {
            AppButton(text = "시작하기", variant = ButtonVariant.START, onClick = {})
            AppButton(text = "기본 버튼", variant = ButtonVariant.DEFAULT, onClick = {})
            AppButton(text = "비활성화", variant = ButtonVariant.DEFAULT, enabled = false, onClick = {})
            AppButton(text = "PRIMARY", variant = ButtonVariant.PRIMARY, onClick = {})
            AppButton(text = "SECONDARY", variant = ButtonVariant.SECONDARY, onClick = {})
        }
    }
}
