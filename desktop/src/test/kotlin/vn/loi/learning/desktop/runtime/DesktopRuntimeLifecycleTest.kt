package vn.loi.learning.desktop.runtime

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue
import vn.loi.learning.infrastructure.LearningApplicationFactory

class DesktopRuntimeLifecycleTest {
    @Test
    fun `runtime configuration and bounded logs survive deterministic restart`() {
        val root = Files.createTempDirectory("desktop-runtime-restart-test")
        try {
            val directories = directories(root)
            Files.createDirectories(directories.config)
            val configurationFile =
                directories.config.resolve(DesktopRuntimeConfiguration.FILE_NAME)
            val configurationBytes =
                "schema.version=1\nlog.level=debug\nlog.retained.files=2\n".toByteArray()
            Files.write(configurationFile, configurationBytes)

            var sessionNumber = 0
            repeat(2) {
                sessionNumber += 1
                val session =
                    DesktopRuntimeLifecycle.start(
                        directories = directories,
                        buildMetadata = metadata(),
                        configurationLoader = DesktopRuntimeConfigurationLoader::load,
                        loggerFactory = { logs, configuration ->
                            FileDesktopRuntimeLogger.open(
                                logsDirectory = logs,
                                configuration = configuration,
                                clock =
                                    java.time.Clock.fixed(
                                        java.time.Instant.parse("2026-07-22T10:00:00Z"),
                                        java.time.ZoneOffset.UTC
                                    ),
                                sessionId = "restart-$sessionNumber"
                            )
                        },
                        applicationFactory = { LearningApplicationFactory.createInMemory() }
                    )

                assertEquals(DesktopLogLevel.DEBUG, session.configuration.logLevel)
                session.close()
            }

            assertContentEquals(configurationBytes, Files.readAllBytes(configurationFile))

            val logFiles =
                Files.list(directories.logs).use { paths ->
                    paths.sorted().toList()
                }
            assertEquals(2, logFiles.size)
            logFiles.forEach { logFile ->
                val content = Files.readString(logFile)
                assertTrue(content.contains("RUNTIME_STARTED"))
                assertTrue(content.contains("RUNTIME_STOPPED"))
            }
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun `startup creates directories composes persisted data path and shutdown logs once`() {
        val root = Files.createTempDirectory("desktop-runtime-lifecycle-test")
        try {
            val directories = directories(root)
            val applicationPaths = mutableListOf<Path>()

            val session =
                DesktopRuntimeLifecycle.start(
                    directories = directories,
                    buildMetadata = metadata(),
                    configurationLoader = { DesktopRuntimeConfiguration() },
                    loggerFactory = { logs, configuration ->
                        FileDesktopRuntimeLogger.open(
                            logsDirectory = logs,
                            configuration = configuration,
                            clock =
                                java.time.Clock.fixed(
                                    java.time.Instant.parse("2026-07-22T09:00:00Z"),
                                    java.time.ZoneOffset.UTC
                                ),
                            sessionId = "lifecycle"
                        )
                    },
                    applicationFactory = { dataPath ->
                        applicationPaths.add(dataPath)
                        LearningApplicationFactory.createInMemory()
                    }
                )

            assertEquals(listOf(directories.data), applicationPaths)
            listOf(
                directories.data,
                directories.config,
                directories.cache,
                directories.logs,
                directories.temp
            ).forEach { path -> assertTrue(Files.isDirectory(path)) }

            session.close()
            session.close()

            val content = Files.readString(session.logFile)
            assertEquals(1, "RUNTIME_STARTED".toRegex().findAll(content).count())
            assertEquals(1, "RUNTIME_STOPPED".toRegex().findAll(content).count())
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun `startup composition failure remains primary and logger is closed`() {
        val root = Files.createTempDirectory("desktop-runtime-start-failure-test")
        try {
            val expected = IllegalStateException("private startup detail")

            val failure =
                assertFailsWith<IllegalStateException> {
                    DesktopRuntimeLifecycle.start(
                        directories = directories(root),
                        buildMetadata = metadata(),
                        configurationLoader = { DesktopRuntimeConfiguration() },
                        loggerFactory = { logs, configuration ->
                            FileDesktopRuntimeLogger.open(
                                logsDirectory = logs,
                                configuration = configuration,
                                sessionId = "failed"
                            )
                        },
                        applicationFactory = { throw expected }
                    )
                }

            assertSame(expected, failure)

            val logFile =
                Files.list(root.resolve("logs")).use { paths ->
                    paths.toList().single()
                }
            val logContent = Files.readString(logFile)
            assertTrue(logContent.contains("RUNTIME_START_FAILED"))
            assertTrue(logContent.contains("java.lang.IllegalStateException"))
            assertTrue(!logContent.contains("private startup detail"))
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    private fun directories(root: Path): DesktopRuntimeDirectories =
        DesktopRuntimeDirectories(
            data = root.resolve("data"),
            config = root.resolve("config"),
            cache = root.resolve("cache"),
            logs = root.resolve("logs"),
            temp = root.resolve("temp"),
            legacyDataInUse = false
        )

    private fun metadata(): DesktopBuildMetadata =
        DesktopBuildMetadata(
            applicationVersion = "1.0",
            buildChannel = "test",
            buildRevision = "revision",
            buildNumber = "1"
        )
}
