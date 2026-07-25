package vn.loi.learning.desktop.ui.shell

import java.io.File
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.application.contentlibrary.ContentLibraryQueryService
import vn.loi.learning.application.contentpackaging.InstalledPackageQueryService
import vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserQueryService
import vn.loi.learning.desktop.ui.browser.PackageContentBrowserFacade
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

class PackageContentBrowserNavigationIntegrationTest {

    @Test
    fun `production route opens PackageContentBrowserUiState and leaves old lessonBrowserUiState null`() {
        val (appContext, instPkgId) = createFixture()
        val facade = ContentLibraryFacade(appContext)
        val lessonBrowserFacade = LessonBrowserFacade(appContext)
        val packageBrowserFacade = PackageContentBrowserFacade(queryService = appContext.packageBrowserQuery)

        val viewModel = ContentLibraryViewModel(
            facade = facade,
            lessonBrowserFacade = lessonBrowserFacade,
            packageBrowserFacade = packageBrowserFacade
        )

        assertNull(viewModel.packageBrowserUiState)
        assertNull(viewModel.lessonBrowserUiState)

        // User clicks Browse Lessons
        viewModel.browsePackageLessons(instPkgId, "Vocabulary Package")

        // Assert PackageContentBrowserUiState is active branch (which LibraryScreen prioritizes over lessonBrowserUiState)
        assertNotNull(viewModel.packageBrowserUiState)

        val state = viewModel.packageBrowserUiState!!
        assertEquals("Vocabulary Package", state.packageName)
        assertEquals(3, state.totalCount)
    }

    @Test
    fun `search vegetarian returns matching row and row selection populates preview panel`() {
        val (appContext, instPkgId) = createFixture()
        val packageBrowserFacade = PackageContentBrowserFacade(queryService = appContext.packageBrowserQuery)
        val viewModel = ContentLibraryViewModel(
            facade = ContentLibraryFacade(appContext),
            lessonBrowserFacade = LessonBrowserFacade(appContext),
            packageBrowserFacade = packageBrowserFacade
        )

        viewModel.browsePackageLessons(instPkgId, "Vocabulary Package")
        val browserState = viewModel.packageBrowserUiState
        assertNotNull(browserState)

        // 1. Search vegetarian
        viewModel.updatePackageBrowserQuery("vegetarian")
        val searchedState = viewModel.packageBrowserUiState!!
        assertEquals(1, searchedState.filteredItems.size)

        val item = searchedState.filteredItems.first()
        assertEquals("vegetarian", item.questionText)
        assertEquals("người ăn chay", item.answerText)
        assertEquals("ˌvedʒ.əˈteə.ri.ən", item.pronunciation)
        assertEquals("noun", item.partOfSpeech)

        // 2. Select row
        viewModel.selectPackageBrowserRow(item.contentId.value)
        val selectedItem = viewModel.packageBrowserUiState?.selectedItemInView
        assertNotNull(selectedItem)
        assertEquals("cnt-veg", selectedItem.contentId.value)
        assertEquals("vegetarian", selectedItem.questionText)
        assertEquals("người ăn chay", selectedItem.answerText)
        assertEquals("veg.png", selectedItem.imageRef)
        assertEquals("veg.mp3", selectedItem.audioRef)
    }

    @Test
    fun `clicking back to library closes browser and restores library view`() {
        val (appContext, instPkgId) = createFixture()
        val packageBrowserFacade = PackageContentBrowserFacade(queryService = appContext.packageBrowserQuery)
        val viewModel = ContentLibraryViewModel(
            facade = ContentLibraryFacade(appContext),
            lessonBrowserFacade = LessonBrowserFacade(appContext),
            packageBrowserFacade = packageBrowserFacade
        )

        viewModel.browsePackageLessons(instPkgId, "Vocabulary Package")
        assertNotNull(viewModel.packageBrowserUiState)

        // Click Back to Library
        viewModel.closePackageBrowser()

        assertNull(viewModel.packageBrowserUiState)
        assertNull(viewModel.lessonBrowserUiState)
        assertNull(viewModel.learningWorkspaceUiState)
    }

    private fun createFixture(): Pair<LearningApplicationContext, InstalledPackageId> {
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
            installedAt = Instant.now(),
            contentCount = 3,
            learningItemCount = 3
        )
        appContext.installedPackageRepository!!.save(instPkg)

        val vegContentId = ContentId("cnt-veg")
        val contentIds = setOf(ContentId("cnt-1"), ContentId("cnt-2"), vegContentId)

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

        appContext.contentRepository!!.save(
            Content(
                id = ContentId("cnt-1"),
                type = ContentType.WORD,
                text = ContentText("apple", "quả táo")
            )
        )
        appContext.contentRepository!!.save(
            Content(
                id = ContentId("cnt-2"),
                type = ContentType.WORD,
                text = ContentText("banana", "quả chuối")
            )
        )
        appContext.contentRepository!!.save(
            Content(
                id = vegContentId,
                type = ContentType.WORD,
                text = ContentText(
                    primaryText = "vegetarian",
                    translatedText = "người ăn chay",
                    pronunciation = "ˌvedʒ.əˈteə.ri.ən",
                    exampleText = "He is a vegetarian.",
                    exampleTranslation = "Anh ấy là người ăn chay."
                ),
                media = ContentMedia(primaryAudio = "veg.mp3", image = "veg.png"),
                metadata = ContentMetadata(lesson = "Diet & Food", group = "Food", tags = setOf("pos:noun", "food"))
            )
        )

        contentIds.forEachIndexed { i, cid ->
            appContext.learningItemRepository!!.save(
                LearningItem(
                    id = LearningItemId("item-$i"),
                    contentId = cid,
                    mode = LearningMode.MEANING_RECOGNITION
                )
            )
        }

        return appContext to instId
    }
}
