package com.jigmnnaga.presentation.appointment

import com.jigmnnaga.domain.model.PreviousMembers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PreviousMembersArgsTest {

    @Test
    fun `멤버 ID 목록은 쉼표로 이어 넘기고 다시 읽는다`() {
        val argument = listOf(2L, 3L, 15L).toMemberIdsArgument()

        assertEquals("2,3,15", argument)
        assertEquals(listOf(2L, 3L, 15L), argument.toMemberIds())
    }

    @Test
    fun `숫자가 아닌 값과 중복과 공백은 버린다`() {
        assertEquals(listOf(2L, 3L), " 2, x,3 ,2,,".toMemberIds())
        assertEquals(emptyList<Long>(), "".toMemberIds())
    }

    @Test
    fun `이동 경로에서 이전 약속 멤버를 만든다`() {
        val destination = AppointmentCreateFromPreviousDestination(7L, "2,3")

        assertEquals(PreviousMembers(7L, listOf(2L, 3L)), destination.toPreviousMembers())
    }

    @Test
    fun `읽을 수 있는 멤버가 없으면 이전 약속 멤버는 없다`() {
        assertNull(AppointmentCreateFromPreviousDestination(7L, "").toPreviousMembers())
        assertNull(AppointmentCreateFromPreviousDestination(7L, "x,y").toPreviousMembers())
    }
}
