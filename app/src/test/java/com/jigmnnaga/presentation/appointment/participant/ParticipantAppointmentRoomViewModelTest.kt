package com.jigmnnaga.presentation.appointment.participant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ParticipantAppointmentRoomViewModelTest {

    private fun state(canReportDeparture: Boolean = false) = ParticipantAppointmentRoomUiState(
        title = "저녁 약속",
        subtitle = "참여자 3명",
        appointmentStatus = ParticipantAppointmentStatus.AWAITING_CONFIRMATION,
        canReportDeparture = canReportDeparture
    )

    private fun viewModel(canReportDeparture: Boolean = false) =
        ParticipantAppointmentRoomViewModel(state(canReportDeparture))

    @Test
    fun `초기 상태는 채팅 탭이고 요약 카드는 접혀 있다`() {
        val ui = viewModel().uiState.value

        assertEquals(ParticipantAppointmentRoomTab.CHAT, ui.selectedTab)
        assertFalse(ui.isSummaryExpanded)
        assertEquals("", ui.draftMessage)
    }

    @Test
    fun `탭을 바꾸면 선택한 탭이 반영된다`() {
        val vm = viewModel()

        vm.onTabSelected(ParticipantAppointmentRoomTab.MAP_STATUS)
        assertEquals(ParticipantAppointmentRoomTab.MAP_STATUS, vm.uiState.value.selectedTab)

        vm.onTabSelected(ParticipantAppointmentRoomTab.CHAT)
        assertEquals(ParticipantAppointmentRoomTab.CHAT, vm.uiState.value.selectedTab)
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

        assertEquals(ParticipantAppointmentRoomNotice.CHAT_API_NOT_CONNECTED, vm.notice.value)
        assertEquals("안녕하세요", vm.uiState.value.draftMessage)
        assertTrue(vm.uiState.value.messages.isEmpty())
    }

    @Test
    fun `출발 버튼은 누를 수 있을 때만 서버 미연동 안내를 띄운다`() {
        val disabled = viewModel(canReportDeparture = false)
        disabled.onDepartureClick()
        assertNull(disabled.notice.value)

        val enabled = viewModel(canReportDeparture = true)
        enabled.onDepartureClick()
        assertEquals(ParticipantAppointmentRoomNotice.DEPARTURE_API_NOT_CONNECTED, enabled.notice.value)
    }

    @Test
    fun `안내를 지우면 다시 비어 있다`() {
        val vm = viewModel(canReportDeparture = true)
        vm.onDepartureClick()

        vm.clearNotice()

        assertNull(vm.notice.value)
    }

    @Test
    fun `약속 정보를 갱신해도 탭과 입력값과 펼침 상태는 유지된다`() {
        val vm = viewModel()
        vm.onTabSelected(ParticipantAppointmentRoomTab.MAP_STATUS)
        vm.onMessageChange("작성 중")
        vm.onSummaryExpandedChange(true)

        vm.onRoomInfoChanged(
            state(canReportDeparture = true).copy(
                title = "점심 약속",
                appointmentStatus = ParticipantAppointmentStatus.CONFIRMED
            )
        )

        val ui = vm.uiState.value
        assertEquals("점심 약속", ui.title)
        assertEquals(ParticipantAppointmentStatus.CONFIRMED, ui.appointmentStatus)
        assertTrue(ui.canReportDeparture)
        assertEquals(ParticipantAppointmentRoomTab.MAP_STATUS, ui.selectedTab)
        assertEquals("작성 중", ui.draftMessage)
        assertTrue(ui.isSummaryExpanded)
    }
}
