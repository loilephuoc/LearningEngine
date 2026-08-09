package vn.loi.learning.android.acceptance

import androidx.lifecycle.SavedStateHandle
import java.time.Instant
import java.time.ZoneId
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
import vn.loi.learning.application.review.ReviewCommand
import vn.loi.learning.application.study.DailyStudyBudgetLimits
import vn.loi.learning.domain.content.library.model.*
import vn.loi.learning.domain.content.model.*
import vn.loi.learning.domain.content.packaging.model.*
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.*
import vn.loi.learning.domain.study.learning.model.*
import vn.loi.learning.domain.study.memory.model.*
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
    fun `Library selection persists exact usable package without creating a session`() {
        val context = LearningApplicationFactory.createInMemory()
        val first = install(context, "select-first")
        val selected = install(context, "select-second")
        val facade = vn.loi.learning.android.library.AndroidLibraryFacade(context)

        assertNull(context.domainLibraryRepository!!.findById(context.defaultLibraryId!!)!!.activePackageId)
        assertTrue(context.studySessionRepository!!.findAll().isEmpty())

        val root = assertIs<vn.loi.learning.android.library.AndroidLibraryState.Root>(
            facade.selectLearningPackage(selected)
        )

        assertEquals(selected, context.domainLibraryRepository!!.findById(context.defaultLibraryId!!)!!.activePackageId)
        assertFalse(root.allPackages.single { it.packageId == first.value }.isActivePackage)
        assertTrue(root.allPackages.single { it.packageId == selected.value }.isActivePackage)
        assertTrue(context.studySessionRepository!!.findAll().isEmpty())
        val reloaded = assertIs<vn.loi.learning.android.library.AndroidLibraryState.Root>(facade.loadRoot())
        assertTrue(reloaded.allPackages.single { it.packageId == selected.value }.isActivePackage)
        assertNotNull(AndroidStudyFacade(context).home().model.contextTitle)
    }

    @Test
    fun `long lived Study ViewModel refreshes Home after Library selection`() = runTest(dispatcher) {
        val context = LearningApplicationFactory.createInMemory()
        val selected = install(context, "view-model-selection", 50)
        val studyViewModel = AndroidStudyViewModel(AndroidStudyFacade(context), SavedStateHandle(), dispatcher)
        advanceUntilIdle()
        assertIs<AndroidHomePrimaryAction.OpenLibrary>(
            assertIs<AndroidStudyState.Home>(studyViewModel.state.value).model.primaryAction
        )

        assertIs<vn.loi.learning.android.library.AndroidLibraryState.Root>(
            vn.loi.learning.android.library.AndroidLibraryFacade(context).selectLearningPackage(selected)
        )
        studyViewModel.onEvent(AndroidStudyEvent.RefreshHomeIfIdle)
        advanceUntilIdle()

        val refreshed = assertIs<AndroidStudyState.Home>(studyViewModel.state.value)
        assertIs<AndroidHomePrimaryAction.StartLearning>(refreshed.model.primaryAction)
        assertNotNull(refreshed.model.contextTitle)
    }

    @Test
    fun `long lived Study ViewModel refreshes changed daily budget without creating a session`() = runTest(dispatcher) {
        val context = LearningApplicationFactory.createInMemory()
        val selected = install(context, "view-model-budget", 80)
        context.libraryCommand!!.setActivePackage(context.defaultLibraryId!!, selected)
        val learner = LearnerId("default-learner")
        val now = 1_700_000_000_000L
        repeat(20) { index ->
            context.engine.review(ReviewCommand(
                ReviewEventId("view-model-budget-$index"), learner,
                LearningItemId("view-model-budget-item-$index"), ReviewRating.GOOD, Moment(now - 1_000 + index)
            ))
        }
        val preferenceStore = object : AndroidStudyPreferenceStore {
            private var value = DailyStudyBudgetLimits(20, 100)
            override fun load() = value
            override fun save(limits: DailyStudyBudgetLimits) { value = limits }
        }
        val preferences = AndroidStudyPreferencesController(preferenceStore)
        val studyViewModel = AndroidStudyViewModel(
            AndroidStudyFacade(context, learner, { now }, dailyLimits = preferences::current, zoneId = { ZoneId.of("Asia/Ho_Chi_Minh") }),
            SavedStateHandle(), dispatcher
        )
        advanceUntilIdle()
        val exhausted = assertIs<AndroidStudyState.Home>(studyViewModel.state.value)
        assertEquals(0, exhausted.model.dailyBudget!!.newRemainingToday)

        assertTrue(preferences.updateNew(50))
        studyViewModel.onEvent(AndroidStudyEvent.RefreshHomeIfIdle)
        advanceUntilIdle()

        val refreshed = assertIs<AndroidStudyState.Home>(studyViewModel.state.value)
        assertEquals(30, refreshed.model.dailyBudget!!.newRemainingToday)
        assertIs<AndroidHomePrimaryAction.StartLearning>(refreshed.model.primaryAction)
        assertTrue(context.studySessionRepository!!.findAll().isEmpty())
    }

    @Test
    fun `root refresh preserves a live Introduction and its exact session`() = runTest(dispatcher) {
        val context = LearningApplicationFactory.createInMemory()
        val selected = install(context, "live-introduction")
        context.libraryCommand!!.setActivePackage(context.defaultLibraryId!!, selected)
        val studyViewModel = AndroidStudyViewModel(AndroidStudyFacade(context), SavedStateHandle(), dispatcher)
        advanceUntilIdle()
        studyViewModel.onEvent(AndroidStudyEvent.Start(AndroidSessionEntry.REVIEW))
        advanceUntilIdle()
        val live = assertIs<AndroidStudyState.Introduction>(studyViewModel.state.value)
        val sessionsBefore = context.studySessionRepository!!.findAll().map { it.id }

        studyViewModel.onEvent(AndroidStudyEvent.RefreshHomeIfIdle)
        advanceUntilIdle()

        val preserved = assertIs<AndroidStudyState.Introduction>(studyViewModel.state.value)
        assertEquals(live, preserved)
        assertEquals(sessionsBefore, context.studySessionRepository!!.findAll().map { it.id })
    }

    @Test
    fun `package detail selection uses canonical authority without starting Study`() {
        val context = LearningApplicationFactory.createInMemory()
        val selected = install(context, "detail-selection")

        AndroidPackageFacade(context).selectLearningPackage(selected).getOrThrow()

        assertEquals(selected, context.domainLibraryRepository!!.findById(context.defaultLibraryId!!)!!.activePackageId)
        assertTrue(context.studySessionRepository!!.findAll().isEmpty())
        val detail = assertIs<AndroidPackageContentState.Content>(AndroidPackageFacade(context).openPackage(selected))
        assertTrue(detail.header.isActivePackage)
    }

    @Test
    fun `Library selection preserves learner daily budget and history before explicit Study start`() {
        val context = LearningApplicationFactory.createInMemory()
        val selected = install(context, "budget-recovery", 50)
        val learner = LearnerId("default-learner")
        val now = 1_700_000_000_000L
        repeat(20) { index ->
            context.engine.review(ReviewCommand(
                ReviewEventId("selection-budget-$index"), learner,
                LearningItemId("budget-recovery-item-$index"), ReviewRating.GOOD,
                Moment(now - 1_000 + index)
            ))
        }
        val beforeHistory = context.reviewEventRepository!!.findAll(learner)
        val library = vn.loi.learning.android.library.AndroidLibraryFacade(context)

        assertIs<vn.loi.learning.android.library.AndroidLibraryState.Root>(library.selectLearningPackage(selected))

        assertTrue(context.studySessionRepository!!.findAll().isEmpty())
        assertEquals(beforeHistory, context.reviewEventRepository!!.findAll(learner))
        val study = AndroidStudyFacade(
            context, learner, { now }, dailyLimits = { DailyStudyBudgetLimits(50, 100) },
            zoneId = { ZoneId.of("Asia/Ho_Chi_Minh") }
        )
        val home = study.home()
        assertEquals(20, home.model.dailyBudget!!.newCompletedToday)
        assertEquals(30, home.model.dailyBudget!!.newRemainingToday)
        assertTrue(context.studySessionRepository!!.findAll().isEmpty())
        val intro = assertIs<AndroidStudyState.Introduction>(study.start(AndroidSessionEntry.REVIEW))
        assertEquals(30, context.engine.getSession(vn.loi.learning.domain.study.session.model.SessionId(intro.sessionId))!!.policy.newItemLimit)
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
    fun `Start reuses exact compatible persisted session without creating a duplicate`() {
        val context = LearningApplicationFactory.createInMemory()
        val selected = install(context, "persisted-start")
        context.libraryCommand!!.setActivePackage(context.defaultLibraryId!!, selected)
        val firstFacade = AndroidStudyFacade(context)
        val existing = assertIs<AndroidStudyState.Introduction>(
            firstFacade.start(AndroidSessionEntry.REVIEW)
        )
        val sessionsBefore = context.studySessionRepository!!.findAll().map { it.id }

        val resumed = assertIs<AndroidStudyState.Introduction>(
            AndroidStudyFacade(context).start(AndroidSessionEntry.REVIEW)
        )

        assertEquals(existing.sessionId, resumed.sessionId)
        assertEquals(sessionsBefore, context.studySessionRepository!!.findAll().map { it.id })
        assertEquals(selected, context.engine.getActiveSession(LearnerId("default-learner"))!!.installedPackageId)
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

    private fun install(
        context: vn.loi.learning.infrastructure.LearningApplicationContext,
        name: String,
        count: Int = 1
    ): InstalledPackageId {
        val contentIds = (0 until count).mapTo(linkedSetOf()) { index ->
            val suffix = if (count == 1) "" else "-$index"
            val contentId = ContentId("$name-content$suffix")
            context.contentRepository!!.save(
                Content(contentId, ContentType.WORD, ContentText("$name$suffix", "$name-answer$suffix"))
            )
            context.learningItemRepository!!.save(
                LearningItem(LearningItemId("$name-item$suffix"), contentId, LearningMode.MEANING_RECOGNITION)
            )
            contentId
        }
        val contentLibraryId = ContentLibraryId("$name-library")
        val packageId = PackageId(name)
        val installedId = InstalledPackageId(name)
        context.contentLibraryRepository!!.save(ContentLibrary(contentLibraryId, LibraryDescriptor(name), contentIds))
        context.contentPackageRepository!!.save(ContentPackage(packageId, PackageDescriptor(name, "1.0.0", "OPD3"), setOf(contentLibraryId)))
        val libraryId = context.defaultLibraryId!!
        context.installedPackageRepository!!.save(InstalledPackage.reconstitute(
            installedId, libraryId, packageId, TopicId("$name-topic"), PackageName(name), PackageVersion("1.0.0"),
            PackageState.ACTIVE, Instant.EPOCH, count, count
        ))
        val library = context.domainLibraryRepository!!.findById(libraryId)!!
        context.domainLibraryRepository!!.save(library.registerEntry(installedId, packageId, Instant.EPOCH))
        return installedId
    }
}
