package com.jigmnnaga.presentation.mypage.account

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.jigmnnaga.core.navigation.AppNavigationGraph
import com.jigmnnaga.core.navigation.AppNavigator

/** 방장 회원탈퇴 안내 화면 네비게이션 그래프. */
object WithdrawalGuideNavigationGraph : AppNavigationGraph {
    override fun register(builder: NavGraphBuilder, navigator: AppNavigator) {
        builder.composable<WithdrawalGuideRoute> {
            WithdrawalGuideRoute(
                onBackClick = { navigator.navigateBack() },
                onWithdrawComplete = { navigator.navigate(WithdrawalCompleteRoute) }
            )
        }
    }
}
