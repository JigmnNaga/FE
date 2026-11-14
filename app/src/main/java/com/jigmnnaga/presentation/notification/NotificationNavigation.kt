package com.jigmnnaga.presentation.notification

import android.content.Context
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.jigmnnaga.core.navigation.AppNavigationGraph
import com.jigmnnaga.core.navigation.AppNavigator
import com.jigmnnaga.core.navigation.LoginReturnRegistry
import com.jigmnnaga.di.NotificationEntryPoint
import com.jigmnnaga.presentation.appointment.AppointmentRoomDestination
import com.jigmnnaga.presentation.common.login.LoginRoute
import dagger.hilt.android.EntryPointAccessors
import kotlinx.serialization.Serializable

@Serializable
data object NotificationDestination

/** 알림 기능이 소유하는 네비게이션 그래프. MainActivity의 AppNavHost에 등록한다. */
object NotificationNavigationGraph : AppNavigationGraph {
    override fun register(builder: NavGraphBuilder, navigator: AppNavigator) {
        builder.composable<NotificationDestination> {
            val applicationContext = LocalContext.current.applicationContext
            val viewModel: NotificationViewModel = viewModel(
                factory = remember { notificationViewModelFactory(applicationContext) }
            )
            NotificationRoute(
                viewModel = viewModel,
                onBack = { navigator.navigateBack() },
                onOpenAppointment = { appointmentId ->
                    navigator.navigate(AppointmentRoomDestination(appointmentId))
                },
                onLoginRequired = { resumeAfterLogin ->
                    LoginReturnRegistry.request(resumeAfterLogin)
                    navigator.navigate(LoginRoute)
                }
            )
        }
    }

    private fun notificationViewModelFactory(context: Context) = viewModelFactory {
        initializer {
            val entryPoint = EntryPointAccessors.fromApplication(context, NotificationEntryPoint::class.java)
            NotificationViewModel(entryPoint.notificationRepository())
        }
    }
}
