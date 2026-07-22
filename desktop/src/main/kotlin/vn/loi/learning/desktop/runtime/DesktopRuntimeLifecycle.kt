package vn.loi.learning.desktop.runtime

import java.nio.file.Files
import java.nio.file.Path
import vn.loi.learning.infrastructure.LearningApplicationContext
import vn.loi.learning.infrastructure.LearningApplicationFactory

class DesktopRuntimeSession internal constructor(
    val directories: DesktopRuntimeDirectories,
    val buildMetadata: DesktopBuildMetadata,
    val configuration: DesktopRuntimeConfiguration,
    val applicationContext: LearningApplicationContext,
    val logFile: Path,
    val diagnostics: DesktopRuntimeDiagnostics,
    val windowPlacement: DesktopWindowPlacementSession,
    private val logger: DesktopRuntimeLogger
) : AutoCloseable {
    private var closed = false

    override fun close() {
        if (closed) {
            return
        }

        closed = true
        var failure: Throwable? = null

        try {
            logger.log(
                DesktopLogLevel.INFO,
                "RUNTIME_STOPPED",
                "Desktop runtime stopped"
            )
        } catch (logFailure: Throwable) {
            failure = logFailure
        }

        try {
            logger.close()
        } catch (closeFailure: Throwable) {
            if (failure == null) {
                failure = closeFailure
            } else {
                failure.addSuppressed(closeFailure)
            }
        }

        failure?.let { throw it }
    }
}

object DesktopRuntimeLifecycle {
    fun start(): DesktopRuntimeSession {
        val directories = DesktopRuntimeDirectoryResolver.resolve()

        return start(
            directories = directories,
            buildMetadata = DesktopBuildMetadataLoader.load(),
            configurationLoader = DesktopRuntimeConfigurationLoader::load,
            loggerFactory = { logsDirectory, configuration ->
                FileDesktopRuntimeLogger.open(logsDirectory, configuration)
            },
            applicationFactory = LearningApplicationFactory::createPersisted
        )
    }

    internal fun start(
        directories: DesktopRuntimeDirectories,
        buildMetadata: DesktopBuildMetadata,
        configurationLoader: (Path) -> DesktopRuntimeConfiguration,
        loggerFactory: (Path, DesktopRuntimeConfiguration) -> DesktopRuntimeLogger,
        applicationFactory: (Path) -> LearningApplicationContext
    ): DesktopRuntimeSession {
        createDirectories(directories)

        val configuration =
            configurationLoader(
                directories.config.resolve(DesktopRuntimeConfiguration.FILE_NAME)
            )

        val logger = loggerFactory(directories.logs, configuration)

        return try {
            logger.log(
                DesktopLogLevel.INFO,
                "RUNTIME_STARTED",
                "${DesktopApplicationIdentity.APPLICATION_ID} ${buildMetadata.displayVersion}"
            )

            val applicationContext = applicationFactory(directories.data)

            DesktopRuntimeSession(
                directories = directories,
                buildMetadata = buildMetadata,
                configuration = configuration,
                applicationContext = applicationContext,
                logFile = logger.filePath,
                diagnostics =
                    DesktopRuntimeDiagnosticsFactory.create(
                        directories = directories,
                        buildMetadata = buildMetadata,
                        logFile = logger.filePath
                    ),
                windowPlacement =
                    DesktopWindowPlacementSession.open(
                        directories.config.resolve(DesktopWindowPlacement.FILE_NAME)
                    ),
                logger = logger
            )
        } catch (failure: Throwable) {
            try {
                logger.log(
                    DesktopLogLevel.ERROR,
                    "RUNTIME_START_FAILED",
                    "Startup failure type: ${failure::class.qualifiedName ?: "unknown"}"
                )
            } catch (logFailure: Throwable) {
                failure.addSuppressed(logFailure)
            }

            try {
                logger.close()
            } catch (closeFailure: Throwable) {
                failure.addSuppressed(closeFailure)
            }

            throw failure
        }
    }

    private fun createDirectories(directories: DesktopRuntimeDirectories) {
        listOf(
            directories.data,
            directories.config,
            directories.cache,
            directories.logs,
            directories.temp
        ).distinct().forEach(Files::createDirectories)
    }
}
