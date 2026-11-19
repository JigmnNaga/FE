package com.jigmnnaga.presentation.mypage.sharing

import com.jigmnnaga.domain.model.LocationSharingScope
import com.jigmnnaga.domain.model.LocationSharingSettings
import com.jigmnnaga.domain.model.LocationSharingStartTime
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

        override suspend fun setSharingEnabled(enabled: Boolean) {
            current.update { it.copy(sharingEnabled = enabled) }
        }

        override suspend fun setScope(scope: LocationSharingScope) {
            current.update { it.copy(scope = scope) }
        }

        override suspend fun setStartTime(startTime: LocationSharingStartTime) {
            current.update { it.copy(startTime = startTime) }
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
