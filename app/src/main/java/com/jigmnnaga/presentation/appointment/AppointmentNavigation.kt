package com.jigmnnaga.presentation.appointment

import android.content.Context
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import com.jigmnnaga.R
import com.jigmnnaga.di.InviteEntryPoint
import com.jigmnnaga.presentation.appointment.create.AppointmentCreateRoute
import com.jigmnnaga.presentation.appointment.host.HostAppointmentRoomRoute
import com.jigmnnaga.presentation.appointment.host.HostAppointmentRoomUiState
import com.jigmnnaga.presentation.appointment.host.HostAppointmentRoomViewModel
import com.jigmnnaga.presentation.appointment.hostconfirmation.HostAppointmentConfirmationRoute
import com.jigmnnaga.presentation.appointment.hostconfirmation.HostConfirmationUiState
import com.jigmnnaga.presentation.appointment.hostconfirmation.HostConfirmationViewModel
import com.jigmnnaga.presentation.appointment.invite.AppointmentInviteRoute
import com.jigmnnaga.presentation.appointment.invite.AppointmentInviteViewModel
import com.jigmnnaga.presentation.appointment.participant.ParticipantAppointmentRoomRoute
import com.jigmnnaga.presentation.appointment.participant.ParticipantAppointmentRoomUiState
import com.jigmnnaga.presentation.appointment.participant.ParticipantAppointmentRoomViewModel
import com.jigmnnaga.presentation.appointment.participant.ParticipantAppointmentStatus
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

/** 방장 약속방(APT-004). 약속 ID를 인자로 받는다. */
@Serializable
data class HostAppointmentRoomDestination(val appointmentId: Long)

/** 참여자 약속방(APT-005). 약속 ID를 인자로 받는다. */
@Serializable
data class ParticipantAppointmentRoomDestination(val appointmentId: Long)

/** 방장 최종 확정 화면. 약속 ID를 인자로 받는다. */
@Serializable
data class HostAppointmentConfirmationDestination(val appointmentId: Long)

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

        // TODO: 약속 상세 API(시트 41·44번) 연동 시 하나의 약속방 목적지로 합치고, 내 역할(myRole)로 방장·참여자 화면을 정한다.
        // 화면 이동 연결(날짜·시간, 장소 투표, 관리)은 각 기능의 목적지가 등록될 때 함께 연결한다.
        builder.composable<HostAppointmentRoomDestination> { entry ->
            val appointmentId = entry.toRoute<HostAppointmentRoomDestination>().appointmentId
            val title = stringResource(R.string.appointment_room_default_title)
            val viewModel: HostAppointmentRoomViewModel = viewModel(
                key = "host-room-$appointmentId",
                factory = remember(appointmentId) {
                    viewModelFactory {
                        initializer {
                            HostAppointmentRoomViewModel(
                                HostAppointmentRoomUiState(title = title, subtitle = "")
                            )
                        }
                    }
                }
            )
            HostAppointmentRoomRoute(
                viewModel = viewModel,
                onBackClick = { navigator.navigateBack() },
                onCalendarClick = { /* TODO: 희망 날짜·시간 화면 등록 후 연결 */ },
                onPlaceClick = { /* TODO: 장소 투표 화면 등록 후 연결 */ },
                onMoreClick = { /* TODO: 약속 관리 화면 등록 후 연결 */ }
            )
        }

        // TODO: 약속 상세·투표 결과 API 연동 시 실제 약속 정보와 후보 목록, 내 역할(myRole)을 넘긴다.
        // 약속방에서 이 화면으로 가는 연결은 방장 약속방의 확정 진입점이 정해질 때 함께 붙인다.
        builder.composable<HostAppointmentConfirmationDestination> { entry ->
            val appointmentId = entry.toRoute<HostAppointmentConfirmationDestination>().appointmentId
            val title = stringResource(R.string.appointment_room_default_title)
            val viewModel: HostConfirmationViewModel = viewModel(
                key = "host-confirmation-$appointmentId",
                factory = remember(appointmentId) {
                    viewModelFactory {
                        initializer {
                            HostConfirmationViewModel(
                                HostConfirmationUiState(
                                    appointmentTitle = title,
                                    selectedDateTime = null,
                                    selectedPlace = null
                                )
                            )
                        }
                    }
                }
            )
            HostAppointmentConfirmationRoute(
                viewModel = viewModel,
                onExit = { navigator.navigateBack() }
            )
        }

        builder.composable<ParticipantAppointmentRoomDestination> { entry ->
            val appointmentId = entry.toRoute<ParticipantAppointmentRoomDestination>().appointmentId
            val title = stringResource(R.string.appointment_room_default_title)
            val viewModel: ParticipantAppointmentRoomViewModel = viewModel(
                key = "participant-room-$appointmentId",
                factory = remember(appointmentId) {
                    viewModelFactory {
                        initializer {
                            ParticipantAppointmentRoomViewModel(
                                ParticipantAppointmentRoomUiState(
                                    title = title,
                                    subtitle = "",
                                    appointmentStatus = ParticipantAppointmentStatus.AWAITING_CONFIRMATION
                                )
                            )
                        }
                    }
                }
            )
            ParticipantAppointmentRoomRoute(
                viewModel = viewModel,
                onBackClick = { navigator.navigateBack() },
                onCalendarClick = { /* TODO: 희망 날짜·시간 화면 등록 후 연결 */ },
                onPlaceClick = { /* TODO: 장소 투표 화면 등록 후 연결 */ },
                onManagementClick = { /* TODO: 약속 관리 화면 등록 후 연결 */ }
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
