package com.jigmnnaga.presentation.appointment.hostconfirmation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HostConfirmationViewModelTest {

    private fun state(
        selectedDateTime: String? = null,
        selectedPlace: String? = null
    ) = HostConfirmationUiState(
        appointmentTitle = "저녁 약속",
        selectedDateTime = selectedDateTime,
        selectedPlace = selectedPlace,
        dateTimeChoices = listOf(
            HostConfirmationChoice(id = "t1", title = "11월 20일 19:00"),
            HostConfirmationChoice(id = "t2", title = "11월 21일 18:30")
        ),
        placeChoices = listOf(
            HostConfirmationChoice(id = "p1", title = "강남역", supportingText = "2번 출구"),
            HostConfirmationChoice(id = "p2", title = "홍대입구")
        ),
        detailLocationChoices = listOf(
            HostConfirmationChoice(id = "d1", title = "1층 입구")
        )
    )

    @Test
    fun `초기 상태는 확인 단계이고 선택값이 없으면 확정할 수 없다`() {
        val ui = HostConfirmationViewModel(state()).uiState.value

        assertEquals(HostConfirmationStep.REVIEW, ui.step)
        assertFalse(ui.canConfirm)
    }

    @Test
    fun `날짜 시간과 장소가 이미 정해져 있으면 처음부터 확정할 수 있다`() {
        val ui = HostConfirmationViewModel(state("11월 20일 19:00", "강남역")).uiState.value

        assertTrue(ui.canConfirm)
    }

    @Test
    fun `날짜 시간 변경을 누르면 선택 단계로 가고 후보를 고르면 확인 단계로 돌아온다`() {
        val vm = HostConfirmationViewModel(state())

        vm.onEditDateTime()
        assertEquals(HostConfirmationStep.DATE_TIME, vm.uiState.value.step)

        vm.onSelectDateTime("t2")
        val ui = vm.uiState.value
        assertEquals(HostConfirmationStep.REVIEW, ui.step)
        assertEquals("11월 21일 18:30", ui.selectedDateTime)
        assertEquals(listOf(false, true), ui.dateTimeChoices.map { it.selected })
    }

    @Test
    fun `장소를 고르면 장소와 설명이 반영된다`() {
        val vm = HostConfirmationViewModel(state())

        vm.onEditPlace()
        vm.onSelectPlace("p1")

        val ui = vm.uiState.value
        assertEquals("강남역", ui.selectedPlace)
        assertEquals("2번 출구", ui.selectedPlaceDescription)
        assertEquals(HostConfirmationStep.REVIEW, ui.step)
    }

    @Test
    fun `날짜 시간과 장소를 모두 골라야 확정 버튼이 활성화된다`() {
        val vm = HostConfirmationViewModel(state())

        vm.onSelectDateTime("t1")
        assertFalse(vm.uiState.value.canConfirm)

        vm.onSelectPlace("p2")
        assertTrue(vm.uiState.value.canConfirm)
    }

    @Test
    fun `없는 후보를 고르면 상태가 바뀌지 않는다`() {
        val vm = HostConfirmationViewModel(state())
        val before = vm.uiState.value

        vm.onSelectDateTime("none")
        vm.onSelectPlace("none")
        vm.onSelectDetailLocation("none")

        assertEquals(before, vm.uiState.value)
    }

    @Test
    fun `세부 위치를 고르거나 나중에 정하기를 누를 수 있다`() {
        val vm = HostConfirmationViewModel(state())

        vm.onOpenDetailLocation()
        assertEquals(HostConfirmationStep.DETAIL_LOCATION, vm.uiState.value.step)
        vm.onSelectDetailLocation("d1")
        assertEquals("1층 입구", vm.uiState.value.selectedDetailLocation)

        vm.onOpenDetailLocation()
        vm.onChooseDetailLater()
        val ui = vm.uiState.value
        assertNull(ui.selectedDetailLocation)
        assertEquals(HostConfirmationStep.REVIEW, ui.step)
        assertTrue(ui.detailLocationChoices.none { it.selected })
    }

    @Test
    fun `뒤로 가기는 선택 단계에서만 확인 단계로 돌아가고 확인 단계에서는 false를 준다`() {
        val vm = HostConfirmationViewModel(state())

        vm.onEditPlace()
        assertTrue(vm.onBack())
        assertEquals(HostConfirmationStep.REVIEW, vm.uiState.value.step)

        assertFalse(vm.onBack())
    }

    @Test
    fun `확정을 누르면 서버로 보내지 않고 안내만 띄우며 선택값은 유지된다`() {
        val vm = HostConfirmationViewModel(state("11월 20일 19:00", "강남역"))

        vm.onConfirm()

        assertEquals(HostConfirmationNotice.CONFIRM_API_NOT_CONNECTED, vm.notice.value)
        val ui = vm.uiState.value
        assertEquals(HostConfirmationStep.REVIEW, ui.step)
        assertEquals("11월 20일 19:00", ui.selectedDateTime)
        assertEquals("강남역", ui.selectedPlace)

        vm.clearNotice()
        assertNull(vm.notice.value)
    }

    @Test
    fun `조건이 안 맞으면 확정해도 안내가 뜨지 않는다`() {
        val vm = HostConfirmationViewModel(state(selectedDateTime = "11월 20일 19:00"))

        vm.onConfirm()

        assertNull(vm.notice.value)
    }

    @Test
    fun `방장이 아니면 선택을 마쳐도 확정할 수 없다`() {
        val vm = HostConfirmationViewModel(state("11월 20일 19:00", "강남역"), isHost = false)

        assertFalse(vm.uiState.value.canConfirm)
        vm.onConfirm()
        assertNull(vm.notice.value)
    }

    @Test
    fun `완료 단계에서는 단계 이동 요청을 무시한다`() {
        val vm = HostConfirmationViewModel(
            state("11월 20일 19:00", "강남역").copy(step = HostConfirmationStep.COMPLETE)
        )

        vm.onEditPlace()

        assertEquals(HostConfirmationStep.COMPLETE, vm.uiState.value.step)
    }
}
