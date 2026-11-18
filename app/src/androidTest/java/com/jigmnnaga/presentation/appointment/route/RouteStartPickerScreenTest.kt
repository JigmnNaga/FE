package com.jigmnnaga.presentation.appointment.route

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RouteStartPickerScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val home = RouteStartLocation(
        id = "basic-1",
        kind = RouteStartKind.BASIC,
        name = "집",
        detail = "서울 강남구 테헤란로 123",
        latitude = 37.5,
        longitude = 127.0
    )
    private val gangnam = RouteStartLocation(
        id = "search-1",
        kind = RouteStartKind.SEARCH,
        name = "강남역",
        detail = "2호선·강남대로 396",
        latitude = 37.49,
        longitude = 127.02
    )

    private fun setPicker(
        initial: RouteStartPickerUiState,
        onConfirm: () -> Unit = {},
        onCurrent: () -> Unit = {}
    ) {
        val state = mutableStateOf(initial)
        composeRule.setContent {
            JigmnnagaTheme {
                RouteStartPickerScreen(
                    state = state.value,
                    onBackClick = {},
                    onSelect = { state.value = state.value.copy(selected = it) },
                    onCurrentLocationClick = onCurrent,
                    onQueryChange = { state.value = state.value.copy(query = it) },
                    onConfirm = onConfirm
                )
            }
        }
    }

    @Test
    fun showsBasicLocationsWithCountAndDisabledConfirmButton() {
        setPicker(RouteStartPickerUiState(isOpen = true, basicLocations = listOf(home)))
        composeRule.onNodeWithText("내 기본 위치").assertExists()
        composeRule.onNodeWithText("1 / 3").assertExists()
        composeRule.onNodeWithText("집").assertExists()
        composeRule.onNodeWithText("서울 강남구 테헤란로 123").assertExists()
        composeRule.onNodeWithTag("route-start-option-current").assertExists()
        composeRule.onNodeWithText("그 외 출발 위치 선택").assertExists()
        composeRule.onNodeWithTag("route-start-confirm").assertIsNotEnabled()
    }

    @Test
    fun selectingALocationEnablesConfirmButton() {
        var confirmed = 0
        setPicker(RouteStartPickerUiState(isOpen = true, basicLocations = listOf(home)), onConfirm = { confirmed++ })
        composeRule.onNodeWithTag("route-start-option-basic-1").performClick()
        composeRule.onNodeWithTag("route-start-option-basic-1").assertIsSelected()
        composeRule.onNodeWithTag("route-start-confirm").assertIsEnabled().performClick()
        assertEquals(1, confirmed)
    }

    @Test
    fun showsSearchResultsAndEmptyBasicHint() {
        setPicker(
            RouteStartPickerUiState(isOpen = true, query = "강남역", searchResults = listOf(gangnam))
        )
        composeRule.onNodeWithTag("route-start-basic-empty").assertExists()
        composeRule.onNodeWithTag("route-start-option-search-1").assertExists()
        composeRule.onNodeWithText("2호선·강남대로 396").assertExists()
    }

    @Test
    fun currentLocationRowReportsClickAndShowsDeniedHint() {
        var clicks = 0
        setPicker(
            RouteStartPickerUiState(isOpen = true, currentStatus = RouteCurrentLocationStatus.PERMISSION_DENIED),
            onCurrent = { clicks++ }
        )
        composeRule.onNodeWithText("위치 권한을 허용하면 현재 위치에서 출발할 수 있어요.").assertExists()
        composeRule.onNodeWithTag("route-start-option-current").performClick()
        assertEquals(1, clicks)
    }
}
