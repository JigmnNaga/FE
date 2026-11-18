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
class RouteScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    /** 화면이 알린 사실을 [RouteUiState]의 전환 규칙으로 되먹여, 실제 ViewModel과 같은 흐름을 만든다. */
    private fun setScreen(
        initial: RouteUiState,
        onArrived: () -> Unit = {},
        onOpenStartPicker: () -> Unit = {}
    ) {
        val state = mutableStateOf(initial)
        composeRule.setContent {
            JigmnnagaTheme {
                RouteScreen(
                    state = state.value,
                    onBackClick = {},
                    onCalendarClick = {},
                    onPlaceClick = {},
                    onMoreClick = {},
                    onChatTabClick = {},
                    onMyCardClick = { state.value = state.value.openRouteSelection() },
                    onSelectTransport = { state.value = state.value.selectTransport(it) },
                    onToggleOption = { state.value = state.value.toggleOptionExpanded(it) },
                    onStartMoving = { state.value = state.value.startMoving() },
                    onToggleMyRoute = { state.value = state.value.toggleMyRouteExpanded() },
                    onArrive = {
                        onArrived()
                        state.value = state.value.arrive()
                    },
                    onMyLocationClick = {},
                    onOpenStartPicker = onOpenStartPicker
                )
            }
        }
    }

    @Test
    fun overviewShowsMembersAndDisabledLeaveButton() {
        setScreen(RouteSampleData.overview)
        composeRule.onNodeWithText("멤버 현황").assertExists()
        composeRule.onNodeWithText("이동 경로를 설정해주세요.").assertExists()
        composeRule.onNodeWithTag("route-member-2").assertExists()
        composeRule.onNodeWithTag("route-action-button").assertIsNotEnabled()
        composeRule.onNodeWithText("지금 나가기").assertExists()
    }

    @Test
    fun myCardOpensRouteSelectionWithAllOptions() {
        setScreen(RouteSampleData.overview)
        composeRule.onNodeWithTag("route-my-card").performClick()
        composeRule.onNodeWithTag("route-option-BUS").assertExists()
        composeRule.onNodeWithTag("route-option-SUBWAY").assertExists()
        composeRule.onNodeWithTag("route-option-TAXI").assertExists()
        composeRule.onNodeWithText("지금 출발하기").assertExists()
    }

    @Test
    fun startButtonStaysDisabledUntilATransportIsChosen() {
        setScreen(RouteSampleData.selecting)
        composeRule.onNodeWithTag("route-action-button").assertIsNotEnabled()
        composeRule.onNodeWithTag("route-option-BUS").performClick()
        composeRule.onNodeWithTag("route-option-BUS").assertIsSelected()
        composeRule.onNodeWithTag("route-time-summary").assertExists()
        composeRule.onNodeWithTag("route-action-button").assertIsEnabled()
    }

    @Test
    fun destinationCardShowsStartLocationLinkAndReportsClick() {
        var opened = 0
        setScreen(RouteSampleData.selecting, onOpenStartPicker = { opened++ })
        composeRule.onNodeWithTag("route-start-location-link").assertExists()
        composeRule.onNodeWithText("출발 위치 선택").assertExists()
        composeRule.onNodeWithTag("route-start-location-link").performClick()
        assertEquals(1, opened)
    }

    @Test
    fun destinationCardShowsChosenStartLocationName() {
        val home = RouteStartLocation(
            id = "basic-1",
            kind = RouteStartKind.BASIC,
            name = "집",
            latitude = 37.5,
            longitude = 127.0
        )
        setScreen(RouteSampleData.selecting.copy(startLocation = home))
        composeRule.onNodeWithText("집에서 출발").assertExists()
    }

    @Test
    fun arrowTogglesStepDetailWithoutChangingSelection() {
        setScreen(RouteSampleData.selecting)
        composeRule.onNodeWithTag("route-option-toggle-SUBWAY").performClick()
        composeRule.onNodeWithTag("route-option-detail-SUBWAY").assertExists()
        composeRule.onNodeWithText("지하철 이동 경로").assertExists()
        composeRule.onNodeWithTag("route-action-button").assertIsNotEnabled()
        composeRule.onNodeWithTag("route-option-toggle-SUBWAY").performClick()
        composeRule.onNodeWithTag("route-option-detail-SUBWAY").assertDoesNotExist()
    }

    @Test
    fun startingMovesToMovingScreen() {
        setScreen(RouteSampleData.selecting)
        composeRule.onNodeWithTag("route-option-BUS").performClick()
        composeRule.onNodeWithTag("route-action-button").performClick()
        composeRule.onNodeWithText("멤버 이동 현황").assertExists()
        composeRule.onNodeWithText("나 · 버스 이동 중").assertExists()
        composeRule.onNodeWithTag("route-action-button").assertIsNotEnabled()
        composeRule.onNodeWithText("이동 중").assertExists()
    }

    @Test
    fun myRouteCardExpandsAndFolds() {
        setScreen(RouteSampleData.moving)
        composeRule.onNodeWithTag("route-my-route-detail").assertDoesNotExist()
        composeRule.onNodeWithTag("route-my-route-toggle").performClick()
        composeRule.onNodeWithTag("route-my-route-detail").assertExists()
        composeRule.onNodeWithText("내 이동 경로").assertExists()
        composeRule.onNodeWithTag("route-my-route-toggle").performClick()
        composeRule.onNodeWithTag("route-my-route-detail").assertDoesNotExist()
    }

    @Test
    fun arriveButtonWorksOnlyNearArrival() {
        var arrivedCount = 0
        setScreen(RouteSampleData.nearArrival, onArrived = { arrivedCount++ })
        composeRule.onNodeWithTag("route-action-button").assertIsEnabled().performClick()
        composeRule.runOnIdle { assertEquals(1, arrivedCount) }
        composeRule.onNodeWithTag("route-action-button").assertIsNotEnabled()
    }
}
