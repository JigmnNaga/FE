
package com.jigmnnaga.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.jigmnnaga.core.designsystem.theme.AppColors
import com.jigmnnaga.core.designsystem.theme.AppDimens
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme
import com.jigmnnaga.ui.components.AppButton
import com.jigmnnaga.ui.components.ButtonVariant
import com.jigmnnaga.ui.components.BottomNavigationBar
import com.jigmnnaga.ui.components.BottomNavigationTab
import com.jigmnnaga.ui.components.ChatInputBar
import com.jigmnnaga.ui.components.LeaveMeetingBar

@Composable
fun ComponentTestScreen() {

    var message by remember { mutableStateOf("보낼 메시지 예시") }
    var selectedTab by remember { mutableStateOf(BottomNavigationTab.HOME) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Monotone.Gray900)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AppDimens.Spacing20, vertical = AppDimens.Spacing12),
        verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing20)
    ) {

        Text(
            text = "시각 확인용 컴포넌트 카탈로그 · 자동화 테스트 아님",
            fontSize = 12.sp,
            color = AppColors.Monotone.White
        )

        // Button
        Text(
            text = "Button",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.Monotone.White
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = AppColors.Monotone.Gray900
                )
                .padding(horizontal = 0.dp, vertical = AppDimens.Spacing12),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
        ) {

            AppButton(
                text = "시작하기",
                variant = ButtonVariant.START,
                onClick = {}
            )

            AppButton(
                text = "button",
                variant = ButtonVariant.DEFAULT,
                onClick = {},
                modifier = Modifier.fillMaxWidth()
            )

            AppButton(
                text = "button",
                variant = ButtonVariant.DEFAULT,
                enabled = false,
                onClick = {},
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing20)
            ) {
                AppButton(
                    text = "button",
                    variant = ButtonVariant.SECONDARY,
                    enabled = false,
                    onClick = {},
                    modifier = Modifier.weight(1f)
                )

                AppButton(
                    text = "button",
                    variant = ButtonVariant.PRIMARY,
                    onClick = {},
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(AppDimens.Spacing4))

        // BottomSheet
        Text(
            text = "bottomsheet",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.Monotone.White
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // Bottom navigation - 비선택 상태
            BottomNavigationBar(
                selectedTab = BottomNavigationTab.NONE,
                onHomeClick = {},
                onAddClick = {},
                onScheduleClick = {},
                modifier = Modifier.fillMaxWidth()
            )

            // Bottom navigation - 홈 선택 상태
            BottomNavigationBar(
                selectedTab = selectedTab,
                onHomeClick = { selectedTab = BottomNavigationTab.HOME },
                onAddClick = {},
                onScheduleClick = { selectedTab = BottomNavigationTab.SCHEDULE },
                modifier = Modifier.fillMaxWidth()
            )

            // Chat input - 비활성화 상태
            ChatInputBar(
                value = "",
                onValueChange = {},
                onSendClick = {},
                modifier = Modifier.fillMaxWidth()
            )

            // Chat input - 활성화 상태
            ChatInputBar(
                value = message,
                onValueChange = { message = it },
                onSendClick = { message = "" },
                modifier = Modifier.fillMaxWidth()
            )

            // Leave meeting - 활성화 상태
            LeaveMeetingBar(
                enabled = true,
                text = "지금 나가기",
                onLeaveClick = {},
                modifier = Modifier.fillMaxWidth()
            )

            // Leave meeting - 비활성화 상태
            LeaveMeetingBar(
                enabled = false,
                text = "지금 나가기",
                onLeaveClick = {},
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 412)
@Composable
private fun ComponentTestScreenPreview() {
    JigmnnagaTheme { ComponentTestScreen() }
}
