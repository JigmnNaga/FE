
package com.jigmnnaga.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jigmnnaga.core.designsystem.theme.AppColors

enum class ButtonVariant {
    START,
    DEFAULT,
    DISABLED,
    SECONDARY,
    PRIMARY
}

@Composable
fun AppButton(
    text: String,
    variant: ButtonVariant = ButtonVariant.DEFAULT,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (variant) {
        ButtonVariant.START,
        ButtonVariant.DEFAULT,
        ButtonVariant.PRIMARY -> AppColors.MainGreen.Green900

        ButtonVariant.DISABLED,
        ButtonVariant.SECONDARY -> AppColors.Monotone.Gray400
    }

    val buttonModifier = when (variant) {
        ButtonVariant.START ->
            modifier.width(165.dp).height(55.33.dp)

        ButtonVariant.SECONDARY,
        ButtonVariant.PRIMARY ->
            modifier.width(175.dp).height(64.dp)

        ButtonVariant.DEFAULT,
        ButtonVariant.DISABLED ->
            modifier.fillMaxWidth().height(64.dp)
    }

    Box(
        modifier = buttonModifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                enabled = variant != ButtonVariant.DISABLED,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                color = AppColors.MainGreen.Green200,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 24.sp
            )

            if (variant == ButtonVariant.START) {
                Text(
                    text = "  →",
                    color = AppColors.MainGreen.Green200,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
