package com.jigmnnaga.presentation.appointment.finalconfirmation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppointmentFinalConfirmationScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsExternallySuppliedAppointmentAndAttendanceSelection() {
        val state = mutableStateOf(sampleState())
        composeRule.setContent {
            JigmnnagaTheme {
                AppointmentFinalConfirmationScreen(
                    state = state.value,
                    actions = actions(
                        onAttendanceChoiceChange = { state.value = state.value.copy(attendanceChoice = it) }
                    )
                )
            }
        }

        composeRule.onNodeWithText("약속 최종 확인").assertIsDisplayed()
        composeRule.onNodeWithText("가을 저녁 모임").assertIsDisplayed()
        composeRule.onNodeWithText("9월 4일 (월)").assertIsDisplayed()
        composeRule.onNodeWithText("오후 5:00").assertIsDisplayed()
        composeRule.onNodeWithText("서울역 근처 장소").assertIsDisplayed()
        composeRule.onNodeWithTag("attendance-attending").performClick().assertIsSelected()
    }

    @Test
    fun confirmButtonFollowsExternalEnabledStateAndInvokesCallback() {
        var confirms = 0
        val state = mutableStateOf(sampleState().copy(canConfirm = false))
        composeRule.setContent {
            JigmnnagaTheme {
                AppointmentFinalConfirmationScreen(
                    state = state.value,
                    actions = actions(onConfirm = { confirms++ })
                )
            }
        }

        composeRule.onNodeWithTag("final-confirmation-confirm").assertIsNotEnabled()
        composeRule.runOnIdle { state.value = state.value.copy(canConfirm = true) }
        composeRule.onNodeWithTag("final-confirmation-confirm").assertIsEnabled().performClick()
        composeRule.runOnIdle { assertEquals(1, confirms) }
    }

    @Test
    fun primaryActionStaysVisibleAt320DpAndFontScales() {
        val fontScale = mutableFloatStateOf(1f)
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale.floatValue)) {
                JigmnnagaTheme {
                    Box(Modifier.width(320.dp)) {
                        AppointmentFinalConfirmationScreen(
                            state = sampleState().copy(
                                appointmentTitle = "작은 화면에서 줄바꿈을 확인하는 긴 약속 이름",
                                placeName = "긴 장소 이름과 주소가 좁은 화면에서도 잘리지 않는지 확인합니다"
                            ),
                            actions = actions()
                        )
                    }
                }
            }
        }

        listOf(1f, 1.3f, 1.5f).forEach { scale ->
            composeRule.runOnIdle { fontScale.floatValue = scale }
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("final-confirmation-confirm").assertIsDisplayed()
        }
        composeRule.onNodeWithText("참석").assertExists()
    }

    private fun sampleState() = AppointmentFinalConfirmationUiState(
        appointmentTitle = "가을 저녁 모임",
        dateLabel = "9월 4일 (월)",
        timeLabel = "오후 5:00",
        placeName = "서울역 근처 장소",
        detailLocation = "1번 출구 앞",
        attendanceChoice = AppointmentAttendanceChoice.NOT_ATTENDING,
        canConfirm = true
    )

    private fun actions(
        onAttendanceChoiceChange: (AppointmentAttendanceChoice) -> Unit = {},
        onConfirm: () -> Unit = {}
    ) = AppointmentFinalConfirmationActions(
        onBack = {},
        onAttendanceChoiceChange = onAttendanceChoiceChange,
        onConfirm = onConfirm
    )
}
