package com.jigmnnaga.presentation.appointment.schedule

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AvailabilityHeatmapTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun headingTimetableAndLegendAreDisplayed() {
        composeRule.setContent {
            JigmnnagaTheme {
                AvailabilityHeatmap(state = testState())
            }
        }

        composeRule.onNodeWithText("시간표").assertExists()
        composeRule.onNodeWithText("2026년 9월").assertExists()
        composeRule.onNodeWithText("1시간 단위").assertExists()
        composeRule.onNodeWithText("4일 (금)").assertExists()
        composeRule.onNodeWithText("10:00").assertExists()
        composeRule.onNodeWithText("가능 인원").assertExists()
        listOf("0명", "1명", "2명", "3명").forEach { label ->
            composeRule.onNodeWithText(label).assertExists()
        }
    }

    @Test
    fun cellDescriptionsExposeAvailableCount() {
        composeRule.setContent {
            JigmnnagaTheme {
                AvailabilityHeatmap(state = testState())
            }
        }

        composeRule.onNodeWithContentDescription("4일 (금) 10:00 3명 가능").assertExists()
        composeRule.onNodeWithContentDescription("5일 (토) 10:00 0명 가능").assertExists()
        composeRule.onNodeWithContentDescription("5일 (토) 11:00 2명 가능").assertExists()
    }

    @Test
    fun narrowLayoutKeepsLegendAvailableAtLargerFontScales() {
        val fontScale = mutableFloatStateOf(1f)
        composeRule.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(baseDensity.density, fontScale.floatValue)) {
                JigmnnagaTheme {
                    Box(Modifier.width(320.dp)) {
                        AvailabilityHeatmap(state = testState())
                    }
                }
            }
        }

        listOf(1f, 1.3f, 1.5f).forEach { scale ->
            composeRule.runOnIdle { fontScale.floatValue = scale }
            composeRule.waitForIdle()
            composeRule.onNodeWithText("시간표").assertExists()
            composeRule.onNodeWithText("가능 인원").assertExists()
        }
    }

    private fun testState() = AvailabilityHeatmapUiState(
        monthLabel = "2026년 9월",
        dates = listOf(
            AvailabilityDateColumnUiState("date-4", "4일 (금)"),
            AvailabilityDateColumnUiState("date-5", "5일 (토)")
        ),
        rows = listOf(
            AvailabilityTimeRowUiState("10:00", listOf(3, 0)),
            AvailabilityTimeRowUiState("11:00", listOf(1, 2))
        )
    )
}