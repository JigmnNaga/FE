
package com.jigmnnaga.ui.screens

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
import com.jigmnnaga.R
import com.jigmnnaga.core.designsystem.theme.AppColors
import com.jigmnnaga.ui.components.CircleButton

@Composable
fun CircleButtonTestScreen() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Monotone.Gray900)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // 1. 뒤로 가기 - 38 x 38
            CircleButton(
                icon = painterResource(R.drawable.chevron_left),
                contentDescription = "뒤로 가기",
                size = 38.dp,
                width = 38.dp,
                height = 38.dp,
                iconSize = 20.dp,
                backgroundColor = AppColors.MainGreen.Green200,
                contentColor = AppColors.MainGreen.Green900
            )

            // 2. 알림 - 38 x 38
            CircleButton(
                icon = painterResource(R.drawable.bell),
                contentDescription = "알림",
                size = 38.dp,
                width = 38.dp,
                height = 38.dp,
                iconSize = 20.dp,
                backgroundColor = AppColors.Monotone.Gray100,
                contentColor = AppColors.MainGreen.Green900
            )

            // 3. 프로필 - 38 x 38
            CircleButton(
                contentDescription = "프로필",
                size = 38.dp,
                width = 38.dp,
                height = 38.dp,
                iconSize = 20.dp,
                backgroundColor = AppColors.MainGreen.Green700,
                contentColor = AppColors.MainGreen.Green200,
                text = "나",
                textSize = 12.sp
            )

            // 4. 일정 - 38 x 28
            CircleButton(
                icon = painterResource(R.drawable.calendar),
                contentDescription = "일정",
                size = 38.dp,
                width = 38.dp,
                height = 28.dp,
                iconSize = 20.dp,
                backgroundColor = AppColors.MainGreen.Green600,
                contentColor = AppColors.MainGreen.Green200,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
            )

            // 5. 장소 - 38 x 28
            CircleButton(
                icon = painterResource(R.drawable.place),
                contentDescription = "장소",
                size = 38.dp,
                width = 38.dp,
                height = 28.dp,
                iconSize = 20.dp,
                backgroundColor = AppColors.MainGreen.Green600,
                contentColor = AppColors.MainGreen.Green200,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
            )

            // 6. 더보기 - 28 x 28
            CircleButton(
                icon = painterResource(R.drawable.more_vertical),
                contentDescription = "더보기",
                size = 28.dp,
                width = 28.dp,
                height = 28.dp,
                iconSize = 14.dp,
                backgroundColor = AppColors.MainGreen.Green200,
                contentColor = AppColors.MainGreen.Green900
            )

            // 7. 전송 비활성화 - 38 x 38
            CircleButton(
                icon = painterResource(R.drawable.send_inactive),
                contentDescription = "전송 비활성화",
                size = 38.dp,
                width = 38.dp,
                height = 38.dp,
                iconSize = 20.dp,
                backgroundColor = AppColors.Monotone.Gray100,
                contentColor = AppColors.Monotone.Gray400,
                enabled = false
            )

            // 8. 전송 활성화 - 38 x 38
            CircleButton(
                icon = painterResource(R.drawable.send_active),
                contentDescription = "전송 활성화",
                size = 38.dp,
                width = 38.dp,
                height = 38.dp,
                iconSize = 20.dp,
                backgroundColor = AppColors.MainGreen.Green700,
                contentColor = AppColors.MainGreen.Green200
            )

            // 9. 추가 - 56 x 56
            CircleButton(
                icon = painterResource(R.drawable.plus),
                contentDescription = "추가",
                size = 56.dp,
                width = 56.dp,
                height = 56.dp,
                iconSize = 24.dp,
                backgroundColor = AppColors.MainGreen.Green900,
                contentColor = AppColors.MainGreen.Green200
            )
        }
    }
}
