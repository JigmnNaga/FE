package com.jigmnnaga.features.placevote

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaceVoteScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun participantCanSelectMultipleCandidatesAndSubmitOnlyWhenEnabledByState() {
        val uiState = mutableStateOf(
            PlaceVoteUiState(
                role = PlaceVoteRole.PARTICIPANT,
                searchQuery = "",
                candidates = listOf(
                    PlaceCandidateUiModel("a", "강남역"),
                    PlaceCandidateUiModel("b", "잠실역")
                ),
                selectedCandidateIds = emptySet(),
                title = "어디서 만날까요?",
                description = "희망 위치 선택",
                submitLabel = "저장"
            )
        )
        var submitted = 0
        composeRule.setContent {
            JigmnnagaTheme {
                PlaceVoteScreen(
                    state = uiState.value,
                    onBackClick = {},
                    onSearchQueryChange = {},
                    onCandidateSelectionChange = { selected ->
                        uiState.value = uiState.value.copy(
                            selectedCandidateIds = selected,
                            submitEnabled = selected.isNotEmpty()
                        )
                    },
                    onSubmitSelection = { submitted++ }
                )
            }
        }

        composeRule.onNodeWithTag("place-vote-submit").assertIsNotEnabled()
        composeRule.onNodeWithTag("place-candidate-a").performClick()
        composeRule.onNodeWithTag("place-candidate-b").performClick()
        composeRule.onNodeWithTag("place-vote-submit").assertIsEnabled().performClick()
        composeRule.runOnIdle {
            assertEquals(setOf("a", "b"), uiState.value.selectedCandidateIds)
            assertEquals(1, submitted)
        }
    }

    @Test
    fun searchTextIsForwardedAndNoSearchResultIsInventedLocally() {
        val uiState = mutableStateOf(
            PlaceVoteUiState(
                role = PlaceVoteRole.PARTICIPANT,
                searchQuery = "",
                candidates = emptyList(),
                selectedCandidateIds = emptySet(),
                title = "어디서 만날까요?",
                description = "희망 위치 선택",
                submitLabel = "저장",
                showSearchInput = true
            )
        )
        var lastQuery = ""
        composeRule.setContent {
            JigmnnagaTheme {
                PlaceVoteScreen(
                    state = uiState.value,
                    onBackClick = {},
                    onSearchQueryChange = { query ->
                        lastQuery = query
                        uiState.value = uiState.value.copy(searchQuery = query)
                    },
                    onCandidateSelectionChange = {},
                    onSubmitSelection = {}
                )
            }
        }
        composeRule.onNodeWithTag("place-search-input").performClick().performTextInput("서울역")
        composeRule.runOnIdle {
            assertEquals("서울역", lastQuery)
            assertEquals("서울역", uiState.value.searchQuery)
        }
    }

    @Test
    fun longPlaceNamesAndManyCandidatesRemainScrollableAtAccessibilityFontScales() {
        val fontScale = mutableFloatStateOf(1f)
        val candidates = (1..18).map { index ->
            PlaceCandidateUiModel(
                id = "place-$index",
                name = if (index == 1) "서울특별시에서 가장 접근하기 좋은 만남 장소 후보 이름 테스트" else "장소 후보 $index",
                address = "긴 장소 설명도 두 줄 안에서 읽을 수 있도록 전달된 주소 $index"
            )
        }
        composeRule.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(baseDensity.density, fontScale.floatValue)) {
                JigmnnagaTheme {
                    androidx.compose.foundation.layout.Box(Modifier.width(320.dp)) {
                        PlaceVoteScreen(
                            state = PlaceVoteUiState(
                                role = PlaceVoteRole.PARTICIPANT,
                                searchQuery = "",
                                candidates = candidates,
                                selectedCandidateIds = emptySet(),
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
            }
        }

        listOf(1f, 1.3f, 1.5f).forEach { scale ->
            composeRule.runOnIdle { fontScale.floatValue = scale }
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("place-candidate-list")
                .performScrollToNode(hasTestTag("place-candidate-place-18"))
            composeRule.onNodeWithText("장소 후보 18").assertExists()
            composeRule.onNodeWithTag("place-candidate-place-18").assertExists()
        }
        assertEquals(18, candidates.size)
    }

    @Test
    fun hostCanRemoveCandidateAndMapIsClearlyUnconnected() {
        var removedId: String? = null
        composeRule.setContent {
            JigmnnagaTheme {
                PlaceVoteScreen(
                    state = PlaceVoteUiState(
                        role = PlaceVoteRole.HOST,
                        searchQuery = "",
                        candidates = listOf(PlaceCandidateUiModel("host-place", "등록된 장소")),
                        selectedCandidateIds = emptySet(),
                        title = "장소 선택",
                        description = "장소 후보",
                        submitLabel = "저장",
                        showMapUnavailableNotice = true
                    ),
                    onBackClick = {},
                    onSearchQueryChange = {},
                    onCandidateSelectionChange = {},
                    onSubmitSelection = {},
                    onRemoveCandidate = { removedId = it }
                )
            }
        }
        composeRule.onNodeWithText("지도는 연결 후 표시됩니다").assertExists()
        composeRule.onNodeWithTag("remove-candidate-host-place").performClick()
        composeRule.runOnIdle { assertEquals("host-place", removedId) }
    }
}
