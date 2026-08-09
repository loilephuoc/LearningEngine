package vn.loi.learning.android.acceptance

import java.time.Instant
import java.time.ZoneId
import kotlin.test.*
import org.junit.Test
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
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.infrastructure.LearningApplicationFactory

class AndroidDailyStudyBudgetAcceptanceTest {
    private val learner = LearnerId("default-learner")
    private val now = 1_700_000_000_000L
    private val zone = ZoneId.of("Asia/Ho_Chi_Minh")

    @Test
    fun `20 completed NEW blocks another 20 then increasing to 50 admits remaining 30`() {
        val context = LearningApplicationFactory.createInMemory()
        val packageId = install(context, 50)
        repeat(20) { index ->
            context.engine.review(ReviewCommand(
                ReviewEventId("budget-review-$index"), learner, LearningItemId("budget-item-$index"),
                ReviewRating.GOOD, Moment(now - 1_000 + index)
            ))
        }

        val capped = AndroidStudyFacade(
            context, learner, { now }, dailyLimits = { DailyStudyBudgetLimits(20, 100) }, zoneId = { zone }
        )
        val home = capped.home()
        assertIs<AndroidHomePrimaryAction.DailyComplete>(home.model.primaryAction)
        assertEquals(20, home.model.dailyBudget!!.newCompletedToday)
        assertIs<AndroidStudyState.Failed>(capped.start(AndroidSessionEntry.REVIEW))

        val increased = AndroidStudyFacade(
            context, learner, { now }, dailyLimits = { DailyStudyBudgetLimits(50, 100) }, zoneId = { zone }
        )
        val intro = assertIs<AndroidStudyState.Introduction>(increased.start(AndroidSessionEntry.REVIEW))
        val session = context.engine.getSession(SessionId(intro.sessionId))!!
        assertEquals(packageId, session.installedPackageId)
        assertEquals(30, session.policy.newItemLimit)
        assertEquals(100, session.policy.reviewItemLimit)
        assertEquals(30, context.engine.getStudyQueueProgress(session.id)!!.effectiveNewWorkload)
    }

    private fun install(
        context: vn.loi.learning.infrastructure.LearningApplicationContext,
        count: Int
    ): InstalledPackageId {
        val ids = (0 until count).mapTo(linkedSetOf()) { index ->
            val contentId = ContentId("budget-content-$index")
            context.contentRepository!!.save(Content(contentId, ContentType.WORD, ContentText("word-$index", "meaning-$index")))
            context.learningItemRepository!!.save(
                LearningItem(LearningItemId("budget-item-$index"), contentId, LearningMode.MEANING_RECOGNITION)
            )
            contentId
        }
        val contentLibraryId = ContentLibraryId("budget-library")
        val canonicalId = PackageId("budget-package")
        val installedId = InstalledPackageId("budget-package")
        context.contentLibraryRepository!!.save(ContentLibrary(contentLibraryId, LibraryDescriptor("Budget"), ids))
        context.contentPackageRepository!!.save(ContentPackage(
            canonicalId, PackageDescriptor("Budget package", "1.0.0", "OPD3"), setOf(contentLibraryId)
        ))
        val libraryId = context.defaultLibraryId!!
        context.installedPackageRepository!!.save(InstalledPackage.reconstitute(
            installedId, libraryId, canonicalId, TopicId("budget-topic"), PackageName("Budget package"),
            PackageVersion("1.0.0"), PackageState.ACTIVE, Instant.EPOCH, count, count
        ))
        val library = context.domainLibraryRepository!!.findById(libraryId)!!
        context.domainLibraryRepository!!.save(library.registerEntry(installedId, canonicalId, Instant.EPOCH))
        context.libraryCommand!!.setActivePackage(libraryId, installedId)
        return installedId
    }
}
