
package com.jigmnnaga.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import com.jigmnnaga.R
import com.jigmnnaga.core.designsystem.theme.AppColors
import com.jigmnnaga.ui.components.BottomNavigationBar
import com.jigmnnaga.ui.components.MainHeader

@Composable
fun HomeScreen(
    onAddClick: () -> Unit = {},
    onScheduleClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf("home") }
    var selectedAppointmentTab by remember { mutableStateOf("upcoming") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.MainGreen.Green200)
            .statusBarsPadding()
    ) {
        MainHeader(
            onNotificationClick = onNotificationClick,
            onProfileClick = onProfileClick,
            modifier = Modifier.padding(
                start = 21.dp,
                end = 21.dp,
                top = 8.dp
            )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 21.dp)
                .padding(top = 20.dp)
                .height(32.27.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.Top
        ) {
            AppointmentTab(
                title = "다가오는 약속",
                selected = selectedAppointmentTab == "upcoming",
                onClick = { selectedAppointmentTab = "upcoming" }
            )

            AppointmentTab(
                title = "지난 약속",
                selected = selectedAppointmentTab == "past",
                onClick = { selectedAppointmentTab = "past" }
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            EmptyAppointmentState(
                message = if (selectedAppointmentTab == "upcoming") {
                    "약속이 없어요"
                } else {
                    "지난 약속이 없어요"
                },
                description = if (selectedAppointmentTab == "upcoming") {
                    "지금 약속을 만들어 보세요."
                } else {
                    "약속이 끝나면 이곳에 기록돼요."
                }
            )
        }

        BottomNavigationBar(
            selectedTab = selectedTab,
            onHomeClick = {
                selectedTab = "home"
            },
            onAddClick = onAddClick,
            onScheduleClick = {
                selectedTab = "schedule"
                onScheduleClick()
            }
        )
    }
}

@Composable
private fun AppointmentTab(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(IntrinsicSize.Max)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) {
                AppColors.MainGreen.Green900
            } else {
                AppColors.Monotone.Gray400
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(
                    if (selected) {
                        AppColors.MainGreen.Green900
                    } else {
                        Color.Transparent
                    }
                )
        )
    }
}

@Composable
private fun EmptyAppointmentState(
    message: String,
    description: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = Color(0xFFE5E8DD),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.close),
                contentDescription = "약속 없음",
                modifier = Modifier.size(22.dp),
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                    AppColors.MainGreen.Green900
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = message,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.MainGreen.Green900
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = description,
            fontSize = 11.sp,
            color = AppColors.Monotone.Gray500
        )
    }
}
