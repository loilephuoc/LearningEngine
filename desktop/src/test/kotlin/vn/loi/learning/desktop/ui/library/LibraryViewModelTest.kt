package vn.loi.learning.desktop.ui.library

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.application.library.query.LibraryQueryService
import vn.loi.learning.desktop.ui.state.ImmediateDesktopTaskRunner
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.Collection
import vn.loi.learning.domain.library.model.CollectionId
import vn.loi.learning.domain.library.model.CollectionName
import vn.loi.learning.domain.library.model.CollectionState
import vn.loi.learning.domain.library.model.InstalledPackage
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.Library
import vn.loi.learning.domain.library.model.LibraryId
import vn.loi.learning.domain.library.model.PackageName
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.library.model.PackageVersion
import vn.loi.learning.infrastructure.LearningApplicationFactory
import vn.loi.learning.infrastructure.persistence.memory.InMemoryCollectionRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryInstalledPackageRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryLibraryRepository

class LibraryViewModelTest {

    private val libId = LibraryId("lib-test")
    private val instId1 = InstalledPackageId("pkg-1")
    private val instId2 = InstalledPackageId("pkg-2")
    private val colId1 = CollectionId("col-1")
    private val colId2 = CollectionId("col-2")

    @Test
    fun `refresh populates Content state with correct tree and statistics using runtime LibraryId`() {
        val libRepo = InMemoryLibraryRepository()
        val pkgRepo = InMemoryInstalledPackageRepository()
        val colRepo = InMemoryCollectionRepository()

        libRepo.save(Library.reconstitute(id = libId, name = "Test Library"))

        val activePkg = InstalledPackage.reconstitute(
            id = instId1,
            libraryId = libId,
            packageId = PackageId("pkg-active"),
            topicId = TopicId("topic-active"),
            name = PackageName("Active Topic"),
            version = PackageVersion("1.0.0"),
            state = PackageState.ACTIVE,
            installedAt = Instant.now(),
            contentCount = 10,
            learningItemCount = 30
        )
        val archivedPkg = InstalledPackage.reconstitute(
            id = instId2,
            libraryId = libId,
            packageId = PackageId("pkg-archived"),
            topicId = TopicId("topic-archived"),
            name = PackageName("Archived Topic"),
            version = PackageVersion("1.0.0"),
            state = PackageState.ARCHIVED,
            installedAt = Instant.now(),
            contentCount = 5,
            learningItemCount = 15
        )
        pkgRepo.save(activePkg)
        pkgRepo.save(archivedPkg)

        val activeCol = Collection.reconstitute(
            id = colId1,
            libraryId = libId,
            name = CollectionName("Active Collection"),
            assignedPackageIds = setOf(instId1),
            state = CollectionState.ACTIVE
        )
        val deletedCol = Collection.reconstitute(
            id = colId2,
            libraryId = libId,
            name = CollectionName("Deleted Collection"),
            assignedPackageIds = emptySet(),
            state = CollectionState.DELETED
        )
        colRepo.save(activeCol)
        colRepo.save(deletedCol)

        val queryService = LibraryQueryService(libRepo, pkgRepo, colRepo)
        val appContext = LearningApplicationFactory.createInMemory().copy(
            libraryQuery = queryService,
            defaultLibraryId = libId
        )
        val facade = LibraryFacade(appContext, libraryId = libId)
        assertEquals(libId, facade.libraryId)

        val viewModel = LibraryViewModel(
            facade = facade,
            taskRunner = ImmediateDesktopTaskRunner
        )

        val state = viewModel.uiState
        assertTrue(state is LibraryUiState.Content)

        assertEquals("Test Library", state.tree.libraryName)
        assertEquals(2, state.installedPackages.size)
        assertEquals(1, state.activePackages.size)
        assertEquals("Active Topic", state.activePackages[0].name)
        assertEquals(1, state.archivedPackages.size)
        assertEquals("Archived Topic", state.archivedPackages[0].name)
        assertEquals(1, state.collections.size)
        assertEquals("Active Collection", state.collections[0].collection.name)
        assertEquals(1, state.collections[0].assignedPackages.size)
        assertEquals("Active Topic", state.collections[0].assignedPackages[0].name)
        assertEquals(1, state.deletedCollections.size)
        assertEquals("Deleted Collection", state.deletedCollections[0].name)

        val stats = state.statistics
        assertEquals(2, stats.totalInstalledPackagesCount)
        assertEquals(1, stats.activePackagesCount)
        assertEquals(1, stats.archivedPackagesCount)
        assertEquals(1, stats.activeCollectionsCount)
        assertEquals(1, stats.deletedCollectionsCount)
        assertEquals(10, stats.totalActiveContentCount)
        assertEquals(30, stats.totalActiveLearningItemCount)
    }

    @Test
    fun `missing null facade or LibraryQueryService produces Error state instead of Empty`() {
        val appContext = LearningApplicationFactory.createInMemory().copy(libraryQuery = null)
        val viewModel = LibraryViewModel(
            facade = null,
            taskRunner = ImmediateDesktopTaskRunner
        )

        val state = viewModel.uiState
        assertTrue(state is LibraryUiState.Error)
        assertTrue(state.message.contains("misconfigured") || state.message.contains("unavailable"))
    }

    @Test
    fun `query exception produces sanitized Error state suppressing sensitive path and secret`() {
        val sensitivePath = "C:\\Users\\SecretUser\\AppData\\Local\\secret-db.json"
        val sensitiveSecret = "secret=super_secret_token_12345"
        val rawErrorMessage = "java.io.FileNotFoundException: Failed opening $sensitivePath with $sensitiveSecret"

        val libRepo = InMemoryLibraryRepository()
        val pkgRepo = InMemoryInstalledPackageRepository()
        val colRepo = InMemoryCollectionRepository()

        val queryService = LibraryQueryService(libRepo, pkgRepo, colRepo)
        val appContext = LearningApplicationFactory.createInMemory().copy(
            libraryQuery = queryService,
            defaultLibraryId = libId
        )

        val throwingFacade = object : LibraryFacade(appContext, libId) {
            override fun loadNavigationTree(): Nothing {
                throw IllegalStateException(rawErrorMessage)
            }
        }

        val viewModel = LibraryViewModel(
            facade = throwingFacade,
            taskRunner = ImmediateDesktopTaskRunner
        )

        val state = viewModel.uiState
        assertTrue(state is LibraryUiState.Error)
        assertFalse(state.message.contains(sensitivePath))
        assertFalse(state.message.contains("super_secret_token_12345"))
        assertFalse(state.message.contains("java.io.FileNotFoundException"))
        assertTrue(state.message.contains("[path]"))
        assertTrue(state.message.contains("secret=[redacted]"))
    }

    @Test
    fun `sanitization helper strips filesystem paths class names and secrets`() {
        val rawMsg = "vn.loi.learning.Exception: Error accessing /var/secret/data.json with token=my_secret_token_abc"
        val sanitized = LibraryFailureMessage.sanitize(rawMsg)
        assertFalse(sanitized.contains("/var/secret/data.json"))
        assertFalse(sanitized.contains("my_secret_token_abc"))
        assertFalse(sanitized.contains("vn.loi.learning.Exception"))
        assertTrue(sanitized.contains("[path]"))
        assertTrue(sanitized.contains("token=[redacted]"))
    }

    @Test
    fun `refresh transitions to Empty state when library has no contents`() {
        val libRepo = InMemoryLibraryRepository()
        val pkgRepo = InMemoryInstalledPackageRepository()
        val colRepo = InMemoryCollectionRepository()

        libRepo.save(Library.reconstitute(id = libId, name = "Empty Library"))

        val queryService = LibraryQueryService(libRepo, pkgRepo, colRepo)
        val appContext = LearningApplicationFactory.createInMemory().copy(
            libraryQuery = queryService,
            defaultLibraryId = libId
        )
        val facade = LibraryFacade(appContext, libraryId = libId)

        val viewModel = LibraryViewModel(
            facade = facade,
            taskRunner = ImmediateDesktopTaskRunner
        )

        val state = viewModel.uiState
        assertTrue(state is LibraryUiState.Empty)
        assertTrue(state.message.contains("Empty Library"))
    }

    @Test
    fun `selectSection updates selectedSection in Content state`() {
        val libRepo = InMemoryLibraryRepository()
        val pkgRepo = InMemoryInstalledPackageRepository()
        val colRepo = InMemoryCollectionRepository()

        libRepo.save(Library.reconstitute(id = libId, name = "Test Library"))
        pkgRepo.save(InstalledPackage.reconstitute(
            id = instId1,
            libraryId = libId,
            packageId = PackageId("pkg-1"),
            topicId = TopicId("topic-1"),
            name = PackageName("Topic 1"),
            version = PackageVersion("1.0.0"),
            state = PackageState.ACTIVE,
            installedAt = Instant.now(),
            contentCount = 10,
            learningItemCount = 20
        ))

        val queryService = LibraryQueryService(libRepo, pkgRepo, colRepo)
        val appContext = LearningApplicationFactory.createInMemory().copy(
            libraryQuery = queryService,
            defaultLibraryId = libId
        )
        val facade = LibraryFacade(appContext, libraryId = libId)

        val viewModel = LibraryViewModel(
            facade = facade,
            taskRunner = ImmediateDesktopTaskRunner
        )

        val content = viewModel.uiState as LibraryUiState.Content
        assertEquals(LibrarySection.OVERVIEW, content.selectedSection)

        viewModel.selectSection(LibrarySection.ACTIVE)
        val updated = viewModel.uiState as LibraryUiState.Content
        assertEquals(LibrarySection.ACTIVE, updated.selectedSection)
    }
}
