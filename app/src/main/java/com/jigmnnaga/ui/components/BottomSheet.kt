
package com.jigmnnaga.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import com.jigmnnaga.R
import com.jigmnnaga.core.designsystem.theme.AppColors

@Composable
fun BottomNavigationBar(
    selectedTab: String,
    onHomeClick: () -> Unit,
    onAddClick: () -> Unit,
    onScheduleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(AppColors.MainGreen.Green200)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 68.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable(onClick = onHomeClick)
            ) {
                Image(
                    painter = painterResource(R.drawable.home),
                    contentDescription = "홈",
                    modifier = Modifier.size(22.dp),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                        if (selectedTab == "home") {
                            AppColors.MainGreen.Green900
                        } else {
                            AppColors.Monotone.Gray400
                        }
                    )
                )

                Text(
                    text = "홈",
                    fontSize = 10.sp,
                    color = if (selectedTab == "home") {
                        AppColors.MainGreen.Green900
                    } else {
                        AppColors.Monotone.Gray400
                    }
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable(onClick = onScheduleClick)
            ) {
                Image(
                    painter = painterResource(R.drawable.calendar),
                    contentDescription = "일정",
                    modifier = Modifier.size(22.dp),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                        if (selectedTab == "schedule") {
                            AppColors.MainGreen.Green900
                        } else {
                            AppColors.Monotone.Gray400
                        }
                    )
                )

                Text(
                    text = "일정",
                    fontSize = 10.sp,
                    color = if (selectedTab == "schedule") {
                        AppColors.MainGreen.Green900
                    } else {
                        AppColors.Monotone.Gray400
                    }
                )
            }
        }

        Surface(
            onClick = onAddClick,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(56.dp),
            shape = CircleShape,
            color = AppColors.MainGreen.Green900,
            shadowElevation = 8.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(R.drawable.plus),
                    contentDescription = "약속 추가",
                    modifier = Modifier.size(24.dp),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                        AppColors.MainGreen.Green200
                    )
                )
            }
        }
    }
}

@Composable
fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(AppColors.MainGreen.Green200)
            .padding(horizontal = 20.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .height(50.dp)
                .background(Color.White, CircleShape)
                .padding(horizontal = 14.dp),
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 14.sp,
                color = AppColors.Monotone.Gray900
            ),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = "메시지를 입력하세요...",
                            fontSize = 14.sp,
                            color = AppColors.Monotone.Gray400
                        )
                    }
                    innerTextField()
                }
            }
        )

        Box(
            modifier = Modifier
                .padding(start = 10.dp)
                .size(50.dp)
                .background(
                    color = if (value.isNotBlank()) {
                        AppColors.MainGreen.Green700
                    } else {
                        AppColors.Monotone.Gray400
                    },
                    shape = CircleShape
                )
                .clickable(
                    enabled = value.isNotBlank(),
                    onClick = onSendClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(
                    if (value.isNotBlank()) {
                        R.drawable.send_active
                    } else {
                        R.drawable.send_inactive
                    }
                ),
                contentDescription = "메시지 전송",
                modifier = Modifier.size(22.dp),
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White)
            )
        }
    }
}

@Composable
fun LeaveMeetingBar(
    isEnabled: Boolean,
    onLeaveClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = "지금 나가기"
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(AppColors.MainGreen.Green200)
            .padding(horizontal = 21.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        AppButton(
            text = text,
            variant = if (isEnabled) {
                ButtonVariant.DEFAULT
            } else {
                ButtonVariant.DISABLED
            },
            onClick = onLeaveClick,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
