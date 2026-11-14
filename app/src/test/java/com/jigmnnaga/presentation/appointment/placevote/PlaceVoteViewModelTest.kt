package com.jigmnnaga.presentation.appointment.placevote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaceVoteViewModelTest {

    private fun candidate(id: String, name: String = id, address: String? = null) =
        PlaceCandidateUiModel(id = id, name = name, address = address)

    private fun template(
        candidates: List<PlaceCandidateUiModel> = listOf(
            candidate("gangnam", "강남역", "서울특별시 강남구"),
            candidate("jamsil", "잠실역", "서울특별시 송파구"),
            candidate("hongdae", "홍대입구역", "서울특별시 마포구")
        ),
        selected: Set<String> = emptySet(),
        withSummary: Boolean = false
    ) = PlaceVoteUiState(
        role = PlaceVoteRole.HOST,
        searchQuery = "",
        candidates = candidates,
        selectedCandidateIds = selected,
        title = "일정 조율",
        question = "어디서 만날까요?",
        submitLabel = "저장",
        showSearchInput = true,
        selectedSummaryTitle = if (withSummary) "" else null
    )

    @Test
    fun `처음에는 넘겨 받은 후보와 선택값을 그대로 보여 준다`() {
        val vm = PlaceVoteViewModel(template(selected = setOf("gangnam")))
        assertEquals(3, vm.uiState.value.candidates.size)
        assertEquals(setOf("gangnam"), vm.uiState.value.selectedCandidateIds)
        assertTrue(vm.uiState.value.submitEnabled)
    }

    @Test
    fun `후보에 없는 처음 선택값은 버린다`() {
        val vm = PlaceVoteViewModel(template(selected = setOf("none")))
        assertTrue(vm.uiState.value.selectedCandidateIds.isEmpty())
        assertFalse(vm.uiState.value.submitEnabled)
    }

    @Test
    fun `검색어는 이름과 주소에서 찾고 선택값은 유지한다`() {
        val vm = PlaceVoteViewModel(template())
        vm.onSelectionChange(setOf("hongdae"))

        vm.onSearchQueryChange("송파")
        assertEquals(listOf("jamsil"), vm.uiState.value.candidates.map { it.id })
        assertEquals(setOf("hongdae"), vm.uiState.value.selectedCandidateIds)

        vm.onSearchQueryChange("")
        assertEquals(3, vm.uiState.value.candidates.size)
    }

    @Test
    fun `복수 선택하면 선택한 장소 이름이 모인다`() {
        val vm = PlaceVoteViewModel(template())
        vm.onSelectionChange(setOf("gangnam", "jamsil"))
        assertEquals(listOf("강남역", "잠실역"), vm.uiState.value.selectedPlaceNames)
    }

    @Test
    fun `최대 개수를 넘기면 선택을 바꾸지 않고 안내를 보낸다`() {
        val vm = PlaceVoteViewModel(template(), maxSelection = 2)
        vm.onSelectionChange(setOf("gangnam", "jamsil"))

        vm.onSelectionChange(setOf("gangnam", "jamsil", "hongdae"))

        assertEquals(setOf("gangnam", "jamsil"), vm.uiState.value.selectedCandidateIds)
        assertEquals(PlaceVoteNotice.MAX_SELECTION_REACHED, vm.notice.value)
    }

    @Test
    fun `후보를 삭제하면 선택에서도 빠진다`() {
        val vm = PlaceVoteViewModel(template(selected = setOf("gangnam")))
        vm.onRemoveCandidate("gangnam")
        assertEquals(listOf("jamsil", "hongdae"), vm.uiState.value.candidates.map { it.id })
        assertTrue(vm.uiState.value.selectedCandidateIds.isEmpty())
        assertFalse(vm.uiState.value.submitEnabled)
    }

    @Test
    fun `후보를 추가하고 같은 id는 무시한다`() {
        val vm = PlaceVoteViewModel(template())
        vm.addCandidate(candidate("sinchon", "신촌역"))
        vm.addCandidate(candidate("sinchon", "신촌역 중복"))
        assertEquals(4, vm.uiState.value.candidates.size)
        assertEquals("신촌역", vm.uiState.value.candidates.last().name)
    }

    @Test
    fun `저장은 서버로 보내지 않고 안내만 보내며 선택값은 유지한다`() {
        val vm = PlaceVoteViewModel(template(selected = setOf("gangnam")))
        vm.onSubmit()
        assertEquals(PlaceVoteNotice.SUBMIT_API_NOT_CONNECTED, vm.notice.value)
        assertEquals(setOf("gangnam"), vm.uiState.value.selectedCandidateIds)
        vm.clearNotice()
        assertNull(vm.notice.value)
    }

    @Test
    fun `선택이 없으면 저장해도 안내를 보내지 않는다`() {
        val vm = PlaceVoteViewModel(template())
        vm.onSubmit()
        assertNull(vm.notice.value)
    }

    @Test
    fun `요약 카드 표시 여부는 처음 템플릿을 따른다`() {
        assertTrue(PlaceVoteViewModel(template(withSummary = true)).showsSelectedSummary)
        assertFalse(PlaceVoteViewModel(template()).showsSelectedSummary)
    }
}
