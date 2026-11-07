package com.jigmnnaga

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme
import com.jigmnnaga.core.designsystem.component.AppButton
import com.jigmnnaga.core.designsystem.component.BottomNavigationBar
import com.jigmnnaga.core.designsystem.component.BottomNavigationTab
import com.jigmnnaga.core.designsystem.component.ButtonVariant
import com.jigmnnaga.core.designsystem.component.CircleButton
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun useAppContext() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals(BuildConfig.APPLICATION_ID, appContext.packageName)
    }

    @Test
    fun buttonExposesDisabledState() {
        composeRule.setContent {
            JigmnnagaTheme {
                AppButton(
                    text = "Continue",
                    variant = ButtonVariant.PRIMARY,
                    enabled = false,
                    onClick = {}
                )
            }
        }

        composeRule.onNodeWithText("Continue").assertIsNotEnabled()
    }

    @Test
    fun circleButtonAndNavigationExposeAccessibleMeaningAndSelection() {
        composeRule.setContent {
            JigmnnagaTheme {
                Column {
                    CircleButton(
                        contentDescription = "Profile action",
                        onClick = {},
                        width = 38.dp,
                        text = "A",
                        enabled = false
                    )
                    BottomNavigationBar(
                        selectedTab = BottomNavigationTab.SCHEDULE,
                        onHomeClick = {},
                        onAddClick = {},
                        onScheduleClick = {}
                    )
                }
            }
        }

        composeRule.onNodeWithContentDescription("Profile action").assertIsNotEnabled()
        composeRule.onNodeWithText("일정").assertIsSelected().assertHasClickAction()
    }
}
