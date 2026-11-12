package com.jigmnnaga.core.designsystem.component

import androidx.compose.foundation.Image
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jigmnnaga.R
import com.jigmnnaga.core.designsystem.theme.AppColors
import com.jigmnnaga.core.designsystem.theme.AppDimens
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme
import kotlin.math.ceil

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
    // SECONDARY는 활성 상태가 이미 회색이라, 비활성은 더 옅은 배경과 글자색으로 구분한다.
    val isDisabledSecondary = !enabled && variant == ButtonVariant.SECONDARY
    val backgroundColor = when {
        isDisabledSecondary -> AppColors.Monotone.Gray100
        !enabled -> AppColors.Monotone.Gray400
        variant == ButtonVariant.SECONDARY -> AppColors.Monotone.Gray400
        else -> AppColors.MainGreen.Green900
    }
    val textColor = if (isDisabledSecondary) {
        AppColors.Monotone.Gray400
    } else {
        AppColors.MainGreen.Green200
    }
    val startTextWidth = if (variant == ButtonVariant.START) {
        val textLayout = rememberTextMeasurer().measure(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1
        )
        with(LocalDensity.current) {
            val measuredWidth =
                textLayout.size.width.toDp() +
                        AppDimens.Spacing40 +
                        AppDimens.StartButtonEndPadding +
                        AppDimens.StartButtonArrowGap +
                        AppDimens.StartButtonArrowSize
            ceil(measuredWidth.toPx()).toInt().toDp().coerceAtLeast(AppDimens.StartButtonWidth)
        }
    } else {
        null
    }
    val sizeModifier = when (variant) {
        ButtonVariant.START -> modifier
            .width(startTextWidth!!)
            .heightIn(min = AppDimens.StartButtonHeight)
        ButtonVariant.SECONDARY, ButtonVariant.PRIMARY -> modifier
            .width(AppDimens.ButtonWidth)
            .heightIn(min = AppDimens.ButtonHeight)
        ButtonVariant.DEFAULT -> modifier
            .fillMaxWidth()
            .heightIn(min = AppDimens.ButtonHeight)
    }
    val verticalPadding = if (variant == ButtonVariant.START) {
        AppDimens.Spacing16
    } else {
        AppDimens.Spacing12
    }

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = sizeModifier,
        shape = shape,
        color = backgroundColor,
        contentColor = textColor
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
                .padding(
                    start = if (variant == ButtonVariant.START) {
                        AppDimens.Spacing40
                    } else {
                        AppDimens.Spacing16
                    },
                    end = if (variant == ButtonVariant.START) {
                        AppDimens.StartButtonEndPadding
                    } else {
                        AppDimens.Spacing16
                    },
                    top = verticalPadding,
                    bottom = verticalPadding
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = text,
                    modifier = if (variant == ButtonVariant.START) {
                        Modifier.weight(1f, fill = false)
                    } else {
                        Modifier
                    },
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

// Figma 시작하기 버튼의 Icon(18x18 SVG)을 그대로 옮긴 arrow_right 드로어블을 쓴다.
@Composable
private fun StartArrowIcon() {
    Image(
        painter = painterResource(R.drawable.arrow_right),
        contentDescription = null,
        modifier = Modifier
            .size(AppDimens.StartButtonArrowSize)
            .testTag("start-button-arrow")
    )
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
            AppButton(
                text = "SECONDARY 비활성",
                variant = ButtonVariant.SECONDARY,
                enabled = false,
                onClick = {}
            )
        }
    }
}