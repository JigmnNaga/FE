package com.jigmnnaga.presentation.appointment.route

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jigmnnaga.R
import com.jigmnnaga.core.designsystem.component.AppButton
import com.jigmnnaga.core.designsystem.component.BackHeader
import com.jigmnnaga.core.designsystem.component.ButtonVariant
import com.jigmnnaga.core.designsystem.theme.AppColors
import com.jigmnnaga.core.designsystem.theme.AppDimens
import com.jigmnnaga.presentation.appointment.placevote.PlaceSearchField

private val StartCardShape = RoundedCornerShape(16.dp)

private fun TextStyle.sized(size: Int, line: Int, weight: FontWeight? = null): TextStyle =
    copy(fontSize = size.sp, lineHeight = line.sp, fontWeight = weight ?: fontWeight)

/**
 * 출발 위치 선택 화면(ROU-001-02). 내 기본 위치, 현재 위치, 검색한 위치 중 하나를 골라
 * "여기에서 출발하기"를 누른다. 누른 사실만 콜백으로 알리고, 권한 확인은 호출한 쪽이 맡는다.
 */
@Composable
fun RouteStartPickerScreen(
    state: RouteStartPickerUiState,
    onBackClick: () -> Unit,
    onSelect: (RouteStartLocation) -> Unit,
    onCurrentLocationClick: () -> Unit,
    onQueryChange: (String) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.MainGreen.Green200)
            .systemBarsPadding()
            .imePadding()
            .testTag("route-start-picker")
    ) {
        BackHeader(title = stringResource(R.string.route_start_picker_title), onBackClick = onBackClick)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppDimens.Spacing20, vertical = AppDimens.Spacing12),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BasicLocationsSection(state, onSelect)
            CurrentLocationSection(state, onCurrentLocationClick)
            SearchSection(state, onSelect, onQueryChange)
        }
        AppButton(
            text = stringResource(R.string.route_start_confirm),
            variant = ButtonVariant.DEFAULT,
            enabled = state.canConfirm,
            onClick = onConfirm,
            modifier = Modifier
                .padding(horizontal = AppDimens.Spacing20)
                .padding(top = 8.dp, bottom = 15.dp)
                .testTag("route-start-confirm")
        )
    }
}

@Composable
private fun BasicLocationsSection(state: RouteStartPickerUiState, onSelect: (RouteStartLocation) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.route_start_basic_title),
            style = MaterialTheme.typography.titleMedium.sized(18, 24),
            color = AppColors.Monotone.SoftBlack
        )
        Box(
            modifier = Modifier
                .background(AppColors.MainGreen.Green700, CircleShape)
                .padding(horizontal = 10.dp, vertical = 2.dp)
        ) {
            Text(
                text = stringResource(R.string.route_start_basic_count, state.basicLocations.size),
                style = MaterialTheme.typography.labelSmall.sized(12, 16, FontWeight.Bold),
                color = AppColors.MainGreen.Green200
            )
        }
    }
    Text(
        text = stringResource(R.string.route_start_basic_caption),
        style = MaterialTheme.typography.labelSmall.sized(12, 16),
        color = AppColors.Monotone.Gray600
    )
    when {
        state.basicLocationsFailed -> Hint(stringResource(R.string.route_start_basic_failed), "route-start-basic-failed")
        state.basicLocations.isEmpty() -> Hint(stringResource(R.string.route_start_basic_empty), "route-start-basic-empty")
        else -> state.basicLocations.forEach { location ->
            LocationCard(
                title = location.name,
                detail = location.detail,
                selected = state.isSelected(location.id),
                onClick = { onSelect(location) },
                modifier = Modifier.testTag("route-start-option-${location.id}")
            )
        }
    }
}

@Composable
private fun CurrentLocationSection(state: RouteStartPickerUiState, onCurrentLocationClick: () -> Unit) {
    SectionTitle(stringResource(R.string.route_start_current_title))
    val current = state.current
    LocationCard(
        title = current?.name ?: stringResource(R.string.route_start_current_title),
        detail = when (state.currentStatus) {
            RouteCurrentLocationStatus.LOADING -> stringResource(R.string.route_start_current_loading)
            RouteCurrentLocationStatus.FAILED -> stringResource(R.string.route_start_current_failed)
            RouteCurrentLocationStatus.PERMISSION_DENIED -> stringResource(R.string.route_start_current_denied)
            RouteCurrentLocationStatus.IDLE -> null
        },
        selected = current != null && state.isSelected(current.id),
        onClick = onCurrentLocationClick,
        modifier = Modifier.testTag("route-start-option-current")
    )
}

@Composable
private fun SearchSection(
    state: RouteStartPickerUiState,
    onSelect: (RouteStartLocation) -> Unit,
    onQueryChange: (String) -> Unit
) {
    SectionTitle(stringResource(R.string.route_start_other_title))
    PlaceSearchField(
        value = state.query,
        hint = stringResource(R.string.route_start_search_hint),
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth()
    )
    Text(
        text = stringResource(R.string.route_start_results_title),
        style = MaterialTheme.typography.labelMedium.sized(13, 18),
        color = AppColors.Monotone.Gray600
    )
    when (state.searchStatus) {
        RouteSearchStatus.LOADING -> Hint(stringResource(R.string.route_start_search_loading), "route-start-search-loading")
        RouteSearchStatus.EMPTY -> Hint(stringResource(R.string.route_start_search_empty), "route-start-search-empty")
        RouteSearchStatus.FAILED -> Hint(stringResource(R.string.route_start_search_failed), "route-start-search-failed")
        RouteSearchStatus.IDLE -> Unit
    }
    state.searchResults.forEach { place ->
        LocationCard(
            title = place.name,
            detail = place.detail,
            selected = state.isSelected(place.id),
            onClick = { onSelect(place) },
            modifier = Modifier.testTag("route-start-option-${place.id}")
        )
    }
}

@Composable
internal fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall.sized(15, 20),
        color = AppColors.Monotone.SoftBlack,
        modifier = Modifier.padding(top = AppDimens.Spacing12)
    )
}

@Composable
internal fun Hint(text: String, tag: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.sized(12, 16),
        color = AppColors.Monotone.Gray600,
        modifier = Modifier.testTag(tag)
    )
}

@Composable
internal fun LocationCard(
    title: String,
    detail: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val border = if (selected) {
        Modifier.border(2.dp, AppColors.MainGreen.Green700, StartCardShape)
    } else {
        Modifier
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(border)
            .background(Color.White, StartCardShape)
            .semantics { this.selected = selected }
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = AppDimens.Spacing12, vertical = AppDimens.Spacing12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(AppColors.Monotone.Gray100, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.place),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = AppColors.Monotone.SoftBlack
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.sized(15, 20),
                color = AppColors.Monotone.SoftBlack,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!detail.isNullOrBlank()) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.labelSmall.sized(11, 16),
                    color = AppColors.Monotone.Gray600,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
