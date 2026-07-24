package vn.loi.learning.application.contentpackaging

import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId

/**
 * Sinh PackageId ổn định từ PackageDescriptor trong Application Layer.
 */
class Sha256PackageIdGenerator(
    private val hasher: PackageIntegrityHasher = Sha256PackageIntegrityHasher()
) : PackageIdGenerator {

    override fun generate(
        descriptor: PackageDescriptor
    ): PackageId {
        val seed = listOf(
            descriptor.name,
            descriptor.version,
            descriptor.format
        ).joinToString("|")

        val hash = hasher.hash(seed).take(ID_LENGTH)

        return PackageId("package-$hash")
    }

    private companion object {
        const val ID_LENGTH = 24
    }
}
