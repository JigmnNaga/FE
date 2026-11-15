package com.jigmnnaga.presentation.appointment.manage

import com.jigmnnaga.domain.model.AppointmentAttendanceStatus
import com.jigmnnaga.domain.model.AppointmentDateSelectionType
import com.jigmnnaga.domain.model.AppointmentDetail
import com.jigmnnaga.domain.model.AppointmentManageRejectReason
import com.jigmnnaga.domain.model.AppointmentManageResult
import com.jigmnnaga.domain.model.AppointmentMember
import com.jigmnnaga.domain.model.AppointmentMemberRole
import com.jigmnnaga.domain.model.CandidateLocationChange
import com.jigmnnaga.domain.model.NewAppointmentDateType
import com.jigmnnaga.domain.model.ReminderSendSummary
import com.jigmnnaga.domain.repository.AppointmentManageRepository
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
class AppointmentManageLeaveViewModelTest {

    private class FakeManage : AppointmentManageRepository {
        var result: AppointmentManageResult<Unit> = AppointmentManageResult.Success(Unit)
        val calls = mutableListOf<String>()

        private fun record(call: String): AppointmentManageResult<Unit> {
            calls += call
            return result
        }

        override suspend fun updateTitle(appointmentId: Long, title: String) = record("title")
        override suspend fun updateCandidateDates(
            appointmentId: Long,
            dateType: NewAppointmentDateType,
            dates: List<String>
        ) = record("dates")

        override suspend fun updateCandidateLocations(appointmentId: Long, change: CandidateLocationChange) =
            record("locations")

        override suspend fun updateResponseDeadline(appointmentId: Long, deadline: String) = record("deadline")
        override suspend fun cancel(appointmentId: Long) = record("cancel:$appointmentId")
        override suspend fun delete(appointmentId: Long) = record("delete:$appointmentId")
        override suspend fun delegateHost(appointmentId: Long, newHostMemberId: Long) =
            record("host:$appointmentId:$newHostMemberId")

        override suspend fun kickParticipant(appointmentId: Long, targetMemberId: Long) = record("kick")
        override suspend fun leave(appointmentId: Long, newHostMemberId: Long?) =
            record("leave:$appointmentId:$newHostMemberId")

        override suspend fun updateNotificationSetting(appointmentId: Long, enabled: Boolean) = record("notification")
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

    private fun member(id: Long, nickname: String, role: AppointmentMemberRole) = AppointmentMember(
        memberId = id,
        nickname = nickname,
        profileImageUrl = null,
        role = role,
        scheduleSubmitted = false,
        attendanceStatus = AppointmentAttendanceStatus.PENDING,
        travelMode = null
    )

    /** 방장(1번)과 멤버를 [memberCount]명으로 맞춘 약속 상세. */
    private fun detail(memberCount: Int) = AppointmentDetail(
        appointmentId = 7L,
        title = "스터디",
        canceled = false,
        dateSelectionType = AppointmentDateSelectionType.RANGE,
        candidateDates = listOf("2026-11-20", "2026-11-21"),
        candidateLocations = emptyList(),
        responseDeadline = "2026-11-19T23:59:59",
        inviteUrl = null,
        confirmed = false,
        confirmedDate = null,
        confirmedTime = null,
        confirmedLocation = null,
        detailPlace = null,
        myRole = null,
        myAttendanceStatus = AppointmentAttendanceStatus.PENDING,
        myNotificationEnabled = false,
        myTravelMode = null,
        joinedCount = memberCount,
        scheduleSubmittedCount = 0,
        members = (1..memberCount).map {
            if (it == 1) member(1L, "방장", AppointmentMemberRole.HOST)
            else member(it.toLong(), "멤버$it", AppointmentMemberRole.MEMBER)
        }
    )

    private fun viewModel(memberCount: Int) = AppointmentManageViewModel(
        detail = detail(memberCount),
        repository = manage,
        onChanged = { changedCount++ }
    )

    @Test
    fun `방 인원으로 나가기 흐름이 정해진다`() {
        assertEquals(AppointmentManageLeaveMode.DELEGATE, viewModel(4).uiState.value.leaveMode)
        assertEquals(AppointmentManageLeaveMode.DELEGATE, viewModel(3).uiState.value.leaveMode)
        assertEquals(AppointmentManageLeaveMode.AUTO_DELEGATE, viewModel(2).uiState.value.leaveMode)
        assertEquals(AppointmentManageLeaveMode.DELETE_ONLY, viewModel(1).uiState.value.leaveMode)
    }

    @Test
    fun `3명 이상이면 새 방장을 골라야 나가기 버튼과 팝업이 열린다`() {
        val vm = viewModel(3)
        vm.openStep(AppointmentManageStep.LEAVE)

        assertEquals(AppointmentManageStep.LEAVE, vm.uiState.value.step)
        assertFalse(vm.uiState.value.canLeave())
        vm.requestLeave()
        assertFalse(vm.uiState.value.isLeaveDialogVisible)

        vm.selectNewHost(2L)

        assertEquals(2L, vm.uiState.value.newHost?.memberId)
        assertTrue(vm.uiState.value.canLeave())
        vm.requestLeave()
        assertTrue(vm.uiState.value.isLeaveDialogVisible)
        assertTrue(manage.calls.isEmpty())
    }

    @Test
    fun `방장 자신이나 멤버가 아닌 ID는 새 방장으로 고를 수 없다`() {
        val vm = viewModel(3)
        vm.openStep(AppointmentManageStep.LEAVE)

        vm.selectNewHost(1L)
        vm.selectNewHost(99L)

        assertNull(vm.uiState.value.selectedNewHostId)
        assertNull(vm.uiState.value.newHost)
    }

    @Test
    fun `나가기 화면이 아니면 새 방장을 고르거나 나가기를 요청해도 무시한다`() {
        val vm = viewModel(3)

        vm.selectNewHost(2L)
        vm.requestLeave()

        assertNull(vm.uiState.value.selectedNewHostId)
        assertFalse(vm.uiState.value.isLeaveDialogVisible)
    }

    @Test
    fun `3명 이상이면 고른 멤버를 새 방장으로 넘기고 나간 뒤 완료 화면으로 바뀐다`() = runTest(dispatcher) {
        val vm = viewModel(3)
        vm.openStep(AppointmentManageStep.LEAVE)
        vm.selectNewHost(3L)
        vm.requestLeave()

        vm.confirmLeave()
        runCurrent()

        assertEquals(listOf("leave:7:3"), manage.calls)
        assertEquals(AppointmentManageCompletion.LEFT, vm.uiState.value.completion)
        assertFalse(vm.uiState.value.isLeaveDialogVisible)
        assertFalse(vm.uiState.value.isWorking)
        assertEquals(1, changedCount)
    }

    @Test
    fun `2명이면 남은 멤버에게 자동으로 방장을 넘기고 나간다`() = runTest(dispatcher) {
        val vm = viewModel(2)
        vm.openStep(AppointmentManageStep.LEAVE)

        assertEquals(2L, vm.uiState.value.newHost?.memberId)
        assertTrue(vm.uiState.value.canLeave())
        vm.requestLeave()
        vm.confirmLeave()
        runCurrent()

        assertEquals(listOf("leave:7:2"), manage.calls)
        assertEquals(AppointmentManageCompletion.LEFT, vm.uiState.value.completion)
    }

    @Test
    fun `혼자 남았으면 나가지 않고 약속방을 삭제한다`() = runTest(dispatcher) {
        val vm = viewModel(1)
        vm.openStep(AppointmentManageStep.LEAVE)

        assertNull(vm.uiState.value.newHost)
        assertTrue(vm.uiState.value.canLeave())
        vm.requestLeave()
        vm.confirmLeave()
        runCurrent()

        assertEquals(listOf("delete:7"), manage.calls)
        assertEquals(AppointmentManageCompletion.DELETED, vm.uiState.value.completion)
        assertEquals(1, changedCount)
    }

    @Test
    fun `팝업을 닫으면 아무것도 보내지 않는다`() = runTest(dispatcher) {
        val vm = viewModel(3)
        vm.openStep(AppointmentManageStep.LEAVE)
        vm.selectNewHost(2L)
        vm.requestLeave()

        vm.dismissLeave()
        vm.confirmLeave()
        runCurrent()

        assertFalse(vm.uiState.value.isLeaveDialogVisible)
        assertTrue(manage.calls.isEmpty())
        assertNull(vm.uiState.value.completion)
        assertEquals(2L, vm.uiState.value.selectedNewHostId)
    }

    @Test
    fun `새 방장 미지정 거절은 완료 화면으로 바꾸지 않고 안내만 한다`() = runTest(dispatcher) {
        manage.result = AppointmentManageResult.Rejected(AppointmentManageRejectReason.NEW_HOST_REQUIRED)
        val vm = viewModel(3)
        vm.openStep(AppointmentManageStep.LEAVE)
        vm.selectNewHost(2L)
        vm.requestLeave()

        vm.confirmLeave()
        runCurrent()

        assertEquals(AppointmentManageNotice.LEAVE_NEW_HOST_REQUIRED, vm.notice.value)
        assertNull(vm.uiState.value.completion)
        assertEquals(2L, vm.uiState.value.selectedNewHostId)
        assertFalse(vm.uiState.value.isWorking)
        assertEquals(0, changedCount)
    }

    @Test
    fun `잘못된 대상 거절과 혼자 남은 방 거절을 각각 안내한다`() = runTest(dispatcher) {
        manage.result = AppointmentManageResult.Rejected(AppointmentManageRejectReason.INVALID_MEMBER)
        val vm = viewModel(3)
        vm.openStep(AppointmentManageStep.LEAVE)
        vm.selectNewHost(2L)
        vm.requestLeave()
        vm.confirmLeave()
        runCurrent()
        assertEquals(AppointmentManageNotice.LEAVE_INVALID_TARGET, vm.notice.value)

        manage.result = AppointmentManageResult.Rejected(AppointmentManageRejectReason.LAST_MEMBER_CANNOT_LEAVE)
        vm.clearNotice()
        vm.requestLeave()
        vm.confirmLeave()
        runCurrent()
        assertEquals(AppointmentManageNotice.LEAVE_LAST_MEMBER, vm.notice.value)
        assertNull(vm.uiState.value.completion)
    }

    @Test
    fun `방장이 아니면 방장 전용 안내를 보여 준다`() = runTest(dispatcher) {
        manage.result = AppointmentManageResult.NotHost
        val vm = viewModel(2)
        vm.openStep(AppointmentManageStep.LEAVE)
        vm.requestLeave()

        vm.confirmLeave()
        runCurrent()

        assertEquals(AppointmentManageNotice.NOT_HOST, vm.notice.value)
        assertNull(vm.uiState.value.completion)
    }

    @Test
    fun `로그인이 필요하면 로그인 뒤 같은 새 방장으로 이어서 나간다`() = runTest(dispatcher) {
        manage.result = AppointmentManageResult.LoginRequired
        val vm = viewModel(3)
        vm.openStep(AppointmentManageStep.LEAVE)
        vm.selectNewHost(2L)
        vm.requestLeave()
        vm.confirmLeave()
        runCurrent()
        assertEquals(AppointmentManageNotice.LOGIN_REQUIRED, vm.notice.value)
        assertNull(vm.uiState.value.completion)

        manage.result = AppointmentManageResult.Success(Unit)
        vm.clearNotice()
        vm.resumeAfterLogin()
        runCurrent()

        assertEquals(listOf("leave:7:2", "leave:7:2"), manage.calls)
        assertEquals(AppointmentManageCompletion.LEFT, vm.uiState.value.completion)
    }

    @Test
    fun `네트워크 오류는 재시도 안내를 하고 선택을 그대로 둔다`() = runTest(dispatcher) {
        manage.result = AppointmentManageResult.Failure(retryable = true)
        val vm = viewModel(3)
        vm.openStep(AppointmentManageStep.LEAVE)
        vm.selectNewHost(3L)
        vm.requestLeave()

        vm.confirmLeave()
        runCurrent()

        assertEquals(AppointmentManageNotice.FAILED_RETRY, vm.notice.value)
        assertEquals(3L, vm.uiState.value.selectedNewHostId)
        assertNull(vm.uiState.value.completion)
    }

    @Test
    fun `뒤로 가면 관리 목록으로 돌아가고 선택과 팝업을 지운다`() {
        val vm = viewModel(3)
        vm.openStep(AppointmentManageStep.LEAVE)
        vm.selectNewHost(2L)
        vm.requestLeave()

        assertTrue(vm.back())

        val state = vm.uiState.value
        assertEquals(AppointmentManageStep.MENU, state.step)
        assertNull(state.selectedNewHostId)
        assertFalse(state.isLeaveDialogVisible)
    }

    @Test
    fun `완료 화면에서는 뒤로 가기를 처리하지 않고 다시 나가기도 막는다`() = runTest(dispatcher) {
        val vm = viewModel(2)
        vm.openStep(AppointmentManageStep.LEAVE)
        vm.requestLeave()
        vm.confirmLeave()
        runCurrent()

        assertFalse(vm.back())
        assertFalse(vm.uiState.value.canLeave())
        vm.requestLeave()
        assertFalse(vm.uiState.value.isLeaveDialogVisible)
        assertEquals(1, manage.calls.size)
    }

    @Test
    fun `나가기 화면에서는 저장 버튼을 쓰지 않는다`() {
        val vm = viewModel(3)
        vm.openStep(AppointmentManageStep.LEAVE)

        assertFalse(vm.uiState.value.canSave())
    }
}
