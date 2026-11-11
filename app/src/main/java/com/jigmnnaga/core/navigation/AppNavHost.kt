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
 *
 * 이동 정책
 * - 시스템 UI: MainActivity가 edge-to-edge를 켠다. 이 호스트는 Scaffold나 inset을 입히지 않으며,
 *   상태바·내비게이션 바·키보드(IME) inset은 각 화면(부모 Screen)이 처리한다. BottomNavigationBar 같은
 *   공통 컴포넌트는 자기 영역만 담당한다.
 * - 약속방: 목적지는 약속 ID만 인자로 받는다. 방장/참여자 화면은 약속 상세 조회의 내 역할(myRole)로
 *   화면 안에서 결정하고, 역할을 Route 인자로 넘기지 않는다(역할이 바뀌어도 오래된 값이 남지 않도록).
 * - 초대 링크: 링크에서 꺼낸 초대 코드를 초대 수락 목적지의 인자로 전달한다. 링크 형식과 앱 진입
 *   설정은 초대 수락 기능(#49)에서 확정한다.
 * - 로그인이 필요한 목적지: 로그인 후 원래 가려던 목적지로 돌아온다. 복귀 연결은 #49와 카카오 로그인
 *   기능(#24)에서 구현한다.
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