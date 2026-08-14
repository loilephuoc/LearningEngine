package vn.loi.learning.application.integrity

import java.time.Instant
import kotlin.test.*
import vn.loi.learning.domain.content.library.model.*
import vn.loi.learning.domain.content.model.*
import vn.loi.learning.domain.content.packaging.model.*
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.*
import vn.loi.learning.domain.study.learning.model.*
import vn.loi.learning.infrastructure.LearningApplicationFactory

class ReconcileIntermediatePublicTransportOrphanTest {
    @Test
    fun `exact runtime-like repair deletes only B and five items then retry is a no-op`() {
        val f = Fixture()
        val libraryBefore = f.contentLibrary.findById(ReconcileIntermediatePublicTransportOrphan.LIBRARY)
        val aBefore = f.contents.findById(ReconcileIntermediatePublicTransportOrphan.OWNED_A)

        assertEquals(IntermediateOrphanRepairStatus.REPAIRED, f.command.execute(true).status)
        assertNull(f.contents.findById(ReconcileIntermediatePublicTransportOrphan.ORPHAN_B))
        assertTrue(f.items.findByContentId(ReconcileIntermediatePublicTransportOrphan.ORPHAN_B).isEmpty())
        assertEquals(aBefore, f.contents.findById(ReconcileIntermediatePublicTransportOrphan.OWNED_A))
        assertEquals(libraryBefore, f.contentLibrary.findById(ReconcileIntermediatePublicTransportOrphan.LIBRARY))
        val installed = f.installed.findById(ReconcileIntermediatePublicTransportOrphan.INSTALLED)!!
        assertEquals(2255, installed.contentCount)
        assertEquals(11275, installed.learningItemCount)

        val snapshot = Triple(f.contents.findAll(), f.items.findAll(), installed)
        assertEquals(IntermediateOrphanRepairStatus.ALREADY_RECONCILED, f.command.execute(true).status)
        assertEquals(snapshot, Triple(f.contents.findAll(), f.items.findAll(), f.installed.findById(ReconcileIntermediatePublicTransportOrphan.INSTALLED)))
    }

    @Test
    fun `backup and membership guards refuse with zero mutation`() {
        val f = Fixture()
        val before = Triple(f.contents.findAll(), f.items.findAll(), f.installed.findAll())
        assertEquals(IntermediateOrphanRepairStatus.BACKUP_REQUIRED, f.command.execute(false).status)
        assertEquals(before, Triple(f.contents.findAll(), f.items.findAll(), f.installed.findAll()))

        f.contentLibrary.save(f.contentLibrary.findById(ReconcileIntermediatePublicTransportOrphan.LIBRARY)!!
            .register(ReconcileIntermediatePublicTransportOrphan.ORPHAN_B))
        assertEquals(IntermediateOrphanRepairStatus.PRECONDITION_FAILED, f.command.execute(true).status)
        assertNotNull(f.contents.findById(ReconcileIntermediatePublicTransportOrphan.ORPHAN_B))
        assertEquals(5, f.items.findByContentId(ReconcileIntermediatePublicTransportOrphan.ORPHAN_B).size)
        assertEquals(2256, f.installed.findById(ReconcileIntermediatePublicTransportOrphan.INSTALLED)!!.contentCount)
    }

    private class Fixture {
        private val context = LearningApplicationFactory.createInMemory()
        val contents = context.contentRepository!!
        val items = context.learningItemRepository!!
        val contentLibrary = context.contentLibraryRepository!!
        val installed = context.installedPackageRepository!!
        val command = context.intermediatePublicTransportRepair!!

        init {
            val owned = linkedSetOf<ContentId>()
            repeat(2255) { index ->
                val id = if (index == 1208) ReconcileIntermediatePublicTransportOrphan.OWNED_A else ContentId("owned-$index")
                owned += id
                contents.save(Content(id, ContentType.WORD, ContentText(if (index == 1208) "public transport" else "word-$index")))
                ReconcileIntermediatePublicTransportOrphan.EXPECTED_MODES.values.forEach { mode ->
                    items.save(LearningItem(LearningItemId("${id.value}-${mode.name}"), id, mode))
                }
            }
            val b = ReconcileIntermediatePublicTransportOrphan.ORPHAN_B
            contents.save(Content(b, ContentType.WORD, ContentText("public transport")))
            ReconcileIntermediatePublicTransportOrphan.EXPECTED_MODES.forEach { (id, mode) -> items.save(LearningItem(id, b, mode)) }
            contentLibrary.save(ContentLibrary(ReconcileIntermediatePublicTransportOrphan.LIBRARY, LibraryDescriptor("Vocabulary_in_Use_Intermediate"), owned))
            context.contentPackageRepository!!.save(ContentPackage(ReconcileIntermediatePublicTransportOrphan.PACKAGE,
                PackageDescriptor("Vocabulary_in_Use_Intermediate", "1.0.0", "OPD3"), setOf(ReconcileIntermediatePublicTransportOrphan.LIBRARY)))
            installed.save(InstalledPackage.reconstitute(ReconcileIntermediatePublicTransportOrphan.INSTALLED,
                LibraryId("default-library"), ReconcileIntermediatePublicTransportOrphan.PACKAGE, TopicId("topic-673e45241f74920672eb81e8"),
                PackageName("Vocabulary_in_Use_Intermediate"), PackageVersion("1.0.0"), PackageState.ACTIVE, Instant.EPOCH, 2256, 11280))
        }
    }
}
