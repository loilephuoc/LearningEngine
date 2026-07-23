package vn.loi.learning.domain.library

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.event.CollectionCreatedEvent
import vn.loi.learning.domain.library.event.CollectionDeletedEvent
import vn.loi.learning.domain.library.event.CollectionRenamedEvent
import vn.loi.learning.domain.library.event.LibraryCreatedEvent
import vn.loi.learning.domain.library.event.PackageArchivedEvent
import vn.loi.learning.domain.library.event.PackageAssignedToCollectionEvent
import vn.loi.learning.domain.library.event.PackageInstalledEvent
import vn.loi.learning.domain.library.event.PackageRemovedEvent
import vn.loi.learning.domain.library.event.PackageRemovedFromCollectionEvent
import vn.loi.learning.domain.library.event.PackageRestoredEvent
import vn.loi.learning.domain.library.model.CollectionId
import vn.loi.learning.domain.library.model.CollectionName
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.library.model.LibraryId
import vn.loi.learning.domain.library.model.PackageVersion

class DomainEventsTest {

    private val libId = LibraryId("lib-1")
    private val instId = InstalledPackageId("inst-1")
    private val pkgId = PackageId("pkg-1")
    private val topicId = TopicId("topic-1")
    private val colId = CollectionId("col-1")
    private val colName = CollectionName("N3 Vocabulary")
    private val newColName = CollectionName("N3 Grammar")

    @Test
    fun `library domain events construct with timestamps`() {
        val now = Instant.now()

        val e1 = LibraryCreatedEvent(libId, "Main Library", now)
        assertEquals(libId, e1.libraryId)
        assertEquals(now, e1.occurredAt)

        val e2 = PackageInstalledEvent(instId, libId, pkgId, topicId, PackageVersion("1.0"), now)
        assertEquals(instId, e2.installedPackageId)

        val e3 = PackageArchivedEvent(instId, pkgId, now)
        assertEquals(instId, e3.installedPackageId)

        val e4 = PackageRestoredEvent(instId, pkgId, now)
        assertEquals(instId, e4.installedPackageId)

        val e5 = PackageRemovedEvent(instId, libId, pkgId, now)
        assertEquals(instId, e5.installedPackageId)

        val e6 = CollectionCreatedEvent(colId, libId, colName, now)
        assertEquals(colId, e6.collectionId)

        val e7 = CollectionRenamedEvent(colId, colName, newColName, now)
        assertEquals(colName, e7.oldName)

        val e8 = CollectionDeletedEvent(colId, libId, now)
        assertEquals(colId, e8.collectionId)

        val e9 = PackageAssignedToCollectionEvent(colId, instId, now)
        assertEquals(instId, e9.installedPackageId)

        val e10 = PackageRemovedFromCollectionEvent(colId, instId, now)
        assertEquals(instId, e10.installedPackageId)

        assertNotNull(e1.occurredAt)
    }
}
