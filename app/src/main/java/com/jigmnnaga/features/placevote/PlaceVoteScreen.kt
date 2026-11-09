package com.jigmnnaga.features.placevote

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jigmnnaga.R
import com.jigmnnaga.core.designsystem.component.AppButton
import com.jigmnnaga.core.designsystem.component.BackHeader
import com.jigmnnaga.core.designsystem.component.ButtonVariant
import com.jigmnnaga.core.designsystem.theme.AppColors
import com.jigmnnaga.core.designsystem.theme.AppDimens
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme

/** 역할 구분은 호출자가 제공하는 상태/동작에만 영향을 주며, 서버 권한을 판정하지 않습니다. */
enum class PlaceVoteRole {
    HOST,
    PARTICIPANT
}

data class PlaceCandidateUiModel(
    val id: String,
    val name: String,
    val address: String? = null,
    /** 서버 또는 상위 화면에서 전달된 집계가 있을 때만 표시합니다. */
    val voteCount: Int? = null
)

data class PlaceVoteUiState(
    val role: PlaceVoteRole,
    val searchQuery: String,
    val candidates: List<PlaceCandidateUiModel>,
    val selectedCandidateIds: Set<String>,
    val title: String,
    val description: String,
    val submitLabel: String,
    val submitEnabled: Boolean = selectedCandidateIds.isNotEmpty(),
    /** 해당 Figma 화면에 검색 입력이 있을 때만 호출 측에서 표시합니다. */
    val showSearchInput: Boolean = false,
    /** 해당 화면에 지도 영역이 정의되어 있지만 SDK가 연결되지 않은 경우에만 표시합니다. */
    val showMapUnavailableNotice: Boolean = false,
    val voteSummary: String? = null
)

/**
 * 장소 검색·선택 UI. 검색 결과와 선택 상태는 모두 호출 측에서 소유합니다.
 * 이 화면은 실제 검색, 후보 저장 또는 투표 제출을 수행하지 않습니다.
 */
@Composable
fun PlaceVoteScreen(
    state: PlaceVoteUiState,
    onBackClick: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCandidateSelectionChange: (Set<String>) -> Unit,
    onSubmitSelection: () -> Unit,
    onRemoveCandidate: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.MainGreen.Green200)
    ) {
        BackHeader(title = state.title, onBackClick = onBackClick)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = AppDimens.Spacing20)
        ) {
            Text(
                text = state.description,
                style = MaterialTheme.typography.titleMedium,
                color = AppColors.Monotone.SoftBlack,
                modifier = Modifier.padding(top = AppDimens.Spacing8, bottom = AppDimens.Spacing16)
            )
            if (state.showSearchInput) {
                PlaceSearchField(
                    value = state.searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            state.voteSummary?.let { summary ->
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.Monotone.Gray700,
                    modifier = Modifier.padding(top = AppDimens.Spacing12)
                )
            }
            if (state.showMapUnavailableNotice) {
                MapUnavailableNotice(modifier = Modifier.padding(top = AppDimens.Spacing12))
            }
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = AppDimens.Spacing16)
                    .testTag("place-candidate-list"),
                verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
            ) {
                items(state.candidates, key = { it.id }) { candidate ->
                    val selected = candidate.id in state.selectedCandidateIds
                    PlaceCandidateCard(
                        candidate = candidate,
                        selected = selected,
                        role = state.role,
                        onClick = {
                            val updated = state.selectedCandidateIds.toMutableSet().apply {
                                if (selected) remove(candidate.id) else add(candidate.id)
                            }
                            onCandidateSelectionChange(updated)
                        },
                        onRemove = onRemoveCandidate?.let { remove -> { remove(candidate.id) } }
                    )
                }
            }
        }
        Surface(color = AppColors.MainGreen.Green200, shadowElevation = 0.dp) {
            AppButton(
                text = state.submitLabel,
                variant = ButtonVariant.DEFAULT,
                enabled = state.submitEnabled,
                onClick = onSubmitSelection,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppDimens.Spacing20, vertical = AppDimens.Spacing12)
                    .testTag("place-vote-submit")
            )
        }
    }
}

@Composable
private fun PlaceSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val searchDescription = stringResource(R.string.place_vote_search_hint)
    Surface(
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(AppDimens.Spacing12),
        color = AppColors.Monotone.White,
        border = BorderStroke(1.dp, AppColors.Monotone.Gray300)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AppDimens.Spacing16),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SearchGlyph(Modifier.size(18.dp))
            Spacer(Modifier.width(AppDimens.Spacing8))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = searchDescription }
                    .testTag("place-search-input"),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = AppColors.Monotone.SoftBlack),
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                text = stringResource(R.string.place_vote_search_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppColors.Monotone.Gray500,
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

@Composable
private fun SearchGlyph(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = 1.7.dp.toPx()
        drawCircle(
            color = AppColors.Monotone.Gray700,
            radius = size.minDimension * 0.3f,
            center = androidx.compose.ui.geometry.Offset(size.width * 0.42f, size.height * 0.42f),
            style = Stroke(width = stroke)
        )
        drawLine(
            color = AppColors.Monotone.Gray700,
            start = androidx.compose.ui.geometry.Offset(size.width * 0.64f, size.height * 0.64f),
            end = androidx.compose.ui.geometry.Offset(size.width * 0.9f, size.height * 0.9f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun MapUnavailableNotice(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = AppColors.Monotone.Gray100,
        shape = RoundedCornerShape(AppDimens.Spacing12)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AppDimens.Spacing12, vertical = AppDimens.Spacing8),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.place_vote_map_unavailable),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.Monotone.Gray700
            )
        }
    }
}

@Composable
private fun PlaceCandidateCard(
    candidate: PlaceCandidateUiModel,
    selected: Boolean,
    role: PlaceVoteRole,
    onClick: () -> Unit,
    onRemove: (() -> Unit)?
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                this.selected = selected
                contentDescription = buildString {
                    append(candidate.name)
                    candidate.address?.let { append(", ").append(it) }
                    candidate.voteCount?.let { append(", ").append("투표 ").append(it) }
                }
            }
            .clickable(role = Role.Checkbox, onClick = onClick)
            .testTag("place-candidate-${candidate.id}"),
        shape = RoundedCornerShape(AppDimens.Spacing12),
        color = if (selected) AppColors.MainGreen.Green200 else AppColors.Monotone.White,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) AppColors.MainGreen.Green900 else AppColors.Monotone.Gray200
        )
    ) {
        Row(
            modifier = Modifier.padding(AppDimens.Spacing16),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SelectionMark(selected = selected)
            Spacer(Modifier.width(AppDimens.Spacing12))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = candidate.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = AppColors.Monotone.SoftBlack,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                candidate.address?.takeIf(String::isNotBlank)?.let { address ->
                    Text(
                        text = address,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.Monotone.Gray600,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = AppDimens.Spacing4)
                    )
                }
                candidate.voteCount?.let { count ->
                    Text(
                        text = stringResource(R.string.place_vote_count, count),
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.MainGreen.Green900,
                        modifier = Modifier.padding(top = AppDimens.Spacing4)
                    )
                }
            }
            if (role == PlaceVoteRole.HOST && onRemove != null) {
                Spacer(Modifier.width(AppDimens.Spacing8))
                Text(
                    text = stringResource(R.string.place_vote_remove_candidate),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.Danger.Red600,
                    modifier = Modifier
                        .clickable(role = Role.Button, onClick = onRemove)
                        .padding(AppDimens.Spacing8)
                        .testTag("remove-candidate-${candidate.id}")
                )
            }
        }
    }
}

@Composable
private fun SelectionMark(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .background(
                color = if (selected) AppColors.MainGreen.Green900 else AppColors.Monotone.White,
                shape = RoundedCornerShape(10.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Canvas(Modifier.size(12.dp)) {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(size.width * 0.18f, size.height * 0.52f)
                    lineTo(size.width * 0.42f, size.height * 0.76f)
                    lineTo(size.width * 0.84f, size.height * 0.24f)
                }
                drawPath(
                    path = path,
                    color = AppColors.MainGreen.Green200,
                    style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun PlaceVoteScreenPreview() {
    JigmnnagaTheme {
        PlaceVoteScreen(
            state = PlaceVoteUiState(
                role = PlaceVoteRole.PARTICIPANT,
                searchQuery = "",
                candidates = listOf(
                    PlaceCandidateUiModel("gangnam", "강남역", "서울특별시 강남구 강남대로 396"),
                    PlaceCandidateUiModel("jamsil", "잠실역", "서울특별시 송파구 올림픽로 265")
                ),
                selectedCandidateIds = setOf("gangnam"),
                title = "어디서 만날까요?",
                description = "희망 위치 선택",
                submitLabel = "저장"
            ),
            onBackClick = {},
            onSearchQueryChange = {},
            onCandidateSelectionChange = {},
            onSubmitSelection = {}
        )
    }
}
