package com.jigmnnaga.presentation.mypage.account

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.jigmnnaga.core.navigation.AppNavigationGraph
import com.jigmnnaga.core.navigation.AppNavigator
import com.jigmnnaga.core.navigation.AppNavigationOptions
import com.jigmnnaga.presentation.home.HomeRoute

/** 로그아웃 완료 화면 네비게이션 그래프. */
object LogoutCompleteNavigationGraph : AppNavigationGraph {
    override fun register(builder: NavGraphBuilder, navigator: AppNavigator) {
        builder.composable<LogoutCompleteRoute> {
            LogoutCompleteRoute(
                onLeaveClick = {
                    navigator.navigate(
                        destination = HomeRoute,
                        options = AppNavigationOptions(
                            popUpTo = HomeRoute,
                            inclusive = false,
                            launchSingleTop = true
                        )
                    )
                }
            )
        }
    }
}
