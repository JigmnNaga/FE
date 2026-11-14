package com.jigmnnaga.presentation.appointment

import android.content.Context
import androidx.compose.runtime.Composable
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
import com.jigmnnaga.di.AppointmentDetailEntryPoint
import com.jigmnnaga.di.InviteEntryPoint
import com.jigmnnaga.domain.model.AppointmentMemberRole
import com.jigmnnaga.presentation.appointment.detail.AppointmentDetailLoadViewModel
import com.jigmnnaga.presentation.appointment.detail.AppointmentDetailLoader
import com.jigmnnaga.presentation.appointment.detail.rememberAppointmentDetailLabels
import com.jigmnnaga.presentation.appointment.detail.toExistingAddUiState
import com.jigmnnaga.presentation.appointment.detail.toFinalConfirmationUiState
import com.jigmnnaga.presentation.appointment.detail.toHostRoomUiState
import com.jigmnnaga.presentation.appointment.detail.toMemberResponses
import com.jigmnnaga.presentation.appointment.detail.toParticipantRoomUiState
import com.jigmnnaga.presentation.appointment.create.AppointmentCreateRoute
import com.jigmnnaga.presentation.appointment.existingadd.ExistingAppointmentAddRoute
import com.jigmnnaga.presentation.appointment.existingadd.ExistingAppointmentAddViewModel
import com.jigmnnaga.presentation.appointment.finalconfirmation.AppointmentFinalConfirmationRoute
import com.jigmnnaga.presentation.appointment.finalconfirmation.AppointmentFinalConfirmationViewModel
import com.jigmnnaga.presentation.appointment.host.HostAppointmentRoomRoute
import com.jigmnnaga.presentation.appointment.host.HostAppointmentRoomViewModel
import com.jigmnnaga.presentation.appointment.hostconfirmation.HostAppointmentConfirmationRoute
import com.jigmnnaga.presentation.appointment.hostconfirmation.HostConfirmationUiState
import com.jigmnnaga.presentation.appointment.hostconfirmation.HostConfirmationViewModel
import com.jigmnnaga.presentation.appointment.invite.AppointmentInviteRoute
import com.jigmnnaga.presentation.appointment.invite.AppointmentInviteViewModel
import com.jigmnnaga.presentation.appointment.participant.ParticipantAppointmentRoomRoute
import com.jigmnnaga.presentation.appointment.participant.ParticipantAppointmentRoomViewModel
import com.jigmnnaga.presentation.appointment.schedule.AppointmentCalendarMonth
import com.jigmnnaga.presentation.appointment.schedule.AppointmentDateTimeRoute
import com.jigmnnaga.presentation.appointment.schedule.AppointmentDateTimeViewModel
import com.jigmnnaga.presentation.appointment.placevote.PlaceVoteRole
import com.jigmnnaga.presentation.appointment.placevote.PlaceVoteRoute
import com.jigmnnaga.presentation.appointment.placevote.PlaceVoteUiState
import com.jigmnnaga.presentation.appointment.placevote.PlaceVoteViewModel
import com.jigmnnaga.presentation.common.login.LoginRoute
import com.jigmnnaga.presentation.common.splash.SplashRoute
import com.jigmnnaga.presentation.home.HomeRoute
import dagger.hilt.android.EntryPointAccessors
import kotlinx.serialization.Serializable
import java.util.Calendar

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

/** 약속 확정 확인 화면(APT-005-05). 약속 ID를 인자로 받는다. */
@Serializable
data class AppointmentFinalConfirmationDestination(val appointmentId: Long)

/** 희망 날짜·시간 선택 화면. 약속 ID를 인자로 받는다. */
@Serializable
data class AppointmentDateTimeDestination(val appointmentId: Long)

/** 장소 검색·투표 화면. 약속 ID와 방장 여부를 인자로 받는다. */
@Serializable
data class PlaceVoteDestination(val appointmentId: Long, val isHost: Boolean = false)

/** 기존 약속에서 다시 약속을 잡거나 참여자에게 요청하는 화면(APT-006). */
@Serializable
data class ExistingAppointmentAddDestination(val appointmentId: Long)

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

        // 약속 상세 API(시트 41·44번) 조회로 약속 정보와 참여자를 채운다.
        // TODO: 하나의 약속방 목적지로 합치고 내 역할(myRole)로 방장·참여자 화면을 정한다. 약속 관리 화면은 목적지가 등록될 때 연결한다.
        builder.composable<HostAppointmentRoomDestination> { entry ->
            val appointmentId = entry.toRoute<HostAppointmentRoomDestination>().appointmentId
            val loader = rememberAppointmentDetailLoader(appointmentId)
            val labels = rememberAppointmentDetailLabels()
            AppointmentDetailLoader(
                viewModel = loader,
                onBack = { navigator.navigateBack() },
                onLoginRequired = { resume -> navigator.requestLogin(resume) }
            ) { detail ->
                val viewModel: HostAppointmentRoomViewModel = viewModel(
                    key = "host-room-$appointmentId",
                    factory = remember(detail) {
                        viewModelFactory {
                            initializer { HostAppointmentRoomViewModel(detail.toHostRoomUiState(labels)) }
                        }
                    }
                )
                HostAppointmentRoomRoute(
                    viewModel = viewModel,
                    onBackClick = { navigator.navigateBack() },
                    onCalendarClick = { navigator.navigate(AppointmentDateTimeDestination(appointmentId)) },
                    onPlaceClick = { navigator.navigate(PlaceVoteDestination(appointmentId, isHost = true)) },
                    onMoreClick = { /* TODO: 약속 관리 화면 등록 후 연결 */ }
                )
            }
        }

        // TODO: 투표 결과 API 연동 시 후보 목록을 넘긴다.
        // 약속방에서 이 화면으로 가는 연결은 방장 약속방의 확정 진입점이 정해질 때 함께 붙인다.
        // 이 화면은 약속 제목만 필요해서 약속 상세 조회 결과의 제목만 쓴다.
        builder.composable<HostAppointmentConfirmationDestination> { entry ->
            val appointmentId = entry.toRoute<HostAppointmentConfirmationDestination>().appointmentId
            val loader = rememberAppointmentDetailLoader(appointmentId)
            AppointmentDetailLoader(
                viewModel = loader,
                onBack = { navigator.navigateBack() },
                onLoginRequired = { resume -> navigator.requestLogin(resume) }
            ) { detail ->
                val viewModel: HostConfirmationViewModel = viewModel(
                    key = "host-confirmation-$appointmentId",
                    factory = remember(detail) {
                        viewModelFactory {
                            initializer {
                                HostConfirmationViewModel(
                                    HostConfirmationUiState(
                                        appointmentTitle = detail.title,
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
        }

        builder.composable<ParticipantAppointmentRoomDestination> { entry ->
            val appointmentId = entry.toRoute<ParticipantAppointmentRoomDestination>().appointmentId
            val loader = rememberAppointmentDetailLoader(appointmentId)
            val labels = rememberAppointmentDetailLabels()
            AppointmentDetailLoader(
                viewModel = loader,
                onBack = { navigator.navigateBack() },
                onLoginRequired = { resume -> navigator.requestLogin(resume) }
            ) { detail ->
                val viewModel: ParticipantAppointmentRoomViewModel = viewModel(
                    key = "participant-room-$appointmentId",
                    factory = remember(detail) {
                        viewModelFactory {
                            initializer {
                                ParticipantAppointmentRoomViewModel(detail.toParticipantRoomUiState(labels))
                            }
                        }
                    }
                )
                ParticipantAppointmentRoomRoute(
                    viewModel = viewModel,
                    onBackClick = { navigator.navigateBack() },
                    onCalendarClick = { navigator.navigate(AppointmentDateTimeDestination(appointmentId)) },
                    onPlaceClick = { navigator.navigate(PlaceVoteDestination(appointmentId, isHost = false)) },
                    onManagementClick = { /* TODO: 약속 관리 화면 등록 후 연결 */ }
                )
            }
        }

        // 확정된 날짜·시간·장소와 내 참석 응답을 약속 상세 조회 값으로 채운다.
        // TODO: 참석 응답·확정 API(시트 56·59번) 연동 시 확인 버튼을 서버 전송으로 연결한다.
        // 약속방에서 이 화면으로 가는 연결은 약속방에 확정 확인 진입점이 정해질 때 함께 붙인다.
        builder.composable<AppointmentFinalConfirmationDestination> { entry ->
            val appointmentId = entry.toRoute<AppointmentFinalConfirmationDestination>().appointmentId
            val loader = rememberAppointmentDetailLoader(appointmentId)
            val labels = rememberAppointmentDetailLabels()
            AppointmentDetailLoader(
                viewModel = loader,
                onBack = { navigator.navigateBack() },
                onLoginRequired = { resume -> navigator.requestLogin(resume) }
            ) { detail ->
                val viewModel: AppointmentFinalConfirmationViewModel = viewModel(
                    key = "final-confirmation-$appointmentId",
                    factory = remember(detail) {
                        viewModelFactory {
                            initializer {
                                AppointmentFinalConfirmationViewModel(detail.toFinalConfirmationUiState(labels))
                            }
                        }
                    }
                )
                AppointmentFinalConfirmationRoute(
                    viewModel = viewModel,
                    onExit = { navigator.navigateBack() }
                )
            }
        }

        // 약속 제목, 예정 시간·장소, 멤버 목록을 약속 상세 조회 값으로 채운다.
        // TODO: 약속 요청 API 연동 시 요청 전송을 연결한다. 약속방에서 이 화면으로 가는 연결은 약속방에 진입점이 정해질 때 붙인다.
        builder.composable<ExistingAppointmentAddDestination> { entry ->
            val appointmentId = entry.toRoute<ExistingAppointmentAddDestination>().appointmentId
            val loader = rememberAppointmentDetailLoader(appointmentId)
            val labels = rememberAppointmentDetailLabels()
            AppointmentDetailLoader(
                viewModel = loader,
                onBack = { navigator.navigateBack() },
                onLoginRequired = { resume -> navigator.requestLogin(resume) }
            ) { detail ->
                val viewModel: ExistingAppointmentAddViewModel = viewModel(
                    key = "existing-add-$appointmentId",
                    factory = remember(detail) {
                        viewModelFactory {
                            initializer { ExistingAppointmentAddViewModel(detail.toExistingAddUiState(labels)) }
                        }
                    }
                )
                ExistingAppointmentAddRoute(
                    viewModel = viewModel,
                    onExit = { navigator.navigateBack() },
                    onCalendarClick = { navigator.navigate(AppointmentDateTimeDestination(appointmentId)) },
                    onPlaceClick = { navigator.navigate(PlaceVoteDestination(appointmentId, isHost = false)) },
                    onMoreClick = { /* TODO: 약속 관리 화면 등록 후 연결 */ }
                )
            }
        }

        // 약속 제목과 참여자 응답 현황(일정 제출 여부)을 약속 상세 조회 값으로 채운다.
        // TODO: 일정 제출·조회 API(시트 50번) 연동 시 후보 날짜 제한과 참여자별 실제 응답을 서버 값으로 채운다.
        builder.composable<AppointmentDateTimeDestination> { entry ->
            val appointmentId = entry.toRoute<AppointmentDateTimeDestination>().appointmentId
            val loader = rememberAppointmentDetailLoader(appointmentId)
            AppointmentDetailLoader(
                viewModel = loader,
                onBack = { navigator.navigateBack() },
                onLoginRequired = { resume -> navigator.requestLogin(resume) }
            ) { detail ->
                val viewModel: AppointmentDateTimeViewModel = viewModel(
                    key = "date-time-$appointmentId",
                    factory = remember(detail) {
                        viewModelFactory {
                            initializer {
                                AppointmentDateTimeViewModel(
                                    appointmentTitle = detail.title,
                                    initialMonth = currentCalendarMonth(),
                                    memberResponses = detail.toMemberResponses()
                                )
                            }
                        }
                    }
                )
                AppointmentDateTimeRoute(
                    viewModel = viewModel,
                    onExit = { navigator.navigateBack() },
                    onGoToChat = { navigator.navigateBack() }
                )
            }
        }

        // 약속 제목은 약속 상세 조회 값으로 채우고, 방장 여부는 내 역할(myRole)이 있으면 그 값을 쓴다.
        // TODO: 장소 검색·투표 API 연동 시 후보 목록과 선택 현황(progress)을 서버 값으로 채운다. 약속방에서 이 화면으로 가는 연결도 그때 붙인다.
        builder.composable<PlaceVoteDestination> { entry ->
            val destination = entry.toRoute<PlaceVoteDestination>()
            val loader = rememberAppointmentDetailLoader(destination.appointmentId)
            val question = stringResource(R.string.place_vote_question)
            val listLabel = stringResource(
                R.string.place_vote_list_label,
                PlaceVoteViewModel.DEFAULT_MAX_SELECTION
            )
            val submitLabel = stringResource(R.string.place_vote_submit)
            val emptyMessage = stringResource(R.string.place_vote_empty)
            AppointmentDetailLoader(
                viewModel = loader,
                onBack = { navigator.navigateBack() },
                onLoginRequired = { resume -> navigator.requestLogin(resume) }
            ) { detail ->
                val isHost = detail.myRole?.let { it == AppointmentMemberRole.HOST } ?: destination.isHost
                val viewModel: PlaceVoteViewModel = viewModel(
                    key = "place-vote-${destination.appointmentId}",
                    factory = remember(destination, detail) {
                        viewModelFactory {
                            initializer {
                                PlaceVoteViewModel(
                                    PlaceVoteUiState(
                                        role = if (isHost) PlaceVoteRole.HOST else PlaceVoteRole.PARTICIPANT,
                                        searchQuery = "",
                                        candidates = emptyList(),
                                        selectedCandidateIds = emptySet(),
                                        title = detail.title,
                                        question = question,
                                        listLabel = listLabel,
                                        submitLabel = submitLabel,
                                        showSearchInput = true,
                                        emptyMessage = emptyMessage,
                                        selectedSummaryTitle = ""
                                    )
                                )
                            }
                        }
                    }
                )
                PlaceVoteRoute(
                    viewModel = viewModel,
                    onBack = { navigator.navigateBack() }
                )
            }
        }
    }

    /** 로그인 화면으로 보내고, 로그인에 성공하면 [resumeAfterLogin]을 실행한 뒤 이 화면으로 돌아온다. */
    private fun AppNavigator.requestLogin(resumeAfterLogin: () -> Unit) {
        LoginReturnRegistry.request(resumeAfterLogin)
        navigate(LoginRoute)
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

@Composable
private fun rememberAppointmentDetailLoader(appointmentId: Long): AppointmentDetailLoadViewModel {
    val applicationContext = LocalContext.current.applicationContext
    return viewModel(
        key = "appointment-detail-$appointmentId",
        factory = remember(appointmentId) {
            viewModelFactory {
                initializer {
                    val entryPoint = EntryPointAccessors.fromApplication(
                        applicationContext,
                        AppointmentDetailEntryPoint::class.java
                    )
                    AppointmentDetailLoadViewModel(appointmentId, entryPoint.appointmentDetailRepository())
                }
            }
        }
    )
}

private fun currentCalendarMonth(): AppointmentCalendarMonth {
    val now = Calendar.getInstance()
    return AppointmentCalendarMonth(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1)
}
