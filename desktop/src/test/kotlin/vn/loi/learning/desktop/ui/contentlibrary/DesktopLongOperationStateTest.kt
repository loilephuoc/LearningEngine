package vn.loi.learning.desktop.ui.contentlibrary

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vn.loi.learning.desktop.ui.state.DesktopTaskRunner
import vn.loi.learning.desktop.ui.study.StudyFacade
import vn.loi.learning.desktop.ui.study.StudyViewModel
import vn.loi.learning.infrastructure.LearningApplicationFactory
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId
import vn.loi.learning.domain.content.library.model.ContentLibrary
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.library.model.LibraryDescriptor
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.topic.model.TopicId

class DesktopLongOperationStateTest {

    @Test
    fun `collection create is queued blocks duplicate and publishes only after success`() {
        val context = collectionContext()
        val runner = QueuedTaskRunner()
        var invalidationCount = 0
        val viewModel = ContentLibraryViewModel(
            ContentLibraryFacade(context),
            LessonBrowserFacade(context),
            onContentDataChanged = { invalidationCount += 1 },
            taskRunner = runner
        )
        runner.runNext()
        viewModel.showCreateCollectionDialog("library-a")
        viewModel.updateCreateCollectionName("Collection A")

        viewModel.confirmCreateCollection()

        assertIs<ContentLibraryOperation.Loading>(viewModel.uiState.operation)
        assertEquals(1, runner.pendingCount)
        assertEquals(0, invalidationCount)
        assertTrue(viewModel.createCollectionDialogState.visible)
        assertTrue(context.libraryCollections.query(ContentLibraryId("library-a")).isEmpty())

        viewModel.confirmCreateCollection()
        assertEquals(1, runner.pendingCount)

        runner.runNext()

        assertIs<ContentLibraryOperation.Idle>(viewModel.uiState.operation)
        assertEquals(1, invalidationCount)
        assertTrue(!viewModel.createCollectionDialogState.visible)
        assertEquals(listOf("Collection A"), viewModel.uiState.libraries.single().collections.map { it.name })
    }

    @Test
    fun `collection create and rename failures keep state reset busy and allow retry`() {
        val context = collectionContext()
        val runner = QueuedTaskRunner()
        var completionCount = 0
        val viewModel = ContentLibraryViewModel(
            ContentLibraryFacade(context),
            LessonBrowserFacade(context),
            taskRunner = runner,
            loadImmediately = false
        )

        viewModel.createCollection("library-a", "Collection A") { completionCount += 1 }
        runner.failNext(IllegalStateException("injected create failure"))
        assertIs<ContentLibraryOperation.Idle>(viewModel.uiState.operation)
        assertEquals(0, completionCount)
        assertNotNull(viewModel.uiState.importError)

        viewModel.createCollection("library-a", "Collection A") { completionCount += 1 }
        runner.runNext()
        val collectionId = viewModel.uiState.libraries.single().collections.single().id
        viewModel.renameCollection(collectionId, "Renamed") { completionCount += 1 }
        runner.failNext(IllegalStateException("injected rename failure"))
        assertEquals("Collection A", viewModel.uiState.libraries.single().collections.single().name)
        assertEquals(1, completionCount)
        assertIs<ContentLibraryOperation.Idle>(viewModel.uiState.operation)

        viewModel.renameCollection(collectionId, "Renamed") { completionCount += 1 }
        runner.runNext()
        assertEquals("Renamed", viewModel.uiState.libraries.single().collections.single().name)
        assertEquals(2, completionCount)
    }

    @Test
    fun `attach detach and delete share async lifecycle preserve browser scope and publish once`() {
        val context = collectionContext(includePackage = true)
        val runner = QueuedTaskRunner()
        var invalidationCount = 0
        val viewModel = ContentLibraryViewModel(
            ContentLibraryFacade(context),
            LessonBrowserFacade(context),
            onContentDataChanged = { invalidationCount += 1 },
            taskRunner = runner
        )
        runner.runNext()
        viewModel.openLibrary("library-a")
        runner.runNext()
        val browserLibraryId = viewModel.lessonBrowserUiState?.libraryId
        viewModel.createCollection("library-a", "Collection A")
        runner.runNext()
        val collectionId = viewModel.uiState.libraries.single().collections.single().id

        viewModel.attachPackageToCollection(collectionId, "Collection A", "package-a", "Package A")
        assertIs<ContentLibraryOperation.Loading>(viewModel.uiState.operation)
        runner.runNext()
        assertEquals(listOf("package-a"), viewModel.uiState.libraries.single().collections.single().attachedPackages.map { it.id })
        assertEquals(browserLibraryId, viewModel.lessonBrowserUiState?.libraryId)

        viewModel.detachPackageFromCollection(collectionId, "Collection A", "package-a", "Package A")
        runner.failNext(IllegalStateException("injected detach failure"))
        assertEquals(listOf("package-a"), viewModel.uiState.libraries.single().collections.single().attachedPackages.map { it.id })
        assertIs<ContentLibraryOperation.Idle>(viewModel.uiState.operation)

        viewModel.detachPackageFromCollection(collectionId, "Collection A", "package-a", "Package A")
        runner.runNext()
        assertTrue(viewModel.uiState.libraries.single().collections.single().attachedPackages.isEmpty())
        viewModel.deleteCollection(collectionId, "Collection A")
        runner.runNext()
        assertTrue(viewModel.uiState.libraries.single().collections.isEmpty())
        assertEquals(4, invalidationCount)
    }

    private fun collectionContext(includePackage: Boolean = false) =
        LearningApplicationFactory.createInMemory().also { context ->
            context.contentLibraryRepository!!.save(
                ContentLibrary(ContentLibraryId("library-a"), LibraryDescriptor("Library A"), emptySet())
            )
            if (includePackage) {
                context.contentPackageRepository!!.save(
                    ContentPackage(
                        id = PackageId("package-a"),
                        descriptor = PackageDescriptor("Package A", "1.0", "OPD3"),
                        libraryIds = setOf(ContentLibraryId("library-a")),
                        topicId = TopicId("topic-a")
                    )
                )
            }
        }

    @Test
    fun `library load and import expose immediate state and suppress duplicate actions`() {
        val context = LearningApplicationFactory.createInMemory()
        val runner = QueuedTaskRunner()
        val viewModel = ContentLibraryViewModel(
            ContentLibraryFacade(context),
            LessonBrowserFacade(context),
            taskRunner = runner
        )

        assertIs<ContentLibraryOperation.Loading>(viewModel.uiState.operation)
        assertEquals(1, runner.pendingCount)
        viewModel.refresh()
        assertEquals(1, runner.pendingCount)
        runner.runNext()
        assertIs<ContentLibraryOperation.Idle>(viewModel.uiState.operation)

        val directory = Files.createTempDirectory("desktop-import-state")
        try {
            viewModel.importFromDirectory(directory)
            assertIs<ContentLibraryOperation.Importing>(viewModel.uiState.operation)
            assertEquals(1, runner.pendingCount)
            viewModel.importFromDirectory(directory)
            assertEquals(1, runner.pendingCount)
            runner.runNext()
            assertIs<ContentLibraryOperation.Idle>(viewModel.uiState.operation)
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `topic removal is queued releases busy state and publishes changes only after success`() {
        val context = LearningApplicationFactory.createInMemory()
        val runner = QueuedTaskRunner()
        var contentChangedCount = 0
        val viewModel = ContentLibraryViewModel(
            ContentLibraryFacade(context),
            LessonBrowserFacade(context),
            onContentDataChanged = { contentChangedCount += 1 },
            taskRunner = runner
        )
        runner.runNext()

        viewModel.uninstallPackage("missing-package", "Topic A")

        val removing = assertIs<ContentLibraryOperation.Loading>(viewModel.uiState.operation)
        assertEquals("Removing topic and learning data", removing.phase)
        assertEquals(1, runner.pendingCount)
        assertEquals(0, contentChangedCount)
        assertNull(viewModel.uiState.importMessage)

        viewModel.uninstallPackage("missing-package", "Topic A")
        assertEquals(1, runner.pendingCount)

        runner.runNext()

        assertIs<ContentLibraryOperation.Idle>(viewModel.uiState.operation)
        assertEquals(1, contentChangedCount)
        assertNotNull(viewModel.uiState.importMessage)
        assertNull(viewModel.uiState.importError)
    }

    @Test
    fun `topic removal failure preserves current state reports error and returns idle`() {
        val context = LearningApplicationFactory.createInMemory()
        val runner = QueuedTaskRunner()
        var contentChangedCount = 0
        val viewModel = ContentLibraryViewModel(
            ContentLibraryFacade(context),
            LessonBrowserFacade(context),
            onContentDataChanged = { contentChangedCount += 1 },
            taskRunner = runner
        )
        runner.runNext()
        val stateBeforeRemoval = viewModel.uiState

        viewModel.uninstallPackage("package-a", "Topic A")
        assertIs<ContentLibraryOperation.Loading>(viewModel.uiState.operation)

        runner.failNext(IllegalStateException("injected uninstall failure"))

        assertIs<ContentLibraryOperation.Idle>(viewModel.uiState.operation)
        assertEquals(stateBeforeRemoval.packages, viewModel.uiState.packages)
        assertEquals(stateBeforeRemoval.libraries, viewModel.uiState.libraries)
        assertEquals(0, contentChangedCount)
        assertNull(viewModel.uiState.importMessage)
        assertNotNull(viewModel.uiState.importError)
    }

    @Test
    fun `study preparation is immediate and duplicate start is suppressed`() {
        val runner = QueuedTaskRunner()
        val viewModel = StudyViewModel(
            StudyFacade(LearningApplicationFactory.createInMemory()),
            taskRunner = runner
        )

        viewModel.startStudy()
        assertTrue(viewModel.uiState.actionInProgress)
        assertEquals("Preparing study session", viewModel.uiState.message)
        assertEquals(1, runner.pendingCount)
        viewModel.startStudy()
        assertEquals(1, runner.pendingCount)
        runner.runNext()
        assertTrue(!viewModel.uiState.actionInProgress)
    }

    @Test
    fun `open library enters loading before query and failures release guards`() {
        val root = Files.createTempDirectory("desktop-open-library-state")
        val packages = root.resolve("packages").also(Files::createDirectories)
        try {
            Files.writeString(
                packages.resolve("topic.json"),
                """[{"group":"G","section":"S","lesson":"L","en":"Hello","vi":"Xin chao"}]"""
            )
            Files.write(
                packages.resolve("topic.pkg"),
                byteArrayOf(79, 80, 68, 51, 0, 0, 0, 1, 0, 0, 0, 0)
            )
            val context = LearningApplicationFactory.createPersisted(root.resolve("data"))
            assertEquals(
                1,
                context.packageImporter(packages)
                    .importAllDetailed(PackageCatalogId("catalog"))
                    .successfulImports.size
            )
            val runner = QueuedTaskRunner()
            val viewModel = ContentLibraryViewModel(
                ContentLibraryFacade(context),
                LessonBrowserFacade(context),
                taskRunner = runner
            )
            runner.runNext()
            val libraryId = viewModel.uiState.libraries.single().id
            viewModel.openLibrary(libraryId)
            assertIs<ContentLibraryOperation.Loading>(viewModel.uiState.operation)
            runner.runNext()
            assertEquals(1, viewModel.lessonBrowserUiState?.lessonCount)

            viewModel.importFromDirectory(root.resolve("missing"))
            assertIs<ContentLibraryOperation.Importing>(viewModel.uiState.operation)
            runner.runNext()
            assertIs<ContentLibraryOperation.Idle>(viewModel.uiState.operation)
            assertTrue(viewModel.uiState.importError != null)
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun `progress never claims completion before commit and preparation failure recovers`() {
        assertEquals(
            0.95f,
            ContentLibraryOperation.Importing("Preparing", 10, 10).fraction
        )
        assertEquals(
            1f,
            ContentLibraryOperation.Importing("Committed", 10, 10, committed = true).fraction
        )

        val runner = QueuedTaskRunner()
        val study = StudyViewModel(StudyFacade(LearningApplicationFactory.createInMemory()), taskRunner = runner)
        study.startLessonStudy("missing-content")
        assertTrue(study.uiState.actionInProgress)
        runner.runNext()
        assertTrue(!study.uiState.actionInProgress)
        assertTrue(study.uiState.loadError != null)
    }

    @Test
    fun `stale debounced search result cannot replace newer query`() {
        val newer = LessonBrowserUiState(query = "new", appliedQuery = "")
        assertEquals("", applyDebouncedLessonQuery(newer, "old").appliedQuery)
        assertEquals("new", applyDebouncedLessonQuery(newer, "new").appliedQuery)
    }

    private class QueuedTaskRunner : DesktopTaskRunner {
        private val tasks = ArrayDeque<() -> Unit>()
        private var injectedFailure: Exception? = null
        val pendingCount: Int get() = tasks.size

        override fun <T> run(work: () -> T, onSuccess: (T) -> Unit, onFailure: (Exception) -> Unit) {
            tasks += {
                val failure = injectedFailure
                injectedFailure = null
                if (failure != null) {
                    onFailure(failure)
                } else {
                    try { onSuccess(work()) } catch (exception: Exception) { onFailure(exception) }
                }
            }
        }

        override fun dispatch(action: () -> Unit) = action()
        fun runNext() = tasks.removeFirst().invoke()
        fun failNext(exception: Exception) {
            injectedFailure = exception
            runNext()
        }
    }
}
