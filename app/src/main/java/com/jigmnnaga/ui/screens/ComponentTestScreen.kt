
package com.jigmnnaga.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.jigmnnaga.core.designsystem.theme.AppColors
import com.jigmnnaga.ui.components.AppButton
import com.jigmnnaga.ui.components.ButtonVariant
import com.jigmnnaga.ui.components.BottomNavigationBar
import com.jigmnnaga.ui.components.ChatInputBar
import com.jigmnnaga.ui.components.LeaveMeetingBar

@Composable
fun ComponentTestScreen() {

    var message by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf("home") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Monotone.Gray900)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {

        // Button
        Text(
            text = "Button",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.MainGreen.Green900
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = AppColors.Monotone.Gray900
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                variant = ButtonVariant.DISABLED,
                onClick = {},
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AppButton(
                    text = "button",
                    variant = ButtonVariant.SECONDARY,
                    onClick = {}
                )

                AppButton(
                    text = "button",
                    variant = ButtonVariant.PRIMARY,
                    onClick = {}
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // BottomSheet
        Text(
            text = "bottomsheet",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.MainGreen.Green900
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // Bottom navigation - 비선택 상태
            BottomNavigationBar(
                selectedTab = "",
                onHomeClick = {},
                onAddClick = {},
                onScheduleClick = {},
                modifier = Modifier.fillMaxWidth()
            )

            // Bottom navigation - 홈 선택 상태
            BottomNavigationBar(
                selectedTab = selectedTab,
                onHomeClick = { selectedTab = "home" },
                onAddClick = {},
                onScheduleClick = { selectedTab = "schedule" },
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
                isEnabled = true,
                text = "지금 나가기",
                onLeaveClick = {},
                modifier = Modifier.fillMaxWidth()
            )

            // Leave meeting - 비활성화 상태
            LeaveMeetingBar(
                isEnabled = false,
                text = "지금 나가기",
                onLeaveClick = {},
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
