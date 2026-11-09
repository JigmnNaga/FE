package com.jigmnnaga.presentation.appointment.invite

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppointmentInviteScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun pendingInviteRendersMembersAndDispatchesDecisionCallbacks() {
        var back = 0
        var accept = 0
        var decline = 0
        composeRule.setContent {
            JigmnnagaTheme {
                AppointmentInviteScreen(inviteState(), { back++ }, { accept++ }, { decline++ }, {}, {})
            }
        }

        composeRule.onNodeWithText("태양이 약속에 초대했어요").assertExists()
        composeRule.onNodeWithText("현재 참여 멤버 2명").assertExists()
        listOf("태양", "하은", "채림", "나").forEachIndexed { index, _ ->
            composeRule.onNodeWithTag("invite-member-$index").assertExists()
        }

        composeRule.onNodeWithTag("invite-accept").performClick()
        composeRule.onNodeWithTag("invite-decline").performClick()
        composeRule.runOnIdle { assertEquals(1, accept); assertEquals(1, decline); assertEquals(0, back) }
    }

    @Test
    fun backHeaderDispatchesBackCallback() {
        var back = 0
        composeRule.setContent {
            JigmnnagaTheme {
                AppointmentInviteScreen(inviteState(), { back++ }, {}, {}, {}, {})
            }
        }
        composeRule.onNodeWithContentDescription("뒤로가기").performClick()
        composeRule.runOnIdle { assertEquals(1, back) }
    }

    @Test
    fun acceptedResultOnlyOffersChatCallback() {
        var openChat = 0
        var returnHome = 0
        composeRule.setContent {
            JigmnnagaTheme {
                AppointmentInviteScreen(
                    inviteState().copy(result = AppointmentInviteResult.ACCEPTED),
                    {}, {}, {}, { openChat++ }, { returnHome++ }
                )
            }
        }
        composeRule.onNodeWithText("약속을 수락했어요!").assertExists()
        composeRule.onNodeWithText("채팅방으로 이동").performClick()
        composeRule.runOnIdle { assertEquals(1, openChat); assertEquals(0, returnHome) }
    }

    @Test
    fun declinedResultOnlyOffersHomeCallback() {
        var openChat = 0
        var returnHome = 0
        composeRule.setContent {
            JigmnnagaTheme {
                AppointmentInviteScreen(
                    inviteState().copy(result = AppointmentInviteResult.DECLINED),
                    {}, {}, {}, { openChat++ }, { returnHome++ }
                )
            }
        }
        composeRule.onNodeWithText("약속을 거절했어요").assertExists()
        composeRule.onNodeWithText("홈으로 돌아가기").performClick()
        composeRule.runOnIdle { assertEquals(0, openChat); assertEquals(1, returnHome) }
    }

    @Test
    fun submittingStateDisablesBothDecisionButtonsAndShowsProgressLabel() {
        composeRule.setContent {
            JigmnnagaTheme {
                AppointmentInviteScreen(
                    inviteState().copy(isSubmitting = true),
                    {}, {}, {}, {}, {}
                )
            }
        }

        composeRule.onNodeWithTag("invite-accept").assertIsNotEnabled()
        composeRule.onNodeWithTag("invite-decline").assertIsNotEnabled()
        composeRule.onNodeWithText("처리 중").assertExists()
    }

    @Test
    fun failedDecisionShowsRetryableErrorWithoutShowingResultScreen() {
        composeRule.setContent {
            JigmnnagaTheme {
                AppointmentInviteScreen(
                    inviteState().copy(decisionError = AppointmentInviteDecisionError.REQUEST_FAILED),
                    {}, {}, {}, {}, {}
                )
            }
        }

        composeRule.onNodeWithText("요청을 처리하지 못했어요. 다시 시도해 주세요.").assertExists()
        composeRule.onNodeWithText("약속을 수락했어요!").assertDoesNotExist()
    }

    @Test
    fun pendingScreenKeepsActionsAtNarrowWidthAndLargerFontScales() {
        val fontScale = mutableFloatStateOf(1f)
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale.floatValue)) {
                JigmnnagaTheme {
                    Box(Modifier.width(320.dp).height(640.dp)) {
                        AppointmentInviteScreen(inviteState(), {}, {}, {}, {}, {})
                    }
                }
            }
        }

        listOf(1f, 1.3f, 1.5f).forEach { scale ->
            composeRule.runOnIdle { fontScale.floatValue = scale }
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("invite-accept").assertExists()
            composeRule.onNodeWithTag("invite-decline").assertExists()
            composeRule.onNodeWithText("태양이 약속에 초대했어요").assertExists()
        }
        val rootBottom = composeRule.onRoot().fetchSemanticsNode().boundsInRoot.bottom
        val acceptBottom = composeRule.onNodeWithTag("invite-accept").fetchSemanticsNode().boundsInRoot.bottom
        val bottomInsetAllowance = with(composeRule.density) { 64.dp.toPx() }
        assertTrue("accept action should stay anchored near the safe bottom", rootBottom - acceptBottom < bottomInsetAllowance)
    }

    private fun inviteState() = AppointmentInviteUiState(
        appointmentName = "중간고사 카공 모임",
        inviterName = "태양",
        members = listOf(
            AppointmentInviteMember("태양", AppointmentInviteMemberStatus.HOST),
            AppointmentInviteMember("하은", AppointmentInviteMemberStatus.ACCEPTED),
            AppointmentInviteMember("채림", AppointmentInviteMemberStatus.DECLINED),
            AppointmentInviteMember("나", AppointmentInviteMemberStatus.WAITING)
        )
    )
}
