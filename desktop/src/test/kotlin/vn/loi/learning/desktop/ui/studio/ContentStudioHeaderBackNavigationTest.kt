package vn.loi.learning.desktop.ui.studio

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.v2.runComposeUiTest
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.application.contentmedia.ContentMediaAsset
import vn.loi.learning.application.port.ContentMediaStorage
import vn.loi.learning.desktop.ui.browser.PackageContentBrowserUiState
import vn.loi.learning.desktop.ui.contentlibrary.LessonThumbnailLoader
import vn.loi.learning.domain.library.model.InstalledPackageId

@OptIn(ExperimentalTestApi::class)
class ContentStudioHeaderBackNavigationTest {
    @Test
    fun `center target keeps middle row centered and clamps list edges`() {
        assertEquals(45, centeredExplorerFirstIndex(selectedIndex = 50, itemCount = 100, visibleItemCount = 10))
        assertEquals(0, centeredExplorerFirstIndex(selectedIndex = 2, itemCount = 100, visibleItemCount = 10))
        assertEquals(90, centeredExplorerFirstIndex(selectedIndex = 99, itemCount = 100, visibleItemCount = 10))
    }

    @Test
    fun `production Content Studio header Back invokes the existing close callback`() {
        var closeInvocations = 0

        runComposeUiTest {
            setContent {
                ContentStudioScreen(
                    uiState = PackageContentBrowserUiState(
                        installedPackageId = InstalledPackageId("installed-package"),
                        packageName = "Vocabulary Package",
                        allItems = emptyList()
                    ),
                    onClose = { closeInvocations++ },
                    onSelectRow = {},
                    onQueryChanged = {},
                    onClearQuery = {},
                    onLessonFilterChanged = {},
                    onMediaFilterChanged = {},
                    onSortChanged = {},
                    onResetFilters = {},
                    thumbnailLoader = LessonThumbnailLoader(NoOpMediaStorage)
                )
            }

            onNodeWithContentDescription("Back to Library")
                .assertIsDisplayed()
                .performClick()
            waitForIdle()
        }

        assertEquals(1, closeInvocations)
    }

    @Test
    fun `Delete dialog Enter confirms exactly once`() {
        var confirmations = 0
        runComposeUiTest {
            setContent {
                ContentStudioScreen(
                    uiState = emptyStudioState(showDeleteConfirm = true),
                    onClose = {}, onSelectRow = {}, onQueryChanged = {}, onClearQuery = {},
                    onLessonFilterChanged = {}, onMediaFilterChanged = {}, onSortChanged = {},
                    onResetFilters = {}, onConfirmDelete = { confirmations++ },
                    thumbnailLoader = LessonThumbnailLoader(NoOpMediaStorage)
                )
            }
            waitForIdle()
            onNodeWithTag("delete-confirm-dialog").performKeyInput {
                pressKey(Key.Enter)
                pressKey(Key.Enter)
            }
            waitForIdle()
        }
        assertEquals(1, confirmations)
    }

    @Test
    fun `Delete dialog Escape dismisses without confirming`() {
        var confirmations = 0
        var dismissals = 0
        runComposeUiTest {
            setContent {
                ContentStudioScreen(
                    uiState = emptyStudioState(showDeleteConfirm = true),
                    onClose = {}, onSelectRow = {}, onQueryChanged = {}, onClearQuery = {},
                    onLessonFilterChanged = {}, onMediaFilterChanged = {}, onSortChanged = {},
                    onResetFilters = {}, onConfirmDelete = { confirmations++ },
                    onDismissDelete = { dismissals++ },
                    thumbnailLoader = LessonThumbnailLoader(NoOpMediaStorage)
                )
            }
            waitForIdle()
            onNodeWithTag("delete-confirm-dialog").performKeyInput { pressKey(Key.Escape) }
            waitForIdle()
        }
        assertEquals(0, confirmations)
        assertEquals(1, dismissals)
    }

    private fun emptyStudioState(showDeleteConfirm: Boolean = false) =
        PackageContentBrowserUiState(
            installedPackageId = InstalledPackageId("installed-package"),
            packageName = "Vocabulary Package",
            allItems = emptyList(),
            showDeleteConfirm = showDeleteConfirm
        )

    private object NoOpMediaStorage : ContentMediaStorage {
        override fun store(packageName: String, fileName: String, content: ByteArray): ContentMediaAsset =
            error("Not used by this presentation test")

        override fun resolve(relativePath: String): Path? = null

        override fun exists(relativePath: String): Boolean = false
    }
}
