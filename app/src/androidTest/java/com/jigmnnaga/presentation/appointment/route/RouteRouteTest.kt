package com.jigmnnaga.presentation.appointment.route

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RouteRouteTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun myCardOpensRouteSelectionThroughViewModel() {
        val viewModel = RouteViewModel(RouteSampleData.overview)
        composeRule.setContent {
            JigmnnagaTheme {
                RouteRoute(viewModel = viewModel)
            }
        }

        composeRule.onNodeWithTag("route-my-card").performClick()

        composeRule.onNodeWithTag("route-option-BUS").assertExists()
    }
}
