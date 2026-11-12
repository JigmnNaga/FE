package com.jigmnnaga.data.repository

import com.jigmnnaga.core.datastore.TokenManager
import com.jigmnnaga.data.remote.api.InviteApi
import com.jigmnnaga.data.remote.dto.ApiResponse
import com.jigmnnaga.data.remote.dto.InviteAcceptDto
import com.jigmnnaga.data.remote.dto.InviteDetailDto
import com.jigmnnaga.domain.model.InviteOperationResult
import com.jigmnnaga.domain.model.InviteParticipantRole
import com.jigmnnaga.domain.model.InviteParticipationStatus
import com.jigmnnaga.domain.model.InviteUnavailableReason
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

class AppointmentInviteRepositoryImplTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private class FakeApi : InviteApi {
        var onGet: suspend () -> Response<ApiResponse<InviteDetailDto>> = { error("not set") }
        var onAccept: suspend () -> Response<ApiResponse<InviteAcceptDto>> = { error("not set") }
        var onDecline: suspend () -> Response<Unit> = { error("not set") }
        var calls = 0
        var lastAuthorization: String? = null
        var lastInviteCode: String? = null

        override suspend fun getInvite(authorization: String?, inviteCode: String):
            Response<ApiResponse<InviteDetailDto>> {
            calls++; lastAuthorization = authorization; lastInviteCode = inviteCode
            return onGet()
        }

        override suspend fun acceptInvite(authorization: String?, inviteCode: String):
            Response<ApiResponse<InviteAcceptDto>> {
            calls++; lastAuthorization = authorization; lastInviteCode = inviteCode
            return onAccept()
        }

        override suspend fun declineInvite(authorization: String?, inviteCode: String): Response<Unit> {
            calls++; lastAuthorization = authorization; lastInviteCode = inviteCode
            return onDecline()
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
        AppointmentInviteRepositoryImpl(api, FakeTokenManager(token), json)

    private fun errorBody(code: Int, body: String) =
        body.toResponseBody("application/json".toMediaType()).let { Response.error<Nothing>(code, it) }

    private fun detail(myStatus: String? = "PENDING", myRole: String? = "MEMBER") = InviteDetailDto(
        appointmentId = 7L,
        title = "저녁 약속",
        hostNickname = "호스트",
        myStatus = myStatus,
        myRole = myRole
    )

    @Test
    fun `getInvite sends bearer token and maps response`() = runTest {
        val api = FakeApi().apply {
            onGet = { Response.success(ApiResponse(data = detail(myStatus = "accepted", myRole = "host"))) }
        }
        val result = repo(api).getInvite("abc")

        assertEquals("Bearer token", api.lastAuthorization)
        assertEquals("abc", api.lastInviteCode)
        val invite = (result as InviteOperationResult.Success).value
        assertEquals(7L, invite.appointmentId)
        assertEquals(InviteParticipationStatus.ACCEPTED, invite.myStatus)
        assertEquals(InviteParticipantRole.HOST, invite.myRole)
    }

    @Test
    fun `missing myStatus becomes pending and missing myRole stays null`() = runTest {
        val api = FakeApi().apply {
            onGet = { Response.success(ApiResponse(data = detail(myStatus = null, myRole = null))) }
        }
        val invite = (repo(api).getInvite("abc") as InviteOperationResult.Success).value
        assertEquals(InviteParticipationStatus.PENDING, invite.myStatus)
        assertEquals(null, invite.myRole)
    }

    @Test
    fun `empty body is non retryable failure`() = runTest {
        val api = FakeApi().apply { onGet = { Response.success(ApiResponse<InviteDetailDto>()) } }
        assertEquals(InviteOperationResult.Failure(false), repo(api).getInvite("abc"))
    }

    @Test
    fun `missing or blank token requires login without calling server`() = runTest {
        for (token in listOf(null, "", "  ")) {
            val api = FakeApi()
            assertEquals(InviteOperationResult.LoginRequired, repo(api, token).acceptInvite("abc"))
            assertEquals(0, api.calls)
        }
    }

    @Test
    fun `accept returns appointment id`() = runTest {
        val api = FakeApi().apply {
            onAccept = { Response.success(ApiResponse(data = InviteAcceptDto(42L))) }
        }
        assertEquals(InviteOperationResult.Success(42L), repo(api).acceptInvite("abc"))
    }

    @Test
    fun `accept without id is failure`() = runTest {
        val api = FakeApi().apply { onAccept = { Response.success(ApiResponse<InviteAcceptDto>()) } }
        assertEquals(InviteOperationResult.Failure(false), repo(api).acceptInvite("abc"))
    }

    @Test
    fun `decline success`() = runTest {
        val api = FakeApi().apply { onDecline = { Response.success(Unit) } }
        assertEquals(InviteOperationResult.Success(Unit), repo(api).declineInvite("abc"))
    }

    @Test
    fun `backend error codes map to results`() = runTest {
        val cases = listOf(
            Triple(401, "UNAUTHORIZED", InviteOperationResult.LoginRequired),
            Triple(401, "EXPIRED_ACCESS_TOKEN", InviteOperationResult.LoginRequired),
            Triple(401, "INVALID_ACCESS_TOKEN", InviteOperationResult.LoginRequired),
            Triple(404, "INVITE_NOT_FOUND", InviteOperationResult.Unavailable(InviteUnavailableReason.NOT_FOUND)),
            Triple(400, "APPOINTMENT_CANCELED", InviteOperationResult.Unavailable(InviteUnavailableReason.CANCELED)),
            Triple(400, "INVITE_EXPIRED", InviteOperationResult.Expired),
            Triple(403, "KICKED_MEMBER_CANNOT_REJOIN", InviteOperationResult.Unavailable(InviteUnavailableReason.KICKED)),
            Triple(400, "HOST_CANNOT_RESPOND", InviteOperationResult.Unavailable(InviteUnavailableReason.HOST_CANNOT_RESPOND)),
            Triple(409, "ALREADY_RESPONDED", InviteOperationResult.AlreadyResponded)
        )
        for ((status, code, expected) in cases) {
            val api = FakeApi().apply {
                onAccept = {
                    Response.error(
                        status,
                        """{"success":false,"code":"$code","message":"m","data":null}"""
                            .toResponseBody("application/json".toMediaType())
                    )
                }
            }
            assertEquals(code, expected, repo(api).acceptInvite("abc"))
        }
    }

    @Test
    fun `lookup 404 maps to not found`() = runTest {
        val api = FakeApi().apply {
            onGet = {
                Response.error(
                    404,
                    """{"success":false,"code":"INVITE_NOT_FOUND","message":"m","data":null}"""
                        .toResponseBody("application/json".toMediaType())
                )
            }
        }
        assertEquals(
            InviteOperationResult.Unavailable(InviteUnavailableReason.NOT_FOUND),
            repo(api).getInvite("abc")
        )
    }

    @Test
    fun `unknown codes fall back to http status`() = runTest {
        val cases = listOf(
            Triple(401, "", InviteOperationResult.LoginRequired),
            Triple(503, "<html>down</html>", InviteOperationResult.Failure(true)),
            Triple(400, """{"code":"SOMETHING_NEW"}""", InviteOperationResult.Failure(false))
        )
        for ((status, body, expected) in cases) {
            val api = FakeApi().apply {
                onAccept = { Response.error(status, body.toResponseBody("application/json".toMediaType())) }
            }
            assertEquals("$status", expected, repo(api).acceptInvite("abc"))
        }
    }

    @Test
    fun `io exception is retryable`() = runTest {
        val api = FakeApi().apply { onGet = { throw IOException("offline") } }
        assertEquals(InviteOperationResult.Failure(true), repo(api).getInvite("abc"))
    }

    @Test
    fun `unexpected exception is not retryable`() = runTest {
        val api = FakeApi().apply { onGet = { throw IllegalStateException("boom") } }
        val result = repo(api).getInvite("abc")
        assertTrue(result is InviteOperationResult.Failure)
        assertEquals(InviteOperationResult.Failure(false), result)
    }
}
