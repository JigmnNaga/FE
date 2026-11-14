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
import com.jigmnnaga.di.AppointmentCreateEntryPoint
import com.jigmnnaga.di.AppointmentDetailEntryPoint
import com.jigmnnaga.di.AppointmentManageEntryPoint
import com.jigmnnaga.di.AttendanceEntryPoint
import com.jigmnnaga.di.ConfirmationEntryPoint
import com.jigmnnaga.di.PlaceSearchEntryPoint
import com.jigmnnaga.di.InviteEntryPoint
import com.jigmnnaga.domain.model.AppointmentMemberRole
import com.jigmnnaga.domain.model.PreviousMembers
import com.jigmnnaga.presentation.appointment.detail.AppointmentDetailLoadViewModel
import com.jigmnnaga.presentation.appointment.detail.AppointmentDetailLoader
import com.jigmnnaga.presentation.appointment.detail.rememberAppointmentDetailLabels
import com.jigmnnaga.presentation.appointment.detail.toExistingAddUiState
import com.jigmnnaga.presentation.appointment.detail.toFinalConfirmationUiState
import com.jigmnnaga.presentation.appointment.detail.toHostRoomUiState
import com.jigmnnaga.presentation.appointment.detail.toMemberResponses
import com.jigmnnaga.presentation.appointment.detail.toParticipantRoomUiState
import com.jigmnnaga.presentation.appointment.create.AppointmentCreateRoute
import com.jigmnnaga.presentation.appointment.create.AppointmentCreateViewModel
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
import com.jigmnnaga.presentation.appointment.manage.AppointmentManageRoute
import com.jigmnnaga.presentation.appointment.manage.AppointmentManageViewModel
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable
import java.util.Calendar

/** 약속 생성 화면(APT-001). */
@Serializable
data object AppointmentCreateDestination

/**
 * 기존 약속의 참여자를 이전 약속 멤버로 담아 약속을 만드는 화면(APT-001).
 * [memberIds]는 쉼표로 이은 멤버 ID다. (예: `"2,3"`)
 */
@Serializable
data class AppointmentCreateFromPreviousDestination(
    val previousAppointmentId: Long,
    val memberIds: String
)

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

/** 방장 약속방 관리 화면(APT-006). 약속 ID를 인자로 받는다. */
@Serializable
data class AppointmentManageDestination(val appointmentId: Long)

/** 약속 기능이 소유하는 이동 경로 그래프. 앱 호스트에는 이 객체만 등록한다. */
object AppointmentNavigationGraph : AppNavigationGraph {
    override fun register(builder: NavGraphBuilder, navigator: AppNavigator) {
        builder.composable<AppointmentCreateDestination> {
            AppointmentCreateContent(navigator = navigator, previousMembers = null)
        }

        builder.composable<AppointmentCreateFromPreviousDestination> { entry ->
            val destination = entry.toRoute<AppointmentCreateFromPreviousDestination>()
            AppointmentCreateContent(navigator = navigator, previousMembers = destination.toPreviousMembers())
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
                    // 약속 관리에서 약속 정보를 바꿔 상세를 다시 불러오면 새 값으로 화면 상태를 다시 만든다.
                    key = "host-room-$appointmentId-${detail.hashCode()}",
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
                    onMoreClick = { navigator.navigate(AppointmentManageDestination(appointmentId)) }
                )
            }
        }

        // 방장이 약속 이름·날짜·장소·응답 기한을 바꾸고 약속을 취소·삭제한다.
        // 열려 있는 약속방이 바뀐 값을 다시 불러오도록, 이 화면의 약속 상세는 변경 신호를 따라가지 않는다.
        // TODO: 참여 멤버 관리(내보내기·방장 위임·미응답자 알림)는 멤버 관리 화면 이슈에서 이 화면에 붙인다.
        builder.composable<AppointmentManageDestination> { entry ->
            val appointmentId = entry.toRoute<AppointmentManageDestination>().appointmentId
            val loader = rememberAppointmentDetailLoader(appointmentId, followChanges = false)
            val applicationContext = LocalContext.current.applicationContext
            AppointmentDetailLoader(
                viewModel = loader,
                onBack = { navigator.navigateBack() },
                onLoginRequired = { resume -> navigator.requestLogin(resume) }
            ) { detail ->
                val viewModel: AppointmentManageViewModel = viewModel(
                    key = "appointment-manage-$appointmentId",
                    factory = remember(detail) {
                        viewModelFactory {
                            initializer {
                                val manageEntryPoint = EntryPointAccessors.fromApplication(
                                    applicationContext,
                                    AppointmentManageEntryPoint::class.java
                                )
                                val searchEntryPoint = EntryPointAccessors.fromApplication(
                                    applicationContext,
                                    PlaceSearchEntryPoint::class.java
                                )
                                AppointmentManageViewModel(
                                    detail = detail,
                                    repository = manageEntryPoint.appointmentManageRepository(),
                                    placeSearchRepository = searchEntryPoint.placeSearchRepository()
                                )
                            }
                        }
                    }
                )
                AppointmentManageRoute(
                    viewModel = viewModel,
                    onExit = { navigator.navigateBack() },
                    onDeleted = { navigator.navigateToHome() },
                    onLoginRequired = { resume -> navigator.requestLogin(resume) }
                )
            }
        }

        // 약속방에서 이 화면으로 가는 연결은 방장 약속방의 확정 진입점이 정해질 때 함께 붙인다.
        // 이 화면은 약속 제목만 필요해서 약속 상세 조회 결과의 제목만 쓴다.
        builder.composable<HostAppointmentConfirmationDestination> { entry ->
            val appointmentId = entry.toRoute<HostAppointmentConfirmationDestination>().appointmentId
            val loader = rememberAppointmentDetailLoader(appointmentId)
            val applicationContext = LocalContext.current.applicationContext
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
                                val confirmationEntryPoint = EntryPointAccessors.fromApplication(
                                    applicationContext,
                                    ConfirmationEntryPoint::class.java
                                )
                                val searchEntryPoint = EntryPointAccessors.fromApplication(
                                    applicationContext,
                                    PlaceSearchEntryPoint::class.java
                                )
                                HostConfirmationViewModel(
                                    initialState = HostConfirmationUiState(
                                        appointmentTitle = detail.title,
                                        selectedDateTime = null,
                                        selectedPlace = null
                                    ),
                                    isHost = detail.myRole != AppointmentMemberRole.MEMBER,
                                    appointmentId = appointmentId,
                                    candidateLocations = detail.candidateLocations,
                                    confirmationRepository = confirmationEntryPoint.confirmationRepository(),
                                    placeSearchRepository = searchEntryPoint.placeSearchRepository()
                                )
                            }
                        }
                    }
                )
                HostAppointmentConfirmationRoute(
                    viewModel = viewModel,
                    onExit = { navigator.navigateBack() },
                    onLoginRequired = { resume -> navigator.requestLogin(resume) }
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
        // 약속방에서 이 화면으로 가는 연결은 약속방에 확정 확인 진입점이 정해질 때 함께 붙인다.
        builder.composable<AppointmentFinalConfirmationDestination> { entry ->
            val appointmentId = entry.toRoute<AppointmentFinalConfirmationDestination>().appointmentId
            val loader = rememberAppointmentDetailLoader(appointmentId)
            val labels = rememberAppointmentDetailLabels()
            val applicationContext = LocalContext.current.applicationContext
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
                                val entryPoint = EntryPointAccessors.fromApplication(
                                    applicationContext,
                                    AttendanceEntryPoint::class.java
                                )
                                AppointmentFinalConfirmationViewModel(
                                    appointmentId = appointmentId,
                                    initialState = detail.toFinalConfirmationUiState(labels),
                                    attendanceRepository = entryPoint.attendanceRepository()
                                )
                            }
                        }
                    }
                )
                AppointmentFinalConfirmationRoute(
                    viewModel = viewModel,
                    onExit = { navigator.navigateBack() },
                    onLoginRequired = { resume -> navigator.requestLogin(resume) }
                )
            }
        }

        // 약속 제목, 예정 시간·장소, 멤버 목록을 약속 상세 조회 값으로 채운다.
        // "약속 요청하기"는 고른 참여자를 이전 약속 멤버로 담아 약속 생성 화면으로 이동한다.
        // TODO: 약속방에서 이 화면으로 가는 연결은 약속방에 진입점이 정해질 때 붙인다.
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
                            initializer {
                                ExistingAppointmentAddViewModel(detail.toExistingAddUiState(labels), appointmentId)
                            }
                        }
                    }
                )
                ExistingAppointmentAddRoute(
                    viewModel = viewModel,
                    onExit = { navigator.navigateBack() },
                    onCalendarClick = { navigator.navigate(AppointmentDateTimeDestination(appointmentId)) },
                    onPlaceClick = { navigator.navigate(PlaceVoteDestination(appointmentId, isHost = false)) },
                    onMoreClick = { /* TODO: 약속 관리 화면 등록 후 연결 */ },
                    onRequestMembers = { request ->
                        navigator.navigate(
                            AppointmentCreateFromPreviousDestination(
                                previousAppointmentId = request.previousAppointmentId,
                                memberIds = request.memberIds.toMemberIdsArgument()
                            )
                        )
                    }
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
            val applicationContext = LocalContext.current.applicationContext
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
                                val searchEntryPoint = EntryPointAccessors.fromApplication(
                                    applicationContext,
                                    PlaceSearchEntryPoint::class.java
                                )
                                PlaceVoteViewModel(
                                    template = PlaceVoteUiState(
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
                                    ),
                                    searchRepository = searchEntryPoint.placeSearchRepository()
                                )
                            }
                        }
                    }
                )
                PlaceVoteRoute(
                    viewModel = viewModel,
                    onBack = { navigator.navigateBack() },
                    onLoginRequired = { resume -> navigator.requestLogin(resume) }
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

    /** 약속 생성 화면. [previousMembers]가 있으면 약속을 만들 때 이전 약속 멤버로 함께 보낸다. */
    @Composable
    private fun AppointmentCreateContent(navigator: AppNavigator, previousMembers: PreviousMembers?) {
        val applicationContext = LocalContext.current.applicationContext
        val viewModel: AppointmentCreateViewModel = viewModel(
            key = previousMembers?.let { "create-from-${it.appointmentId}-${it.memberIds}" },
            factory = remember(previousMembers) {
                viewModelFactory {
                    initializer {
                        val createEntryPoint = EntryPointAccessors.fromApplication(
                            applicationContext,
                            AppointmentCreateEntryPoint::class.java
                        )
                        val searchEntryPoint = EntryPointAccessors.fromApplication(
                            applicationContext,
                            PlaceSearchEntryPoint::class.java
                        )
                        AppointmentCreateViewModel(
                            createRepository = createEntryPoint.appointmentCreateRepository(),
                            placeSearchRepository = searchEntryPoint.placeSearchRepository(),
                            previousMembers = previousMembers
                        )
                    }
                }
            }
        )
        AppointmentCreateRoute(
            onExit = { navigator.navigateBack() },
            viewModel = viewModel,
            onLoginRequired = { resume -> navigator.requestLogin(resume) }
        )
    }

    private fun inviteViewModelFactory(context: Context) = viewModelFactory {
        initializer {
            val entryPoint = EntryPointAccessors.fromApplication(context, InviteEntryPoint::class.java)
            AppointmentInviteViewModel(entryPoint.appointmentInviteRepository())
        }
    }
}

/**
 * 약속 상세를 불러오는 ViewModel. [followChanges]가 true면 약속 관리 화면에서 약속 정보가 바뀔 때
 * 상세를 다시 불러온다. 약속 관리 화면 자신은 false로 둬서 저장할 때마다 화면이 깜빡이지 않게 한다.
 */
@Composable
private fun rememberAppointmentDetailLoader(
    appointmentId: Long,
    followChanges: Boolean = true
): AppointmentDetailLoadViewModel {
    val applicationContext = LocalContext.current.applicationContext
    return viewModel(
        key = if (followChanges) "appointment-detail-$appointmentId" else "appointment-detail-fixed-$appointmentId",
        factory = remember(appointmentId, followChanges) {
            viewModelFactory {
                initializer {
                    val entryPoint = EntryPointAccessors.fromApplication(
                        applicationContext,
                        AppointmentDetailEntryPoint::class.java
                    )
                    val repository = entryPoint.appointmentDetailRepository()
                    if (followChanges) {
                        AppointmentDetailLoadViewModel(appointmentId, repository)
                    } else {
                        AppointmentDetailLoadViewModel(appointmentId, repository, MutableStateFlow(0))
                    }
                }
            }
        }
    )
}

private fun currentCalendarMonth(): AppointmentCalendarMonth {
    val now = Calendar.getInstance()
    return AppointmentCalendarMonth(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1)
}
