package com.jigmnnaga.presentation.appointment

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.jigmnnaga.di.ChatEntryPoint
import com.jigmnnaga.di.ConfirmationEntryPoint
import com.jigmnnaga.di.PlaceSearchEntryPoint
import com.jigmnnaga.di.ScheduleEntryPoint
import com.jigmnnaga.di.InviteEntryPoint
import com.jigmnnaga.di.MemberLocationEntryPoint
import com.jigmnnaga.di.TravelEntryPoint
import com.jigmnnaga.domain.model.AppointmentDetail
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import com.jigmnnaga.presentation.appointment.detail.AppointmentDetailLoadState
import com.jigmnnaga.presentation.appointment.detail.confirmedDateTimeSummary
import com.jigmnnaga.domain.model.AppointmentDetailPlace
import com.jigmnnaga.domain.model.AppointmentMemberRole
import com.jigmnnaga.domain.model.ConfirmDetailPlace
import com.jigmnnaga.domain.model.PreviousMembers
import com.jigmnnaga.presentation.appointment.detail.AppointmentDetailLoadViewModel
import com.jigmnnaga.presentation.appointment.detail.AppointmentDetailLabels
import com.jigmnnaga.presentation.appointment.detail.AppointmentDetailLoader
import com.jigmnnaga.presentation.appointment.detail.rememberAppointmentDetailLabels
import com.jigmnnaga.presentation.appointment.detail.toExistingAddUiState
import com.jigmnnaga.presentation.appointment.detail.toFinalConfirmationUiState
import com.jigmnnaga.presentation.appointment.detail.toHostRoomUiState
import com.jigmnnaga.presentation.appointment.detail.toMemberResponses
import com.jigmnnaga.presentation.appointment.detail.toParticipantManagementUiState
import com.jigmnnaga.presentation.appointment.detail.toParticipantRoomUiState
import com.jigmnnaga.presentation.appointment.route.AndroidCurrentLocationProvider
import com.jigmnnaga.presentation.appointment.route.RouteRoute
import com.jigmnnaga.presentation.appointment.route.RouteViewModel
import com.jigmnnaga.presentation.appointment.route.rememberRouteMemberLabels
import com.jigmnnaga.presentation.appointment.route.toRouteUiState
import com.jigmnnaga.presentation.appointment.create.AppointmentCreateRoute
import com.jigmnnaga.presentation.appointment.create.AppointmentCreateViewModel
import com.jigmnnaga.presentation.appointment.existingadd.ExistingAppointmentAddRoute
import com.jigmnnaga.presentation.appointment.existingadd.ExistingAppointmentAddViewModel
import com.jigmnnaga.presentation.appointment.finalconfirmation.AppointmentFinalConfirmationRoute
import com.jigmnnaga.presentation.appointment.finalconfirmation.AppointmentFinalConfirmationViewModel
import com.jigmnnaga.presentation.appointment.host.HostAppointmentRoomRoute
import com.jigmnnaga.presentation.chat.AppointmentChatViewModel
import com.jigmnnaga.presentation.appointment.host.HostAppointmentRoomViewModel
import com.jigmnnaga.presentation.appointment.hostconfirmation.HostAppointmentConfirmationRoute
import com.jigmnnaga.presentation.appointment.hostconfirmation.HostConfirmationUiState
import com.jigmnnaga.presentation.appointment.hostconfirmation.HostConfirmationViewModel
import com.jigmnnaga.presentation.appointment.invite.AppointmentInviteRoute
import com.jigmnnaga.presentation.appointment.manage.AppointmentManageRoute
import com.jigmnnaga.presentation.appointment.manage.AppointmentManageViewModel
import com.jigmnnaga.presentation.appointment.invite.AppointmentInviteViewModel
import com.jigmnnaga.presentation.appointment.participant.ParticipantAppointmentManagementRoute
import com.jigmnnaga.presentation.appointment.participant.ParticipantAppointmentManagementViewModel
import com.jigmnnaga.presentation.appointment.participant.ParticipantAppointmentRoomRoute
import com.jigmnnaga.presentation.appointment.participant.ParticipantAppointmentRoomViewModel
import com.jigmnnaga.presentation.appointment.schedule.AppointmentCalendarMonth
import com.jigmnnaga.presentation.appointment.schedule.AppointmentDateValue
import com.jigmnnaga.presentation.appointment.schedule.AppointmentDateTimeRoute
import com.jigmnnaga.presentation.appointment.schedule.AppointmentDateTimeViewModel
import com.jigmnnaga.presentation.appointment.placevote.PlaceVoteRole
import com.jigmnnaga.presentation.appointment.placevote.PlaceVoteRoute
import com.jigmnnaga.presentation.appointment.placevote.PlaceVoteTexts
import com.jigmnnaga.presentation.appointment.placevote.toPlaceCandidate
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

/**
 * 약속방. 약속 ID를 인자로 받고, 내 역할에 따라 방장 약속방(APT-004)이나 참여자 약속방(APT-005)을 보여 준다.
 * 약속방으로 들어가는 모든 경로는 이 목적지를 쓴다.
 */
@Serializable
data class AppointmentRoomDestination(val appointmentId: Long)

/** 참여자 약속방 관리 화면(APT-005-02·06). 약속 ID를 인자로 받는다. */
@Serializable
data class ParticipantAppointmentManagementDestination(val appointmentId: Long)

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
                // 수락한 약속방으로 이동한다. 초대 화면은 백스택에서 빼서 뒤로 가도 다시 보이지 않게 한다.
                onOpenChat = { appointmentId ->
                    navigator.navigate(
                        destination = AppointmentRoomDestination(appointmentId),
                        options = AppNavigationOptions(
                            popUpTo = AppointmentInviteDestination(inviteCode),
                            inclusive = true
                        )
                    )
                },
                onReturnHome = { navigator.navigateToHome() }
            )
        }

        // 약속 상세 API(시트 41·44번) 조회로 약속 정보와 참여자를 채운다.
        // 내 역할(myRole)이 방장이면 방장 약속방을, 그 밖에는 참여자 약속방을 보여 준다.
        builder.composable<AppointmentRoomDestination> { entry ->
            val appointmentId = entry.toRoute<AppointmentRoomDestination>().appointmentId
            val loader = rememberAppointmentDetailLoader(appointmentId)
            val labels = rememberAppointmentDetailLabels()
            AppointmentDetailLoader(
                viewModel = loader,
                onBack = { navigator.navigateBack() },
                onLoginRequired = { resume -> navigator.requestLogin(resume) }
            ) { detail ->
                if (detail.opensHostRoom()) {
                    HostRoomContent(navigator, appointmentId, detail, labels)
                } else {
                    ParticipantRoomContent(navigator, appointmentId, detail, labels)
                }
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
                // 일정 확정을 마치고 돌아오면 확정 정보와 멤버 응답을 새 값으로 채운다.
                val liveLoader = rememberAppointmentDetailLoader(appointmentId)
                val liveState by liveLoader.state.collectAsState()
                LaunchedEffect(liveState) {
                    (liveState as? AppointmentDetailLoadState.Loaded)?.let { viewModel.onDetailRefreshed(it.detail) }
                }
                AppointmentManageRoute(
                    viewModel = viewModel,
                    onExit = { navigator.navigateBack() },
                    onDeleted = { navigator.navigateToHome() },
                    onLoginRequired = { resume -> navigator.requestLogin(resume) },
                    onOpenConfirmation = {
                        navigator.navigate(HostAppointmentConfirmationDestination(appointmentId))
                    }
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
                                        selectedDateTime = detail.confirmedDateTimeSummary(),
                                        selectedPlace = null,
                                        selectedDetailLocation = detail.detailPlace?.placeName
                                    ),
                                    isHost = detail.myRole != AppointmentMemberRole.MEMBER,
                                    appointmentId = appointmentId,
                                    candidateLocations = detail.candidateLocations,
                                    confirmationRepository = confirmationEntryPoint.confirmationRepository(),
                                    placeSearchRepository = searchEntryPoint.placeSearchRepository(),
                                    presetDateTime = detail.confirmedDate?.let { date ->
                                        detail.confirmedTime?.let { time -> date to time }
                                    },
                                    presetLocationId = detail.confirmedLocation?.locationId,
                                    presetDetailPlace = detail.detailPlace?.toConfirmDetailPlace()
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

        // 참여자가 약속 정보를 보고 약속방 알림을 켜고 끄거나 약속방을 나간다.
        // 나가면 돌아갈 약속방이 없으므로 홈으로 보낸다. 약속 정보는 방장만 바꿀 수 있어 읽기만 한다.
        builder.composable<ParticipantAppointmentManagementDestination> { entry ->
            val appointmentId = entry.toRoute<ParticipantAppointmentManagementDestination>().appointmentId
            val loader = rememberAppointmentDetailLoader(appointmentId, followChanges = false)
            val labels = rememberAppointmentDetailLabels()
            val applicationContext = LocalContext.current.applicationContext
            AppointmentDetailLoader(
                viewModel = loader,
                onBack = { navigator.navigateBack() },
                onLoginRequired = { resume -> navigator.requestLogin(resume) }
            ) { detail ->
                val viewModel: ParticipantAppointmentManagementViewModel = viewModel(
                    key = "participant-management-$appointmentId",
                    factory = remember(detail) {
                        viewModelFactory {
                            initializer {
                                val entryPoint = EntryPointAccessors.fromApplication(
                                    applicationContext,
                                    AppointmentManageEntryPoint::class.java
                                )
                                ParticipantAppointmentManagementViewModel(
                                    appointmentId = appointmentId,
                                    initialState = detail.toParticipantManagementUiState(labels),
                                    repository = entryPoint.appointmentManageRepository()
                                )
                            }
                        }
                    }
                )
                ParticipantAppointmentManagementRoute(
                    viewModel = viewModel,
                    onBackClick = { navigator.navigateBack() },
                    onEditAvailabilityClick = { navigator.navigate(AppointmentDateTimeDestination(appointmentId)) },
                    onLeft = { navigator.navigateToHome() },
                    onLoginRequired = { resume -> navigator.requestLogin(resume) }
                )
            }
        }

        // 확정된 날짜·시간·장소와 내 참석 응답을 약속 상세 조회 값으로 채운다.
        // 참여자 약속방은 확정된 뒤 "약속 정보" 영역을 누르면 이 화면으로 온다.
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

        // 약속 제목과 후보 날짜는 약속 상세 조회 값으로 채운다.
        // 내가 낸 일정과 참여자 응답 현황은 화면을 열 때 불러오고, 다음 버튼으로 일정을 서버에 제출한다.
        builder.composable<AppointmentDateTimeDestination> { entry ->
            val appointmentId = entry.toRoute<AppointmentDateTimeDestination>().appointmentId
            val loader = rememberAppointmentDetailLoader(appointmentId)
            val scheduleContext = LocalContext.current.applicationContext
            AppointmentDetailLoader(
                viewModel = loader,
                onBack = { navigator.navigateBack() },
                onLoginRequired = { resume -> navigator.requestLogin(resume) }
            ) { detail ->
                val candidateDates = detail.candidateDates
                    .mapNotNull { AppointmentDateValue.parseIso(it) }
                    .toSet()
                val viewModel: AppointmentDateTimeViewModel = viewModel(
                    key = "date-time-$appointmentId",
                    factory = remember(detail) {
                        viewModelFactory {
                            initializer {
                                val scheduleRepository = EntryPointAccessors.fromApplication(
                                    scheduleContext,
                                    ScheduleEntryPoint::class.java
                                ).scheduleRepository()
                                AppointmentDateTimeViewModel(
                                    appointmentTitle = detail.title,
                                    initialMonth = candidateDates.firstCalendarMonth() ?: currentCalendarMonth(),
                                    memberResponses = detail.toMemberResponses(),
                                    appointmentId = appointmentId,
                                    scheduleRepository = scheduleRepository,
                                    candidateDates = candidateDates
                                )
                            }
                        }
                    }
                )
                AppointmentDateTimeRoute(
                    viewModel = viewModel,
                    onExit = { navigator.navigateBack() },
                    onGoToChat = { navigator.navigateBack() },
                    onLoginRequired = { resume -> navigator.requestLogin(resume) }
                )
            }
        }

        // 약속 제목과 후보 장소는 약속 상세 조회 값으로 채운다.
        // 방장도 참여자와 같은 화면에서 희망 장소 하나를 제출한다. 후보 장소 수정은 약속방 관리에서 한다. 제출 뒤에 약속방은 상세를 다시 불러온다.
        // 이 화면이 저장 도중에 다시 불러오지 않도록 약속 상세 변경은 따라가지 않는다.
        builder.composable<PlaceVoteDestination> { entry ->
            val destination = entry.toRoute<PlaceVoteDestination>()
            val loader = rememberAppointmentDetailLoader(destination.appointmentId, followChanges = false)
            val question = stringResource(R.string.place_vote_question)
            val participantListLabel = stringResource(R.string.place_vote_participant_list_label)
            val submitLabel = stringResource(R.string.place_vote_submit)
            val emptyMessage = stringResource(R.string.place_vote_empty)
            val applicationContext = LocalContext.current.applicationContext
            val voteTexts = remember(applicationContext) {
                PlaceVoteTexts(
                    voteLabel = { count, total ->
                        applicationContext.getString(R.string.place_vote_vote_ratio, count, total)
                    },
                    progressTitle = { selected ->
                        applicationContext.getString(R.string.place_vote_progress_title, selected)
                    },
                    progressSubtitle = { remaining ->
                        applicationContext.getString(R.string.place_vote_progress_subtitle, remaining)
                    },
                    progressSelecting = applicationContext.getString(R.string.place_vote_progress_selecting),
                    progressDone = applicationContext.getString(R.string.place_vote_progress_done)
                )
            }
            AppointmentDetailLoader(
                viewModel = loader,
                onBack = { navigator.navigateBack() },
                onLoginRequired = { resume -> navigator.requestLogin(resume) }
            ) { detail ->
                val viewModel: PlaceVoteViewModel = viewModel(
                    key = "place-vote-${destination.appointmentId}",
                    factory = remember(destination, detail) {
                        viewModelFactory {
                            initializer {
                                val searchEntryPoint = EntryPointAccessors.fromApplication(
                                    applicationContext,
                                    PlaceSearchEntryPoint::class.java
                                )
                                val manageEntryPoint = EntryPointAccessors.fromApplication(
                                    applicationContext,
                                    AppointmentManageEntryPoint::class.java
                                )
                                val scheduleEntryPoint = EntryPointAccessors.fromApplication(
                                    applicationContext,
                                    ScheduleEntryPoint::class.java
                                )
                                val chatEntryPoint = EntryPointAccessors.fromApplication(
                                    applicationContext,
                                    ChatEntryPoint::class.java
                                )
                                val locationCandidates = detail.candidateLocations.mapNotNull { it.toPlaceCandidate() }
                                PlaceVoteViewModel(
                                    template = PlaceVoteUiState(
                                        role = PlaceVoteRole.PARTICIPANT,
                                        searchQuery = "",
                                        candidates = locationCandidates,
                                        selectedCandidateIds = emptySet(),
                                        title = detail.title,
                                        question = question,
                                        listLabel = participantListLabel,
                                        submitLabel = submitLabel,
                                        emptyMessage = emptyMessage
                                    ),
                                    searchRepository = searchEntryPoint.placeSearchRepository(),
                                    appointmentId = destination.appointmentId,
                                    manageRepository = manageEntryPoint.appointmentManageRepository(),
                                    scheduleRepository = scheduleEntryPoint.scheduleRepository(),
                                    myMemberIdProvider = chatEntryPoint.myMemberIdProvider(),
                                    texts = voteTexts
                                )
                            }
                        }
                    }
                )
                PlaceVoteRoute(
                    viewModel = viewModel,
                    onBack = { navigator.navigateBack() },
                    onLoginRequired = { resume -> navigator.requestLogin(resume) },
                    onSaved = { navigator.navigateBack() }
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

    /** 방장 약속방(APT-004). 약속 관리에서 약속 정보를 바꿔 상세를 다시 불러오면 새 값으로 화면 상태를 다시 만든다. */
    @Composable
    private fun HostRoomContent(
        navigator: AppNavigator,
        appointmentId: Long,
        detail: AppointmentDetail,
        labels: AppointmentDetailLabels
    ) {
        val viewModel: HostAppointmentRoomViewModel = viewModel(
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
            onMoreClick = { navigator.navigate(AppointmentManageDestination(appointmentId)) },
            chatViewModel = rememberAppointmentChatViewModel(appointmentId),
            routeContent = { routeModifier -> RouteTabContent(appointmentId, detail, labels, routeModifier) }
        )
    }

    /** 참여자 약속방(APT-005). */
    @Composable
    private fun ParticipantRoomContent(
        navigator: AppNavigator,
        appointmentId: Long,
        detail: AppointmentDetail,
        labels: AppointmentDetailLabels
    ) {
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
            onManagementClick = {
                navigator.navigate(ParticipantAppointmentManagementDestination(appointmentId))
            },
            onConfirmationClick = {
                navigator.navigate(AppointmentFinalConfirmationDestination(appointmentId))
            },
            chatViewModel = rememberAppointmentChatViewModel(appointmentId),
            routeContent = { routeModifier -> RouteTabContent(appointmentId, detail, labels, routeModifier) }
        )
    }

    /**
     * 확정된 약속방 "지도·현황" 탭 안에 들어가는 이동 경로 내용. 내 멤버 ID를 알아낸 뒤에 화면 상태를 만들어,
     * 처음부터 "나" 카드가 맞게 보이게 한다. 헤더와 "채팅 / 지도·현황" 탭은 약속방이 그린다.
     */
    @Composable
    private fun RouteTabContent(
        appointmentId: Long,
        detail: AppointmentDetail,
        labels: AppointmentDetailLabels,
        modifier: Modifier
    ) {
        val applicationContext = LocalContext.current.applicationContext
        val routeLabels = rememberRouteMemberLabels()
        val currentLocationName = stringResource(R.string.route_start_current_title)
        var myMemberId by remember { mutableStateOf<Long?>(null) }
        var myMemberResolved by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            myMemberId = EntryPointAccessors
                .fromApplication(applicationContext, ChatEntryPoint::class.java)
                .myMemberIdProvider()
                .myMemberId()
            myMemberResolved = true
        }
        if (myMemberResolved) {
            val viewModel: RouteViewModel = viewModel(
                key = "route-$appointmentId",
                factory = remember(detail, myMemberId) {
                    viewModelFactory {
                        initializer {
                            RouteViewModel(
                                initialState = detail.toRouteUiState(
                                    memberSummary = labels.memberSummary(detail.joinedCount),
                                    labels = routeLabels,
                                    myMemberId = myMemberId
                                ),
                                memberLocationRepository = EntryPointAccessors
                                    .fromApplication(applicationContext, MemberLocationEntryPoint::class.java)
                                    .memberLocationRepository(),
                                placeSearchRepository = EntryPointAccessors
                                    .fromApplication(applicationContext, PlaceSearchEntryPoint::class.java)
                                    .placeSearchRepository(),
                                currentLocationProvider = AndroidCurrentLocationProvider(applicationContext),
                                currentLocationName = currentLocationName,
                                appointmentId = appointmentId,
                                travelRepository = EntryPointAccessors
                                    .fromApplication(applicationContext, TravelEntryPoint::class.java)
                                    .travelRepository()
                            )
                        }
                    }
                }
            )
            RouteRoute(viewModel = viewModel, modifier = modifier)
        } else {
            Box(modifier)
        }
    }

    /**
     * 약속방 채팅 ViewModel. 약속 정보가 다시 불러와져 방 화면이 새로 만들어져도 채팅은 유지되도록
     * 약속 ID로만 키를 둔다. 약속방을 떠나 백스택에서 빠지면 연결도 함께 정리된다.
     */
    @Composable
    private fun rememberAppointmentChatViewModel(appointmentId: Long): AppointmentChatViewModel {
        val applicationContext = LocalContext.current.applicationContext
        return viewModel(
            key = "appointment-chat-$appointmentId",
            factory = remember(appointmentId) {
                viewModelFactory {
                    initializer {
                        val entryPoint = EntryPointAccessors.fromApplication(
                            applicationContext,
                            ChatEntryPoint::class.java
                        )
                        AppointmentChatViewModel(
                            appointmentId = appointmentId,
                            repository = entryPoint.chatRepository(),
                            realtime = entryPoint.chatRealtime(),
                            memberIdProvider = entryPoint.myMemberIdProvider()
                        )
                    }
                }
            }
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

/** 후보 날짜 중 가장 이른 날짜의 달. 후보가 없으면 null. */
private fun Set<AppointmentDateValue>.firstCalendarMonth(): AppointmentCalendarMonth? =
    minWithOrNull(compareBy<AppointmentDateValue>({ it.year }, { it.month }, { it.day }))
        ?.let { AppointmentCalendarMonth(it.year, it.month) }

private fun currentCalendarMonth(): AppointmentCalendarMonth {
    val now = Calendar.getInstance()
    return AppointmentCalendarMonth(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1)
}

/** 이미 정해진 세부 위치를 확정 요청 값으로 바꾼다. 좌표가 없으면 서버가 받지 않으므로 null이다. */
private fun AppointmentDetailPlace.toConfirmDetailPlace(): ConfirmDetailPlace? {
    val lat = latitude ?: return null
    val lng = longitude ?: return null
    return ConfirmDetailPlace(placeName = placeName, address = address, latitude = lat, longitude = lng)
}
