package com.jigmnnaga.presentation.mypage.location

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jigmnnaga.R
import com.jigmnnaga.core.designsystem.component.BackHeader
import com.jigmnnaga.core.designsystem.theme.AppColors
import com.jigmnnaga.core.designsystem.theme.AppDimens

private val CardShape = RoundedCornerShape(16.dp)

private fun TextStyle.sized(size: Int, line: Int, weight: FontWeight? = null): TextStyle =
    copy(fontSize = size.sp, lineHeight = line.sp, fontWeight = weight ?: fontWeight)

/**
 * 기본 위치 설정 화면(MYP-001-01). 등록한 기본 위치를 보여 주고, 추가·수정·삭제를 누른 사실만 콜백으로 알린다.
 * 3개를 다 채우면 "기본 위치 추가하기"를 누를 수 없다.
 */
@Composable
fun DefaultLocationScreen(
    state: DefaultLocationUiState,
    onBackClick: () -> Unit,
    onAddClick: () -> Unit,
    onEditClick: (Long) -> Unit,
    onDeleteClick: (Long) -> Unit,
    onRetryLoad: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.MainGreen.Green200)
            .systemBarsPadding()
            .testTag("default-location-screen")
    ) {
        BackHeader(title = stringResource(R.string.default_location_title), onBackClick = onBackClick)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppDimens.Spacing20)
                .padding(top = AppDimens.Spacing16, bottom = AppDimens.Spacing20),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing16)
        ) {
            TitleRow(count = state.count)
            Text(
                text = stringResource(R.string.default_location_caption),
                style = MaterialTheme.typography.labelMedium.sized(13, 18, FontWeight.Normal),
                color = AppColors.Monotone.Gray600
            )
            when (state.loadStatus) {
                DefaultLocationLoadStatus.LOADING -> StatusText(
                    text = stringResource(R.string.default_location_loading),
                    tag = "default-location-loading"
                )
                DefaultLocationLoadStatus.FAILED -> {
                    StatusText(
                        text = stringResource(R.string.default_location_failed),
                        tag = "default-location-failed"
                    )
                    Text(
                        text = stringResource(R.string.default_location_retry),
                        style = MaterialTheme.typography.labelMedium.sized(13, 18, FontWeight.Bold),
                        color = AppColors.MainGreen.Green700,
                        modifier = Modifier
                            .clickable(role = Role.Button, onClick = onRetryLoad)
                            .padding(vertical = 4.dp)
                            .testTag("default-location-retry")
                    )
                }
                DefaultLocationLoadStatus.LOADED -> {
                    if (state.locations.isEmpty()) {
                        StatusText(
                            text = stringResource(R.string.default_location_empty),
                            tag = "default-location-empty"
                        )
                    }
                    state.locations.forEach { location ->
                        DefaultLocationCard(
                            location = location,
                            onEditClick = { onEditClick(location.id) },
                            onDeleteClick = { onDeleteClick(location.id) }
                        )
                    }
                }
            }
            if (state.deleteStatus == DefaultLocationActionStatus.FAILED) {
                StatusText(
                    text = stringResource(R.string.default_location_action_failed),
                    tag = "default-location-action-failed"
                )
            }
            AddButton(enabled = state.canAdd, onClick = onAddClick)
            Text(
                text = stringResource(R.string.default_location_limit_note),
                style = MaterialTheme.typography.labelSmall.sized(12, 16, FontWeight.Normal),
                color = AppColors.Monotone.Gray600
            )
        }
    }
}

@Composable
private fun TitleRow(count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.default_location_section_title),
            style = MaterialTheme.typography.titleMedium.sized(18, 24, FontWeight.Bold),
            color = AppColors.Monotone.SoftBlack
        )
        Box(
            modifier = Modifier
                .background(AppColors.MainGreen.Green700, CircleShape)
                .padding(horizontal = 10.dp, vertical = 3.dp)
        ) {
            Text(
                text = stringResource(R.string.default_location_count, count),
                style = MaterialTheme.typography.labelSmall.sized(12, 14, FontWeight.Bold),
                color = AppColors.MainGreen.Green200,
                modifier = Modifier.testTag("default-location-count")
            )
        }
    }
}

@Composable
private fun StatusText(text: String, tag: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.sized(12, 16, FontWeight.Normal),
        color = AppColors.Monotone.Gray600,
        modifier = Modifier.testTag(tag)
    )
}

@Composable
private fun DefaultLocationCard(
    location: DefaultLocationItem,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, CardShape)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("default-location-card-${location.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(AppColors.Monotone.Gray100, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.place),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = AppColors.Monotone.SoftBlack
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = location.name,
                style = MaterialTheme.typography.titleSmall.sized(15, 18, FontWeight.Bold),
                color = AppColors.Monotone.SoftBlack,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!location.address.isNullOrBlank()) {
                Text(
                    text = location.address,
                    style = MaterialTheme.typography.labelSmall.sized(12, 14, FontWeight.Normal),
                    color = AppColors.Monotone.Gray600,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CardAction(
                text = stringResource(R.string.default_location_edit),
                onClick = onEditClick,
                modifier = Modifier.testTag("default-location-edit-${location.id}")
            )
            CardAction(
                text = stringResource(R.string.default_location_delete),
                onClick = onDeleteClick,
                modifier = Modifier.testTag("default-location-delete-${location.id}")
            )
        }
    }
}

@Composable
private fun CardAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.sized(13, 16, FontWeight.Normal),
        color = AppColors.Monotone.Gray600,
        modifier = modifier.clickable(role = Role.Button, onClick = onClick)
    )
}

/** 점선 테두리의 "+ 기본 위치 추가하기". 3개를 다 채우면 흐리게 보이고 눌리지 않는다. */
@Composable
private fun AddButton(enabled: Boolean, onClick: () -> Unit) {
    val color = if (enabled) AppColors.MainGreen.Green700 else AppColors.Monotone.Gray400
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.MainGreen.Green200, CardShape)
            .drawBehind {
                val stroke = 1.5.dp.toPx()
                drawRoundRect(
                    color = color,
                    topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2),
                    size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(16.dp.toPx()),
                    style = Stroke(
                        width = stroke,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))
                    )
                )
            }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 18.dp)
            .testTag("default-location-add"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = "+",
            style = MaterialTheme.typography.titleMedium.sized(20, 27, FontWeight.Bold),
            color = color
        )
        Text(
            text = stringResource(R.string.default_location_add),
            style = MaterialTheme.typography.titleSmall.sized(15, 20, FontWeight.Bold),
            color = color,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
