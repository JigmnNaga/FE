package com.jigmnnaga.presentation.mypage.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.jigmnnaga.R
import com.jigmnnaga.core.designsystem.theme.AppColors
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme

/** 회원탈퇴 확인 팝업. Figma MYP-002-01 오버레이. */
@Composable
fun WithdrawalConfirmDialog(
    activeAppointmentCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.MainGreen.Green200, RoundedCornerShape(22.dp))
                .padding(horizontal = 22.dp)
                .padding(top = 26.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(AppColors.Danger.Red200, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.account_withdrawal),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color.Unspecified
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Text(
                    text = stringResource(R.string.withdrawal_dialog_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 21.sp,
                        lineHeight = 30.sp
                    ),
                    color = AppColors.MainGreen.Green900,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.withdrawal_dialog_body),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    ),
                    color = AppColors.MainGreen.Green700,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            if (activeAppointmentCount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.Danger.Red200, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.warning),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.Unspecified
                    )
                    Text(
                        text = stringResource(R.string.withdrawal_dialog_active_warning, activeAppointmentCount),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        ),
                        color = AppColors.Danger.Red600,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .size(height = 52.dp, width = 0.dp)
                        .background(AppColors.Monotone.Gray200, RoundedCornerShape(13.dp))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.withdrawal_dialog_cancel),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        ),
                        color = AppColors.Monotone.SoftBlack
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .size(height = 52.dp, width = 0.dp)
                        .background(AppColors.Danger.Red600, RoundedCornerShape(13.dp))
                        .clickable(onClick = onConfirm),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.withdrawal_dialog_confirm),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        ),
                        color = AppColors.Monotone.White
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "회원탈퇴 팝업 - 진행 중 약속 없음")
@Composable
private fun WithdrawalConfirmDialogPreview() {
    JigmnnagaTheme {
        WithdrawalConfirmDialog(
            activeAppointmentCount = 0,
            onDismiss = {},
            onConfirm = {}
        )
    }
}

@Preview(showBackground = true, name = "회원탈퇴 팝업 - 진행 중 약속 있음")
@Composable
private fun WithdrawalConfirmDialogWithWarningPreview() {
    JigmnnagaTheme {
        WithdrawalConfirmDialog(
            activeAppointmentCount = 2,
            onDismiss = {},
            onConfirm = {}
        )
    }
}
