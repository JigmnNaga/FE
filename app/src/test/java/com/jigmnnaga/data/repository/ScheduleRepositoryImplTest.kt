package com.jigmnnaga.data.repository

import com.jigmnnaga.core.datastore.TokenManager
import com.jigmnnaga.data.remote.api.ScheduleApi
import com.jigmnnaga.data.remote.dto.ApiResponse
import com.jigmnnaga.data.remote.dto.AvailabilityDto
import com.jigmnnaga.data.remote.dto.LocationVoteDto
import com.jigmnnaga.data.remote.dto.MemberScheduleDto
import com.jigmnnaga.data.remote.dto.MyScheduleDto
import com.jigmnnaga.data.remote.dto.ScheduleStatusDto
import com.jigmnnaga.data.remote.dto.ScheduleSubmitRequestDto
import com.jigmnnaga.data.remote.dto.ScheduleVoterDto
import com.jigmnnaga.domain.model.ScheduleAvailability
import com.jigmnnaga.domain.model.ScheduleRejectReason
import com.jigmnnaga.domain.model.ScheduleResult
import com.jigmnnaga.domain.model.ScheduleSubmission
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class ScheduleRepositoryImplTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private class FakeApi : ScheduleApi {
        var onMy: suspend () -> Response<ApiResponse<MyScheduleDto>> = { error("not set") }
        var onStatus: suspend () -> Response<ApiResponse<ScheduleStatusDto>> = { error("not set") }
        var onSubmit: suspend () -> Response<Unit> = { error("not set") }
        var calls = 0
        var lastAuthorization: String? = null
        var lastAppointmentId: Long? = null
        var lastRequest: ScheduleSubmitRequestDto? = null

        override suspend fun getMySchedule(authorization: String?, appointmentId: Long):
            Response<ApiResponse<MyScheduleDto>> {
            calls++; lastAuthorization = authorization; lastAppointmentId = appointmentId
            return onMy()
        }

        override suspend fun getScheduleStatus(authorization: String?, appointmentId: Long):
            Response<ApiResponse<ScheduleStatusDto>> {
            calls++; lastAuthorization = authorization; lastAppointmentId = appointmentId
            return onStatus()
        }

        override suspend fun submitSchedule(
            authorization: String?,
            appointmentId: Long,
            request: ScheduleSubmitRequestDto
        ): Response<Unit> {
            calls++; lastAuthorization = authorization; lastAppointmentId = appointmentId
            lastRequest = request
            return onSubmit()
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
        ScheduleRepositoryImpl(api, FakeTokenManager(token), json)

    private fun <T> errorBody(code: Int, body: String): Response<T> =
        Response.error(code, body.toResponseBody("application/json".toMediaType()))

    private val submission = ScheduleSubmission(
        availabilities = listOf(
            ScheduleAvailability("2026-11-20", "15:00", "18:00"),
            ScheduleAvailability("2026-11-21", "09:00", "12:00")
        ),
        preferredLocationId = 3L
    )

    @Test
    fun `내 일정 조회는 토큰과 약속 ID를 보내고 응답을 변환한다`() = runTest {
        val api = FakeApi().apply {
            onMy = {
                Response.success(
                    ApiResponse(
                        data = MyScheduleDto(
                            submitted = true,
                            availabilities = listOf(AvailabilityDto("2026-11-20", "15:00", "18:00")),
                            preferredLocationId = 3L
                        )
                    )
                )
            }
        }
        val mine = (repo(api).getMySchedule(7L) as ScheduleResult.Success).value

        assertEquals("Bearer token", api.lastAuthorization)
        assertEquals(7L, api.lastAppointmentId)
        assertTrue(mine.submitted)
        assertEquals(listOf(ScheduleAvailability("2026-11-20", "15:00", "18:00")), mine.availabilities)
        assertEquals(3L, mine.preferredLocationId)
    }

    @Test
    fun `아직 제출하지 않았으면 빈 목록과 null 장소로 받는다`() = runTest {
        val api = FakeApi().apply { onMy = { Response.success(ApiResponse(data = MyScheduleDto())) } }
        val mine = (repo(api).getMySchedule(7L) as ScheduleResult.Success).value

        assertEquals(false, mine.submitted)
        assertTrue(mine.availabilities.isEmpty())
        assertNull(mine.preferredLocationId)
    }

    @Test
    fun `응답 현황 조회는 멤버별 일정과 장소별 선택 현황을 변환한다`() = runTest {
        val api = FakeApi().apply {
            onStatus = {
                Response.success(
                    ApiResponse(
                        data = ScheduleStatusDto(
                            totalCount = 3,
                            submittedCount = 1,
                            members = listOf(
                                MemberScheduleDto(
                                    memberId = 1L, nickname = "나", submitted = true,
                                    availabilities = listOf(AvailabilityDto("2026-11-20", "15:00", "18:00")),
                                    preferredLocationId = 3L
                                ),
                                MemberScheduleDto(memberId = 2L, nickname = "태양")
                            ),
                            locationVotes = listOf(
                                LocationVoteDto(
                                    locationId = 3L, placeName = "강남역", lineNames = "2호선",
                                    voteCount = 1, topVoted = true,
                                    voters = listOf(ScheduleVoterDto(1L, "나"))
                                ),
                                LocationVoteDto(locationId = 4L)
                            )
                        )
                    )
                )
            }
        }
        val status = (repo(api).getScheduleStatus(7L) as ScheduleResult.Success).value

        assertEquals(3, status.totalCount)
        assertEquals(1, status.submittedCount)
        assertEquals(listOf(1L, 2L), status.members.map { it.memberId })
        assertTrue(status.members[0].submitted)
        assertEquals("15:00", status.members[0].availabilities.single().startTime)
        assertEquals(false, status.members[1].submitted)
        assertTrue(status.members[1].availabilities.isEmpty())
        assertEquals("강남역", status.locationVotes[0].placeName)
        assertEquals(1, status.locationVotes[0].voteCount)
        assertTrue(status.locationVotes[0].topVoted)
        assertEquals("나", status.locationVotes[0].voters.single().nickname)
        assertEquals("", status.locationVotes[1].placeName)
    }

    @Test
    fun `일정 제출은 날짜별 시간과 희망 장소를 요청 본문에 담아 보낸다`() = runTest {
        val api = FakeApi().apply { onSubmit = { Response.success(Unit) } }
        val result = repo(api).submitSchedule(7L, submission)

        assertEquals(ScheduleResult.Success(Unit), result)
        assertEquals("Bearer token", api.lastAuthorization)
        assertEquals(7L, api.lastAppointmentId)
        val request = api.lastRequest!!
        assertEquals(3L, request.preferredLocationId)
        assertEquals(
            listOf(
                AvailabilityDto("2026-11-20", "15:00", "18:00"),
                AvailabilityDto("2026-11-21", "09:00", "12:00")
            ),
            request.availabilities
        )
    }

    @Test
    fun `응답 본문이 비어 있으면 재시도할 수 없는 실패이다`() = runTest {
        val api = FakeApi().apply {
            onMy = { Response.success(ApiResponse<MyScheduleDto>()) }
            onStatus = { Response.success(ApiResponse<ScheduleStatusDto>()) }
        }
        assertEquals(ScheduleResult.Failure(false), repo(api).getMySchedule(7L))
        assertEquals(ScheduleResult.Failure(false), repo(api).getScheduleStatus(7L))
    }

    @Test
    fun `토큰이 없거나 비어 있으면 서버를 부르지 않고 로그인이 필요하다`() = runTest {
        for (token in listOf(null, "", "  ")) {
            val api = FakeApi()
            val repository = repo(api, token)
            assertEquals(ScheduleResult.LoginRequired, repository.getMySchedule(7L))
            assertEquals(ScheduleResult.LoginRequired, repository.getScheduleStatus(7L))
            assertEquals(ScheduleResult.LoginRequired, repository.submitSchedule(7L, submission))
            assertEquals(0, api.calls)
        }
    }

    @Test
    fun `토큰 오류 코드와 401은 로그인이 필요하다`() = runTest {
        for (code in listOf("UNAUTHORIZED", "EXPIRED_ACCESS_TOKEN", "INVALID_ACCESS_TOKEN")) {
            val api = FakeApi().apply { onSubmit = { errorBody(401, """{"code":"$code"}""") } }
            assertEquals(ScheduleResult.LoginRequired, repo(api).submitSchedule(7L, submission))
        }
        val api = FakeApi().apply { onMy = { errorBody(401, "not json") } }
        assertEquals(ScheduleResult.LoginRequired, repo(api).getMySchedule(7L))
    }

    @Test
    fun `참여자가 아니거나 약속이 없으면 참여자 아님이다`() = runTest {
        val forbidden = FakeApi().apply {
            onStatus = { errorBody(403, """{"code":"NOT_APPOINTMENT_PARTICIPANT"}""") }
        }
        assertEquals(ScheduleResult.NotParticipant, repo(forbidden).getScheduleStatus(7L))

        val notFound = FakeApi().apply { onMy = { errorBody(404, "") } }
        assertEquals(ScheduleResult.NotParticipant, repo(notFound).getMySchedule(7L))
    }

    @Test
    fun `제출이 거절된 이유를 오류 코드로 구분한다`() = runTest {
        val expected = mapOf(
            "APPOINTMENT_CANCELED" to ScheduleRejectReason.APPOINTMENT_CANCELED,
            "SCHEDULE_RESPONSE_DEADLINE_PASSED" to ScheduleRejectReason.DEADLINE_PASSED,
            "INVALID_AVAILABLE_DATE" to ScheduleRejectReason.INVALID_DATE,
            "DUPLICATE_AVAILABLE_DATE" to ScheduleRejectReason.DUPLICATE_DATE,
            "INVALID_AVAILABLE_TIME" to ScheduleRejectReason.INVALID_TIME,
            "INVALID_PREFERRED_LOCATION" to ScheduleRejectReason.INVALID_LOCATION
        )
        for ((code, reason) in expected) {
            val api = FakeApi().apply { onSubmit = { errorBody(400, """{"code":"$code"}""") } }
            assertEquals(ScheduleResult.Rejected(reason), repo(api).submitSchedule(7L, submission))
        }
    }

    @Test
    fun `서버 오류는 재시도 가능하고 모르는 400은 재시도할 수 없다`() = runTest {
        val server = FakeApi().apply { onSubmit = { errorBody(500, "") } }
        assertEquals(ScheduleResult.Failure(true), repo(server).submitSchedule(7L, submission))

        val client = FakeApi().apply { onSubmit = { errorBody(400, """{"code":"BAD_REQUEST"}""") } }
        assertEquals(ScheduleResult.Failure(false), repo(client).submitSchedule(7L, submission))
    }

    @Test
    fun `네트워크 오류와 예상하지 못한 예외는 실패로 바뀐다`() = runTest {
        val network = FakeApi().apply { onSubmit = { throw IOException("offline") } }
        assertEquals(ScheduleResult.Failure(true), repo(network).submitSchedule(7L, submission))

        val unexpected = FakeApi().apply { onMy = { throw IllegalStateException("boom") } }
        assertEquals(ScheduleResult.Failure(false), repo(unexpected).getMySchedule(7L))
    }
}
