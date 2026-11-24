package com.jigmnnaga.presentation.mypage.account

import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.jigmnnaga.core.navigation.AppNavigationGraph
import com.jigmnnaga.core.navigation.AppNavigator
import com.jigmnnaga.di.AccountEntryPoint
import dagger.hilt.android.EntryPointAccessors

/** 방장 회원탈퇴 안내 화면 네비게이션 그래프. */
object WithdrawalGuideNavigationGraph : AppNavigationGraph {
    override fun register(builder: NavGraphBuilder, navigator: AppNavigator) {
        builder.composable<WithdrawalGuideRoute> {
            val applicationContext = LocalContext.current.applicationContext
            val viewModel: AccountViewModel = viewModel(
                factory = remember(applicationContext) {
                    viewModelFactory {
                        initializer {
                            val entryPoint = EntryPointAccessors
                                .fromApplication(applicationContext, AccountEntryPoint::class.java)
                            AccountViewModel(
                                authRepository = entryPoint.authRepository(),
                                myAppointmentApi = entryPoint.myAppointmentApi(),
                                tokenManager = entryPoint.tokenManager(),
                                appointmentDetailRepository = entryPoint.appointmentDetailRepository()
                            )
                        }
                    }
                }
            )
            WithdrawalGuideRoute(
                viewModel = viewModel,
                onBackClick = { navigator.navigateBack() },
                onWithdrawComplete = { navigator.navigate(WithdrawalCompleteRoute) }
            )
        }
    }
}
