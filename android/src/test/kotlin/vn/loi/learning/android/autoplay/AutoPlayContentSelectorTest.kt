package vn.loi.learning.android.autoplay

import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test
import vn.loi.learning.application.review.ReviewCommand
import vn.loi.learning.domain.content.library.model.*
import vn.loi.learning.domain.content.model.*
import vn.loi.learning.domain.content.packaging.model.*
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.*
import vn.loi.learning.domain.study.learning.model.*
import vn.loi.learning.domain.study.memory.model.*
import vn.loi.learning.infrastructure.LearningApplicationContext
import vn.loi.learning.infrastructure.LearningApplicationFactory

class AutoPlayContentSelectorTest {

    @Test
    fun `selector accurately selects items based on canonical sources`() {
        val context = LearningApplicationFactory.createInMemory()
        val learner = LearnerId("default-learner")
        val installed = installPackage(context, "selector-pkg", count = 6)
        context.libraryCommand!!.setActivePackage(context.defaultLibraryId!!, installed)

        // item 0: GOOD (Learned)
        context.engine.review(
            ReviewCommand(ReviewEventId("e-0"), learner, LearningItemId("selector-pkg-item-0"), ReviewRating.GOOD, Moment(1_000))
        )
        // item 1: AGAIN (Difficult + Learned)
        context.engine.review(
            ReviewCommand(ReviewEventId("e-1"), learner, LearningItemId("selector-pkg-item-1"), ReviewRating.AGAIN, Moment(2_000))
        )
        // item 2: HARD (Difficult + Learned)
        context.engine.review(
            ReviewCommand(ReviewEventId("e-2"), learner, LearningItemId("selector-pkg-item-2"), ReviewRating.HARD, Moment(3_000))
        )
        // items 3, 4, 5: Unreviewed (New)

        val selector = AutoPlayContentSelector(
            context = context,
            learnerId = learner,
            resolveMedia = { "/resolved/$it" },
            now = { 10_000_000_000L } // far future so reviewed items are due
        )

        // 1. LEARNED
        val learned = selector.selectItems(AutoPlaySource.LEARNED)
        assertEquals(3, learned.size)
        val learnedHeadwords = learned.map { it.headword }.toSet()
        assertTrue(learnedHeadwords.contains("selector-pkg-0"))
        assertTrue(learnedHeadwords.contains("selector-pkg-1"))
        assertTrue(learnedHeadwords.contains("selector-pkg-2"))

        // 2. AGAIN_HARD
        val againHard = selector.selectItems(AutoPlaySource.AGAIN_HARD)
        assertEquals(2, againHard.size)
        val againHardHeadwords = againHard.map { it.headword }.toSet()
        assertTrue(againHardHeadwords.contains("selector-pkg-1"))
        assertTrue(againHardHeadwords.contains("selector-pkg-2"))

        // 3. RANDOM_ALL
        val randomAll = selector.selectItems(AutoPlaySource.RANDOM_ALL, randomSeed = 123L)
        assertEquals(6, randomAll.size)

        // 4. RANDOM_LEARNED
        val randomLearned = selector.selectItems(AutoPlaySource.RANDOM_LEARNED, randomSeed = 123L)
        assertEquals(3, randomLearned.size)

        // 5. Media resolution
        val item0 = learned.first { it.headword == "selector-pkg-0" }
        assertEquals("/resolved/audio/selector-pkg-0.mp3", item0.wordAudioPath)
    }

    private fun installPackage(
        context: LearningApplicationContext,
        name: String,
        count: Int
    ): InstalledPackageId {
        val contentIds = (0 until count).mapTo(linkedSetOf()) { index ->
            val suffix = "-$index"
            val contentId = ContentId("$name-content$suffix")
            context.contentRepository!!.save(
                Content(
                    contentId,
                    ContentType.WORD,
                    ContentText("$name$suffix", "$name-answer$suffix"),
                    media = ContentMedia(
                        primaryAudio = "audio/$name$suffix.mp3"
                    )
                )
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
