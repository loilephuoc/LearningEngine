package vn.loi.learning.application.contentpackaging

import java.util.Locale
import vn.loi.learning.domain.content.topic.model.TopicId

/**
 * Platform-neutral pairing and validation for legacy JSON + PKG topics.
 *
 * The folder reader owns platform I/O. This service owns logical-name matching, cardinality,
 * validation, deterministic ordering, and structured diagnostics. It performs no conversion,
 * media extraction, persistence, or OPD3 writing.
 */
class LegacyTopicPairDiscoveryService(
    private val folderReader:
    LegacyTopicFolderReader
) {

    fun discover(
        folder: String
    ): LegacyTopicDiscoveryResult {
        require(folder.isNotBlank()) {
            "Legacy topic discovery folder must not be blank."
        }

        val files =
            folderReader
                .read(folder)
                .sortedWith(fileComparator)
        val diagnostics =
            mutableListOf<LegacyTopicDiscoveryDiagnostic>()

        files
            .filter { file ->
                !file.readable
            }
            .forEach { file ->
                diagnostics +=
                    diagnostic(
                        code =
                            LegacyTopicDiscoveryDiagnosticCode.UNREADABLE_FILE,
                        logicalTopicName =
                            file.logicalBaseNameOrNull(),
                        sources =
                            listOf(file.source),
                        message =
                            "Legacy topic file is not readable: ${file.source}"
                    )
            }

        files
            .filter { file ->
                file.kind == LegacyTopicFileKind.UNSUPPORTED ||
                    !file.supportedFormat
            }
            .forEach { file ->
                diagnostics +=
                    diagnostic(
                        code =
                            LegacyTopicDiscoveryDiagnosticCode.UNSUPPORTED_FORMAT,
                        logicalTopicName =
                            file.logicalBaseNameOrNull(),
                        sources =
                            listOf(file.source),
                        message =
                            "Legacy topic file format is not supported: ${file.source}"
                    )
            }

        val candidateFiles =
            files.filter { file ->
                file.kind != LegacyTopicFileKind.UNSUPPORTED
            }
        val grouped =
            candidateFiles
                .groupBy { file ->
                    file.normalizedBaseName()
                }
                .toSortedMap()

        grouped.forEach { (_, topicFiles) ->
            val jsonFiles =
                topicFiles
                    .filter { file ->
                        file.kind == LegacyTopicFileKind.JSON
                    }
            val packageFiles =
                topicFiles
                    .filter { file ->
                        file.kind == LegacyTopicFileKind.PKG
                    }
            val logicalName =
                topicFiles
                    .minBy { file ->
                        file.fileName.lowercase(
                            Locale.ROOT
                        )
                    }
                    .logicalBaseName()

            if (jsonFiles.isEmpty()) {
                diagnostics +=
                    diagnostic(
                        code =
                            LegacyTopicDiscoveryDiagnosticCode.MISSING_JSON,
                        logicalTopicName =
                            logicalName,
                        sources =
                            packageFiles.map { file ->
                                file.source
                            },
                        message =
                            "Legacy topic '$logicalName' is missing its JSON file."
                    )
            } else if (jsonFiles.size > 1) {
                diagnostics +=
                    diagnostic(
                        code =
                            LegacyTopicDiscoveryDiagnosticCode.DUPLICATE_JSON,
                        logicalTopicName =
                            logicalName,
                        sources =
                            jsonFiles.map { file ->
                                file.source
                            },
                        message =
                            "Legacy topic '$logicalName' has multiple JSON files."
                    )
            }

            if (packageFiles.isEmpty()) {
                diagnostics +=
                    diagnostic(
                        code =
                            LegacyTopicDiscoveryDiagnosticCode.MISSING_PKG,
                        logicalTopicName =
                            logicalName,
                        sources =
                            jsonFiles.map { file ->
                                file.source
                            },
                        message =
                            "Legacy topic '$logicalName' is missing its PKG file."
                    )
            } else if (packageFiles.size > 1) {
                diagnostics +=
                    diagnostic(
                        code =
                            LegacyTopicDiscoveryDiagnosticCode.DUPLICATE_PKG,
                        logicalTopicName =
                            logicalName,
                        sources =
                            packageFiles.map { file ->
                                file.source
                            },
                        message =
                            "Legacy topic '$logicalName' has multiple PKG files."
                    )
            }
        }

        addBaseNameMismatchDiagnostic(
            files = candidateFiles,
            diagnostics = diagnostics
        )

        val invalidSources =
            diagnostics
                .flatMap { diagnostic ->
                    diagnostic.sources
                }
                .toSet()

        val pairs =
            grouped
                .values
                .mapNotNull { topicFiles ->
                    val jsonFiles =
                        topicFiles.filter { file ->
                            file.kind == LegacyTopicFileKind.JSON
                        }
                    val packageFiles =
                        topicFiles.filter { file ->
                            file.kind == LegacyTopicFileKind.PKG
                        }

                    if (
                        jsonFiles.size != 1 ||
                        packageFiles.size != 1
                    ) {
                        return@mapNotNull null
                    }

                    val jsonFile =
                        jsonFiles.single()
                    val packageFile =
                        packageFiles.single()

                    if (
                        jsonFile.source in invalidSources ||
                        packageFile.source in invalidSources
                    ) {
                        return@mapNotNull null
                    }

                    val logicalName =
                        jsonFile.logicalBaseName()

                    ValidatedLegacyTopicPair(
                        logicalTopicName =
                            logicalName,
                        topicId =
                            TopicId.deriveForLegacyPackage(
                                packageName = logicalName,
                                packageFormat = "OPD3"
                            ),
                        jsonSource =
                            jsonFile.source,
                        packageSource =
                            packageFile.source
                    )
                }
                .sortedWith(pairComparator)

        return LegacyTopicDiscoveryResult(
            pairs = pairs,
            diagnostics =
                diagnostics
                    .distinct()
                    .sortedWith(diagnosticComparator)
        )
    }

    private fun addBaseNameMismatchDiagnostic(
        files: List<LegacyTopicDiscoveryFile>,
        diagnostics:
        MutableList<LegacyTopicDiscoveryDiagnostic>
    ) {
        val jsonFiles =
            files.filter { file ->
                file.kind == LegacyTopicFileKind.JSON
            }
        val packageFiles =
            files.filter { file ->
                file.kind == LegacyTopicFileKind.PKG
            }

        if (
            jsonFiles.size == 1 &&
            packageFiles.size == 1 &&
            jsonFiles.single().normalizedBaseName() !=
            packageFiles.single().normalizedBaseName()
        ) {
            diagnostics +=
                diagnostic(
                    code =
                        LegacyTopicDiscoveryDiagnosticCode.BASE_NAME_MISMATCH,
                    logicalTopicName = null,
                    sources =
                        listOf(
                            jsonFiles.single().source,
                            packageFiles.single().source
                        ),
                    message =
                        "Legacy JSON and PKG base names do not match."
                )
        }
    }

    private fun LegacyTopicDiscoveryFile.logicalBaseNameOrNull():
        String? =
        fileName
            .substringBeforeLast(
                delimiter = '.',
                missingDelimiterValue = ""
            )
            .takeIf(
                String::isNotBlank
            )

    private fun LegacyTopicDiscoveryFile.logicalBaseName():
        String =
        requireNotNull(
            logicalBaseNameOrNull()
        ) {
            "Legacy topic file must have a logical base name: $fileName"
        }

    private fun LegacyTopicDiscoveryFile.normalizedBaseName():
        String =
        logicalBaseName()
            .trim()
            .lowercase(
                Locale.ROOT
            )

    private fun diagnostic(
        code: LegacyTopicDiscoveryDiagnosticCode,
        logicalTopicName: String?,
        sources: List<String>,
        message: String
    ): LegacyTopicDiscoveryDiagnostic =
        LegacyTopicDiscoveryDiagnostic(
            code = code,
            logicalTopicName = logicalTopicName,
            sources = sources.sorted(),
            message = message
        )

    private companion object {

        val fileComparator:
            Comparator<LegacyTopicDiscoveryFile> =
            compareBy(
                { file ->
                    file.fileName.lowercase(
                        Locale.ROOT
                    )
                },
                LegacyTopicDiscoveryFile::fileName,
                LegacyTopicDiscoveryFile::source
            )

        val pairComparator:
            Comparator<ValidatedLegacyTopicPair> =
            compareBy(
                { pair ->
                    pair.logicalTopicName.lowercase(
                        Locale.ROOT
                    )
                },
                ValidatedLegacyTopicPair::logicalTopicName,
                ValidatedLegacyTopicPair::jsonSource,
                ValidatedLegacyTopicPair::packageSource
            )

        val diagnosticComparator:
            Comparator<LegacyTopicDiscoveryDiagnostic> =
            compareBy(
                { diagnostic ->
                    diagnostic.logicalTopicName
                        ?.lowercase(
                            Locale.ROOT
                        )
                        .orEmpty()
                },
                { diagnostic ->
                    diagnostic.code.name
                },
                { diagnostic ->
                    diagnostic.sources.joinToString(
                        separator = "\u0000"
                    )
                }
            )
    }
}
