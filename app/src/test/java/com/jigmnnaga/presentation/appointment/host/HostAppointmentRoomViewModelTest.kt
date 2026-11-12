package com.jigmnnaga.presentation.appointment.host

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HostAppointmentRoomViewModelTest {

    private fun state() = HostAppointmentRoomUiState(
        title = "저녁 약속",
        subtitle = "참여자 3명"
    )

    private fun viewModel() = HostAppointmentRoomViewModel(state())

    @Test
    fun `초기 상태는 채팅 탭이고 요약 카드는 접혀 있다`() {
        val ui = viewModel().uiState.value

        assertEquals(HostAppointmentRoomTab.CHAT, ui.selectedTab)
        assertFalse(ui.isSummaryExpanded)
        assertEquals("", ui.draftMessage)
    }

    @Test
    fun `탭을 바꾸면 선택한 탭이 반영된다`() {
        val vm = viewModel()

        vm.onTabSelected(HostAppointmentRoomTab.MAP_STATUS)
        assertEquals(HostAppointmentRoomTab.MAP_STATUS, vm.uiState.value.selectedTab)

        vm.onTabSelected(HostAppointmentRoomTab.CHAT)
        assertEquals(HostAppointmentRoomTab.CHAT, vm.uiState.value.selectedTab)
    }

    @Test
    fun `요약 카드 펼침 상태를 바꾼다`() {
        val vm = viewModel()

        vm.onSummaryExpandedChange(true)
        assertTrue(vm.uiState.value.isSummaryExpanded)

        vm.onSummaryExpandedChange(false)
        assertFalse(vm.uiState.value.isSummaryExpanded)
    }

    @Test
    fun `메시지 입력값이 상태에 반영된다`() {
        val vm = viewModel()

        vm.onMessageChange("안녕하세요")

        assertEquals("안녕하세요", vm.uiState.value.draftMessage)
    }

    @Test
    fun `공백만 입력하고 전송하면 안내 없이 무시한다`() {
        val vm = viewModel()
        vm.onMessageChange("   ")

        vm.onSendMessage()

        assertNull(vm.notice.value)
        assertTrue(vm.uiState.value.messages.isEmpty())
    }

    @Test
    fun `메시지를 전송하면 서버 미연동 안내가 나오고 입력값과 메시지 목록은 그대로다`() {
        val vm = viewModel()
        vm.onMessageChange("안녕하세요")

        vm.onSendMessage()

        assertEquals(HostAppointmentRoomNotice.CHAT_API_NOT_CONNECTED, vm.notice.value)
        assertEquals("안녕하세요", vm.uiState.value.draftMessage)
        assertTrue(vm.uiState.value.messages.isEmpty())
    }

    @Test
    fun `안내를 지우면 다시 비어 있다`() {
        val vm = viewModel()
        vm.onMessageChange("안녕하세요")
        vm.onSendMessage()

        vm.clearNotice()

        assertNull(vm.notice.value)
    }

    @Test
    fun `약속 정보를 갱신해도 탭과 입력값과 펼침 상태는 유지된다`() {
        val vm = viewModel()
        vm.onTabSelected(HostAppointmentRoomTab.MAP_STATUS)
        vm.onMessageChange("작성 중")
        vm.onSummaryExpandedChange(true)

        vm.onRoomInfoChanged(state().copy(title = "점심 약속", participantCount = 4))

        val ui = vm.uiState.value
        assertEquals("점심 약속", ui.title)
        assertEquals(4, ui.participantCount)
        assertEquals(HostAppointmentRoomTab.MAP_STATUS, ui.selectedTab)
        assertEquals("작성 중", ui.draftMessage)
        assertTrue(ui.isSummaryExpanded)
    }
}
