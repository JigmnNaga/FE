package com.jigmnnaga.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController

/**
 * Hosts destinations registered by feature navigation modules.
 *
 * The start destination and route types are supplied by the app/feature layer so this foundation
 * does not choose an authentication flow or product route policy. Destinations should pass
 * [AppNavigator] callbacks to screen composables instead of exposing a [NavHostController] there.
 */
@Composable
fun AppNavHost(
    startDestination: Any,
    modifier: Modifier = Modifier,
    destinations: NavGraphBuilder.(AppNavigator) -> Unit
) {
    val navController = rememberNavController()
    val navigator = remember(navController) { AppNavigator(navController) }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        destinations(navigator)
    }
}

/** Navigation actions that feature screens can receive as callbacks. */
class AppNavigator internal constructor(
    private val navController: NavHostController
) {
    /**
     * Navigate to a string route or a type-safe serializable route supported by Navigation Compose.
     */
    fun navigate(destination: Any) {
        navController.navigate(destination)
    }

    /** Returns whether a previous destination was available and popped. */
    fun navigateBack(): Boolean = navController.popBackStack()
}
