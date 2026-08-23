package vn.loi.learning.android.study

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.library.model.InstalledPackage
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.PackageName
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.library.model.PackageVersion
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.recall.StudyMode
import vn.loi.learning.infrastructure.LearningApplicationContext
import vn.loi.learning.infrastructure.LearningApplicationFactory

class AndroidStudyLiveLimitUpdateAcceptanceTest {

    private val learnerId = LearnerId("default-learner")

    @Test
    fun `long press metric live update 100 to 15 keeps session open and updates hud immediately`() {
        val fixture = createFixture(itemCount = 25)
        var currentDailyLimits = vn.loi.learning.application.study.DailyStudyBudgetLimits(100, 100)
        val facade = AndroidStudyFacade(
            context = fixture.context,
            learnerId = learnerId,
            now = { 2_000L },
            dailyLimits = { currentDailyLimits }
        )

        val started = facade.start(AndroidSessionEntry.REVIEW, StudyMode.LEARN_NEW)
        val initialIntro = assertIs<AndroidStudyState.Introduction>(started)

        var current: AndroidStudyState = initialIntro
        for (i in 1..3) {
            val intro = assertIs<AndroidStudyState.Introduction>(current)
            current = facade.rateIntroduction(intro, ReviewRating.GOOD)
        }

        val runtime = assertIs<AndroidStudyState.Introduction>(current)
        val hudBefore = assertNotNull(runtime.hud)
        assertEquals(3, hudBefore.newCompleted)
        assertEquals(100, hudBefore.newConfiguredTarget)

        currentDailyLimits = vn.loi.learning.application.study.DailyStudyBudgetLimits(15, 100)
        val updatedState = facade.updateDailyLimits(runtime, newLimit = 15, reviewLimit = 100)
        val updatedIntro = assertIs<AndroidStudyState.Introduction>(updatedState)
        val hudAfter = assertNotNull(updatedIntro.hud)

        assertEquals(3, hudAfter.newCompleted)
        assertEquals(15, hudAfter.newConfiguredTarget)

        val next = facade.rateIntroduction(updatedIntro, ReviewRating.GOOD)
        val nextIntro = assertIs<AndroidStudyState.Introduction>(next)
        val nextHud = assertNotNull(nextIntro.hud)
        assertEquals(4, nextHud.newCompleted)
        assertEquals(15, nextHud.newConfiguredTarget)
    }

    @Test
    fun `live update 100 to 20 with 17 completed continues past new quota into practice recall without error`() {
        val fixture = createFixture(itemCount = 30)
        var currentDailyLimits = vn.loi.learning.application.study.DailyStudyBudgetLimits(100, 100)
        val facade = AndroidStudyFacade(
            context = fixture.context,
            learnerId = learnerId,
            now = { 2_000L },
            dailyLimits = { currentDailyLimits }
        )

        val started = facade.start(AndroidSessionEntry.REVIEW, StudyMode.LEARN_NEW)
        var current: AndroidStudyState = assertIs<AndroidStudyState.Introduction>(started)

        // Complete 17 new items
        for (i in 1..17) {
            val intro = assertIs<AndroidStudyState.Introduction>(current)
            current = facade.rateIntroduction(intro, ReviewRating.GOOD)
        }

        val introAt17 = assertIs<AndroidStudyState.Introduction>(current)
        val hud17 = assertNotNull(introAt17.hud)
        assertEquals(17, hud17.newCompleted)
        assertEquals(100, hud17.newConfiguredTarget)

        // Live update new limit from 100 to 20
        currentDailyLimits = vn.loi.learning.application.study.DailyStudyBudgetLimits(20, 100)
        val updatedState = facade.updateDailyLimits(introAt17, newLimit = 20, reviewLimit = 100)
        val updatedIntro = assertIs<AndroidStudyState.Introduction>(updatedState)
        val hud20 = assertNotNull(updatedIntro.hud)
        assertEquals(17, hud20.newCompleted)
        assertEquals(20, hud20.newConfiguredTarget)

        // Complete items 18, 19, 20
        current = updatedIntro
        for (i in 18..20) {
            val intro = assertIs<AndroidStudyState.Introduction>(current)
            current = facade.rateIntroduction(intro, ReviewRating.GOOD)
        }

        // After completing all 20 new items, session is cleanly completed without error
        assertFalse(current is AndroidStudyState.Failed)
        val completion = assertIs<AndroidStudyState.Completion>(current)
        assertEquals(20, completion.newCompleted)
    }

    @Test
    fun `recovery of existing session with 17 of 20 completed loads recall plan without error`() {
        val fixture = createFixture(itemCount = 30)
        val facade1 = AndroidStudyFacade(
            context = fixture.context,
            learnerId = learnerId,
            now = { 2_000L },
            dailyLimits = { vn.loi.learning.application.study.DailyStudyBudgetLimits(100, 100) }
        )

        val started = facade1.start(AndroidSessionEntry.REVIEW, StudyMode.LEARN_NEW)
        var current: AndroidStudyState = assertIs<AndroidStudyState.Introduction>(started)
        for (i in 1..17) {
            val intro = assertIs<AndroidStudyState.Introduction>(current)
            current = facade1.rateIntroduction(intro, ReviewRating.GOOD)
        }

        val sessionId = assertIs<AndroidStudyState.Introduction>(current).sessionId

        // Simulate limit update to 20
        facade1.updateDailyLimits(current, newLimit = 20, reviewLimit = 100)

        // Simulate app restart / reopen: load exact session from scratch
        val restoredFacade = AndroidStudyFacade(
            context = fixture.context,
            learnerId = learnerId,
            now = { 3_000L },
            dailyLimits = { vn.loi.learning.application.study.DailyStudyBudgetLimits(20, 100) }
        )
        val reloadedState = restoredFacade.loadExact(sessionId)

        val runtime = assertIs<AndroidStudyState.Runtime>(reloadedState)
        val reloadedHud = assertNotNull(runtime.hud)
        assertEquals(17, reloadedHud.newCompleted)
        assertEquals(20, reloadedHud.newConfiguredTarget)
    }

    @Test
    fun `invalid limit update fails validation atomically and keeps session and queue intact`() {
        val fixture = createFixture(itemCount = 20)
        val facade = AndroidStudyFacade(
            context = fixture.context,
            learnerId = learnerId,
            now = { 2_000L },
            dailyLimits = { vn.loi.learning.application.study.DailyStudyBudgetLimits(100, 100) }
        )

        val started = facade.start(AndroidSessionEntry.REVIEW, StudyMode.LEARN_NEW)
        var current: AndroidStudyState = assertIs<AndroidStudyState.Introduction>(started)
        for (i in 1..17) {
            val intro = assertIs<AndroidStudyState.Introduction>(current)
            current = facade.rateIntroduction(intro, ReviewRating.GOOD)
        }

        val introAt17 = assertIs<AndroidStudyState.Introduction>(current)

        // Attempting to set limit to 5 (less than 17 already completed) must fail
        val failedState = facade.updateDailyLimits(introAt17, newLimit = 5, reviewLimit = 100)
        assertIs<AndroidStudyState.Failed>(failedState)

        // The session and queue are not corrupted, continuing with old limits
        val reloaded = facade.loadExact(introAt17.sessionId)
        val runtime = assertIs<AndroidStudyState.Introduction>(reloaded)
        val hud = assertNotNull(runtime.hud)
        assertEquals(17, hud.newCompleted)
        assertEquals(100, hud.newConfiguredTarget)
    }

    @Test
    fun `review limit live update works atomically`() {
        val fixture = createFixture(itemCount = 20)
        var currentLimits = vn.loi.learning.application.study.DailyStudyBudgetLimits(20, 100)
        val facade = AndroidStudyFacade(
            context = fixture.context,
            learnerId = learnerId,
            now = { 2_000L },
            dailyLimits = { currentLimits }
        )

        val started = facade.start(AndroidSessionEntry.REVIEW, StudyMode.LEARN_NEW)
        val intro = assertIs<AndroidStudyState.Introduction>(started)

        currentLimits = vn.loi.learning.application.study.DailyStudyBudgetLimits(20, 30)
        val updatedState = facade.updateDailyLimits(intro, newLimit = 20, reviewLimit = 30)
        val updatedIntro = assertIs<AndroidStudyState.Introduction>(updatedState)
        val hud = assertNotNull(updatedIntro.hud)

        assertEquals(20, hud.newConfiguredTarget)
        assertEquals(30, hud.reviewConfiguredTarget)
    }

    private fun createFixture(itemCount: Int): Fixture {
        val context = LearningApplicationFactory.createInMemory()
        val installedId = install(context, "opd-2nd-pkg", itemCount)
        context.libraryCommand!!.setActivePackage(context.defaultLibraryId!!, installedId)
        return Fixture(context, installedId)
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
        val contentLibraryId = vn.loi.learning.domain.content.library.model.ContentLibraryId("$name-library")
        val packageId = PackageId(name)
        val installedId = InstalledPackageId(name)
        context.contentLibraryRepository!!.save(
            vn.loi.learning.domain.content.library.model.ContentLibrary(
                contentLibraryId,
                vn.loi.learning.domain.content.library.model.LibraryDescriptor(name),
                contentIds
            )
        )
        context.contentPackageRepository!!.save(
            vn.loi.learning.domain.content.packaging.model.ContentPackage(
                packageId,
                vn.loi.learning.domain.content.packaging.model.PackageDescriptor(name, "1.0.0", "OPD3"),
                setOf(contentLibraryId)
            )
        )
        val libraryId = context.defaultLibraryId!!
        context.installedPackageRepository!!.save(
            InstalledPackage.reconstitute(
                installedId, libraryId, packageId, TopicId("$name-topic"), PackageName(name), PackageVersion("1.0.0"),
                PackageState.ACTIVE, java.time.Instant.EPOCH, count, count
            )
        )
        val library = context.domainLibraryRepository!!.findById(libraryId)!!
        context.domainLibraryRepository!!.save(library.registerEntry(installedId, packageId, java.time.Instant.EPOCH))
        return installedId
    }

    private data class Fixture(
        val context: LearningApplicationContext,
        val packageId: InstalledPackageId
    )
}
