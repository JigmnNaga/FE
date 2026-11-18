package com.jigmnnaga.presentation.mypage

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.jigmnnaga.core.navigation.AppNavigationGraph
import com.jigmnnaga.core.navigation.AppNavigator
import com.jigmnnaga.presentation.mypage.location.DefaultLocationRoute

/**
 * 마이페이지 기능이 소유하는 네비게이션 그래프.
 * MainActivity의 AppNavHost에 등록한다.
 */
object MypageNavigationGraph : AppNavigationGraph {
    override fun register(builder: NavGraphBuilder, navigator: AppNavigator) {
        builder.composable<MypageRoute> {
            MypageRoute(
                onBackClick = { navigator.navigateBack() },
                onEditProfileClick = { navigator.navigate(ProfileEditRoute) },
                onDefaultLocationClick = { navigator.navigate(DefaultLocationRoute) }
            )
        }
        builder.composable<ProfileEditRoute> {
            ProfileEditRoute(
                onBackClick = { navigator.navigateBack() }
            )
        }
    }
}
