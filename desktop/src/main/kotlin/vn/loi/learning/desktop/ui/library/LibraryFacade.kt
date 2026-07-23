package vn.loi.learning.desktop.ui.library

import vn.loi.learning.application.library.query.LibraryNavigationTree
import vn.loi.learning.domain.library.model.LibraryId
import vn.loi.learning.infrastructure.LearningApplicationContext

/**
 * Presentation Facade cho Desktop Library.
 *
 * Chỉ giao tiếp với LP-002 application query boundary (LibraryQueryService),
 * không gọi repository trực tiếp và không làm biến đổi (mutate) aggregates.
 */
open class LibraryFacade(
    private val applicationContext: LearningApplicationContext,
    val libraryId: LibraryId
) {
    open fun loadNavigationTree(): LibraryNavigationTree {
        val queryService = applicationContext.libraryQuery
            ?: throw LibraryServiceUnavailableException()
        return queryService.getNavigationTree(libraryId)
            ?: throw LibraryNotFoundException(libraryId)
    }
}
