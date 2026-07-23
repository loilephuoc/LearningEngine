package vn.loi.learning.domain.library

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.InstalledPackage
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.Library
import vn.loi.learning.domain.library.model.LibraryEntry
import vn.loi.learning.domain.library.model.LibraryId
import vn.loi.learning.domain.library.model.PackageName
import vn.loi.learning.domain.library.model.PackageState
import vn.loi.learning.domain.library.model.PackageVersion

class LibraryAggregateTest {

    private val libId = LibraryId("lib-main")
    private val pkgId1 = PackageId("pkg-kanji-n1")
    private val instId1 = InstalledPackageId("inst-1")
    private val topicId1 = TopicId("topic-1")

    private fun createSamplePackage(
        id: InstalledPackageId,
        packageId: PackageId,
        state: PackageState = PackageState.ACTIVE
    ): InstalledPackage = InstalledPackage.reconstitute(
        id = id,
        libraryId = libId,
        packageId = packageId,
        topicId = topicId1,
        name = PackageName("Sample Package"),
        version = PackageVersion("1.0"),
        state = state,
        installedAt = java.time.Instant.now(),
        contentCount = 10,
        learningItemCount = 20
    )

    @Test
    fun `Library factory creates aggregate and emits LibraryCreatedEvent`() {
        val mutation = Library.create(id = libId, name = "Main Library")
        val library = mutation.aggregate
        val event = mutation.event

        assertEquals(libId, library.id)
        assertEquals("Main Library", library.name)
        assertTrue(library.entries.isEmpty())
        assertEquals(libId, event.libraryId)
        assertEquals("Main Library", event.name)
    }

    @Test
    fun `Library creation rejects blank name`() {
        assertFailsWith<IllegalArgumentException> {
            Library.create(id = libId, name = "   ")
        }
    }

    @Test
    fun `Library rejects duplicate entry initializations`() {
        val duplicateEntries = listOf(
            LibraryEntry(instId1, pkgId1),
            LibraryEntry(instId1, pkgId1)
        )

        assertFailsWith<IllegalArgumentException> {
            Library.reconstitute(id = libId, name = "Main Library", entries = duplicateEntries)
        }
    }

    @Test
    fun `hasActivePackageForPackageId correctly queries installed packages state`() {
        val lib = Library.create(id = libId, name = "Main Library").aggregate.registerEntry(instId1, pkgId1)

        val activePkg = createSamplePackage(instId1, pkgId1, PackageState.ACTIVE)
        val archivedPkg = createSamplePackage(instId1, pkgId1, PackageState.ARCHIVED)

        assertTrue(lib.hasActivePackageForPackageId(pkgId1, listOf(activePkg)))
        assertFalse(lib.hasActivePackageForPackageId(pkgId1, listOf(archivedPkg)))
    }
}
