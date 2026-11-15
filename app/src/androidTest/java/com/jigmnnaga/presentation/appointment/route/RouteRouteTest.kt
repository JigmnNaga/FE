package com.jigmnnaga.presentation.appointment.route

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
class RouteRouteTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun setRoute(onChatTab: () -> Unit = {}, onBack: () -> Unit = {}) {
        val viewModel = RouteViewModel(RouteSampleData.overview)
        composeRule.setContent {
            JigmnnagaTheme {
                RouteRoute(
                    viewModel = viewModel,
                    onBackClick = onBack,
                    onCalendarClick = {},
                    onPlaceClick = {},
                    onMoreClick = {},
                    onChatTabClick = onChatTab
                )
            }
        }
    }

    @Test
    fun chatTabNotifiesCaller() {
        var chatTabClicks = 0
        setRoute(onChatTab = { chatTabClicks++ })

        composeRule.onNodeWithText("채팅").performClick()

        assertEquals(1, chatTabClicks)
    }

    @Test
    fun myCardOpensRouteSelectionThroughViewModel() {
        setRoute()

        composeRule.onNodeWithTag("route-my-card").performClick()

        composeRule.onNodeWithTag("route-option-BUS").assertExists()
    }
}
