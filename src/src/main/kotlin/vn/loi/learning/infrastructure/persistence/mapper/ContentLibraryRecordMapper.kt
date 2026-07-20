package vn.loi.learning.infrastructure.persistence.mapper

import vn.loi.learning.domain.content.library.model.ContentLibrary
import vn.loi.learning.domain.content.library.model.ContentLibraryId
import vn.loi.learning.domain.content.library.model.LibraryDescriptor
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.infrastructure.persistence.record.ContentLibraryRecord

object ContentLibraryRecordMapper {

    fun toRecord(
        contentLibrary: ContentLibrary
    ): ContentLibraryRecord =
        ContentLibraryRecord(
            id = contentLibrary.id.toString(),
            name = contentLibrary.descriptor.name,
            contentIds = contentLibrary.contentIds
                .map(ContentId::toString)
                .toSet()
        )

    fun toDomain(
        record: ContentLibraryRecord
    ): ContentLibrary =
        ContentLibrary(
            id = ContentLibraryId(record.id),
            descriptor = LibraryDescriptor(
                name = record.name
            ),
            contentIds = record.contentIds
                .map(::ContentId)
                .toSet()
        )
}