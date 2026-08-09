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
    fun `ACTIVE package without Library selection is usable but not current or startable from generic Study`() {
        val context = LearningApplicationFactory.createInMemory()
        val installed = install(context, "usable-not-current")

        val home = AndroidStudyFacade(context).home()
        assertIs<AndroidHomePrimaryAction.OpenLibrary>(home.model.primaryAction)
        assertNull(home.model.contextTitle)
        assertFalse(home.availability.canStartReview)
        val detail = assertIs<AndroidPackageContentState.Content>(AndroidPackageFacade(context).openPackage(installed))
        assertEquals("ACTIVE", detail.header.state)
        assertFalse(detail.header.isActivePackage)
        assertIs<AndroidPackageCta.StudyPackage>(detail.cta)
    }

    @Test
    fun `Study Package selects package starts Introduction and reports session for navigation`() = runTest(dispatcher) {
        val context = LearningApplicationFactory.createInMemory()
        val previous = install(context, "previous")
        val target = install(context, "target")
        context.libraryCommand!!.setActivePackage(context.defaultLibraryId!!, previous)
        val previousSessionId = assertIs<AndroidStudyState.Introduction>(
            AndroidStudyFacade(context).start(AndroidSessionEntry.REVIEW)
        ).sessionId
        val viewModel = AndroidPackageViewModel(AndroidPackageFacade(context), SavedStateHandle(), dispatcher)
        viewModel.open(target.value)
        advanceUntilIdle()
        var navigatedSessionId: String? = null

        viewModel.startStudy { navigatedSessionId = it }
        advanceUntilIdle()

        assertEquals(target, context.domainLibraryRepository!!.findById(context.defaultLibraryId!!)!!.activePackageId)
        val sessionId = requireNotNull(navigatedSessionId)
        assertEquals(target, context.engine.getSession(vn.loi.learning.domain.study.session.model.SessionId(sessionId))!!.installedPackageId)
        assertEquals(vn.loi.learning.domain.study.session.model.SessionStatus.FINISHED,
            context.engine.getSession(vn.loi.learning.domain.study.session.model.SessionId(previousSessionId))!!.status)
        assertIs<AndroidStudyState.Introduction>(AndroidStudyFacade(context).loadExact(sessionId))
    }

    @Test
    fun `Home reconciles active session that does not match canonical package`() {
        val context = LearningApplicationFactory.createInMemory()
        val first = install(context, "session-package")
        val selected = install(context, "selected-package")
        context.libraryCommand!!.setActivePackage(context.defaultLibraryId!!, first)
        val oldSession = assertIs<AndroidStudyState.Introduction>(
            AndroidStudyFacade(context, now = { 2_000_000_000_000 }).start(AndroidSessionEntry.REVIEW)
        ).sessionId
        context.libraryCommand!!.setActivePackage(context.defaultLibraryId!!, selected)

        val home = AndroidStudyFacade(context, now = { 2_000_000_001_000 }).home()

        assertIs<AndroidHomePrimaryAction.StartLearning>(home.model.primaryAction)
        assertEquals(vn.loi.learning.domain.study.session.model.SessionStatus.FINISHED,
            context.engine.getSession(vn.loi.learning.domain.study.session.model.SessionId(oldSession))!!.status)
        val next = assertIs<AndroidStudyState.Introduction>(AndroidStudyFacade(context).start(AndroidSessionEntry.REVIEW))
        assertEquals(selected,
            context.engine.getSession(vn.loi.learning.domain.study.session.model.SessionId(next.sessionId))!!.installedPackageId)
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

    @Test
    fun `session completion preserves selected package for the next canonical start`() {
        val context = LearningApplicationFactory.createInMemory()
        val target = install(context, "finish-current")
        context.libraryCommand!!.setActivePackage(context.defaultLibraryId!!, target)
        val facade = AndroidStudyFacade(context, now = { 1_700_000_000_000 })
        val intro = assertIs<AndroidStudyState.Introduction>(facade.start(AndroidSessionEntry.REVIEW))
        val revealed = assertIs<AndroidStudyState.Introduction>(facade.revealIntroduction(intro))
        val completion = assertIs<AndroidStudyState.Completion>(
            facade.rateIntroduction(revealed, vn.loi.learning.domain.study.memory.model.ReviewRating.GOOD)
        )

        assertEquals(vn.loi.learning.domain.study.session.model.SessionStatus.FINISHED,
            context.engine.getSession(vn.loi.learning.domain.study.session.model.SessionId(completion.sessionId))!!.status)
        assertEquals(target, context.domainLibraryRepository!!.findById(context.defaultLibraryId!!)!!.activePackageId)
        assertIs<AndroidHomePrimaryAction.DailyComplete>(facade.home().model.primaryAction)
        assertEquals(target, context.domainLibraryRepository!!.findById(context.defaultLibraryId!!)!!.activePackageId)
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
