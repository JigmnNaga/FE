package com.jigmnnaga.presentation.appointment.schedule

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppointmentDateTimeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun dateSelectionAndNextRemainOwnedByCallbacks() {
        var state by mutableStateOf(dateState())
        var nextCount = 0
        val date = AppointmentDateValue(2026, 9, 5)

        composeRule.setContent {
            JigmnnagaTheme {
                screen(state = state, onDateToggle = {
                    state = state.copy(selectedDates = state.selectedDates + it, canAdvance = true)
                }, onNext = { nextCount++ })
            }
        }

        composeRule.onNodeWithContentDescription(date.accessibilityLabel())
            .assertHasClickAction()
            .performClick()
        composeRule.onNodeWithContentDescription(date.accessibilityLabel()).assertIsSelected()
        composeRule.onNodeWithText("다음").performClick()
        composeRule.runOnIdle { assertEquals(1, nextCount) }
    }

    @Test
    fun nextButtonIsDisabledUntilOwnerAllowsAdvancing() {
        composeRule.setContent {
            JigmnnagaTheme {
                screen(state = dateState().copy(canAdvance = false))
            }
        }

        composeRule.onNodeWithText("다음").assertIsNotEnabled()
    }

    @Test
    fun ownerProvidedValidationMessageIsDisplayed() {
        composeRule.setContent {
            JigmnnagaTheme {
                screen(state = dateState().copy(errorMessage = "시간을 선택해주세요"))
            }
        }

        composeRule.onNodeWithText("시간을 선택해주세요").assertExists()
    }

    @Test
    fun timePickerAppliesSelectedValueThroughCallback() {
        var state by mutableStateOf(
            dateState().copy(
                step = AppointmentDateTimeStep.TIMES,
                selectedDates = setOf(AppointmentDateValue(2026, 9, 5)),
                activeDate = AppointmentDateValue(2026, 9, 5)
            )
        )
        var applied: AppointmentTimeValue? = null

        composeRule.setContent {
            JigmnnagaTheme {
                AppointmentDateTimeScreen(
                    state = state,
                    onBack = {}, onMonthChange = {}, onDateToggle = {}, onActiveDateSelect = {},
                    onStartTimeClick = {
                        state = state.copy(
                            timePicker = TimePickerUiState(
                                TimePickerTarget.START,
                                AppointmentTimeValue(12, 0)
                            )
                        )
                    },
                    onEndTimeClick = {}, onQuickTimeClick = {},
                    onTimePickerValueChange = {},
                    onTimePickerDismiss = { state = state.copy(timePicker = null) },
                    onTimePickerConfirm = { applied = it; state = state.copy(timePicker = null) },
                    onNext = {}, onGoToChat = {}
                )
            }
        }

        composeRule.onNodeWithContentDescription("시작 시간").performClick()
        composeRule.onNodeWithText("13").performClick()
        composeRule.onNodeWithText("적용").performClick()
        composeRule.runOnIdle { assertEquals(AppointmentTimeValue(13, 0), applied) }
    }

    @Test
    fun completionActionIsPassedToOwner() {
        var goToChatCount = 0
        composeRule.setContent {
            JigmnnagaTheme {
                screen(
                    state = dateState().copy(step = AppointmentDateTimeStep.COMPLETE),
                    onGoToChat = { goToChatCount++ }
                )
            }
        }

        composeRule.onNodeWithText("약속 일정 선택을 완료했어요").assertExists()
        composeRule.onNodeWithText("채팅방으로 이동").performClick()
        composeRule.runOnIdle { assertEquals(1, goToChatCount) }
    }

    @Test
    fun narrowLayoutKeepsCoreControlsAvailableAtLargerFontScales() {
        var currentScale by mutableFloatStateOf(1f)
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, currentScale)) {
                JigmnnagaTheme {
                    Box(Modifier.width(320.dp).height(720.dp)) {
                        AppointmentDateTimeScreen(
                            state = dateState(),
                            onBack = {}, onMonthChange = {}, onDateToggle = {}, onActiveDateSelect = {},
                            onStartTimeClick = {}, onEndTimeClick = {}, onQuickTimeClick = {},
                            onTimePickerValueChange = {}, onTimePickerDismiss = {}, onTimePickerConfirm = {},
                            onNext = {}, onGoToChat = {}
                        )
                    }
                }
            }
        }
        listOf(1f, 1.3f, 1.5f).forEach { requestedScale ->
            composeRule.runOnIdle { currentScale = requestedScale }
            composeRule.waitForIdle()
            composeRule.onNodeWithText("중간고사 카공 모임").assertExists()
            composeRule.onNodeWithText("다음").assertExists()
        }
    }

    @Composable
    private fun screen(
        state: AppointmentDateTimeUiState,
        onDateToggle: (AppointmentDateValue) -> Unit = {},
        onNext: () -> Unit = {},
        onGoToChat: () -> Unit = {}
    ) {
        AppointmentDateTimeScreen(
            state = state,
            onBack = {}, onMonthChange = {}, onDateToggle = onDateToggle, onActiveDateSelect = {},
            onStartTimeClick = {}, onEndTimeClick = {}, onQuickTimeClick = {},
            onTimePickerValueChange = {}, onTimePickerDismiss = {}, onTimePickerConfirm = {},
            onNext = onNext, onGoToChat = onGoToChat
        )
    }

    private fun dateState() = AppointmentDateTimeUiState(
        appointmentTitle = "중간고사 카공 모임",
        completionStatusLabel = "1명 선택 완료",
        displayedMonth = AppointmentCalendarMonth(2026, 9)
    )
}
