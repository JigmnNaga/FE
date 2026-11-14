package com.jigmnnaga.data.repository

import com.jigmnnaga.core.datastore.TokenManager
import com.jigmnnaga.data.remote.api.AppointmentManageApi
import com.jigmnnaga.data.remote.dto.ApiResponse
import com.jigmnnaga.data.remote.dto.AppointmentLeaveRequestDto
import com.jigmnnaga.data.remote.dto.AppointmentTitleUpdateRequestDto
import com.jigmnnaga.data.remote.dto.CandidateDatesUpdateRequestDto
import com.jigmnnaga.data.remote.dto.CandidateLocationsUpdateRequestDto
import com.jigmnnaga.data.remote.dto.CreateCandidateLocationDto
import com.jigmnnaga.data.remote.dto.HostDelegateRequestDto
import com.jigmnnaga.data.remote.dto.NotificationSettingRequestDto
import com.jigmnnaga.data.remote.dto.ReminderResultDto
import com.jigmnnaga.data.remote.dto.ReminderSendResponseDto
import com.jigmnnaga.data.remote.dto.ResponseDeadlineUpdateRequestDto
import com.jigmnnaga.domain.model.AppointmentManageRejectReason
import com.jigmnnaga.domain.model.AppointmentManageResult
import com.jigmnnaga.domain.model.CandidateLocationChange
import com.jigmnnaga.domain.model.NewAppointmentDateType
import com.jigmnnaga.domain.model.NewCandidateLocation
import com.jigmnnaga.domain.model.ReminderCount
import com.jigmnnaga.domain.model.ReminderSendSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class AppointmentManageRepositoryImplTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private class FakeApi : AppointmentManageApi {
        var unitResponse: suspend () -> Response<Unit> = { Response.success(Unit) }
        var reminderResponse: suspend () -> Response<ApiResponse<ReminderSendResponseDto>> =
            { error("not set") }
        var calls = 0
        var lastAuthorization: String? = null
        var lastAppointmentId: Long? = null
        var lastTargetMemberId: Long? = null
        var lastTitle: AppointmentTitleUpdateRequestDto? = null
        var lastDates: CandidateDatesUpdateRequestDto? = null
        var lastLocations: CandidateLocationsUpdateRequestDto? = null
        var lastDeadline: ResponseDeadlineUpdateRequestDto? = null
        var lastHost: HostDelegateRequestDto? = null
        var lastAction: String? = null
        var lastLeave: AppointmentLeaveRequestDto? = null
        var lastNotification: NotificationSettingRequestDto? = null

        private suspend fun record(action: String, auth: String?, id: Long): Response<Unit> {
            calls++; lastAction = action; lastAuthorization = auth; lastAppointmentId = id
            return unitResponse()
        }

        override suspend fun updateTitle(
            authorization: String?,
            appointmentId: Long,
            request: AppointmentTitleUpdateRequestDto
        ): Response<Unit> {
            lastTitle = request
            return record("title", authorization, appointmentId)
        }

        override suspend fun updateCandidateDates(
            authorization: String?,
            appointmentId: Long,
            request: CandidateDatesUpdateRequestDto
        ): Response<Unit> {
            lastDates = request
            return record("dates", authorization, appointmentId)
        }

        override suspend fun updateCandidateLocations(
            authorization: String?,
            appointmentId: Long,
            request: CandidateLocationsUpdateRequestDto
        ): Response<Unit> {
            lastLocations = request
            return record("locations", authorization, appointmentId)
        }

        override suspend fun updateResponseDeadline(
            authorization: String?,
            appointmentId: Long,
            request: ResponseDeadlineUpdateRequestDto
        ): Response<Unit> {
            lastDeadline = request
            return record("deadline", authorization, appointmentId)
        }

        override suspend fun cancel(authorization: String?, appointmentId: Long): Response<Unit> =
            record("cancel", authorization, appointmentId)

        override suspend fun delete(authorization: String?, appointmentId: Long): Response<Unit> =
            record("delete", authorization, appointmentId)

        override suspend fun delegateHost(
            authorization: String?,
            appointmentId: Long,
            request: HostDelegateRequestDto
        ): Response<Unit> {
            lastHost = request
            return record("host", authorization, appointmentId)
        }

        override suspend fun kickParticipant(
            authorization: String?,
            appointmentId: Long,
            targetMemberId: Long
        ): Response<Unit> {
            lastTargetMemberId = targetMemberId
            return record("kick", authorization, appointmentId)
        }

        override suspend fun leave(
            authorization: String?,
            appointmentId: Long,
            request: AppointmentLeaveRequestDto
        ): Response<Unit> {
            lastLeave = request
            return record("leave", authorization, appointmentId)
        }

        override suspend fun updateNotificationSetting(
            authorization: String?,
            appointmentId: Long,
            request: NotificationSettingRequestDto
        ): Response<Unit> {
            lastNotification = request
            return record("notification", authorization, appointmentId)
        }

        override suspend fun sendReminders(
            authorization: String?,
            appointmentId: Long
        ): Response<ApiResponse<ReminderSendResponseDto>> {
            calls++; lastAction = "reminders"; lastAuthorization = authorization; lastAppointmentId = appointmentId
            return reminderResponse()
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
        AppointmentManageRepositoryImpl(api, FakeTokenManager(token), json)

    private fun <T> errorBody(code: Int, body: String): Response<T> =
        Response.error(code, body.toResponseBody("application/json".toMediaType()))

    private val success = AppointmentManageResult.Success(Unit)

    @Test
    fun `제목 수정은 토큰과 약속 ID와 제목을 보낸다`() = runTest {
        val api = FakeApi()

        val result = repo(api).updateTitle(7L, "새 이름")

        assertEquals(success, result)
        assertEquals("Bearer token", api.lastAuthorization)
        assertEquals(7L, api.lastAppointmentId)
        assertEquals(AppointmentTitleUpdateRequestDto("새 이름"), api.lastTitle)
    }

    @Test
    fun `후보 날짜 수정은 선택 방식과 날짜 목록을 보낸다`() = runTest {
        val api = FakeApi()

        val result = repo(api).updateCandidateDates(
            7L,
            NewAppointmentDateType.RANGE,
            listOf("2026-11-20", "2026-11-21")
        )

        assertEquals(success, result)
        assertEquals(
            CandidateDatesUpdateRequestDto("RANGE", listOf("2026-11-20", "2026-11-21")),
            api.lastDates
        )
    }

    @Test
    fun `후보 장소 수정은 유지할 ID와 새 장소를 보낸다`() = runTest {
        val api = FakeApi()
        val change = CandidateLocationChange(
            keepLocationIds = listOf(1L, 2L),
            newLocations = listOf(NewCandidateLocation("잠실역", "서울 송파구", "2호선", 37.5133, 127.1001))
        )

        val result = repo(api).updateCandidateLocations(7L, change)

        assertEquals(success, result)
        assertEquals(
            CandidateLocationsUpdateRequestDto(
                keepLocationIds = listOf(1L, 2L),
                newLocations = listOf(CreateCandidateLocationDto("잠실역", "서울 송파구", "2호선", 37.5133, 127.1001))
            ),
            api.lastLocations
        )
    }

    @Test
    fun `응답 기한 수정은 기한 문자열을 그대로 보낸다`() = runTest {
        val api = FakeApi()

        val result = repo(api).updateResponseDeadline(7L, "2026-11-19T23:59:59")

        assertEquals(success, result)
        assertEquals(ResponseDeadlineUpdateRequestDto("2026-11-19T23:59:59"), api.lastDeadline)
    }

    @Test
    fun `취소와 삭제와 위임과 내보내기는 각각 알맞은 호출을 한다`() = runTest {
        val api = FakeApi()
        val repo = repo(api)

        assertEquals(success, repo.cancel(7L))
        assertEquals("cancel", api.lastAction)

        assertEquals(success, repo.delete(7L))
        assertEquals("delete", api.lastAction)

        assertEquals(success, repo.delegateHost(7L, 22L))
        assertEquals("host", api.lastAction)
        assertEquals(HostDelegateRequestDto(22L), api.lastHost)

        assertEquals(success, repo.kickParticipant(7L, 33L))
        assertEquals("kick", api.lastAction)
        assertEquals(33L, api.lastTargetMemberId)

        assertEquals(4, api.calls)
        assertEquals(7L, api.lastAppointmentId)
    }

    @Test
    fun `미응답자 알림은 유형별 발송 인원을 변환한다`() = runTest {
        val api = FakeApi().apply {
            reminderResponse = {
                Response.success(
                    ApiResponse(
                        data = ReminderSendResponseDto(
                            sentCount = 3,
                            reminders = listOf(
                                ReminderResultDto("SCHEDULE_INPUT_REQUESTED", 2),
                                ReminderResultDto("ATTENDANCE_REQUESTED", 1)
                            )
                        )
                    )
                )
            }
        }

        val result = repo(api).sendReminders(7L)

        assertEquals(
            AppointmentManageResult.Success(
                ReminderSendSummary(
                    3,
                    listOf(ReminderCount("SCHEDULE_INPUT_REQUESTED", 2), ReminderCount("ATTENDANCE_REQUESTED", 1))
                )
            ),
            result
        )
    }

    @Test
    fun `참여자가 나갈 때는 새 방장 없이, 방장은 새 방장과 함께 보낸다`() = runTest {
        val api = FakeApi()

        assertEquals(success, repo(api).leave(7L))
        assertEquals("leave", api.lastAction)
        assertEquals(AppointmentLeaveRequestDto(null), api.lastLeave)

        assertEquals(success, repo(api).leave(7L, newHostMemberId = 3L))
        assertEquals(AppointmentLeaveRequestDto(3L), api.lastLeave)
        assertEquals("Bearer token", api.lastAuthorization)
    }

    @Test
    fun `알림 설정은 켜짐 여부를 보낸다`() = runTest {
        val api = FakeApi()

        assertEquals(success, repo(api).updateNotificationSetting(7L, false))

        assertEquals("notification", api.lastAction)
        assertEquals(NotificationSettingRequestDto(false), api.lastNotification)
    }

    @Test
    fun `나가기 거절 코드는 이유별로 나눈다`() = runTest {
        val api = FakeApi().apply {
            unitResponse = { errorBody(400, """{"code":"NEW_HOST_REQUIRED","message":"새 방장"}""") }
        }
        assertEquals(
            AppointmentManageResult.Rejected(AppointmentManageRejectReason.NEW_HOST_REQUIRED),
            repo(api).leave(7L)
        )

        api.unitResponse = { errorBody(400, """{"code":"LAST_MEMBER_CANNOT_LEAVE","message":"혼자"}""") }
        assertEquals(
            AppointmentManageResult.Rejected(AppointmentManageRejectReason.LAST_MEMBER_CANNOT_LEAVE),
            repo(api).leave(7L)
        )
    }

    @Test
    fun `미응답자 알림 응답에 데이터가 없으면 재시도 불가 실패다`() = runTest {
        val api = FakeApi().apply { reminderResponse = { Response.success(ApiResponse()) } }

        assertEquals(AppointmentManageResult.Failure(retryable = false), repo(api).sendReminders(7L))
    }

    @Test
    fun `토큰이 없으면 서버를 부르지 않고 로그인이 필요하다고 알린다`() = runTest {
        val api = FakeApi()
        val repo = repo(api, token = null)

        assertEquals(AppointmentManageResult.LoginRequired, repo.cancel(7L))
        assertEquals(AppointmentManageResult.LoginRequired, repo.sendReminders(7L))
        assertEquals(AppointmentManageResult.LoginRequired, repo(api, token = " ").updateTitle(7L, "x"))
        assertEquals(0, api.calls)
    }

    @Test
    fun `방장이 아니면 방장이 아니라고 알린다`() = runTest {
        val api = FakeApi().apply {
            unitResponse = { errorBody(403, """{"code":"NOT_APPOINTMENT_HOST","message":"방장만"}""") }
        }

        assertEquals(AppointmentManageResult.NotHost, repo(api).delete(7L))
    }

    @Test
    fun `서버 오류는 재시도 가능 실패이고 네트워크 오류도 그렇다`() = runTest {
        val api = FakeApi().apply { unitResponse = { errorBody(500, "") } }
        assertEquals(AppointmentManageResult.Failure(retryable = true), repo(api).cancel(7L))

        api.unitResponse = { throw IOException("offline") }
        assertEquals(AppointmentManageResult.Failure(retryable = true), repo(api).cancel(7L))

        api.unitResponse = { throw IllegalStateException("boom") }
        assertEquals(AppointmentManageResult.Failure(retryable = false), repo(api).cancel(7L))
    }

    @Test
    fun `코루틴 취소는 삼키지 않고 다시 던진다`() = runTest {
        val api = FakeApi().apply { unitResponse = { throw CancellationException("cancelled") } }

        assertThrows(CancellationException::class.java) {
            kotlinx.coroutines.runBlocking { repo(api).cancel(7L) }
        }
    }

    @Test
    fun `오류 코드를 거절 이유로 나눈다`() {
        fun reject(r: AppointmentManageRejectReason) = AppointmentManageResult.Rejected(r)

        assertEquals(AppointmentManageResult.LoginRequired, mapAppointmentManageError(401, "EXPIRED_ACCESS_TOKEN"))
        assertEquals(AppointmentManageResult.LoginRequired, mapAppointmentManageError(401, null))
        assertEquals(AppointmentManageResult.NotHost, mapAppointmentManageError(403, "NOT_APPOINTMENT_PARTICIPANT"))
        assertEquals(AppointmentManageResult.NotHost, mapAppointmentManageError(404, null))
        assertEquals(
            reject(AppointmentManageRejectReason.APPOINTMENT_CANCELED),
            mapAppointmentManageError(400, "APPOINTMENT_CANCELED")
        )
        assertEquals(
            reject(AppointmentManageRejectReason.INVALID_DATES),
            mapAppointmentManageError(400, "PAST_CANDIDATE_DATE")
        )
        assertEquals(
            reject(AppointmentManageRejectReason.INVALID_DATES),
            mapAppointmentManageError(400, "DUPLICATE_CANDIDATE_DATE")
        )
        assertEquals(
            reject(AppointmentManageRejectReason.DATES_BEFORE_DEADLINE),
            mapAppointmentManageError(400, "CANDIDATE_DATE_BEFORE_RESPONSE_DEADLINE")
        )
        assertEquals(
            reject(AppointmentManageRejectReason.INVALID_DEADLINE),
            mapAppointmentManageError(400, "INVALID_RESPONSE_DEADLINE")
        )
        assertEquals(
            reject(AppointmentManageRejectReason.INVALID_LOCATIONS),
            mapAppointmentManageError(400, "INVALID_CANDIDATE_LOCATION_COUNT")
        )
        assertEquals(
            reject(AppointmentManageRejectReason.INVALID_LOCATIONS),
            mapAppointmentManageError(400, "INVALID_CANDIDATE_LOCATION")
        )
        assertEquals(
            reject(AppointmentManageRejectReason.INVALID_MEMBER),
            mapAppointmentManageError(404, "MEMBER_NOT_FOUND")
        )
        assertEquals(
            reject(AppointmentManageRejectReason.INVALID_INPUT),
            mapAppointmentManageError(400, "INVALID_INPUT_VALUE")
        )
        assertEquals(AppointmentManageResult.Failure(retryable = false), mapAppointmentManageError(409, null))
    }
}
