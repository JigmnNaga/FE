package com.jigmnnaga.presentation.mypage.location

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DefaultLocationScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val home = DefaultLocationItem(1, "집", "서울 강남구 테헤란로 123", 37.5, 127.0)
    private val office = DefaultLocationItem(2, "회사", "서울 서초구 서초대로 45", 37.4, 127.1)
    private val school = DefaultLocationItem(3, "학교", null, 37.3, 127.2)

    private fun setList(
        state: DefaultLocationUiState,
        onAdd: () -> Unit = {},
        onEdit: (Long) -> Unit = {},
        onDelete: (Long) -> Unit = {},
        onRetry: () -> Unit = {}
    ) {
        composeRule.setContent {
            JigmnnagaTheme {
                DefaultLocationScreen(
                    state = state,
                    onBackClick = {},
                    onAddClick = onAdd,
                    onEditClick = onEdit,
                    onDeleteClick = onDelete,
                    onRetryLoad = onRetry
                )
            }
        }
    }

    private fun loaded(vararg items: DefaultLocationItem) =
        DefaultLocationUiState(loadStatus = DefaultLocationLoadStatus.LOADED, locations = items.toList())

    @Test
    fun showsRegisteredLocationsWithCountAndAddButton() {
        setList(loaded(home, office))
        composeRule.onNodeWithText("내 기본 위치").assertExists()
        composeRule.onNodeWithText("2 / 3").assertExists()
        composeRule.onNodeWithText("집").assertExists()
        composeRule.onNodeWithText("서울 서초구 서초대로 45").assertExists()
        composeRule.onNodeWithText("기본 위치 추가하기").assertExists()
        composeRule.onNodeWithTag("default-location-add").assertIsEnabled()
    }

    @Test
    fun editAndDeleteReportTheCardTheyBelongTo() {
        var edited = 0L
        var deleted = 0L
        setList(loaded(home, office), onEdit = { edited = it }, onDelete = { deleted = it })

        composeRule.onNodeWithTag("default-location-edit-2").performClick()
        composeRule.onNodeWithTag("default-location-delete-1").performClick()

        assertEquals(2L, edited)
        assertEquals(1L, deleted)
    }

    @Test
    fun addButtonIsDisabledWhenThreeLocationsAreRegistered() {
        var added = 0
        setList(loaded(home, office, school), onAdd = { added++ })
        composeRule.onNodeWithText("3 / 3").assertExists()
        composeRule.onNodeWithTag("default-location-add").assertIsNotEnabled()
        composeRule.onNodeWithTag("default-location-add").performClick()
        assertEquals(0, added)
    }

    @Test
    fun showsEmptyState() {
        setList(loaded())
        composeRule.onNodeWithTag("default-location-empty").assertExists()
        composeRule.onNodeWithText("0 / 3").assertExists()
    }

    @Test
    fun failedLoadOffersRetry() {
        var retried = 0
        setList(
            DefaultLocationUiState(loadStatus = DefaultLocationLoadStatus.FAILED),
            onRetry = { retried++ }
        )
        composeRule.onNodeWithTag("default-location-failed").assertExists()
        composeRule.onNodeWithTag("default-location-retry").performClick()
        assertEquals(1, retried)
    }

    @Test
    fun editorSavesOnlyAfterNameAndPlaceAreChosen() {
        var saved = 0
        var name = ""
        composeRule.setContent {
            JigmnnagaTheme {
                val editor = DefaultLocationEditorUiState(
                    name = name,
                    selected = DefaultLocationPlace("p", "강남역", null, "서울 강남구", 37.5, 127.0)
                )
                DefaultLocationEditorScreen(
                    editor = editor,
                    onBackClick = {},
                    onNameChange = { name = it },
                    onQueryChange = {},
                    onSelectPlace = {},
                    onSave = { saved++ }
                )
            }
        }
        composeRule.onNodeWithTag("default-location-selected").assertExists()
        composeRule.onNodeWithTag("default-location-save").assertIsNotEnabled()
        composeRule.onNodeWithTag("default-location-name-input").performTextInput("집")
        assertEquals("집", name)
    }

    @Test
    fun editorShowsAddOrEditTitle() {
        composeRule.setContent {
            JigmnnagaTheme {
                DefaultLocationEditorScreen(
                    editor = DefaultLocationEditorUiState(editingId = 1, name = "집"),
                    onBackClick = {},
                    onNameChange = {},
                    onQueryChange = {},
                    onSelectPlace = {},
                    onSave = {}
                )
            }
        }
        composeRule.onNodeWithText("기본 위치 수정").assertExists()
    }
}
