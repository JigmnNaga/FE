package com.jigmnnaga.presentation.common.login.terms

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jigmnnaga.core.designsystem.component.BackHeader
import com.jigmnnaga.core.designsystem.theme.AppColors
import com.jigmnnaga.core.designsystem.theme.AppDimens

@Composable
internal fun PolicyDocumentLayout(
    title: String,
    badgeText: String,
    documentTitle: String,
    documentDescription: String,
    effectiveDate: String,
    guideText: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.MainGreen.Green200)
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            BackHeader(title = title, onBackClick = onBackClick)

            Box(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = AppDimens.Spacing20)
                        .padding(top = AppDimens.Spacing16, bottom = 48.dp)
                        .navigationBarsPadding()
                ) {
                    // 문서 머리말 카드
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = AppColors.MainGreen.Green600,
                                shape = RoundedCornerShape(AppDimens.ButtonCornerRadius)
                            )
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = AppColors.MainGreen.Green200,
                                modifier = Modifier
                                    .background(
                                        color = Color.White.copy(alpha = 0.12f),
                                        shape = CircleShape
                                    )
                                    .padding(horizontal = 9.dp, vertical = 5.dp)
                            )
                            Text(
                                text = "v1.0",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = AppColors.MainGreen.Green300
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        Text(
                            text = documentTitle,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                fontSize = 19.sp,
                                lineHeight = 28.sp
                            ),
                            color = AppColors.MainGreen.Green200,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(10.dp))

                        Text(
                            text = documentDescription,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                lineHeight = 18.sp
                            ),
                            color = AppColors.MainGreen.Green300,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = Color.White.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(9.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "시행일",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = AppColors.MainGreen.Green200
                            )
                            Spacer(Modifier.padding(start = AppDimens.Spacing8))
                            Text(
                                text = effectiveDate,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = AppColors.MainGreen.Green300
                            )
                        }
                    }

                    Spacer(Modifier.height(22.dp))

                    // 안내 박스
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = AppColors.Monotone.White,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = AppColors.Monotone.Gray100,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = guideText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                lineHeight = 17.sp
                            ),
                            color = AppColors.Monotone.Gray500
                        )
                    }

                    Spacer(Modifier.height(22.dp))

                    content()
                }

                // 하단 페이드
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .align(androidx.compose.ui.Alignment.BottomCenter)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    AppColors.MainGreen.Green200.copy(alpha = 0f),
                                    AppColors.MainGreen.Green200
                                )
                            )
                        )
                )
            }
        }
    }
}

@Composable
internal fun PolicySectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            lineHeight = 21.sp
        ),
        color = AppColors.MainGreen.Green900,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
internal fun PolicySectionBody(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 11.sp,
            lineHeight = 19.sp
        ),
        color = AppColors.Monotone.Gray700,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
internal fun PolicyDivider() {
    HorizontalDivider(
        color = AppColors.Monotone.Gray100,
        thickness = 1.dp
    )
}

@Composable
internal fun PolicySection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(7.dp)
    ) {
        PolicySectionTitle(text = title)
        content()
    }
}
