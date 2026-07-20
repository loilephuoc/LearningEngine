package vn.loi.learning.infrastructure.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor

class Sha256PackageIdGeneratorTest {

    private val generator = Sha256PackageIdGenerator()

    @Test
    fun `same descriptor produces same package id`() {
        val descriptor = PackageDescriptor(
            name = "English Elementary",
            version = "1.0.0",
            format = "OPD3"
        )

        val first = generator.generate(descriptor)
        val second = generator.generate(descriptor)

        assertEquals(first, second)
    }

    @Test
    fun `different descriptor produces different package id`() {
        val first = generator.generate(
            PackageDescriptor(
                name = "English Elementary",
                version = "1.0.0",
                format = "OPD3"
            )
        )

        val second = generator.generate(
            PackageDescriptor(
                name = "English Elementary",
                version = "2.0.0",
                format = "OPD3"
            )
        )

        assertNotEquals(first, second)
    }
}
