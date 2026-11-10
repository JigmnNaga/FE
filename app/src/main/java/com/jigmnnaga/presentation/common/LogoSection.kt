package com.jigmnnaga.presentation.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jigmnnaga.R
import com.jigmnnaga.core.designsystem.theme.AppColors
import com.jigmnnaga.core.designsystem.theme.AppDimens

@Composable
internal fun LogoSection(
    isSmallScreen: Boolean,
    modifier: Modifier = Modifier
) {
    val logoSize = if (isSmallScreen) 72.dp else 100.dp
    val logoOffsetX = if (isSmallScreen) 6.dp else 9.dp
    val logoTextWidth = if (isSmallScreen) 112.dp else 148.dp
    val logoTextHeight = if (isSmallScreen) 33.dp else 44.dp
    val logoSpacing = if (isSmallScreen) 10.dp else AppDimens.Spacing16

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.logo_mark),
            contentDescription = null,
            modifier = Modifier
                .size(logoSize)
                .offset(x = logoOffsetX)
        )

        Spacer(Modifier.height(logoSpacing))

        Image(
            painter = painterResource(R.drawable.logo_text),
            contentDescription = "지금 나가",
            modifier = Modifier.size(width = logoTextWidth, height = logoTextHeight)
        )

        Spacer(Modifier.height(if (isSmallScreen) 4.dp else 5.dp))

        Text(
            text = "약속, 이제 묻지 않아도 돼요",
            style = if (isSmallScreen) {
                MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
            } else {
                MaterialTheme.typography.bodySmall
            },
            color = AppColors.MainGreen.Green900
        )
    }
}
