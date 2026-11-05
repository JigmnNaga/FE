
package com.jigmnnaga.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jigmnnaga.R
import com.jigmnnaga.core.designsystem.theme.AppColors

// 1. 하단 내비게이션 바
@Composable
fun BottomNavigationBar(
    selectedTab: String,
    onHomeClick: () -> Unit,
    onAddClick: () -> Unit,
    onScheduleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(AppColors.MainGreen.Green200)
            .padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.clickable { onHomeClick() },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(R.drawable.home),
                contentDescription = "홈",
                modifier = Modifier.size(22.dp),
                tint = if (selectedTab == "home") {
                    AppColors.MainGreen.Green900
                } else {
                    AppColors.Monotone.Gray400
                }
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

        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    AppColors.MainGreen.Green900,
                    CircleShape
                )
                .clickable { onAddClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.plus),
                contentDescription = "추가",
                modifier = Modifier.size(24.dp),
                tint = AppColors.MainGreen.Green200
            )
        }

        Column(
            modifier = Modifier.clickable { onScheduleClick() },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(R.drawable.calendar),
                contentDescription = "일정",
                modifier = Modifier.size(22.dp),
                tint = if (selectedTab == "schedule") {
                    AppColors.MainGreen.Green900
                } else {
                    AppColors.Monotone.Gray400
                }
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
}

// 2. 채팅 입력 바
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
        Box(
            modifier = Modifier
                .weight(1f)
                .height(50.dp)
                .background(
                    AppColors.Monotone.White,
                    CircleShape
                )
                .border(
                    1.dp,
                    AppColors.Monotone.Gray100,
                    CircleShape
                )
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    color = AppColors.Monotone.Gray800
                ),
                singleLine = true,
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                text = "메시지를 입력하세요...",
                                color = AppColors.Monotone.Gray400,
                                fontSize = 14.sp
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Box(
            modifier = Modifier
                .size(50.dp)
                .background(
                    if (value.isNotBlank()) {
                        AppColors.MainGreen.Green700
                    } else {
                        AppColors.Monotone.Gray400
                    },
                    CircleShape
                )
                .clickable(enabled = value.isNotBlank()) {
                    onSendClick()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(
                    if (value.isNotBlank()) {
                        R.drawable.send_active
                    } else {
                        R.drawable.send_inactive
                    }
                ),
                contentDescription = "메시지 전송",
                modifier = Modifier.size(22.dp),
                tint = Color.White
            )
        }
    }
}

// 3. 지금 나가기 바
@Composable
fun LeaveMeetingBar(
    isEnabled: Boolean,
    onLeaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(AppColors.MainGreen.Green200)
            .padding(
                horizontal = 21.dp,
                vertical = 8.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(
                    color = if (isEnabled) {
                        AppColors.MainGreen.Green900
                    } else {
                        AppColors.Monotone.Gray400
                    },
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable(enabled = isEnabled) {
                    onLeaveClick()
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "지금 나가기",
                color = AppColors.MainGreen.Green200,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
