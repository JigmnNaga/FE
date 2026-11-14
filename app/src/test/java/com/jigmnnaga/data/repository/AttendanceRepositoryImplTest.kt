package com.jigmnnaga.data.repository

import com.jigmnnaga.core.datastore.TokenManager
import com.jigmnnaga.data.remote.api.AttendanceApi
import com.jigmnnaga.data.remote.dto.AttendanceRespondRequestDto
import com.jigmnnaga.domain.model.AttendanceAnswer
import com.jigmnnaga.domain.model.AttendanceRejectReason
import com.jigmnnaga.domain.model.AttendanceResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class AttendanceRepositoryImplTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private class FakeApi : AttendanceApi {
        var onRespond: suspend () -> Response<Unit> = { error("not set") }
        var calls = 0
        var lastAuthorization: String? = null
        var lastAppointmentId: Long? = null
        var lastRequest: AttendanceRespondRequestDto? = null

        override suspend fun respond(
            authorization: String?,
            appointmentId: Long,
            request: AttendanceRespondRequestDto
        ): Response<Unit> {
            calls++; lastAuthorization = authorization; lastAppointmentId = appointmentId
            lastRequest = request
            return onRespond()
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
        AttendanceRepositoryImpl(api, FakeTokenManager(token), json)

    private fun errorBody(code: Int, body: String): Response<Unit> =
        Response.error(code, body.toResponseBody("application/json".toMediaType()))

    @Test
    fun `참석 응답은 토큰과 약속 ID와 응답 값을 보낸다`() = runTest {
        val expected = mapOf(
            AttendanceAnswer.ATTENDING to "ATTENDING",
            AttendanceAnswer.NOT_ATTENDING to "NOT_ATTENDING",
            AttendanceAnswer.RESCHEDULE_REQUESTED to "RESCHEDULE_REQUESTED"
        )
        for ((answer, status) in expected) {
            val api = FakeApi().apply { onRespond = { Response.success(Unit) } }
            val result = repo(api).respond(7L, answer)

            assertEquals(AttendanceResult.Success(Unit), result)
            assertEquals("Bearer token", api.lastAuthorization)
            assertEquals(7L, api.lastAppointmentId)
            assertEquals(status, api.lastRequest?.attendanceStatus)
        }
    }

    @Test
    fun `토큰이 없거나 비어 있으면 서버를 부르지 않고 로그인이 필요하다`() = runTest {
        for (token in listOf(null, "", "  ")) {
            val api = FakeApi()
            assertEquals(
                AttendanceResult.LoginRequired,
                repo(api, token).respond(7L, AttendanceAnswer.ATTENDING)
            )
            assertEquals(0, api.calls)
        }
    }

    @Test
    fun `토큰 오류 코드와 401은 로그인이 필요하다`() = runTest {
        for (code in listOf("UNAUTHORIZED", "EXPIRED_ACCESS_TOKEN", "INVALID_ACCESS_TOKEN")) {
            val api = FakeApi().apply { onRespond = { errorBody(401, """{"code":"$code"}""") } }
            assertEquals(AttendanceResult.LoginRequired, repo(api).respond(7L, AttendanceAnswer.ATTENDING))
        }
        val api = FakeApi().apply { onRespond = { errorBody(401, "not json") } }
        assertEquals(AttendanceResult.LoginRequired, repo(api).respond(7L, AttendanceAnswer.ATTENDING))
    }

    @Test
    fun `참여자가 아니거나 약속이 없으면 참여자 아님이다`() = runTest {
        val forbidden = FakeApi().apply {
            onRespond = { errorBody(403, """{"code":"NOT_APPOINTMENT_PARTICIPANT"}""") }
        }
        assertEquals(AttendanceResult.NotParticipant, repo(forbidden).respond(7L, AttendanceAnswer.ATTENDING))

        val notFound = FakeApi().apply { onRespond = { errorBody(404, "") } }
        assertEquals(AttendanceResult.NotParticipant, repo(notFound).respond(7L, AttendanceAnswer.ATTENDING))
    }

    @Test
    fun `응답이 거절된 이유를 오류 코드로 구분한다`() = runTest {
        val expected = mapOf(
            "APPOINTMENT_CANCELED" to AttendanceRejectReason.APPOINTMENT_CANCELED,
            "APPOINTMENT_NOT_CONFIRMED" to AttendanceRejectReason.NOT_CONFIRMED,
            "APPOINTMENT_TIME_PASSED" to AttendanceRejectReason.TIME_PASSED,
            "INVALID_ATTENDANCE_STATUS" to AttendanceRejectReason.INVALID_ANSWER
        )
        for ((code, reason) in expected) {
            val api = FakeApi().apply { onRespond = { errorBody(400, """{"code":"$code"}""") } }
            assertEquals(
                AttendanceResult.Rejected(reason),
                repo(api).respond(7L, AttendanceAnswer.NOT_ATTENDING)
            )
        }
    }

    @Test
    fun `서버 오류는 재시도 가능하고 모르는 400은 재시도할 수 없다`() = runTest {
        val server = FakeApi().apply { onRespond = { errorBody(500, "") } }
        assertEquals(AttendanceResult.Failure(true), repo(server).respond(7L, AttendanceAnswer.ATTENDING))

        val client = FakeApi().apply { onRespond = { errorBody(400, """{"code":"BAD_REQUEST"}""") } }
        assertEquals(AttendanceResult.Failure(false), repo(client).respond(7L, AttendanceAnswer.ATTENDING))
    }

    @Test
    fun `네트워크 오류와 예상하지 못한 예외는 실패로 바뀐다`() = runTest {
        val network = FakeApi().apply { onRespond = { throw IOException("offline") } }
        assertEquals(AttendanceResult.Failure(true), repo(network).respond(7L, AttendanceAnswer.ATTENDING))

        val unexpected = FakeApi().apply { onRespond = { throw IllegalStateException("boom") } }
        assertEquals(AttendanceResult.Failure(false), repo(unexpected).respond(7L, AttendanceAnswer.ATTENDING))
    }
}
