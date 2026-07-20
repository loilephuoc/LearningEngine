package vn.loi.learning.infrastructure.persistence.mapper

import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId

class PackageRecordMapperTest {

    @Test
    fun `maps content package to record and back`() {
        val contentPackage = ContentPackage(
            id = PackageId("package-english"),
            descriptor = PackageDescriptor(
                name = "English Elementary",
                version = "1.0.0",
                format = "OPD3"
            ),
            libraryIds = setOf(
                ContentLibraryId("library-vocabulary"),
                ContentLibraryId("library-conversations")
            )
        )

        val record =
            PackageRecordMapper.toRecord(contentPackage)

        val restored =
            PackageRecordMapper.toDomain(record)

        assertEquals(contentPackage, restored)
    }
}
