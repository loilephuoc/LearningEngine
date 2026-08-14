package vn.loi.learning.desktop.ui.studio

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
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

    private object NoOpMediaStorage : ContentMediaStorage {
        override fun store(packageName: String, fileName: String, content: ByteArray): ContentMediaAsset =
            error("Not used by this presentation test")

        override fun resolve(relativePath: String): Path? = null

        override fun exists(relativePath: String): Boolean = false
    }
}
