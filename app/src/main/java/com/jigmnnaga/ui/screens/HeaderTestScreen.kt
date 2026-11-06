
package com.jigmnnaga.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.jigmnnaga.core.designsystem.theme.AppColors
import com.jigmnnaga.core.designsystem.theme.AppDimens
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme
import com.jigmnnaga.ui.components.MainHeader
import com.jigmnnaga.ui.components.MeetingHeader
import com.jigmnnaga.ui.components.BackHeader

@Composable
fun HeaderTestScreen() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.MainGreen.Green200)
            .verticalScroll(rememberScrollState())
            .padding(AppDimens.Spacing20),
        verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing24)
    ) {

        // MainHeader
        Text(
            text = "MainHeader",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.MainGreen.Green900
        )

        MainHeader(
            onNotificationClick = {},
            onProfileClick = {},
            profileLabel = "나",
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(AppDimens.Spacing8))

        // MeetingHeader
        Text(
            text = "MeetingHeader",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.MainGreen.Green900
        )

        MeetingHeader(
            onBackClick = {},
            onCalendarClick = {},
            onPlaceClick = {},
            onMoreClick = {},
            title = "주말 산행과 맛집 방문을 함께하는 스터디 모임",
            subtitle = "4명 · 텍스트 채팅"
        )

        Spacer(modifier = Modifier.height(AppDimens.Spacing8))

        // BackHeader
        Text(
            text = "BackHeader",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.MainGreen.Green900
        )

        BackHeader(
            title = "약속 만들기",
            onBackClick = {},
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true, widthDp = 320)
@Composable
private fun HeaderTestScreenPreview() {
    JigmnnagaTheme { HeaderTestScreen() }
}
