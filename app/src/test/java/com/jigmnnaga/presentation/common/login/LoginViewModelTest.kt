package com.jigmnnaga.presentation.common.login

import android.app.Activity
import com.jigmnnaga.domain.model.LoginResult
import com.jigmnnaga.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    // ── Fakes ────────────────────────────────────────────────────────────────

    private class FakeKakaoLoginClient : KakaoLoginClient {
        var nextResult: KakaoLoginResult = KakaoLoginResult.Cancelled

        override fun login(activity: Activity, onResult: (KakaoLoginResult) -> Unit) {
            onResult(nextResult)
        }
    }

    private class FakeAuthRepository : AuthRepository {
        var nextResult: LoginResult = LoginResult.Success(isNewMember = false)

        override suspend fun kakaoLogin(kakaoAccessToken: String): LoginResult = nextResult
        override suspend fun reissueTokens() = com.jigmnnaga.domain.model.ReissueResult.Success
        override suspend fun logout() = com.jigmnnaga.domain.model.LogoutResult.Success
        override suspend fun withdraw() = com.jigmnnaga.domain.model.WithdrawResult.Success
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private val fakeActivity: Activity get() = Activity()

    private fun viewModel(
        client: KakaoLoginClient = FakeKakaoLoginClient(),
        repo: AuthRepository = FakeAuthRepository()
    ) = LoginViewModel(client, repo)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── 성공 케이스 ──────────────────────────────────────────────────────────

    @Test
    fun `카카오 성공 후 백엔드 성공 시 Success 상태가 된다`() = runTest {
        val client = FakeKakaoLoginClient().apply {
            nextResult = KakaoLoginResult.Success("kakao-token")
        }
        val repo = FakeAuthRepository().apply {
            nextResult = LoginResult.Success(isNewMember = false)
        }
        val vm = viewModel(client, repo)

        vm.login(fakeActivity)

        assertEquals(LoginUiState.Success, vm.uiState.value)
    }

    // ── 카카오 SDK 결과 ──────────────────────────────────────────────────────

    @Test
    fun `카카오 취소 시 Idle 상태가 된다`() = runTest {
        val client = FakeKakaoLoginClient().apply { nextResult = KakaoLoginResult.Cancelled }
        val vm = viewModel(client)

        vm.login(fakeActivity)

        assertEquals(LoginUiState.Idle, vm.uiState.value)
    }

    @Test
    fun `SDK 미초기화 시 SdkNotInitialized 상태가 된다`() = runTest {
        val client = FakeKakaoLoginClient().apply { nextResult = KakaoLoginResult.SdkNotInitialized }
        val vm = viewModel(client)

        vm.login(fakeActivity)

        assertEquals(LoginUiState.Failure.SdkNotInitialized, vm.uiState.value)
    }

    @Test
    fun `카카오 SDK 실패 시 KakaoAuthFailed 상태가 된다`() = runTest {
        val client = FakeKakaoLoginClient().apply {
            nextResult = KakaoLoginResult.Failure(RuntimeException("SDK 오류"))
        }
        val vm = viewModel(client)

        vm.login(fakeActivity)

        assertEquals(LoginUiState.Failure.KakaoAuthFailed, vm.uiState.value)
    }

    // ── 백엔드 에러 케이스 ───────────────────────────────────────────────────

    @Test
    fun `백엔드 InvalidKakaoToken 시 해당 Failure 상태가 된다`() = runTest {
        val client = FakeKakaoLoginClient().apply {
            nextResult = KakaoLoginResult.Success("token")
        }
        val repo = FakeAuthRepository().apply { nextResult = LoginResult.InvalidKakaoToken }
        val vm = viewModel(client, repo)

        vm.login(fakeActivity)

        assertEquals(LoginUiState.Failure.InvalidKakaoToken, vm.uiState.value)
    }

    @Test
    fun `백엔드 WithdrawnMember 시 해당 Failure 상태가 된다`() = runTest {
        val client = FakeKakaoLoginClient().apply {
            nextResult = KakaoLoginResult.Success("token")
        }
        val repo = FakeAuthRepository().apply { nextResult = LoginResult.WithdrawnMember }
        val vm = viewModel(client, repo)

        vm.login(fakeActivity)

        assertEquals(LoginUiState.Failure.WithdrawnMember, vm.uiState.value)
    }

    @Test
    fun `백엔드 KakaoAccountInfoMissing 시 해당 Failure 상태가 된다`() = runTest {
        val client = FakeKakaoLoginClient().apply {
            nextResult = KakaoLoginResult.Success("token")
        }
        val repo = FakeAuthRepository().apply { nextResult = LoginResult.KakaoAccountInfoMissing }
        val vm = viewModel(client, repo)

        vm.login(fakeActivity)

        assertEquals(LoginUiState.Failure.KakaoAccountInfoMissing, vm.uiState.value)
    }

    @Test
    fun `백엔드 NetworkError 시 NetworkError Failure 상태가 된다`() = runTest {
        val client = FakeKakaoLoginClient().apply {
            nextResult = KakaoLoginResult.Success("token")
        }
        val repo = FakeAuthRepository().apply {
            nextResult = LoginResult.NetworkError(debugInfo = "IOException")
        }
        val vm = viewModel(client, repo)

        vm.login(fakeActivity)

        assertTrue(vm.uiState.value is LoginUiState.Failure.NetworkError)
    }

    @Test
    fun `NetworkError의 debugInfo가 그대로 전달된다`() = runTest {
        val client = FakeKakaoLoginClient().apply {
            nextResult = KakaoLoginResult.Success("token")
        }
        val repo = FakeAuthRepository().apply {
            nextResult = LoginResult.NetworkError(debugInfo = "SocketTimeoutException")
        }
        val vm = viewModel(client, repo)

        vm.login(fakeActivity)

        assertEquals("SocketTimeoutException", (vm.uiState.value as LoginUiState.Failure.NetworkError).debugInfo)
    }

    @Test
    fun `백엔드 ServerError 시 ServerError Failure 상태가 된다`() = runTest {
        val client = FakeKakaoLoginClient().apply {
            nextResult = KakaoLoginResult.Success("token")
        }
        val repo = FakeAuthRepository().apply {
            nextResult = LoginResult.ServerError(debugInfo = "HTTP 500")
        }
        val vm = viewModel(client, repo)

        vm.login(fakeActivity)

        assertTrue(vm.uiState.value is LoginUiState.Failure.ServerError)
    }

    // ── 중복 호출 방지 ───────────────────────────────────────────────────────

    @Test
    fun `Loading 중 login 재호출은 무시된다`() = runTest {
        var callCount = 0
        val client = object : KakaoLoginClient {
            override fun login(activity: Activity, onResult: (KakaoLoginResult) -> Unit) {
                callCount++
                // 콜백을 호출하지 않아 Loading 상태 유지
            }
        }
        val vm = viewModel(client)

        vm.login(fakeActivity) // Loading 상태 진입 (콜백 없음)
        vm.login(fakeActivity) // 무시되어야 함

        assertEquals(1, callCount)
    }

    // ── clearSuccess / clearFailure ──────────────────────────────────────────

    @Test
    fun `clearSuccess 호출 시 Success 상태가 Idle로 바뀐다`() = runTest {
        val client = FakeKakaoLoginClient().apply {
            nextResult = KakaoLoginResult.Success("token")
        }
        val vm = viewModel(client)
        vm.login(fakeActivity)
        assertEquals(LoginUiState.Success, vm.uiState.value)

        vm.clearSuccess()

        assertEquals(LoginUiState.Idle, vm.uiState.value)
    }

    @Test
    fun `clearSuccess는 Success 상태가 아닐 때 아무 일도 안 한다`() = runTest {
        val vm = viewModel()
        // Idle 상태에서 clearSuccess 호출
        vm.clearSuccess()

        assertEquals(LoginUiState.Idle, vm.uiState.value)
    }

    @Test
    fun `clearFailure 호출 시 Failure 상태가 Idle로 바뀐다`() = runTest {
        val client = FakeKakaoLoginClient().apply { nextResult = KakaoLoginResult.SdkNotInitialized }
        val vm = viewModel(client)
        vm.login(fakeActivity)
        assertTrue(vm.uiState.value is LoginUiState.Failure)

        vm.clearFailure()

        assertEquals(LoginUiState.Idle, vm.uiState.value)
    }
}
