package com.jigmnnaga.presentation.appointment.participant

import com.jigmnnaga.domain.model.AppointmentManageRejectReason
import com.jigmnnaga.domain.model.AppointmentManageResult
import com.jigmnnaga.domain.model.CandidateLocationChange
import com.jigmnnaga.domain.model.NewAppointmentDateType
import com.jigmnnaga.domain.model.ReminderSendSummary
import com.jigmnnaga.domain.repository.AppointmentManageRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ParticipantAppointmentManagementViewModelTest {

    private class FakeManage : AppointmentManageRepository {
        var result: AppointmentManageResult<Unit> = AppointmentManageResult.Success(Unit)
        var gate: CompletableDeferred<Unit>? = null
        val calls = mutableListOf<String>()

        private suspend fun record(call: String): AppointmentManageResult<Unit> {
            calls += call
            gate?.await()
            return result
        }

        override suspend fun leave(appointmentId: Long, newHostMemberId: Long?) =
            record("leave:$appointmentId:$newHostMemberId")

        override suspend fun updateNotificationSetting(appointmentId: Long, enabled: Boolean) =
            record("notification:$appointmentId:$enabled")

        override suspend fun updateTitle(appointmentId: Long, title: String) = record("title")
        override suspend fun updateCandidateDates(
            appointmentId: Long,
            dateType: NewAppointmentDateType,
            dates: List<String>
        ) = record("dates")

        override suspend fun updateCandidateLocations(appointmentId: Long, change: CandidateLocationChange) =
            record("locations")

        override suspend fun updateResponseDeadline(appointmentId: Long, deadline: String) = record("deadline")
        override suspend fun cancel(appointmentId: Long) = record("cancel")
        override suspend fun delete(appointmentId: Long) = record("delete")
        override suspend fun delegateHost(appointmentId: Long, newHostMemberId: Long) = record("host")
        override suspend fun kickParticipant(appointmentId: Long, targetMemberId: Long) = record("kick")
        override suspend fun sendReminders(appointmentId: Long): AppointmentManageResult<ReminderSendSummary> =
            AppointmentManageResult.Success(ReminderSendSummary(0, emptyList()))
    }

    private val dispatcher = StandardTestDispatcher()
    private val manage = FakeManage()
    private var changedCount = 0

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(notificationsEnabled: Boolean = true) = ParticipantAppointmentManagementViewModel(
        appointmentId = 7L,
        initialState = ParticipantAppointmentManagementUiState(
            appointmentName = "스터디",
            scheduleStatusLabel = "확정 전",
            isNotificationsEnabled = notificationsEnabled
        ),
        repository = manage,
        onChanged = { changedCount++ }
    )

    @Test
    fun `알림을 끄면 먼저 화면에 반영하고 서버에 보낸다`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>().also { manage.gate = it }
        val vm = viewModel(notificationsEnabled = true)

        vm.onNotificationsEnabledChange(false)
        runCurrent()

        assertFalse(vm.uiState.value.management.isNotificationsEnabled)
        assertTrue(vm.uiState.value.isWorking)
        assertEquals(listOf("notification:7:false"), manage.calls)

        gate.complete(Unit)
        runCurrent()

        assertFalse(vm.uiState.value.isWorking)
        assertFalse(vm.uiState.value.management.isNotificationsEnabled)
        assertNull(vm.notice.value)
    }

    @Test
    fun `알림 설정이 같은 값이거나 요청 중이면 다시 보내지 않는다`() = runTest(dispatcher) {
        manage.gate = CompletableDeferred()
        val vm = viewModel(notificationsEnabled = true)

        vm.onNotificationsEnabledChange(true)
        runCurrent()
        assertTrue(manage.calls.isEmpty())

        vm.onNotificationsEnabledChange(false)
        runCurrent()
        vm.onNotificationsEnabledChange(true)
        runCurrent()

        assertEquals(listOf("notification:7:false"), manage.calls)
    }

    @Test
    fun `알림 설정이 실패하면 이전 값으로 되돌리고 실패를 알린다`() = runTest(dispatcher) {
        manage.result = AppointmentManageResult.Failure(retryable = true)
        val vm = viewModel(notificationsEnabled = true)

        vm.onNotificationsEnabledChange(false)
        runCurrent()

        assertTrue(vm.uiState.value.management.isNotificationsEnabled)
        assertEquals(ParticipantManagementNotice.FAILED_RETRY, vm.notice.value)
        assertFalse(vm.uiState.value.isWorking)
    }

    @Test
    fun `알림 설정이 로그인이 필요하면 되돌린 뒤 로그인하고 같은 값을 다시 보낸다`() = runTest(dispatcher) {
        manage.result = AppointmentManageResult.LoginRequired
        val vm = viewModel(notificationsEnabled = true)
        vm.onNotificationsEnabledChange(false)
        runCurrent()

        assertTrue(vm.uiState.value.management.isNotificationsEnabled)
        assertEquals(ParticipantManagementNotice.LOGIN_REQUIRED, vm.notice.value)

        manage.result = AppointmentManageResult.Success(Unit)
        vm.resumeAfterLogin()
        runCurrent()

        assertEquals(listOf("notification:7:false", "notification:7:false"), manage.calls)
        assertFalse(vm.uiState.value.management.isNotificationsEnabled)
    }

    @Test
    fun `나가기는 확인 팝업에서 확인한 뒤에만 서버에 보내고 성공하면 나갔다고 알린다`() = runTest(dispatcher) {
        val vm = viewModel()

        vm.requestLeave()
        assertTrue(vm.uiState.value.isLeaveDialogVisible)
        assertTrue(manage.calls.isEmpty())

        vm.confirmLeave()
        runCurrent()

        assertEquals(listOf("leave:7:null"), manage.calls)
        assertFalse(vm.uiState.value.isLeaveDialogVisible)
        assertTrue(vm.uiState.value.hasLeft)
        assertEquals(1, changedCount)
    }

    @Test
    fun `나가기 팝업을 닫으면 아무것도 보내지 않는다`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.requestLeave()

        vm.dismissLeave()
        vm.confirmLeave()
        runCurrent()

        assertFalse(vm.uiState.value.isLeaveDialogVisible)
        assertTrue(manage.calls.isEmpty())
        assertFalse(vm.uiState.value.hasLeft)
    }

    @Test
    fun `확인 팝업 없이는 나가지 않는다`() = runTest(dispatcher) {
        val vm = viewModel()

        vm.confirmLeave()
        runCurrent()

        assertTrue(manage.calls.isEmpty())
    }

    @Test
    fun `나가기가 로그인이 필요하면 로그인 뒤 확인 없이 이어서 나간다`() = runTest(dispatcher) {
        manage.result = AppointmentManageResult.LoginRequired
        val vm = viewModel()
        vm.requestLeave()
        vm.confirmLeave()
        runCurrent()

        assertEquals(ParticipantManagementNotice.LOGIN_REQUIRED, vm.notice.value)
        assertFalse(vm.uiState.value.hasLeft)

        manage.result = AppointmentManageResult.Success(Unit)
        vm.resumeAfterLogin()
        runCurrent()

        assertEquals(listOf("leave:7:null", "leave:7:null"), manage.calls)
        assertTrue(vm.uiState.value.hasLeft)
    }

    @Test
    fun `나가기가 거절되면 이유별로 안내하고 나가지 않는다`() = runTest(dispatcher) {
        val vm = viewModel()

        manage.result = AppointmentManageResult.Rejected(AppointmentManageRejectReason.LAST_MEMBER_CANNOT_LEAVE)
        vm.requestLeave(); vm.confirmLeave(); runCurrent()
        assertEquals(ParticipantManagementNotice.LAST_MEMBER, vm.notice.value)
        vm.clearNotice()

        manage.result = AppointmentManageResult.Rejected(AppointmentManageRejectReason.NEW_HOST_REQUIRED)
        vm.requestLeave(); vm.confirmLeave(); runCurrent()
        assertEquals(ParticipantManagementNotice.HOST_MUST_DELEGATE, vm.notice.value)
        vm.clearNotice()

        manage.result = AppointmentManageResult.NotHost
        vm.requestLeave(); vm.confirmLeave(); runCurrent()
        assertEquals(ParticipantManagementNotice.NOT_PARTICIPANT, vm.notice.value)

        assertFalse(vm.uiState.value.hasLeft)
        assertEquals(0, changedCount)
    }

    @Test
    fun `나가기 요청 중에는 다시 나가거나 알림 설정을 바꾸지 않는다`() = runTest(dispatcher) {
        manage.gate = CompletableDeferred()
        val vm = viewModel()
        vm.requestLeave()
        vm.confirmLeave()
        runCurrent()

        vm.requestLeave()
        vm.onNotificationsEnabledChange(false)
        runCurrent()

        assertEquals(listOf("leave:7:null"), manage.calls)
        assertFalse(vm.uiState.value.isLeaveDialogVisible)
        assertTrue(vm.uiState.value.management.isNotificationsEnabled)
    }
}
