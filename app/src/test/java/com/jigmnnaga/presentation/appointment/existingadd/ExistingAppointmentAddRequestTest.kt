package com.jigmnnaga.presentation.appointment.existingadd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExistingAppointmentAddRequestTest {

    private fun state(ids: List<String> = listOf("10", "11", "12", "13")) = ExistingAppointmentAddUiState(
        page = ExistingAppointmentAddPage.RESCHEDULE,
        appointmentTitle = "중간고사 카공 모임",
        memberSummary = "4명",
        scheduledTime = "오후 3:05",
        scheduledPlace = "강남역",
        members = ids.mapIndexed { index, id ->
            ExistingAppointmentMemberUiState(
                id = id,
                name = "멤버 $index",
                statusLabel = "도착",
                isMe = index == 0,
                isSelectable = index != 0
            )
        }
    )

    private fun requestPage(appointmentId: Long? = 5L, ids: List<String> = listOf("10", "11", "12", "13")) =
        ExistingAppointmentAddViewModel(state(ids), appointmentId).also { it.onRescheduleClick() }

    @Test
    fun `요청하기를 누르면 고른 참여자를 약속 ID와 함께 내보내고 선택은 유지한다`() {
        val vm = requestPage()
        vm.onMemberSelectionChange("12", true)
        vm.onMemberSelectionChange("11", true)

        vm.onRequestClick()

        assertEquals(PreviousMembersRequest(5L, listOf(11L, 12L)), vm.requestedMembers.value)
        assertEquals(setOf("11", "12"), vm.uiState.value.selectedMemberIds)
        assertNull(vm.notice.value)
    }

    @Test
    fun `아무도 고르지 않으면 요청하지 않는다`() {
        val vm = requestPage()

        vm.onRequestClick()

        assertNull(vm.requestedMembers.value)
        assertNull(vm.notice.value)
    }

    @Test
    fun `본인은 고를 수 없어 요청에 들어가지 않는다`() {
        val vm = requestPage()
        vm.onMemberSelectionChange("10", true)
        vm.onMemberSelectionChange("13", true)

        vm.onRequestClick()

        assertEquals(PreviousMembersRequest(5L, listOf(13L)), vm.requestedMembers.value)
    }

    @Test
    fun `이동한 뒤 비우면 다시 요청할 수 있다`() {
        val vm = requestPage()
        vm.onMemberSelectionChange("11", true)
        vm.onRequestClick()

        vm.consumeRequestedMembers()
        assertNull(vm.requestedMembers.value)

        vm.onRequestClick()
        assertEquals(PreviousMembersRequest(5L, listOf(11L)), vm.requestedMembers.value)
    }

    @Test
    fun `멤버 ID를 숫자로 바꿀 수 없으면 요청하지 않고 안내한다`() {
        val vm = requestPage(ids = listOf("me", "a", "b"))
        vm.onMemberSelectionChange("a", true)

        vm.onRequestClick()

        assertNull(vm.requestedMembers.value)
        assertEquals(ExistingAppointmentAddNotice.REQUEST_MEMBERS_INVALID, vm.notice.value)
        assertEquals(setOf("a"), vm.uiState.value.selectedMemberIds)
    }

    @Test
    fun `일부만 숫자가 아니면 숫자인 멤버만 내보낸다`() {
        val vm = requestPage(ids = listOf("10", "11", "x"))
        vm.onMemberSelectionChange("11", true)
        vm.onMemberSelectionChange("x", true)

        vm.onRequestClick()

        assertEquals(PreviousMembersRequest(5L, listOf(11L)), vm.requestedMembers.value)
    }

    @Test
    fun `약속 ID가 없으면 이전처럼 안내만 보낸다`() {
        val vm = requestPage(appointmentId = null)
        vm.onMemberSelectionChange("11", true)

        vm.onRequestClick()

        assertNull(vm.requestedMembers.value)
        assertEquals(ExistingAppointmentAddNotice.REQUEST_API_NOT_CONNECTED, vm.notice.value)
        assertTrue(vm.uiState.value.selectedMemberIds.contains("11"))
    }
}
