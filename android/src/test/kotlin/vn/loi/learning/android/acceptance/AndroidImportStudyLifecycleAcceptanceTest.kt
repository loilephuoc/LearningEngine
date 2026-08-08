package vn.loi.learning.android.acceptance

import androidx.lifecycle.SavedStateHandle
import java.nio.file.Files
import java.time.Instant
import kotlin.io.path.createTempDirectory
import kotlin.test.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Test
import vn.loi.learning.android.library.*
import vn.loi.learning.android.study.*
import vn.loi.learning.application.contentpackaging.PackageImportResult
import vn.loi.learning.domain.content.library.model.*
import vn.loi.learning.domain.content.model.*
import vn.loi.learning.domain.content.packaging.model.*
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.study.learning.model.*
import vn.loi.learning.domain.study.memory.model.*
import vn.loi.learning.domain.study.recall.StudyMode
import vn.loi.learning.domain.study.session.model.*
import vn.loi.learning.infrastructure.LearningApplicationContext
import vn.loi.learning.infrastructure.LearningApplicationFactory

@OptIn(ExperimentalCoroutinesApi::class)
class AndroidImportStudyLifecycleAcceptanceTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `persisted raw import completion is immediately visible and fresh Study starts Introduction`() = runTest(dispatcher) {
        fixture().use { f ->
            val viewModel = AndroidLibraryViewModel(AndroidLibraryFacade(f.context), SavedStateHandle(), dispatcher)
            advanceUntilIdle()
            assertTrue(assertIs<AndroidLibraryState.Root>(viewModel.state.value).packages.isEmpty())
            f.persistRawPackage()
            f.completeLifecycle()

            assertEquals(listOf(f.installedId), f.context.installedPackageRepository!!.findAll().map { it.id })
            assertEquals(f.installedId, f.context.domainLibraryRepository!!.findById(f.context.defaultLibraryId!!)!!.activePackageId)
            val root = assertIs<AndroidLibraryState.Root>(AndroidLibraryFacade(f.context).loadRoot())
            assertEquals(listOf(f.installedId.value), root.packages.map { it.packageId })

            viewModel.reload()
            advanceUntilIdle()
            assertEquals(listOf(f.installedId.value), assertIs<AndroidLibraryState.Root>(viewModel.state.value).packages.map { it.packageId })

            val state = assertIs<AndroidStudyState.Introduction>(AndroidStudyFacade(f.context).start(AndroidSessionEntry.REVIEW))
            val session = requireNotNull(f.context.engine.getSession(SessionId(state.sessionId)))
            assertEquals(StudyMode.ADAPTIVE, session.studyMode)
            assertFalse(state.revealed)
        }
    }

    @Test
    fun `later import preserves the existing canonical active package`() {
        fixture().use { f ->
            f.persistRawPackage()
            f.completeLifecycle()
            val original = f.context.domainLibraryRepository!!.findById(f.context.defaultLibraryId!!)!!.activePackageId

            f.persistAndCompleteSecondPackage()

            assertEquals(original, f.context.domainLibraryRepository!!.findById(f.context.defaultLibraryId!!)!!.activePackageId)
            assertEquals(2, f.context.installedPackageRepository!!.findAll().size)
        }
    }

    @Test
    fun `incompatible stale Typing session is reconciled while identical reimport stays singular`() {
        fixture().use { f ->
            f.persistStaleTypingSession()
            f.persistRawPackage()
            f.completeLifecycle()
            f.context.activeStudySessionScopeReconciler!!.reconcile(f.learner, Moment(2_000))

            assertNull(f.context.engine.getActiveSession(f.learner))
            assertFalse(AndroidStudyFacade(f.context).home().model.primaryAction is AndroidHomePrimaryAction.Resume)
            assertIs<AndroidStudyState.Completion>(AndroidStudyFacade(f.context).loadExact(f.staleSessionId.value))
            assertIs<AndroidStudyState.Introduction>(AndroidStudyFacade(f.context).start(AndroidSessionEntry.REVIEW))

            val compatible = requireNotNull(f.context.engine.getActiveSession(f.learner))
            f.completeLifecycle()
            assertEquals(compatible.id, f.context.activeStudySessionScopeReconciler!!
                .reconcile(f.learner, Moment(3_000))?.id)
            assertEquals(StudyMode.ADAPTIVE, f.context.engine.getActiveSession(f.learner)!!.studyMode)
            assertEquals(1, f.context.installedPackageRepository!!.findAll().count { it.id == f.installedId })
            assertEquals(1, f.context.domainLibraryRepository!!.findById(f.context.defaultLibraryId!!)!!
                .entries.count { it.installedPackageId == f.installedId })
        }
    }

    private fun fixture() = Fixture(createTempDirectory("android-import-study"))

    private class Fixture(private val root: java.nio.file.Path) : AutoCloseable {
        val context: LearningApplicationContext = LearningApplicationFactory.createPersisted(root)
        val learner = LearnerId("default-learner")
        val staleSessionId = SessionId("stale-typing")
        val packageId = PackageId("fresh-package")
        val installedId = vn.loi.learning.domain.library.model.InstalledPackageId("inst-${packageId.value}")
        private val contentId = ContentId("fresh-content")
        private val itemId = LearningItemId("fresh-item")
        private val libraryId = ContentLibraryId("fresh-library")
        private val pkg = ContentPackage(
            packageId, PackageDescriptor("Fresh package", "1.0.0", "OPD3"), setOf(libraryId),
            topicId = TopicId("fresh-topic")
        )
        private val result = PackageImportResult(pkg, 1, 1, 1)

        fun persistRawPackage() {
            context.contentRepository!!.save(Content(contentId, ContentType.WORD, ContentText("bed", "cái giường")))
            context.learningItemRepository!!.save(LearningItem(itemId, contentId, LearningMode.MEANING_RECOGNITION))
            context.contentLibraryRepository!!.save(ContentLibrary(libraryId, LibraryDescriptor("Fresh"), setOf(contentId)))
            context.contentPackageRepository!!.save(pkg)
        }

        fun completeLifecycle() {
            context.completePackageImportLifecycle!!.execute(listOf(result))
        }

        fun persistAndCompleteSecondPackage() {
            val secondContentId = ContentId("second-content")
            val secondItemId = LearningItemId("second-item")
            val secondLibraryId = ContentLibraryId("second-library")
            val secondPackage = ContentPackage(
                PackageId("second-package"), PackageDescriptor("Second package", "1.0.0", "OPD3"),
                setOf(secondLibraryId), topicId = TopicId("second-topic")
            )
            context.contentRepository!!.save(Content(secondContentId, ContentType.WORD, ContentText("chair", "cai ghe")))
            context.learningItemRepository!!.save(LearningItem(secondItemId, secondContentId, LearningMode.MEANING_RECOGNITION))
            context.contentLibraryRepository!!.save(ContentLibrary(secondLibraryId, LibraryDescriptor("Second"), setOf(secondContentId)))
            context.contentPackageRepository!!.save(secondPackage)
            context.completePackageImportLifecycle!!.execute(listOf(PackageImportResult(secondPackage, 1, 1, 1)))
        }

        fun persistStaleTypingSession() {
            val stalePackageId = vn.loi.learning.domain.library.model.InstalledPackageId("missing-installed-package")
            context.studySessionRepository!!.save(StudySession.start(
                staleSessionId, learner, Moment(1_000), SessionPolicy(newItemLimit = 1, reviewItemLimit = 0),
                installedPackageId = stalePackageId, studyMode = StudyMode.TYPING
            ))
            context.studyQueue.create(
                staleSessionId, Moment(1_000), listOf(itemId),
                mapOf(itemId to SessionItemOrigin.NEW), mapOf(itemId to contentId),
                configuredNewTarget = 1, effectiveNewWorkload = 1
            )
        }

        override fun close() {
            Files.walk(root).use { it.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists) }
        }
    }
}
