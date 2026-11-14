package com.jigmnnaga.data.repository

import com.jigmnnaga.core.datastore.TokenManager
import com.jigmnnaga.data.remote.api.NotificationApi
import com.jigmnnaga.data.remote.dto.ApiResponse
import com.jigmnnaga.data.remote.dto.NotificationCountDto
import com.jigmnnaga.data.remote.dto.NotificationDto
import com.jigmnnaga.data.remote.dto.NotificationPageDto
import com.jigmnnaga.domain.model.NotificationResult
import com.jigmnnaga.domain.model.NotificationType
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

class NotificationRepositoryImplTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private class FakeApi : NotificationApi {
        var onGet: suspend () -> Response<ApiResponse<NotificationPageDto>> = { error("not set") }
        var onRead: suspend () -> Response<Unit> = { error("not set") }
        var onReadAll: suspend () -> Response<ApiResponse<NotificationCountDto>> = { error("not set") }
        var calls = 0
        var lastAuthorization: String? = null
        var lastCursor: Long? = null

        override suspend fun getNotifications(
            authorization: String?,
            cursor: Long?,
            size: Int
        ): Response<ApiResponse<NotificationPageDto>> {
            calls++; lastAuthorization = authorization; lastCursor = cursor
            return onGet()
        }

        override suspend fun readNotification(authorization: String?, notificationId: Long): Response<Unit> {
            calls++; lastAuthorization = authorization
            return onRead()
        }

        override suspend fun readAllNotifications(
            authorization: String?
        ): Response<ApiResponse<NotificationCountDto>> {
            calls++; lastAuthorization = authorization
            return onReadAll()
        }
    }

    private class FakeTokenManager(private val token: String?) : TokenManager {
        override val accessToken: Flow<String?> = flowOf(token)
        override val refreshToken: Flow<String?> = flowOf(null)
        override suspend fun saveTokens(accessToken: String, refreshToken: String) = Unit
        override suspend fun clearTokens() = Unit
        override suspend fun isAccessTokenExpired(): Boolean = false
        override suspend fun isRefreshTokenExpired(): Boolean = false
        override suspend fun validateAndClearInvalidTokens(): Boolean = false
    }

    private fun repo(api: FakeApi, token: String? = "token") =
        NotificationRepositoryImpl(api, FakeTokenManager(token), json)

    private fun errorBody(code: Int, body: String = ""): Response<Unit> =
        Response.error(code, body.toResponseBody("application/json".toMediaType()))

    @Test
    fun `토큰이 없으면 서버를 부르지 않고 로그인 필요를 돌려준다`() = runTest {
        val api = FakeApi()
        val result = repo(api, token = null).getNotifications(null, 20)
        assertEquals(NotificationResult.LoginRequired, result)
        assertEquals(0, api.calls)
    }

    @Test
    fun `목록 조회에 성공하면 Bearer 토큰과 커서를 보내고 도메인으로 바꾼다`() = runTest {
        val api = FakeApi()
        api.onGet = {
            Response.success(
                ApiResponse(
                    success = true,
                    message = "ok",
                    data = NotificationPageDto(
                        notifications = listOf(
                            NotificationDto(1, "INVITED", "제목", "본문", 7L, false, null),
                            NotificationDto(2, "NEW_TYPE", "t", "b", null, true, null)
                        ),
                        nextCursor = 1L,
                        hasNext = true
                    )
                )
            )
        }
        val result = repo(api).getNotifications(cursor = 5L, size = 20)

        val page = (result as NotificationResult.Success).value
        assertEquals("Bearer token", api.lastAuthorization)
        assertEquals(5L, api.lastCursor)
        assertEquals(NotificationType.INVITED, page.items[0].type)
        assertEquals(NotificationType.OTHER, page.items[1].type)
        assertEquals(7L, page.items[0].appointmentId)
        assertTrue(page.hasNext)
        assertEquals(1L, page.nextCursor)
    }

    @Test
    fun `응답 본문이 비어 있으면 재시도 불가 실패`() = runTest {
        val api = FakeApi()
        api.onGet = { Response.success(null) }
        assertEquals(NotificationResult.Failure(false), repo(api).getNotifications(null, 20))
    }

    @Test
    fun `401은 로그인 필요`() = runTest {
        val api = FakeApi()
        api.onRead = { errorBody(401) }
        assertEquals(NotificationResult.LoginRequired, repo(api).readNotification(1))
    }

    @Test
    fun `토큰 만료 코드는 로그인 필요`() = runTest {
        val api = FakeApi()
        api.onRead = { errorBody(403, """{"code":"EXPIRED_ACCESS_TOKEN","message":"x"}""") }
        assertEquals(NotificationResult.LoginRequired, repo(api).readNotification(1))
    }

    @Test
    fun `500은 재시도 가능 실패, 404는 재시도 불가 실패`() = runTest {
        val api = FakeApi()
        api.onRead = { errorBody(500) }
        assertEquals(NotificationResult.Failure(true), repo(api).readNotification(1))
        api.onRead = { errorBody(404) }
        assertEquals(NotificationResult.Failure(false), repo(api).readNotification(1))
    }

    @Test
    fun `네트워크 오류는 재시도 가능 실패`() = runTest {
        val api = FakeApi()
        api.onRead = { throw IOException("offline") }
        assertEquals(NotificationResult.Failure(true), repo(api).readNotification(1))
    }

    @Test
    fun `전체 읽음은 읽음 처리한 개수를 돌려준다`() = runTest {
        val api = FakeApi()
        api.onReadAll = { Response.success(ApiResponse(true, "ok", NotificationCountDto(3))) }
        assertEquals(NotificationResult.Success(3L), repo(api).readAllNotifications())
    }
}
