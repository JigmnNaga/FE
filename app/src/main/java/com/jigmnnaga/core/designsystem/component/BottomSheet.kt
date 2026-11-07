package com.jigmnnaga.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.jigmnnaga.R
import com.jigmnnaga.core.designsystem.theme.AppColors
import com.jigmnnaga.core.designsystem.theme.AppDimens
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme

enum class BottomNavigationTab {
    NONE,
    HOME,
    SCHEDULE
}

// 네비게이션 바 inset은 부모 Screen 담당. 이 컴포넌트는 80dp 바 영역만 담당.
@Composable
fun BottomNavigationBar(
    selectedTab: BottomNavigationTab,
    onHomeClick: () -> Unit,
    onAddClick: () -> Unit,
    onScheduleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(AppDimens.BottomNavigationHeight)
            .background(AppColors.MainGreen.Green200)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = AppDimens.BottomNavigationHorizontalPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavigationItem(
                label = "홈",
                icon = R.drawable.home,
                selected = selectedTab == BottomNavigationTab.HOME,
                onClick = onHomeClick
            )
            BottomNavigationItem(
                label = "일정",
                icon = R.drawable.calendar_gray,
                preserveIconColors = true,
                selected = selectedTab == BottomNavigationTab.SCHEDULE,
                onClick = onScheduleClick
            )
        }

        CircleButton(
            contentDescription = "약속 추가",
            onClick = onAddClick,
            width = AppDimens.CircleButtonLarge,
            icon = painterResource(R.drawable.plus),
            iconSize = AppDimens.IconLarge,
            backgroundColor = AppColors.MainGreen.Green900,
            contentColor = AppColors.MainGreen.Green200,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

// 기존 HomeScreen 호환용 오버로드. 신규 코드는 enum API 사용.
@Composable
fun BottomNavigationBar(
    selectedTab: String,
    onHomeClick: () -> Unit,
    onAddClick: () -> Unit,
    onScheduleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tab = when (selectedTab.lowercase()) {
        "home" -> BottomNavigationTab.HOME
        "schedule" -> BottomNavigationTab.SCHEDULE
        else -> BottomNavigationTab.NONE
    }
    BottomNavigationBar(tab, onHomeClick, onAddClick, onScheduleClick, modifier)
}

@Composable
private fun BottomNavigationItem(
    label: String,
    icon: Int,
    preserveIconColors: Boolean = false,
    selected: Boolean,
    onClick: () -> Unit
) {
    val tint = if (selected) AppColors.Brown.Brown950 else AppColors.Monotone.Gray400
    Column(
        modifier = Modifier
            .selectable(selected = selected, onClick = onClick, role = Role.Tab)
            .semantics { this.selected = selected },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(AppDimens.BottomNavigationIconSize),
            colorFilter = if (preserveIconColors && !selected) null else
                androidx.compose.ui.graphics.ColorFilter.tint(tint)
        )
        androidx.compose.foundation.layout.Spacer(
            modifier = Modifier.height(AppDimens.BottomNavigationLabelGap)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = tint
        )
    }
}

// IME inset은 부모 Screen 담당. IME 전송 액션을 동일 동작에 연결.
@Composable
fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canSend = value.isNotBlank()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(AppDimens.BottomNavigationHeight)
            .background(AppColors.MainGreen.Green200)
            .padding(horizontal = AppDimens.Spacing20, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .height(AppDimens.InputHeight)
                .background(AppColors.Monotone.White, CircleShape)
                .padding(horizontal = AppDimens.InputHorizontalPadding),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall.copy(
                color = AppColors.Monotone.Gray800
            ),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = "메시지를 입력하세요...",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.Monotone.Gray400,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    innerTextField()
                }
            }
        )
        androidx.compose.foundation.layout.Spacer(
            modifier = Modifier.width(AppDimens.InputSendGap)
        )
        CircleButton(
            contentDescription = "메시지 전송",
            onClick = onSendClick,
            width = AppDimens.SendButtonSize,
            icon = painterResource(
                if (canSend) R.drawable.send_active else R.drawable.send_inactive
            ),
            iconSize = AppDimens.SendIconSize,
            backgroundColor = if (canSend) AppColors.MainGreen.Green700 else AppColors.Monotone.Gray100,
            contentColor = if (canSend) AppColors.MainGreen.Green200 else AppColors.Monotone.Gray400,
            disabledBackgroundColor = AppColors.Monotone.Gray100,
            disabledContentColor = AppColors.Monotone.Gray400,
            enabled = canSend
        )
    }
}

@Composable
fun LeaveMeetingBar(
    enabled: Boolean,
    onLeaveClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = "지금 나가기"
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = AppDimens.BottomNavigationHeight)
            .background(AppColors.MainGreen.Green200)
            .padding(
                start = AppDimens.Spacing20,
                end = AppDimens.Spacing20,
                top = AppDimens.LeaveButtonTopPadding,
                bottom = AppDimens.LeaveButtonBottomPadding
            ),
        contentAlignment = Alignment.Center
    ) {
        AppButton(
            text = text,
            variant = ButtonVariant.DEFAULT,
            enabled = enabled,
            onClick = onLeaveClick,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true, widthDp = 412)
@Composable
private fun BottomNavigationBarPreview() {
    JigmnnagaTheme {
        Column {
            BottomNavigationBar(
                selectedTab = BottomNavigationTab.NONE,
                onHomeClick = {}, onAddClick = {}, onScheduleClick = {}
            )
            BottomNavigationBar(
                selectedTab = BottomNavigationTab.HOME,
                onHomeClick = {}, onAddClick = {}, onScheduleClick = {}
            )
            BottomNavigationBar(
                selectedTab = BottomNavigationTab.SCHEDULE,
                onHomeClick = {}, onAddClick = {}, onScheduleClick = {}
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 412)
@Composable
private fun ChatInputBarPreview() {
    JigmnnagaTheme {
        Column {
            ChatInputBar(value = "", onValueChange = {}, onSendClick = {})
            ChatInputBar(value = "보낼 메시지 예시", onValueChange = {}, onSendClick = {})
        }
    }
}

@Preview(showBackground = true, widthDp = 412)
@Composable
private fun LeaveMeetingBarPreview() {
    JigmnnagaTheme {
        Column {
            LeaveMeetingBar(enabled = true, onLeaveClick = {})
            LeaveMeetingBar(enabled = false, onLeaveClick = {})
        }
    }
}
