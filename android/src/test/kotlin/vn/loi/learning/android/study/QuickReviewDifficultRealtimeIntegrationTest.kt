package vn.loi.learning.android.study

import androidx.lifecycle.SavedStateHandle
import java.time.Instant
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
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
import vn.loi.learning.application.review.ReviewCommand
import vn.loi.learning.domain.content.library.model.ContentLibrary
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.library.model.LibraryDescriptor
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.InstalledPackage
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.PackageName
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.library.model.PackageVersion
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.learning.model.LearningMode
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.session.model.FocusedPracticeKind
import vn.loi.learning.infrastructure.LearningApplicationContext
import vn.loi.learning.infrastructure.LearningApplicationFactory

@OptIn(ExperimentalCoroutinesApi::class)
class QuickReviewDifficultRealtimeIntegrationTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `ProjectHome refreshes canonical difficult eligibility and latest Good removes older Again`() =
        runTest(dispatcher) {
            val context = LearningApplicationFactory.createInMemory()
            val itemId = installLearnedWord(context)
            val learner = LearnerId("default-learner")
            context.engine.review(ReviewCommand(
                ReviewEventId("initial-good"), learner, itemId, ReviewRating.GOOD, Moment(1_000)
            ))
            var now = 1_700_000_000_000L
            val viewModel = AndroidStudyViewModel(
                AndroidStudyFacade(context, learner, now = { now++ }), SavedStateHandle(), dispatcher
            )
            advanceUntilIdle()

            viewModel.onEvent(AndroidStudyEvent.Start(AndroidSessionEntry.QUICK_REVIEW))
            advanceUntilIdle()
            viewModel.onEvent(AndroidStudyEvent.RevealIntroduction)
            advanceUntilIdle()
            viewModel.onEvent(AndroidStudyEvent.RateIntroduction(ReviewRating.AGAIN))
            advanceUntilIdle()
            viewModel.onEvent(AndroidStudyEvent.ProjectHome)
            advanceUntilIdle()
            assertTrue(assertIs<AndroidStudyState.Home>(viewModel.state.value).availability.canStartDifficultPractice)

            viewModel.onEvent(AndroidStudyEvent.Start(AndroidSessionEntry.DIFFICULT))
            advanceUntilIdle()
            assertTrue(
                assertIs<AndroidStudyState.Introduction>(viewModel.state.value).focusedPracticeKind ==
                    FocusedPracticeKind.DIFFICULT
            )

            viewModel.onEvent(AndroidStudyEvent.Start(AndroidSessionEntry.QUICK_REVIEW))
            advanceUntilIdle()
            viewModel.onEvent(AndroidStudyEvent.RevealIntroduction)
            advanceUntilIdle()
            viewModel.onEvent(AndroidStudyEvent.RateIntroduction(ReviewRating.GOOD))
            advanceUntilIdle()
            viewModel.onEvent(AndroidStudyEvent.ProjectHome)
            advanceUntilIdle()
            assertFalse(assertIs<AndroidStudyState.Home>(viewModel.state.value).availability.canStartDifficultPractice)
        }

    private fun installLearnedWord(context: LearningApplicationContext): LearningItemId {
        val contentId = ContentId("phase4-content")
        val itemId = LearningItemId("phase4-item")
        context.contentRepository!!.save(Content(
            contentId, ContentType.WORD, ContentText("insight", "thông tin")
        ))
        context.learningItemRepository!!.save(LearningItem(
            itemId, contentId, LearningMode.MEANING_RECOGNITION
        ))
        val contentLibraryId = ContentLibraryId("phase4-library")
        val packageId = PackageId("phase4-package")
        val installedId = InstalledPackageId("phase4-installed")
        context.contentLibraryRepository!!.save(ContentLibrary(
            contentLibraryId, LibraryDescriptor("Phase 4"), setOf(contentId)
        ))
        context.contentPackageRepository!!.save(ContentPackage(
            packageId, PackageDescriptor("Phase 4", "1.0.0", "OPD3"), setOf(contentLibraryId)
        ))
        val libraryId = context.defaultLibraryId!!
        context.installedPackageRepository!!.save(InstalledPackage.reconstitute(
            installedId, libraryId, packageId, TopicId("phase4-topic"), PackageName("Phase 4"),
            PackageVersion("1.0.0"), PackageState.ACTIVE, Instant.EPOCH, 1, 1
        ))
        val library = context.domainLibraryRepository!!.findById(libraryId)!!
        context.domainLibraryRepository!!.save(library.registerEntry(installedId, packageId, Instant.EPOCH))
        context.libraryCommand!!.setActivePackage(libraryId, installedId)
        return itemId
    }
}
