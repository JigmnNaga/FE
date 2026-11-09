package com.jigmnnaga.core.navigation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import org.junit.Assert.assertTrue
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
                    TextButton(onClick = { navigator.navigate(NavigationTestRoute.Details("system-back")) }) {
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

    @Test
    fun featureGraphCanRegisterTypedDestinationAndPassItsArgument() {
        val graph = AppNavigationGraph { builder, navigator ->
            builder.composable<NavigationTestRoute.Home> {
                TextButton(
                    onClick = { navigator.navigate(NavigationTestRoute.Details("test-value")) }
                ) {
                    Text("Open argument test")
                }
            }
            builder.composable<NavigationTestRoute.Details> { entry ->
                val route = entry.toRoute<NavigationTestRoute.Details>()
                Text("Received ${route.value}")
            }
        }

        composeRule.setContent {
            AppNavHost(
                startDestination = NavigationTestRoute.Home,
                graphs = listOf(graph)
            )
        }

        composeRule.onNodeWithText("Open argument test").performClick()
        composeRule.onNodeWithText("Received test-value").assertIsDisplayed()
    }

    @Test
    fun singleTopNavigationDoesNotAddDuplicateRoute() {
        var poppedBackToHome = false
        composeRule.setContent {
            AppNavHost(startDestination = NavigationTestRoute.Home) { navigator ->
                composable<NavigationTestRoute.Home> {
                    TextButton(
                        onClick = { navigator.navigate(NavigationTestRoute.Middle) }
                    ) {
                        Text("Open middle")
                    }
                }
                composable<NavigationTestRoute.Middle> {
                    Column {
                        TextButton(onClick = { navigator.navigateSingleTop(NavigationTestRoute.Middle) }) {
                            Text("Open same route")
                        }
                        TextButton(onClick = { poppedBackToHome = navigator.navigateBack() }) {
                            Text("Pop to previous")
                        }
                    }
                }
            }
        }

        composeRule.onNodeWithText("Open middle").performClick()
        composeRule.onNodeWithText("Open same route").performClick()
        composeRule.onNodeWithText("Pop to previous").performClick()
        composeRule.onNodeWithText("Open middle").assertIsDisplayed()
        composeRule.runOnIdle { assertTrue(poppedBackToHome) }
    }

    @Test
    fun navigateBackToPopsIntermediateRoutes() {
        composeRule.setContent {
            AppNavHost(startDestination = NavigationTestRoute.Home) { navigator ->
                composable<NavigationTestRoute.Home> {
                    TextButton(onClick = { navigator.navigate(NavigationTestRoute.Middle) }) {
                        Text("Open middle")
                    }
                }
                composable<NavigationTestRoute.Middle> {
                    TextButton(onClick = { navigator.navigate(NavigationTestRoute.Details("back-stack")) }) {
                        Text("Open details")
                    }
                }
                composable<NavigationTestRoute.Details> {
                    TextButton(onClick = { navigator.navigateBackTo(NavigationTestRoute.Home) }) {
                        Text("Back to home")
                    }
                }
            }
        }

        composeRule.onNodeWithText("Open middle").performClick()
        composeRule.onNodeWithText("Open details").performClick()
        composeRule.onNodeWithText("Back to home").performClick()
        composeRule.onNodeWithText("Open middle").assertIsDisplayed()
    }

    @Test
    fun saveableScreenStateSurvivesNavigationAndReturn() {
        composeRule.setContent {
            AppNavHost(startDestination = NavigationTestRoute.Home) { navigator ->
                composable<NavigationTestRoute.Home> {
                    var count by rememberSaveable { mutableIntStateOf(0) }
                    Column {
                        TextButton(onClick = { count++ }) { Text("Count $count") }
                        TextButton(onClick = { navigator.navigate(NavigationTestRoute.Middle) }) {
                            Text("Open state destination")
                        }
                    }
                }
                composable<NavigationTestRoute.Middle> {
                    Text("State destination")
                }
            }
        }

        composeRule.onNodeWithText("Count 0").performClick()
        composeRule.onNodeWithText("Open state destination").performClick()
        composeRule.runOnIdle { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.onNodeWithText("Count 1").assertIsDisplayed()
    }

    @Test
    fun popUpToCanSaveAndRestoreFeatureState() {
        composeRule.setContent {
            AppNavHost(startDestination = NavigationTestRoute.Home) { navigator ->
                composable<NavigationTestRoute.Home> {
                    TextButton(onClick = { navigator.navigate(NavigationTestRoute.Middle) }) {
                        Text("Open restorable feature")
                    }
                }
                composable<NavigationTestRoute.Middle> {
                    var count by rememberSaveable { mutableIntStateOf(0) }
                    Column {
                        TextButton(onClick = { count++ }) { Text("Restored count $count") }
                        TextButton(
                            onClick = {
                                navigator.navigate(
                                    destination = NavigationTestRoute.Details("save-state"),
                                    options = AppNavigationOptions(
                                        popUpTo = NavigationTestRoute.Home,
                                        saveState = true
                                    )
                                )
                            }
                        ) {
                            Text("Open temporary destination")
                        }
                    }
                }
                composable<NavigationTestRoute.Details> {
                    TextButton(
                        onClick = {
                            navigator.navigateSingleTop(
                                destination = NavigationTestRoute.Middle,
                                restoreState = true
                            )
                        }
                    ) {
                        Text("Restore feature")
                    }
                }
            }
        }

        composeRule.onNodeWithText("Open restorable feature").performClick()
        composeRule.onNodeWithText("Restored count 0").performClick()
        composeRule.onNodeWithText("Open temporary destination").performClick()
        composeRule.onNodeWithText("Restore feature").performClick()
        composeRule.onNodeWithText("Restored count 1").assertIsDisplayed()
    }

    private sealed interface NavigationTestRoute {
        @Serializable
        data object Home : NavigationTestRoute

        @Serializable
        data object Middle : NavigationTestRoute

        @Serializable
        data class Details(val value: String) : NavigationTestRoute
    }
}
