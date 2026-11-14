package com.jigmnnaga.data.repository

import com.jigmnnaga.core.datastore.TokenManager
import com.jigmnnaga.data.remote.api.AppointmentCreateApi
import com.jigmnnaga.data.remote.dto.ApiResponse
import com.jigmnnaga.data.remote.dto.AppointmentCreateRequestDto
import com.jigmnnaga.data.remote.dto.AppointmentCreateResponseDto
import com.jigmnnaga.data.remote.dto.CreateCandidateLocationDto
import com.jigmnnaga.data.remote.dto.CreatePreviousMembersDto
import com.jigmnnaga.domain.model.AppointmentCreateRejectReason
import com.jigmnnaga.domain.model.AppointmentCreateResult
import com.jigmnnaga.domain.model.CreatedAppointment
import com.jigmnnaga.domain.model.NewAppointment
import com.jigmnnaga.domain.model.NewAppointmentDateType
import com.jigmnnaga.domain.model.NewCandidateLocation
import com.jigmnnaga.domain.model.PreviousMembers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class AppointmentCreateRepositoryImplTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private class FakeApi : AppointmentCreateApi {
        var onCreate: suspend () -> Response<ApiResponse<AppointmentCreateResponseDto>> = { error("not set") }
        var calls = 0
        var lastAuthorization: String? = null
        var lastRequest: AppointmentCreateRequestDto? = null

        override suspend fun createAppointment(
            authorization: String?,
            request: AppointmentCreateRequestDto
        ): Response<ApiResponse<AppointmentCreateResponseDto>> {
            calls++; lastAuthorization = authorization; lastRequest = request
            return onCreate()
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
        AppointmentCreateRepositoryImpl(api, FakeTokenManager(token), json)

    private fun <T> errorBody(code: Int, body: String): Response<T> =
        Response.error(code, body.toResponseBody("application/json".toMediaType()))

    private fun appointment(previousMembers: PreviousMembers? = null) = NewAppointment(
        title = "저녁 모임",
        dateType = NewAppointmentDateType.RANGE,
        candidateDates = listOf("2026-12-05", "2026-12-06"),
        candidateLocations = listOf(
            NewCandidateLocation("강남역", "서울특별시 강남구", "2호선, 신분당선", 37.49, 127.02),
            NewCandidateLocation("잠실역", null, null, 37.51, 127.10)
        ),
        responseDeadline = "2026-12-03T23:59:59",
        previousMembers = previousMembers
    )

    private fun success(
        inviteUrl: String? = "https://example.com/invite/abc",
        inviteCode: String? = "abc"
    ): Response<ApiResponse<AppointmentCreateResponseDto>> = Response.success(
        ApiResponse(
            data = AppointmentCreateResponseDto(
                appointmentId = 9L,
                title = "저녁 모임",
                inviteCode = inviteCode,
                inviteUrl = inviteUrl
            )
        )
    )

    @Test
    fun `생성 요청은 토큰과 입력 내용을 보내고 초대 링크를 변환한다`() = runTest {
        val api = FakeApi().apply { onCreate = { success() } }

        val result = repo(api).create(appointment())

        assertEquals(
            AppointmentCreateResult.Success(
                CreatedAppointment(9L, "abc", "https://example.com/invite/abc")
            ),
            result
        )
        assertEquals("Bearer token", api.lastAuthorization)
        assertEquals(
            AppointmentCreateRequestDto(
                title = "저녁 모임",
                dateSelectionType = "RANGE",
                candidateDates = listOf("2026-12-05", "2026-12-06"),
                candidateLocations = listOf(
                    CreateCandidateLocationDto("강남역", "서울특별시 강남구", "2호선, 신분당선", 37.49, 127.02),
                    CreateCandidateLocationDto("잠실역", null, null, 37.51, 127.10)
                ),
                responseDeadline = "2026-12-03T23:59:59",
                previousMembers = null
            ),
            api.lastRequest
        )
    }

    @Test
    fun `이전 약속 멤버를 함께 보낸다`() = runTest {
        val api = FakeApi().apply { onCreate = { success() } }

        repo(api).create(appointment(PreviousMembers(4L, listOf(2L, 3L))))

        assertEquals(CreatePreviousMembersDto(4L, listOf(2L, 3L)), api.lastRequest?.previousMembers)
    }

    @Test
    fun `토큰이 없으면 서버를 부르지 않고 로그인이 필요하다고 알린다`() = runTest {
        val noToken = FakeApi()
        val blankToken = FakeApi()

        assertEquals(AppointmentCreateResult.LoginRequired, repo(noToken, token = null).create(appointment()))
        assertEquals(AppointmentCreateResult.LoginRequired, repo(blankToken, token = " ").create(appointment()))
        assertEquals(0, noToken.calls)
        assertEquals(0, blankToken.calls)
    }

    @Test
    fun `응답에 데이터가 없으면 다시 시도해도 소용없는 실패로 본다`() = runTest {
        val api = FakeApi().apply { onCreate = { Response.success(ApiResponse(data = null)) } }

        assertEquals(AppointmentCreateResult.Failure(retryable = false), repo(api).create(appointment()))
    }

    @Test
    fun `응답의 초대 링크가 비어 있어도 성공 값은 그대로 전달한다`() = runTest {
        val api = FakeApi().apply { onCreate = { success(inviteUrl = null, inviteCode = null) } }

        val result = repo(api).create(appointment())

        assertEquals(AppointmentCreateResult.Success(CreatedAppointment(9L, null, null)), result)
    }

    @Test
    fun `서버 오류 코드를 결과로 바꾼다`() = runTest {
        val cases = mapOf(
            "UNAUTHORIZED" to AppointmentCreateResult.LoginRequired,
            "EXPIRED_ACCESS_TOKEN" to AppointmentCreateResult.LoginRequired,
            "INVALID_ACCESS_TOKEN" to AppointmentCreateResult.LoginRequired,
            "DUPLICATE_CANDIDATE_DATE" to AppointmentCreateResult.Rejected(AppointmentCreateRejectReason.INVALID_DATES),
            "PAST_CANDIDATE_DATE" to AppointmentCreateResult.Rejected(AppointmentCreateRejectReason.INVALID_DATES),
            "INVALID_CANDIDATE_DATES" to AppointmentCreateResult.Rejected(AppointmentCreateRejectReason.INVALID_DATES),
            "INVALID_RESPONSE_DEADLINE" to
                AppointmentCreateResult.Rejected(AppointmentCreateRejectReason.INVALID_DEADLINE),
            "CANDIDATE_DATE_BEFORE_RESPONSE_DEADLINE" to
                AppointmentCreateResult.Rejected(AppointmentCreateRejectReason.INVALID_DEADLINE),
            "INVALID_CANDIDATE_LOCATION_COUNT" to
                AppointmentCreateResult.Rejected(AppointmentCreateRejectReason.INVALID_LOCATIONS),
            "INVALID_PREVIOUS_MEMBER" to
                AppointmentCreateResult.Rejected(AppointmentCreateRejectReason.INVALID_PREVIOUS_MEMBERS),
            "NOT_APPOINTMENT_PARTICIPANT" to
                AppointmentCreateResult.Rejected(AppointmentCreateRejectReason.INVALID_PREVIOUS_MEMBERS),
            "INVALID_INPUT_VALUE" to AppointmentCreateResult.Rejected(AppointmentCreateRejectReason.INVALID_INPUT)
        )

        for ((code, expected) in cases) {
            val api = FakeApi().apply {
                onCreate = { errorBody(400, """{"code":"$code","message":"x"}""") }
            }
            assertEquals(code, expected, repo(api).create(appointment()))
        }
    }

    @Test
    fun `알 수 없는 오류는 상태 코드로 판단한다`() = runTest {
        assertEquals(AppointmentCreateResult.LoginRequired, mapAppointmentCreateError(401, null))
        assertEquals(
            AppointmentCreateResult.Rejected(AppointmentCreateRejectReason.INVALID_INPUT),
            mapAppointmentCreateError(400, null)
        )
        assertEquals(AppointmentCreateResult.Failure(retryable = true), mapAppointmentCreateError(500, null))
        assertEquals(AppointmentCreateResult.Failure(retryable = true), mapAppointmentCreateError(503, "X"))
        assertEquals(AppointmentCreateResult.Failure(retryable = false), mapAppointmentCreateError(403, null))
        assertEquals(AppointmentCreateResult.Failure(retryable = false), mapAppointmentCreateError(404, "X"))
    }

    @Test
    fun `본문이 JSON이 아닌 오류 응답도 상태 코드로 처리한다`() = runTest {
        val api = FakeApi().apply { onCreate = { errorBody(500, "<html>error</html>") } }

        assertEquals(AppointmentCreateResult.Failure(retryable = true), repo(api).create(appointment()))
    }

    @Test
    fun `네트워크 오류는 다시 시도할 수 있는 실패다`() = runTest {
        val api = FakeApi().apply { onCreate = { throw IOException("offline") } }

        assertEquals(AppointmentCreateResult.Failure(retryable = true), repo(api).create(appointment()))
    }

    @Test
    fun `예상하지 못한 예외는 다시 시도해도 소용없는 실패다`() = runTest {
        val api = FakeApi().apply { onCreate = { throw IllegalStateException("boom") } }

        assertEquals(AppointmentCreateResult.Failure(retryable = false), repo(api).create(appointment()))
    }

    @Test
    fun `취소 예외는 삼키지 않고 다시 던진다`() = runTest {
        val api = FakeApi().apply { onCreate = { throw CancellationException("cancel") } }

        var cancelled = false
        try {
            repo(api).create(appointment())
        } catch (e: CancellationException) {
            cancelled = true
        }

        assertTrue(cancelled)
        assertEquals(1, api.calls)
    }
}
