package vn.loi.learning.application.contentpackaging

import vn.loi.learning.domain.content.packaging.model.ContentPackage

/**
 * PackageInstaller mặc định của Application.
 *
 * Installer chỉ điều phối việc đọc descriptor, tạo PackageId
 * và khởi tạo ContentPackage bất biến.
 */
class DefaultPackageInstaller(
    private val descriptorReader: PackageDescriptorReader,
    private val packageIdGenerator: PackageIdGenerator
) : PackageInstaller {

    override fun install(
        candidate: PackageScanCandidate
    ): ContentPackage {
        val descriptor =
            descriptorReader.read(candidate)

        val packageId =
            packageIdGenerator.generate(descriptor)

        return ContentPackage(
            id = packageId,
            descriptor = descriptor
        )
    }
}
