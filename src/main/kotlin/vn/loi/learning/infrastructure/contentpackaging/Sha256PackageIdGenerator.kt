package vn.loi.learning.infrastructure.contentpackaging

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import vn.loi.learning.application.contentpackaging.PackageIdGenerator
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId

/**
 * Sinh PackageId ổn định từ PackageDescriptor.
 */
class Sha256PackageIdGenerator : PackageIdGenerator {

    override fun generate(
        descriptor: PackageDescriptor
    ): PackageId {
        val seed = listOf(
            descriptor.name,
            descriptor.version,
            descriptor.format
        ).joinToString("|")

        val hash = MessageDigest
            .getInstance("SHA-256")
            .digest(seed.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
            .take(ID_LENGTH)

        return PackageId("package-$hash")
    }

    private companion object {
        const val ID_LENGTH = 24
    }
}
