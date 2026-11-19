package com.jigmnnaga.presentation.mypage.sharing

import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.jigmnnaga.core.navigation.AppNavigationGraph
import com.jigmnnaga.core.navigation.AppNavigator
import com.jigmnnaga.di.LocationSharingSettingsEntryPoint
import com.jigmnnaga.presentation.appointment.route.hasLocationPermission
import dagger.hilt.android.EntryPointAccessors

/**
 * 위치 공유 설정 화면의 네비게이션 그래프.
 * MainActivity의 AppNavHost에 등록하고, 마이페이지의 "위치 공유 설정" 줄이 이 목적지로 이동한다.
 */
object LocationSharingSettingsNavigationGraph : AppNavigationGraph {
    override fun register(builder: NavGraphBuilder, navigator: AppNavigator) {
        builder.composable<LocationSharingSettingsRoute> {
            val applicationContext = LocalContext.current.applicationContext
            val viewModel: LocationSharingSettingsViewModel = viewModel(
                factory = remember(applicationContext) {
                    viewModelFactory {
                        initializer {
                            LocationSharingSettingsViewModel(
                                repository = EntryPointAccessors
                                    .fromApplication(applicationContext, LocationSharingSettingsEntryPoint::class.java)
                                    .locationSharingSettingsRepository(),
                                initialPermissionGranted = applicationContext.hasLocationPermission()
                            )
                        }
                    }
                }
            )
            LocationSharingSettingsRoute(
                viewModel = viewModel,
                onBackClick = { navigator.navigateBack() }
            )
        }
    }
}
