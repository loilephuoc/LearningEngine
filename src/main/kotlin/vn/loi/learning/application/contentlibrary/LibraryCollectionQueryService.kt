package vn.loi.learning.application.contentlibrary

import vn.loi.learning.application.port.LibraryCollectionRepository
import vn.loi.learning.domain.content.library.model.ContentLibraryId

/**
 * Application query service đọc danh sách collection của một ContentLibrary.
 *
 * Service:
 * - chỉ đọc thông qua LibraryCollectionRepository;
 * - không trả trực tiếp Domain aggregate cho Presentation Layer;
 * - trả kết quả ổn định theo tên và ID;
 * - công khai PackageId dưới dạng String đã được sắp xếp ổn định.
 */
class LibraryCollectionQueryService(
    private val libraryCollectionRepository: LibraryCollectionRepository
) {

    fun query(
        libraryId: ContentLibraryId
    ): List<LibraryCollectionItem> =
        libraryCollectionRepository
            .findAllByLibraryId(
                libraryId
            )
            .sortedWith(
                compareBy(
                    { collection ->
                        collection.name.lowercase()
                    },
                    { collection ->
                        collection.id.value
                    }
                )
            )
            .map { collection ->
                LibraryCollectionItem(
                    id = collection.id.value,
                    libraryId = collection.libraryId.value,
                    name = collection.name,
                    packageIds =
                        collection.packageIds
                            .map { packageId ->
                                packageId.value
                            }
                            .sorted()
                )
            }
}