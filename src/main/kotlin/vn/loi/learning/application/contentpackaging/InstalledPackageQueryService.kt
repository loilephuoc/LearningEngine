package vn.loi.learning.application.contentpackaging

import vn.loi.learning.application.port.ContentPackageRepository

/**
 * Dữ liệu package đã được chuẩn bị cho Presentation Layer.
 *
 * DTO này không để Desktop UI phụ thuộc trực tiếp
 * vào Domain model ContentPackage.
 */
data class InstalledPackageItem(
    val id: String,
    val name: String,
    val version: String,
    val format: String,
    val libraryCount: Int
)

/**
 * Application query service đọc danh sách package đã cài.
 *
 * Service:
 * - chỉ đọc thông qua ContentPackageRepository;
 * - không thay đổi repository;
 * - không chứa logic Compose hoặc filesystem;
 * - trả kết quả ổn định theo tên và phiên bản.
 */
class InstalledPackageQueryService(
    private val contentPackageRepository: ContentPackageRepository
) {

    fun query(): List<InstalledPackageItem> =
        contentPackageRepository
            .findAll()
            .sortedWith(
                compareBy(
                    { contentPackage ->
                        contentPackage.name.lowercase()
                    },
                    { contentPackage ->
                        contentPackage.version.lowercase()
                    },
                    { contentPackage ->
                        contentPackage.id.toString()
                    }
                )
            )
            .map { contentPackage ->
                InstalledPackageItem(
                    id = contentPackage.id.toString(),
                    name = contentPackage.name,
                    version = contentPackage.version,
                    format = contentPackage.format,
                    libraryCount = contentPackage.libraryCount
                )
            }
}