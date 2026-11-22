package com.jigmnnaga.presentation.mypage.sharing

import com.jigmnnaga.domain.model.LocationSharingScope
import com.jigmnnaga.domain.model.LocationSharingSettings
import com.jigmnnaga.domain.model.LocationSharingStartTime
import com.jigmnnaga.domain.model.LocationSharingSyncResult
import com.jigmnnaga.domain.repository.LocationSharingSettingsRepository
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
class LocationSharingSettingsViewModelTest {
    private class FakeRepository(initial: LocationSharingSettings = LocationSharingSettings()) :
        LocationSharingSettingsRepository {
        val current = MutableStateFlow(initial)
        override val settings: Flow<LocationSharingSettings> = current
        var refreshResult: LocationSharingSyncResult = LocationSharingSyncResult.Success
        var updateResult: LocationSharingSyncResult = LocationSharingSyncResult.Success
        var refreshCalls = 0
        var updateCalls = 0

        override suspend fun refresh(): LocationSharingSyncResult {
            refreshCalls++
            return refreshResult
        }

        override suspend fun update(
            transform: (LocationSharingSettings) -> LocationSharingSettings
        ): LocationSharingSyncResult {
            updateCalls++
            // 실제 저장소처럼 서버 저장에 성공했을 때만 바뀐 값이 남는다.
            if (updateResult == LocationSharingSyncResult.Success) current.update(transform)
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

    @Test
    fun `처음에는 Figma 기본값을 보여 준다`() = runTest(dispatcher) {
        val viewModel = LocationSharingSettingsViewModel(FakeRepository())
        advanceUntilIdle()

        assertEquals(LocationSharingSettingsUiState(), viewModel.uiState.value)
    }

    @Test
    fun `저장된 설정을 화면 상태로 보여 준다`() = runTest(dispatcher) {
        val repository = FakeRepository(
            LocationSharingSettings(
                sharingEnabled = false,
                scope = LocationSharingScope.NONE,
                startTime = LocationSharingStartTime.HOUR_1
            )
        )
        val viewModel = LocationSharingSettingsViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.sharingEnabled)
        assertEquals(LocationSharingScope.NONE, state.scope)
        assertEquals(LocationSharingStartTime.HOUR_1, state.startTime)
    }

    @Test
    fun `화면을 열면 서버 값을 한 번 받아 온다`() = runTest(dispatcher) {
        val repository = FakeRepository()
        LocationSharingSettingsViewModel(repository)
        advanceUntilIdle()

        assertEquals(1, repository.refreshCalls)
    }

    @Test
    fun `서버 값을 받지 못해도 안내를 띄우지 않고 기기 값을 보여 준다`() = runTest(dispatcher) {
        val repository = FakeRepository().apply { refreshResult = LocationSharingSyncResult.Failure(retryable = true) }
        val viewModel = LocationSharingSettingsViewModel(repository)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.saveFailed)
        assertFalse(viewModel.uiState.value.loginRequired)
    }

    @Test
    fun `서버 값을 받을 때 로그인이 만료됐으면 로그인이 필요하다고 알린다`() = runTest(dispatcher) {
        val repository = FakeRepository().apply { refreshResult = LocationSharingSyncResult.LoginRequired }
        val viewModel = LocationSharingSettingsViewModel(repository)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.loginRequired)

        viewModel.consumeLoginRequired()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.loginRequired)
    }

    @Test
    fun `공유 사용을 바꾸면 저장하고 화면에 반영한다`() = runTest(dispatcher) {
        val repository = FakeRepository()
        val viewModel = LocationSharingSettingsViewModel(repository)
        advanceUntilIdle()

        viewModel.onSharingEnabledChange(false)
        advanceUntilIdle()

        assertFalse(repository.current.value.sharingEnabled)
        assertFalse(viewModel.uiState.value.sharingEnabled)
    }

    @Test
    fun `공유 범위를 바꾸면 저장하고 화면에 반영한다`() = runTest(dispatcher) {
        val repository = FakeRepository()
        val viewModel = LocationSharingSettingsViewModel(repository)
        advanceUntilIdle()

        viewModel.onScopeSelect(LocationSharingScope.NONE)
        advanceUntilIdle()

        assertEquals(LocationSharingScope.NONE, repository.current.value.scope)
        assertEquals(LocationSharingScope.NONE, viewModel.uiState.value.scope)
    }

    @Test
    fun `공유 시작 시간을 바꾸면 저장하고 화면에 반영한다`() = runTest(dispatcher) {
        val repository = FakeRepository()
        val viewModel = LocationSharingSettingsViewModel(repository)
        advanceUntilIdle()

        viewModel.onStartTimeSelect(LocationSharingStartTime.MINUTES_10)
        advanceUntilIdle()

        assertEquals(LocationSharingStartTime.MINUTES_10, repository.current.value.startTime)
        assertEquals(LocationSharingStartTime.MINUTES_10, viewModel.uiState.value.startTime)
    }

    @Test
    fun `한 항목을 바꿔도 다른 설정은 그대로다`() = runTest(dispatcher) {
        val repository = FakeRepository()
        val viewModel = LocationSharingSettingsViewModel(repository)
        advanceUntilIdle()

        viewModel.onStartTimeSelect(LocationSharingStartTime.HOUR_1)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.sharingEnabled)
        assertEquals(LocationSharingScope.FRIENDS_ONLY, state.scope)
    }

    @Test
    fun `서버에 저장하지 못하면 이전 값 그대로 두고 저장 실패를 알린다`() = runTest(dispatcher) {
        val repository = FakeRepository().apply { updateResult = LocationSharingSyncResult.Failure(retryable = true) }
        val viewModel = LocationSharingSettingsViewModel(repository)
        advanceUntilIdle()

        viewModel.onSharingEnabledChange(false)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.saveFailed)
        assertTrue(viewModel.uiState.value.sharingEnabled)

        viewModel.consumeSaveFailed()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.saveFailed)
    }

    @Test
    fun `저장할 때 로그인이 만료됐으면 로그인이 필요하다고 알린다`() = runTest(dispatcher) {
        val repository = FakeRepository().apply { updateResult = LocationSharingSyncResult.LoginRequired }
        val viewModel = LocationSharingSettingsViewModel(repository)
        advanceUntilIdle()

        viewModel.onScopeSelect(LocationSharingScope.NONE)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.loginRequired)
        assertFalse(viewModel.uiState.value.saveFailed)
    }

    @Test
    fun `위치 권한이 없는 채로 열면 허용 안 됨 상태로 시작한다`() = runTest(dispatcher) {
        val viewModel = LocationSharingSettingsViewModel(FakeRepository(), initialPermissionGranted = false)

        assertFalse(viewModel.uiState.value.locationPermissionGranted)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.locationPermissionGranted)
    }

    @Test
    fun `앱 설정에서 돌아와 권한을 다시 확인하면 상태가 바뀐다`() = runTest(dispatcher) {
        val viewModel = LocationSharingSettingsViewModel(FakeRepository(), initialPermissionGranted = false)
        advanceUntilIdle()

        viewModel.onPermissionChecked(true)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.locationPermissionGranted)

        viewModel.onPermissionChecked(false)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.locationPermissionGranted)
    }

    @Test
    fun `권한 상태가 바뀌어도 설정 값은 유지된다`() = runTest(dispatcher) {
        val repository = FakeRepository(LocationSharingSettings(scope = LocationSharingScope.NONE))
        val viewModel = LocationSharingSettingsViewModel(repository)
        advanceUntilIdle()

        viewModel.onPermissionChecked(false)
        advanceUntilIdle()

        assertEquals(LocationSharingScope.NONE, viewModel.uiState.value.scope)
    }
}
