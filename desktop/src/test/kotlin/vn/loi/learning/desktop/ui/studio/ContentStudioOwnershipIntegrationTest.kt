package vn.loi.learning.desktop.ui.studio

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.application.contentpackaging.InstalledPackageQueryService
import vn.loi.learning.application.contentpackaging.browser.ContentBrowserEditService
import vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserQueryService
import vn.loi.learning.desktop.ui.browser.ContentDraftEdits
import vn.loi.learning.desktop.ui.browser.PackageContentBrowserFacade
import vn.loi.learning.desktop.ui.contentlibrary.ContentLibraryFacade
import vn.loi.learning.desktop.ui.contentlibrary.ContentLibraryViewModel
import vn.loi.learning.desktop.ui.contentlibrary.LessonBrowserFacade
import vn.loi.learning.domain.content.library.model.ContentLibrary
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.library.model.LibraryDescriptor
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.InstalledPackage
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.LibraryId
import vn.loi.learning.domain.library.model.PackageName
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.library.model.PackageVersion
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.infrastructure.LearningApplicationFactory
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentLibraryRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentPackageRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryInstalledPackageRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryLearningItemRepository

/**
 * PLE-019 Integration Tests — New Content Ownership Fix
 *
 * Production-like fixture where IDs are deliberately different:
 *   InstalledPackage.id        = "installed-vocabulary"
 *   InstalledPackage.libraryId = "main-user-library"          ← NOT a ContentLibraryId
 *   InstalledPackage.packageId = "vocabulary-package"
 *   ContentPackage.id          = "vocabulary-package"
 *   ContentPackage.libraryIds  = ["vocabulary-content-library"]
 *   ContentLibrary.id          = "vocabulary-content-library"
 *
 * Created Content MUST be registered in "vocabulary-content-library".
 * It must NOT be registered in "main-user-library", "installed-vocabulary",
 * or "vocabulary-package".
 */
class ContentStudioOwnershipIntegrationTest {

    // -----------------------------------------------------------------------
    // Shared fixture IDs (production-like, deliberately distinct)
    // -----------------------------------------------------------------------

    private val INSTALLED_PACKAGE_ID     = InstalledPackageId("installed-vocabulary")
    private val INSTALLED_PACKAGE_LIB_ID = LibraryId("main-user-library")           // NOT a ContentLibraryId
    private val PACKAGE_ID               = PackageId("vocabulary-package")
    private val CONTENT_LIB_ID           = ContentLibraryId("vocabulary-content-library")

    // Wrong IDs that must NOT receive the new Content
    private val WRONG_LIB_ID_MAIN      = ContentLibraryId("main-user-library")
    private val WRONG_LIB_ID_INST      = ContentLibraryId("installed-vocabulary")
    private val WRONG_LIB_ID_PKG       = ContentLibraryId("vocabulary-package")

    private val EXISTING_CONTENT_ID = ContentId("cnt-existing-vocab")

    // -----------------------------------------------------------------------
    // Fixture builder
    // -----------------------------------------------------------------------

    private data class Fixture(
        val instRepo: InMemoryInstalledPackageRepository,
        val pkgRepo: InMemoryContentPackageRepository,
        val libRepo: InMemoryContentLibraryRepository,
        val contentRepo: InMemoryContentRepository,
        val itemRepo: InMemoryLearningItemRepository,
        val editService: ContentBrowserEditService,
        val queryService: PackageContentBrowserQueryService,
        val facade: PackageContentBrowserFacade
    )

    private fun buildFixture(): Fixture {
        val instRepo    = InMemoryInstalledPackageRepository()
        val pkgRepo     = InMemoryContentPackageRepository()
        val libRepo     = InMemoryContentLibraryRepository()
        val contentRepo = InMemoryContentRepository()
        val itemRepo    = InMemoryLearningItemRepository()

        instRepo.save(
            InstalledPackage.reconstitute(
                id                = INSTALLED_PACKAGE_ID,
                libraryId         = INSTALLED_PACKAGE_LIB_ID,
                packageId         = PACKAGE_ID,
                topicId           = TopicId.deriveForLegacyPackage("Vocabulary Package", "OPD3"),
                name              = PackageName("Vocabulary Package"),
                version           = PackageVersion("1.0.0"),
                state             = PackageState.ACTIVE,
                installedAt       = Instant.now(),
                contentCount      = 1,
                learningItemCount = 1
            )
        )

        pkgRepo.save(
            ContentPackage(
                id         = PACKAGE_ID,
                descriptor = PackageDescriptor(name = "Vocabulary Package", version = "1.0.0", format = "OPD3"),
                libraryIds = setOf(CONTENT_LIB_ID)
            )
        )

        libRepo.save(
            ContentLibrary(
                id         = CONTENT_LIB_ID,
                descriptor = LibraryDescriptor(name = "Vocabulary Content Library"),
                contentIds = setOf(EXISTING_CONTENT_ID)
            )
        )

        contentRepo.save(
            Content(
                id   = EXISTING_CONTENT_ID,
                type = ContentType.WORD,
                text = ContentText(primaryText = "existing-word", translatedText = "nghĩa")
            )
        )
        itemRepo.save(
            LearningItem(
                id        = LearningItemId("item-existing-vocab"),
                contentId = EXISTING_CONTENT_ID,
                mode      = LearningMode.MEANING_RECOGNITION
            )
        )

        val editService = ContentBrowserEditService(
            contentRepository          = contentRepo,
            contentLibraryRepository   = libRepo,
            installedPackageRepository = instRepo,
            contentPackageRepository   = pkgRepo,
            transactionRunner          = vn.loi.learning.infrastructure.transaction.InMemoryTransactionRunner()
        )

        val queryService = PackageContentBrowserQueryService(
            installedPackageRepository = instRepo,
            installedPackages          = InstalledPackageQueryService(pkgRepo),
            contentPackageRepository   = pkgRepo,
            contentLibraryRepository   = libRepo,
            contentRepository          = contentRepo,
            learningItemRepository     = itemRepo
        )

        val facade = PackageContentBrowserFacade(
            queryService           = queryService,
            editService            = editService,
            learningItemRepository = itemRepo
        )

        return Fixture(instRepo, pkgRepo, libRepo, contentRepo, itemRepo, editService, queryService, facade)
    }

    // -----------------------------------------------------------------------
    // TC-OWN01: createContent registers new ContentId in the canonical library
    // -----------------------------------------------------------------------
    @Test
    fun `TC-OWN01 createContent registers new ContentId in vocabulary-content-library`() {
        val f = buildFixture()

        val created = f.editService.createContent(
            installedPackageId     = INSTALLED_PACKAGE_ID,
            questionText           = "new-question",
            answerText             = "new-answer",
            learningItemRepository = f.itemRepo
        )

        val lib = f.libRepo.findById(CONTENT_LIB_ID)!!
        assertTrue(
            lib.contains(created.id),
            "Expected new ContentId ${created.id.value} to be in $CONTENT_LIB_ID"
        )
    }

    // -----------------------------------------------------------------------
    // TC-OWN02: createContent does NOT register into the wrong/main library
    // -----------------------------------------------------------------------
    @Test
    fun `TC-OWN02 createContent does not register into main-user-library or wrong library IDs`() {
        val f = buildFixture()

        f.editService.createContent(
            installedPackageId     = INSTALLED_PACKAGE_ID,
            questionText           = "wrong-lib-check",
            answerText             = "answer",
            learningItemRepository = f.itemRepo
        )

        // These libraries must not exist as ContentLibraries (were never seeded)
        assertNull(f.libRepo.findById(WRONG_LIB_ID_MAIN), "main-user-library must not exist as a ContentLibrary")
        assertNull(f.libRepo.findById(WRONG_LIB_ID_INST), "installed-vocabulary must not exist as a ContentLibrary")
        assertNull(f.libRepo.findById(WRONG_LIB_ID_PKG),  "vocabulary-package must not exist as a ContentLibrary")
    }

    // -----------------------------------------------------------------------
    // TC-OWN03: ContentRepository contains the new Content
    // -----------------------------------------------------------------------
    @Test
    fun `TC-OWN03 ContentRepository contains the new Content after create`() {
        val f = buildFixture()

        val created = f.editService.createContent(
            installedPackageId     = INSTALLED_PACKAGE_ID,
            questionText           = "persisted-question",
            answerText             = "persisted-answer",
            learningItemRepository = f.itemRepo
        )

        val found = f.contentRepo.findById(created.id)
        assertNotNull(found, "Content must be in ContentRepository")
        assertEquals("persisted-question", found.text.primaryText)
        assertEquals("persisted-answer", found.text.translatedText)
    }

    // -----------------------------------------------------------------------
    // TC-OWN04: LearningItemRepository contains the owned LearningItem
    // -----------------------------------------------------------------------
    @Test
    fun `TC-OWN04 LearningItemRepository contains the owned LearningItem`() {
        val f = buildFixture()

        val created = f.editService.createContent(
            installedPackageId     = INSTALLED_PACKAGE_ID,
            questionText           = "item-question",
            answerText             = "item-answer",
            learningItemRepository = f.itemRepo
        )

        val items = f.itemRepo.findAllEnabled().filter { it.contentId == created.id }
        assertEquals(1, items.size, "Exactly one LearningItem must be created for the new Content")
        assertEquals(LearningMode.MEANING_RECOGNITION, items.first().mode)
    }

    // -----------------------------------------------------------------------
    // TC-OWN05: InstalledPackage contentCount increases by 1
    // -----------------------------------------------------------------------
    @Test
    fun `TC-OWN05 InstalledPackage contentCount increases by exactly 1`() {
        val f = buildFixture()
        val before = f.instRepo.findById(INSTALLED_PACKAGE_ID)!!.contentCount

        f.editService.createContent(
            installedPackageId     = INSTALLED_PACKAGE_ID,
            questionText           = "count-check",
            answerText             = "answer",
            learningItemRepository = f.itemRepo
        )

        val after = f.instRepo.findById(INSTALLED_PACKAGE_ID)!!.contentCount
        assertEquals(before + 1, after, "contentCount must increase by 1")
    }

    // -----------------------------------------------------------------------
    // TC-OWN06: InstalledPackage learningItemCount increases correctly
    // -----------------------------------------------------------------------
    @Test
    fun `TC-OWN06 InstalledPackage learningItemCount increases correctly`() {
        val f = buildFixture()
        val before = f.instRepo.findById(INSTALLED_PACKAGE_ID)!!.learningItemCount

        f.editService.createContent(
            installedPackageId     = INSTALLED_PACKAGE_ID,
            questionText           = "item-count-check",
            answerText             = "answer",
            learningItemRepository = f.itemRepo
        )

        val after = f.instRepo.findById(INSTALLED_PACKAGE_ID)!!.learningItemCount
        assertEquals(before + 1, after, "learningItemCount must increase by 1")
    }

    // -----------------------------------------------------------------------
    // TC-OWN07: PackageContentBrowserQueryService returns the new item
    // -----------------------------------------------------------------------
    @Test
    fun `TC-OWN07 PackageContentBrowserQueryService returns the new Content after create`() {
        val f = buildFixture()

        val created = f.editService.createContent(
            installedPackageId     = INSTALLED_PACKAGE_ID,
            questionText           = "query-visible",
            answerText             = "answer",
            learningItemRepository = f.itemRepo
        )

        val items = f.queryService.getBrowserItemsForPackage(INSTALLED_PACKAGE_ID)
        assertTrue(
            items.any { it.contentId == created.id },
            "QueryService must return the newly created Content"
        )
    }

    // -----------------------------------------------------------------------
    // TC-OWN08: PackageContentBrowserFacade reload contains created.id
    // -----------------------------------------------------------------------
    @Test
    fun `TC-OWN08 PackageContentBrowserFacade reload contains created ContentId`() {
        val f = buildFixture()
        val draft = ContentDraftEdits(
            contentId    = "",
            questionText = "facade-reload-check",
            answerText   = "answer"
        )

        val result = f.facade.createContent(
            draft              = draft,
            installedPackageId = INSTALLED_PACKAGE_ID,
            packageName        = "Vocabulary Package"
        )

        val createdId = result.selectedContentId
        assertNotNull(createdId)
        assertTrue(result.allItems.any { it.contentId.value == createdId })
        assertEquals(2, result.allItems.size, "Browser must show 2 items after create (1 existing + 1 new)")
    }

    // -----------------------------------------------------------------------
    // TC-OWN09: Search by exact Question finds the created item
    // -----------------------------------------------------------------------
    @Test
    fun `TC-OWN09 Search by exact new Question finds the created item`() {
        val f = buildFixture()

        val created = f.editService.createContent(
            installedPackageId     = INSTALLED_PACKAGE_ID,
            questionText           = "unique-search-target",
            answerText             = "search-answer",
            learningItemRepository = f.itemRepo
        )

        val items = f.queryService.getBrowserItemsForPackage(INSTALLED_PACKAGE_ID)
        val found = items.filter {
            it.searchableText.contains("unique-search-target", ignoreCase = true)
        }
        assertEquals(1, found.size, "Exact search should find exactly one result")
        assertEquals(created.id, found.first().contentId)
    }

    // -----------------------------------------------------------------------
    // TC-OWN10: Repository reload still finds the item (simulated restart)
    // -----------------------------------------------------------------------
    @Test
    fun `TC-OWN10 Repository reload still returns the created Content after restart simulation`() {
        val f = buildFixture()

        val created = f.editService.createContent(
            installedPackageId     = INSTALLED_PACKAGE_ID,
            questionText           = "restart-persistence",
            answerText             = "restart-answer",
            learningItemRepository = f.itemRepo
        )

        // Simulate restart: re-query from the same in-memory repos
        val reloaded = f.contentRepo.findById(created.id)
        assertNotNull(reloaded, "Content must still be findable after simulated restart")
        assertEquals("restart-persistence", reloaded.text.primaryText)

        val libAfter = f.libRepo.findById(CONTENT_LIB_ID)!!
        assertTrue(libAfter.contains(created.id), "ContentLibrary must still contain the ContentId")

        val queryItems = f.queryService.getBrowserItemsForPackage(INSTALLED_PACKAGE_ID)
        assertTrue(queryItems.any { it.contentId == created.id })
    }

    // -----------------------------------------------------------------------
    // TC-OWN11: Missing ContentPackage fails explicitly
    // -----------------------------------------------------------------------
    @Test
    fun `TC-OWN11 createContent fails explicitly when ContentPackage is missing`() {
        val instRepo    = InMemoryInstalledPackageRepository()
        val pkgRepo     = InMemoryContentPackageRepository()  // empty — no ContentPackage
        val libRepo     = InMemoryContentLibraryRepository()
        val contentRepo = InMemoryContentRepository()

        instRepo.save(
            InstalledPackage.reconstitute(
                id                = INSTALLED_PACKAGE_ID,
                libraryId         = INSTALLED_PACKAGE_LIB_ID,
                packageId         = PACKAGE_ID,
                topicId           = TopicId.deriveForLegacyPackage("Missing Pkg", "OPD3"),
                name              = PackageName("Missing Pkg"),
                version           = PackageVersion("1.0.0"),
                state             = PackageState.ACTIVE,
                installedAt       = Instant.now(),
                contentCount      = 0,
                learningItemCount = 0
            )
        )
        // NOTE: no ContentPackage saved

        val editService = ContentBrowserEditService(
            contentRepository          = contentRepo,
            contentLibraryRepository   = libRepo,
            installedPackageRepository = instRepo,
            contentPackageRepository   = pkgRepo
        )

        val ex = assertFailsWith<IllegalStateException> {
            editService.createContent(
                installedPackageId = INSTALLED_PACKAGE_ID,
                questionText       = "orphan",
                answerText         = "answer"
            )
        }
        assertTrue(
            ex.message?.contains("ContentPackage not found") == true,
            "Exception must mention ContentPackage not found, was: ${ex.message}"
        )
    }

    // -----------------------------------------------------------------------
    // TC-OWN12: Missing package ContentLibrary fails explicitly
    // -----------------------------------------------------------------------
    @Test
    fun `TC-OWN12 createContent fails explicitly when package ContentLibrary is missing`() {
        val instRepo    = InMemoryInstalledPackageRepository()
        val pkgRepo     = InMemoryContentPackageRepository()
        val libRepo     = InMemoryContentLibraryRepository()  // ContentLibrary NOT saved
        val contentRepo = InMemoryContentRepository()

        instRepo.save(
            InstalledPackage.reconstitute(
                id                = INSTALLED_PACKAGE_ID,
                libraryId         = INSTALLED_PACKAGE_LIB_ID,
                packageId         = PACKAGE_ID,
                topicId           = TopicId.deriveForLegacyPackage("Missing Lib Pkg", "OPD3"),
                name              = PackageName("Missing Lib Pkg"),
                version           = PackageVersion("1.0.0"),
                state             = PackageState.ACTIVE,
                installedAt       = Instant.now(),
                contentCount      = 0,
                learningItemCount = 0
            )
        )
        pkgRepo.save(
            ContentPackage(
                id         = PACKAGE_ID,
                descriptor = PackageDescriptor(name = "Missing Lib Pkg", version = "1.0.0", format = "OPD3"),
                libraryIds = setOf(CONTENT_LIB_ID)  // referenced but not saved in libRepo
            )
        )

        val editService = ContentBrowserEditService(
            contentRepository          = contentRepo,
            contentLibraryRepository   = libRepo,
            installedPackageRepository = instRepo,
            contentPackageRepository   = pkgRepo
        )

        val ex = assertFailsWith<IllegalStateException> {
            editService.createContent(
                installedPackageId = INSTALLED_PACKAGE_ID,
                questionText       = "orphan",
                answerText         = "answer"
            )
        }
        assertTrue(
            ex.message?.contains("No writable ContentLibrary found") == true,
            "Exception must mention no writable ContentLibrary, was: ${ex.message}"
        )
    }

    // -----------------------------------------------------------------------
    // TC-OWN13: Facade throws if created Content is not query-visible
    // -----------------------------------------------------------------------
    @Test
    fun `TC-OWN13 Facade throws if created Content is not query-visible after reload`() {
        val f = buildFixture()

        // Build a broken facade: editService writes to a shadow libRepo
        // that the queryService cannot see → post-condition must fail
        val shadowLibRepo = InMemoryContentLibraryRepository()
        shadowLibRepo.save(
            ContentLibrary(
                id         = CONTENT_LIB_ID,
                descriptor = LibraryDescriptor(name = "Shadow Library"),
                contentIds = setOf(EXISTING_CONTENT_ID)
            )
        )
        val brokenEditService = ContentBrowserEditService(
            contentRepository          = f.contentRepo,
            contentLibraryRepository   = shadowLibRepo,  // writes to shadow, not real libRepo
            installedPackageRepository = f.instRepo,
            contentPackageRepository   = f.pkgRepo,
            transactionRunner          = vn.loi.learning.infrastructure.transaction.InMemoryTransactionRunner()
        )
        val brokenFacade = PackageContentBrowserFacade(
            queryService           = f.queryService,    // reads from real libRepo
            editService            = brokenEditService,  // writes to shadow libRepo
            learningItemRepository = f.itemRepo
        )

        val ex = assertFailsWith<IllegalStateException> {
            brokenFacade.createContent(
                draft              = ContentDraftEdits(contentId = "", questionText = "ghost", answerText = "ghost"),
                installedPackageId = INSTALLED_PACKAGE_ID,
                packageName        = "Vocabulary Package"
            )
        }
        assertTrue(
            ex.message?.contains("not visible in package ownership") == true ||
                ex.message?.contains("not registered") == true,
            "Exception must indicate visibility failure, was: ${ex.message}"
        )
    }

    // -----------------------------------------------------------------------
    // TC-OWN14: A committed Create with a projection failure cannot be resubmitted
    // -----------------------------------------------------------------------
    @Test
    fun `TC-OWN14 ViewModel recognizes committed Create when projection refresh fails`() {
        val appContext = LearningApplicationFactory.createInMemory()

        val instId = InstalledPackageId("inst-vm-own-fail")
        val pkgId  = PackageId("pkg-vm-own-fail")
        val libId  = ContentLibraryId("lib-vm-own-fail")

        appContext.installedPackageRepository!!.save(
            InstalledPackage.reconstitute(
                id                = instId,
                libraryId         = appContext.defaultLibraryId!!,
                packageId         = pkgId,
                topicId           = TopicId.deriveForLegacyPackage("VM Own Fail Package", "OPD3"),
                name              = PackageName("VM Own Fail Package"),
                version           = PackageVersion("1.0.0"),
                state             = PackageState.ACTIVE,
                installedAt       = Instant.now(),
                contentCount      = 0,
                learningItemCount = 0
            )
        )
        appContext.contentPackageRepository!!.save(
            ContentPackage(
                id         = pkgId,
                descriptor = PackageDescriptor(name = "VM Own Fail Package", version = "1.0.0", format = "OPD3"),
                libraryIds = setOf(libId)
            )
        )
        appContext.contentLibraryRepository!!.save(
            ContentLibrary(
                id         = libId,
                descriptor = LibraryDescriptor(name = "VM Own Fail Library"),
                contentIds = emptySet()
            )
        )

        // editService writes to shadow repo → queryService sees different libRepo → post-condition fails
        val shadowLibRepo = InMemoryContentLibraryRepository()
        shadowLibRepo.save(
            ContentLibrary(id = libId, descriptor = LibraryDescriptor(name = "Shadow"), contentIds = emptySet())
        )
        val brokenEditService = ContentBrowserEditService(
            contentRepository          = appContext.contentRepository!!,
            contentLibraryRepository   = shadowLibRepo,
            installedPackageRepository = appContext.installedPackageRepository,
            contentPackageRepository   = appContext.contentPackageRepository,
            transactionRunner          = requireNotNull(appContext.transactionRunner)
        )
        val packageBrowserFacade = PackageContentBrowserFacade(
            queryService           = appContext.packageBrowserQuery,
            editService            = brokenEditService,
            learningItemRepository = appContext.learningItemRepository
        )

        val vm = ContentLibraryViewModel(
            facade               = ContentLibraryFacade(appContext),
            lessonBrowserFacade  = LessonBrowserFacade(appContext),
            packageBrowserFacade = packageBrowserFacade
        )

        vm.browsePackageLessons(instId, "VM Own Fail Package")
        vm.startNewItem()
        vm.updateDraftQuestion("fail-draft-question")
        vm.updateDraftAnswer("fail-draft-answer")
        vm.saveNewItem()

        val afterFailure = vm.packageBrowserUiState!!
        assertFalse(afterFailure.isCreatingNewItem)
        assertNull(afterFailure.draftEdits)
        assertTrue(vm.uiState.importMessage?.contains("committed") == true)
    }

    // -----------------------------------------------------------------------
    // TC-OWN15: Existing edit/delete workflows are unaffected (regression guard)
    // -----------------------------------------------------------------------
    @Test
    fun `TC-OWN15 Existing edit and delete workflows are unaffected by create fix`() {
        val appContext = LearningApplicationFactory.createInMemory()
        val instId    = InstalledPackageId("inst-own-regression")
        val pkgId     = PackageId("pkg-own-regression")
        val libId     = ContentLibraryId("lib-own-regression")

        appContext.installedPackageRepository!!.save(
            InstalledPackage.reconstitute(
                id                = instId,
                libraryId         = appContext.defaultLibraryId!!,
                packageId         = pkgId,
                topicId           = TopicId.deriveForLegacyPackage("Regression Package", "OPD3"),
                name              = PackageName("Regression Package"),
                version           = PackageVersion("1.0.0"),
                state             = PackageState.ACTIVE,
                installedAt       = Instant.now(),
                contentCount      = 2,
                learningItemCount = 2
            )
        )
        appContext.contentPackageRepository!!.save(
            ContentPackage(
                id         = pkgId,
                descriptor = PackageDescriptor(name = "Regression Package", version = "1.0.0", format = "OPD3"),
                libraryIds = setOf(libId)
            )
        )
        val cntR1 = ContentId("cnt-own-r1")
        val cntR2 = ContentId("cnt-own-r2")
        appContext.contentLibraryRepository!!.save(
            ContentLibrary(
                id         = libId,
                descriptor = LibraryDescriptor(name = "Regression Library"),
                contentIds = setOf(cntR1, cntR2)
            )
        )
        for (id in listOf(cntR1, cntR2)) {
            appContext.contentRepository!!.save(
                Content(id = id, type = ContentType.WORD, text = ContentText(primaryText = "Word ${id.value}"))
            )
            appContext.learningItemRepository!!.save(
                LearningItem(
                    id        = LearningItemId("item-own-${id.value}"),
                    contentId = id,
                    mode      = LearningMode.MEANING_RECOGNITION
                )
            )
        }

        val editService = ContentBrowserEditService(
            contentRepository          = appContext.contentRepository!!,
            contentLibraryRepository   = appContext.contentLibraryRepository,
            installedPackageRepository = appContext.installedPackageRepository,
            contentPackageRepository   = appContext.contentPackageRepository,
            transactionRunner          = requireNotNull(appContext.transactionRunner)
        )

        // Update works
        editService.updateTextFields(
            contentId          = cntR1,
            questionText       = "Updated Word",
            answerText         = "Updated Answer",
            pronunciation      = "",
            partOfSpeech       = "",
            exampleText        = "",
            exampleTranslation = ""
        )
        assertEquals("Updated Word", appContext.contentRepository!!.findById(cntR1)!!.text.primaryText)

        // Delete works
        editService.deleteContent(
            contentId              = cntR2,
            learningItemRepository = appContext.learningItemRepository!!,
            installedPackageId     = instId
        )
        assertNull(appContext.contentRepository!!.findById(cntR2))
        val lib = appContext.contentLibraryRepository!!.findById(libId)!!
        assertFalse(lib.contains(cntR2))
        val instAfterDelete = appContext.installedPackageRepository!!.findById(instId)!!
        assertEquals(1, instAfterDelete.contentCount)
    }
}
