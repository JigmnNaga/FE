package com.jigmnnaga.data.repository

import com.jigmnnaga.core.datastore.TokenManager
import com.jigmnnaga.data.remote.api.AppointmentDetailApi
import com.jigmnnaga.data.remote.dto.ApiResponse
import com.jigmnnaga.data.remote.dto.AppointmentDetailDto
import com.jigmnnaga.data.remote.dto.AppointmentDetailPlaceDto
import com.jigmnnaga.data.remote.dto.AppointmentLocationDto
import com.jigmnnaga.data.remote.dto.AppointmentMemberDto
import com.jigmnnaga.domain.model.AppointmentAttendanceStatus
import com.jigmnnaga.domain.model.AppointmentDateSelectionType
import com.jigmnnaga.domain.model.AppointmentDetailResult
import com.jigmnnaga.domain.model.AppointmentMemberRole
import com.jigmnnaga.domain.model.AppointmentTravelMode
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

class AppointmentDetailRepositoryImplTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private class FakeApi : AppointmentDetailApi {
        var onGet: suspend () -> Response<ApiResponse<AppointmentDetailDto>> = { error("not set") }
        var calls = 0
        var lastAuthorization: String? = null
        var lastAppointmentId: Long? = null

        override suspend fun getAppointmentDetail(authorization: String?, appointmentId: Long):
            Response<ApiResponse<AppointmentDetailDto>> {
            calls++; lastAuthorization = authorization; lastAppointmentId = appointmentId
            return onGet()
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
        AppointmentDetailRepositoryImpl(api, FakeTokenManager(token), json)

    private fun <T> errorBody(code: Int, body: String): Response<T> =
        Response.error(code, body.toResponseBody("application/json".toMediaType()))

    private fun dto(
        dateSelectionType: String? = "MULTIPLE",
        myRole: String? = "HOST",
        myAttendanceStatus: String? = "ATTENDING",
        myTravelMode: String? = "SUBWAY"
    ) = AppointmentDetailDto(
        appointmentId = 7L,
        title = "중간고사 카공 모임",
        dateSelectionType = dateSelectionType,
        candidateDates = listOf("2026-11-20", "2026-11-21"),
        candidateLocations = listOf(
            AppointmentLocationDto(
                locationId = 3L, placeName = "강남역", address = "서울 강남구",
                lineNames = "2호선", latitude = 37.49, longitude = 127.02
            )
        ),
        responseDeadline = "2026-11-18T23:59:00",
        inviteUrl = "https://example.com/invite/abc",
        confirmed = true,
        confirmedDate = "2026-11-20",
        confirmedTime = "15:00",
        confirmedLocation = AppointmentLocationDto(locationId = 3L, placeName = "강남역"),
        detailPlace = AppointmentDetailPlaceDto(placeName = "카공카페", address = "서울 강남구 1길"),
        myRole = myRole,
        myAttendanceStatus = myAttendanceStatus,
        myNotificationEnabled = true,
        myTravelMode = myTravelMode,
        joinedCount = 2,
        scheduleSubmittedCount = 1,
        members = listOf(
            AppointmentMemberDto(
                memberId = 1L, nickname = "나", role = "host", scheduleSubmitted = true,
                attendanceStatus = "attending", travelMode = "bus"
            ),
            AppointmentMemberDto(memberId = 2L, nickname = "태양")
        )
    )

    @Test
    fun `sends bearer token and appointment id and maps response`() = runTest {
        val api = FakeApi().apply { onGet = { Response.success(ApiResponse(data = dto())) } }
        val detail = (repo(api).getAppointmentDetail(7L) as AppointmentDetailResult.Success).value

        assertEquals("Bearer token", api.lastAuthorization)
        assertEquals(7L, api.lastAppointmentId)
        assertEquals("중간고사 카공 모임", detail.title)
        assertEquals(AppointmentDateSelectionType.MULTIPLE, detail.dateSelectionType)
        assertEquals(listOf("2026-11-20", "2026-11-21"), detail.candidateDates)
        assertEquals("강남역", detail.candidateLocations.single().placeName)
        assertEquals("2호선", detail.candidateLocations.single().lineNames)
        assertEquals("2026-11-20", detail.confirmedDate)
        assertEquals("15:00", detail.confirmedTime)
        assertEquals("카공카페", detail.detailPlace?.placeName)
        assertEquals(AppointmentMemberRole.HOST, detail.myRole)
        assertEquals(AppointmentAttendanceStatus.ATTENDING, detail.myAttendanceStatus)
        assertEquals(AppointmentTravelMode.SUBWAY, detail.myTravelMode)
        assertEquals(2, detail.joinedCount)
        assertEquals(1, detail.scheduleSubmittedCount)
    }

    @Test
    fun `members are mapped and missing member fields get safe defaults`() = runTest {
        val api = FakeApi().apply { onGet = { Response.success(ApiResponse(data = dto())) } }
        val members = (repo(api).getAppointmentDetail(7L) as AppointmentDetailResult.Success).value.members

        assertEquals(2, members.size)
        assertEquals(AppointmentMemberRole.HOST, members[0].role)
        assertEquals(AppointmentAttendanceStatus.ATTENDING, members[0].attendanceStatus)
        assertEquals(AppointmentTravelMode.BUS, members[0].travelMode)
        assertTrue(members[0].scheduleSubmitted)
        assertEquals(AppointmentMemberRole.OTHER, members[1].role)
        assertEquals(AppointmentAttendanceStatus.PENDING, members[1].attendanceStatus)
        assertNull(members[1].travelMode)
        assertEquals(false, members[1].scheduleSubmitted)
    }

    @Test
    fun `unknown or missing enum values fall back safely`() = runTest {
        val api = FakeApi().apply {
            onGet = {
                Response.success(
                    ApiResponse(
                        data = dto(
                            dateSelectionType = "SOMETHING_NEW",
                            myRole = null,
                            myAttendanceStatus = null,
                            myTravelMode = null
                        )
                    )
                )
            }
        }
        val detail = (repo(api).getAppointmentDetail(7L) as AppointmentDetailResult.Success).value

        assertEquals(AppointmentDateSelectionType.OTHER, detail.dateSelectionType)
        assertNull(detail.myRole)
        assertEquals(AppointmentAttendanceStatus.PENDING, detail.myAttendanceStatus)
        assertNull(detail.myTravelMode)
    }

    @Test
    fun `unconfirmed appointment keeps confirmed fields null`() = runTest {
        val api = FakeApi().apply {
            onGet = {
                Response.success(
                    ApiResponse(data = AppointmentDetailDto(appointmentId = 7L, title = "약속"))
                )
            }
        }
        val detail = (repo(api).getAppointmentDetail(7L) as AppointmentDetailResult.Success).value

        assertEquals(false, detail.confirmed)
        assertNull(detail.confirmedDate)
        assertNull(detail.confirmedTime)
        assertNull(detail.confirmedLocation)
        assertNull(detail.detailPlace)
        assertTrue(detail.candidateDates.isEmpty())
        assertTrue(detail.members.isEmpty())
    }

    @Test
    fun `empty body is non retryable failure`() = runTest {
        val api = FakeApi().apply { onGet = { Response.success(ApiResponse<AppointmentDetailDto>()) } }
        assertEquals(AppointmentDetailResult.Failure(false), repo(api).getAppointmentDetail(7L))
    }

    @Test
    fun `missing or blank token requires login without calling server`() = runTest {
        for (token in listOf(null, "", "  ")) {
            val api = FakeApi()
            assertEquals(AppointmentDetailResult.LoginRequired, repo(api, token).getAppointmentDetail(7L))
            assertEquals(0, api.calls)
        }
    }

    @Test
    fun `token error codes and 401 require login`() = runTest {
        for (code in listOf("UNAUTHORIZED", "EXPIRED_ACCESS_TOKEN", "INVALID_ACCESS_TOKEN")) {
            val api = FakeApi().apply {
                onGet = { errorBody(401, """{"code":"$code","message":"x"}""") }
            }
            assertEquals(AppointmentDetailResult.LoginRequired, repo(api).getAppointmentDetail(7L))
        }
        val api = FakeApi().apply { onGet = { errorBody(401, "not json") } }
        assertEquals(AppointmentDetailResult.LoginRequired, repo(api).getAppointmentDetail(7L))
    }

    @Test
    fun `not a participant and not found are mapped`() = runTest {
        val forbidden = FakeApi().apply {
            onGet = { errorBody(403, """{"code":"NOT_APPOINTMENT_PARTICIPANT"}""") }
        }
        assertEquals(AppointmentDetailResult.NotParticipant, repo(forbidden).getAppointmentDetail(7L))

        val notFound = FakeApi().apply { onGet = { errorBody(404, "") } }
        assertEquals(AppointmentDetailResult.NotParticipant, repo(notFound).getAppointmentDetail(7L))
    }

    @Test
    fun `server errors are retryable and other client errors are not`() = runTest {
        val server = FakeApi().apply { onGet = { errorBody(500, "") } }
        assertEquals(AppointmentDetailResult.Failure(true), repo(server).getAppointmentDetail(7L))

        val client = FakeApi().apply { onGet = { errorBody(400, """{"code":"BAD_REQUEST"}""") } }
        assertEquals(AppointmentDetailResult.Failure(false), repo(client).getAppointmentDetail(7L))
    }

    @Test
    fun `network and unexpected exceptions become failures`() = runTest {
        val network = FakeApi().apply { onGet = { throw IOException("offline") } }
        assertEquals(AppointmentDetailResult.Failure(true), repo(network).getAppointmentDetail(7L))

        val unexpected = FakeApi().apply { onGet = { throw IllegalStateException("boom") } }
        assertEquals(AppointmentDetailResult.Failure(false), repo(unexpected).getAppointmentDetail(7L))
    }
}
