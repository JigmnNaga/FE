package com.jigmnnaga.data.repository

import com.jigmnnaga.core.datastore.TokenManager
import com.jigmnnaga.data.remote.api.ChatApi
import com.jigmnnaga.data.remote.dto.ApiResponse
import com.jigmnnaga.data.remote.dto.ChatMessageDto
import com.jigmnnaga.data.remote.dto.ChatPageDto
import com.jigmnnaga.data.remote.dto.ChatReadRequestDto
import com.jigmnnaga.domain.model.ChatMessageType
import com.jigmnnaga.domain.model.ChatResult
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
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class ChatRepositoryImplTest {

    private val json = Json { ignoreUnknownKeys = true }

    private class FakeApi : ChatApi {
        var pageResponse: suspend () -> Response<ApiResponse<ChatPageDto>> = { error("not set") }
        var readResponse: suspend () -> Response<Unit> = { Response.success(Unit) }
        var calls = 0
        var lastAuthorization: String? = null
        var lastAppointmentId: Long? = null
        var lastCursor: Long? = null
        var lastSize: Int? = null
        var lastRead: ChatReadRequestDto? = null

        override suspend fun getMessages(
            authorization: String?,
            appointmentId: Long,
            cursor: Long?,
            size: Int
        ): Response<ApiResponse<ChatPageDto>> {
            calls++
            lastAuthorization = authorization
            lastAppointmentId = appointmentId
            lastCursor = cursor
            lastSize = size
            return pageResponse()
        }

        override suspend fun markAsRead(
            authorization: String?,
            appointmentId: Long,
            request: ChatReadRequestDto
        ): Response<Unit> {
            calls++
            lastAuthorization = authorization
            lastAppointmentId = appointmentId
            lastRead = request
            return readResponse()
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

    private fun repo(api: FakeApi, token: String? = "token") = ChatRepositoryImpl(api, FakeTokenManager(token), json)

    private fun <T> errorBody(code: Int, body: String): Response<T> =
        Response.error(code, body.toResponseBody("application/json".toMediaType()))

    private fun page(vararg ids: Long) = ApiResponse(
        data = ChatPageDto(
            messages = ids.map { ChatMessageDto(messageId = it, senderId = 1L, content = "m$it", unreadCount = 2) },
            myLastReadMessageId = 5L,
            nextCursor = 3L,
            hasNext = true
        )
    )

    @Test
    fun getMessages_sendsTokenAndCursorAndMapsPageSortedOldestFirst() = runTest {
        val api = FakeApi().apply { pageResponse = { Response.success(page(9, 7, 8)) } }

        val result = repo(api).getMessages(appointmentId = 4L, cursor = 12L, size = 20)

        assertEquals("Bearer token", api.lastAuthorization)
        assertEquals(4L, api.lastAppointmentId)
        assertEquals(12L, api.lastCursor)
        assertEquals(20, api.lastSize)
        val page = (result as ChatResult.Success).value
        assertEquals(listOf(7L, 8L, 9L), page.messages.map { it.id })
        assertEquals(5L, page.myLastReadMessageId)
        assertEquals(3L, page.nextCursor)
        assertTrue(page.hasNext)
        assertEquals(ChatMessageType.TEXT, page.messages.first().type)
    }

    @Test
    fun getMessages_mapsSystemMessageWithoutSender() = runTest {
        val api = FakeApi().apply {
            pageResponse = {
                Response.success(
                    ApiResponse(
                        data = ChatPageDto(
                            messages = listOf(ChatMessageDto(messageId = 1, type = "SYSTEM", content = "입장"))
                        )
                    )
                )
            }
        }

        val message = (repo(api).getMessages(1L) as ChatResult.Success).value.messages.single()

        assertEquals(ChatMessageType.SYSTEM, message.type)
        assertNull(message.senderId)
        assertNull(message.unreadCount)
    }

    @Test
    fun withoutToken_returnsLoginRequiredWithoutCallingServer() = runTest {
        val api = FakeApi()

        assertEquals(ChatResult.LoginRequired, repo(api, token = null).getMessages(1L))
        assertEquals(ChatResult.LoginRequired, repo(api, token = " ").markAsRead(1L, 3L))
        assertEquals(0, api.calls)
    }

    @Test
    fun emptyBody_isNonRetryableFailure() = runTest {
        val api = FakeApi().apply { pageResponse = { Response.success(ApiResponse(data = null)) } }

        assertEquals(ChatResult.Failure(retryable = false), repo(api).getMessages(1L))
    }

    @Test
    fun errorsAreMapped() = runTest {
        val api = FakeApi()
        val repo = repo(api)

        api.pageResponse = { errorBody(401, """{"code":"EXPIRED_ACCESS_TOKEN"}""") }
        assertEquals(ChatResult.LoginRequired, repo.getMessages(1L))

        api.pageResponse = { errorBody(403, """{"code":"NOT_APPOINTMENT_PARTICIPANT"}""") }
        assertEquals(ChatResult.NotParticipant, repo.getMessages(1L))

        api.pageResponse = { errorBody(404, """{"code":"APPOINTMENT_NOT_FOUND"}""") }
        assertEquals(ChatResult.NotParticipant, repo.getMessages(1L))

        api.pageResponse = { errorBody(500, "oops") }
        assertEquals(ChatResult.Failure(retryable = true), repo.getMessages(1L))

        api.pageResponse = { errorBody(400, """{"code":"INVALID_INPUT"}""") }
        assertEquals(ChatResult.Failure(retryable = false), repo.getMessages(1L))

        api.pageResponse = { throw IOException("offline") }
        assertEquals(ChatResult.Failure(retryable = true), repo.getMessages(1L))

        api.pageResponse = { throw IllegalStateException("bug") }
        assertEquals(ChatResult.Failure(retryable = false), repo.getMessages(1L))
    }

    @Test
    fun markAsRead_sendsLastReadMessageId() = runTest {
        val api = FakeApi()

        val result = repo(api).markAsRead(appointmentId = 4L, lastReadMessageId = 77L)

        assertEquals(ChatResult.Success(Unit), result)
        assertEquals("Bearer token", api.lastAuthorization)
        assertEquals(4L, api.lastAppointmentId)
        assertEquals(ChatReadRequestDto(77L), api.lastRead)
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun tokenWithPayload(payload: String) =
        "header." + Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT).encode(payload.encodeToByteArray()) + ".sig"

    @Test
    fun memberIdFromAccessToken_readsSubject() {
        assertEquals(42L, memberIdFromAccessToken(tokenWithPayload("""{"sub":"42","type":"access"}""")))
    }

    @Test
    fun memberIdFromAccessToken_returnsNullWhenUnreadable() {
        assertNull(memberIdFromAccessToken(null))
        assertNull(memberIdFromAccessToken(""))
        assertNull(memberIdFromAccessToken("only-one-part"))
        assertNull(memberIdFromAccessToken("a.!!!.c"))
        assertNull(memberIdFromAccessToken(tokenWithPayload("""{"type":"access"}""")))
        assertNull(memberIdFromAccessToken(tokenWithPayload("""{"sub":"abc"}""")))
    }
}
