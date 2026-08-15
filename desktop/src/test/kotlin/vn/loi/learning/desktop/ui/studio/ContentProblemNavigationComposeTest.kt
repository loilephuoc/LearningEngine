package vn.loi.learning.desktop.ui.studio

import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.application.contentpackaging.browser.*
import vn.loi.learning.desktop.ui.browser.*
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.library.model.InstalledPackageId

@OptIn(ExperimentalTestApi::class)
class ContentProblemNavigationComposeTest {
    @Test
    fun `production explorer shows count filter and boundary-aware navigation`() {
        var next = 0
        var selectedFilter: ContentProblemFilter? = null
        runComposeUiTest {
            setContent {
                ContentExplorerPane(
                    uiState = state(), onClose = {}, onSelectRow = {}, onSubmitSearch = {},
                    onToggleHighlight = {}, onQueryChanged = {}, onClearQuery = {},
                    onLessonFilterChanged = {}, onMediaFilterChanged = {}, onSortChanged = {},
                    onResetFilters = {}, onDoubleClickRow = {},
                    onProblemFilterChanged = { selectedFilter = it }, onNextProblem = { next++ }
                )
            }
            onNodeWithText("Problems: 2").assertIsDisplayed()
            onNodeWithText("All Problems").performClick()
            onNodeWithText("Missing Image (2)").performClick()
            onNodeWithText("← Previous Problem").assertIsNotEnabled()
            onNodeWithText("Next Problem →").assertIsEnabled().performClick()
        }
        assertEquals(ContentProblemFilter.MISSING_IMAGE, selectedFilter)
        assertEquals(1, next)
    }

    private fun state(): PackageContentBrowserUiState {
        val items = (1..2).map { index ->
            PackageContentBrowserItem(
                index, ContentId("c$index"), "Question $index", "Answer $index", "", "WORD",
                null, null, "Lesson", "Package", true, false, "missing-$index.png", null,
                exampleText = null, exampleTranslation = null, learningItemCount = 0,
                learningItemIds = emptyList(), learningModes = emptyList(), tags = emptySet(), searchableText = "question $index"
            )
        }
        return PackageContentBrowserUiState(
            InstalledPackageId("installed"), "Package", items,
            selectedContentId = "c1", problemFilter = ContentProblemFilter.ALL_PROBLEMS,
            problemProjection = ContentProblemProjection(items.associate { it.contentId.value to setOf(ContentProblem.MISSING_IMAGE) })
        )
    }
}
