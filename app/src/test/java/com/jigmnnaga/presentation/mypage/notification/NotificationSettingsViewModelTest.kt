package com.jigmnnaga.presentation.mypage.notification

import com.jigmnnaga.domain.model.NotificationSettings
import com.jigmnnaga.domain.model.NotificationSettingsSyncResult
import com.jigmnnaga.domain.repository.NotificationSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationSettingsViewModelTest {

    private class FakeRepository(
        initial: NotificationSettings = NotificationSettings(),
    ) : NotificationSettingsRepository {
        val current = MutableStateFlow(initial)
        override val settings: Flow<NotificationSettings> = current
        var refreshResult: NotificationSettingsSyncResult = NotificationSettingsSyncResult.Success
        var updateResult: NotificationSettingsSyncResult = NotificationSettingsSyncResult.Success
        var refreshCalls = 0
        var updateCalls = 0

        override suspend fun refresh(): NotificationSettingsSyncResult {
            refreshCalls++
            return refreshResult
        }

        override suspend fun update(
            transform: (NotificationSettings) -> NotificationSettings,
        ): NotificationSettingsSyncResult {
            updateCalls++
            // 실제 저장소처럼 서버 저장에 성공했을 때만 바뀐 값이 남는다.
            if (updateResult == NotificationSettingsSyncResult.Success) current.update(transform)
            return updateResult
        }
    }

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ─────────────────────────────────────────
    // 초기 상태
    // ─────────────────────────────────────────

    @Test
    fun `처음에는 기본값을 보여 준다`() = runTest(dispatcher) {
        val viewModel = NotificationSettingsViewModel(FakeRepository())
        advanceUntilIdle()

        assertEquals(NotificationSettingsUiState(), viewModel.uiState.value)
    }

    @Test
    fun `저장된 설정을 화면 상태로 보여 준다`() = runTest(dispatcher) {
        val repository = FakeRepository(
            NotificationSettings(
                allNotificationsEnabled = false,
                appointmentReminderEnabled = false,
                marketingEnabled = true,
            ),
        )
        val viewModel = NotificationSettingsViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.allNotificationsEnabled)
        assertFalse(state.appointmentReminderEnabled)
        assertTrue(state.marketingEnabled)
    }

    // ─────────────────────────────────────────
    // 화면 진입 시 서버 조회
    // ─────────────────────────────────────────

    @Test
    fun `화면을 열면 서버 값을 한 번 받아 온다`() = runTest(dispatcher) {
        val repository = FakeRepository()
        NotificationSettingsViewModel(repository)
        advanceUntilIdle()

        assertEquals(1, repository.refreshCalls)
    }

    @Test
    fun `서버 값을 받지 못해도 안내를 띄우지 않고 DataStore 값을 보여 준다`() = runTest(dispatcher) {
        val repository = FakeRepository().apply {
            refreshResult = NotificationSettingsSyncResult.Failure(retryable = true)
        }
        val viewModel = NotificationSettingsViewModel(repository)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.saveFailed)
        assertFalse(viewModel.uiState.value.loginRequired)
    }

    @Test
    fun `서버 값을 받을 때 로그인이 만료됐으면 로그인이 필요하다고 알린다`() = runTest(dispatcher) {
        val repository = FakeRepository().apply {
            refreshResult = NotificationSettingsSyncResult.LoginRequired
        }
        val viewModel = NotificationSettingsViewModel(repository)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.loginRequired)

        viewModel.consumeLoginRequired()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.loginRequired)
    }

    // ─────────────────────────────────────────
    // 토글 변경
    // ─────────────────────────────────────────

    @Test
    fun `전체 알림을 끄면 저장하고 화면에 반영한다`() = runTest(dispatcher) {
        val repository = FakeRepository()
        val viewModel = NotificationSettingsViewModel(repository)
        advanceUntilIdle()

        viewModel.onAllNotificationsChange(false)
        advanceUntilIdle()

        assertFalse(repository.current.value.allNotificationsEnabled)
        assertFalse(viewModel.uiState.value.allNotificationsEnabled)
    }

    @Test
    fun `약속 알림을 끄면 저장하고 화면에 반영한다`() = runTest(dispatcher) {
        val repository = FakeRepository()
        val viewModel = NotificationSettingsViewModel(repository)
        advanceUntilIdle()

        viewModel.onAppointmentReminderChange(false)
        advanceUntilIdle()

        assertFalse(repository.current.value.appointmentReminderEnabled)
        assertFalse(viewModel.uiState.value.appointmentReminderEnabled)
    }

    @Test
    fun `출발 시간 알림을 끄면 저장하고 화면에 반영한다`() = runTest(dispatcher) {
        val repository = FakeRepository()
        val viewModel = NotificationSettingsViewModel(repository)
        advanceUntilIdle()

        viewModel.onDepartureTimeChange(false)
        advanceUntilIdle()

        assertFalse(repository.current.value.departureTimeEnabled)
        assertFalse(viewModel.uiState.value.departureTimeEnabled)
    }

    @Test
    fun `친구 도착 알림을 끄면 저장하고 화면에 반영한다`() = runTest(dispatcher) {
        val repository = FakeRepository()
        val viewModel = NotificationSettingsViewModel(repository)
        advanceUntilIdle()

        viewModel.onFriendArrivalChange(false)
        advanceUntilIdle()

        assertFalse(repository.current.value.friendArrivalEnabled)
        assertFalse(viewModel.uiState.value.friendArrivalEnabled)
    }

    @Test
    fun `마케팅 알림을 켜면 저장하고 화면에 반영한다`() = runTest(dispatcher) {
        val repository = FakeRepository()
        val viewModel = NotificationSettingsViewModel(repository)
        advanceUntilIdle()

        viewModel.onMarketingChange(true)
        advanceUntilIdle()

        assertTrue(repository.current.value.marketingEnabled)
        assertTrue(viewModel.uiState.value.marketingEnabled)
    }

    @Test
    fun `한 항목을 바꿔도 다른 설정은 그대로다`() = runTest(dispatcher) {
        val repository = FakeRepository()
        val viewModel = NotificationSettingsViewModel(repository)
        advanceUntilIdle()

        viewModel.onMarketingChange(true)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.allNotificationsEnabled)
        assertTrue(state.appointmentReminderEnabled)
        assertTrue(state.departureTimeEnabled)
        assertTrue(state.friendArrivalEnabled)
    }

    // ─────────────────────────────────────────
    // 저장 실패
    // ─────────────────────────────────────────

    @Test
    fun `서버에 저장하지 못하면 이전 값 그대로 두고 저장 실패를 알린다`() = runTest(dispatcher) {
        val repository = FakeRepository().apply {
            updateResult = NotificationSettingsSyncResult.Failure(retryable = true)
        }
        val viewModel = NotificationSettingsViewModel(repository)
        advanceUntilIdle()

        viewModel.onAllNotificationsChange(false)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.saveFailed)
        assertTrue(viewModel.uiState.value.allNotificationsEnabled)

        viewModel.consumeSaveFailed()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.saveFailed)
    }

    @Test
    fun `저장할 때 로그인이 만료됐으면 로그인이 필요하다고 알린다`() = runTest(dispatcher) {
        val repository = FakeRepository().apply {
            updateResult = NotificationSettingsSyncResult.LoginRequired
        }
        val viewModel = NotificationSettingsViewModel(repository)
        advanceUntilIdle()

        viewModel.onMarketingChange(true)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.loginRequired)
        assertFalse(viewModel.uiState.value.saveFailed)
    }

    // ─────────────────────────────────────────
    // 기존 DataStore 기능 회귀 테스트
    // ─────────────────────────────────────────

    @Test
    fun `저장 성공 시 saveFailed가 false다`() = runTest(dispatcher) {
        val repository = FakeRepository()
        val viewModel = NotificationSettingsViewModel(repository)
        advanceUntilIdle()

        viewModel.onMarketingChange(true)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.saveFailed)
    }

    @Test
    fun `저장 성공 시 loginRequired가 false다`() = runTest(dispatcher) {
        val repository = FakeRepository()
        val viewModel = NotificationSettingsViewModel(repository)
        advanceUntilIdle()

        viewModel.onDepartureTimeChange(false)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.loginRequired)
    }
}
