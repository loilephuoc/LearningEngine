package vn.loi.learning.desktop.ui.browser

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.application.contentpackaging.browser.BrowserMediaFilter
import vn.loi.learning.application.contentpackaging.browser.BrowserSortOption
import vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserProjectionPolicy
import vn.loi.learning.desktop.ui.contentlibrary.ContentLibraryFacade
import vn.loi.learning.desktop.ui.contentlibrary.ContentLibraryViewModel
import vn.loi.learning.desktop.ui.contentlibrary.LessonBrowserFacade
import vn.loi.learning.domain.content.library.model.ContentLibrary
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.library.model.LibraryDescriptor
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentMedia
import vn.loi.learning.domain.content.model.ContentMetadata
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.InstalledPackage
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.PackageName
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.library.model.PackageVersion
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.infrastructure.LearningApplicationContext
import vn.loi.learning.infrastructure.LearningApplicationFactory

class PackageContentBrowserViewModelTest {

    @Test
    fun `ui layer dependency direction guard ensures zero infrastructure imports in desktop ui browser`() {
        val rootDir = if (File("desktop").exists()) File(".") else File("..")
        val browserDir = File(rootDir, "desktop/src/main/kotlin/vn/loi/learning/desktop/ui/browser")
        assertTrue(browserDir.exists(), "Browser UI directory must exist at ${browserDir.absolutePath}")

        val forbiddenImports = listOf(
            "import vn.loi.learning.infrastructure",
            "import vn.loi.learning.adapter"
        )

        browserDir.walkTopDown().filter { it.extension == "kt" }.forEach { file ->
            val lines = file.readLines()
            forbiddenImports.forEach { forbidden ->
                val hasForbidden = lines.any { line -> line.trim().startsWith(forbidden) }
                assertFalse(hasForbidden, "File ${file.name} contains forbidden infrastructure import: $forbidden")
            }
        }
    }

    @Test
    fun `browsing package updates viewModel state and allows query filter sort reset selection`() {
        val (appContext, instPkgId) = createFixture(contentCount = 5)
        val facade = ContentLibraryFacade(appContext)
        val lessonBrowserFacade = LessonBrowserFacade(appContext)
        val packageBrowserFacade = PackageContentBrowserFacade(queryService = appContext.packageBrowserQuery)

        val viewModel = ContentLibraryViewModel(
            facade = facade,
            lessonBrowserFacade = lessonBrowserFacade,
            packageBrowserFacade = packageBrowserFacade
        )

        assertNull(viewModel.packageBrowserUiState)

        // 1. Browse package lessons
        viewModel.browsePackageLessons(instPkgId, "Vocabulary Package")

        val state = viewModel.packageBrowserUiState
        assertNotNull(state)
        assertEquals("Vocabulary Package", state.packageName)
        assertEquals(5, state.totalCount)
        assertEquals(5, state.filteredItems.size)

        // 2. Query filter
        viewModel.updatePackageBrowserQuery("Sentence 2")
        val queriedState = viewModel.packageBrowserUiState!!
        assertEquals(1, queriedState.filteredItems.size)
        assertEquals("Sentence 2", queriedState.filteredItems.first().questionText)

        // 3. Selection
        viewModel.selectPackageBrowserRow(queriedState.filteredItems.first().contentId.value)
        assertEquals("cnt-2", viewModel.packageBrowserUiState?.selectedContentId)

        // 4. Clear search
        viewModel.clearPackageBrowserQuery()
        assertEquals(5, viewModel.packageBrowserUiState?.filteredItems?.size)
        // Selection is preserved!
        assertEquals("cnt-2", viewModel.packageBrowserUiState?.selectedContentId)

        // 5. Close browser
        viewModel.closePackageBrowser()
        assertNull(viewModel.packageBrowserUiState)
    }

    @Test
    fun `selection is cleared safely when filtered out by query`() {
        val (appContext, instPkgId) = createFixture(contentCount = 3)
        val packageBrowserFacade = PackageContentBrowserFacade(queryService = appContext.packageBrowserQuery)
        val viewModel = ContentLibraryViewModel(
            facade = ContentLibraryFacade(appContext),
            lessonBrowserFacade = LessonBrowserFacade(appContext),
            packageBrowserFacade = packageBrowserFacade
        )

        viewModel.browsePackageLessons(instPkgId, "Vocabulary Package")
        viewModel.selectPackageBrowserRow("cnt-1")

        assertEquals("cnt-1", viewModel.packageBrowserUiState?.selectedItemInView?.contentId?.value)

        // Apply query that hides cnt-1
        viewModel.updatePackageBrowserQuery("Sentence 2")

        // selectedItemInView evaluates safely to null (not in view), while selectedItemAnywhere retains cnt-1
        assertNull(viewModel.packageBrowserUiState?.selectedItemInView)
        assertNotNull(viewModel.packageBrowserUiState?.selectedItemAnywhere)
    }

    @Test
    fun `browser 1_0 introduces zero edit or delete mutations on content`() {
        val (appContext, instPkgId) = createFixture(contentCount = 3)
        val initialContents = appContext.contentRepository!!.findAll()
        val initialItems = appContext.learningItemRepository!!.findAllEnabled()

        val packageBrowserFacade = PackageContentBrowserFacade(queryService = appContext.packageBrowserQuery)
        val viewModel = ContentLibraryViewModel(
            facade = ContentLibraryFacade(appContext),
            lessonBrowserFacade = LessonBrowserFacade(appContext),
            packageBrowserFacade = packageBrowserFacade
        )

        viewModel.browsePackageLessons(instPkgId, "Vocabulary Package")
        viewModel.updatePackageBrowserQuery("Sentence 1")
        viewModel.updatePackageBrowserLessonFilter("Lesson 1")
        viewModel.updatePackageBrowserMediaFilter(BrowserMediaFilter.HAS_IMAGE)
        viewModel.updatePackageBrowserSort(BrowserSortOption.QUESTION_ASC)
        viewModel.selectPackageBrowserRow("cnt-1")
        viewModel.closePackageBrowser()

        val finalContents = appContext.contentRepository!!.findAll()
        val finalItems = appContext.learningItemRepository!!.findAllEnabled()

        assertEquals(initialContents.size, finalContents.size)
        assertEquals(initialItems.size, finalItems.size)
    }

    @Test
    fun `large package scalability test with 2000 contents and 10000 learning items loads in memory without exception`() {
        val (appContext, instPkgId) = createFixture(contentCount = 2000)
        val packageBrowserFacade = PackageContentBrowserFacade(queryService = appContext.packageBrowserQuery)

        val state = packageBrowserFacade.loadForPackage(instPkgId, "2000Cau Large Package")

        assertEquals(2000, state.totalCount)
        assertEquals(2000, state.filteredItems.size)

        // Test search speed
        val filtered = PackageContentBrowserProjectionPolicy.filterAndSort(
            items = state.allItems,
            query = "Sentence 1999",
            lessonFilter = "ALL",
            mediaFilter = BrowserMediaFilter.ALL,
            sortOption = BrowserSortOption.ORIGINAL_ORDER
        )

        assertEquals(1, filtered.size)
        assertEquals("Sentence 1999", filtered.first().questionText)
    }

    private fun createFixture(contentCount: Int): Pair<LearningApplicationContext, InstalledPackageId> {
        val appContext = LearningApplicationFactory.createInMemory()

        val instId = InstalledPackageId("inst-test-pkg")
        val pkgId = PackageId("pkg-test")
        val libId = ContentLibraryId("lib-test")

        val instPkg = InstalledPackage.reconstitute(
            id = instId,
            libraryId = appContext.defaultLibraryId!!,
            packageId = pkgId,
            topicId = TopicId.deriveForLegacyPackage("Vocabulary Package", "OPD3"),
            name = PackageName("Vocabulary Package"),
            version = PackageVersion("1.0.0"),
            state = PackageState.ACTIVE,
            installedAt = java.time.Instant.now(),
            contentCount = contentCount,
            learningItemCount = contentCount * 5
        )
        appContext.installedPackageRepository!!.save(instPkg)

        val contentIds = (1..contentCount).map { ContentId("cnt-$it") }.toSet()
        appContext.contentPackageRepository!!.save(
            ContentPackage(
                id = pkgId,
                descriptor = PackageDescriptor(name = "Vocabulary Package", version = "1.0.0", format = "OPD3"),
                libraryIds = setOf(libId)
            )
        )
        appContext.contentLibraryRepository!!.save(
            ContentLibrary(
                id = libId,
                descriptor = LibraryDescriptor(name = "Vocabulary Library"),
                contentIds = contentIds
            )
        )

        for (i in 1..contentCount) {
            val cid = ContentId("cnt-$i")
            appContext.contentRepository!!.save(
                Content(
                    id = cid,
                    type = ContentType.SENTENCE,
                    text = ContentText(primaryText = "Sentence $i", translatedText = "Cau $i", pronunciation = "p-$i"),
                    media = ContentMedia(primaryAudio = if (i % 2 == 0) "audio-$i.mp3" else null, image = if (i % 3 == 0) "img-$i.jpg" else null),
                    metadata = ContentMetadata(lesson = "Lesson ${i % 10}", group = "Group ${i % 5}", tags = setOf("tag-$i"))
                )
            )
            // 5 items per content
            for (m in 1..5) {
                appContext.learningItemRepository!!.save(
                    LearningItem(
                        id = LearningItemId("item-$i-$m"),
                        contentId = cid,
                        mode = LearningMode.entries[(m - 1) % LearningMode.entries.size]
                    )
                )
            }
        }

        return appContext to instId
    }
}
