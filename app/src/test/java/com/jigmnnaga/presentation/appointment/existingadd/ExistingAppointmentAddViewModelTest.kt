package com.jigmnnaga.presentation.appointment.existingadd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExistingAppointmentAddViewModelTest {

    private fun state(
        page: ExistingAppointmentAddPage = ExistingAppointmentAddPage.RESCHEDULE
    ) = ExistingAppointmentAddUiState(
        page = page,
        appointmentTitle = "중간고사 카공 모임",
        memberSummary = "3명",
        scheduledTime = "오후 3:05",
        scheduledPlace = "강남역",
        members = listOf(
            ExistingAppointmentMemberUiState("me", "나", "도착", isMe = true, isSelectable = false),
            ExistingAppointmentMemberUiState("a", "태양", "도착"),
            ExistingAppointmentMemberUiState("b", "하은", "도착")
        )
    )

    private fun requestPage() = ExistingAppointmentAddViewModel(state()).also { it.onRescheduleClick() }

    @Test
    fun `처음에는 다시 약속 잡기 페이지이고 요청할 수 없다`() {
        val vm = ExistingAppointmentAddViewModel(state())
        assertEquals(ExistingAppointmentAddPage.RESCHEDULE, vm.uiState.value.page)
        assertFalse(vm.uiState.value.canRequest)
    }

    @Test
    fun `다시 약속 잡기를 누르면 참여자 선택 페이지로 넘어간다`() {
        val vm = requestPage()
        assertEquals(ExistingAppointmentAddPage.REQUEST_PARTICIPANTS, vm.uiState.value.page)
        assertTrue(vm.uiState.value.selectedMemberIds.isEmpty())
        assertFalse(vm.uiState.value.canRequest)
    }

    @Test
    fun `멤버를 한 명 이상 고르면 요청할 수 있다`() {
        val vm = requestPage()
        vm.onMemberSelectionChange("a", true)
        assertEquals(setOf("a"), vm.uiState.value.selectedMemberIds)
        assertTrue(vm.uiState.value.canRequest)
    }

    @Test
    fun `모두 해제하면 요청할 수 없다`() {
        val vm = requestPage()
        vm.onMemberSelectionChange("a", true)
        vm.onMemberSelectionChange("a", false)
        assertTrue(vm.uiState.value.selectedMemberIds.isEmpty())
        assertFalse(vm.uiState.value.canRequest)
    }

    @Test
    fun `고를 수 없는 멤버와 없는 멤버는 선택되지 않는다`() {
        val vm = requestPage()
        vm.onMemberSelectionChange("me", true)
        vm.onMemberSelectionChange("none", true)
        assertTrue(vm.uiState.value.selectedMemberIds.isEmpty())
    }

    @Test
    fun `다시 약속 잡기 페이지에서는 멤버를 고를 수 없다`() {
        val vm = ExistingAppointmentAddViewModel(state())
        vm.onMemberSelectionChange("a", true)
        assertTrue(vm.uiState.value.selectedMemberIds.isEmpty())
    }

    @Test
    fun `요청하면 안내만 나오고 선택은 유지된다`() {
        val vm = requestPage()
        vm.onMemberSelectionChange("a", true)
        vm.onRequestClick()
        assertEquals(ExistingAppointmentAddNotice.REQUEST_API_NOT_CONNECTED, vm.notice.value)
        assertEquals(setOf("a"), vm.uiState.value.selectedMemberIds)
        assertEquals(ExistingAppointmentAddPage.REQUEST_PARTICIPANTS, vm.uiState.value.page)
    }

    @Test
    fun `고른 멤버가 없으면 요청해도 안내가 나오지 않는다`() {
        val vm = requestPage()
        vm.onRequestClick()
        assertNull(vm.notice.value)
    }

    @Test
    fun `안내는 지우면 사라진다`() {
        val vm = requestPage()
        vm.onMemberSelectionChange("a", true)
        vm.onRequestClick()
        vm.clearNotice()
        assertNull(vm.notice.value)
    }

    @Test
    fun `채팅 탭을 누르면 탭은 그대로이고 안내만 나온다`() {
        val vm = ExistingAppointmentAddViewModel(state())
        vm.onChatTabClick()
        assertEquals(ExistingAppointmentAddNotice.CHAT_NOT_CONNECTED, vm.notice.value)
        assertEquals(ExistingAppointmentRoomTab.MAP_AND_STATUS, vm.uiState.value.selectedRoomTab)
    }

    @Test
    fun `참여자 선택 페이지에서 뒤로 가면 이전 페이지로 돌아가고 선택이 비워진다`() {
        val vm = requestPage()
        vm.onMemberSelectionChange("a", true)
        assertTrue(vm.onBackClick())
        assertEquals(ExistingAppointmentAddPage.RESCHEDULE, vm.uiState.value.page)
        assertTrue(vm.uiState.value.selectedMemberIds.isEmpty())
        assertFalse(vm.uiState.value.canRequest)
    }

    @Test
    fun `첫 페이지에서 뒤로 가면 처리하지 않는다`() {
        val vm = ExistingAppointmentAddViewModel(state())
        assertFalse(vm.onBackClick())
    }
}
