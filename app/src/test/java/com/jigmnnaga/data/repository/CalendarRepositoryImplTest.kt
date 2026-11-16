package com.jigmnnaga.data.repository

import com.jigmnnaga.core.datastore.TokenManager
import com.jigmnnaga.data.remote.api.MyAppointmentApi
import com.jigmnnaga.data.remote.dto.ApiResponse
import com.jigmnnaga.data.remote.dto.CalendarDayResponseDto
import com.jigmnnaga.data.remote.dto.CalendarResponseDto
import com.jigmnnaga.data.remote.dto.InviteAcceptDto
import com.jigmnnaga.data.remote.dto.InviteDetailDto
import com.jigmnnaga.data.remote.dto.MyAppointmentResponseDto
import com.jigmnnaga.data.remote.dto.ReceivedInviteDto
import com.jigmnnaga.domain.model.CalendarResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class CalendarRepositoryImplTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // ── Fakes ────────────────────────────────────────────────────────────────

    private class FakeMyAppointmentApi : MyAppointmentApi {
        var onGetCalendar: suspend (String?, String) -> Response<ApiResponse<CalendarResponseDto>> =
            { _, _ -> error("not set") }
        var lastAuth: String? = null
        var lastMonth: String? = null

        override suspend fun getMyAppointments(
            authorization: String?,
            type: String
        ): Response<ApiResponse<List<MyAppointmentResponseDto>>> = error("not used")

        override suspend fun getNextAppointment(
            authorization: String?
        ): Response<ApiResponse<MyAppointmentResponseDto?>> = error("not used")

        override suspend fun getCalendar(
            authorization: String?,
            month: String
        ): Response<ApiResponse<CalendarResponseDto>> {
            lastAuth = authorization
            lastMonth = month
            return onGetCalendar(authorization, month)
        }
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
        api: FakeMyAppointmentApi = FakeMyAppointmentApi(),
        token: String? = "valid-access-token"
    ) = CalendarRepositoryImpl(api, FakeTokenManager(token), json)

    private fun successResponse(days: List<CalendarDayResponseDto> = emptyList()) =
        Response.success(ApiResponse(data = CalendarResponseDto(days)))

    private fun errorResponse(httpStatus: Int, code: String = "ERROR") =
        """{"code":"$code","message":"error"}"""
            .toResponseBody("application/json".toMediaType())
            .let { Response.error<ApiResponse<CalendarResponseDto>>(httpStatus, it) }

    private fun appointmentDto(
        id: Long = 1L,
        title: String = "테스트 약속",
        confirmedDate: String? = "2026-09-06",
        confirmedTime: String? = "15:00",
        confirmedLocationName: String? = "강남역",
        detailPlaceName: String? = null,
        confirmed: Boolean = true,
        memberCount: Int = 3
    ) = MyAppointmentResponseDto(
        appointmentId = id,
        title = title,
        canceled = false,
        confirmed = confirmed,
        confirmedDate = confirmedDate,
        confirmedTime = confirmedTime,
        confirmedLocationName = confirmedLocationName,
        detailPlaceName = detailPlaceName,
        memberCount = memberCount
    )

    private fun dayDto(
        date: String,
        appointments: List<MyAppointmentResponseDto>
    ) = CalendarDayResponseDto(date = date, appointments = appointments)

    // ── 토큰 없음 ─────────────────────────────────────────────────────────────

    @Test
    fun `토큰이 null이면 LoginRequired를 반환한다`() = runTest {
        assertEquals(
            CalendarResult.LoginRequired,
            repo(token = null).getCalendar("2026-09")
        )
    }

    @Test
    fun `토큰이 빈 문자열이면 LoginRequired를 반환한다`() = runTest {
        assertEquals(
            CalendarResult.LoginRequired,
            repo(token = "").getCalendar("2026-09")
        )
    }

    // ── 정상 응답 ─────────────────────────────────────────────────────────────

    @Test
    fun `정상 응답 시 Success를 반환한다`() = runTest {
        val api = FakeMyAppointmentApi().apply {
            onGetCalendar = { _, _ -> successResponse(listOf(dayDto("2026-09-06", listOf(appointmentDto())))) }
        }

        assertTrue(repo(api).getCalendar("2026-09") is CalendarResult.Success)
    }

    @Test
    fun `Bearer 토큰이 Authorization 헤더에 포함된다`() = runTest {
        val api = FakeMyAppointmentApi().apply {
            onGetCalendar = { _, _ -> successResponse() }
        }

        repo(api, token = "my-token").getCalendar("2026-09")

        assertEquals("Bearer my-token", api.lastAuth)
    }

    @Test
    fun `month 파라미터가 그대로 전달된다`() = runTest {
        val api = FakeMyAppointmentApi().apply {
            onGetCalendar = { _, _ -> successResponse() }
        }

        repo(api).getCalendar("2026-11")

        assertEquals("2026-11", api.lastMonth)
    }

    // ── 빈 월 응답 ────────────────────────────────────────────────────────────

    @Test
    fun `약속이 없는 월은 days가 빈 Success를 반환한다`() = runTest {
        val api = FakeMyAppointmentApi().apply {
            onGetCalendar = { _, _ -> successResponse(emptyList()) }
        }

        val result = repo(api).getCalendar("2026-09") as CalendarResult.Success

        assertTrue(result.days.isEmpty())
    }

    @Test
    fun `data가 null이면 days가 빈 Success를 반환한다`() = runTest {
        val api = FakeMyAppointmentApi().apply {
            onGetCalendar = { _, _ -> Response.success(ApiResponse(data = null)) }
        }

        val result = repo(api).getCalendar("2026-09") as CalendarResult.Success

        assertTrue(result.days.isEmpty())
    }

    // ── 여러 날짜·약속 ────────────────────────────────────────────────────────

    @Test
    fun `여러 날짜의 약속이 days 목록에 담긴다`() = runTest {
        val days = listOf(
            dayDto("2026-09-06", listOf(appointmentDto(id = 1L))),
            dayDto("2026-09-12", listOf(appointmentDto(id = 2L))),
            dayDto("2026-09-20", listOf(appointmentDto(id = 3L)))
        )
        val api = FakeMyAppointmentApi().apply {
            onGetCalendar = { _, _ -> successResponse(days) }
        }

        val result = repo(api).getCalendar("2026-09") as CalendarResult.Success

        assertEquals(3, result.days.size)
        assertEquals("2026-09-06", result.days[0].date)
        assertEquals("2026-09-12", result.days[1].date)
        assertEquals("2026-09-20", result.days[2].date)
    }

    @Test
    fun `같은 날짜에 여러 약속이 있으면 모두 포함된다`() = runTest {
        val day = dayDto(
            "2026-09-06",
            listOf(
                appointmentDto(id = 1L, confirmedTime = "10:00"),
                appointmentDto(id = 2L, confirmedTime = "14:00"),
                appointmentDto(id = 3L, confirmedTime = "18:00")
            )
        )
        val api = FakeMyAppointmentApi().apply {
            onGetCalendar = { _, _ -> successResponse(listOf(day)) }
        }

        val result = repo(api).getCalendar("2026-09") as CalendarResult.Success

        assertEquals(1, result.days.size)
        assertEquals(3, result.days[0].appointments.size)
        assertEquals(1L, result.days[0].appointments[0].appointmentId)
        assertEquals(2L, result.days[0].appointments[1].appointmentId)
        assertEquals(3L, result.days[0].appointments[2].appointmentId)
    }

    // ── DTO/Mapper 변환 정확성 ────────────────────────────────────────────────

    @Test
    fun `appointmentId가 도메인 모델에 정확히 매핑된다`() = runTest {
        val api = FakeMyAppointmentApi().apply {
            onGetCalendar = { _, _ ->
                successResponse(listOf(dayDto("2026-09-06", listOf(appointmentDto(id = 99L)))))
            }
        }

        val result = repo(api).getCalendar("2026-09") as CalendarResult.Success

        assertEquals(99L, result.days[0].appointments[0].appointmentId)
    }

    @Test
    fun `confirmedDate가 도메인 모델에 정확히 매핑된다`() = runTest {
        val api = FakeMyAppointmentApi().apply {
            onGetCalendar = { _, _ ->
                successResponse(
                    listOf(dayDto("2026-09-15", listOf(appointmentDto(confirmedDate = "2026-09-15"))))
                )
            }
        }

        val result = repo(api).getCalendar("2026-09") as CalendarResult.Success

        assertEquals("2026-09-15", result.days[0].appointments[0].confirmedDate)
    }

    @Test
    fun `날짜 date 필드가 CalendarDay에 정확히 매핑된다`() = runTest {
        val api = FakeMyAppointmentApi().apply {
            onGetCalendar = { _, _ ->
                successResponse(listOf(dayDto("2026-09-06", listOf(appointmentDto()))))
            }
        }

        val result = repo(api).getCalendar("2026-09") as CalendarResult.Success

        assertEquals("2026-09-06", result.days[0].date)
    }

    // ── confirmed=false이지만 confirmedDate가 있는 약속 ───────────────────────

    @Test
    fun `confirmed=false이지만 confirmedDate가 있는 약속도 정상 처리된다`() = runTest {
        val partiallyConfirmed = appointmentDto(
            id = 42L,
            confirmed = false,
            confirmedDate = "2026-09-10",
            confirmedLocationName = null
        )
        val api = FakeMyAppointmentApi().apply {
            onGetCalendar = { _, _ ->
                successResponse(listOf(dayDto("2026-09-10", listOf(partiallyConfirmed))))
            }
        }

        val result = repo(api).getCalendar("2026-09") as CalendarResult.Success

        val appointment = result.days[0].appointments[0]
        assertEquals(42L, appointment.appointmentId)
        assertFalse(appointment.confirmed)
        assertEquals("2026-09-10", appointment.confirmedDate)
    }

    // ── 장소 정보가 null인 약속 ───────────────────────────────────────────────

    @Test
    fun `장소 정보가 모두 null이어도 오류가 발생하지 않는다`() = runTest {
        val noLocation = appointmentDto(
            confirmedLocationName = null,
            detailPlaceName = null
        )
        val api = FakeMyAppointmentApi().apply {
            onGetCalendar = { _, _ ->
                successResponse(listOf(dayDto("2026-09-06", listOf(noLocation))))
            }
        }

        val result = repo(api).getCalendar("2026-09") as CalendarResult.Success

        val appointment = result.days[0].appointments[0]
        assertTrue(appointment.confirmedLocationName == null)
        assertTrue(appointment.detailPlaceName == null)
    }

    @Test
    fun `memberCount가 도메인 모델에 정확히 매핑된다`() = runTest {
        val api = FakeMyAppointmentApi().apply {
            onGetCalendar = { _, _ ->
                successResponse(listOf(dayDto("2026-09-06", listOf(appointmentDto(memberCount = 7)))))
            }
        }

        val result = repo(api).getCalendar("2026-09") as CalendarResult.Success

        assertEquals(7, result.days[0].appointments[0].memberCount)
    }

    // ── 인증 오류 ─────────────────────────────────────────────────────────────

    @Test
    fun `401 응답 시 LoginRequired를 반환한다`() = runTest {
        val api = FakeMyAppointmentApi().apply {
            onGetCalendar = { _, _ -> errorResponse(401, "UNAUTHORIZED") }
        }

        assertEquals(CalendarResult.LoginRequired, repo(api).getCalendar("2026-09"))
    }

    @Test
    fun `EXPIRED_ACCESS_TOKEN 코드면 LoginRequired를 반환한다`() = runTest {
        val api = FakeMyAppointmentApi().apply {
            onGetCalendar = { _, _ -> errorResponse(401, "EXPIRED_ACCESS_TOKEN") }
        }

        assertEquals(CalendarResult.LoginRequired, repo(api).getCalendar("2026-09"))
    }

    @Test
    fun `INVALID_ACCESS_TOKEN 코드면 LoginRequired를 반환한다`() = runTest {
        val api = FakeMyAppointmentApi().apply {
            onGetCalendar = { _, _ -> errorResponse(401, "INVALID_ACCESS_TOKEN") }
        }

        assertEquals(CalendarResult.LoginRequired, repo(api).getCalendar("2026-09"))
    }

    // ── 서버 오류 ─────────────────────────────────────────────────────────────

    @Test
    fun `500 응답 시 retryable Failure를 반환한다`() = runTest {
        val api = FakeMyAppointmentApi().apply {
            onGetCalendar = { _, _ -> errorResponse(500, "INTERNAL_SERVER_ERROR") }
        }

        val result = repo(api).getCalendar("2026-09") as CalendarResult.Failure

        assertTrue(result.retryable)
    }

    @Test
    fun `400 응답 시 non-retryable Failure를 반환한다`() = runTest {
        val api = FakeMyAppointmentApi().apply {
            onGetCalendar = { _, _ -> errorResponse(400, "BAD_REQUEST") }
        }

        val result = repo(api).getCalendar("2026-09") as CalendarResult.Failure

        assertFalse(result.retryable)
    }

    // ── 네트워크 오류 ─────────────────────────────────────────────────────────

    @Test
    fun `IOException 시 retryable Failure를 반환한다`() = runTest {
        val api = FakeMyAppointmentApi().apply {
            onGetCalendar = { _, _ -> throw IOException("network error") }
        }

        val result = repo(api).getCalendar("2026-09") as CalendarResult.Failure

        assertTrue(result.retryable)
    }

    @Test
    fun `일반 Exception 시 non-retryable Failure를 반환한다`() = runTest {
        val api = FakeMyAppointmentApi().apply {
            onGetCalendar = { _, _ -> throw RuntimeException("unexpected") }
        }

        val result = repo(api).getCalendar("2026-09") as CalendarResult.Failure

        assertFalse(result.retryable)
    }

    // ── 코루틴 취소 처리 ──────────────────────────────────────────────────────

    @Test
    fun `CancellationException은 잡지 않고 재던진다`() = runTest {
        val api = FakeMyAppointmentApi().apply {
            onGetCalendar = { _, _ -> throw CancellationException("cancelled") }
        }

        var threw = false
        try {
            repo(api).getCalendar("2026-09")
        } catch (e: CancellationException) {
            threw = true
        }

        assertTrue(threw)
    }
}
