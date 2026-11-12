package com.jigmnnaga.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.jigmnnaga.R
import com.jigmnnaga.core.designsystem.theme.AppColors
import com.jigmnnaga.core.designsystem.theme.AppDimens
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme

// 좌우 20dp inset은 부모 담당. 헤더는 68dp 콘텐츠 영역만 담당. 프로필 라벨은 원 안에 첫 글자만 표시한다.
@Composable
fun MainHeader(
    onNotificationClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
    profileLabel: String? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(AppDimens.MainHeaderHeight)
            .background(AppColors.MainGreen.Green200)
            .padding(vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.logo_mark),
            contentDescription = stringResource(R.string.app_logo_mark_description),
            modifier = Modifier.size(38.dp)
        )
        Spacer(Modifier.width(AppDimens.Spacing8))
        Image(
            painter = painterResource(R.drawable.logo_text),
            contentDescription = stringResource(R.string.app_logo_text_description),
            modifier = Modifier.size(width = 69.dp, height = 24.dp)
        )
        Spacer(Modifier.weight(1f))

        CircleButton(
            contentDescription = stringResource(R.string.notification_description),
            onClick = onNotificationClick,
            width = AppDimens.HeaderActionSize,
            height = AppDimens.HeaderActionSize,
            icon = painterResource(R.drawable.bell),
            iconSize = AppDimens.HeaderIconSize,
            backgroundColor = AppColors.Monotone.Gray100,
            contentColor = AppColors.MainGreen.Green700
        )
        Spacer(Modifier.width(AppDimens.Spacing8))
        CircleButton(
            contentDescription = stringResource(R.string.profile_description),
            onClick = onProfileClick,
            width = AppDimens.HeaderActionSize,
            height = AppDimens.HeaderActionSize,
            icon = if (profileLabel.isNullOrBlank()) painterResource(R.drawable.profile) else null,
            iconSize = if (profileLabel.isNullOrBlank()) AppDimens.HeaderIconSize else null,
            text = profileLabel?.trim()?.takeIf { it.isNotEmpty() }?.let { it.substring(0, it.offsetByCodePoints(0, 1)) },
            textSize = MaterialTheme.typography.titleSmall.fontSize,
            backgroundColor = AppColors.MainGreen.Green700,
            contentColor = AppColors.MainGreen.Green200
        )
    }
}

// 네비게이션 바·IME inset은 부모 Scaffold 담당.
@Composable
fun MeetingHeader(
    onBackClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onPlaceClick: () -> Unit,
    onMoreClick: () -> Unit,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(AppDimens.MeetingHeaderHeight)
            .padding(horizontal = AppDimens.HeaderHorizontalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircleButton(
            contentDescription = stringResource(R.string.back_description),
            onClick = onBackClick,
            width = AppDimens.HeaderActionSize,
            height = AppDimens.HeaderActionSize,
            icon = painterResource(R.drawable.chevron_left),
            iconSize = AppDimens.HeaderIconSize,
            backgroundColor = AppColors.MainGreen.Green200,
            contentColor = AppColors.Monotone.Gray700
        )
        Spacer(Modifier.width(AppDimens.HeaderActionGap))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = AppColors.Monotone.SoftBlack,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.Monotone.Gray500,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(AppDimens.HeaderActionGap))
        CircleButton(
            contentDescription = stringResource(R.string.schedule_description),
            onClick = onCalendarClick,
            width = AppDimens.HeaderActionSize,
            height = AppDimens.CompactHeaderActionHeight,
            icon = painterResource(R.drawable.calendar),
            iconSize = AppDimens.HeaderIconSize,
            iconTint = null,
            backgroundColor = AppColors.MainGreen.Green600,
            contentColor = AppColors.MainGreen.Green200,
            shape = RoundedCornerShape(AppDimens.CompactHeaderActionRadius)
        )
        Spacer(Modifier.width(AppDimens.HeaderActionGap))
        CircleButton(
            contentDescription = stringResource(R.string.place_description),
            onClick = onPlaceClick,
            width = AppDimens.HeaderActionSize,
            height = AppDimens.CompactHeaderActionHeight,
            icon = painterResource(R.drawable.place),
            iconSize = AppDimens.HeaderIconSize,
            iconTint = null,
            backgroundColor = AppColors.MainGreen.Green600,
            contentColor = AppColors.MainGreen.Green200,
            shape = RoundedCornerShape(AppDimens.CompactHeaderActionRadius)
        )
        Spacer(Modifier.width(AppDimens.HeaderActionGap))
        IconButton(onClick = onMoreClick, modifier = Modifier.size(48.dp)) {
            Image(
                painter = painterResource(R.drawable.more_vertical),
                contentDescription = stringResource(R.string.more_description),
                modifier = Modifier.size(width = 4.dp, height = 18.dp)
            )
        }
    }
}

@Composable
fun BackHeader(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(AppDimens.BackHeaderHeight)
            .background(AppColors.MainGreen.Green200)
            .padding(horizontal = AppDimens.HeaderHorizontalPadding),
        contentAlignment = Alignment.Center
    ) {
        CircleButton(
            contentDescription = stringResource(R.string.back_description),
            onClick = onBackClick,
            width = AppDimens.HeaderActionSize,
            height = AppDimens.HeaderActionSize,
            icon = painterResource(R.drawable.chevron_left),
            iconSize = AppDimens.HeaderIconSize,
            backgroundColor = AppColors.MainGreen.Green200,
            contentColor = AppColors.Monotone.Gray700,
            modifier = Modifier.align(Alignment.CenterStart)
        )
        Text(
            text = title,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelLarge,
            color = AppColors.Monotone.SoftBlack,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview(showBackground = true, widthDp = 412)
@Composable
private fun MainHeaderPreview() {
    JigmnnagaTheme {
        Column {
            MainHeader(onNotificationClick = {}, onProfileClick = {})
            MainHeader(onNotificationClick = {}, onProfileClick = {}, profileLabel = "나")
        }
    }
}

@Preview(showBackground = true, widthDp = 412)
@Composable
private fun MeetingHeaderPreview() {
    JigmnnagaTheme {
        MeetingHeader(
            onBackClick = {}, onCalendarClick = {}, onPlaceClick = {}, onMoreClick = {},
            title = "주말 산행과 맛집 방문을 함께하는 스터디 모임",
            subtitle = "4명 · 텍스트 채팅"
        )
    }
}

@Preview(showBackground = true, widthDp = 412)
@Composable
private fun BackHeaderPreview() {
    JigmnnagaTheme {
        BackHeader(title = "약속 만들기", onBackClick = {})
    }
}
