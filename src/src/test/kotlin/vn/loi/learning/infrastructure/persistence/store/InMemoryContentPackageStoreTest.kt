package vn.loi.learning.infrastructure.persistence.store

import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.infrastructure.persistence.record.PackageRecord

class InMemoryContentPackageStoreTest {

    @Test
    fun `stores and returns package records`() {
        val initialRecord = PackageRecord(
            id = "package-one",
            name = "Package One",
            version = "1.0.0",
            format = "OPD3",
            libraryIds = setOf("library-one")
        )

        val store =
            InMemoryContentPackageStore(
                initialRecords = listOf(initialRecord)
            )

        assertEquals(
            listOf(initialRecord),
            store.loadAll()
        )

        val updatedRecord = PackageRecord(
            id = "package-two",
            name = "Package Two",
            version = "2.0.0",
            format = "OPD3",
            libraryIds = setOf(
                "library-two",
                "library-three"
            )
        )

        store.saveAll(
            listOf(updatedRecord)
        )

        assertEquals(
            listOf(updatedRecord),
            store.loadAll()
        )
    }
}
