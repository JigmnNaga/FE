
package com.jigmnnaga.catalog.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.jigmnnaga.R
import com.jigmnnaga.core.designsystem.theme.AppColors
import com.jigmnnaga.core.designsystem.theme.AppDimens
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme
import com.jigmnnaga.core.designsystem.component.CircleButton

@Composable
fun CircleButtonTestScreen() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Monotone.Gray900)
            .padding(AppDimens.Spacing12),
        verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
    ) {

        Text(
            text = "Circle Button",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.MainGreen.Green200
        )

        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(vertical = AppDimens.Spacing8),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // 비주얼 확인용. Figma 측정 전 임시 치수.
            // 1. 뒤로 가기
            CircleButton(
                icon = painterResource(R.drawable.chevron_left),
                contentDescription = "뒤로 가기",
                onClick = {},
                width = AppDimens.CircleButtonMedium,
                height = AppDimens.CircleButtonMedium,
                iconSize = AppDimens.HeaderIconSize,
                backgroundColor = AppColors.MainGreen.Green200,
                contentColor = AppColors.Monotone.Gray700
            )

            // 2. 알림
            CircleButton(
                icon = painterResource(R.drawable.bell),
                contentDescription = "알림",
                onClick = {},
                width = AppDimens.CircleButtonMedium,
                height = AppDimens.CircleButtonMedium,
                iconSize = AppDimens.HeaderIconSize,
                backgroundColor = AppColors.Monotone.Gray100,
                contentColor = AppColors.MainGreen.Green700
            )

            // 3. 프로필
            CircleButton(
                contentDescription = "프로필",
                onClick = {},
                width = AppDimens.CircleButtonMedium,
                height = AppDimens.CircleButtonMedium,
                iconSize = 20.dp,
                backgroundColor = AppColors.MainGreen.Green700,
                contentColor = AppColors.MainGreen.Green200,
                text = "나",
                textSize = 14.sp
            )

            // 4. 일정
            CircleButton(
                icon = painterResource(R.drawable.calendar),
                contentDescription = "일정",
                onClick = {},
                width = AppDimens.CircleButtonMedium,
                height = AppDimens.CompactHeaderActionHeight,
                iconSize = AppDimens.HeaderIconSize,
                backgroundColor = AppColors.MainGreen.Green600,
                contentColor = AppColors.MainGreen.Green200,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(AppDimens.CompactHeaderActionRadius)
            )

            // 5. 장소
            CircleButton(
                icon = painterResource(R.drawable.place),
                contentDescription = "장소",
                onClick = {},
                width = AppDimens.CircleButtonMedium,
                height = AppDimens.CompactHeaderActionHeight,
                iconSize = AppDimens.HeaderIconSize,
                backgroundColor = AppColors.MainGreen.Green600,
                contentColor = AppColors.MainGreen.Green200,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(AppDimens.CompactHeaderActionRadius)
            )

            // 6. 더보기
            CircleButton(
                icon = painterResource(R.drawable.more_vertical),
                contentDescription = "더보기",
                onClick = {},
                width = AppDimens.CircleButtonSmall,
                height = AppDimens.CircleButtonSmall,
                iconSize = AppDimens.HeaderIconSize,
                backgroundColor = AppColors.MainGreen.Green200,
                contentColor = AppColors.Monotone.Gray500
            )

            // 7. 전송 비활성화
            CircleButton(
                icon = painterResource(R.drawable.send_inactive),
                contentDescription = "전송 비활성화",
                onClick = {},
                width = AppDimens.CircleButtonMedium,
                height = AppDimens.CircleButtonMedium,
                iconSize = AppDimens.HeaderIconSize,
                backgroundColor = AppColors.Monotone.Gray100,
                contentColor = AppColors.Monotone.Gray400,
                enabled = false
            )

            // 8. 전송 활성화
            CircleButton(
                icon = painterResource(R.drawable.send_active),
                contentDescription = "전송 활성화",
                onClick = {},
                width = AppDimens.CircleButtonMedium,
                height = AppDimens.CircleButtonMedium,
                iconSize = AppDimens.HeaderIconSize,
                backgroundColor = AppColors.MainGreen.Green700,
                contentColor = AppColors.MainGreen.Green200
            )

            // 9. 추가
            CircleButton(
                icon = painterResource(R.drawable.plus),
                contentDescription = "추가",
                onClick = {},
                width = AppDimens.CircleButtonLarge,
                height = AppDimens.CircleButtonLarge,
                iconSize = AppDimens.IconLarge,
                backgroundColor = AppColors.MainGreen.Green900,
                contentColor = AppColors.MainGreen.Green200
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 412)
@Composable
private fun CircleButtonTestScreenPreview() {
    JigmnnagaTheme { CircleButtonTestScreen() }
}
