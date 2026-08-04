package vn.loi.learning.android.acceptance

import java.time.Instant
import org.junit.Test
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.model.ContentText
import vn.loi.learning.domain.content.model.ContentType
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.InstalledPackage
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.PackageName
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.library.model.PackageVersion
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AndroidPersistedStartupCompatibilityTest {
    @Test
    fun `startup graph reloads existing content and installed package without empty fallback`() {
        AndroidAcceptanceFixture.create().use { fixture ->
            val first = fixture.graph.engine
            val contentId = ContentId("persisted-vietnamese-content")
            first.contentRepository!!.save(
                Content(contentId, ContentType.WORD, ContentText("bed", "cái giường"))
            )
            val libraryId = requireNotNull(first.defaultLibraryId)
            val installedId = InstalledPackageId("persisted-import")
            val packageId = PackageId("persisted-opd3")
            first.installedPackageRepository!!.save(
                InstalledPackage.reconstitute(
                    id = installedId,
                    libraryId = libraryId,
                    packageId = packageId,
                    topicId = TopicId("persisted-topic"),
                    name = PackageName("Persisted OPD3"),
                    version = PackageVersion("1.0.0"),
                    state = PackageState.ACTIVE,
                    installedAt = Instant.EPOCH,
                    contentCount = 1,
                    learningItemCount = 0
                )
            )
            val libraryRepository = requireNotNull(first.domainLibraryRepository)
            libraryRepository.save(
                requireNotNull(libraryRepository.findById(libraryId))
                    .registerEntry(installedId, packageId, Instant.EPOCH)
            )

            val restarted = fixture.restartedGraph().engine

            assertEquals("cái giường", restarted.contentRepository!!.findById(contentId)?.text?.translatedText)
            val libraryQuery = requireNotNull(restarted.libraryQuery)
            assertEquals("Persisted OPD3", libraryQuery.getPackageSummary(installedId)?.name)
            assertNotNull(libraryQuery.getNavigationTree(libraryId))
        }
    }
}
