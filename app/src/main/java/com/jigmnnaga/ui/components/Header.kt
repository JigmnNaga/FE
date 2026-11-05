
package com.jigmnnaga.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jigmnnaga.R
import com.jigmnnaga.core.designsystem.theme.AppColors

@Composable
fun MainHeader(
    onNotificationClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.logo_mark),
            contentDescription = "지금 나가 로고",
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(4.dp))

        Image(
            painter = painterResource(R.drawable.logo_text),
            contentDescription = "지금 나가",
            modifier = Modifier.size(
                width = 52.dp,
                height = 18.dp
            )
        )

        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    AppColors.Monotone.Gray100,
                    CircleShape
                )
                .clickable { onNotificationClick() },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.bell),
                contentDescription = "알림",
                modifier = Modifier.size(14.dp),
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                    AppColors.MainGreen.Green900
                )
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    AppColors.MainGreen.Green700,
                    CircleShape
                )
                .clickable { onProfileClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "나",
                color = AppColors.MainGreen.Green200,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun MeetingHeader(
    onBackClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onPlaceClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "스터디 모임",
    subtitle: String = "4명 · 텍스트 채팅"
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.chevron_left),
            contentDescription = "뒤로가기",
            modifier = Modifier
                .size(28.dp)
                .clickable { onBackClick() }
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.MainGreen.Green900
            )

            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = AppColors.Monotone.Gray500
            )
        }

        Image(
            painter = painterResource(R.drawable.calendar),
            contentDescription = "일정",
            modifier = Modifier
                .size(24.dp)
                .clickable { onCalendarClick() }
        )

        Image(
            painter = painterResource(R.drawable.place),
            contentDescription = "장소",
            modifier = Modifier
                .size(24.dp)
                .clickable { onPlaceClick() }
        )

        Image(
            painter = painterResource(R.drawable.more_vertical),
            contentDescription = "더보기",
            modifier = Modifier
                .size(24.dp)
                .clickable { onMoreClick() }
        )
    }
}

@Composable
fun BackHeader(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.chevron_left),
            contentDescription = "뒤로가기",
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(28.dp)
                .clickable { onBackClick() }
        )

        Text(
            text = title,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.MainGreen.Green900
        )
    }
}
