package com.jigmnnaga.presentation.common.error

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jigmnnaga.R
import com.jigmnnaga.core.designsystem.component.AppButton
import com.jigmnnaga.core.designsystem.component.ButtonVariant
import com.jigmnnaga.core.designsystem.theme.AppColors
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme

/**
 * 공통 오류 페이지 (COM-003-01 ~ 04)
 *
 * @param type      표시할 오류 유형 — [ErrorType] 참고
 * @param onActionClick 하단 버튼 클릭 콜백
 *                  (서비스/네트워크: 다시 시도, 위치 권한: 설정 이동, 페이지 없음: 홈 이동)
 */
@Composable
fun ErrorScreen(
    type: ErrorType,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val config = type.config()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.MainGreen.Green200)
    ) {
        // 중앙 콘텐츠 영역
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(50.dp)
            ) {
                Image(
                    painter = painterResource(config.iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(244.dp)
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(22.dp)
                ) {
                    Text(
                        text = config.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 20.sp,
                            lineHeight = 28.sp,
                            color = AppColors.Monotone.SoftBlack
                        ),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = config.description,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = AppColors.Monotone.Gray500
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // 하단 버튼 영역
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 1.dp, bottom = 15.dp)
        ) {
            AppButton(
                text = config.buttonText,
                variant = ButtonVariant.DEFAULT,
                onClick = onActionClick
            )
        }
    }
}

// ─── 내부 설정 모델 ───────────────────────────────────────────────────────────

private data class ErrorConfig(
    @DrawableRes val iconRes: Int,
    val title: String,
    val description: String,
    val buttonText: String
)

private fun ErrorType.config(): ErrorConfig = when (this) {
    ErrorType.Service -> ErrorConfig(
        iconRes = R.drawable.error_cloud,
        title = "서비스를 이용할 수 없어요",
        description = "일시적인 오류가 발생했어요.\n잠시 후 다시 시도해 주세요.",
        buttonText = "다시 시도"
    )
    ErrorType.Network -> ErrorConfig(
        iconRes = R.drawable.error_wifi,
        title = "연결이 끊겼어요",
        description = "인터넷 연결 상태를 확인한 후\n다시 시도해 주세요.",
        buttonText = "다시 시도"
    )
    ErrorType.LocationPermission -> ErrorConfig(
        iconRes = R.drawable.error_location,
        title = "위치 권한이 필요해요",
        description = "실시간 위치 공유를 이용하려면\n위치 접근 권한이 필요해요.",
        buttonText = "설정으로 이동"
    )
    ErrorType.NotFound -> ErrorConfig(
        iconRes = R.drawable.error_document,
        title = "페이지를 찾을 수 없어요",
        description = "요청하신 페이지가 삭제되었거나\n더 이상 제공되지 않아요.",
        buttonText = "홈으로 이동"
    )
}

// ─── Compose Preview ─────────────────────────────────────────────────────────

@Preview(showBackground = true, widthDp = 412, heightDp = 892, name = "서비스 오류")
@Composable
private fun ErrorScreenServicePreview() {
    JigmnnagaTheme {
        ErrorScreen(type = ErrorType.Service, onActionClick = {})
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892, name = "네트워크 오류")
@Composable
private fun ErrorScreenNetworkPreview() {
    JigmnnagaTheme {
        ErrorScreen(type = ErrorType.Network, onActionClick = {})
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892, name = "위치 권한 오류")
@Composable
private fun ErrorScreenLocationPermissionPreview() {
    JigmnnagaTheme {
        ErrorScreen(type = ErrorType.LocationPermission, onActionClick = {})
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892, name = "페이지 없음")
@Composable
private fun ErrorScreenNotFoundPreview() {
    JigmnnagaTheme {
        ErrorScreen(type = ErrorType.NotFound, onActionClick = {})
    }
}
