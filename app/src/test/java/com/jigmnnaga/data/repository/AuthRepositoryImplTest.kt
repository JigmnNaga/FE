package com.jigmnnaga.data.repository

import com.jigmnnaga.core.datastore.TokenManager
import com.jigmnnaga.data.remote.api.AuthApi
import com.jigmnnaga.data.remote.dto.ApiResponse
import com.jigmnnaga.data.remote.dto.KakaoLoginRequestDto
import com.jigmnnaga.data.remote.dto.LoginResponseDto
import com.jigmnnaga.domain.model.LoginResult
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
        var lastRequest: KakaoLoginRequestDto? = null

        override suspend fun kakaoLogin(
            request: KakaoLoginRequestDto
        ): Response<ApiResponse<LoginResponseDto>> {
            lastRequest = request
            return onKakaoLogin(request)
        }
    }

    private class FakeTokenManager : TokenManager {
        var savedAccessToken: String? = null
        var savedRefreshToken: String? = null
        var saveShouldThrow: Boolean = false

        override val accessToken: Flow<String?> = flowOf(null)
        override val refreshToken: Flow<String?> = flowOf(null)
        override suspend fun saveTokens(accessToken: String, refreshToken: String) {
            if (saveShouldThrow) throw IOException("DataStore 오류")
            savedAccessToken = accessToken
            savedRefreshToken = refreshToken
        }
        override suspend fun clearTokens() = Unit
        override suspend fun isAccessTokenExpired(): Boolean = false
        override suspend fun isRefreshTokenExpired(): Boolean = false
        override suspend fun validateAndClearInvalidTokens(): Boolean = true
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun repo(api: FakeAuthApi, tokenManager: FakeTokenManager = FakeTokenManager()) =
        AuthRepositoryImpl(api, tokenManager, json)

    private fun successResponse(isNewMember: Boolean = false) = Response.success(
        ApiResponse(
            data = LoginResponseDto(
                memberId = 1L,
                accessToken = "server-access-token",
                refreshToken = "server-refresh-token",
                isNewMember = isNewMember
            )
        )
    )

    private fun errorBody(httpStatus: Int, errorCode: String) =
        """{"success":false,"code":"$errorCode","message":"error"}"""
            .toResponseBody("application/json".toMediaType())
            .let { Response.error<ApiResponse<LoginResponseDto>>(httpStatus, it) }

    // ── 성공 케이스 ──────────────────────────────────────────────────────────

    @Test
    fun `성공 시 두 JWT 토큰을 모두 저장한다`() = runTest {
        val tokenManager = FakeTokenManager()
        val api = FakeAuthApi().apply { onKakaoLogin = { successResponse() } }

        repo(api, tokenManager).kakaoLogin("kakao-token")

        assertNotNull(tokenManager.savedAccessToken)
        assertNotNull(tokenManager.savedRefreshToken)
    }

    @Test
    fun `성공 시 LoginResult_Success를 반환한다`() = runTest {
        val api = FakeAuthApi().apply { onKakaoLogin = { successResponse() } }

        assertTrue(repo(api).kakaoLogin("kakao-token") is LoginResult.Success)
    }

    @Test
    fun `신규 회원 여부가 Success에 담긴다`() = runTest {
        val api = FakeAuthApi().apply { onKakaoLogin = { successResponse(isNewMember = true) } }

        val result = repo(api).kakaoLogin("kakao-token") as LoginResult.Success

        assertTrue(result.isNewMember)
    }

    @Test
    fun `요청에 카카오 액세스 토큰이 전달된다`() = runTest {
        val api = FakeAuthApi().apply { onKakaoLogin = { successResponse() } }

        repo(api).kakaoLogin("my-kakao-token")

        assertEquals("my-kakao-token", api.lastRequest?.kakaoAccessToken)
    }

    // ── 실패 케이스 ──────────────────────────────────────────────────────────

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
        val api = FakeAuthApi().apply { onKakaoLogin = { successResponse() } }

        val result = repo(api, tokenManager).kakaoLogin("kakao-token")

        assertFalse(result is LoginResult.Success)
        assertNull(tokenManager.savedAccessToken)
    }

    @Test
    fun `KAKAO_TOKEN_INVALID 에러코드면 InvalidKakaoToken을 반환한다`() = runTest {
        val api = FakeAuthApi().apply { onKakaoLogin = { errorBody(401, "KAKAO_TOKEN_INVALID") } }

        assertEquals(LoginResult.InvalidKakaoToken, repo(api).kakaoLogin("bad-token"))
    }

    @Test
    fun `WITHDRAWN_MEMBER 에러코드면 WithdrawnMember를 반환한다`() = runTest {
        val api = FakeAuthApi().apply { onKakaoLogin = { errorBody(401, "WITHDRAWN_MEMBER") } }

        assertEquals(LoginResult.WithdrawnMember, repo(api).kakaoLogin("token"))
    }

    @Test
    fun `KAKAO_ACCOUNT_INFO_MISSING 에러코드면 KakaoAccountInfoMissing을 반환한다`() = runTest {
        val api = FakeAuthApi().apply { onKakaoLogin = { errorBody(400, "KAKAO_ACCOUNT_INFO_MISSING") } }

        assertEquals(LoginResult.KakaoAccountInfoMissing, repo(api).kakaoLogin("token"))
    }

    @Test
    fun `HTTP 500이면 ServerError를 반환한다`() = runTest {
        val api = FakeAuthApi().apply { onKakaoLogin = { errorBody(500, "INTERNAL_SERVER_ERROR") } }

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
}
