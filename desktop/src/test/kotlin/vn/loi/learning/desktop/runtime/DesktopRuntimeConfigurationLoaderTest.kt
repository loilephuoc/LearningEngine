package vn.loi.learning.desktop.runtime

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class DesktopRuntimeConfigurationLoaderTest {
    @Test
    fun `missing configuration returns typed defaults without creating a file`() {
        val directory = Files.createTempDirectory("desktop-config-missing-test")
        try {
            val file = directory.resolve(DesktopRuntimeConfiguration.FILE_NAME)

            assertEquals(
                DesktopRuntimeConfiguration(),
                DesktopRuntimeConfigurationLoader.load(file)
            )
            assertFalse(Files.exists(file))
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `loads valid typed configuration`() {
        val directory = Files.createTempDirectory("desktop-config-valid-test")
        try {
            val file = directory.resolve(DesktopRuntimeConfiguration.FILE_NAME)
            Files.writeString(
                file,
                "schema.version=1\nlog.level=debug\nlog.retained.files=25\n"
            )

            assertEquals(
                DesktopRuntimeConfiguration(
                    logLevel = DesktopLogLevel.DEBUG,
                    retainedLogFiles = 25
                ),
                DesktopRuntimeConfigurationLoader.load(file)
            )
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `corrupt configuration remains unchanged across loader recreation`() {
        val directory = Files.createTempDirectory("desktop-config-corrupt-test")
        try {
            val file = directory.resolve(DesktopRuntimeConfiguration.FILE_NAME)
            val bytes =
                "schema.version=1\nlog.level=private-value\nlog.retained.files=10\n"
                    .toByteArray()
            Files.write(file, bytes)

            repeat(2) {
                val failure =
                    assertFailsWith<InvalidDesktopConfigurationException> {
                        DesktopRuntimeConfigurationLoader.load(file)
                    }

                assertEquals(file, failure.filePath)
                assertEquals("log.level", failure.propertyName)
                assertFalse(failure.message.orEmpty().contains("private-value"))
            }

            assertContentEquals(bytes, Files.readAllBytes(file))
            assertEquals(
                listOf(DesktopRuntimeConfiguration.FILE_NAME),
                Files.list(directory).use { files ->
                    files.map { it.fileName.toString() }.toList()
                }
            )
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `rejects unsupported schema and out of range retention`() {
        val directory = Files.createTempDirectory("desktop-config-validation-test")
        try {
            val file = directory.resolve(DesktopRuntimeConfiguration.FILE_NAME)

            Files.writeString(
                file,
                "schema.version=2\nlog.level=info\nlog.retained.files=10\n"
            )
            assertEquals(
                "schema.version",
                assertFailsWith<InvalidDesktopConfigurationException> {
                    DesktopRuntimeConfigurationLoader.load(file)
                }.propertyName
            )

            Files.writeString(
                file,
                "schema.version=1\nlog.level=info\nlog.retained.files=0\n"
            )
            assertEquals(
                "log.retained.files",
                assertFailsWith<InvalidDesktopConfigurationException> {
                    DesktopRuntimeConfigurationLoader.load(file)
                }.propertyName
            )
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}
