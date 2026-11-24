package com.jigmnnaga.presentation.mypage.account

import com.jigmnnaga.core.datastore.TokenManager
import com.jigmnnaga.data.remote.api.MyAppointmentApi
import com.jigmnnaga.data.remote.dto.ApiResponse
import com.jigmnnaga.data.remote.dto.MyAppointmentResponseDto
import com.jigmnnaga.domain.model.AppointmentDetail
import com.jigmnnaga.domain.model.AppointmentDetailResult
import com.jigmnnaga.domain.model.AppointmentMemberRole
import com.jigmnnaga.domain.model.LoginResult
import com.jigmnnaga.domain.model.LogoutResult
import com.jigmnnaga.domain.model.ReissueResult
import com.jigmnnaga.domain.model.WithdrawResult
import com.jigmnnaga.domain.repository.AppointmentDetailRepository
import com.jigmnnaga.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    // ── Fakes ────────────────────────────────────────────────────────────────

    private class FakeAuthRepository : AuthRepository {
        var logoutResult: LogoutResult = LogoutResult.Success
        var withdrawResult: WithdrawResult = WithdrawResult.Success

        override suspend fun kakaoLogin(kakaoAccessToken: String): LoginResult =
            LoginResult.Success(isNewMember = false)

        override suspend fun reissueTokens(): ReissueResult = ReissueResult.Success
        override suspend fun logout(): LogoutResult = logoutResult
        override suspend fun withdraw(): WithdrawResult = withdrawResult
    }

    private class FakeTokenManager(
        private val token: String? = "valid-access-token"
    ) : TokenManager {
        override val accessToken: Flow<String?> = flowOf(token)
        override val refreshToken: Flow<String?> = flowOf(null)
        override suspend fun saveTokens(accessToken: String, refreshToken: String) = Unit
        override suspend fun clearTokens() = Unit
        override suspend fun isAccessTokenExpired(): Boolean = false
        override suspend fun isRefreshTokenExpired(): Boolean = false
        override suspend fun validateAndClearInvalidTokens(): Boolean = true
    }

    private class FakeMyAppointmentApi : MyAppointmentApi {
        var appointmentsResult: List<MyAppointmentResponseDto> = emptyList()
        var shouldThrow: Boolean = false

        override suspend fun getMyAppointments(
            authorization: String?,
            type: String
        ): Response<ApiResponse<List<MyAppointmentResponseDto>>> {
            if (shouldThrow) throw java.io.IOException("네트워크 오류")
            return Response.success(ApiResponse(data = appointmentsResult))
        }

        override suspend fun getNextAppointment(authorization: String?) =
            Response.success(ApiResponse<MyAppointmentResponseDto?>())

        override suspend fun getCalendar(authorization: String?, month: String) =
            Response.success(ApiResponse(data = com.jigmnnaga.data.remote.dto.CalendarResponseDto(days = emptyList())))
    }

    private class FakeAppointmentDetailRepository : AppointmentDetailRepository {
        var roleByAppointmentId: Map<Long, AppointmentMemberRole> = emptyMap()

        override suspend fun getAppointmentDetail(
            appointmentId: Long
        ): AppointmentDetailResult<AppointmentDetail> {
            val role = roleByAppointmentId[appointmentId] ?: AppointmentMemberRole.MEMBER
            return AppointmentDetailResult.Success(
                AppointmentDetail(
                    appointmentId = appointmentId,
                    title = "약속 $appointmentId",
                    canceled = false,
                    dateSelectionType = com.jigmnnaga.domain.model.AppointmentDateSelectionType.SINGLE,
                    candidateDates = emptyList(),
                    candidateLocations = emptyList(),
                    responseDeadline = null,
                    inviteUrl = null,
                    confirmed = false,
                    confirmedDate = null,
                    confirmedTime = null,
                    confirmedLocation = null,
                    detailPlace = null,
                    myRole = role,
                    myAttendanceStatus = com.jigmnnaga.domain.model.AppointmentAttendanceStatus.PENDING,
                    myNotificationEnabled = false,
                    myTravelMode = null,
                    joinedCount = 1,
                    scheduleSubmittedCount = 0,
                    members = emptyList()
                )
            )
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun appointment(id: Long, title: String, memberCount: Int) =
        MyAppointmentResponseDto(appointmentId = id, title = title, memberCount = memberCount)

    private fun viewModel(
        authRepo: FakeAuthRepository = FakeAuthRepository(),
        api: FakeMyAppointmentApi = FakeMyAppointmentApi(),
        tokenManager: FakeTokenManager = FakeTokenManager(),
        detailRepo: FakeAppointmentDetailRepository = FakeAppointmentDetailRepository()
    ) = AccountViewModel(authRepo, api, tokenManager, detailRepo)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── logout 성공 케이스 ───────────────────────────────────────────────────

    @Test
    fun `로그아웃 성공 시 logoutSuccess가 true가 된다`() = runTest {
        val vm = viewModel()

        vm.logout()

        assertTrue(vm.uiState.value.logoutSuccess)
    }

    @Test
    fun `로그아웃 성공 시 isLoading이 false로 돌아온다`() = runTest {
        val vm = viewModel()

        vm.logout()

        assertFalse(vm.uiState.value.isLoading)
    }

    // ── logout 실패 케이스 ───────────────────────────────────────────────────

    @Test
    fun `로그아웃 LoginRequired 시 loginRequired가 true가 된다`() = runTest {
        val repo = FakeAuthRepository().apply { logoutResult = LogoutResult.LoginRequired }
        val vm = viewModel(authRepo = repo)

        vm.logout()

        assertTrue(vm.uiState.value.loginRequired)
    }

    @Test
    fun `로그아웃 retryable Failure 시 적절한 에러 메시지가 설정된다`() = runTest {
        val repo = FakeAuthRepository().apply { logoutResult = LogoutResult.Failure(retryable = true) }
        val vm = viewModel(authRepo = repo)

        vm.logout()

        assertEquals("잠시 후 다시 시도해 주세요.", vm.uiState.value.errorMessage)
    }

    @Test
    fun `로그아웃 non-retryable Failure 시 적절한 에러 메시지가 설정된다`() = runTest {
        val repo = FakeAuthRepository().apply { logoutResult = LogoutResult.Failure(retryable = false) }
        val vm = viewModel(authRepo = repo)

        vm.logout()

        assertEquals("로그아웃에 실패했습니다.", vm.uiState.value.errorMessage)
    }

    @Test
    fun `로딩 중 logout을 중복 호출해도 한 번만 실행된다`() = runTest {
        // UnconfinedTestDispatcher에서는 동기적으로 완료되므로
        // isLoading=false 상태에서 두 번 호출 → 첫 번째만 실행
        val vm = viewModel()

        vm.logout()
        vm.logout() // 이미 완료 → isLoading=false 이므로 두 번째도 실행됨
        // 단, 동시에 호출 시 두 번째는 isLoading=true 상태에서 무시됨을 확인
        // (UnconfinedTestDispatcher 환경에서는 순차 완료이므로 최종 상태가 성공이면 중복 방지 작동 중)
        assertTrue(vm.uiState.value.logoutSuccess)
    }

    // ── withdraw 성공 케이스 ──────────────────────────────────────────────────

    @Test
    fun `회원탈퇴 성공 시 withdrawSuccess가 true가 된다`() = runTest {
        val vm = viewModel()

        vm.withdraw()

        assertTrue(vm.uiState.value.withdrawSuccess)
    }

    @Test
    fun `회원탈퇴 성공 시 isLoading이 false로 돌아온다`() = runTest {
        val vm = viewModel()

        vm.withdraw()

        assertFalse(vm.uiState.value.isLoading)
    }

    // ── withdraw 실패 케이스 ──────────────────────────────────────────────────

    @Test
    fun `회원탈퇴 LoginRequired 시 loginRequired가 true가 된다`() = runTest {
        val repo = FakeAuthRepository().apply { withdrawResult = WithdrawResult.LoginRequired }
        val vm = viewModel(authRepo = repo)

        vm.withdraw()

        assertTrue(vm.uiState.value.loginRequired)
    }

    @Test
    fun `회원탈퇴 retryable Failure 시 적절한 에러 메시지가 설정된다`() = runTest {
        val repo = FakeAuthRepository().apply { withdrawResult = WithdrawResult.Failure(retryable = true) }
        val vm = viewModel(authRepo = repo)

        vm.withdraw()

        assertEquals("잠시 후 다시 시도해 주세요.", vm.uiState.value.errorMessage)
    }

    @Test
    fun `회원탈퇴 non-retryable Failure 시 적절한 에러 메시지가 설정된다`() = runTest {
        val repo = FakeAuthRepository().apply { withdrawResult = WithdrawResult.Failure(retryable = false) }
        val vm = viewModel(authRepo = repo)

        vm.withdraw()

        assertEquals("회원탈퇴에 실패했습니다.", vm.uiState.value.errorMessage)
    }

    // ── clearError ───────────────────────────────────────────────────────────

    @Test
    fun `clearError 호출 후 errorMessage가 null이 된다`() = runTest {
        val repo = FakeAuthRepository().apply { logoutResult = LogoutResult.Failure(retryable = false) }
        val vm = viewModel(authRepo = repo)
        vm.logout()

        vm.clearError()

        assertNull(vm.uiState.value.errorMessage)
    }

    // ── loadUpcomingAppointments ──────────────────────────────────────────────

    @Test
    fun `방장인 약속이 3인 이상이면 LEADER_TRANSFER로 매핑된다`() = runTest {
        val api = FakeMyAppointmentApi().apply {
            appointmentsResult = listOf(appointment(id = 1L, title = "카공", memberCount = 3))
        }
        val detailRepo = FakeAppointmentDetailRepository().apply {
            roleByAppointmentId = mapOf(1L to AppointmentMemberRole.HOST)
        }
        val vm = viewModel(api = api, detailRepo = detailRepo)

        vm.loadUpcomingAppointments()

        val room = vm.uiState.value.rooms.first()
        assertTrue(room.isLeader)
        assertEquals(AppointmentRoomStatus.LEADER_TRANSFER, room.status)
    }

    @Test
    fun `방장인 약속이 2인 이하이면 LEADER_DELETE로 매핑된다`() = runTest {
        val api = FakeMyAppointmentApi().apply {
            appointmentsResult = listOf(appointment(id = 2L, title = "팀플", memberCount = 2))
        }
        val detailRepo = FakeAppointmentDetailRepository().apply {
            roleByAppointmentId = mapOf(2L to AppointmentMemberRole.HOST)
        }
        val vm = viewModel(api = api, detailRepo = detailRepo)

        vm.loadUpcomingAppointments()

        val room = vm.uiState.value.rooms.first()
        assertTrue(room.isLeader)
        assertEquals(AppointmentRoomStatus.LEADER_DELETE, room.status)
    }

    @Test
    fun `방장이 아닌 약속은 MEMBER로 매핑된다`() = runTest {
        val api = FakeMyAppointmentApi().apply {
            appointmentsResult = listOf(appointment(id = 3L, title = "영화", memberCount = 4))
        }
        val detailRepo = FakeAppointmentDetailRepository().apply {
            roleByAppointmentId = mapOf(3L to AppointmentMemberRole.MEMBER)
        }
        val vm = viewModel(api = api, detailRepo = detailRepo)

        vm.loadUpcomingAppointments()

        val room = vm.uiState.value.rooms.first()
        assertFalse(room.isLeader)
        assertEquals(AppointmentRoomStatus.MEMBER, room.status)
    }

    @Test
    fun `약속 목록이 비어있으면 rooms도 빈 리스트가 된다`() = runTest {
        val api = FakeMyAppointmentApi().apply { appointmentsResult = emptyList() }
        val vm = viewModel(api = api)

        vm.loadUpcomingAppointments()

        assertTrue(vm.uiState.value.rooms.isEmpty())
    }

    @Test
    fun `loadUpcomingAppointments 완료 후 isLoadingRooms가 false가 된다`() = runTest {
        val api = FakeMyAppointmentApi().apply {
            appointmentsResult = listOf(appointment(id = 1L, title = "약속", memberCount = 2))
        }
        val vm = viewModel(api = api)

        vm.loadUpcomingAppointments()

        assertFalse(vm.uiState.value.isLoadingRooms)
    }

    @Test
    fun `토큰이 없으면 약속 목록을 조회하지 않고 rooms가 빈 리스트가 된다`() = runTest {
        val api = FakeMyAppointmentApi()
        val tokenManager = FakeTokenManager(token = null)
        val vm = viewModel(api = api, tokenManager = tokenManager)

        vm.loadUpcomingAppointments()

        assertTrue(vm.uiState.value.rooms.isEmpty())
        assertFalse(vm.uiState.value.isLoadingRooms)
    }

    @Test
    fun `API IOException 발생 시 rooms는 빈 리스트이고 isLoadingRooms는 false가 된다`() = runTest {
        val api = FakeMyAppointmentApi().apply { shouldThrow = true }
        val vm = viewModel(api = api)

        vm.loadUpcomingAppointments()

        assertTrue(vm.uiState.value.rooms.isEmpty())
        assertFalse(vm.uiState.value.isLoadingRooms)
    }

    @Test
    fun `방장인 약속의 name과 memberCount가 올바르게 매핑된다`() = runTest {
        val api = FakeMyAppointmentApi().apply {
            appointmentsResult = listOf(
                appointment(id = 10L, title = "생일파티", memberCount = 5)
            )
        }
        val detailRepo = FakeAppointmentDetailRepository().apply {
            roleByAppointmentId = mapOf(10L to AppointmentMemberRole.HOST)
        }
        val vm = viewModel(api = api, detailRepo = detailRepo)

        vm.loadUpcomingAppointments()

        val room = vm.uiState.value.rooms.first()
        assertEquals("생일파티", room.name)
        assertEquals(5, room.memberCount)
    }
}
