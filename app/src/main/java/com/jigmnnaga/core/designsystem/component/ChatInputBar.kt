package com.jigmnnaga.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jigmnnaga.R
import com.jigmnnaga.core.designsystem.theme.AppColors
import com.jigmnnaga.core.designsystem.theme.AppDimens
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme

// IME inset은 부모 Screen이 처리하며, 전송 후 키보드와 입력 포커스는 유지한다.
@Composable
fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canSend = value.isNotBlank()
    val sendMessage = {
        if (canSend) onSendClick()
    }
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
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { sendMessage() }),
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
                            text = stringResource(R.string.chat_input_hint),
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
            contentDescription = stringResource(R.string.send_message_description),
            onClick = sendMessage,
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
