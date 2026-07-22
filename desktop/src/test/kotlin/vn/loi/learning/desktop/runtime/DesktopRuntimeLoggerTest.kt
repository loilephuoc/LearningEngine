package vn.loi.learning.desktop.runtime

import java.nio.file.Files
import java.nio.file.attribute.FileTime
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopRuntimeLoggerTest {
    private val clock =
        Clock.fixed(
            Instant.parse("2026-07-22T08:30:45.123Z"),
            ZoneOffset.UTC
        )

    @Test
    fun `writes filtered single-line utf eight events and closes idempotently`() {
        val directory = Files.createTempDirectory("desktop-logger-write-test")
        try {
            val logger =
                FileDesktopRuntimeLogger.open(
                    logsDirectory = directory,
                    configuration =
                        DesktopRuntimeConfiguration(
                            logLevel = DesktopLogLevel.INFO
                        ),
                    clock = clock,
                    sessionId = "session-1"
                )

            logger.log(DesktopLogLevel.DEBUG, "DEBUG_EVENT", "not written")
            logger.log(DesktopLogLevel.INFO, "STARTED", "Học tập\nready")
            logger.close()
            logger.close()

            assertEquals(
                "learning-engine-20260722-083045-123-session-1.log",
                logger.filePath.fileName.toString()
            )

            val content = Files.readString(logger.filePath)
            assertTrue(
                content.contains(
                    "2026-07-22T08:30:45.123Z\tINFO\tSTARTED\tHọc tập ready"
                )
            )
            assertFalse(content.contains("not written"))
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `retention deletes only oldest matching application logs`() {
        val directory = Files.createTempDirectory("desktop-logger-retention-test")
        try {
            repeat(4) { index ->
                val file =
                    directory.resolve(
                        "learning-engine-2026072${index + 1}-080000-000-session-$index.log"
                    )
                Files.writeString(file, "old-$index")
                Files.setLastModifiedTime(file, FileTime.fromMillis(index.toLong()))
            }

            val unrelated = directory.resolve("user-notes.log")
            Files.writeString(unrelated, "keep")

            val logger =
                FileDesktopRuntimeLogger.open(
                    logsDirectory = directory,
                    configuration = DesktopRuntimeConfiguration(retainedLogFiles = 3),
                    clock = clock,
                    sessionId = "current"
                )
            logger.close()

            val names =
                Files.list(directory).use { paths ->
                    paths.map { it.fileName.toString() }.sorted().toList()
                }

            assertEquals(4, names.size)
            assertTrue(names.contains("user-notes.log"))
            assertTrue(names.contains(logger.filePath.fileName.toString()))
            assertFalse(
                names.contains(
                    "learning-engine-20260721-080000-000-session-0.log"
                )
            )
            assertFalse(
                names.contains(
                    "learning-engine-20260722-080000-000-session-1.log"
                )
            )
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}
