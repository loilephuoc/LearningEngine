package vn.loi.learning.desktop.ui.studio

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserItem
import vn.loi.learning.desktop.ui.browser.PackageContentBrowserUiState
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.library.model.InstalledPackageId

@OptIn(ExperimentalTestApi::class)
class ContentExplorerProductivityPresentationTest {
    @Test
    fun `highlighted selected row exposes highlight and keeps audio action without image indicator`() =
        runComposeUiTest {
            setContent {
                ContentExplorerPane(
                    uiState = state(query = "", highlighted = true),
                    onClose = {}, onSelectRow = {}, onSubmitSearch = {}, onToggleHighlight = {},
                    onQueryChanged = {}, onClearQuery = {}, onLessonFilterChanged = {},
                    onMediaFilterChanged = {}, onSortChanged = {}, onResetFilters = {},
                    onDoubleClickRow = null, onPlayQuestionAudio = { _, _ -> }
                )
            }

            onNodeWithContentDescription("Row 1: Canal").assert(
                SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Highlighted")
            )
            onNodeWithContentDescription("Play question audio").assertExists()
            onNodeWithContentDescription("View image").assertDoesNotExist()
        }

    @Test
    fun `search Enter submits while Escape and clear button use canonical clear callback`() =
        runComposeUiTest {
            var submits = 0
            var clears = 0
            setContent {
                ContentExplorerPane(
                    uiState = state(query = "canal", highlighted = false),
                    onClose = {}, onSelectRow = {}, onSubmitSearch = { submits++ }, onToggleHighlight = {},
                    onQueryChanged = {}, onClearQuery = { clears++ }, onLessonFilterChanged = {},
                    onMediaFilterChanged = {}, onSortChanged = {}, onResetFilters = {},
                    onDoubleClickRow = null
                )
            }

            onNodeWithTag("explorer-search-input").performClick()
            onNodeWithTag("explorer-search-input").performKeyInput { pressKey(Key.Enter) }
            onNodeWithTag("explorer-search-input").performKeyInput { pressKey(Key.Escape) }
            onNodeWithTag("explorer-clear-search").performClick()
            assertEquals(1, submits)
            assertEquals(2, clears)
        }

    private fun state(query: String, highlighted: Boolean): PackageContentBrowserUiState {
        val item = PackageContentBrowserItem(
            index = 1,
            contentId = ContentId("content-1"),
            questionText = "Canal",
            answerText = "Kênh đào",
            pronunciation = "",
            partOfSpeech = "noun",
            group = null,
            section = null,
            lesson = "Lesson",
            packageName = "Package",
            hasImage = true,
            hasAudio = true,
            imageRef = "image.png",
            audioRef = "audio.mp3",
            questionAudioRef = "audio.mp3",
            exampleText = null,
            exampleTranslation = null,
            learningItemCount = 0,
            learningItemIds = emptyList(),
            learningModes = emptyList(),
            tags = emptySet(),
            searchableText = "canal kênh đào"
        )
        return PackageContentBrowserUiState(
            installedPackageId = InstalledPackageId("installed"),
            packageName = "Package",
            allItems = listOf(item),
            query = query,
            appliedQuery = query,
            selectedContentId = item.contentId.value,
            highlightedContentIds = if (highlighted) setOf(item.contentId.value) else emptySet()
        )
    }
}
