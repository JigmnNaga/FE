package com.jigmnnaga.presentation.mypage.sharing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationSharingSettingsUiStateTest {

    @Test
    fun `기본값은 Figma 화면과 같다`() {
        val state = LocationSharingSettingsUiState()

        assertTrue(state.sharingEnabled)
        assertEquals(LocationSharingScope.FRIENDS_ONLY, state.scope)
        assertEquals(LocationSharingStartTime.MINUTES_30, state.startTime)
    }

    @Test
    fun `공유 시작 시간은 화면에 보이는 순서와 분 단위가 맞다`() {
        assertEquals(
            listOf(10, 30, 60),
            LocationSharingStartTime.entries.map { it.minutesBefore }
        )
    }
}
