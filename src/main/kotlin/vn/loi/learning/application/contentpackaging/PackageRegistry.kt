package vn.loi.learning.application.contentpackaging

import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId

/**
 * Điều phối việc cài đặt và đăng ký một package candidate.
 *
 * Registry không truy cập repository trực tiếp.
 * Việc lưu package và cập nhật catalog được ủy quyền cho
 * RegisterContentPackageUseCase để giữ đúng transaction boundary.
 */
class PackageRegistry(
    private val packageInstaller: PackageInstaller,
    private val registerContentPackageUseCase: RegisterContentPackageUseCase
) {

    fun register(
        catalogId: PackageCatalogId,
        candidate: PackageScanCandidate
    ): ContentPackage {
        val contentPackage =
            packageInstaller.install(candidate)

        registerContentPackageUseCase.execute(
            RegisterContentPackageCommand(
                catalogId = catalogId,
                contentPackage = contentPackage
            )
        )

        return contentPackage
    }
}
