package com.jigmnnaga.features.appointment.hostconfirmation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme

class HostAppointmentConfirmationScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun reviewShowsExternallySuppliedDateAndPlaceAndConfirmIsDisabledByState() {
        composeRule.setContent {
            JigmnnagaTheme {
                HostAppointmentConfirmationScreen(
                    state = HostConfirmationUiState(
                        appointmentTitle = "주말 약속",
                        selectedDateTime = "6월 15일 오후 2시",
                        selectedPlace = "서울역",
                        canConfirm = false
                    ),
                    actions = testActions()
                )
            }
        }
        composeRule.onNodeWithText("주말 약속").assertIsDisplayed()
        composeRule.onNodeWithText("6월 15일 오후 2시").assertIsDisplayed()
        composeRule.onNodeWithText("서울역").assertIsDisplayed()
        composeRule.onNodeWithTag("host-confirm").assertIsNotEnabled()
    }

    @Test
    fun dateTimeChoiceForwardsIdToCaller() {
        var selectedId: String? = null
        composeRule.setContent {
            JigmnnagaTheme {
                HostAppointmentConfirmationScreen(
                    state = HostConfirmationUiState(
                        step = HostConfirmationStep.DATE_TIME,
                        appointmentTitle = "약속",
                        selectedDateTime = null,
                        selectedPlace = null,
                        dateTimeChoices = listOf(HostConfirmationChoice("slot-1", "6월 15일 오후 2시"))
                    ),
                    actions = testActions(onSelectDateTime = { selectedId = it })
                )
            }
        }
        composeRule.onNodeWithTag("date-time-choice-slot-1").performClick()
        composeRule.runOnIdle { assertEquals("slot-1", selectedId) }
    }

    @Test
    fun detailLocationCanBeDeferredByCaller() {
        var deferred = false
        composeRule.setContent {
            JigmnnagaTheme {
                HostAppointmentConfirmationScreen(
                    state = HostConfirmationUiState(
                        step = HostConfirmationStep.DETAIL_LOCATION,
                        appointmentTitle = "약속",
                        selectedDateTime = "6월 15일 오후 2시",
                        selectedPlace = "서울역"
                    ),
                    actions = testActions(onChooseDetailLater = { deferred = true })
                )
            }
        }
        composeRule.onNodeWithTag("choose-detail-later").performClick()
        composeRule.runOnIdle { assertEquals(true, deferred) }
    }

    @Test
    fun confirmClickOnlyInvokesExternalCallback() {
        var confirmed = false
        composeRule.setContent {
            JigmnnagaTheme {
                HostAppointmentConfirmationScreen(
                    state = HostConfirmationUiState(
                        appointmentTitle = "약속",
                        selectedDateTime = "6월 15일 오후 2시",
                        selectedPlace = "서울역",
                        canConfirm = true
                    ),
                    actions = testActions(onConfirm = { confirmed = true })
                )
            }
        }
        composeRule.onNodeWithTag("host-confirm").performClick()
        composeRule.runOnIdle { assertEquals(true, confirmed) }
        composeRule.onNodeWithText("약속").assertIsDisplayed()
    }

    @Test
    fun completeStateShowsFigmaCopyAndForwardsChatNavigationCallback() {
        var completed = false
        composeRule.setContent {
            JigmnnagaTheme {
                HostAppointmentConfirmationScreen(
                    state = HostConfirmationUiState(
                        step = HostConfirmationStep.COMPLETE,
                        appointmentTitle = "주말 약속",
                        selectedDateTime = "6월 15일 오후 2시",
                        selectedPlace = "서울역"
                    ),
                    actions = testActions(onComplete = { completed = true })
                )
            }
        }

        composeRule.onNodeWithText("약속 일정 선택이 완료했어요").assertIsDisplayed()
        composeRule.onNodeWithText("멤버들에게 알림이 전송됐어요").assertIsDisplayed()
        composeRule.onNodeWithTag("confirmation-done").assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertEquals(true, completed) }
    }

    @Test
    fun completeStateFits320DpAtFontScalesUpTo150Percent() {
        val fontScale = mutableFloatStateOf(1f)
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale.floatValue)) {
                Box(Modifier.width(320.dp).height(850.dp)) {
                    HostAppointmentConfirmationScreen(
                        state = HostConfirmationUiState(
                            step = HostConfirmationStep.COMPLETE,
                            appointmentTitle = "긴 약속명 예시",
                            selectedDateTime = "긴 날짜 예시",
                            selectedPlace = "긴 장소명 예시"
                        ),
                        actions = testActions()
                    )
                }
            }
        }

        for (scale in listOf(1f, 1.3f, 1.5f)) {
            composeRule.runOnIdle { fontScale.floatValue = scale }
            composeRule.waitForIdle()
            composeRule.onNodeWithText("약속 일정 선택이 완료했어요").assertIsDisplayed()
            composeRule.onNodeWithText("멤버들에게 알림이 전송됐어요").assertIsDisplayed()
            composeRule.onNodeWithTag("confirmation-done").assertIsDisplayed()
        }
    }

    @Test
    fun reviewContentRemainsAvailableAt320DpAndIncreasedFontScales() {
        val fontScale = mutableFloatStateOf(1f)
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale.floatValue)) {
                Box(Modifier.width(320.dp).height(850.dp)) {
                    HostAppointmentConfirmationScreen(
                        state = HostConfirmationUiState(
                            appointmentTitle = "매우 긴 약속 이름이 작은 화면에서 줄바꿈되는지 확인하는 예시",
                            selectedDateTime = "2026년 12월 31일 오후 11시 30분부터 다음 날 오전 1시까지",
                            selectedPlace = "매우 긴 장소 이름이 작은 화면에서 잘리지 않고 표시되는지 확인하는 예시 장소",
                            selectedPlaceDescription = "긴 주소 안내 문구가 여러 줄로 표시됩니다.",
                            canConfirm = true
                        ),
                        actions = testActions()
                    )
                }
            }
        }
        composeRule.runOnIdle { fontScale.floatValue = 1.3f }
        composeRule.waitForIdle()
        composeRule.runOnIdle { fontScale.floatValue = 1.5f }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("매우 긴 약속 이름이 작은 화면에서 줄바꿈되는지 확인하는 예시")
            .assertIsDisplayed()
        composeRule.onNodeWithTag("host-confirm").assertIsDisplayed()
    }

    private fun testActions(
        onSelectDateTime: (String) -> Unit = {},
        onChooseDetailLater: () -> Unit = {},
        onConfirm: () -> Unit = {},
        onComplete: () -> Unit = {}
    ) = HostConfirmationActions(
        onBack = {}, onEditDateTime = {}, onEditPlace = {}, onSelectDateTime = onSelectDateTime,
        onSelectPlace = {}, onOpenDetailLocation = {}, onSelectDetailLocation = {},
        onChooseDetailLater = onChooseDetailLater, onConfirm = onConfirm, onComplete = onComplete
    )
}
