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

class DesktopLongOperationStateTest {

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
        runner.runNext()

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
        runner.runNext()
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
