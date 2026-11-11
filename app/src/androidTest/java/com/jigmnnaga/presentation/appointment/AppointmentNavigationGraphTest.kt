package com.jigmnnaga.presentation.appointment

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.composable
import com.jigmnnaga.core.navigation.AppNavHost
import com.jigmnnaga.core.navigation.AppNavigationGraph
import kotlinx.serialization.Serializable
import org.junit.Rule
import org.junit.Test

class AppointmentNavigationGraphTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun createDestinationIsRegisteredAndBackReturnsToPreviousDestination() {
        val homeGraph = AppNavigationGraph { builder, navigator ->
            builder.composable<TestHomeDestination> {
                TextButton(onClick = { navigator.navigate(AppointmentCreateDestination) }) {
                    Text("Open create")
                }
            }
        }

        composeRule.setContent {
            AppNavHost(
                startDestination = TestHomeDestination,
                graphs = listOf(homeGraph, AppointmentNavigationGraph)
            )
        }

        composeRule.onNodeWithText("Open create").performClick()
        composeRule.onNodeWithText("Open create").assertDoesNotExist()

        composeRule.runOnIdle {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }

        composeRule.onNodeWithText("Open create").assertIsDisplayed()
    }
}

@Serializable
private data object TestHomeDestination