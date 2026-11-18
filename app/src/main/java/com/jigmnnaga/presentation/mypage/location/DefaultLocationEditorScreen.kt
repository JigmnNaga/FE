package com.jigmnnaga.presentation.mypage.location

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jigmnnaga.R
import com.jigmnnaga.core.designsystem.component.AppButton
import com.jigmnnaga.core.designsystem.component.BackHeader
import com.jigmnnaga.core.designsystem.component.ButtonVariant
import com.jigmnnaga.core.designsystem.theme.AppColors
import com.jigmnnaga.core.designsystem.theme.AppDimens
import com.jigmnnaga.presentation.appointment.placevote.PlaceSearchField
import com.jigmnnaga.presentation.appointment.route.Hint
import com.jigmnnaga.presentation.appointment.route.LocationCard
import com.jigmnnaga.presentation.appointment.route.SectionTitle

/**
 * 기본 위치 추가·수정 화면. Figma에 전용 화면이 없어서 출발 위치 선택 화면(ROU-001-02)의
 * 위치 검색과 카드를 그대로 쓰고, 서버가 필수로 요구하는 이름 입력칸만 더했다.
 */
@Composable
fun DefaultLocationEditorScreen(
    editor: DefaultLocationEditorUiState,
    onBackClick: () -> Unit,
    onNameChange: (String) -> Unit,
    onQueryChange: (String) -> Unit,
    onSelectPlace: (DefaultLocationPlace) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = if (editor.isEditing) R.string.default_location_edit_title else R.string.default_location_add_title
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.MainGreen.Green200)
            .systemBarsPadding()
            .imePadding()
            .testTag("default-location-editor")
    ) {
        BackHeader(title = stringResource(title), onBackClick = onBackClick)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppDimens.Spacing20, vertical = AppDimens.Spacing12),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SectionTitle(stringResource(R.string.default_location_name_title))
            NameField(
                value = editor.name,
                hint = stringResource(R.string.default_location_name_hint),
                onValueChange = onNameChange
            )
            SectionTitle(stringResource(R.string.default_location_place_title))
            editor.selected?.let { selected ->
                Text(
                    text = stringResource(R.string.default_location_selected_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.Monotone.Gray600
                )
                LocationCard(
                    title = selected.title,
                    detail = selected.detail,
                    selected = true,
                    onClick = {},
                    modifier = Modifier.testTag("default-location-selected")
                )
            }
            PlaceSearchField(
                value = editor.query,
                hint = stringResource(R.string.default_location_search_hint),
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth()
            )
            when (editor.searchStatus) {
                DefaultLocationSearchStatus.LOADING ->
                    Hint(stringResource(R.string.default_location_search_loading), "default-location-search-loading")
                DefaultLocationSearchStatus.EMPTY ->
                    Hint(stringResource(R.string.default_location_search_empty), "default-location-search-empty")
                DefaultLocationSearchStatus.FAILED ->
                    Hint(stringResource(R.string.default_location_search_failed), "default-location-search-failed")
                DefaultLocationSearchStatus.IDLE -> Unit
            }
            editor.searchResults.filterNot { editor.isSelected(it) }.forEach { place ->
                LocationCard(
                    title = place.title,
                    detail = place.detail,
                    selected = false,
                    onClick = { onSelectPlace(place) },
                    modifier = Modifier.testTag("default-location-result-${place.id}")
                )
            }
        }
        if (editor.saveStatus == DefaultLocationActionStatus.FAILED) {
            Hint(
                text = stringResource(R.string.default_location_save_failed),
                tag = "default-location-save-failed"
            )
        }
        AppButton(
            text = stringResource(R.string.default_location_save),
            variant = ButtonVariant.DEFAULT,
            enabled = editor.canSave,
            onClick = onSave,
            modifier = Modifier
                .padding(horizontal = AppDimens.Spacing20)
                .padding(top = 8.dp, bottom = 15.dp)
                .testTag("default-location-save")
        )
    }
}

/** 위치 검색칸과 같은 모양의 한 줄 입력칸. */
@Composable
private fun NameField(
    value: String,
    hint: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(AppDimens.ButtonCornerRadius),
        color = AppColors.Monotone.White,
        border = BorderStroke(1.dp, AppColors.Monotone.Gray100)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = AppDimens.Spacing16),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = hint }
                    .testTag("default-location-name-input"),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = AppColors.Monotone.SoftBlack),
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                text = hint,
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppColors.Monotone.SoftBlack.copy(alpha = 0.5f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}
