package com.jigmnnaga.presentation.appointment

import android.content.Context
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.jigmnnaga.core.navigation.AppNavigationGraph
import com.jigmnnaga.core.navigation.AppNavigationOptions
import com.jigmnnaga.core.navigation.AppNavigator
import com.jigmnnaga.core.navigation.LoginReturnRegistry
import com.jigmnnaga.di.InviteEntryPoint
import com.jigmnnaga.presentation.appointment.create.AppointmentCreateRoute
import com.jigmnnaga.presentation.appointment.invite.AppointmentInviteRoute
import com.jigmnnaga.presentation.appointment.invite.AppointmentInviteViewModel
import com.jigmnnaga.presentation.common.login.LoginRoute
import com.jigmnnaga.presentation.common.splash.SplashRoute
import com.jigmnnaga.presentation.home.HomeRoute
import dagger.hilt.android.EntryPointAccessors
import kotlinx.serialization.Serializable

/** 약속 생성 화면(APT-001). */
@Serializable
data object AppointmentCreateDestination

/** 초대 수락 화면(APT-002). 초대 링크의 코드를 인자로 받는다. */
@Serializable
data class AppointmentInviteDestination(val inviteCode: String)

/** 약속 기능이 소유하는 이동 경로 그래프. 앱 호스트에는 이 객체만 등록한다. */
object AppointmentNavigationGraph : AppNavigationGraph {
    override fun register(builder: NavGraphBuilder, navigator: AppNavigator) {
        builder.composable<AppointmentCreateDestination> {
            AppointmentCreateRoute(onExit = { navigator.navigateBack() })
        }

        builder.composable<AppointmentInviteDestination> { entry ->
            val inviteCode = entry.toRoute<AppointmentInviteDestination>().inviteCode
            val applicationContext = LocalContext.current.applicationContext
            val viewModel: AppointmentInviteViewModel = viewModel(
                factory = remember { inviteViewModelFactory(applicationContext) }
            )
            AppointmentInviteRoute(
                viewModel = viewModel,
                inviteCode = inviteCode,
                onBack = { navigator.navigateBack() },
                onLoginRequired = { resumeInviteAfterLogin ->
                    // 로그인에 성공하면 로그인 화면이 이 일을 꺼내 실행하고 초대 화면으로 돌아온다.
                    LoginReturnRegistry.request(resumeInviteAfterLogin)
                    navigator.navigate(LoginRoute)
                },
                // TODO: 약속방 화면이 등록되면 appointmentId 약속방으로 이동하도록 바꾼다.
                onOpenChat = { navigator.navigateToHome() },
                onReturnHome = { navigator.navigateToHome() }
            )
        }
    }

    private fun AppNavigator.navigateToHome() {
        navigate(
            destination = HomeRoute,
            options = AppNavigationOptions(
                popUpTo = SplashRoute,
                inclusive = true,
                launchSingleTop = true
            )
        )
    }

    private fun inviteViewModelFactory(context: Context) = viewModelFactory {
        initializer {
            val entryPoint = EntryPointAccessors.fromApplication(context, InviteEntryPoint::class.java)
            AppointmentInviteViewModel(entryPoint.appointmentInviteRepository())
        }
    }
}
