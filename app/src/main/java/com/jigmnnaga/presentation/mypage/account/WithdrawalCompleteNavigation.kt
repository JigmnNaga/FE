package com.jigmnnaga.presentation.mypage.account

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.jigmnnaga.core.navigation.AppNavigationGraph
import com.jigmnnaga.core.navigation.AppNavigator
import com.jigmnnaga.core.navigation.AppNavigationOptions
import com.jigmnnaga.presentation.common.login.LoginRoute
import com.jigmnnaga.presentation.home.HomeRoute

/** 회원탈퇴 완료 화면 네비게이션 그래프. */
object WithdrawalCompleteNavigationGraph : AppNavigationGraph {
    override fun register(builder: NavGraphBuilder, navigator: AppNavigator) {
        builder.composable<WithdrawalCompleteRoute> {
            WithdrawalCompleteRoute(
                onLeaveClick = {
                    navigator.navigate(
                        destination = LoginRoute,
                        options = AppNavigationOptions(
                            popUpTo = HomeRoute,
                            inclusive = true,
                            launchSingleTop = true
                        )
                    )
                }
            )
        }
    }
}
