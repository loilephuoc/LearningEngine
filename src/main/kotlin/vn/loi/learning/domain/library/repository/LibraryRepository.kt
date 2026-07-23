package vn.loi.learning.domain.library.repository

import vn.loi.learning.domain.library.model.Library
import vn.loi.learning.domain.library.model.LibraryId

/**
 * Repository interface định nghĩa hợp đồng lưu trữ và truy vấn cho Aggregate Root Library.
 */
interface LibraryRepository {
    fun findById(id: LibraryId): Library?
    fun save(library: Library)
    fun existsById(id: LibraryId): Boolean
}
