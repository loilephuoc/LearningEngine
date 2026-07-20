package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId

class DefaultPackageInstallerTest {

    @Test
    fun `installer creates content package from candidate descriptor and generated id`() {
        val candidate = PackageScanCandidate(
            source = "C:/packages/english.opd3"
        )

        val descriptor = PackageDescriptor(
            name = "English Elementary",
            version = "1.0.0",
            format = "OPD3"
        )

        val expectedId = PackageId(
            "package-test-id"
        )

        val installer = DefaultPackageInstaller(
            descriptorReader = PackageDescriptorReader { receivedCandidate ->
                assertEquals(candidate, receivedCandidate)
                descriptor
            },
            packageIdGenerator = PackageIdGenerator { receivedDescriptor ->
                assertEquals(descriptor, receivedDescriptor)
                expectedId
            }
        )

        val result = installer.install(candidate)

        assertEquals(expectedId, result.id)
        assertEquals(descriptor, result.descriptor)
        assertTrue(result.libraryIds.isEmpty())
    }
}
