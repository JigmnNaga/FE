package com.jigmnnaga.core.navigation

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import org.junit.Rule
import org.junit.Test

class AppNavHostTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun typeSafeDestinationNavigatesAndSystemBackReturnsToPreviousDestination() {
        composeRule.setContent {
            AppNavHost(startDestination = NavigationTestRoute.Home) { navigator ->
                composable<NavigationTestRoute.Home> {
                    TextButton(onClick = { navigator.navigate(NavigationTestRoute.Details) }) {
                        Text("Open test destination")
                    }
                }
                composable<NavigationTestRoute.Details> {
                    Text("Test destination")
                }
            }
        }

        composeRule.onNodeWithText("Open test destination").performClick()
        composeRule.onNodeWithText("Test destination").assertIsDisplayed()

        composeRule.runOnIdle {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }

        composeRule.onNodeWithText("Open test destination").assertIsDisplayed()
    }

    private sealed interface NavigationTestRoute {
        @Serializable
        data object Home : NavigationTestRoute

        @Serializable
        data object Details : NavigationTestRoute
    }
}
