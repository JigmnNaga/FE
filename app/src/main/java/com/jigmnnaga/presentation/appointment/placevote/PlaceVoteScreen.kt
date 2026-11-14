package com.jigmnnaga.presentation.appointment.placevote

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jigmnnaga.R
import com.jigmnnaga.core.designsystem.component.AppButton
import com.jigmnnaga.core.designsystem.component.BackHeader
import com.jigmnnaga.core.designsystem.component.ButtonVariant
import com.jigmnnaga.core.designsystem.component.map.AppMapMarker
import com.jigmnnaga.core.designsystem.component.map.AppMapStatus
import com.jigmnnaga.core.designsystem.component.map.AppMapView
import com.jigmnnaga.core.designsystem.component.map.isMapSupported
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
    /** 서버 또는 상위 화면에서 전달된 집계 문구. 예: "3/3명". 집계가 있을 때만 전달합니다. */
    val voteLabel: String? = null,
    /** 모든 멤버가 투표했으면 true. 배지 배경이 달라집니다. */
    val isVoteComplete: Boolean = false,
    /** 지도 마커 위치(위도). 좌표를 모르는 후보는 null이며 지도에 표시하지 않습니다. */
    val latitude: Double? = null,
    /** 지도 마커 위치(경도). */
    val longitude: Double? = null
)

data class PlaceVoteProgressMemberUiModel(
    val initial: String,
    val isMe: Boolean = false
)

/** 참여자 화면 상단의 선택 현황 카드. 값은 모두 호출 측에서 계산해 전달합니다. */
data class PlaceVoteProgressUiState(
    val members: List<PlaceVoteProgressMemberUiModel>,
    val title: String,
    val subtitle: String? = null,
    val statusLabel: String? = null
)

data class PlaceVoteUiState(
    val role: PlaceVoteRole,
    val searchQuery: String,
    val candidates: List<PlaceCandidateUiModel>,
    val selectedCandidateIds: Set<String>,
    /** 헤더에 표시되는 제목. 예: 약속방 이름 */
    val title: String,
    /** 본문 질문 문구. 예: "어디서 만날까요?" */
    val question: String,
    /** 후보 목록 위의 안내 문구. 예: "희망 위치 선택", "위치 선택·최대 5개" */
    val listLabel: String? = null,
    val submitLabel: String,
    val submitEnabled: Boolean = selectedCandidateIds.isNotEmpty(),
    /** 해당 Figma 화면에 검색 입력이 있을 때만 호출 측에서 표시합니다. */
    val showSearchInput: Boolean = false,
    /** 검색창 안내 문구. 비어 있으면 기본 문구를 사용합니다. 예: "역 이름 검색..." */
    val searchHint: String? = null,
    /** 후보가 없고 이 값이 있을 때 목록 자리에 표시합니다. 예: "검색 결과가 없어요" */
    val emptyMessage: String? = null,
    /** 해당 화면에 지도 영역이 정의되어 있지만 SDK가 연결되지 않은 경우에만 표시합니다. */
    val showMapUnavailableNotice: Boolean = false,
    /** 후보 위치를 보여 주는 지도 영역을 표시합니다. 지도를 쓸 수 없으면 안내 문구로 대신합니다. */
    val showMap: Boolean = false,
    val progress: PlaceVoteProgressUiState? = null,
    /** 저장 버튼 위 요약 카드 제목. 예: "선택된 장소 (2개)" */
    val selectedSummaryTitle: String? = null,
    val selectedPlaceNames: List<String> = emptyList()
)

private val VoteCompleteBadgeColor = Color(0xFFE9EFE3)

private val MapHeight = 180.dp

/** 좌표가 있는 후보만 지도 마커로 바꿉니다. */
internal fun List<PlaceCandidateUiModel>.toMapMarkers(): List<AppMapMarker> = mapNotNull { candidate ->
    val latitude = candidate.latitude
    val longitude = candidate.longitude
    if (latitude != null && longitude != null) {
        AppMapMarker(id = candidate.id, latitude = latitude, longitude = longitude)
    } else {
        null
    }
}

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
    onProgressClick: (() -> Unit)? = null,
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
            state.progress?.let { progress ->
                PlaceVoteProgressCard(
                    progress = progress,
                    onClick = onProgressClick,
                    modifier = Modifier.padding(top = AppDimens.Spacing8)
                )
            }
            Text(
                text = state.question,
                style = MaterialTheme.typography.titleSmall,
                color = AppColors.Monotone.SoftBlack,
                modifier = Modifier.padding(
                    top = if (state.progress != null) AppDimens.Spacing16 else AppDimens.Spacing8
                )
            )
            if (state.showSearchInput) {
                PlaceSearchField(
                    value = state.searchQuery,
                    hint = state.searchHint ?: stringResource(R.string.place_vote_search_hint),
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = AppDimens.Spacing12)
                )
            }
            state.listLabel?.let { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.Monotone.Gray500,
                    modifier = Modifier.padding(top = AppDimens.Spacing12)
                )
            }
            if (state.showMap) {
                PlaceVoteMap(
                    markers = state.candidates.toMapMarkers(),
                    modifier = Modifier.padding(top = AppDimens.Spacing12)
                )
            } else if (state.showMapUnavailableNotice) {
                MapUnavailableNotice(modifier = Modifier.padding(top = AppDimens.Spacing12))
            }
            if (state.candidates.isEmpty() && state.emptyMessage != null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("place-empty-message"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.emptyMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.Monotone.Gray500,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(top = AppDimens.Spacing12)
                        .testTag("place-candidate-list"),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
        }
        if (state.selectedSummaryTitle != null && state.selectedPlaceNames.isNotEmpty()) {
            SelectedPlacesSummary(
                title = state.selectedSummaryTitle,
                names = state.selectedPlaceNames,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppDimens.Spacing20)
                    .padding(top = AppDimens.Spacing8)
            )
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
private fun PlaceVoteProgressCard(
    progress: PlaceVoteProgressUiState,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimens.ButtonCornerRadius),
        color = AppColors.Monotone.White,
        border = BorderStroke(0.6.dp, AppColors.Monotone.Gray100)
    ) {
        Row(
            modifier = Modifier
                .then(
                    if (onClick != null) {
                        Modifier.clickable(role = Role.Button, onClick = onClick)
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = AppDimens.Spacing16, vertical = AppDimens.Spacing12)
                .semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                progress.members.forEach { member ->
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(
                                if (member.isMe) AppColors.MainGreen.Green700 else AppColors.MainGreen.Green400,
                                CircleShape
                            )
                            .border(1.7.dp, AppColors.Monotone.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = member.initial,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (member.isMe) AppColors.SubYellow.Yellow200 else AppColors.MainGreen.Green200,
                            maxLines = 1
                        )
                    }
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = progress.title,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = AppColors.Monotone.SoftBlack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                progress.subtitle?.let { subtitle ->
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
                        color = AppColors.Monotone.Gray500,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            progress.statusLabel?.let { status ->
                Text(
                    text = status,
                    modifier = Modifier
                        .background(AppColors.SubYellow.Yellow600.copy(alpha = 0.18f), CircleShape)
                        .padding(horizontal = AppDimens.Spacing8, vertical = AppDimens.Spacing4),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.SubYellow.Yellow600,
                    maxLines = 1
                )
            }
            if (onClick != null) {
                Icon(
                    painter = painterResource(R.drawable.chevron_down),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color.Unspecified
                )
            }
        }
    }
}

@Composable
private fun PlaceSearchField(
    value: String,
    hint: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(AppDimens.ButtonCornerRadius),
        color = AppColors.Monotone.White,
        border = BorderStroke(1.dp, AppColors.Monotone.Gray100)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AppDimens.Spacing16),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.search),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = Color.Unspecified
            )
            Spacer(Modifier.width(AppDimens.Spacing8))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = hint }
                    .testTag("place-search-input"),
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

/** 후보 위치 지도. 앱 키가 없거나 지도 인증에 실패하면 안내 문구로 바꿔 보여 줍니다. */
@Composable
private fun PlaceVoteMap(
    markers: List<AppMapMarker>,
    modifier: Modifier = Modifier
) {
    var mapFailed by remember { mutableStateOf(false) }
    if (!isMapSupported() || mapFailed) {
        MapUnavailableNotice(modifier = modifier)
        return
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(MapHeight)
            .clip(RoundedCornerShape(AppDimens.Spacing12))
            .background(AppColors.Monotone.Gray100)
            .testTag("place-vote-map")
    ) {
        AppMapView(
            markers = markers,
            modifier = Modifier.fillMaxSize(),
            onStatusChange = { status ->
                if (status == AppMapStatus.UNAVAILABLE) mapFailed = true
            }
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
                    candidate.voteLabel?.let { append(", ").append(it) }
                }
            }
            .clickable(role = Role.Checkbox, onClick = onClick)
            .testTag("place-candidate-${candidate.id}"),
        shape = RoundedCornerShape(AppDimens.ButtonCornerRadius),
        color = AppColors.Monotone.White,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) AppColors.MainGreen.Green500 else AppColors.Monotone.Gray100
        )
    ) {
        Row(
            modifier = Modifier.padding(AppDimens.Spacing16),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(AppColors.Monotone.Gray100, RoundedCornerShape(AppDimens.Spacing12)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.place),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = AppColors.MainGreen.Green900
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing4)
            ) {
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
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = AppColors.Monotone.Gray500,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            candidate.voteLabel?.let { label ->
                Text(
                    text = label,
                    modifier = Modifier
                        .background(
                            if (candidate.isVoteComplete) {
                                VoteCompleteBadgeColor
                            } else {
                                AppColors.MainGreen.Green700.copy(alpha = 0.12f)
                            },
                            CircleShape
                        )
                        .padding(horizontal = 10.dp, vertical = AppDimens.Spacing4),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = AppColors.MainGreen.Green700,
                    maxLines = 1
                )
            }
            if (role == PlaceVoteRole.HOST && onRemove != null) {
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
private fun SelectedPlacesSummary(
    title: String,
    names: List<String>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.testTag("place-selected-summary"),
        shape = RoundedCornerShape(AppDimens.ButtonCornerRadius),
        color = AppColors.Monotone.White,
        border = BorderStroke(0.6.dp, AppColors.Monotone.Gray100)
    ) {
        Column(modifier = Modifier.padding(AppDimens.Spacing16)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = AppColors.Monotone.SoftBlack,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier
                    .padding(top = AppDimens.Spacing8)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                names.forEach { name ->
                    Text(
                        text = name,
                        modifier = Modifier
                            .background(AppColors.MainGreen.Green900.copy(alpha = 0.08f), CircleShape)
                            .padding(horizontal = 10.dp, vertical = AppDimens.Spacing4),
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.MainGreen.Green700,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun PlaceVoteParticipantPreview() {
    JigmnnagaTheme {
        PlaceVoteScreen(
            state = PlaceVoteUiState(
                role = PlaceVoteRole.PARTICIPANT,
                searchQuery = "",
                candidates = listOf(
                    PlaceCandidateUiModel("gangnam", "강남역", "2호선, 신분당선·서울특별시 강남구 역삼동"),
                    PlaceCandidateUiModel("jamsil", "잠실역", "2호선, 8호선·서울특별시 송파구 잠실동")
                ),
                selectedCandidateIds = setOf("gangnam"),
                title = "중간고사 카공 모임",
                question = "어디서 만날까요?",
                listLabel = "희망 위치 선택",
                submitLabel = "저장",
                progress = PlaceVoteProgressUiState(
                    members = listOf(
                        PlaceVoteProgressMemberUiModel("태"),
                        PlaceVoteProgressMemberUiModel("하"),
                        PlaceVoteProgressMemberUiModel("나", isMe = true)
                    ),
                    title = "1명 선택 완료",
                    subtitle = "2명 더 선택하면 돼요!",
                    statusLabel = "장소 선택중"
                )
            ),
            onBackClick = {},
            onSearchQueryChange = {},
            onCandidateSelectionChange = {},
            onSubmitSelection = {},
            onProgressClick = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun PlaceVoteHostVotePreview() {
    JigmnnagaTheme {
        PlaceVoteScreen(
            state = PlaceVoteUiState(
                role = PlaceVoteRole.HOST,
                searchQuery = "",
                candidates = listOf(
                    PlaceCandidateUiModel(
                        "gangnam", "강남역", "2호선, 신분당선·서울특별시 강남구 역삼동",
                        voteLabel = "3/3명", isVoteComplete = true
                    ),
                    PlaceCandidateUiModel(
                        "jamsil", "잠실역", "2호선, 8호선·서울특별시 송파구 잠실동",
                        voteLabel = "0/3명"
                    )
                ),
                selectedCandidateIds = emptySet(),
                title = "일정 조율",
                question = "어디서 만날까요?",
                submitLabel = "저장"
            ),
            onBackClick = {},
            onSearchQueryChange = {},
            onCandidateSelectionChange = {},
            onSubmitSelection = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun PlaceVoteSearchPreview() {
    JigmnnagaTheme {
        PlaceVoteScreen(
            state = PlaceVoteUiState(
                role = PlaceVoteRole.HOST,
                searchQuery = "",
                candidates = emptyList(),
                selectedCandidateIds = setOf("gangnam", "jamsil"),
                title = "중간고사 카공 모임",
                question = "어디서 만날까요?",
                listLabel = "위치 선택·최대 5개",
                submitLabel = "변경사항 저장",
                submitEnabled = true,
                showSearchInput = true,
                searchHint = "역 이름 검색...",
                selectedSummaryTitle = "선택된 장소 (2개)",
                selectedPlaceNames = listOf("강남역", "잠실역")
            ),
            onBackClick = {},
            onSearchQueryChange = {},
            onCandidateSelectionChange = {},
            onSubmitSelection = {}
        )
    }
}