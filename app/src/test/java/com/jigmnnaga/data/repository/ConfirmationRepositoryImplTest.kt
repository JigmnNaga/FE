package com.jigmnnaga.data.repository

import com.jigmnnaga.core.datastore.TokenManager
import com.jigmnnaga.data.remote.api.ConfirmationApi
import com.jigmnnaga.data.remote.dto.ApiResponse
import com.jigmnnaga.data.remote.dto.ConfirmDateTimeRequestDto
import com.jigmnnaga.data.remote.dto.ConfirmLocationRequestDto
import com.jigmnnaga.data.remote.dto.DetailPlaceDto
import com.jigmnnaga.data.remote.dto.ScheduleHeatmapDto
import com.jigmnnaga.data.remote.dto.TimeRangeCandidateDto
import com.jigmnnaga.domain.model.ConfirmDetailPlace
import com.jigmnnaga.domain.model.ConfirmationRejectReason
import com.jigmnnaga.domain.model.ConfirmationResult
import com.jigmnnaga.domain.model.TimeCandidate
import com.jigmnnaga.domain.model.TimeCandidates
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class ConfirmationRepositoryImplTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private class FakeApi : ConfirmationApi {
        var onHeatmap: suspend () -> Response<ApiResponse<ScheduleHeatmapDto>> = { error("not set") }
        var onDateTime: suspend () -> Response<Unit> = { error("not set") }
        var onLocation: suspend () -> Response<Unit> = { error("not set") }
        var calls = 0
        var lastAuthorization: String? = null
        var lastAppointmentId: Long? = null
        var lastSlotMinutes: Int? = null
        var lastDateTime: ConfirmDateTimeRequestDto? = null
        var lastLocation: ConfirmLocationRequestDto? = null

        override suspend fun getHeatmap(
            authorization: String?,
            appointmentId: Long,
            slotMinutes: Int
        ): Response<ApiResponse<ScheduleHeatmapDto>> {
            calls++; lastAuthorization = authorization; lastAppointmentId = appointmentId
            lastSlotMinutes = slotMinutes
            return onHeatmap()
        }

        override suspend fun confirmDateTime(
            authorization: String?,
            appointmentId: Long,
            request: ConfirmDateTimeRequestDto
        ): Response<Unit> {
            calls++; lastAuthorization = authorization; lastAppointmentId = appointmentId
            lastDateTime = request
            return onDateTime()
        }

        override suspend fun confirmLocation(
            authorization: String?,
            appointmentId: Long,
            request: ConfirmLocationRequestDto
        ): Response<Unit> {
            calls++; lastAuthorization = authorization; lastAppointmentId = appointmentId
            lastLocation = request
            return onLocation()
        }
    }

    private class FakeTokenManager(private val token: String?) : TokenManager {
        override val accessToken: Flow<String?> = flowOf(token)
        override val refreshToken: Flow<String?> = flowOf(null)
        override suspend fun saveTokens(accessToken: String, refreshToken: String) = Unit
        override suspend fun clearTokens() = Unit
        override suspend fun isAccessTokenExpired(): Boolean = false
        override suspend fun isRefreshTokenExpired(): Boolean = false
        override suspend fun validateAndClearInvalidTokens(): Boolean = true
    }

    private fun repo(api: FakeApi, token: String? = "token") =
        ConfirmationRepositoryImpl(api, FakeTokenManager(token), json)

    private fun <T> errorBody(code: Int, body: String): Response<T> =
        Response.error(code, body.toResponseBody("application/json".toMediaType()))

    @Test
    fun `시간 후보 조회는 토큰과 약속 ID와 슬롯 단위를 보내고 응답을 변환한다`() = runTest {
        val api = FakeApi().apply {
            onHeatmap = {
                Response.success(
                    ApiResponse(
                        data = ScheduleHeatmapDto(
                            slotMinutes = 60,
                            totalMemberCount = 4,
                            topCandidates = listOf(TimeRangeCandidateDto("2026-11-20", "19:00", "21:00", 3))
                        )
                    )
                )
            }
        }

        val result = repo(api).getTimeCandidates(7L)

        assertEquals(
            ConfirmationResult.Success(
                TimeCandidates(4, listOf(TimeCandidate("2026-11-20", "19:00", "21:00", 3)))
            ),
            result
        )
        assertEquals("Bearer token", api.lastAuthorization)
        assertEquals(7L, api.lastAppointmentId)
        assertEquals(60, api.lastSlotMinutes)
    }

    @Test
    fun `날짜·시간 확정은 날짜와 시간을 보낸다`() = runTest {
        val api = FakeApi().apply { onDateTime = { Response.success(Unit) } }
        val result = repo(api).confirmDateTime(7L, "2026-11-20", "19:00")
        assertEquals(ConfirmationResult.Success(Unit), result)
        assertEquals(ConfirmDateTimeRequestDto("2026-11-20", "19:00"), api.lastDateTime)
        assertEquals(7L, api.lastAppointmentId)
    }

    @Test
    fun `장소 확정은 장소 ID와 세부 위치를 보내고 세부 위치가 없으면 비워서 보낸다`() = runTest {
        val api = FakeApi().apply { onLocation = { Response.success(Unit) } }

        repo(api).confirmLocation(7L, 3L, ConfirmDetailPlace("스터디카페", "강남대로 390", 37.49, 127.02))
        assertEquals(
            ConfirmLocationRequestDto(3L, DetailPlaceDto("스터디카페", "강남대로 390", 37.49, 127.02)),
            api.lastLocation
        )

        repo(api).confirmLocation(7L, 3L, null)
        assertEquals(3L, api.lastLocation?.locationId)
        assertNull(api.lastLocation?.detailPlace)
    }

    @Test
    fun `토큰이 없거나 비어 있으면 서버를 부르지 않고 로그인이 필요하다`() = runTest {
        for (token in listOf(null, "", "  ")) {
            val api = FakeApi()
            assertEquals(ConfirmationResult.LoginRequired, repo(api, token).confirmDateTime(7L, "2026-11-20", "19:00"))
            assertEquals(0, api.calls)
        }
    }

    @Test
    fun `토큰 오류 코드와 401은 로그인이 필요하다`() = runTest {
        for (code in listOf("UNAUTHORIZED", "EXPIRED_ACCESS_TOKEN", "INVALID_ACCESS_TOKEN")) {
            val api = FakeApi().apply { onDateTime = { errorBody(401, """{"code":"$code","message":"x"}""") } }
            assertEquals(ConfirmationResult.LoginRequired, repo(api).confirmDateTime(7L, "2026-11-20", "19:00"))
        }
        val api = FakeApi().apply { onDateTime = { errorBody(401, "") } }
        assertEquals(ConfirmationResult.LoginRequired, repo(api).confirmDateTime(7L, "2026-11-20", "19:00"))
    }

    @Test
    fun `방장이 아니거나 약속이 없으면 방장 아님으로 처리한다`() = runTest {
        for ((status, body) in listOf(
            403 to """{"code":"NOT_APPOINTMENT_HOST"}""",
            403 to """{"code":"NOT_APPOINTMENT_PARTICIPANT"}""",
            403 to "",
            404 to ""
        )) {
            val api = FakeApi().apply { onLocation = { errorBody(status, body) } }
            assertEquals(ConfirmationResult.NotHost, repo(api).confirmLocation(7L, 3L, null))
        }
    }

    @Test
    fun `서버가 거절한 이유별로 나눈다`() = runTest {
        val expected = mapOf(
            "APPOINTMENT_CANCELED" to ConfirmationRejectReason.APPOINTMENT_CANCELED,
            "INVALID_CONFIRMED_DATE" to ConfirmationRejectReason.INVALID_DATE,
            "INVALID_CONFIRMED_LOCATION" to ConfirmationRejectReason.INVALID_LOCATION,
            "INVALID_CANDIDATE_LOCATION" to ConfirmationRejectReason.INVALID_LOCATION
        )
        for ((code, reason) in expected) {
            val api = FakeApi().apply { onLocation = { errorBody(400, """{"code":"$code"}""") } }
            assertEquals(ConfirmationResult.Rejected(reason), repo(api).confirmLocation(7L, 3L, null))
        }
    }

    @Test
    fun `서버 오류와 네트워크 오류는 다시 시도할 수 있고 그 밖의 오류는 아니다`() = runTest {
        val server = FakeApi().apply { onDateTime = { errorBody(500, "") } }
        assertEquals(ConfirmationResult.Failure(true), repo(server).confirmDateTime(7L, "2026-11-20", "19:00"))

        val io = FakeApi().apply { onDateTime = { throw IOException("offline") } }
        assertEquals(ConfirmationResult.Failure(true), repo(io).confirmDateTime(7L, "2026-11-20", "19:00"))

        val bad = FakeApi().apply { onDateTime = { errorBody(400, """{"code":"X"}""") } }
        assertEquals(ConfirmationResult.Failure(false), repo(bad).confirmDateTime(7L, "2026-11-20", "19:00"))

        val empty = FakeApi().apply { onHeatmap = { Response.success(ApiResponse(data = null)) } }
        assertEquals(ConfirmationResult.Failure(false), repo(empty).getTimeCandidates(7L))
    }
}
