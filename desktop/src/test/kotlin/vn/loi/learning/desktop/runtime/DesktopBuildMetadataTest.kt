package vn.loi.learning.desktop.runtime

import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DesktopBuildMetadataTest {
    @Test
    fun `loads generated build metadata from classpath`() {
        val metadata = DesktopBuildMetadataLoader.load()

        assertEquals("1.0-SNAPSHOT", metadata.applicationVersion)
        assertEquals("development", metadata.buildChannel)
        assertEquals("local", metadata.buildRevision)
        assertEquals("0", metadata.buildNumber)
        assertEquals(
            "1.0-SNAPSHOT (development 0, local)",
            metadata.displayVersion
        )
    }

    @Test
    fun `rejects incomplete metadata deterministically`() {
        val failure =
            assertFailsWith<IllegalStateException> {
                DesktopBuildMetadataLoader.load(
                    ByteArrayInputStream(
                        "application.version=1.0\n".toByteArray()
                    )
                )
            }

        assertEquals(
            "Missing desktop build metadata property: build.channel",
            failure.message
        )
    }
}
