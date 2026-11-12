package com.jigmnnaga

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jigmnnaga.core.designsystem.component.MainHeader
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DesignSystemProfileLabelTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun mainHeaderShowsOnlyFirstCharacterOfLongProfileName() {
        val longName = "김민지입니다아주아주긴이름"
        composeRule.setContent {
            JigmnnagaTheme {
                MainHeader(onNotificationClick = {}, onProfileClick = {}, profileLabel = longName)
            }
        }

        composeRule.onNodeWithText("김").assertExists()
        composeRule.onAllNodesWithText(longName).assertCountEquals(0)
    }

    @Test
    fun mainHeaderTrimsLeadingWhitespaceBeforeTakingFirstCharacter() {
        composeRule.setContent {
            JigmnnagaTheme {
                MainHeader(onNotificationClick = {}, onProfileClick = {}, profileLabel = "  지민")
            }
        }

        composeRule.onNodeWithText("지").assertExists()
    }
}