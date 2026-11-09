package com.jigmnnaga.presentation.appointment.create

import android.content.ClipboardManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppointmentCreateRouteTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun copyButtonUsesAndroidClipboardOnlyForAnIssuedLink() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        val link = "https://example.invalid/server-issued-in-test"
        val viewModel = AppointmentCreateViewModel()
        viewModel.updateUiState(
            AppointmentCreateUiState(
                step = AppointmentCreateStep.INVITATION,
                inviteLink = link
            )
        )

        composeRule.setContent {
            JigmnnagaTheme {
                AppointmentCreateRoute(onExit = {}, viewModel = viewModel)
            }
        }
        composeRule.onNodeWithTag("appointment-copy-link").performClick()

        composeRule.runOnIdle {
            assertEquals(link, clipboard.primaryClip?.getItemAt(0)?.text?.toString())
            assertEquals(true, viewModel.uiState.value.copyConfirmationVisible)
        }
        composeRule.onNodeWithText("링크를 복사했어요!").assertIsDisplayed()
    }

    @Test
    fun copyAndShareAreDisabledUntilBackendSuppliesAnInviteLink() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("previous", "unchanged"))
        val viewModel = AppointmentCreateViewModel()
        viewModel.updateUiState(AppointmentCreateUiState(step = AppointmentCreateStep.INVITATION))

        composeRule.setContent {
            JigmnnagaTheme {
                AppointmentCreateRoute(onExit = {}, viewModel = viewModel)
            }
        }

        composeRule.onNodeWithTag("appointment-share-kakao").assertIsNotEnabled()
        composeRule.onNodeWithTag("appointment-copy-link").assertIsNotEnabled()
        composeRule.runOnIdle {
            assertEquals("unchanged", clipboard.primaryClip?.getItemAt(0)?.text?.toString())
            assertNull(viewModel.uiState.value.inviteLink)
        }
        composeRule.onNodeWithText("링크가 발급되면 여기에 표시됩니다").assertIsDisplayed()
    }

    @Test
    fun externalShareIntentContainsOnlyTheSuppliedLink() {
        val link = "https://example.invalid/server-issued-in-test"

        val shareIntent = buildInviteShareIntent(link)

        assertEquals(android.content.Intent.ACTION_SEND, shareIntent.action)
        assertEquals("text/plain", shareIntent.type)
        assertEquals(link, shareIntent.getStringExtra(android.content.Intent.EXTRA_TEXT))
    }

    @Test
    fun createAttemptWithoutApiIsNotLoadingAndCannotBeSubmittedTwice() {
        val viewModel = AppointmentCreateViewModel()
        viewModel.updateUiState(
            AppointmentCreateUiState(
                step = AppointmentCreateStep.RESPONSE_DEADLINE,
                appointmentName = "모임",
                scheduleType = AppointmentScheduleType.SINGLE_DAY,
                selectedStartDate = AppointmentDate(2026, 11, 14),
                selectedLocations = listOf(AppointmentLocation("강남역", "서울")),
                deadlineChoice = AppointmentDeadlineChoice.ONE_DAY
            )
        )
        composeRule.setContent {
            JigmnnagaTheme {
                AppointmentCreateRoute(onExit = {}, viewModel = viewModel)
            }
        }

        composeRule.onNodeWithTag("appointment-next").assertIsEnabled().performClick()
        composeRule.onNodeWithTag("appointment-next").assertIsNotEnabled()
        composeRule.runOnIdle {
            val prepared = viewModel.pendingDraft.value
            assertEquals("모임", prepared?.name)
            assertEquals(AppointmentCreateSubmissionStatus.API_UNAVAILABLE, viewModel.uiState.value.submissionStatus)
            assertEquals(false, viewModel.uiState.value.isCreating)
            assertNull(viewModel.uiState.value.inviteLink)
        }
    }

    @Test
    fun missingShareTargetProducesUnavailableNoticeWithoutLaunchingChooser() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val viewModel = AppointmentCreateViewModel()

        context.handleInviteAction(
            action = AppointmentShareAction.KAKAO_TALK,
            link = "https://example.invalid/invite",
            viewModel = viewModel,
            hasShareTarget = { false }
        )

        assertEquals(AppointmentCreateNotice.SHARE_UNAVAILABLE, viewModel.notice.value)
    }
}
