package vn.loi.learning.desktop.runtime

import java.io.BufferedWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

interface DesktopRuntimeLogger : AutoCloseable {
    val filePath: Path

    fun log(
        level: DesktopLogLevel,
        eventCode: String,
        message: String
    )
}

class FileDesktopRuntimeLogger private constructor(
    override val filePath: Path,
    private val minimumLevel: DesktopLogLevel,
    private val clock: Clock,
    private val writer: BufferedWriter
) : DesktopRuntimeLogger {
    private var closed = false

    override fun log(
        level: DesktopLogLevel,
        eventCode: String,
        message: String
    ) {
        check(!closed) { "Desktop runtime logger is closed." }
        require(eventCode.matches(EVENT_CODE_PATTERN)) {
            "Desktop log event code must contain only A-Z, 0-9, and underscores."
        }

        if (level.ordinal > minimumLevel.ordinal) {
            return
        }

        writer.append(clock.instant().toString())
        writer.append('\t')
        writer.append(level.name)
        writer.append('\t')
        writer.append(eventCode)
        writer.append('\t')
        writer.append(sanitize(message))
        writer.newLine()
        writer.flush()
    }

    override fun close() {
        if (!closed) {
            closed = true
            writer.close()
        }
    }

    companion object {
        private val EVENT_CODE_PATTERN = Regex("[A-Z0-9_]+")
        private val FILE_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS")
                .withZone(ZoneOffset.UTC)

        fun open(
            logsDirectory: Path,
            configuration: DesktopRuntimeConfiguration,
            clock: Clock = Clock.systemUTC(),
            sessionId: String = UUID.randomUUID().toString()
        ): FileDesktopRuntimeLogger {
            require(sessionId.matches(Regex("[A-Za-z0-9-]+"))) {
                "Desktop log session ID contains unsupported characters."
            }

            Files.createDirectories(logsDirectory)

            val filePath =
                logsDirectory.resolve(
                    "learning-engine-" +
                        FILE_TIMESTAMP.format(clock.instant()) +
                        "-$sessionId.log"
                )

            val writer =
                Files.newBufferedWriter(
                    filePath,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE
                )

            return try {
                enforceRetention(
                    logsDirectory = logsDirectory,
                    retainedLogFiles = configuration.retainedLogFiles
                )

                FileDesktopRuntimeLogger(
                    filePath = filePath,
                    minimumLevel = configuration.logLevel,
                    clock = clock,
                    writer = writer
                )
            } catch (failure: Throwable) {
                writer.close()
                Files.deleteIfExists(filePath)
                throw failure
            }
        }

        internal fun enforceRetention(
            logsDirectory: Path,
            retainedLogFiles: Int
        ) {
            require(retainedLogFiles > 0) {
                "Retained log files must be positive."
            }

            val candidates =
                Files.list(logsDirectory).use { paths ->
                    paths.filter { path ->
                        Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) &&
                            path.fileName.toString().matches(LOG_FILE_PATTERN)
                    }.sorted(
                        compareByDescending<Path> { path ->
                            Files.getLastModifiedTime(path).toMillis()
                        }.thenByDescending { path ->
                            path.fileName.toString()
                        }
                    ).toList()
                }

            candidates.drop(retainedLogFiles).forEach(Files::delete)
        }

        private val LOG_FILE_PATTERN =
            Regex("learning-engine-[0-9]{8}-[0-9]{6}-[0-9]{3}-[A-Za-z0-9-]+\\.log")

        private fun sanitize(message: String): String =
            message
                .replace('\r', ' ')
                .replace('\n', ' ')
                .trim()
    }
}
