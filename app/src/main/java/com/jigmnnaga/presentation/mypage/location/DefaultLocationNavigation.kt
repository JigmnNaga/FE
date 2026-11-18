package com.jigmnnaga.presentation.mypage.location

import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.jigmnnaga.core.navigation.AppNavigationGraph
import com.jigmnnaga.core.navigation.AppNavigationOptions
import com.jigmnnaga.core.navigation.AppNavigator
import com.jigmnnaga.di.MemberLocationEntryPoint
import com.jigmnnaga.di.PlaceSearchEntryPoint
import com.jigmnnaga.presentation.common.login.LoginRoute
import com.jigmnnaga.presentation.home.HomeRoute
import dagger.hilt.android.EntryPointAccessors

/**
 * 기본 위치 설정 화면의 네비게이션 그래프.
 * MainActivity의 AppNavHost에 등록하고, 마이페이지의 "기본 위치 설정" 줄이 이 목적지로 이동한다.
 */
object DefaultLocationNavigationGraph : AppNavigationGraph {
    override fun register(builder: NavGraphBuilder, navigator: AppNavigator) {
        builder.composable<DefaultLocationRoute> {
            val applicationContext = LocalContext.current.applicationContext
            val viewModel: DefaultLocationViewModel = viewModel(
                factory = remember(applicationContext) {
                    viewModelFactory {
                        initializer {
                            DefaultLocationViewModel(
                                memberLocationRepository = EntryPointAccessors
                                    .fromApplication(applicationContext, MemberLocationEntryPoint::class.java)
                                    .memberLocationRepository(),
                                placeSearchRepository = EntryPointAccessors
                                    .fromApplication(applicationContext, PlaceSearchEntryPoint::class.java)
                                    .placeSearchRepository()
                            )
                        }
                    }
                }
            )
            DefaultLocationRoute(
                viewModel = viewModel,
                onBackClick = { navigator.navigateBack() },
                onLoginRequired = {
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
