package vn.loi.learning.desktop.ui.studio

import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.input.key.Key
import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserItem
import vn.loi.learning.desktop.ui.browser.ContentProblem
import vn.loi.learning.desktop.ui.browser.ContentProblemProjection
import vn.loi.learning.desktop.ui.browser.PackageContentBrowserUiState
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.library.model.InstalledPackageId

@OptIn(ExperimentalTestApi::class)
class ContentMultiSelectionComposeTest {
    @Test
    fun `real multimodal input dispatches exactly one modified action and no plain selection`() {
        var toggles = 0
        var ranges = 0
        var selects = 0
        var doubles = 0
        runComposeUiTest {
            setContent {
                ContentExplorerPane(
                    uiState = state().copy(selectedContentIds = emptySet()),
                    onClose = {}, onSelectRow = { selects++ }, onSubmitSearch = {},
                    onToggleHighlight = {}, onQueryChanged = {}, onClearQuery = {},
                    onLessonFilterChanged = {}, onMediaFilterChanged = {}, onSortChanged = {},
                    onResetFilters = {}, onDoubleClickRow = { doubles++ },
                    onToggleMultiSelection = { toggles++ }, onSelectMultiRange = { ranges++ }
                )
            }
            onNodeWithTag("explorer-row-c1").performMultiModalInput {
                key { keyDown(Key.CtrlLeft) }
                mouse { click() }
                key { keyUp(Key.CtrlLeft) }
            }
            onNodeWithTag("explorer-row-c2").performMultiModalInput {
                key { keyDown(Key.ShiftLeft) }
                mouse { click() }
                key { keyUp(Key.ShiftLeft) }
            }
        }
        assertEquals(listOf(1, 1, 0, 0), listOf(toggles, ranges, selects, doubles))
    }

    @Test
    fun `production explorer exposes truthful selection status and safe batch actions`() {
        var highlight = 0
        var remove = 0
        var check = 0
        var clear = 0
        runComposeUiTest {
            setContent {
                ContentExplorerPane(
                    uiState = state(), onClose = {}, onSelectRow = {}, onSubmitSearch = {},
                    onToggleHighlight = {}, onQueryChanged = {}, onClearQuery = {},
                    onLessonFilterChanged = {}, onMediaFilterChanged = {}, onSortChanged = {},
                    onResetFilters = {}, onDoubleClickRow = {},
                    onHighlightSelected = { highlight++ },
                    onRemoveHighlightSelected = { remove++ },
                    onCheckSelectedMedia = { check++ },
                    onClearMultiSelection = { clear++ }
                )
            }
            onNodeWithText("2 selected").assertIsDisplayed()
            onNodeWithContentDescription("2 selected items").assertExists()
            onNodeWithContentDescription("Highlight 2 selected items").performClick()
            onNodeWithContentDescription("Remove highlight from 2 selected items").performClick()
            onNodeWithContentDescription("Check media for 2 selected items").performClick()
            onNodeWithText("Clear Selection").performClick()
            onNodeWithTag("explorer-row-c1").assertIsSelected()
        }
        assertEquals(listOf(1, 1, 1, 1), listOf(highlight, remove, check, clear))
    }

    private fun state(): PackageContentBrowserUiState {
        val items = (1..2).map { index ->
            PackageContentBrowserItem(
                index, ContentId("c$index"), "Question $index", "Answer $index", "", "WORD",
                null, null, "Lesson", "Package", false, false, null, null,
                exampleText = null, exampleTranslation = null, learningItemCount = 0,
                learningItemIds = emptyList(), learningModes = emptyList(), tags = emptySet(),
                searchableText = "question $index"
            )
        }
        return PackageContentBrowserUiState(
            InstalledPackageId("installed"), "Package", items,
            selectedContentId = "c2",
            selectedContentIds = setOf("c1", "c2"),
            highlightedContentIds = setOf("c1"),
            problemProjection = ContentProblemProjection(mapOf("c1" to setOf(ContentProblem.MISSING_IMAGE)))
        )
    }
}
