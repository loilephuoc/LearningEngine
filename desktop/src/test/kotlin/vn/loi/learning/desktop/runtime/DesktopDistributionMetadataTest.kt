package vn.loi.learning.desktop.runtime

import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DesktopDistributionMetadataTest {
    @Test
    fun `generated metadata matches native Windows package contract`() {
        assertEquals(
            DesktopDistributionMetadata(
                packageName = "LearningEngine",
                packageVersion = "1.0.0",
                windowsFormats = listOf("msi", "exe")
            ),
            DesktopDistributionMetadataLoader.load()
        )
    }

    @Test
    fun `loader rejects missing distribution fields`() {
        assertFailsWith<IllegalStateException> {
            DesktopDistributionMetadataLoader.load(
                ByteArrayInputStream(
                    "distribution.package.name=LearningEngine\n".toByteArray()
                )
            )
        }
    }

    @Test
    fun `contract rejects unsupported formats and invalid versions`() {
        assertFailsWith<IllegalArgumentException> {
            DesktopDistributionMetadata("LearningEngine", "1.0-SNAPSHOT", listOf("exe"))
        }
        assertFailsWith<IllegalArgumentException> {
            DesktopDistributionMetadata("LearningEngine", "1.0.0", listOf("zip"))
        }
    }
}
