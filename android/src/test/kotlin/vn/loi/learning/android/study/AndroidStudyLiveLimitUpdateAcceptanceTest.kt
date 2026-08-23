package vn.loi.learning.android.study

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
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
        val fixture = createFixture(itemCount = 20)
        var currentDailyLimits = vn.loi.learning.application.study.DailyStudyBudgetLimits(100, 100)
        val facade = AndroidStudyFacade(
            context = fixture.context,
            learnerId = learnerId,
            now = { 2_000L },
            dailyLimits = { currentDailyLimits }
        )

        // Start session with new limit 100
        val started = facade.start(AndroidSessionEntry.REVIEW, StudyMode.LEARN_NEW)
        val initialIntro = assertIs<AndroidStudyState.Introduction>(started)

        // Rate the first 3 cards as GOOD
        var current: AndroidStudyState = initialIntro
        for (i in 1..3) {
            val intro = assertIs<AndroidStudyState.Introduction>(current)
            current = facade.rateIntroduction(intro, ReviewRating.GOOD)
        }

        val runtime = assertIs<AndroidStudyState.Introduction>(current)
        val hudBefore = assertNotNull(runtime.hud)
        assertEquals(3, hudBefore.newCompleted)
        assertEquals(100, hudBefore.newConfiguredTarget)

        // Live update limits: new limit from 100 to 15
        currentDailyLimits = vn.loi.learning.application.study.DailyStudyBudgetLimits(15, 100)
        val updatedState = facade.updateDailyLimits(runtime, newLimit = 15, reviewLimit = 100)
        val updatedIntro = assertIs<AndroidStudyState.Introduction>(updatedState)
        val hudAfter = assertNotNull(updatedIntro.hud)

        assertEquals(3, hudAfter.newCompleted)
        assertEquals(15, hudAfter.newConfiguredTarget)

        // Progress continues smoothly without crashing
        val next = facade.rateIntroduction(updatedIntro, ReviewRating.GOOD)
        val nextIntro = assertIs<AndroidStudyState.Introduction>(next)
        val nextHud = assertNotNull(nextIntro.hud)
        assertEquals(4, nextHud.newCompleted)
        assertEquals(15, nextHud.newConfiguredTarget)
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
