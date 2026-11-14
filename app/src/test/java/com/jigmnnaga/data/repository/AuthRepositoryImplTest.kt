package com.jigmnnaga.data.repository

import com.jigmnnaga.core.datastore.TokenManager
import com.jigmnnaga.data.remote.api.AuthApi
import com.jigmnnaga.data.remote.dto.ApiResponse
import com.jigmnnaga.data.remote.dto.KakaoLoginRequestDto
import com.jigmnnaga.data.remote.dto.LoginResponseDto
import com.jigmnnaga.data.remote.dto.TokenReissueRequestDto
import com.jigmnnaga.data.remote.dto.TokenReissueResponseDto
import com.jigmnnaga.domain.model.LoginResult
import com.jigmnnaga.domain.model.ReissueResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class AuthRepositoryImplTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // ── Fakes ────────────────────────────────────────────────────────────────

    private class FakeAuthApi : AuthApi {
        var onKakaoLogin: suspend (KakaoLoginRequestDto) -> Response<ApiResponse<LoginResponseDto>> =
            { error("not set") }
        var lastKakaoRequest: KakaoLoginRequestDto? = null

        var onReissueTokens: suspend (TokenReissueRequestDto) -> Response<ApiResponse<TokenReissueResponseDto>> =
            { error("not set") }
        var lastReissueRequest: TokenReissueRequestDto? = null

        override suspend fun kakaoLogin(
            request: KakaoLoginRequestDto
        ): Response<ApiResponse<LoginResponseDto>> {
            lastKakaoRequest = request
            return onKakaoLogin(request)
        }

        override suspend fun reissueTokens(
            request: TokenReissueRequestDto
        ): Response<ApiResponse<TokenReissueResponseDto>> {
            lastReissueRequest = request
            return onReissueTokens(request)
        }
    }

    private class FakeTokenManager(
        storedRefreshToken: String? = null
    ) : TokenManager {
        var savedAccessToken: String? = null
        var savedRefreshToken: String? = null
        var saveShouldThrow: Boolean = false
        var clearCalled: Boolean = false
        private val _storedRefreshToken = storedRefreshToken

        override val accessToken: Flow<String?> = flowOf(null)
        override val refreshToken: Flow<String?> = flowOf(_storedRefreshToken)

        override suspend fun saveTokens(accessToken: String, refreshToken: String) {
            if (saveShouldThrow) throw IOException("DataStore 오류")
            savedAccessToken = accessToken
            savedRefreshToken = refreshToken
        }
        override suspend fun clearTokens() { clearCalled = true }
        override suspend fun isAccessTokenExpired(): Boolean = false
        override suspend fun isRefreshTokenExpired(): Boolean = false
        override suspend fun validateAndClearInvalidTokens(): Boolean = true
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun repo(api: FakeAuthApi, tokenManager: FakeTokenManager = FakeTokenManager()) =
        AuthRepositoryImpl(api, tokenManager, json)

    private fun kakaoSuccessResponse(isNewMember: Boolean = false) = Response.success(
        ApiResponse(
            data = LoginResponseDto(
                memberId = 1L,
                accessToken = "server-access-token",
                refreshToken = "server-refresh-token",
                isNewMember = isNewMember
            )
        )
    )

    private fun reissueSuccessResponse() = Response.success(
        ApiResponse(
            data = TokenReissueResponseDto(
                accessToken = "new-access-token",
                refreshToken = "new-refresh-token"
            )
        )
    )

    private fun kakaoErrorBody(httpStatus: Int, errorCode: String) =
        """{"success":false,"code":"$errorCode","message":"error"}"""
            .toResponseBody("application/json".toMediaType())
            .let { Response.error<ApiResponse<LoginResponseDto>>(httpStatus, it) }

    private fun reissueErrorBody(httpStatus: Int, errorCode: String) =
        """{"success":false,"code":"$errorCode","message":"error"}"""
            .toResponseBody("application/json".toMediaType())
            .let { Response.error<ApiResponse<TokenReissueResponseDto>>(httpStatus, it) }

    // ── kakaoLogin 성공 케이스 ───────────────────────────────────────────────

    @Test
    fun `성공 시 두 JWT 토큰을 모두 저장한다`() = runTest {
        val tokenManager = FakeTokenManager()
        val api = FakeAuthApi().apply { onKakaoLogin = { kakaoSuccessResponse() } }

        repo(api, tokenManager).kakaoLogin("kakao-token")

        assertNotNull(tokenManager.savedAccessToken)
        assertNotNull(tokenManager.savedRefreshToken)
    }

    @Test
    fun `성공 시 LoginResult_Success를 반환한다`() = runTest {
        val api = FakeAuthApi().apply { onKakaoLogin = { kakaoSuccessResponse() } }

        assertTrue(repo(api).kakaoLogin("kakao-token") is LoginResult.Success)
    }

    @Test
    fun `신규 회원 여부가 Success에 담긴다`() = runTest {
        val api = FakeAuthApi().apply { onKakaoLogin = { kakaoSuccessResponse(isNewMember = true) } }

        val result = repo(api).kakaoLogin("kakao-token") as LoginResult.Success

        assertTrue(result.isNewMember)
    }

    @Test
    fun `요청에 카카오 액세스 토큰이 전달된다`() = runTest {
        val api = FakeAuthApi().apply { onKakaoLogin = { kakaoSuccessResponse() } }

        repo(api).kakaoLogin("my-kakao-token")

        assertEquals("my-kakao-token", api.lastKakaoRequest?.kakaoAccessToken)
    }

    // ── kakaoLogin 실패 케이스 ───────────────────────────────────────────────

    @Test
    fun `응답 body가 null이면 ServerError를 반환한다`() = runTest {
        val api = FakeAuthApi().apply {
            onKakaoLogin = { Response.success(ApiResponse<LoginResponseDto>()) }
        }

        assertTrue(repo(api).kakaoLogin("kakao-token") is LoginResult.ServerError)
    }

    @Test
    fun `토큰 저장 실패 시 로그인 성공 처리되지 않는다`() = runTest {
        val tokenManager = FakeTokenManager().apply { saveShouldThrow = true }
        val api = FakeAuthApi().apply { onKakaoLogin = { kakaoSuccessResponse() } }

        val result = repo(api, tokenManager).kakaoLogin("kakao-token")

        assertFalse(result is LoginResult.Success)
        assertNull(tokenManager.savedAccessToken)
    }

    @Test
    fun `KAKAO_TOKEN_INVALID 에러코드면 InvalidKakaoToken을 반환한다`() = runTest {
        val api = FakeAuthApi().apply { onKakaoLogin = { kakaoErrorBody(401, "KAKAO_TOKEN_INVALID") } }

        assertEquals(LoginResult.InvalidKakaoToken, repo(api).kakaoLogin("bad-token"))
    }

    @Test
    fun `WITHDRAWN_MEMBER 에러코드면 WithdrawnMember를 반환한다`() = runTest {
        val api = FakeAuthApi().apply { onKakaoLogin = { kakaoErrorBody(401, "WITHDRAWN_MEMBER") } }

        assertEquals(LoginResult.WithdrawnMember, repo(api).kakaoLogin("token"))
    }

    @Test
    fun `KAKAO_ACCOUNT_INFO_MISSING 에러코드면 KakaoAccountInfoMissing을 반환한다`() = runTest {
        val api = FakeAuthApi().apply { onKakaoLogin = { kakaoErrorBody(400, "KAKAO_ACCOUNT_INFO_MISSING") } }

        assertEquals(LoginResult.KakaoAccountInfoMissing, repo(api).kakaoLogin("token"))
    }

    @Test
    fun `HTTP 500이면 ServerError를 반환한다`() = runTest {
        val api = FakeAuthApi().apply { onKakaoLogin = { kakaoErrorBody(500, "INTERNAL_SERVER_ERROR") } }

        assertTrue(repo(api).kakaoLogin("token") is LoginResult.ServerError)
    }

    @Test
    fun `IOException이면 NetworkError를 반환한다`() = runTest {
        val api = FakeAuthApi().apply { onKakaoLogin = { throw IOException("연결 실패") } }

        assertTrue(repo(api).kakaoLogin("token") is LoginResult.NetworkError)
    }

    @Test
    fun `NetworkError에 예외 클래스명이 담긴다`() = runTest {
        val api = FakeAuthApi().apply { onKakaoLogin = { throw IOException("연결 실패") } }

        val result = repo(api).kakaoLogin("token") as LoginResult.NetworkError

        assertEquals("IOException", result.debugInfo)
    }

    // ── reissueTokens 성공 케이스 ────────────────────────────────────────────

    @Test
    fun `재발급 성공 시 두 토큰이 모두 저장된다`() = runTest {
        val tokenManager = FakeTokenManager(storedRefreshToken = "stored-refresh-token")
        val api = FakeAuthApi().apply { onReissueTokens = { reissueSuccessResponse() } }

        repo(api, tokenManager).reissueTokens()

        assertEquals("new-access-token", tokenManager.savedAccessToken)
        assertEquals("new-refresh-token", tokenManager.savedRefreshToken)
    }

    @Test
    fun `재발급 성공 시 Success를 반환한다`() = runTest {
        val tokenManager = FakeTokenManager(storedRefreshToken = "stored-refresh-token")
        val api = FakeAuthApi().apply { onReissueTokens = { reissueSuccessResponse() } }

        assertEquals(ReissueResult.Success, repo(api, tokenManager).reissueTokens())
    }

    @Test
    fun `재발급 요청에 저장된 Refresh Token이 전달된다`() = runTest {
        val tokenManager = FakeTokenManager(storedRefreshToken = "my-stored-refresh")
        val api = FakeAuthApi().apply { onReissueTokens = { reissueSuccessResponse() } }

        repo(api, tokenManager).reissueTokens()

        assertEquals("my-stored-refresh", api.lastReissueRequest?.refreshToken)
    }

    // ── reissueTokens 실패 케이스 ────────────────────────────────────────────

    @Test
    fun `저장된 Refresh Token이 없으면 TokenInvalidOrExpired를 반환한다`() = runTest {
        val api = FakeAuthApi()

        assertEquals(
            ReissueResult.TokenInvalidOrExpired,
            repo(api, FakeTokenManager(storedRefreshToken = null)).reissueTokens()
        )
    }

    @Test
    fun `EXPIRED_REFRESH_TOKEN 에러코드면 TokenInvalidOrExpired를 반환한다`() = runTest {
        val tokenManager = FakeTokenManager(storedRefreshToken = "expired-token")
        val api = FakeAuthApi().apply {
            onReissueTokens = { reissueErrorBody(401, "EXPIRED_REFRESH_TOKEN") }
        }

        assertEquals(ReissueResult.TokenInvalidOrExpired, repo(api, tokenManager).reissueTokens())
    }

    @Test
    fun `INVALID_REFRESH_TOKEN 에러코드면 TokenInvalidOrExpired를 반환한다`() = runTest {
        val tokenManager = FakeTokenManager(storedRefreshToken = "invalid-token")
        val api = FakeAuthApi().apply {
            onReissueTokens = { reissueErrorBody(401, "INVALID_REFRESH_TOKEN") }
        }

        assertEquals(ReissueResult.TokenInvalidOrExpired, repo(api, tokenManager).reissueTokens())
    }

    @Test
    fun `재발급 HTTP 500이면 ServerError를 반환한다`() = runTest {
        val tokenManager = FakeTokenManager(storedRefreshToken = "token")
        val api = FakeAuthApi().apply {
            onReissueTokens = { reissueErrorBody(500, "INTERNAL_SERVER_ERROR") }
        }

        assertTrue(repo(api, tokenManager).reissueTokens() is ReissueResult.ServerError)
    }

    @Test
    fun `재발급 IOException이면 NetworkError를 반환한다`() = runTest {
        val tokenManager = FakeTokenManager(storedRefreshToken = "token")
        val api = FakeAuthApi().apply { onReissueTokens = { throw IOException("연결 실패") } }

        assertTrue(repo(api, tokenManager).reissueTokens() is ReissueResult.NetworkError)
    }

    @Test
    fun `재발급 성공 후 기존 kakaoLogin은 그대로 동작한다`() = runTest {
        val tokenManager = FakeTokenManager()
        val api = FakeAuthApi().apply { onKakaoLogin = { kakaoSuccessResponse() } }

        val result = repo(api, tokenManager).kakaoLogin("kakao-token")

        assertTrue(result is LoginResult.Success)
    }
}
