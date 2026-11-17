package com.jigmnnaga.presentation.common.splash

import com.jigmnnaga.core.datastore.TokenManager
import com.jigmnnaga.domain.model.LoginResult
import com.jigmnnaga.domain.model.ReissueResult
import com.jigmnnaga.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    // ── Fakes ────────────────────────────────────────────────────────────────

    private class FakeTokenManager : TokenManager {
        var validateResult: Boolean = true
        var isAccessExpired: Boolean = false
        var clearCalled: Boolean = false

        override val accessToken: Flow<String?> = flowOf(null)
        override val refreshToken: Flow<String?> = flowOf(null)
        override suspend fun saveTokens(accessToken: String, refreshToken: String) = Unit
        override suspend fun clearTokens() { clearCalled = true }
        override suspend fun isAccessTokenExpired(): Boolean = isAccessExpired
        override suspend fun isRefreshTokenExpired(): Boolean = false
        override suspend fun validateAndClearInvalidTokens(): Boolean = validateResult
    }

    private class FakeAuthRepository : AuthRepository {
        var nextReissueResult: ReissueResult = ReissueResult.Success

        override suspend fun kakaoLogin(kakaoAccessToken: String): LoginResult =
            LoginResult.Success(isNewMember = false)

        override suspend fun reissueTokens(): ReissueResult = nextReissueResult
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun viewModel(
        tokenManager: TokenManager = FakeTokenManager(),
        repo: AuthRepository = FakeAuthRepository()
    ) = SplashViewModel(tokenManager, repo)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Case A: Access Token 유효 ─────────────────────────────────────────────

    @Test
    fun `Case A - Access Token 유효 시 Authenticated 상태가 된다`() = runTest {
        val tokenManager = FakeTokenManager().apply {
            validateResult = true
            isAccessExpired = false
        }
        val vm = viewModel(tokenManager)

        assertEquals(SplashAuthState.Authenticated, vm.authState.value)
    }

    @Test
    fun `Case A - Access Token 유효 시 재발급 API를 호출하지 않는다`() = runTest {
        var reissueCalled = false
        val tokenManager = FakeTokenManager().apply {
            validateResult = true
            isAccessExpired = false
        }
        val repo = object : AuthRepository {
            override suspend fun kakaoLogin(kakaoAccessToken: String) =
                LoginResult.Success(isNewMember = false)
            override suspend fun reissueTokens(): ReissueResult {
                reissueCalled = true
                return ReissueResult.Success
            }
        }
        viewModel(tokenManager, repo)

        assertEquals(false, reissueCalled)
    }

    // ── Case B: Access Token 만료 + Refresh 성공 ─────────────────────────────

    @Test
    fun `Case B - Access Token 만료 + 재발급 성공 시 Authenticated 상태가 된다`() = runTest {
        val tokenManager = FakeTokenManager().apply {
            validateResult = true
            isAccessExpired = true
        }
        val repo = FakeAuthRepository().apply { nextReissueResult = ReissueResult.Success }
        val vm = viewModel(tokenManager, repo)

        assertEquals(SplashAuthState.Authenticated, vm.authState.value)
    }

    // ── Case C: 토큰 없음 ─────────────────────────────────────────────────────

    @Test
    fun `Case C - 토큰 없음 시 Unauthenticated 상태가 된다`() = runTest {
        val tokenManager = FakeTokenManager().apply { validateResult = false }
        val vm = viewModel(tokenManager)

        assertEquals(SplashAuthState.Unauthenticated, vm.authState.value)
    }

    // ── Case D: Refresh Token 만료 또는 무효 ─────────────────────────────────

    @Test
    fun `Case D - Refresh Token 만료 시 Unauthenticated 상태가 된다`() = runTest {
        val tokenManager = FakeTokenManager().apply {
            validateResult = true
            isAccessExpired = true
        }
        val repo = FakeAuthRepository().apply {
            nextReissueResult = ReissueResult.TokenInvalidOrExpired
        }
        val vm = viewModel(tokenManager, repo)

        assertEquals(SplashAuthState.Unauthenticated, vm.authState.value)
    }

    @Test
    fun `Case D - Refresh Token 만료 시 저장된 토큰을 삭제한다`() = runTest {
        val tokenManager = FakeTokenManager().apply {
            validateResult = true
            isAccessExpired = true
        }
        val repo = FakeAuthRepository().apply {
            nextReissueResult = ReissueResult.TokenInvalidOrExpired
        }
        viewModel(tokenManager, repo)

        assertEquals(true, tokenManager.clearCalled)
    }

    // ── Case E: 네트워크/서버 오류 ───────────────────────────────────────────

    @Test
    fun `Case E - 네트워크 오류 시 RetryableError 상태가 된다`() = runTest {
        val tokenManager = FakeTokenManager().apply {
            validateResult = true
            isAccessExpired = true
        }
        val repo = FakeAuthRepository().apply {
            nextReissueResult = ReissueResult.NetworkError("IOException")
        }
        val vm = viewModel(tokenManager, repo)

        assertEquals(SplashAuthState.RetryableError(isNetworkError = true), vm.authState.value)
    }

    @Test
    fun `Case E - 네트워크 오류 시 isNetworkError가 true이다`() = runTest {
        val tokenManager = FakeTokenManager().apply {
            validateResult = true
            isAccessExpired = true
        }
        val repo = FakeAuthRepository().apply {
            nextReissueResult = ReissueResult.NetworkError()
        }
        val vm = viewModel(tokenManager, repo)

        assertEquals(true, (vm.authState.value as SplashAuthState.RetryableError).isNetworkError)
    }

    @Test
    fun `Case E - 서버 오류 시 RetryableError 상태가 된다`() = runTest {
        val tokenManager = FakeTokenManager().apply {
            validateResult = true
            isAccessExpired = true
        }
        val repo = FakeAuthRepository().apply {
            nextReissueResult = ReissueResult.ServerError("HTTP 500")
        }
        val vm = viewModel(tokenManager, repo)

        assertEquals(SplashAuthState.RetryableError(isNetworkError = false), vm.authState.value)
    }

    @Test
    fun `Case E - 서버 오류 시 isNetworkError가 false이다`() = runTest {
        val tokenManager = FakeTokenManager().apply {
            validateResult = true
            isAccessExpired = true
        }
        val repo = FakeAuthRepository().apply {
            nextReissueResult = ReissueResult.ServerError()
        }
        val vm = viewModel(tokenManager, repo)

        assertEquals(false, (vm.authState.value as SplashAuthState.RetryableError).isNetworkError)
    }

    @Test
    fun `Case E - 네트워크 오류 시 저장된 토큰을 삭제하지 않는다`() = runTest {
        val tokenManager = FakeTokenManager().apply {
            validateResult = true
            isAccessExpired = true
        }
        val repo = FakeAuthRepository().apply {
            nextReissueResult = ReissueResult.NetworkError()
        }
        viewModel(tokenManager, repo)

        assertEquals(false, tokenManager.clearCalled)
    }

    // ── Case F: 복호화 실패 ───────────────────────────────────────────────────

    @Test
    fun `Case F - 복호화 실패 시 Unauthenticated 상태가 된다`() = runTest {
        val tokenManager = FakeTokenManager().apply { validateResult = false }
        val vm = viewModel(tokenManager)

        assertEquals(SplashAuthState.Unauthenticated, vm.authState.value)
    }

    // ── 재시도 ────────────────────────────────────────────────────────────────

    @Test
    fun `재시도 후 성공 시 Authenticated 상태가 된다`() = runTest {
        val tokenManager = FakeTokenManager().apply {
            validateResult = true
            isAccessExpired = true
        }
        val repo = FakeAuthRepository().apply {
            nextReissueResult = ReissueResult.NetworkError()
        }
        val vm = viewModel(tokenManager, repo)
        assertEquals(SplashAuthState.RetryableError(isNetworkError = true), vm.authState.value)

        repo.nextReissueResult = ReissueResult.Success
        vm.retry()

        assertEquals(SplashAuthState.Authenticated, vm.authState.value)
    }

    @Test
    fun `RetryableError가 아닌 상태에서 retry 호출 시 아무 일도 없다`() = runTest {
        val tokenManager = FakeTokenManager().apply {
            validateResult = true
            isAccessExpired = false
        }
        val vm = viewModel(tokenManager)
        assertEquals(SplashAuthState.Authenticated, vm.authState.value)

        vm.retry()

        assertEquals(SplashAuthState.Authenticated, vm.authState.value)
    }
}
