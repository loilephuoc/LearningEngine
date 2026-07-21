package vn.loi.learning.infrastructure.persistence.mapper

import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.library.model.LibraryCollection
import vn.loi.learning.domain.content.library.model.LibraryCollectionDescriptor
import vn.loi.learning.domain.content.library.model.LibraryCollectionId
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.infrastructure.persistence.record.LibraryCollectionRecord

object LibraryCollectionRecordMapper {

    fun toRecord(
        collection: LibraryCollection
    ): LibraryCollectionRecord =
        LibraryCollectionRecord(
            id = collection.id.toString(),
            libraryId = collection.libraryId.toString(),
            name = collection.name,
            packageIds =
                collection.packageIds
                    .map(PackageId::toString)
                    .toSet()
        )

    fun toDomain(
        record: LibraryCollectionRecord
    ): LibraryCollection =
        LibraryCollection(
            id =
                LibraryCollectionId(
                    record.id
                ),
            libraryId =
                ContentLibraryId(
                    record.libraryId
                ),
            descriptor =
                LibraryCollectionDescriptor(
                    name = record.name
                ),
            packageIds =
                record.packageIds
                    .map(::PackageId)
                    .toSet()
        )
}