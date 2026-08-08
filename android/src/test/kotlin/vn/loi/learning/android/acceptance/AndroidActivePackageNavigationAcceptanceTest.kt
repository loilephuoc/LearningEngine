package vn.loi.learning.android.acceptance

import androidx.lifecycle.SavedStateHandle
import java.time.Instant
import kotlin.test.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import vn.loi.learning.android.packageexperience.*
import vn.loi.learning.android.study.*
import vn.loi.learning.domain.content.library.model.*
import vn.loi.learning.domain.content.model.*
import vn.loi.learning.domain.content.packaging.model.*
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.*
import vn.loi.learning.domain.study.learning.model.*
import vn.loi.learning.domain.study.recall.StudyMode
import vn.loi.learning.infrastructure.LearningApplicationFactory

@OptIn(ExperimentalCoroutinesApi::class)
class AndroidActivePackageNavigationAcceptanceTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `multiple ACTIVE packages follow canonical Library selection`() {
        val context = LearningApplicationFactory.createInMemory()
        val first = install(context, "a-package")
        val selected = install(context, "z-package")
        context.libraryCommand!!.setActivePackage(context.defaultLibraryId!!, selected)

        val intro = assertIs<AndroidStudyState.Introduction>(AndroidStudyFacade(context).start(AndroidSessionEntry.REVIEW))
        val session = context.engine.getSession(vn.loi.learning.domain.study.session.model.SessionId(intro.sessionId))!!
        assertEquals(selected, session.installedPackageId)
        assertNotEquals(first, session.installedPackageId)
        assertEquals(StudyMode.ADAPTIVE, session.studyMode)
    }

    @Test
    fun `Study Package selects package starts Introduction and reports session for navigation`() = runTest(dispatcher) {
        val context = LearningApplicationFactory.createInMemory()
        val previous = install(context, "previous")
        val target = install(context, "target")
        context.libraryCommand!!.setActivePackage(context.defaultLibraryId!!, previous)
        val viewModel = AndroidPackageViewModel(AndroidPackageFacade(context), SavedStateHandle(), dispatcher)
        viewModel.open(target.value)
        advanceUntilIdle()
        var navigatedSessionId: String? = null

        viewModel.startStudy { navigatedSessionId = it }
        advanceUntilIdle()

        assertEquals(target, context.domainLibraryRepository!!.findById(context.defaultLibraryId!!)!!.activePackageId)
        val sessionId = requireNotNull(navigatedSessionId)
        assertEquals(target, context.engine.getSession(vn.loi.learning.domain.study.session.model.SessionId(sessionId))!!.installedPackageId)
        assertIs<AndroidStudyState.Introduction>(AndroidStudyFacade(context).loadExact(sessionId))
    }

    @Test
    fun `Continue Learning opens exact session without creating duplicate`() = runTest(dispatcher) {
        val context = LearningApplicationFactory.createInMemory()
        val target = install(context, "continue")
        context.libraryCommand!!.setActivePackage(context.defaultLibraryId!!, target)
        val existing = assertIs<AndroidStudyState.Introduction>(AndroidStudyFacade(context).start(AndroidSessionEntry.REVIEW)).sessionId
        val before = context.studySessionRepository!!.findAll().map { it.id }
        val viewModel = AndroidPackageViewModel(AndroidPackageFacade(context), SavedStateHandle(), dispatcher)
        viewModel.open(target.value)
        advanceUntilIdle()
        var opened: String? = null

        viewModel.startStudy { opened = it }
        advanceUntilIdle()

        assertEquals(existing, opened)
        assertEquals(before, context.studySessionRepository!!.findAll().map { it.id })
    }

    private fun install(context: vn.loi.learning.infrastructure.LearningApplicationContext, name: String): InstalledPackageId {
        val contentId = ContentId("$name-content")
        val contentLibraryId = ContentLibraryId("$name-library")
        val packageId = PackageId(name)
        val installedId = InstalledPackageId(name)
        context.contentRepository!!.save(Content(contentId, ContentType.WORD, ContentText(name, "$name-answer")))
        context.learningItemRepository!!.save(LearningItem(LearningItemId("$name-item"), contentId, LearningMode.MEANING_RECOGNITION))
        context.contentLibraryRepository!!.save(ContentLibrary(contentLibraryId, LibraryDescriptor(name), setOf(contentId)))
        context.contentPackageRepository!!.save(ContentPackage(packageId, PackageDescriptor(name, "1.0.0", "OPD3"), setOf(contentLibraryId)))
        val libraryId = context.defaultLibraryId!!
        context.installedPackageRepository!!.save(InstalledPackage.reconstitute(
            installedId, libraryId, packageId, TopicId("$name-topic"), PackageName(name), PackageVersion("1.0.0"),
            PackageState.ACTIVE, Instant.EPOCH, 1, 1
        ))
        val library = context.domainLibraryRepository!!.findById(libraryId)!!
        context.domainLibraryRepository!!.save(library.registerEntry(installedId, packageId, Instant.EPOCH))
        return installedId
    }
}
