package com.jigmnnaga.presentation.mypage

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
import com.jigmnnaga.di.MypageEntryPoint
import com.jigmnnaga.presentation.common.login.LoginRoute
import com.jigmnnaga.presentation.home.HomeRoute
import com.jigmnnaga.presentation.mypage.account.LogoutCompleteRoute
import com.jigmnnaga.presentation.mypage.account.WithdrawalGuideRoute
import com.jigmnnaga.presentation.mypage.help.HelpSupportRoute
import com.jigmnnaga.presentation.mypage.location.DefaultLocationRoute
import com.jigmnnaga.presentation.mypage.notification.NotificationSettingsRoute
import com.jigmnnaga.presentation.mypage.profile.ProfileEditRoute
import com.jigmnnaga.presentation.mypage.profile.ProfileEditViewModel
import com.jigmnnaga.presentation.mypage.sharing.LocationSharingSettingsRoute
import dagger.hilt.android.EntryPointAccessors

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
                onNotificationSettingsClick = { navigator.navigate(NotificationSettingsRoute) },
                onHelpClick = { navigator.navigate(HelpSupportRoute) },
                onDefaultLocationClick = { navigator.navigate(DefaultLocationRoute) },
                onLocationSharingSettingsClick = { navigator.navigate(LocationSharingSettingsRoute) },
                onLogoutClick = { navigator.navigate(LogoutCompleteRoute) },
                onWithdrawConfirmed = { navigator.navigate(WithdrawalGuideRoute) }
            )
        }
        builder.composable<ProfileEditRoute> {
            val applicationContext = LocalContext.current.applicationContext
            val viewModel: ProfileEditViewModel = viewModel(
                factory = remember(applicationContext) {
                    viewModelFactory {
                        initializer {
                            ProfileEditViewModel(
                                repository = EntryPointAccessors
                                    .fromApplication(applicationContext, MypageEntryPoint::class.java)
                                    .mypageRepository()
                            )
                        }
                    }
                }
            )
            ProfileEditRoute(
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
