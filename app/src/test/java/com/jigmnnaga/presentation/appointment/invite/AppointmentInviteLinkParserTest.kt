package com.jigmnnaga.presentation.appointment.invite

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppointmentInviteLinkParserTest {
    @Test
    fun extractsCodeFromBackendInviteUrl() {
        assertEquals(
            "AbC123",
            AppointmentInviteLinkParser.extractInviteCode("https://jigeumnaga.app/i/AbC123")
        )
    }

    @Test
    fun rejectsUnsupportedHostSchemeAndPath() {
        assertNull(AppointmentInviteLinkParser.extractInviteCode("http://jigeumnaga.app/i/abc"))
        assertNull(AppointmentInviteLinkParser.extractInviteCode("https://example.com/i/abc"))
        assertNull(AppointmentInviteLinkParser.extractInviteCode("https://jigeumnaga.app/abc"))
        assertNull(AppointmentInviteLinkParser.extractInviteCode("https://jigeumnaga.app/i/abc/extra"))
        assertNull(AppointmentInviteLinkParser.extractInviteCode("not a url"))
    }
}
