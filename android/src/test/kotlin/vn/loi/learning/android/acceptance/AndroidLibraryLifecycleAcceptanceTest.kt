package vn.loi.learning.android.acceptance

import androidx.lifecycle.SavedStateHandle
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import vn.loi.learning.android.library.AndroidLibraryFacade
import vn.loi.learning.android.library.AndroidLibraryState
import vn.loi.learning.android.library.AndroidLibraryViewModel
import vn.loi.learning.domain.content.library.model.ContentLibrary
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.library.model.LibraryDescriptor
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.InstalledPackage
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.PackageName
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.library.model.PackageVersion
import vn.loi.learning.infrastructure.LearningApplicationFactory
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.assertNull

class AndroidLibraryLifecycleAcceptanceTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `process recreation restores selected content through canonical browser`() = runTest(dispatcher) {
        val fixture = fixture()
        val saved = SavedStateHandle(
            mapOf(
                "library.package" to fixture.packageId.value,
                "library.content" to fixture.contentId.value,
                "library.package.query" to "bed"
            )
        )

        val viewModel = AndroidLibraryViewModel(AndroidLibraryFacade(fixture.context), saved, dispatcher)
        advanceUntilIdle()

        val state = assertIs<AndroidLibraryState.PackageBrowser>(viewModel.state.value)
        assertEquals(fixture.contentId.value, state.selectedContentId)
        assertEquals("bed", state.criteria.query)
        assertEquals(listOf(fixture.contentId), state.visibleItems.map { it.contentId })
    }

    @Test
    fun `process recreation restores global query through canonical search`() = runTest(dispatcher) {
        val fixture = fixture()
        val saved = SavedStateHandle(mapOf("library.root.query" to "Acceptance"))

        val viewModel = AndroidLibraryViewModel(AndroidLibraryFacade(fixture.context), saved, dispatcher)
        advanceUntilIdle()

        val state = assertIs<AndroidLibraryState.Root>(viewModel.state.value)
        assertEquals("Acceptance", state.query)
        assertEquals(listOf(fixture.packageId.value), state.packages.map { it.packageId })
    }

    @Test
    fun `stale package destination falls back to safe library root`() = runTest(dispatcher) {
        val fixture = fixture()
        val saved = SavedStateHandle(
            mapOf(
                "library.package" to "uninstalled-package",
                "library.content" to fixture.contentId.value
            )
        )

        val viewModel = AndroidLibraryViewModel(AndroidLibraryFacade(fixture.context), saved, dispatcher)
        advanceUntilIdle()

        assertIs<AndroidLibraryState.Root>(viewModel.state.value)
        assertEquals(null, saved.get<String>("library.package"))
        assertEquals(null, saved.get<String>("library.content"))
    }

    @Test
    fun `rapid global query change cannot publish cancelled stale results`() = runTest(dispatcher) {
        val fixture = fixture()
        val viewModel = AndroidLibraryViewModel(
            AndroidLibraryFacade(fixture.context), SavedStateHandle(), dispatcher
        )
        advanceUntilIdle()

        viewModel.globalSearch("Acceptance")
        viewModel.globalSearch("absent")
        advanceUntilIdle()

        val state = assertIs<AndroidLibraryState.Root>(viewModel.state.value)
        assertEquals("absent", state.query)
        assertTrue(state.packages.isEmpty())
    }

    @Test
    fun `Library package Study selects canonical package before creating session`() {
        val fixture = fixture()
        assertNull(fixture.context.domainLibraryRepository!!.findById(fixture.context.defaultLibraryId!!)!!.activePackageId)

        val started = assertIs<AndroidLibraryState.StudyStarted>(
            AndroidLibraryFacade(fixture.context).startPackage(fixture.packageId)
        )

        assertEquals(fixture.packageId,
            fixture.context.domainLibraryRepository!!.findById(fixture.context.defaultLibraryId!!)!!.activePackageId)
        assertEquals(fixture.packageId,
            fixture.context.engine.getSession(vn.loi.learning.domain.study.session.model.SessionId(started.sessionId))!!.installedPackageId)
    }

    private fun fixture(): Fixture {
        val context = LearningApplicationFactory.createInMemory()
        val libraryId = requireNotNull(context.defaultLibraryId)
        val canonicalPackageId = PackageId("acceptance-canonical-package")
        val packageId = InstalledPackageId(canonicalPackageId.value)
        val contentId = ContentId("acceptance-bed")
        val contentLibraryId = ContentLibraryId(canonicalPackageId.value)

        context.contentRepository!!.save(
            Content(contentId, ContentType.WORD, ContentText("bed", "cái giường"))
        )
        context.learningItemRepository!!.save(
            vn.loi.learning.domain.study.learning.model.LearningItem(
                vn.loi.learning.domain.study.learning.model.LearningItemId("acceptance-item"),
                contentId,
                vn.loi.learning.domain.study.learning.model.LearningMode.MEANING_RECOGNITION
            )
        )
        context.contentLibraryRepository!!.save(
            ContentLibrary(contentLibraryId, LibraryDescriptor("Acceptance package"), setOf(contentId))
        )
        context.contentPackageRepository!!.save(
            ContentPackage(
                canonicalPackageId,
                PackageDescriptor("Acceptance package", "1.0.0", "OPD3"),
                setOf(contentLibraryId)
            )
        )
        context.installedPackageRepository!!.save(
            InstalledPackage.reconstitute(
                id = packageId,
                libraryId = libraryId,
                packageId = canonicalPackageId,
                topicId = TopicId("acceptance-topic"),
                name = PackageName("Acceptance package"),
                version = PackageVersion("1.0.0"),
                state = PackageState.ACTIVE,
                installedAt = Instant.EPOCH,
                contentCount = 1,
                learningItemCount = 1
            )
        )
        val libraryRepository = requireNotNull(context.domainLibraryRepository)
        val library = requireNotNull(libraryRepository.findById(libraryId))
        libraryRepository.save(library.registerEntry(packageId, canonicalPackageId, Instant.EPOCH))
        return Fixture(context, packageId, contentId)
    }

    private data class Fixture(
        val context: vn.loi.learning.infrastructure.LearningApplicationContext,
        val packageId: InstalledPackageId,
        val contentId: ContentId
    )
}
