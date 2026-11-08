package com.jigmnnaga.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.jigmnnaga.R
import com.jigmnnaga.core.designsystem.theme.AppColors
import com.jigmnnaga.core.designsystem.theme.AppDimens
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme

@Composable
fun LeaveMeetingBar(
    enabled: Boolean,
    onLeaveClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = stringResource(R.string.leave_meeting_label)
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
private fun LeaveMeetingBarPreview() {
    JigmnnagaTheme {
        Column {
            LeaveMeetingBar(enabled = true, onLeaveClick = {})
            LeaveMeetingBar(enabled = false, onLeaveClick = {})
        }
    }
}
