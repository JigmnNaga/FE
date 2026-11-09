package com.jigmnnaga.presentation.appointment.create

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppointmentCreateScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun nameQuickChoiceEnablesNextAndDelegatesNavigation() {
        val state = mutableStateOf(AppointmentCreateUiState())
        var nextCount = 0
        composeRule.setContent {
            JigmnnagaTheme {
                AppointmentCreateScreen(
                    state = state.value,
                    onStateChange = { state.value = it },
                    onBack = {},
                    onNext = { nextCount++ },
                    onCreate = {},
                    onShare = { _, _ -> },
                    onDismissCopyConfirmation = {}
                )
            }
        }

        composeRule.onNodeWithTag("appointment-next").assertIsNotEnabled()
        composeRule.onNodeWithTag("appointment-quick-choice-dinner").performClick()
        composeRule.onNodeWithTag("appointment-next").assertIsEnabled().performClick()
        composeRule.runOnIdle {
            assertEquals("저녁 모임", state.value.appointmentName)
            assertEquals(1, nextCount)
        }
    }

    @Test
    fun narrowScreenWithLargeFontScaleKeepsMainControlsVisible() {
        composeRule.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(baseDensity.density, 1.5f)) {
                JigmnnagaTheme {
                    Box(Modifier.width(320.dp)) {
                        AppointmentCreateScreen(
                            state = AppointmentCreateUiState(),
                            onStateChange = {},
                            onBack = {},
                            onNext = {},
                            onCreate = {},
                            onShare = { _, _ -> },
                            onDismissCopyConfirmation = {}
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithTag("appointment-name-field").assertExists()
        composeRule.onNodeWithTag("appointment-next").assertIsDisplayed().assertIsNotEnabled()
    }

    @Test
    fun selectedScheduleDateEnablesNext() {
        val displayedMonth = AppointmentDate.currentMonth()
        val state = mutableStateOf(
            AppointmentCreateUiState(
                step = AppointmentCreateStep.SCHEDULE,
                displayedMonth = displayedMonth
            )
        )
        var nextCount = 0
        var previousCount = 0
        composeRule.setContent {
            JigmnnagaTheme {
                AppointmentCreateScreen(
                    state = state.value,
                    onStateChange = { state.value = it },
                    onBack = {},
                    onPrevious = { previousCount++ },
                    onNext = { nextCount++ },
                    onCreate = {},
                    onShare = { _, _ -> },
                    onDismissCopyConfirmation = {}
                )
            }
        }

        composeRule.onNodeWithTag("appointment-schedule-single_day").performClick()
        composeRule.onNodeWithTag(
            "appointment-date-${displayedMonth.year}-${displayedMonth.month}-1"
        ).performClick()
        composeRule.onNodeWithTag("appointment-next").assertIsEnabled().performClick()
        composeRule.runOnIdle { assertEquals(1, nextCount) }
        composeRule.onNodeWithTag("appointment-previous").performClick()
        composeRule.runOnIdle { assertEquals(1, previousCount) }
    }

    @Test
    fun createCallbackReceivesCompleteUiDraftWithoutServerAssumptions() {
        val date = AppointmentDate.currentMonth()
        val state = AppointmentCreateUiState(
            step = AppointmentCreateStep.RESPONSE_DEADLINE,
            appointmentName = "저녁 모임",
            scheduleType = AppointmentScheduleType.SINGLE_DAY,
            selectedStartDate = date,
            selectedLocations = listOf(AppointmentLocation("강남역", "주소 미연동")),
            deadlineChoice = AppointmentDeadlineChoice.THREE_DAYS
        )
        var createdDraft: AppointmentCreateDraft? = null
        composeRule.setContent {
            JigmnnagaTheme {
                AppointmentCreateScreen(
                    state = state,
                    onStateChange = {},
                    onBack = {},
                    onNext = {},
                    onCreate = { createdDraft = it },
                    onShare = { _, _ -> },
                    onDismissCopyConfirmation = {}
                )
            }
        }

        composeRule.onNodeWithTag("appointment-next").assertIsEnabled().performClick()
        composeRule.runOnIdle {
            assertNotNull(createdDraft)
            assertEquals("저녁 모임", createdDraft?.name)
            assertEquals(listOf(AppointmentLocation("강남역", "주소 미연동")), createdDraft?.locations)
        }
    }

    @Test
    fun invalidDirectDateKeepsCreateButtonDisabledAndDoesNotCreateDraft() {
        val state = AppointmentCreateUiState(
            step = AppointmentCreateStep.RESPONSE_DEADLINE,
            appointmentName = "모임",
            scheduleType = AppointmentScheduleType.SINGLE_DAY,
            selectedStartDate = AppointmentDate(2026, 2, 30),
            selectedLocations = listOf(AppointmentLocation("강남역", "주소")),
            deadlineChoice = AppointmentDeadlineChoice.ONE_DAY
        )
        var createCallbackCount = 0
        composeRule.setContent {
            JigmnnagaTheme {
                AppointmentCreateScreen(
                    state = state,
                    onStateChange = {},
                    onBack = {},
                    onNext = {},
                    onCreate = { createCallbackCount++ },
                    onShare = { _, _ -> },
                    onDismissCopyConfirmation = {}
                )
            }
        }

        composeRule.onNodeWithTag("appointment-next").assertIsNotEnabled()
        composeRule.runOnIdle {
            assertNull(state.toDraftOrNull())
            assertEquals(0, createCallbackCount)
        }
    }

    @Test
    fun locationPickerAllowsAtMostFiveSelectedCandidates() {
        val candidates = (1..6).map { AppointmentLocation("장소 $it", "주소 $it") }
        val state = mutableStateOf(
            AppointmentCreateUiState(
                step = AppointmentCreateStep.LOCATIONS,
                appointmentName = "모임",
                locationCandidates = candidates
            )
        )
        composeRule.setContent {
            JigmnnagaTheme {
                AppointmentCreateScreen(
                    state = state.value,
                    onStateChange = { state.value = it },
                    onBack = {},
                    onNext = {},
                    onCreate = {},
                    onShare = { _, _ -> },
                    onDismissCopyConfirmation = {}
                )
            }
        }

        (1..5).forEach { index -> composeRule.onNodeWithText("장소 $index").performClick() }
        composeRule.onNodeWithText("장소 6").assertIsNotEnabled()
        composeRule.runOnIdle { assertEquals(5, state.value.selectedLocations.size) }
    }

    @Test
    fun customDeadlineSelectionEnablesCreation() {
        val displayedMonth = AppointmentDate.currentMonth()
        val state = mutableStateOf(
            AppointmentCreateUiState(
                step = AppointmentCreateStep.RESPONSE_DEADLINE,
                appointmentName = "모임",
                scheduleType = AppointmentScheduleType.SINGLE_DAY,
                selectedStartDate = displayedMonth,
                selectedLocations = listOf(AppointmentLocation("강남역", "주소")),
                displayedMonth = displayedMonth
            )
        )
        composeRule.setContent {
            JigmnnagaTheme {
                AppointmentCreateScreen(
                    state = state.value,
                    onStateChange = { state.value = it },
                    onBack = {},
                    onNext = {},
                    onCreate = {},
                    onShare = { _, _ -> },
                    onDismissCopyConfirmation = {}
                )
            }
        }

        composeRule.onNodeWithTag("appointment-next").assertIsNotEnabled()
        composeRule.onNodeWithTag("appointment-deadline-custom_date").performClick()
        composeRule.onNodeWithTag(
            "appointment-date-${displayedMonth.year}-${displayedMonth.month}-1"
        ).performClick()
        composeRule.onNodeWithTag("appointment-next").assertIsEnabled()
    }

    @Test
    fun inviteActionsDelegateOnlyTheProvidedInviteLink() {
        val inviteLink = "https://example.invalid/invite-preview"
        val state = AppointmentCreateUiState(
            step = AppointmentCreateStep.INVITATION,
            appointmentName = "저녁 모임",
            inviteLink = inviteLink
        )
        val actions = mutableListOf<Pair<AppointmentShareAction, String>>()
        composeRule.setContent {
            JigmnnagaTheme {
                AppointmentCreateScreen(
                    state = state,
                    onStateChange = {},
                    onBack = {},
                    onNext = {},
                    onCreate = {},
                    onShare = { action, link -> actions += action to link },
                    onDismissCopyConfirmation = {}
                )
            }
        }

        composeRule.onNodeWithTag("appointment-copy-link").performClick()
        composeRule.onNodeWithTag("appointment-share-kakao").performClick()
        composeRule.runOnIdle {
            assertEquals(
                listOf(
                    AppointmentShareAction.COPY_LINK to inviteLink,
                    AppointmentShareAction.KAKAO_TALK to inviteLink
                ),
                actions
            )
        }
    }

    @Test
    fun existingInviteLinkCanBeSharedAgainForReinviting() {
        val inviteLink = "https://example.invalid/issued-link"
        val state = AppointmentCreateUiState(
            step = AppointmentCreateStep.INVITATION,
            inviteLink = inviteLink
        )
        val sharedLinks = mutableListOf<String>()
        composeRule.setContent {
            JigmnnagaTheme {
                AppointmentCreateScreen(
                    state = state,
                    onStateChange = {},
                    onBack = {},
                    onNext = {},
                    onCreate = {},
                    onShare = { _, link -> sharedLinks += link },
                    onDismissCopyConfirmation = {}
                )
            }
        }

        composeRule.onNodeWithTag("appointment-share-kakao").performClick()
        composeRule.onNodeWithTag("appointment-share-kakao").performClick()
        composeRule.runOnIdle { assertEquals(listOf(inviteLink, inviteLink), sharedLinks) }
    }
}
