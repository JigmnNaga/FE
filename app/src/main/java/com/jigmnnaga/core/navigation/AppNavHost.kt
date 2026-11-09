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

/**
 * A feature-owned destination graph. Feature modules keep their serializable route types and
 * destinations local, then register them with the shared host without receiving a NavController.
 */
fun interface AppNavigationGraph {
    fun register(builder: NavGraphBuilder, navigator: AppNavigator)
}

/** Hosts independently owned feature graphs under one app-level start destination. */
@Composable
fun AppNavHost(
    startDestination: Any,
    graphs: List<AppNavigationGraph>,
    modifier: Modifier = Modifier
) {
    AppNavHost(startDestination = startDestination, modifier = modifier) { navigator ->
        graphs.forEach { graph -> graph.register(this, navigator) }
    }
}

/** Options for common back-stack behavior; route values remain owned by their feature. */
data class AppNavigationOptions(
    val popUpTo: Any? = null,
    val inclusive: Boolean = false,
    val saveState: Boolean = false,
    val launchSingleTop: Boolean = false,
    val restoreState: Boolean = false
)

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

    /**
     * Navigates with explicit back-stack options. A feature passes its own serializable route
     * object; this shared layer does not define product identifiers or argument types.
     */
    fun navigate(destination: Any, options: AppNavigationOptions) {
        navController.navigate(destination) {
            options.popUpTo?.let { target ->
                popUpTo(target) {
                    inclusive = options.inclusive
                    saveState = options.saveState
                }
            }
            launchSingleTop = options.launchSingleTop
            restoreState = options.restoreState
        }
    }

    /** Avoids adding another copy of the same destination on top of the back stack. */
    fun navigateSingleTop(destination: Any, restoreState: Boolean = false) {
        navigate(
            destination = destination,
            options = AppNavigationOptions(
                launchSingleTop = true,
                restoreState = restoreState
            )
        )
    }

    /** Returns whether a previous destination was available and popped. */
    fun navigateBack(): Boolean = navController.popBackStack()

    /** Pops destinations until [destination], optionally removing it and saving its state. */
    fun navigateBackTo(
        destination: Any,
        inclusive: Boolean = false,
        saveState: Boolean = false
    ): Boolean = navController.popBackStack(destination, inclusive, saveState)
}
