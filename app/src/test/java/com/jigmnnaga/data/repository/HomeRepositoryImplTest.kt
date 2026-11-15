package com.jigmnnaga.data.repository

import com.jigmnnaga.core.datastore.TokenManager
import com.jigmnnaga.data.remote.api.InviteApi
import com.jigmnnaga.data.remote.api.MyAppointmentApi
import com.jigmnnaga.data.remote.dto.ApiResponse
import com.jigmnnaga.data.remote.dto.InviteAcceptDto
import com.jigmnnaga.data.remote.dto.InviteDetailDto
import com.jigmnnaga.data.remote.dto.MyAppointmentResponseDto
import com.jigmnnaga.data.remote.dto.ReceivedInviteDto
import com.jigmnnaga.domain.model.HomeAppointmentListResult
import com.jigmnnaga.domain.model.NextAppointmentResult
import com.jigmnnaga.domain.model.PendingInviteCountResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class HomeRepositoryImplTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // ── Fakes ────────────────────────────────────────────────────────────────

    private class FakeMyAppointmentApi : MyAppointmentApi {
        var onGetMyAppointments: suspend (String?, String) -> Response<ApiResponse<List<MyAppointmentResponseDto>>> =
            { _, _ -> error("not set") }
        var lastType: String? = null
        var lastAuth: String? = null

        var onGetNextAppointment: suspend (String?) -> Response<ApiResponse<MyAppointmentResponseDto?>> =
            { _ -> error("not set") }

        override suspend fun getMyAppointments(
            authorization: String?,
            type: String
        ): Response<ApiResponse<List<MyAppointmentResponseDto>>> {
            lastType = type
            lastAuth = authorization
            return onGetMyAppointments(authorization, type)
        }

        override suspend fun getNextAppointment(
            authorization: String?
        ): Response<ApiResponse<MyAppointmentResponseDto?>> = onGetNextAppointment(authorization)
    }

    private class FakeInviteApi : InviteApi {
        var onGetReceivedInvites: suspend (String?) -> Response<ApiResponse<List<ReceivedInviteDto>>> =
            { _ -> error("not set") }

        override suspend fun getInvite(authorization: String?, inviteCode: String):
            Response<ApiResponse<InviteDetailDto>> = error("not used")

        override suspend fun acceptInvite(authorization: String?, inviteCode: String):
            Response<ApiResponse<InviteAcceptDto>> = error("not used")

        override suspend fun declineInvite(authorization: String?, inviteCode: String):
            Response<Unit> = error("not used")

        override suspend fun getReceivedInvites(authorization: String?):
            Response<ApiResponse<List<ReceivedInviteDto>>> = onGetReceivedInvites(authorization)
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

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun repo(
        appointmentApi: FakeMyAppointmentApi = FakeMyAppointmentApi(),
        inviteApi: FakeInviteApi = FakeInviteApi(),
        token: String? = "valid-access-token"
    ) = HomeRepositoryImpl(appointmentApi, inviteApi, FakeTokenManager(token), json)

    private fun appointmentListSuccessResponse(
        appointments: List<MyAppointmentResponseDto> = emptyList()
    ) = Response.success(ApiResponse(data = appointments))

    private fun nextSuccessResponse(
        dto: MyAppointmentResponseDto? = null
    ): Response<ApiResponse<MyAppointmentResponseDto?>> = Response.success(ApiResponse(data = dto))

    private fun inviteListSuccessResponse(
        invites: List<ReceivedInviteDto> = emptyList()
    ) = Response.success(ApiResponse(data = invites))

    private fun appointmentListErrorResponse(
        httpStatus: Int,
        code: String = "ERROR"
    ) = """{"code":"$code","message":"error"}"""
        .toResponseBody("application/json".toMediaType())
        .let { Response.error<ApiResponse<List<MyAppointmentResponseDto>>>(httpStatus, it) }

    private fun nextErrorResponse(
        httpStatus: Int,
        code: String = "ERROR"
    ) = """{"code":"$code","message":"error"}"""
        .toResponseBody("application/json".toMediaType())
        .let { Response.error<ApiResponse<MyAppointmentResponseDto?>>(httpStatus, it) }

    private fun inviteErrorResponse(
        httpStatus: Int,
        code: String = "ERROR"
    ) = """{"code":"$code","message":"error"}"""
        .toResponseBody("application/json".toMediaType())
        .let { Response.error<ApiResponse<List<ReceivedInviteDto>>>(httpStatus, it) }

    private fun dto(
        id: Long = 1L,
        title: String = "테스트 약속",
        confirmedDate: String? = "2025-10-24",
        confirmedTime: String? = "15:00",
        memberCount: Int = 4
    ) = MyAppointmentResponseDto(
        appointmentId = id,
        title = title,
        canceled = false,
        confirmed = confirmedDate != null,
        confirmedDate = confirmedDate,
        confirmedTime = confirmedTime,
        memberCount = memberCount
    )

    private fun invite(id: Long = 1L) = ReceivedInviteDto(
        inviteCode = "code-$id",
        appointmentId = id,
        title = "초대 $id",
        expired = false
    )

    // ── getUpcomingAppointments: 토큰 ─────────────────────────────────────────

    @Test
    fun `토큰이 null이면 LoginRequired를 반환한다`() = runTest {
        assertEquals(
            HomeAppointmentListResult.LoginRequired,
            repo(token = null).getUpcomingAppointments()
        )
    }

    @Test
    fun `토큰이 빈 문자열이면 LoginRequired를 반환한다`() = runTest {
        assertEquals(
            HomeAppointmentListResult.LoginRequired,
            repo(token = "").getUpcomingAppointments()
        )
    }

    // ── getUpcomingAppointments: 정상 응답 ────────────────────────────────────

    @Test
    fun `예정 약속 정상 응답 시 Success를 반환한다`() = runTest {
        val api = FakeMyAppointmentApi().apply {
            onGetMyAppointments = { _, _ -> appointmentListSuccessResponse(listOf(dto())) }
        }

        assertTrue(repo(api).getUpcomingAppointments() is HomeAppointmentListResult.Success)
    }

    @Test
    fun `예정 약속 빈 목록도 Success로 반환한다`() = runTest {
        val api = FakeMyAppointmentApi().apply {
            onGetMyAppointments = { _, _ -> appointmentListSuccessResponse(emptyList()) }
        }

        val result = repo(api).getUpcomingAppointments() as HomeAppointmentListResult.Success

        assertTrue(result.appointments.isEmpty())
    }

    @Test
    fun `예정 약속 조회 시 type이 UPCOMING으로 전달된다`() = runTest {
        val api = FakeMyAppointmentApi().apply {
            onGetMyAppointments = { _, _ -> appointmentListSuccessResponse() }
        }

        repo(api).getUpcomingAppointments()

        assertEquals("UPCOMING", api.lastType)
    }

    @Test
    fun `예정 약속 조회 시 Bearer 토큰이 헤더에 포함된다`() = runTest {
        val api = FakeMyAppointmentApi().apply {
            onGetMyAppointments = { _, _ -> appointmentListSuccessResponse() }
        }

        repo(api, token = "my-token").getUpcomingAppointments()

        assertEquals("Bearer my-token", api.lastAuth)
    }

    @Test
    fun `확정된 약속의 필드가 도메인 모델에 정확히 매핑된다`() = runTest {
        val source = dto(id = 42L, title = "확정 약속", confirmedDate = "2025-11-01", confirmedTime = "14:30")
        val api = FakeMyAppointmentApi().apply {
            onGetMyAppointments = { _, _ -> appointmentListSuccessResponse(listOf(source)) }
        }

        val result = repo(api).getUpcomingAppointments() as HomeAppointmentListResult.Success
        val appointment = result.appointments.first()

        assertEquals(42L, appointment.appointmentId)
        assertEquals("확정 약속", appointment.title)
        assertEquals("2025-11-01", appointment.confirmedDate)
        assertEquals("14:30", appointment.confirmedTime)
    }

    @Test
    fun `미확정 약속의 nullable 필드는 null로 매핑된다`() = runTest {
        val source = MyAppointmentResponseDto(
            appointmentId = 10L,
            title = "미확정 약속",
            confirmed = false,
            confirmedDate = null,
            confirmedTime = null,
            confirmedLocationName = null,
            memberCount = 2
        )
        val api = FakeMyAppointmentApi().apply {
            onGetMyAppointments = { _, _ -> appointmentListSuccessResponse(listOf(source)) }
        }

        val result = repo(api).getUpcomingAppointments() as HomeAppointmentListResult.Success
        val appointment = result.appointments.first()

        assertNull(appointment.confirmedDate)
        assertNull(appointment.confirmedTime)
        assertNull(appointment.confirmedLocationName)
    }

    @Test
    fun `appointmentId가 그대로 유지된다`() = runTest {
        val api = FakeMyAppointmentApi().apply {
            onGetMyAppointments = { _, _ -> appointmentListSuccessResponse(listOf(dto(id = 99L))) }
        }

        val result = repo(api).getUpcomingAppointments() as HomeAppointmentListResult.Success

        assertEquals(99L, result.appointments.first().appointmentId)
    }

    // ── getUpcomingAppointments: 오류 응답 ────────────────────────────────────

    @Test
    fun `예정 약속 401 응답 시 LoginRequired를 반환한다`() = runTest {
        val api = FakeMyAppointmentApi().apply {
            onGetMyAppointments = { _, _ -> appointmentListErrorResponse(401, "UNAUTHORIZED") }
        }

        assertEquals(HomeAppointmentListResult.LoginRequired, repo(api).getUpcomingAppointments())
    }

    @Test
    fun `예정 약속 EXPIRED_ACCESS_TOKEN 코드면 LoginRequired를 반환한다`() = runTest {
        val api = FakeMyAppointmentApi().apply {
            onGetMyAppointments = { _, _ -> appointmentListErrorResponse(401, "EXPIRED_ACCESS_TOKEN") }
        }

        assertEquals(HomeAppointmentListResult.LoginRequired, repo(api).getUpcomingAppointments())
    }

    @Test
    fun `예정 약속 INVALID_ACCESS_TOKEN 코드면 LoginRequired를 반환한다`() = runTest {
        val api = FakeMyAppointmentApi().apply {
            onGetMyAppointments = { _, _ -> appointmentListErrorResponse(401, "INVALID_ACCESS_TOKEN") }
        }

        assertEquals(HomeAppointmentListResult.LoginRequired, repo(api).getUpcomingAppointments())
    }

    @Test
    fun `예정 약속 500 응답 시 retryable Failure를 반환한다`() = runTest {
        val api = FakeMyAppointmentApi().apply {
            onGetMyAppointments = { _, _ -> appointmentListErrorResponse(500, "INTERNAL_SERVER_ERROR") }
        }

        val result = repo(api).getUpcomingAppointments() as HomeAppointmentListResult.Failure

        assertTrue(result.retryable)
    }

    @Test
    fun `예정 약속 400 응답 시 non-retryable Failure를 반환한다`() = runTest {
        val api = FakeMyAppointmentApi().apply {
            onGetMyAppointments = { _, _ -> appointmentListErrorResponse(400, "BAD_REQUEST") }
        }

        val result = repo(api).getUpcomingAppointments() as HomeAppointmentListResult.Failure

        assertFalse(result.retryable)
    }

    @Test
    fun `예정 약속 IOException 시 retryable Failure를 반환한다`() = runTest {
        val api = FakeMyAppointmentApi().apply {
            onGetMyAppointments = { _, _ -> throw IOException("network error") }
        }

        val result = repo(api).getUpcomingAppointments() as HomeAppointmentListResult.Failure

        assertTrue(result.retryable)
    }

    // ── getPastAppointments ───────────────────────────────────────────────────

    @Test
    fun `지난 약속 정상 응답 시 Success를 반환한다`() = runTest {
        val api = FakeMyAppointmentApi().apply {
            onGetMyAppointments = { _, _ -> appointmentListSuccessResponse(listOf(dto())) }
        }

        assertTrue(repo(api).getPastAppointments() is HomeAppointmentListResult.Success)
    }

    @Test
    fun `지난 약속 빈 목록도 Success로 반환한다`() = runTest {
        val api = FakeMyAppointmentApi().apply {
            onGetMyAppointments = { _, _ -> appointmentListSuccessResponse(emptyList()) }
        }

        val result = repo(api).getPastAppointments() as HomeAppointmentListResult.Success

        assertTrue(result.appointments.isEmpty())
    }

    @Test
    fun `지난 약속 조회 시 type이 PAST로 전달된다`() = runTest {
        val api = FakeMyAppointmentApi().apply {
            onGetMyAppointments = { _, _ -> appointmentListSuccessResponse() }
        }

        repo(api).getPastAppointments()

        assertEquals("PAST", api.lastType)
    }

    @Test
    fun `지난 약속 서버 반환 순서가 유지된다`() = runTest {
        val list = listOf(dto(1L, "첫 번째"), dto(2L, "두 번째"), dto(3L, "세 번째"))
        val api = FakeMyAppointmentApi().apply {
            onGetMyAppointments = { _, _ -> appointmentListSuccessResponse(list) }
        }

        val result = repo(api).getPastAppointments() as HomeAppointmentListResult.Success

        assertEquals(listOf(1L, 2L, 3L), result.appointments.map { it.appointmentId })
    }

    @Test
    fun `지난 약속 토큰 없으면 LoginRequired를 반환한다`() = runTest {
        assertEquals(
            HomeAppointmentListResult.LoginRequired,
            repo(token = null).getPastAppointments()
        )
    }

    // ── getNextAppointment ────────────────────────────────────────────────────

    @Test
    fun `다음 약속 정상 응답 시 Success에 약속이 담긴다`() = runTest {
        val source = dto(id = 5L, title = "다음 약속")
        val api = FakeMyAppointmentApi().apply {
            onGetNextAppointment = { nextSuccessResponse(source) }
        }

        val result = repo(api).getNextAppointment() as NextAppointmentResult.Success

        assertNotNull(result.appointment)
        assertEquals(5L, result.appointment?.appointmentId)
    }

    @Test
    fun `다음 약속 data가 null이면 Success에 null이 담긴다`() = runTest {
        val api = FakeMyAppointmentApi().apply {
            onGetNextAppointment = { nextSuccessResponse(null) }
        }

        val result = repo(api).getNextAppointment() as NextAppointmentResult.Success

        assertNull(result.appointment)
    }

    @Test
    fun `다음 약속 토큰 없으면 LoginRequired를 반환한다`() = runTest {
        assertEquals(
            NextAppointmentResult.LoginRequired,
            repo(token = null).getNextAppointment()
        )
    }

    @Test
    fun `다음 약속 401 응답 시 LoginRequired를 반환한다`() = runTest {
        val api = FakeMyAppointmentApi().apply {
            onGetNextAppointment = { nextErrorResponse(401, "UNAUTHORIZED") }
        }

        assertEquals(NextAppointmentResult.LoginRequired, repo(api).getNextAppointment())
    }

    @Test
    fun `다음 약속 500 응답 시 retryable Failure를 반환한다`() = runTest {
        val api = FakeMyAppointmentApi().apply {
            onGetNextAppointment = { nextErrorResponse(500) }
        }

        val result = repo(api).getNextAppointment() as NextAppointmentResult.Failure

        assertTrue(result.retryable)
    }

    @Test
    fun `다음 약속 IOException 시 retryable Failure를 반환한다`() = runTest {
        val api = FakeMyAppointmentApi().apply {
            onGetNextAppointment = { throw IOException("network") }
        }

        val result = repo(api).getNextAppointment() as NextAppointmentResult.Failure

        assertTrue(result.retryable)
    }

    // ── getPendingInviteCount ─────────────────────────────────────────────────

    @Test
    fun `초대 목록 3개이면 count 3을 반환한다`() = runTest {
        val fakeInvite = FakeInviteApi().apply {
            onGetReceivedInvites = { inviteListSuccessResponse(listOf(invite(1), invite(2), invite(3))) }
        }

        val result = HomeRepositoryImpl(FakeMyAppointmentApi(), fakeInvite, FakeTokenManager(), json)
            .getPendingInviteCount() as PendingInviteCountResult.Success

        assertEquals(3, result.count)
    }

    @Test
    fun `초대 목록이 비어있으면 count 0을 반환한다`() = runTest {
        val fakeInvite = FakeInviteApi().apply {
            onGetReceivedInvites = { inviteListSuccessResponse(emptyList()) }
        }

        val result = HomeRepositoryImpl(FakeMyAppointmentApi(), fakeInvite, FakeTokenManager(), json)
            .getPendingInviteCount() as PendingInviteCountResult.Success

        assertEquals(0, result.count)
    }

    @Test
    fun `초대 토큰 없으면 LoginRequired를 반환한다`() = runTest {
        assertEquals(
            PendingInviteCountResult.LoginRequired,
            repo(token = null).getPendingInviteCount()
        )
    }

    @Test
    fun `초대 401 응답 시 LoginRequired를 반환한다`() = runTest {
        val fakeInvite = FakeInviteApi().apply {
            onGetReceivedInvites = { inviteErrorResponse(401, "UNAUTHORIZED") }
        }

        assertEquals(
            PendingInviteCountResult.LoginRequired,
            HomeRepositoryImpl(FakeMyAppointmentApi(), fakeInvite, FakeTokenManager(), json).getPendingInviteCount()
        )
    }

    @Test
    fun `초대 500 응답 시 retryable Failure를 반환한다`() = runTest {
        val fakeInvite = FakeInviteApi().apply {
            onGetReceivedInvites = { inviteErrorResponse(500) }
        }

        val result = HomeRepositoryImpl(FakeMyAppointmentApi(), fakeInvite, FakeTokenManager(), json)
            .getPendingInviteCount() as PendingInviteCountResult.Failure

        assertTrue(result.retryable)
    }

    @Test
    fun `초대 400 응답 시 non-retryable Failure를 반환한다`() = runTest {
        val fakeInvite = FakeInviteApi().apply {
            onGetReceivedInvites = { inviteErrorResponse(400) }
        }

        val result = HomeRepositoryImpl(FakeMyAppointmentApi(), fakeInvite, FakeTokenManager(), json)
            .getPendingInviteCount() as PendingInviteCountResult.Failure

        assertFalse(result.retryable)
    }

    @Test
    fun `초대 API Failure는 약속 목록 Result 타입에 영향을 주지 않는다`() = runTest {
        // 초대 API 실패가 PendingInviteCountResult.Failure를 반환할 뿐,
        // 다른 API 호출 결과 타입(HomeAppointmentListResult 등)에는 영향이 없다
        val fakeInvite = FakeInviteApi().apply {
            onGetReceivedInvites = { inviteErrorResponse(500) }
        }

        val result = HomeRepositoryImpl(FakeMyAppointmentApi(), fakeInvite, FakeTokenManager(), json)
            .getPendingInviteCount()

        assertTrue(result is PendingInviteCountResult.Failure)
    }
}
