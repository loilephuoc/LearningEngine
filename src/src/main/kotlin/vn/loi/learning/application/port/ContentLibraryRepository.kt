package vn.loi.learning.application.port

import vn.loi.learning.domain.content.library.model.ContentLibrary
import vn.loi.learning.domain.content.library.model.ContentLibraryId

/**
 * Port dùng để lưu và truy xuất ContentLibrary.
 *
 * Application chỉ biết contract này, không biết library được lưu
 * trong RAM, JSON, SQLite, server hay cloud.
 *
 * Việc đọc package nguồn và phân giải media không thuộc repository này.
 */
interface ContentLibraryRepository {

    fun findById(
        libraryId: ContentLibraryId
    ): ContentLibrary?

    fun save(
        library: ContentLibrary
    )

    fun deleteById(
        libraryId: ContentLibraryId
    )

    fun findAll(): List<ContentLibrary>
}
